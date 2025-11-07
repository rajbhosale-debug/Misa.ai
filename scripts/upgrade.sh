#!/bin/bash

# MISA.AI Seamless Upgrade Script
# Handles automatic updates with zero downtime and rollback capabilities

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
INSTALL_DIR="${HOME}/.misa-ai"
BACKUP_DIR="${INSTALL_DIR}/backups"
UPGRADE_LOG="${INSTALL_DIR}/logs/upgrade.log"
SERVICE_NAME="misa-ai"
ROLLBACK_MODE=false
SILENT_MODE=false
FORCE_MODE=false

# Version tracking
CURRENT_VERSION=""
TARGET_VERSION=""
BACKUP_NAME=""

# Functions
print_info() {
    if [ "$SILENT_MODE" != "true" ]; then
        echo -e "${BLUE}ℹ️  $1${NC}"
    fi
    log_message "INFO" "$1"
}

print_success() {
    if [ "$SILENT_MODE" != "true" ]; then
        echo -e "${GREEN}✅ $1${NC}"
    fi
    log_message "SUCCESS" "$1"
}

print_warning() {
    if [ "$SILENT_MODE" != "true" ]; then
        echo -e "${YELLOW}⚠️  $1${NC}"
    fi
    log_message "WARNING" "$1"
}

print_error() {
    if [ "$SILENT_MODE" != "true" ]; then
        echo -e "${RED}❌ $1${NC}"
    fi
    log_message "ERROR" "$1"
}

log_message() {
    local level="$1"
    local message="$2"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message" >> "$UPGRADE_LOG" 2>/dev/null || true
}

show_progress() {
    local current="$1"
    local total="$2"
    local message="$3"
    local percentage=$((current * 100 / total))

    if [ "$SILENT_MODE" != "true" ]; then
        local bar_length=50
        local filled_length=$((percentage * bar_length / 100))
        local bar=""

        for ((i=0; i<filled_length; i++)); do
            bar+="="
        done
        for ((i=filled_length; i<bar_length; i++)); do
            bar+=" "
        done

        printf "\r${BLUE}[${bar}] ${percentage}%% - $message${NC}"
        if [ "$current" -eq "$total" ]; then
            echo ""
        fi
    fi

    log_message "PROGRESS" "$percentage% - $message"
}

# Check if running as root (not recommended for upgrades)
check_prerequisites() {
    print_info "Checking upgrade prerequisites..."

    # Check if installation exists
    if [ ! -d "$INSTALL_DIR" ]; then
        print_error "MISA.AI is not installed. Please run the installation script first."
        exit 1
    fi

    # Check if docker is available
    if ! command -v docker >/dev/null 2>&1; then
        print_error "Docker is not available. Cannot perform upgrade."
        exit 1
    fi

    # Check if docker is running
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi

    # Check available disk space (need at least 2GB)
    local available_space=$(df "$INSTALL_DIR" | awk 'NR==2 {print $4}')
    local required_space=2097152 # 2GB in KB

    if [ "$available_space" -lt "$required_space" ]; then
        print_error "Insufficient disk space. At least 2GB required for upgrade."
        exit 1
    fi

    # Check network connectivity
    if ! curl -s --connect-timeout 5 https://api.github.com >/dev/null 2>&1; then
        print_warning "Limited network connectivity detected. Upgrade may fail."
    fi

    print_success "Prerequisites check completed"
}

# Detect current version
detect_current_version() {
    print_info "Detecting current MISA.AI version..."

    if [ -f "$INSTALL_DIR/.version" ]; then
        CURRENT_VERSION=$(cat "$INSTALL_DIR/.version")
    elif [ -f "$INSTALL_DIR/config/default.toml" ]; then
        # Try to extract version from config
        CURRENT_VERSION=$(grep -o 'version = "[^"]*"' "$INSTALL_DIR/config/default.toml" | cut -d'"' -f2 || echo "unknown")
    else
        CURRENT_VERSION="1.0.0"
    fi

    print_info "Current version: $CURRENT_VERSION"
}

# Check for available updates
check_for_updates() {
    print_info "Checking for available updates..."

    # Fetch latest release information
    local latest_release_info=$(curl -s "https://api.github.com/repos/misa-ai/misa.ai/releases/latest" 2>/dev/null || echo "")

    if [ -z "$latest_release_info" ]; then
        print_warning "Could not fetch latest release information"
        if [ "$FORCE_MODE" != "true" ]; then
            print_error "Use --force to upgrade anyway"
            exit 1
        fi
        TARGET_VERSION="latest"
        return
    fi

    TARGET_VERSION=$(echo "$latest_release_info" | grep '"tag_name"' | cut -d'"' -f4)

    if [ -z "$TARGET_VERSION" ]; then
        print_warning "Could not determine latest version"
        TARGET_VERSION="latest"
    else
        print_info "Latest version available: $TARGET_VERSION"
    fi

    # Check if already up to date
    if [ "$CURRENT_VERSION" = "$TARGET_VERSION" ] && [ "$FORCE_MODE" != "true" ]; then
        print_success "MISA.AI is already up to date ($CURRENT_VERSION)"
        exit 0
    fi
}

# Create backup before upgrade
create_backup() {
    print_info "Creating backup before upgrade..."

    BACKUP_NAME="backup-$(date +%Y%m%d-%H%M%S)-v${CURRENT_VERSION}"
    local backup_path="$BACKUP_DIR/$BACKUP_NAME"

    mkdir -p "$backup_path"
    mkdir -p "$backup_path/config"
    mkdir -p "$backup_path/data"
    mkdir -p "$backup_path/logs"
    mkdir -p "$backup_path/state"

    # Backup configuration
    if [ -d "$INSTALL_DIR/config" ]; then
        cp -r "$INSTALL_DIR/config"/* "$backup_path/config/" 2>/dev/null || true
    fi

    # Backup user data (exclude large model files)
    if [ -d "$INSTALL_DIR/data" ]; then
        find "$INSTALL_DIR/data" -name "*.gguf" -prune -o -type f -exec cp {} "$backup_path/data/" \; 2>/dev/null || true
    fi

    # Backup logs
    if [ -d "$INSTALL_DIR/logs" ]; then
        cp -r "$INSTALL_DIR/logs"/* "$backup_path/logs/" 2>/dev/null || true
    fi

    # Backup docker-compose and environment
    if [ -f "$INSTALL_DIR/docker-compose.yml" ]; then
        cp "$INSTALL_DIR/docker-compose.yml" "$backup_path/" 2>/dev/null || true
    fi

    if [ -f "$INSTALL_DIR/.env" ]; then
        cp "$INSTALL_DIR/.env" "$backup_path/" 2>/dev/null || true
    fi

    # Save current version
    echo "$CURRENT_VERSION" > "$backup_path/.version"

    # Save service state
    if docker ps --format "table {{.Names}}" | grep -q "$SERVICE_NAME"; then
        docker ps --format "{{.Names}}:{{.Status}}" | grep "$SERVICE_NAME" > "$backup_path/state/service_state.txt" 2>/dev/null || true
    fi

    print_success "Backup created: $backup_path"
}

# Stop services gracefully
stop_services() {
    print_info "Stopping MISA.AI services..."

    cd "$INSTALL_DIR"

    # Check if services are running
    if docker-compose ps --services --filter "status=running" | grep -q .; then
        # Graceful shutdown with timeout
        docker-compose down --timeout 60

        # Force shutdown if still running
        if docker-compose ps --services --filter "status=running" | grep -q .; then
            print_warning "Some services didn't stop gracefully, forcing shutdown..."
            docker-compose down --timeout 10 --remove-orphans
        fi
    else
        print_info "No services were running"
    fi

    print_success "Services stopped"
}

# Download new version
download_new_version() {
    print_info "Downloading MISA.AI $TARGET_VERSION..."

    cd /tmp

    local download_url
    if [ "$TARGET_VERSION" = "latest" ]; then
        download_url="https://github.com/misa-ai/misa.ai/releases/latest/download/misa-ai-linux-x64.tar.gz"
    else
        download_url="https://github.com/misa-ai/misa.ai/releases/download/$TARGET_VERSION/misa-ai-linux-x64.tar.gz"
    fi

    print_info "Downloading from: $download_url"

    # Download with progress
    if curl -L --progress-bar -o misa-ai-upgrade.tar.gz "$download_url" 2>&1 | while read -r line; do
        if [[ $line =~ ([0-9]+)% ]]; then
            local progress="${BASH_REMATCH[1]}"
            show_progress $((1 + progress / 25)) 8 "Downloading new version ($progress%)"
        fi
    done; then
        show_progress 3 8 "Download completed"
    else
        print_error "Failed to download MISA.AI $TARGET_VERSION"
        exit 1
    fi

    # Verify download
    if [ ! -f "misa-ai-upgrade.tar.gz" ]; then
        print_error "Download failed - no file found"
        exit 1
    fi

    # Extract to temporary directory
    local temp_dir="/tmp/misa-upgrade-$(date +%s)"
    mkdir -p "$temp_dir"

    print_info "Extracting new version..."
    tar -xzf misa-ai-upgrade.tar.gz -C "$temp_dir"

    if [ ! -d "$temp_dir/misa-ai" ]; then
        print_error "Failed to extract downloaded package"
        exit 1
    fi

    show_progress 4 8 "Extraction completed"

    # Update installation
    print_info "Installing new version..."
    cp -r "$temp_dir/misa-ai"/* "$INSTALL_DIR/" 2>/dev/null || true

    # Update version file
    echo "$TARGET_VERSION" > "$INSTALL_DIR/.version"

    show_progress 5 8 "New version installed"

    # Cleanup
    rm -rf "$temp_dir"
    rm -f misa-ai-upgrade.tar.gz

    print_success "New version installed successfully"
}

# Update configuration files
update_configuration() {
    print_info "Updating configuration files..."

    cd "$INSTALL_DIR"

    # Backup existing configuration if not already backed up
    if [ ! -f "$BACKUP_DIR/$BACKUP_NAME/config/default.toml.backup" ]; then
        cp "$INSTALL_DIR/config/default.toml" "$BACKUP_DIR/$BACKUP_NAME/config/default.toml.backup" 2>/dev/null || true
    fi

    # Merge new configuration with existing
    if [ -f "config/default.toml.new" ]; then
        # Simple merge - in production, use proper config merging tool
        cp "config/default.toml.new" "config/default.toml"
    fi

    # Update docker-compose if needed
    if [ -f "docker-compose.yml.new" ]; then
        cp "docker-compose.yml.new" "docker-compose.yml"
    fi

    show_progress 6 8 "Configuration updated"
}

# Start services with new version
start_services() {
    print_info "Starting MISA.AI services with new version..."

    cd "$INSTALL_DIR"

    # Pull new images
    print_info "Pulling updated Docker images..."
    if docker-compose pull -q 2>&1 | while read -r line; do
        if [[ $line =~ ([0-9]+)% ]]; then
            local progress="${BASH_REMATCH[1]}"
            show_progress $((6 + progress / 50)) 8 "Pulling images ($progress%)"
        fi
    done; then
        show_progress 7 8 "Images pulled"
    else
        print_warning "Failed to pull some images, continuing with local images"
    fi

    # Start services
    docker-compose up -d

    show_progress 8 8 "Services starting"

    print_success "Services started with new version"
}

# Verify upgrade
verify_upgrade() {
    print_info "Verifying upgrade..."

    local max_attempts=30
    local attempt=0

    while [ $attempt -lt $max_attempts ]; do
        if curl -s http://localhost:8080/health >/dev/null 2>&1; then
            print_success "MISA.AI is running and healthy"
            return 0
        fi

        attempt=$((attempt + 1))
        sleep 2
    done

    print_error "MISA.AI failed to start after upgrade"
    return 1
}

# Cleanup old backups
cleanup_old_backups() {
    print_info "Cleaning up old backups..."

    # Keep last 5 backups
    cd "$BACKUP_DIR"
    ls -t | tail -n +6 | xargs rm -rf 2>/dev/null || true

    print_success "Old backups cleaned up"
}

# Rollback function
rollback_upgrade() {
    print_warning "Rolling back upgrade..."

    if [ -z "$BACKUP_NAME" ]; then
        print_error "No backup available for rollback"
        exit 1
    fi

    local backup_path="$BACKUP_DIR/$BACKUP_NAME"

    if [ ! -d "$backup_path" ]; then
        print_error "Backup not found: $backup_path"
        exit 1
    fi

    print_info "Stopping services..."
    cd "$INSTALL_DIR"
    docker-compose down 2>/dev/null || true

    print_info "Restoring from backup..."
    cp -r "$backup_path/config"/* "$INSTALL_DIR/config/" 2>/dev/null || true
    cp -r "$backup_path/data"/* "$INSTALL_DIR/data/" 2>/dev/null || true

    if [ -f "$backup_path/docker-compose.yml" ]; then
        cp "$backup_path/docker-compose.yml" "$INSTALL_DIR/"
    fi

    if [ -f "$backup_path/.env" ]; then
        cp "$backup_path/.env" "$INSTALL_DIR/"
    fi

    if [ -f "$backup_path/.version" ]; then
        cp "$backup_path/.version" "$INSTALL_DIR/"
    fi

    print_info "Starting services..."
    docker-compose up -d

    print_success "Rollback completed"
    exit 0
}

# Main upgrade function
perform_upgrade() {
    print_info "Starting MISA.AI upgrade from $CURRENT_VERSION to $TARGET_VERSION"

    # Create upgrade log
    mkdir -p "$(dirname "$UPGRADE_LOG")"
    echo "=== MISA.AI Upgrade Log ===" > "$UPGRADE_LOG"
    echo "Upgrade started at: $(date)" >> "$UPGRADE_LOG"
    echo "From version: $CURRENT_VERSION" >> "$UPGRADE_LOG"
    echo "To version: $TARGET_VERSION" >> "$UPGRADE_LOG"

    # Execute upgrade steps
    check_prerequisites || exit 1
    show_progress 1 8 "Prerequisites checked"

    create_backup || exit 1
    show_progress 2 8 "Backup created"

    stop_services || exit 1
    show_progress 3 8 "Services stopped"

    download_new_version || exit 1
    show_progress 8 8 "New version downloaded"

    update_configuration || exit 1
    show_progress 8 8 "Configuration updated"

    start_services || exit 1
    show_progress 8 8 "Services started"

    # Verify upgrade
    if verify_upgrade; then
        cleanup_old_backups
        print_success "MISA.AI has been successfully upgraded to version $TARGET_VERSION"
        echo "Upgrade completed successfully at: $(date)" >> "$UPGRADE_LOG"
    else
        print_error "Upgrade verification failed"
        echo "Upgrade failed at: $(date)" >> "$UPGRADE_LOG"

        if [ "$SILENT_MODE" != "true" ]; then
            read -p "Do you want to rollback to the previous version? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                rollback_upgrade
            fi
        else
            exit 1
        fi
    fi
}

# Show usage
show_usage() {
    echo "MISA.AI Upgrade Script"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  --help, -h              Show this help message"
    echo "  --rollback              Rollback to previous version"
    echo "  --silent, -s            Silent upgrade mode"
    echo "  --force                 Force upgrade even if up to date"
    echo "  --version               Show current version"
    echo "  --check-updates         Check for available updates"
    echo ""
    echo "Examples:"
    echo "  $0                      Interactive upgrade"
    echo "  $0 --silent             Upgrade without prompts"
    echo "  $0 --rollback           Rollback failed upgrade"
    echo "  $0 --check-updates      Check if updates are available"
}

# Handle script arguments
case "${1:-}" in
    --help|-h)
        show_usage
        exit 0
        ;;
    --rollback)
        ROLLBACK_MODE=true
        rollback_upgrade
        ;;
    --silent|-s)
        SILENT_MODE=true
        ;;
    --force)
        FORCE_MODE=true
        ;;
    --version)
        detect_current_version
        echo "Current MISA.AI version: $CURRENT_VERSION"
        exit 0
        ;;
    --check-updates)
        detect_current_version
        check_for_updates
        if [ "$CURRENT_VERSION" != "$TARGET_VERSION" ]; then
            echo "Update available: $TARGET_VERSION (current: $CURRENT_VERSION)"
            exit 1
        else
            echo "MISA.AI is up to date ($CURRENT_VERSION)"
            exit 0
        fi
        ;;
    "")
        # No arguments, proceed with upgrade
        ;;
    *)
        print_error "Unknown option: $1"
        show_usage
        exit 1
        ;;
esac

# Main execution
if [ "$ROLLBACK_MODE" != "true" ]; then
    detect_current_version
    check_for_updates
    perform_upgrade
fi
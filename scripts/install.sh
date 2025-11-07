#!/bin/bash

# MISA.AI One-Click Installation Script
# This script installs MISA.AI with minimal user interaction
# Supports: Linux, macOS, Windows (WSL)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
INSTALL_DIR="${HOME}/.misa-ai"
SERVICE_NAME="misa-ai"
KERNEL_PORT=8080
WEB_PORT=3000
SILENT_MODE=false
AUTO_INSTALL_PREREQUISITES=false
BACKGROUND_INSTALL=false
PROGRESS_CALLBACK=""

# Functions
print_banner() {
    echo -e "${BLUE}"
    echo "  ___  __   __  __   __   ___  __   __   "
    echo " | __| \\ \\ / /  \\ \\ / /  | __| \\ \\ / /   "
    echo " | _|   \\ V /    \\ V /   | _|   \\ V /    "
    echo " |___|   |_|      |_|    |___|   |_|     "
    echo "                                           "
    echo "    🤖 Intelligent Assistant Platform     "
    echo "    Privacy-First AI with Enterprise Grade Security"
    echo -e "${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    if [ "$SILENT_MODE" != "true" ]; then
        echo -e "${BLUE}ℹ️  $1${NC}"
    fi
    log_message "INFO" "$1"
}

log_message() {
    local level="$1"
    local message="$2"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message" >> "$INSTALL_DIR/logs/installation.log" 2>/dev/null || true
}

show_progress() {
    local current="$1"
    local total="$2"
    local message="$3"
    local percentage=$((current * 100 / total))

    if [ -n "$PROGRESS_CALLBACK" ]; then
        "$PROGRESS_CALLBACK" "$percentage" "$message" 2>/dev/null || true
    fi

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

# Check if running as root (not recommended)
check_root() {
    if [ "$EUID" -eq 0 ]; then
        print_error "Please do not run this script as root."
        print_info "MISA.AI should be installed as a regular user for security."
        exit 1
    fi
}

# Detect OS and architecture
detect_platform() {
    OS="$(uname -s)"
    ARCH="$(uname -m)"

    case "$OS" in
        Linux*)
            PLATFORM="linux"
            ;;
        Darwin*)
            PLATFORM="macos"
            ;;
        CYGWIN*|MINGW*|MSYS*)
            PLATFORM="windows"
            print_warning "Windows detected. Using WSL is recommended."
            ;;
        *)
            print_error "Unsupported OS: $OS"
            exit 1
            ;;
    esac

    case "$ARCH" in
        x86_64)
            ARCH="amd64"
            ;;
        aarch64|arm64)
            ARCH="arm64"
            ;;
        armv7l)
            ARCH="arm"
            ;;
        *)
            print_error "Unsupported architecture: $ARCH"
            exit 1
            ;;
    esac

    print_success "Platform detected: $PLATFORM-$ARCH"
}

# Check network connectivity
check_network_connectivity() {
    print_info "Checking network connectivity..."

    if curl -s --connect-timeout 5 https://api.github.com >/dev/null 2>&1; then
        print_success "Network connectivity verified"
        return 0
    else
        print_warning "Limited network connectivity detected"
        return 1
    fi
}

# Check if Docker is installed and running
check_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        if [ "$AUTO_INSTALL_PREREQUISITES" = "true" ]; then
            print_info "Docker not found. Installing automatically..."
            install_docker
        else
            print_error "Docker is not installed."
            print_info "Please install Docker first:"
            echo "  • Linux: https://docs.docker.com/engine/install/"
            echo "  • macOS: Download Docker Desktop from https://www.docker.com/products/docker-desktop"
            echo "  • Windows: Use WSL2 and install Docker Desktop"
            echo "  • Or run with --auto-prereqs to install automatically"
            exit 1
        fi
    fi

    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker."
        exit 1
    fi

    print_success "Docker is available and running"
}

# Install Docker automatically based on platform
install_docker() {
    show_progress 1 10 "Installing Docker..."

    case "$PLATFORM" in
        "linux")
            install_docker_linux
            ;;
        "macos")
            install_docker_macos
            ;;
        "windows")
            print_warning "Automatic Docker installation not supported on Windows. Please install Docker Desktop manually."
            exit 1
            ;;
        *)
            print_error "Unsupported platform for automatic Docker installation"
            exit 1
            ;;
    esac

    show_progress 10 10 "Docker installation completed"
}

install_docker_linux() {
    # Check distribution
    if [ -f /etc/debian_version ]; then
        # Debian/Ubuntu
        print_info "Installing Docker on Debian/Ubuntu..."

        # Update package index
        sudo apt-get update -qq >/dev/null 2>&1

        # Install prerequisites
        sudo apt-get install -y ca-certificates curl gnupg lsb-release >/dev/null 2>&1

        # Add Docker's official GPG key
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg >/dev/null 2>&1

        # Set up the repository
        echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

        # Install Docker Engine
        sudo apt-get update -qq >/dev/null 2>&1
        sudo apt-get install -y docker-ce docker-ce-cli containerd.io >/dev/null 2>&1

        # Start and enable Docker
        sudo systemctl start docker >/dev/null 2>&1
        sudo systemctl enable docker >/dev/null 2>&1

        # Add user to docker group
        sudo usermod -aG docker "$USER" >/dev/null 2>&1

    elif [ -f /etc/redhat-release ]; then
        # RHEL/CentOS/Fedora
        print_info "Installing Docker on RHEL/CentOS/Fedora..."

        # Install prerequisites
        sudo yum install -y yum-utils >/dev/null 2>&1

        # Add Docker repository
        sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo >/dev/null 2>&1

        # Install Docker Engine
        sudo yum install -y docker-ce docker-ce-cli containerd.io >/dev/null 2>&1

        # Start and enable Docker
        sudo systemctl start docker >/dev/null 2>&1
        sudo systemctl enable docker >/dev/null 2>&1

        # Add user to docker group
        sudo usermod -aG docker "$USER" >/dev/null 2>&1
    else
        print_error "Unsupported Linux distribution for automatic Docker installation"
        exit 1
    fi
}

install_docker_macos() {
    print_info "Installing Docker Desktop on macOS..."

    # Check if Homebrew is installed
    if ! command -v brew >/dev/null 2>&1; then
        print_info "Installing Homebrew first..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)" >/dev/null 2>&1
    fi

    # Install Docker Desktop
    brew install --cask docker >/dev/null 2>&1

    print_info "Please start Docker Desktop manually after installation completes"
}

# Check if Docker Compose is available
check_docker_compose() {
    if ! command -v docker-compose >/dev/null 2>&1; then
        print_error "Docker Compose is not installed."
        print_info "Please install Docker Compose:"
        echo "  • Linux: sudo apt-get install docker-compose (Ubuntu/Debian)"
        echo "  • macOS: Included with Docker Desktop"
        echo "  • Or download from: https://docs.docker.com/compose/install/"
        exit 1
    fi

    print_success "Docker Compose is available"
}

# Create installation directory
create_install_dir() {
    show_progress 1 15 "Creating installation directory..."

    if [ -d "$INSTALL_DIR" ]; then
        if [ "$SILENT_MODE" = "true" ]; then
            # In silent mode, backup existing installation
            local backup_dir="${INSTALL_DIR}.backup.$(date +%s)"
            mv "$INSTALL_DIR" "$backup_dir"
            log_message "INFO" "Existing installation backed up to: $backup_dir"
        else
            print_warning "Installation directory already exists."
            read -p "Do you want to remove existing installation? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                rm -rf "$INSTALL_DIR"
                print_success "Removed existing installation"
            else
                print_info "Updating existing installation..."
            fi
        fi
    fi

    mkdir -p "$INSTALL_DIR"
    mkdir -p "$INSTALL_DIR/data"
    mkdir -p "$INSTALL_DIR/config"
    mkdir -p "$INSTALL_DIR/logs"
    mkdir -p "$INSTALL_DIR/backups"

    # Create installation state file for rollback
    echo '{"status":"started","step":"directory_created","timestamp":"'$(date -Iseconds)'"}' > "$INSTALL_DIR/.install-state"

    show_progress 2 15 "Installation directory created"
}

# Create backup for rollback
create_backup() {
    local backup_dir="${INSTALL_DIR}/backups/pre-$(date +%s)"
    mkdir -p "$backup_dir"

    # Backup current configuration and data
    if [ -d "$INSTALL_DIR/config" ]; then
        cp -r "$INSTALL_DIR/config" "$backup_dir/" 2>/dev/null || true
    fi
    if [ -d "$INSTALL_DIR/data" ]; then
        cp -r "$INSTALL_DIR/data" "$backup_dir/" 2>/dev/null || true
    fi

    echo "$backup_dir"
}

# Rollback installation on failure
rollback_install() {
    print_error "Installation failed. Rolling back..."

    # Stop any running services
    if [ -f "$INSTALL_DIR/docker-compose.yml" ]; then
        cd "$INSTALL_DIR"
        docker-compose down 2>/dev/null || true
    fi

    # Restore from backup if exists
    local latest_backup=$(ls -t "$INSTALL_DIR/backups" 2>/dev/null | head -1)
    if [ -n "$latest_backup" ]; then
        print_info "Restoring from backup..."
        cp -r "$INSTALL_DIR/backups/$latest_backup"/* "$INSTALL_DIR/" 2>/dev/null || true
    fi

    # Update installation state
    echo '{"status":"rolled_back","step":"rollback","timestamp":"'$(date -Iseconds)'"}' > "$INSTALL_DIR/.install-state"

    print_error "Installation rolled back due to errors"
}

# Silent installation function
silent_install() {
    SILENT_MODE=true
    AUTO_INSTALL_PREREQUISITES=true

    print_info "Starting silent installation..."

    # Set default values for prompts
    export DEBIAN_FRONTEND=noninteractive

    # Create installation progress tracking
    local total_steps=15
    local current_step=0

    # Enhanced silent installation with progress tracking
    check_prerequisites_silent || return 1
    ((current_step += 3)); show_progress $current_step $total_steps "Prerequisites checked"

    create_install_dir || return 1
    ((current_step += 1)); show_progress $current_step $total_steps "Directory created"

    download_distribution_silent || return 1
    ((current_step += 2)); show_progress $current_step $total_steps "Distribution downloaded"

    create_config || return 1
    ((current_step += 1)); show_progress $current_step $total_steps "Configuration created"

    create_docker_compose || return 1
    ((current_step += 1)); show_progress $current_step $total_steps "Docker compose created"

    create_env_file || return 1
    ((current_step += 1)); show_progress $current_step $total_steps "Environment configured"

    pull_images_silent || return 1
    ((current_step += 3)); show_progress $current_step $total_steps "Docker images pulled"

    start_services_silent || return 1
    ((current_step += 1)); show_progress $current_step $total_steps "Services started"

    wait_for_services_silent || return 1
    ((current_step += 1)); show_progress $current_step $total_steps "Services ready"

    download_model_silent || return 1
    ((current_step += 1)); show_progress $current_step $total_steps "AI model downloaded"

    create_management_scripts || return 1
    show_progress $total_steps $total_steps "Installation completed"

    # Update installation state
    echo '{"status":"completed","step":"finished","timestamp":"'$(date -Iseconds)'"}' > "$INSTALL_DIR/.install-state"

    print_success "Silent installation completed successfully"
}

# Check prerequisites in silent mode
check_prerequisites_silent() {
    show_progress 0 3 "Checking prerequisites..."

    check_root || return 1
    show_progress 1 3 "Root privileges checked"

    detect_platform || return 1
    show_progress 2 3 "Platform detected"

    check_docker || return 1
    show_progress 3 3 "Docker verified"

    check_docker_compose || return 1
    show_progress 4 3 "Docker Compose verified"

    check_network_connectivity || return 0  # Network connectivity is optional

    return 0
}

# Download MISA.AI distribution
download_distribution() {
    show_progress 3 15 "Downloading MISA.AI distribution..."

    # Check if we're in a git repository (development mode)
    if [ -d ".git" ] && [ -f "docker-compose.yml" ]; then
        print_info "Installing from current directory (development mode)"
        cp -r infrastructure/docker/docker-compose.yml "$INSTALL_DIR/"
        cp -r config "$INSTALL_DIR/" 2>/dev/null || true
    else
        # Download from GitHub releases
        LATEST_RELEASE=$(curl -s https://api.github.com/repos/misa-ai/misa.ai/releases/latest | grep tag_name | cut -d '"' -f 4)

        if [ -z "$LATEST_RELEASE" ]; then
            print_warning "Could not fetch latest release. Using development mode."
            if [ -f "infrastructure/docker/docker-compose.yml" ]; then
                cp -r infrastructure/docker/docker-compose.yml "$INSTALL_DIR/"
            else
                print_error "No distribution found. Please ensure you're in the MISA.AI repository."
                exit 1
            fi
        else
            DOWNLOAD_URL="https://github.com/misa-ai/misa.ai/releases/download/$LATEST_RELEASE/misa-ai-$PLATFORM-$ARCH.tar.gz"
            print_info "Downloading from: $DOWNLOAD_URL"

            cd /tmp
            curl -L -o misa-ai.tar.gz "$DOWNLOAD_URL"
            tar -xzf misa-ai.tar.gz
            cp -r misa-ai/* "$INSTALL_DIR/"
            rm -rf misa-ai.tar.gz misa-ai
            cd - >/dev/null
        fi
    fi

    show_progress 5 15 "Distribution downloaded and extracted"
}

# Silent version of download_distribution
download_distribution_silent() {
    show_progress 3 15 "Downloading distribution..."

    # Check if we're in a git repository (development mode)
    if [ -d ".git" ] && [ -f "docker-compose.yml" ]; then
        log_message "INFO" "Installing from current directory (development mode)"
        cp -r infrastructure/docker/docker-compose.yml "$INSTALL_DIR/" 2>/dev/null || true
        cp -r config "$INSTALL_DIR/" 2>/dev/null || true
    else
        # Download from GitHub releases with progress
        LATEST_RELEASE=$(curl -s https://api.github.com/repos/misa-ai/misa.ai/releases/latest | grep tag_name | cut -d '"' -f 4)

        if [ -z "$LATEST_RELEASE" ]; then
            log_message "WARNING" "Could not fetch latest release. Using development mode."
            if [ -f "infrastructure/docker/docker-compose.yml" ]; then
                cp -r infrastructure/docker/docker-compose.yml "$INSTALL_DIR/" 2>/dev/null || true
            else
                log_message "ERROR" "No distribution found. Please ensure you're in the MISA.AI repository."
                return 1
            fi
        else
            DOWNLOAD_URL="https://github.com/misa-ai/misa.ai/releases/download/$LATEST_RELEASE/misa-ai-$PLATFORM-$ARCH.tar.gz"
            log_message "INFO" "Downloading from: $DOWNLOAD_URL"

            cd /tmp
            if curl -L --progress-bar -o misa-ai.tar.gz "$DOWNLOAD_URL" 2>&1 | while read -r line; do
                if [[ $line =~ ([0-9]+)% ]]; then
                    local progress="${BASH_REMATCH[1]}"
                    show_progress $((3 + progress / 25)) 15 "Downloading distribution ($progress%)"
                fi
            done; then
                tar -xzf misa-ai.tar.gz
                cp -r misa-ai/* "$INSTALL_DIR/" 2>/dev/null || true
                rm -rf misa-ai.tar.gz misa-ai
                cd - >/dev/null
            else
                log_message "ERROR" "Failed to download distribution"
                return 1
            fi
        fi
    fi

    show_progress 5 15 "Distribution downloaded"
}

# Create configuration file
create_config() {
    show_progress 6 15 "Creating configuration..."

    if [ ! -f "$INSTALL_DIR/config/default.toml" ]; then
        cat > "$INSTALL_DIR/config/default.toml" << 'EOF'
# MISA.AI Default Configuration
# Privacy-First Settings

[network]
websocket_port = 8080
grpc_port = 50051
tls_enabled = false
bind_address = "0.0.0.0"

[models]
default_model = "mixtral"
local_server_url = "http://ollama:11434"
switching_preferences.prefer_local = true
auto_download_models = true

[devices]
discovery_enabled = true
remote_desktop_enabled = true
max_devices = 10
auto_pairing = true

[security]
auth_required = true
session_timeout_minutes = 30
plugin_sandboxing = true
audit_logging = true

[memory]
local_db_path = "/data/misa_memory.db"
retention_days = 365
encryption_enabled = true

[privacy]
data_collection = false
crash_reports = false
usage_analytics = false
local_only_processing = true

[ui]
theme = "auto"
avatar_enabled = true
voice_wake_word = "hey misa"
express_setup_completed = false
EOF
        print_success "Configuration file created"
    else
        print_info "Configuration file already exists"
    fi

    # Update installation state
    echo '{"status":"in_progress","step":"config_created","timestamp":"'$(date -Iseconds)'"}' > "$INSTALL_DIR/.install-state"

    show_progress 7 15 "Configuration created"
}

# Create Docker Compose file for user installation
create_docker_compose() {
    show_progress 8 15 "Creating Docker Compose configuration..."

    cat > "$INSTALL_DIR/docker-compose.yml" << 'EOF'
version: '3.8'

services:
  misa-kernel:
    image: misa-ai/kernel:latest
    container_name: misa-kernel
    ports:
      - "8080:8080"
      - "50051:50051"
    volumes:
      - ./data:/data
      - ./config:/config
      - ./logs:/logs
    environment:
      - RUST_LOG=info
      - MISA_DATA_DIR=/data
      - MISA_CONFIG_DIR=/config
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  ollama:
    image: ollama/ollama:latest
    container_name: misa-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:11434/api/tags"]
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: postgres:15-alpine
    container_name: misa-postgres
    environment:
      POSTGRES_DB: misa_ai
      POSTGRES_USER: misa
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-misa_secure_password}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U misa"]
      interval: 30s
      timeout: 10s
      retries: 3

  redis:
    image: redis:7-alpine
    container_name: misa-redis
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  web-app:
    image: misa-ai/web:latest
    container_name: misa-web
    ports:
      - "3000:80"
    environment:
      - REACT_APP_API_URL=http://localhost:8080
    depends_on:
      - misa-kernel
    restart: unless-stopped

volumes:
  ollama_data:
  postgres_data:
  redis_data:

networks:
  default:
    name: misa-ai-network
EOF

    show_progress 9 15 "Docker Compose configuration created"
}

# Create environment file
create_env_file() {
    show_progress 10 15 "Creating environment configuration..."

    if [ ! -f "$INSTALL_DIR/.env" ]; then
        cat > "$INSTALL_DIR/.env" << 'EOF'
# MISA.AI Environment Configuration
# Copy this file and customize as needed

# Database Configuration
POSTGRES_PASSWORD=misa_secure_password_change_me

# AI Model API Keys (Optional - for cloud models)
# OPENAI_API_KEY=your_openai_key_here
# ANTHROPIC_API_KEY=your_claude_key_here
# GOOGLE_AI_API_KEY=your_gemini_key_here

# Security Keys (Generate your own for production)
JWT_SECRET=$(openssl rand -base64 32)
ENCRYPTION_KEY=$(openssl rand -hex 32)

# Cloud Services (Optional)
# AWS_ACCESS_KEY=your_aws_key
# AWS_SECRET_KEY=your_aws_secret
# AWS_REGION=us-east-1

# Monitoring (Optional)
# SENTRY_DSN=your_sentry_dsn

# Feature Flags
ENABLE_CLOUD_MODELS=false
ENABLE_TELEMETRY=false
ENABLE_CRASH_REPORTS=false
ENABLE_SILENT_MODE=true
AUTO_INSTALL_PREREQUISITES=true
EOF
        print_success "Environment file created"
    else
        print_info "Environment file already exists"
    fi

    show_progress 11 15 "Environment configuration created"
}

# Pull Docker images
pull_images() {
    show_progress 12 15 "Pulling Docker images (this may take a few minutes)..."

    cd "$INSTALL_DIR"
    docker-compose pull

    show_progress 15 15 "Docker images pulled"
}

# Silent version of pull_images
pull_images_silent() {
    show_progress 12 15 "Pulling Docker images..."

    cd "$INSTALL_DIR"
    if docker-compose pull -q 2>&1 | while read -r line; do
        if [[ $line =~ ([0-9]+)% ]]; then
            local progress="${BASH_REMATCH[1]}"
            show_progress $((12 + progress / 33)) 15 "Pulling Docker images ($progress%)"
        fi
    done; then
        show_progress 15 15 "Docker images pulled"
    else
        log_message "ERROR" "Failed to pull Docker images"
        return 1
    fi
}

# Start services
start_services() {
    print_info "Starting MISA.AI services..."

    cd "$INSTALL_DIR"
    docker-compose up -d

    print_success "Services started"
}

# Silent version of start_services
start_services_silent() {
    show_progress 15 15 "Starting services..."

    cd "$INSTALL_DIR"
    if docker-compose up -d 2>/dev/null; then
        log_message "INFO" "Services started successfully"
        show_progress 16 15 "Services started"
    else
        log_message "ERROR" "Failed to start services"
        return 1
    fi
}

# Wait for services to be ready
wait_for_services() {
    print_info "Waiting for services to be ready..."

    # Wait for kernel
    for i in {1..30}; do
        if curl -s http://localhost:$KERNEL_PORT/health >/dev/null 2>&1; then
            print_success "Kernel is ready"
            break
        fi
        if [ $i -eq 30 ]; then
            print_warning "Kernel is taking longer to start..."
        fi
        sleep 2
    done

    # Wait for web app
    for i in {1..30}; do
        if curl -s http://localhost:$WEB_PORT >/dev/null 2>&1; then
            print_success "Web application is ready"
            break
        fi
        if [ $i -eq 30 ]; then
            print_warning "Web application is taking longer to start..."
        fi
        sleep 2
    done
}

# Silent version of wait_for_services
wait_for_services_silent() {
    show_progress 16 15 "Waiting for services to be ready..."

    # Wait for kernel
    for i in {1..30}; do
        if curl -s http://localhost:$KERNEL_PORT/health >/dev/null 2>&1; then
            log_message "INFO" "Kernel is ready"
            break
        fi
        if [ $i -eq 30 ]; then
            log_message "WARNING" "Kernel is taking longer to start..."
        fi
        sleep 2
    done

    # Wait for web app
    for i in {1..30}; do
        if curl -s http://localhost:$WEB_PORT >/dev/null 2>&1; then
            log_message "INFO" "Web application is ready"
            break
        fi
        if [ $i -eq 30 ]; then
            log_message "WARNING" "Web application is taking longer to start..."
        fi
        sleep 2
    done

    show_progress 17 15 "Services ready"
}

# Download default AI model
download_model() {
    print_info "Downloading default AI model (Mixtral)..."

    # Wait for Ollama to be ready
    for i in {1..60}; do
        if curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
            break
        fi
        if [ $i -eq 60 ]; then
            print_warning "Ollama is taking longer to start. Model download will be skipped."
            print_info "You can download models later with: docker exec -it misa-ollama ollama pull mixtral"
            return
        fi
        sleep 2
    done

    # Download Mixtral model
    if docker exec misa-ollama ollama pull mixtral; then
        print_success "Mixtral model downloaded"
    else
        print_warning "Failed to download Mixtral model"
        print_info "You can download it manually later with: docker exec -it misa-ollama ollama pull mixtral"
    fi
}

# Silent version of download_model
download_model_silent() {
    show_progress 17 15 "Downloading AI model..."

    # Wait for Ollama to be ready
    for i in {1..60}; do
        if curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
            break
        fi
        if [ $i -eq 60 ]; then
            log_message "WARNING" "Ollama is taking longer to start. Model download will be skipped."
            return 0
        fi
        sleep 2
    done

    # Download Mixtral model silently
    if docker exec misa-ollama ollama pull mixtral >/dev/null 2>&1; then
        log_message "INFO" "Mixtral model downloaded successfully"
        show_progress 18 15 "AI model downloaded"
    else
        log_message "WARNING" "Failed to download Mixtral model"
        show_progress 18 15 "AI model download skipped"
    fi
}

# Create management scripts
create_management_scripts() {
    print_info "Creating management scripts..."

    # Start script
    cat > "$INSTALL_DIR/start.sh" << EOF
#!/bin/bash
# MISA.AI Start Script

cd "\$(dirname "\$0")"
echo "🚀 Starting MISA.AI..."
docker-compose up -d
echo "✅ MISA.AI started"
echo "📱 Web App: http://localhost:$WEB_PORT"
echo "🔧 Kernel API: http://localhost:$KERNEL_PORT"
EOF

    # Stop script
    cat > "$INSTALL_DIR/stop.sh" << EOF
#!/bin/bash
# MISA.AI Stop Script

cd "\$(dirname "\$0")"
echo "🛑 Stopping MISA.AI..."
docker-compose down
echo "✅ MISA.AI stopped"
EOF

    # Status script
    cat > "$INSTALL_DIR/status.sh" << EOF
#!/bin/bash
# MISA.AI Status Script

cd "\$(dirname "\$0")"
echo "📊 MISA.AI Status:"
docker-compose ps
echo ""
echo "📱 Web App: http://localhost:$WEB_PORT"
echo "🔧 Kernel API: http://localhost:$KERNEL_PORT"
EOF

    # Update script
    cat > "$INSTALL_DIR/update.sh" << EOF
#!/bin/bash
# MISA.AI Update Script

cd "\$(dirname "\$0")"
echo "🔄 Updating MISA.AI..."
docker-compose pull
docker-compose up -d
echo "✅ MISA.AI updated"
EOF

    # Make scripts executable
    chmod +x "$INSTALL_DIR"/*.sh

    print_success "Management scripts created"
}

# Show success message
show_success() {
    echo ""
    echo -e "${GREEN}🎉 MISA.AI Installation Complete!${NC}"
    echo ""
    echo -e "${BLUE}📱 Access Your AI Assistant:${NC}"
    echo "   Web Application: http://localhost:$WEB_PORT"
    echo "   Kernel API:      http://localhost:$KERNEL_PORT"
    echo ""
    echo -e "${BLUE}🔧 Management Commands:${NC}"
    echo "   Start:   $INSTALL_DIR/start.sh"
    echo "   Stop:    $INSTALL_DIR/stop.sh"
    echo "   Status:  $INSTALL_DIR/status.sh"
    echo "   Update:  $INSTALL_DIR/update.sh"
    echo ""
    echo -e "${BLUE}📁 Installation Directory:${NC}"
    echo "   $INSTALL_DIR"
    echo ""
    echo -e "${BLUE}📚 Next Steps:${NC}"
    echo "   1. Open http://localhost:$WEB_PORT in your browser"
    echo "   2. Complete the setup wizard"
    echo "   3. Configure your AI models and preferences"
    echo "   4. Start using your intelligent assistant!"
    echo ""
    echo -e "${BLUE}🆘 Need Help?${NC}"
    echo "   • Documentation: https://docs.misa.ai"
    echo "   • Community: https://discord.gg/misa-ai"
    echo "   • Issues: https://github.com/misa-ai/misa.ai/issues"
    echo ""
    echo -e "${GREEN}✨ Welcome to MISA.AI - Your Privacy-First AI Assistant!${NC}"
}

# Main installation flow
main() {
    if [ "$SILENT_MODE" != "true" ]; then
        print_banner
        print_info "Starting MISA.AI installation..."
        echo ""
    else
        log_message "INFO" "Starting MISA.AI installation..."
    fi

    # Create backup before installation
    create_backup >/dev/null 2>&1

    # Installation with error handling and rollback
    if [ "$SILENT_MODE" = "true" ]; then
        if ! silent_install; then
            rollback_install
            exit 1
        fi
    else
        # Interactive installation
        # Check prerequisites
        check_root || exit 1
        detect_platform || exit 1
        check_docker || exit 1
        check_docker_compose || exit 1
        check_network_connectivity || true

        # Installation steps
        create_install_dir || exit 1
        download_distribution || exit 1
        create_config || exit 1
        create_docker_compose || exit 1
        create_env_file || exit 1
        pull_images || exit 1
        start_services || exit 1
        wait_for_services || true  # Continue even if services take time
        download_model || true  # Continue even if model download fails
        create_management_scripts || exit 1

        # Success
        show_success
    fi
}

# Handle script arguments
case "${1:-}" in
    --help|-h)
        echo "MISA.AI Installation Script"
        echo ""
        echo "Usage: $0 [options]"
        echo ""
        echo "Options:"
        echo "  --help, -h     Show this help message"
        echo "  --uninstall    Remove MISA.AI installation"
        echo ""
        echo "This script installs MISA.AI with Docker."
        echo "Prerequisites: Docker, Docker Compose"
        exit 0
        ;;
    --uninstall)
        echo "🗑️  Uninstalling MISA.AI..."
        if [ -d "$INSTALL_DIR" ]; then
            cd "$INSTALL_DIR"
            docker-compose down -v 2>/dev/null || true
            cd - >/dev/null
            rm -rf "$INSTALL_DIR"
            echo "✅ MISA.AI uninstalled successfully"
        else
            echo "ℹ️  MISA.AI is not installed"
        fi
        exit 0
        ;;
    "")
        # No arguments, run main installation
        main
        ;;
    *)
        print_error "Unknown option: $1"
        echo "Use --help for usage information"
        exit 1
        ;;
esac
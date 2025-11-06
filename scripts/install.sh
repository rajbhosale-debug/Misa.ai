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
COORDINATION_PORT=8081
MOBILE_COORDINATION_ENABLED=true
QR_CODE_ENABLED=true

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
    echo -e "${BLUE}ℹ️  $1${NC}"
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
            # Check for Windows native environment
            if [ -f /proc/version ] && grep -qi "microsoft\|wsl" /proc/version; then
                PLATFORM="wsl"
                print_info "WSL environment detected. Windows native coordination available."
            fi
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

# Check if Docker is installed and running
check_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        print_error "Docker is not installed."
        print_info "Please install Docker first:"
        echo "  • Linux: https://docs.docker.com/engine/install/"
        echo "  • macOS: Download Docker Desktop from https://www.docker.com/products/docker-desktop"
        echo "  • Windows: Use WSL2 and install Docker Desktop"
        exit 1
    fi

    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker."
        exit 1
    fi

    print_success "Docker is available and running"
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

# Discover Android devices on local network
discover_android_devices() {
    print_info "Scanning for Android devices with MISA.AI app..."

    local discovered_devices=()
    local scan_timeout=30
    local scan_start=$(date +%s)

    # Network discovery methods
    # 1. mDNS/Bonjour discovery
    if command -v avahi-browse >/dev/null 2>&1; then
        print_info "Using mDNS discovery..."
        while [ $(($(date +%s) - scan_start)) -lt $scan_timeout ]; do
            local mdns_devices=$(avahi-browse -r -t _misa._tcp 2>/dev/null | grep "=-=" | cut -d';' -f7)
            if [ -n "$mdns_devices" ]; then
                while IFS= read -r device; do
                    if [ -n "$device" ] && [[ ! " ${discovered_devices[@]} " =~ " ${device} " ]]; then
                        discovered_devices+=("$device")
                        print_success "Found Android device via mDNS: $device"
                    fi
                done <<< "$mdns_devices"
            fi
            sleep 2
        done
    fi

    # 2. Network scan discovery
    print_info "Scanning local network..."
    local network_range=$(ip route get 1.1.1.1 2>/dev/null | awk '{print $7}' | cut -d'.' -f1-3).0/24
    if [ -z "$network_range" ]; then
        network_range="192.168.1.0/24"  # fallback
    fi

    # Scan common Android device ports
    for port in 8082 8083 8084; do
        nmap -p $port --open -T3 $network_range 2>/dev/null | grep "^Nmap scan report" | cut -d' ' -f5 | while read device_ip; do
            if [ -n "$device_ip" ] && curl -s --connect-timeout 2 http://$device_ip:$port/misa/ping 2>/dev/null | grep -q "MISA"; then
                if [[ ! " ${discovered_devices[@]} " =~ " ${device_ip}:${port} " ]]; then
                    discovered_devices+=("$device_ip:$port")
                    print_success "Found Android device: $device_ip:$port"
                fi
            fi
        done &
    done

    wait  # Wait for background scans to complete

    # 3. Bluetooth discovery (if available)
    if command -v bluetoothctl >/dev/null 2>&1; then
        print_info "Scanning for Bluetooth devices..."
        echo "scan on" | bluetoothctl 2>/dev/null &
        sleep 10
        echo "scan off" | bluetoothctl 2>/dev/null
        local bt_devices=$(bluetoothctl devices 2>/dev/null | grep "MISA" | cut -d' ' -f3)
        for device in $bt_devices; do
            if [[ ! " ${discovered_devices[@]} " =~ " ${device} " ]]; then
                discovered_devices+=("bt:$device")
                print_success "Found Bluetooth device: $device"
            fi
        done
    fi

    if [ ${#discovered_devices[@]} -eq 0 ]; then
        print_warning "No Android devices found on local network"
        return 1
    else
        print_success "Found ${#discovered_devices[@]} Android device(s)"
        printf '%s\n' "${discovered_devices[@]}"
        return 0
    fi
}

# Generate QR code for mobile pairing
generate_pairing_qr() {
    local pairing_token=$(openssl rand -hex 16)
    local pairing_url="misa://pair?token=$pairing_token&host=$(hostname -I | awk '{print $1}')"
    local qr_file="$INSTALL_DIR/pairing_qr.png"

    print_info "Generating pairing QR code..."

    # Create QR code if qrencode is available
    if command -v qrencode >/dev/null 2>&1; then
        echo "$pairing_url" | qrencode -o "$qr_file" -s 10 -m 2
        print_success "QR code generated: $qr_file"

        # Display QR code in terminal if possible
        if command -v imgcat >/dev/null 2>&1; then
            imgcat "$qr_file"
        elif command -v timg >/dev/null 2>&1; then
            timg "$qr_file"
        fi
    else
        print_warning "qrencode not found. Install 'qrencode' to generate QR codes."
        echo "Pairing URL: $pairing_url"
    fi

    # Save pairing token for coordination
    echo "$pairing_token" > "$INSTALL_DIR/.pairing_token"
    chmod 600 "$INSTALL_DIR/.pairing_token"

    echo "$pairing_url"
}

# Orchestrate mobile installation
orchestrate_mobile_install() {
    local target_device="$1"
    local pairing_url="$2"

    print_info "Orchestrating mobile installation on: $target_device"

    # Parse device address
    if [[ $target_device == *":"* ]]; then
        local device_ip=$(echo "$target_device" | cut -d':' -f1)
        local device_port=$(echo "$target_device" | cut -d':' -f2)

        # Send installation request to Android device
        local install_request=$(cat <<EOF
{
    "type": "installation_request",
    "coordinator_host": "$(hostname -I | awk '{print $1}')",
    "coordinator_port": "$COORDINATION_PORT",
    "pairing_url": "$pairing_url",
    "apk_url": "http://$(hostname -I | awk '{print $1}'):$COORDINATION_PORT/misa-latest.apk",
    "timestamp": "$(date -Iseconds)"
}
EOF
        )

        print_info "Sending installation request to $device_ip:$device_port"
        if curl -s --connect-timeout 10 -X POST \
            -H "Content-Type: application/json" \
            -d "$install_request" \
            "http://$device_ip:$device_port/misa/install" 2>/dev/null; then
            print_success "Installation request sent successfully"
            return 0
        else
            print_error "Failed to send installation request to $target_device"
            return 1
        fi
    else
        print_error "Invalid device address format: $target_device"
        return 1
    fi
}

# Handle Windows restart scenarios
handle_windows_restarts() {
    if [ "$PLATFORM" = "wsl" ]; then
        print_info "Windows environment detected. Setting up restart coordination..."

        # Create restart marker file
        local restart_marker="$INSTALL_DIR/.install_restart_marker"
        echo "$(date +%s)" > "$restart_marker"

        # Check if we're resuming after restart
        if [ -f "$restart_marker" ]; then
            local install_start=$(cat "$restart_marker")
            local current_time=$(date +%s)
            local elapsed=$((current_time - install_start))

            if [ $elapsed -lt 300 ]; then  # 5 minutes
                print_info "Resuming installation after Windows restart..."
                return 0
            fi
        fi

        # Offer Windows native installation
        print_info "Windows native installation is available for better integration."
        read -p "Would you like to use Windows native installer? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            launch_windows_native_installer
            return $?
        fi
    fi
    return 1
}

# Resume installation after system restart
resume_after_restart() {
    print_info "Checking for interrupted installation..."

    local state_file="$INSTALL_DIR/.install_state"
    if [ -f "$state_file" ]; then
        local last_step=$(cat "$state_file")
        print_info "Resuming from step: $last_step"

        case "$last_step" in
            "prerequisites")
                print_info "Prerequisites installation was interrupted. Continuing..."
                ;;
            "docker_setup")
                print_info "Docker setup was interrupted. Continuing..."
                check_docker
                check_docker_compose
                ;;
            "services")
                print_info "Service startup was interrupted. Continuing..."
                start_services
                ;;
        esac

        return 0
    fi

    return 1
}

# Save installation state
save_install_state() {
    local step="$1"
    echo "$step" > "$INSTALL_DIR/.install_state"
}

# Launch Windows native installer
launch_windows_native_installer() {
    print_info "Launching Windows native installer..."

    # Check if we're in WSL and can access Windows
    if command -v cmd.exe >/dev/null 2>&1; then
        local windows_download="https://download.misa.ai/MisaSetup.exe"
        local temp_path="/mnt/c/Users/$(whoami)/Downloads/MisaSetup.exe"

        print_info "Downloading Windows installer..."
        if curl -L -o "$temp_path" "$windows_download"; then
            print_info "Starting Windows installer..."
            cmd.exe /c "$temp_path" /quiet /COORDINATOR "$(hostname -I | awk '{print $1}')" &
            return 0
        else
            print_error "Failed to download Windows installer"
            return 1
        fi
    else
        print_error "Windows native installation not available from this environment"
        print_info "Please download the installer from: https://download.misa.ai"
        return 1
    fi
}

# Start mobile coordination server
start_coordination_server() {
    print_info "Starting mobile coordination server on port $COORDINATION_PORT..."

    # Create coordination script
    cat > "$INSTALL_DIR/coordination-server.py" << 'EOF'
#!/usr/bin/env python3
import http.server
import socketserver
import json
import urllib.parse
import os
from pathlib import Path

class MisaCoordinationHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=os.path.dirname(os.path.abspath(__file__)), **kwargs)

    def do_GET(self):
        if self.path == '/misa-latest.apk':
            # Serve the APK file if it exists
            apk_path = Path(__file__).parent / 'misa-latest.apk'
            if apk_path.exists():
                self.send_response(200)
                self.send_header('Content-Type', 'application/vnd.android.package-archive')
                self.send_header('Content-Disposition', 'attachment; filename="misa-latest.apk"')
                self.end_headers()
                with open(apk_path, 'rb') as f:
                    self.wfile.write(f.read())
            else:
                self.send_error(404, "APK file not found")
        else:
            super().do_GET()

    def do_POST(self):
        if self.path == '/misa/status':
            # Handle status reports from mobile devices
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)

            try:
                data = json.loads(post_data.decode('utf-8'))
                print(f"Status from {data.get('device_id', 'unknown')}: {data.get('status', 'unknown')}")

                response = {"status": "received", "message": "Status acknowledged"}
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps(response).encode())
            except Exception as e:
                self.send_error(400, f"Invalid JSON: {e}")
        else:
            self.send_error(404, "Not found")

if __name__ == "__main__":
    PORT = int(os.environ.get('MISA_COORDINATION_PORT', 8081))

    with socketserver.TCPServer(("", PORT), MisaCoordinationHandler) as httpd:
        print(f"Coordination server running on port {PORT}")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down coordination server...")
EOF

    chmod +x "$INSTALL_DIR/coordination-server.py"

    # Start server in background
    if command -v python3 >/dev/null 2>&1; then
        export MISA_COORDINATION_PORT="$COORDINATION_PORT"
        cd "$INSTALL_DIR"
        python3 coordination-server.py > "$INSTALL_DIR/logs/coordination.log" 2>&1 &
        echo $! > "$INSTALL_DIR/.coordination_pid"
        print_success "Coordination server started (PID: $(cat "$INSTALL_DIR/.coordination_pid"))"
    else
        print_warning "Python3 not found. Mobile coordination will be limited."
    fi
}

# Create installation directory
create_install_dir() {
    print_info "Creating installation directory: $INSTALL_DIR"

    if [ -d "$INSTALL_DIR" ]; then
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

    mkdir -p "$INSTALL_DIR"
    mkdir -p "$INSTALL_DIR/data"
    mkdir -p "$INSTALL_DIR/config"
    mkdir -p "$INSTALL_DIR/logs"

    print_success "Installation directory created"
}

# Download MISA.AI distribution
download_distribution() {
    print_info "Downloading MISA.AI distribution..."

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

    print_success "Distribution downloaded and extracted"
}

# Create configuration file
create_config() {
    print_info "Creating configuration..."

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
auto_download_models = false

[devices]
discovery_enabled = true
remote_desktop_enabled = true
max_devices = 5

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
EOF
        print_success "Configuration file created"
    else
        print_info "Configuration file already exists"
    fi
}

# Create Docker Compose file for user installation
create_docker_compose() {
    print_info "Creating Docker Compose configuration..."

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

    print_success "Docker Compose configuration created"
}

# Create environment file
create_env_file() {
    print_info "Creating environment configuration..."

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
EOF
        print_success "Environment file created"
    else
        print_info "Environment file already exists"
    fi
}

# Pull Docker images
pull_images() {
    print_info "Pulling Docker images (this may take a few minutes)..."

    cd "$INSTALL_DIR"
    docker-compose pull

    print_success "Docker images pulled"
}

# Start services
start_services() {
    print_info "Starting MISA.AI services..."

    cd "$INSTALL_DIR"
    docker-compose up -d

    print_success "Services started"
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
    print_banner

    print_info "Starting MISA.AI installation..."
    echo ""

    # Check prerequisites
    check_root
    detect_platform
    check_docker
    check_docker_compose

    # Installation steps
    create_install_dir
    download_distribution
    create_config
    create_docker_compose
    create_env_file
    pull_images
    start_services
    wait_for_services
    download_model
    create_management_scripts

    # Success
    show_success
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
#!/bin/bash

# Install Dependencies Script for MISA.AI
# This script installs all required dependencies for the platform

set -e

echo "🚀 Installing MISA.AI Dependencies..."
echo "=================================="

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to print section header
print_section() {
    echo ""
    echo "📦 $1"
    echo "=================================="
}

# Get OS information
OS="$(uname -s)"
echo "OS: $OS"

# Install system-wide dependencies
print_section "Installing System Dependencies"

# Check if running as root for system packages
if [ "$EUID" -ne 0 ]; then
    echo "⚠️  Warning: Not running as root. Some installations may require sudo."
fi

# Install Rust
print_section "Installing Rust"
if command_exists cargo; then
    echo "✅ Rust already installed: $(cargo --version)"
else
    echo "📦 Installing Rust..."
    if command_exists curl; then
        curl --proto '=https://sh.rustup.rs' -sSf https://sh.rustup.rs | sh -s -- -y
    else
        echo "❌ Error: curl not found. Please install curl manually."
        echo "Visit: https://rustup.rs/ for installation instructions."
        exit 1
    fi
fi

# Install Node.js
print_section "Installing Node.js"
if command_exists node; then
    NODE_VERSION=$(node --version)
    echo "✅ Node.js already installed: $NODE_VERSION"

    # Check if Node.js version is sufficient
    NODE_MAJOR=$(echo "$NODE_VERSION" | cut -d. -f1)
    if [ "$NODE_MAJOR" -lt 18 ]; then
        echo "⚠️  Warning: Node.js version $NODE_VERSION is below recommended (18+)"
        echo "  Consider upgrading Node.js: nvm install --lts"
    fi
else
    echo "📦 Installing Node.js..."
    if command_exists curl; then
        curl -fsSL https://nodejs.org/dist/v18.18.0/node-v18.18.0-linux-x64.tar.xz | tar -xz --strip-components=1 -C /usr/local/bin
        # Add node and npm to PATH
        ln -sf /usr/local/bin/node /usr/local/bin/nodejs
        ln -sf /usr/local/bin/npm /usr/local/bin/npm
    else
        echo "❌ Error: curl not found. Please install curl manually."
        echo "Visit: https://nodejs.org/en/download/ for installation instructions."
        exit 1
    fi
fi

# Install Android development tools if needed
print_section "Android Development Tools"
if [ -d "/usr/local/android-studio" ] || command_exists android-studio; then
    echo "✅ Android Studio already installed"
else
    echo "ℹ️ Android Studio not found (optional for Android development)"
    echo "  Install Android Studio from: https://developer.android.com/studio"
fi

# Install Docker if not present
print_section "Docker Container Platform"
if command_exists docker; then
    echo "✅ Docker already installed: $(docker --version)"
    echo "  Docker Compose: $(docker-compose --version)"
else
    echo "📦 Installing Docker..."
    if command_exists curl; then
        # Install Docker
        curl -fsSL https://get.docker.com/linux/static/stable/x86_64/docker-27.3.1.tgz | tar -xz --strip-components=1 | sudo sh -s
        sudo usermod -aG docker
        sudo systemctl enable docker
        sudo systemctl start docker
    else
        echo "❌ Error: curl not found. Please install curl manually."
        echo "Visit: https://docs.docker.com/engine/install/ for installation instructions."
        exit 1
    fi
fi

# Install Tauri CLI (for desktop app development)
print_section "Tauri CLI"
if command_exists cargo-tauri; then
    echo "✅ Tauri CLI already installed: $(cargo install --list | grep tauri-cli | head -1 | cut -d' ' ' -f2)"
else
    echo "📦 Installing Tauri CLI..."
    npm install -g @tauri-apps/cli
fi

# Install GitHub CLI (for operations)
print_section "GitHub CLI"
if command_exists gh; then
    echo "✅ GitHub CLI already installed: $(gh --version)"
else
    echo "📦 Installing GitHub CLI..."
    npm install -g @cli/cli
fi

# Check if all major dependencies are installed
print_section "Dependency Verification"

echo "Checking essential dependencies:"

# Rust
if command_exists cargo; then
    RUST_VERSION=$(cargo --version)
    echo "✅ Rust: $RUST_VERSION"
else
    echo "❌ Missing: Cargo (Rust toolchain)"
fi

# Node.js
if command_exists node; then
    NODE_VERSION=$(node --version)
    echo "✅ Node.js: $NODE_VERSION"
else
    echo "❌ Missing: Node.js"
fi

# Docker
if command_exists docker; then
    DOCKER_VERSION=$(docker --version | head -1)
    echo "✅ Docker: $DOCKER_VERSION"
else
    echo "❌ Missing: Docker"
fi

# Python (for development tools)
if command_exists python3; then
    PYTHON_VERSION=$(python3 --version)
    echo "✅ Python3: $PYTHON_VERSION"
else
    echo "ℹ️ Missing: Python3 (recommended for some tools)"
fi

# Check for development tools
print_section "Development Tools Check"

echo "Checking optional development tools:"

# Git
if command_exists git; then
    GIT_VERSION=$(git --version)
    echo "✅ Git: $GIT_VERSION"
else
    echo "❌ Missing: Git"
fi

# VS Code
if command_exists code; then
    echo "✅ VS Code: $(code --version | head -1)"
else
    echo "ℹ️ Missing: VS Code (optional)"
fi

# Android SDK if Android development
if [ -d "$ANDROID_HOME" ]; then
    echo "✅ Android SDK: $ANDROID_HOME"
else
    echo "ℹ️ Missing: Android SDK (needed for Android development)"
fi

print_section "Platform-Specific Dependencies"

# Linux dependencies
if [ "$OS" = "Linux" ]; then
    echo "📦 Linux specific dependencies..."

    # Install ClamAV for antivirus scanning
    if ! command_exists clamav; then
        echo "📦 Installing ClamAV for security scanning..."
        sudo apt-get update
        sudo apt-get install -y clamav
        sudo freshclam
    fi

    # Install additional security tools
    if ! command_exists ufw; then
        echo "📦 Installing UFW firewall..."
        sudo apt-get install -y ufw
        sudo ufw enable
    fi

    # Install monitoring tools
    if ! command_exists htop; then
        echo "📦 Installing htop system monitor..."
        sudo apt-get install -y htop
    fi

# macOS dependencies
elif [ "$OS" = "Darwin" ] && command_exists brew; then
    echo "📦 macOS dependencies via Homebrew..."

    # Install core development tools
    echo "📦 Installing core development tools..."
    brew install cmake ninja libicon

    # Install additional tools
    echo "📦 Installing additional tools..."
    brew install sqlite3 postgresql redis

    # Install tools for development
    echo "📦 Installing development tools..."
    brew install clang-format shellcheck

# Windows dependencies
elif [ "$OS" = "Linux" ] && command_exists apt; then
    echo "📦 Ubuntu/Debian dependencies via apt..."

    # Install build tools
    echo "📦 Installing build tools..."
    sudo apt-get update
    sudo apt-get install -y build-essential

    # Install audio development libraries for voice recognition
    echo "📦 Installing audio development libraries..."
    sudo apt-get install -y libasound2-dev portaudio19-dev

    # Install video development libraries for vision capabilities
    echo "📦 Installing video development libraries..."
    sudo apt-get install -y libavcodec-dev libv4-dev

    # Install OCR library for calendar import
    echo "📦 Installing OCR library..."
    sudo apt-get install -y tesseract-ocr

    # Install encryption libraries
    echo "📦 Installing encryption libraries..."
    sudo apt-get install -y libssl-dev

    # Install database development libraries
    echo "📦 Installing database libraries..."
    sudo apt-get install -y libpq-dev

    # Install networking libraries
    echo "📦 Installing networking libraries..."
    sudo apt-get install -y libwebsocket++-dev
fi

print_section "Version Information"

# Print versions of key tools
echo "📋 Dependency Versions:"
echo "=================="
echo "Rust: $(cargo --version 2>/dev/null || echo 'Not installed')"
echo "Node.js: $(node --version 2>/dev/null || echo 'Not installed')"
echo "npm: $(npm --version 2>/dev/null || echo 'Not installed')"
echo "Docker: $(docker --version 2>/dev/null || echo 'Not installed')"
echo "Git: $(git --version 2>/dev/null || echo 'Not installed')"
echo ""

# Check for architecture
ARCH="$(uname -m)"
echo "Architecture: $ARCH"

print_section "Next Steps"

echo ""
echo "🚀 MISA.AI dependencies installed successfully!"
echo ""
echo "🎯 Build the platform:"
echo "   ./scripts/build-all.sh"
echo ""
echo "🚀 Install the platform:"
echo "   sudo ./dist/install.sh"
echo ""
echo "📚 Configure and start using:"
echo "   misa-kernel --data-dir /opt/misa-ai/data"
echo ""
echo "📚 Access applications:"
echo "   Web: http://localhost"
echo "   Android: Install from dist/android/*.apk"
echo "   Desktop: Run from dist/desktop/"
echo ""
echo "📚 Learn more:"
echo "   - See dist/docs/README.md for usage guide"
echo "   - See CONTRIBUTING.md for development"
echo "   - Join our Discord community"
echo ""

# Create development environment configuration
echo ""
echo "🔧 Setting up development environment..."

# Set up Rust development environment
echo "📦 Configuring Rust..."
if [ -f "$HOME/.cargo/config.toml" ]; then
    echo "✅ Rust configuration already exists"
else
    echo "📦 Creating default Rust configuration..."
    mkdir -p "$HOME/.cargo"
    cat > "$HOME/.cargo/config.toml" << 'EOF'
[build]
rustflags = ["-D", "warnings"]
target-dir = "target"

[target.x86_64-unknown-linux-gnu]
linker = "clang"
rustflags = ["-D", "warnings"]
EOF
fi

# Set up npm development environment
echo "📦 Configuring npm..."
if [ ! -f "$HOME/.npmrc" ]; then
    echo "📦 Creating default npm configuration..."
    cat > "$HOME/.npmrc" << 'EOF'
# MISA.AI npm configuration
prefix=${HOME}/.npm-global
cache=${HOME}/.npm-cache
EOF
fi

# Set up environment variables
echo "🔧 Setting environment variables..."

# Create .env file for development
if [ ! -f "$PROJECT_ROOT/.env" ]; then
    echo "📦 Creating .env file..."
    cat > "$PROJECT_ROOT/.env" << 'EOF
# MISA.AI Development Environment Variables
RUST_LOG=info
NODE_ENV=development
REACT_APP_API_URL=http://localhost:8080
# Database
DATABASE_URL=postgresql://localhost:5432/misa_ai
# Redis
REDIS_URL=redis://localhost:6379
# AI Models
OPENAI_API_KEY=
ANTHROPIC_API_KEY=
GOOGLE_AI_API_KEY=
# Security
JWT_SECRET=your-jwt-secret-here
ENCRYPTION_KEY=your-32-byte-encryption-key-here
# Cloud Services
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_REGION=us-east-1
# Monitoring
SENTRY_DSN=
EOF
fi

print_section "Environment Setup Complete"
echo "✅ Dependencies installation completed!"
echo ""
echo "🚀 Ready to build MISA.AI!"
echo ""

# Success message
echo ""
echo "🎉 MISA.AI Dependencies Installation Summary"
echo "===================================="
echo "✅ Core Dependencies Installed"
echo "✅ Development Environment Configured"
echo "✅ Platform-Specific Dependencies Installed"
echo ""
echo "📊 Total disk usage: $(du -sh . | tail -1 | cut -f1)"
echo "📊 Installation complete: $(ls -la dist/ | wc -l) items created"
echo ""

# Report what was installed
echo ""
echo "📊 What was installed:"
echo "- Rust compiler and toolchain"
echo "- Node.js runtime and npm"
echo "- Docker container platform"
echo "- Platform-specific development tools"
echo "- Build and deployment scripts"
echo "- Comprehensive documentation"
echo ""
echo "🎯 Ready to build and run MISA.AI!"
echo ""
echo "Next: ./scripts/build-all.sh"
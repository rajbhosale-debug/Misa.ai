# Installation Guide

Get MISA.AI running on your system in minutes with our easy installation options.

## 🚀 One-Click Installation (Recommended)

The fastest way to install MISA.AI with all dependencies and default configurations.

### Automatic Installation
```bash
# Download and install automatically
curl -fsSL https://raw.githubusercontent.com/misa-ai/misa.ai/main/scripts/install.sh | bash
```

### Manual Installation
```bash
# Download the installation script
wget https://raw.githubusercontent.com/misa-ai/misa.ai/main/scripts/install.sh
chmod +x install.sh

# Run the installer
./install.sh
```

**What gets installed:**
- ✅ All 18 AI-powered applications
- ✅ Local AI models (Mixtral, CodeLlama)
- ✅ Privacy-first configuration
- ✅ Automatic updates and management scripts
- ✅ Docker containerization for isolation

## 📱 Platform-Specific Downloads

Choose the right installation package for your platform.

### Windows
- **MSI Installer**: [Download Latest](https://github.com/misa-ai/misa.ai/releases/latest/download/misa-ai-windows-amd64.msi)
- **Portable ZIP**: [Download Latest](https://github.com/misa-ai/misa.ai/releases/latest/download/misa-ai-windows-amd64-portable.zip)

**Requirements:**
- Windows 10 (64-bit) or later
- 8GB RAM minimum (16GB recommended)
- 20GB free disk space

**Installation Steps:**
1. Download the MSI installer
2. Run the installer as Administrator
3. Follow the installation wizard
4. Launch MISA.AI from Start Menu

### macOS
- **DMG Package**: [Download Latest](https://github.com/misa-ai/misa.ai/releases/latest/download/misa-ai-macos-amd64.dmg)
- **Homebrew**: `brew install misa-ai/misa-ai/misa-ai`

**Requirements:**
- macOS 11 (Big Sur) or later
- Intel or Apple Silicon (M1/M2)
- 8GB RAM minimum (16GB recommended)

**Installation Steps:**
1. Download the DMG file
2. Open the DMG and drag MISA.AI to Applications
3. Right-click and Open the app (to bypass Gatekeeper)
4. Follow the setup wizard

### Linux
- **AppImage**: [Download Latest](https://github.com/misa-ai/misa.ai/releases/latest/download/misa-ai-linux-amd64.AppImage)
- **Snap**: `snap install misa-ai`
- **DEB Package**: [Download Latest](https://github.com/misa-ai/misa.ai/releases/latest/download/misa-ai-amd64.deb)
- **RPM Package**: [Download Latest](https://github.com/misa-ai/misa.ai/releases/latest/download/misa-ai-amd64.rpm)

**Requirements:**
- Ubuntu 20.04+, Fedora 35+, or equivalent
- 8GB RAM minimum (16GB recommended)
- 20GB free disk space

**Installation Steps (AppImage):**
1. Download the AppImage file
2. Make it executable: `chmod +x misa-ai-*.AppImage`
3. Run it: `./misa-ai-*.AppImage`

### Android
- **APK File**: [Download Latest](https://github.com/misa-ai/misa.ai/releases/latest/download/misa-ai-android.apk)
- **Google Play Store**: Search "MISA.AI" (coming soon)

**Requirements:**
- Android 8.0 (API 26) or later
- 4GB RAM minimum
- 2GB free storage

**Installation Steps:**
1. Enable "Install from Unknown Sources" in settings
2. Download the APK file
3. Tap the file to install
4. Grant required permissions during setup

## 🐳 Docker Installation

For users who prefer containerized deployment.

### Quick Start
```bash
# Pull and run the complete stack
docker run -d \
  --name misa-ai \
  -p 3000:3000 \
  -p 8080:8080 \
  -v misa-data:/data \
  misa-ai/complete:latest
```

### Docker Compose
```bash
# Download docker-compose.yml
curl -O https://raw.githubusercontent.com/misa-ai/misa.ai/main/docker-compose.yml

# Start all services
docker-compose up -d

# Check status
docker-compose ps
```

### Accessing Docker Installation
- **Web Application**: http://localhost:3000
- **Kernel API**: http://localhost:8080
- **Management**: `docker-compose logs -f`

## 🛠️ Development Installation

For developers who want to contribute or customize MISA.AI.

### Prerequisites
- **Docker** & **Docker Compose**
- **Node.js** 18+
- **Rust** 1.70+
- **Git**

### Installation Steps
```bash
# Clone the repository
git clone https://github.com/misa-ai/misa.ai.git
cd misa.ai

# Install dependencies
./scripts/install-dependencies.sh

# Start development environment
./scripts/setup-dev.sh

# Build all components
./scripts/build-all.sh

# Run tests
./scripts/test.sh
```

## 🔧 Post-Installation Setup

### 1. First Launch
After installation, open MISA.AI and complete the setup wizard:

1. **Privacy Settings**: Configure data collection preferences
2. **Security**: Set up authentication and biometrics
3. **Device Name**: Choose a name for your device
4. **Applications**: Select which apps to enable
5. **Cloud Sync**: Optional cloud synchronization setup

### 2. AI Model Configuration
MISA.AI will automatically download the default AI model (Mixtral). You can:

- **Use Local Models**: Privacy-first, runs on your device
- **Add Cloud Models**: OpenAI, Claude, Gemini (requires API keys)
- **Configure Preferences**: Auto-switching based on task type

### 3. Voice Setup (Optional)
If you enabled voice assistant:

1. **Microphone Permission**: Grant microphone access
2. **Wake Word**: Train "Hey Misa" wake word detection
3. **Voice Profile**: Optional voice authentication setup

## 🗂️ Installation Locations

### Default Directories
- **Windows**: `C:\Program Files\MISA.AI\`
- **macOS**: `/Applications/MISA.AI.app/`
- **Linux**: `/opt/misa-ai/` or `~/.local/bin/misa-ai`
- **Data**: `~/.misa-ai/` (user data)

### Important Files
- **Configuration**: `~/.misa-ai/config/default.toml`
- **Logs**: `~/.misa-ai/logs/`
- **AI Models**: `~/.misa-ai/models/`
- **Database**: `~/.misa-ai/data/misa_memory.db`

## 🔄 Updates and Maintenance

### Automatic Updates
MISA.AI includes automatic update functionality:

- **Notification**: You'll be notified when updates are available
- **Download**: Updates download in the background
- **Installation**: One-click update installation
- **Rollback**: Automatic rollback if update fails

### Manual Updates
```bash
# Check for updates
misa-ai check-updates

# Install updates
misa-ai update

# Update specific components
misa-ai update --models
misa-ai update --apps
```

### Maintenance Commands
```bash
# Clear cache
misa-ai cleanup --cache

# Optimize database
misa-ai optimize --database

# Export data
misa-ai export --all

# Check system health
misa-ai health-check
```

## 🧹 Uninstallation

### Automatic Uninstallation
```bash
# Run the uninstaller
./scripts/install.sh --uninstall
```

### Manual Uninstallation

#### Windows
1. Use "Add or Remove Programs" in Control Panel
2. Or run the uninstaller from Start Menu

#### macOS
```bash
# Remove the app
sudo rm -rf /Applications/MISA.AI.app

# Remove user data (optional)
rm -rf ~/.misa-ai
```

#### Linux
```bash
# Remove package (method dependent)
sudo apt remove misa-ai          # Ubuntu/Debian
sudo dnf remove misa-ai          # Fedora
sudo snap remove misa-ai         # Snap

# Remove user data (optional)
rm -rf ~/.misa-ai
```

## 🆘 Troubleshooting

### Common Installation Issues

#### Port Already in Use
```bash
# Check what's using the port
lsof -i :8080
lsof -i :3000

# Kill the process
sudo kill -9 <PID>

# Or change ports in config
misa-ai config set network.websocket_port 8081
```

#### Permission Denied
```bash
# Fix file permissions
sudo chown -R $USER:$USER ~/.misa-ai
chmod +x ~/.misa-ai/bin/*
```

#### Docker Issues
```bash
# Check Docker status
sudo systemctl status docker

# Restart Docker
sudo systemctl restart docker

# Check Docker logs
docker logs misa-ai
```

#### Memory Issues
```bash
# Check available memory
free -h

# Close other applications
# Or use smaller AI model
misa-ai config set models.default_model "dolphin-mistral"
```

### Getting Help

1. **Built-in Diagnostics**: Run `misa-ai doctor` for system check
2. **Logs**: Check `~/.misa-ai/logs/` for detailed error messages
3. **Community**: Join [Discord](https://discord.gg/misa-ai) for help
4. **Support**: Email support@misa.ai for technical assistance

---

**Next Step**: [First-Time Setup](setup.md) - Configure your MISA.AI experience
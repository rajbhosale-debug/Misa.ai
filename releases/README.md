# MISA.AI Release Distribution

This directory contains pre-built distribution packages for MISA.AI across all supported platforms.

## 📦 Distribution Structure

```
releases/
├── windows/           # Windows distributions
│   ├── binaries/      # Windows executables
│   ├── packages/      # MSI installers, ZIP packages
│   └── docs/          # Windows-specific documentation
├── macos/             # macOS distributions
│   ├── binaries/      # macOS binaries
│   ├── packages/      # DMG packages, Homebrew bottles
│   └── docs/          # macOS-specific documentation
├── linux/             # Linux distributions
│   ├── binaries/      # Linux executables
│   ├── packages/      # AppImage, Snap, DEB, RPM packages
│   └── docs/          # Linux-specific documentation
├── android/           # Android distributions
│   ├── binaries/      # APK files
│   ├── packages/      # AAB bundles for Play Store
│   └── docs/          # Android-specific documentation
└── universal/         # Cross-platform distributions
    ├── docker/        # Docker images and manifests
    ├── source/        # Source code archives
    └── docs/          # General release documentation
```

## 🚀 Installation Methods

### Quick Install (Recommended)
```bash
curl -fsSL https://raw.githubusercontent.com/misa-ai/misa.ai/main/scripts/install.sh | bash
```

### Platform-Specific Downloads

#### Windows
- **MSI Installer**: `windows/packages/misa-ai-windows-amd64.msi`
- **Portable ZIP**: `windows/packages/misa-ai-windows-amd64-portable.zip`
- **Requirements**: Windows 10+ (64-bit)

#### macOS
- **DMG Package**: `macos/packages/misa-ai-macos-amd64.dmg`
- **Homebrew**: `brew install misa-ai/misa-ai/misa-ai`
- **Requirements**: macOS 11+ (Intel/Apple Silicon)

#### Linux
- **AppImage**: `linux/packages/misa-ai-linux-amd64.AppImage`
- **Snap**: `snap install misa-ai`
- **DEB/Ubuntu**: `linux/packages/misa-ai-amd64.deb`
- **RPM/Fedora**: `linux/packages/misa-ai-amd64.rpm`
- **Requirements**: Ubuntu 20.04+, Fedora 35+, or equivalent

#### Android
- **APK**: `android/binaries/misa-ai-android.apk`
- **Google Play**: Search "MISA.AI" on Play Store
- **Requirements**: Android 8.0+ (API 26+)

### Docker Distribution
```bash
# Pull and run complete stack
docker run -p 3000:3000 -p 8080:8080 misa-ai/complete:latest

# Or with docker-compose
docker-compose -f https://raw.githubusercontent.com/misa-ai/misa.ai/main/docker-compose.yml up
```

## 🔐 Security & Verification

All releases are:
- ✅ **Code signed** with official MISA.AI certificates
- ✅ **Checksum verified** with SHA-256 hashes
- ✅ **Virus scanned** before distribution
- ✅ **Reproducible builds** for transparency

### Verify Downloads
```bash
# Verify checksum
sha256sum -c misa-ai-v1.0.0.sha256

# Verify signature (GPG)
gpg --verify misa-ai-v1.0.0.sig misa-ai-v1.0.0.tar.gz
```

## 📋 Version Information

Current version and release notes are available at:
- [GitHub Releases](https://github.com/misa-ai/misa.ai/releases)
- [Release Notes](docs/release-notes.md)
- [Changelog](../CHANGELOG.md)

## 🔄 Update Process

MISA.AI includes automatic update functionality:
- **Web/Desktop**: Built-in updater with rollback support
- **Mobile**: App store updates or manual APK installation
- **Docker**: `docker-compose pull && docker-compose up -d`

## 🆘 Support & Documentation

- **User Guide**: [docs/user-guide/](../docs/user-guide/)
- **API Reference**: [docs/api-reference.md](../docs/api-reference.md)
- **Troubleshooting**: [docs/troubleshooting.md](../docs/troubleshooting.md)
- **Community**: [Discord Server](https://discord.gg/misa-ai)
- **Issues**: [GitHub Issues](https://github.com/misa-ai/misa.ai/issues)

## 📄 License

- **Personal Use**: MIT License - Free for personal and educational use
- **Commercial Use**: Requires commercial license
- **Enterprise**: Custom licensing and support available
- See [LICENSE](../LICENSE) for details
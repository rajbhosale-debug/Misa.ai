# MISA.AI - Quick Installation Guide

## 🚀 One-Command Installation

### Method 1: Direct Install (Recommended)
```bash
curl -fsSL https://install.misa.ai | bash
```

### Method 2: Manual Install
```bash
git clone https://github.com/misa-ai/misa.ai.git
cd misa.ai
./scripts/install.sh
```

## 📋 What Gets Installed

✅ **MISA.AI Kernel** - Rust-based orchestration engine
✅ **Ollama AI Models** - Local LLM support (Mixtral, CodeLlama, etc.)
✅ **Web Interface** - React-based dashboard at http://localhost:3000
✅ **Android Apps** - Calendar, Notes, Tasks, and 15 other applications
✅ **Docker Infrastructure** - 11-service production stack
✅ **Monitoring Stack** - Prometheus + Grafana + Jaeger
✅ **Security & Privacy** - Enterprise-grade encryption and consent management

## 🖥️ System Requirements

**Minimum:**
- 4GB RAM
- 2 CPU cores
- 10GB free disk space
- Docker & Docker Compose

**Recommended:**
- 8GB+ RAM
- 4+ CPU cores
- 20GB+ free disk space
- GPU support (for local AI acceleration)

## 📱 Access After Installation

| Service | URL | Description |
|---------|-----|-------------|
| **Web App** | http://localhost:3000 | Main interface |
| **API Docs** | http://localhost:8080/docs | Kernel API |
| **Monitoring** | http://localhost:3001 | Grafana dashboard |
| **Metrics** | http://localhost:9090 | Prometheus |

## 🔧 Management Commands

```bash
# Start services
~/.misa-ai/start.sh

# Stop services
~/.misa-ai/stop.sh

# Check status
~/.misa-ai/status.sh

# Update MISA.AI
~/.misa-ai/update.sh

# Uninstall completely
./scripts/install.sh --uninstall
```

## 📋 Installation Data Locations

```
~/.misa-ai/
├── config/          # Configuration files
├── data/            # Local data and models
├── logs/            # Application logs
├── docker-compose.yml  # Service definitions
└── .env             # Environment variables
```

## 🤖 First Time Setup

1. **Open Web Interface**: http://localhost:3000
2. **Complete Setup Wizard** (2 minutes)
3. **Configure AI Models** - Choose local vs cloud models
4. **Set Privacy Preferences** - Control data sharing
5. **Pair Mobile Devices** - Install Android app and scan QR code

## 📱 Mobile Setup

### Google Play Store
1. Search "MISA.AI"
2. Install and launch
3. Auto-discover desktop on same network
4. Scan QR code to pair

### APK Direct Download
1. Visit https://download.misa.ai on Android device
2. Download latest APK
3. Enable "Install from unknown sources"
4. Install and pair with desktop

## 🆘 Troubleshooting

### Port Already in Use
```bash
# Check what's using port 3000
lsof -i :3000
# Kill the process
kill -9 <PID>
```

### Docker Issues
```bash
# Restart Docker
sudo systemctl restart docker

# Check Docker status
sudo systemctl status docker
```

### Permission Issues
```bash
# Add user to docker group
sudo usermod -aG docker $USER
# Log out and log back in
```

### Out of Space
```bash
# Check disk usage
df -h

# Clean Docker
docker system prune -a
```

## 🔄 Updating MISA.AI

```bash
# Update all services
~/.misa-ai/update.sh

# Update specific models
docker exec -it misa-ollama ollama pull mixtral
```

## 📚 What You Can Do Immediately

After installation (5 minutes), you can:

- 🗣️ **Voice Commands**: "Hey Misa, what's my schedule today?"
- 📅 **Calendar Management**: "Add meeting with John at 2 PM tomorrow"
- 📝 **Note-Taking**: "Create a note about project ideas with voice"
- 🔄 **Task Automation**: "Remind me to follow up with Sarah tomorrow morning"
- 📱 **Multi-Device**: "Show me my desktop screen on phone"
- 🔍 **Smart Search**: "Find that email about the Q4 report"
- 🎵 **Media Control**: "Play my focus playlist on laptop"
- 🏠 **Home Control**: "Turn off living room lights"

## 🔒 Privacy & Security

- ✅ **Local-First Processing** - Data stays on your device by default
- ✅ **End-to-End Encryption** - All communications encrypted
- ✅ **Granular Controls** - Per-app and per-feature permissions
- ✅ **GDPR/CCPA Compliant** - Full data export and deletion tools
- ✅ **Audit Logging** - Complete audit trail of all actions
- ✅ **Biometric Auth** - Fingerprint/face recognition support

## 📞 Support & Community

- **Documentation**: https://docs.misa.ai
- **Community Discord**: https://discord.gg/misa-ai
- **GitHub Issues**: https://github.com/misa-ai/misa.ai/issues
- **Video Tutorials**: https://tutorials.misa.ai

---

## 🎉 Congratulations!

You now have a fully functional AI assistant that:
- Runs **locally** on your hardware for privacy
- Supports **18 integrated applications**
- Provides **voice-first interaction** with wake word detection
- Offers **remote desktop** and file transfer capabilities
- Includes **enterprise-grade security** and compliance features
- Works **seamlessly across all your devices**

**Welcome to MISA.AI - Your Privacy-First AI Assistant!** 🚀
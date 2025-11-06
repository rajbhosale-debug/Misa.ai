# First-Time Setup Guide

Complete the MISA.AI setup wizard to configure your intelligent assistant according to your preferences.

## 🚀 Starting the Setup Wizard

After installation, MISA.AI will automatically launch the setup wizard on first launch.

### Accessing the Setup Wizard
- **Automatic**: Launches automatically on first start
- **Manual**: Access from Settings → Setup Wizard
- **Browser**: Open http://localhost:3000 if not auto-redirected

The setup wizard consists of 6 steps and takes approximately 2-3 minutes to complete.

## 📋 Setup Steps Overview

### Step 1: Welcome & Privacy
Configure your privacy preferences and understand MISA.AI's privacy-first approach.

### Step 2: Security & Authentication
Set up security features including biometric authentication and session management.

### Step 3: Device Configuration
Configure device name and multi-device synchronization settings.

### Step 4: Applications & AI Models
Choose which applications to enable and select your preferred AI model.

### Step 5: Cloud Services (Optional)
Configure optional cloud synchronization and backup services.

### Step 6: Complete Setup
Review your configuration and start using MISA.AI.

## 🔐 Step 1: Welcome & Privacy

### Privacy Settings
MISA.AI is privacy-first by default. Choose your data sharing preferences:

#### Data Collection (Default: Disabled)
- **What**: Anonymous usage statistics to improve MISA.AI
- **Impact**: Helps us understand feature usage patterns
- **Recommendation**: Keep disabled for maximum privacy

#### Crash Reports (Default: Disabled)
- **What**: Automatic error reports when MISA.AI crashes
- **Impact**: Helps us fix bugs faster
- **Recommendation**: Enable if you want to help improve stability

#### Usage Analytics (Default: Disabled)
- **What**: Detailed interaction data with AI models
- **Impact**: Improves AI model performance and features
- **Recommendation**: Keep disabled for privacy-conscious users

### Privacy-First Guarantees
Regardless of your choices, MISA.AI always:
- ✅ Processes sensitive data locally on your device
- ✅ Uses end-to-end encryption for all communications
- ✅ Never sells or shares your personal data
- ✅ Allows you to export or delete your data anytime

## 🛡️ Step 2: Security & Authentication

### Authentication Methods
Configure how you want to secure access to MISA.AI.

#### Biometric Authentication (Recommended)
- **Fingerprint**: Use your fingerprint sensor
- **Face Recognition**: Windows Hello or Face ID
- **Voice Authentication**: Train voice profile (optional)

**Requirements**:
- Windows: Windows Hello compatible device
- macOS: Touch ID or Face ID compatible Mac
- Linux: Fingerprint sensor or libfprint support

#### Two-Factor Authentication (Optional)
Add an extra layer of security with 2FA:
- **Authenticator Apps**: Google Authenticator, Authy
- **Email Codes**: Receive codes via email
- **SMS**: Text message codes (less secure)

**Setup Process**:
1. Choose your preferred 2FA method
2. Scan QR code with authenticator app
3. Enter verification code
4. Save backup codes securely

#### Session Timeout
Configure how long MISA.AI stays unlocked:
- **15 minutes**: High security, frequent re-authentication
- **30 minutes**: Balanced (recommended)
- **1 hour**: Convenient, moderate security
- **2+ hours**: Less secure, very convenient

### Security Recommendations
- ✅ **Enable biometric authentication** for convenience and security
- ✅ **Set moderate session timeout** (30-60 minutes)
- ✅ **Consider 2FA** if handling sensitive information
- ✅ **Keep software updated** for latest security patches

## 📱 Step 3: Device Configuration

### Device Information
Set up your device identity for multi-device experiences.

#### Device Name
Choose a unique name for your device:
- **Format**: `[DeviceType]-[Identifier]` (e.g., "MacBook-Pro-ABC1")
- **Purpose**: Identifies this device in your MISA.AI network
- **Changeable**: Can be modified later in settings

#### Auto-Detection
MISA.AI automatically detects:
- **Platform**: Windows, macOS, or Linux
- **Browser**: Chrome, Firefox, Safari, or Edge
- **Hardware**: CPU, RAM, and GPU capabilities

### Multi-Device Features
Enable synchronization and remote control features.

#### Device Synchronization
- **What**: Sync your data across all your devices
- **How**: End-to-end encrypted synchronization
- **Benefits**: Seamless handoff between devices
- **Privacy**: Your data is encrypted before syncing

#### Remote Desktop
- **What**: Control this device from other paired devices
- **Security**: Requires explicit approval for each session
- **Features**: Screen sharing, file transfer, clipboard sync
- **Privacy**: Session recording toggle available

### Device Discovery
Scan for other MISA.AI devices on your network:
- **Automatic Discovery**: Find devices on same WiFi network
- **Manual Pairing**: Pair devices using QR codes or codes
- **Security**: Encrypted pairing process
- **Permissions**: Granular control over what each device can access

## 🎯 Step 4: Applications & AI Models

### AI Model Selection
Choose your default AI model based on your needs and hardware capabilities.

#### Available Models

##### Mixtral 8x7B (Recommended)
- **Size**: 26GB
- **Best For**: General purpose tasks, balanced performance
- **Requirements**: 8GB+ RAM, decent CPU
- **Speed**: Moderate response time

##### CodeLlama 34B
- **Size**: 64GB
- **Best For**: Coding, programming, technical tasks
- **Requirements**: 16GB+ RAM, fast CPU recommended
- **Speed**: Slower but more accurate for code

##### WizardCoder 15B
- **Size**: 30GB
- **Best For**: Code generation, debugging, explanations
- **Requirements**: 12GB+ RAM
- **Speed**: Good balance for coding tasks

##### Dolphin Mistral 7B
- **Size**: 14GB
- **Best For**: Quick responses, limited resources
- **Requirements**: 4GB+ RAM
- **Speed**: Fastest responses

#### Model Selection Tips
- **First-time users**: Start with Mixtral
- **Limited RAM**: Choose Dolphin Mistral
- **Developers**: Try CodeLlama or WizardCoder
- **Multiple models**: MISA.AI can auto-switch based on task

### Voice Assistant Configuration
Enable voice interaction with your AI assistant.

#### Voice Features
- **Wake Word**: "Hey Misa" activation
- **Voice Commands**: Natural language commands
- **Voice Authentication**: Optional voice-based security
- **Text-to-Speech**: Spoken responses

#### Setup Requirements
- **Microphone**: Built-in or external microphone
- **Permission**: Grant microphone access
- **Quiet Environment**: For wake word training
- **Network**: For cloud-based voice processing (optional)

### Application Selection
Choose which of the 18 integrated applications to enable.

#### Core Applications (Recommended)
- **Calendar**: AI scheduling with OCR import
- **Notes**: Rich text, voice, and handwriting
- **Tasks**: AI-powered task management
- **FileHub**: Unified file management with AI search

#### Productivity Applications
- **Focus**: Productivity tracking and sessions
- **WorkSuite**: Professional productivity tools
- **DevHub**: IDE integrations for developers

#### Communication Applications
- **ChatSync**: Multi-platform message integration
- **Meet**: Meeting recording and transcription
- **WebIQ**: Browser assistant

#### Advanced Applications
- **Persona Studio**: Avatar and voice customization
- **Home**: IoT device control and automation
- **Workflow AI**: Visual automation builder
- **Store**: Plugin marketplace
- **Vault**: Secure password management
- **BioLink**: Wearable data integration
- **Ambient Mode**: Contextual background assistance
- **PowerSense**: System monitoring and compute routing

### Resource Requirements
Consider your hardware capabilities when selecting applications:

#### Minimum Requirements
- **RAM**: 8GB (16GB recommended)
- **Storage**: 20GB free space
- **CPU**: 4+ cores recommended

#### Performance Tips
- **Fewer apps**: Better performance on limited hardware
- **Smaller models**: Faster responses on less powerful devices
- **SSD storage**: Improves model loading times
- **GPU acceleration**: Supported for NVIDIA and Apple Silicon

## ☁️ Step 5: Cloud Services (Optional)

Configure optional cloud synchronization and backup services.

### Cloud Synchronization
Choose your preferred cloud provider for data synchronization:

#### MISA Cloud Vault (Recommended)
- **Provider**: Official MISA.AI cloud service
- **Privacy**: End-to-end encrypted, zero-knowledge
- **Features**: Optimized for MISA.AI data
- **Cost**: Free tier available, paid plans for more storage

#### Third-Party Providers
- **Google Drive**: Use your existing Google account
- **OneDrive**: Use your Microsoft account
- **Dropbox**: Use your existing Dropbox account

**Note**: Third-party providers may have different privacy policies.

### Synchronization Settings
Configure how and when your data syncs:

#### Automatic Sync
- **What**: Automatically sync changes across devices
- **When**: Real-time or periodic based on settings
- **Conflict Resolution**: Automatic or manual resolution
- **Network**: WiFi only or include cellular data

#### Selective Sync
Choose what to synchronize:
- **Notes and Documents**: Text files, PDFs, images
- **Calendar Events**: Schedules and appointments
- **Tasks**: Task lists and projects
- **Settings**: Application preferences and configurations
- **AI Models**: Model configurations and fine-tunes

### Backup and Recovery
Configure automatic backups:

#### Local Backups
- **Location**: `~/.misa-ai/backups/`
- **Frequency**: Daily or weekly
- **Retention**: Keep last 7 backups by default
- **Encryption**: Same encryption as main data

#### Cloud Backups
- **Storage**: Your chosen cloud provider
- **Frequency**: Daily automatic backups
- **Versioning**: Keep multiple versions
- **Restore**: Point-in-time recovery available

## ✅ Step 6: Complete Setup

### Configuration Summary
Review your setup choices before completing:

#### Privacy Settings
- Data Collection: [Enabled/Disabled]
- Crash Reports: [Enabled/Disabled]
- Usage Analytics: [Enabled/Disabled]

#### Security Settings
- Biometric Authentication: [Enabled/Disabled]
- Two-Factor Authentication: [Enabled/Disabled]
- Session Timeout: [X minutes/hours]

#### Device Configuration
- Device Name: [Your Device Name]
- Sync Enabled: [Yes/No]
- Remote Desktop: [Enabled/Disabled]

#### Applications
- Enabled Apps: [X] of 18 applications
- Default Model: [Selected Model]
- Voice Assistant: [Enabled/Disabled]

#### Cloud Services
- Cloud Sync: [Enabled/Disabled]
- Provider: [Selected Provider]
- Auto Sync: [Enabled/Disabled]

### Finalizing Setup
Click "Start Using MISA.AI" to complete the setup and launch the main interface.

### What Happens Next
1. **Model Download**: Default AI model downloads in background
2. **Indexing**: Initial file and content indexing begins
3. **Tutorials**: Optional interactive tutorials available
4. **Welcome Tour**: Guided tour of the interface

## 🔧 Post-Setup Configuration

After completing the setup wizard, you can:

### Access Settings
- **Menu**: Click the gear icon (⚙️) in the top-right
- **Keyboard**: Ctrl+, (Windows/Linux) or Cmd+, (macOS)
- **Voice**: "Hey Misa, open settings"

### Customize Further
- **Themes**: Light, dark, or automatic themes
- **Shortcuts**: Configure keyboard shortcuts
- **Notifications**: Customize notification preferences
- **Privacy**: Adjust privacy settings anytime
- **Security**: Update security preferences

### Add More Devices
1. Install MISA.AI on additional devices
2. Sign in with the same account
3. Enable device synchronization
4. Pair devices using QR codes or pairing codes

## 🆘 Setup Troubleshooting

### Common Setup Issues

#### Model Download Fails
- **Check Internet**: Ensure stable internet connection
- **Storage Space**: Verify enough disk space available
- **Restart**: Restart MISA.AI and try again
- **Manual Download**: Download model from settings

#### Biometric Setup Fails
- **Hardware Check**: Verify biometric hardware works
- **Permissions**: Grant necessary system permissions
- **Drivers**: Update biometric device drivers
- **Alternative**: Use password authentication instead

#### Cloud Sync Issues
- **Credentials**: Verify cloud account credentials
- **Permissions**: Grant necessary API permissions
- **Network**: Check firewall and network settings
- **Storage**: Verify cloud storage has available space

#### Voice Assistant Not Working
- **Microphone**: Test microphone with other apps
- **Permissions**: Grant microphone permission
- **Background**: Allow background microphone access
- **Training**: Complete voice training in quiet environment

### Getting Help During Setup
- **Built-in Help**: Click the (?) icon in setup wizard
- **Diagnostic Tool**: Run `misa-ai doctor` in terminal
- **Community**: Join Discord for real-time help
- **Support**: Email support@misa.ai with setup logs

## 🎉 Congratulations!

You've successfully set up MISA.AI! Here's what to do next:

1. **Explore Applications**: Try different apps from the sidebar
2. **Voice Commands**: Say "Hey Misa, what can you do?"
3. **Customize**: Adjust settings to your preferences
4. **Install Mobile**: Get MISA.AI on your phone
5. **Join Community**: Connect with other users

**Enjoy your privacy-first intelligent assistant!** 🚀

---

**Next Step**: [Quick Tour](quick-tour.md) - Learn the MISA.AI interface
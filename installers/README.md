# MISA.AI Installers

This directory contains all installation packages and build scripts for MISA.AI across supported platforms.

## Directory Structure

### Windows Installers
- `msix/` - Modern MSIX package for Windows 10/11
- `msi/` - Traditional MSI installer for Windows 7+/Enterprise
- `resources/` - Common Windows installer resources (icons, images, license)
- `bundles/` - Dependency installers (Docker Desktop, Ollama, models)

### Android Distribution
- `signing/` - Release keystore and signing configuration
- `release/` - Signed APK output and distribution files

### Shared Components
- `scripts/` - Build automation and helper scripts
- `assets/` - Common resources across platforms

## Build Process

### Windows
```bash
# Build MSIX package
cd installers/windows/msix
msbuild MisaPackage.wxsproj /p:Configuration=Release

# Build MSI installer
cd installers/windows/msi
candle.exe *.wxs
light.exe *.wixobj -out Misa.ai.msi
```

### Android
```bash
# Build release APK
cd android
./gradlew assembleRelease

# Sign and copy to release directory
cd ../installers/android
./scripts/sign-apk.sh
```

## Automated Builds

All installers are built automatically via GitHub Actions when tags are pushed:
- `.github/workflows/build-windows.yml` - Windows installers
- `.github/workflows/build-android.yml` - Android APK
- `.github/workflows/release.yml` - Combined release

## Distribution

Final packages are distributed via:
- GitHub Releases (source of truth)
- `download.misa.ai` - Direct download server
- Windows Store (MSIX package)
- Direct APK download for Android

## Security

- Windows installers are code signed with Authenticode certificates
- Android APKs are signed with release keystore
- All packages have integrity verification
- Dependencies are bundled from official sources
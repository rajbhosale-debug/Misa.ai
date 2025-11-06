#!/bin/bash

# MISA.AI Android Release Build Script
# This script builds the release APK with proper signing

set -e

echo "Building MISA.AI Android release..."

# Set variables
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ANDROID_DIR="$PROJECT_ROOT/android"
SIGNING_DIR="$PROJECT_ROOT/installers/android/signing"
RELEASE_DIR="$PROJECT_ROOT/installers/android/release"
BUILD_DIR="$ANDROID_DIR/app/build/outputs/apk/release"

# Check prerequisites
echo "Checking prerequisites..."

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ]; then
    echo "Error: ANDROID_HOME not set. Please set it to your Android SDK path."
    exit 1
fi

if [ ! -d "$ANDROID_HOME" ]; then
    echo "Error: Android SDK not found at $ANDROID_HOME"
    exit 1
fi

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "Error: Java not found. Please install Java 17 or later."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "Error: Java 17 or later is required. Current version: $JAVA_VERSION"
    exit 1
fi

# Check if keystore exists
KEYSTORE_FILE="$SIGNING_DIR/release.keystore"
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "Error: Release keystore not found at $KEYSTORE_FILE"
    echo "Please run setup-signing.sh first to create the keystore."
    exit 1
fi

# Create release directory
mkdir -p "$RELEASE_DIR"

# Set environment variables for signing
if [ -f "$SIGNING_DIR/signing.properties" ]; then
    echo "Loading signing configuration..."
    source "$SIGNING_DIR/signing.properties"

    # Export for Gradle
    export KEYSTORE_PASSWORD
    export KEY_ALIAS
    export KEY_PASSWORD
else
    echo "Warning: signing.properties not found. Using environment variables."
fi

# Check if signing environment variables are set
if [ -z "$KEYSTORE_PASSWORD" ] || [ -z "$KEY_ALIAS" ] || [ -z "$KEY_PASSWORD" ]; then
    echo "Error: Signing environment variables not set."
    echo "Please set KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD."
    exit 1
fi

echo "Building release APK..."

# Navigate to Android project
cd "$ANDROID_DIR"

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean

# Build release APKs
echo "Building release APKs..."
./gradlew assembleRelease

if [ $? -ne 0 ]; then
    echo "Error: Gradle build failed."
    exit 1
fi

# Copy APKs to release directory
echo "Copying release APKs..."
find "$BUILD_DIR" -name "*.apk" -type f -exec cp {} "$RELEASE_DIR/" \;

# Verify APKs were created
APK_COUNT=$(find "$RELEASE_DIR" -name "*.apk" -type f | wc -l)
if [ "$APK_COUNT" -eq 0 ]; then
    echo "Error: No release APKs found."
    exit 1
fi

echo "Found $APK_COUNT release APK(s):"

# List and verify APKs
for apk in "$RELEASE_DIR"/*.apk; do
    if [ -f "$apk" ]; then
        APK_SIZE=$(stat -f%z "$apk" 2>/dev/null || stat -c%s "$apk" 2>/dev/null)
        APK_SIZE_MB=$((APK_SIZE / 1024 / 1024))

        echo "  $(basename "$apk") (${APK_SIZE_MB}MB)"

        # Verify APK signature
        if aapt dump badging "$apk" | grep -q "application-label"; then
            echo "    ✅ Valid APK"
        else
            echo "    ❌ Invalid APK"
            exit 1
        fi
    fi
done

# Generate release information
echo "Generating release information..."
RELEASE_INFO_FILE="$RELEASE_DIR/release-info.json"
cat > "$RELEASE_INFO_FILE" << EOF
{
  "version": "$(grep 'versionName' app/build.gradle.kts | cut -d'"' -f2 | head -1)",
  "versionCode": "$(grep 'versionCode' app/build.gradle.kts | awk '{print $3}')",
  "buildDate": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "commitHash": "$(git rev-parse HEAD 2>/dev/null || echo 'unknown')",
  "branch": "$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')",
  "buildType": "release",
  "apks": [
$(find "$RELEASE_DIR" -name "*.apk" -type f -exec basename {} \; | sed 's/.*/"&",/' | sed '$s/,$//')
  ]
}
EOF

# Generate checksums
echo "Generating checksums..."
CHECKSUMS_FILE="$RELEASE_DIR/checksums.txt"
cd "$RELEASE_DIR"
for apk in *.apk; do
    if [ -f "$apk" ]; then
        sha256sum "$apk" >> "$CHECKSUMS_FILE"
        md5sum "$apk" >> "$CHECKSUMS_FILE"
    fi
done

# Create zip archive for distribution
ARCHIVE_NAME="misa-android-release-$(date +%Y%m%d-%H%M%S).zip"
echo "Creating distribution archive: $ARCHIVE_NAME"
zip -r "$ARCHIVE_NAME" *.apk *.json *.txt 2>/dev/null

echo ""
echo "Release build completed successfully!"
echo ""
echo "Generated files:"
echo "  APKs: $RELEASE_DIR/*.apk"
echo "  Info: $RELEASE_INFO_FILE"
echo "  Checksums: $CHECKSUMS_FILE"
echo "  Archive: $RELEASE_DIR/$ARCHIVE_NAME"
echo ""
echo "APK sizes:"
for apk in "$RELEASE_DIR"/*.apk; do
    if [ -f "$apk" ]; then
        APK_SIZE=$(stat -f%z "$apk" 2>/dev/null || stat -c%s "$apk" 2>/dev/null)
        APK_SIZE_MB=$((APK_SIZE / 1024 / 1024))
        echo "  $(basename "$apk"): ${APK_SIZE_MB}MB"
    fi
done
echo ""
echo "To test the APKs:"
echo "  adb install \"$RELEASE_DIR/app-arm64-v8a-release.apk\""
echo ""
echo "To upload to distribution:"
echo "  scp $RELEASE_DIR/*.apk user@server:/path/to/downloads/"
echo ""
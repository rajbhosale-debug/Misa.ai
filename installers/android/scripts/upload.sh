#!/bin/bash

# MISA.AI Android Upload Script
# This script uploads release APKs to distribution server

set -e

echo "Uploading MISA.AI Android release APKs..."

# Set variables
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RELEASE_DIR="$PROJECT_ROOT/installers/android/release"

# Configuration (can be overridden by environment variables)
DOWNLOAD_SERVER="${DOWNLOAD_SERVER:-download.misa.ai}"
DOWNLOAD_PATH="${DOWNLOAD_PATH:-/var/www/misa.ai/releases/android}"
SSH_USER="${SSH_USER:-deploy}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/id_rsa}"

# Check prerequisites
echo "Checking prerequisites..."

# Check if release APKs exist
if [ ! -d "$RELEASE_DIR" ]; then
    echo "Error: Release directory not found: $RELEASE_DIR"
    echo "Please run release-build.sh first."
    exit 1
fi

APK_COUNT=$(find "$RELEASE_DIR" -name "*.apk" -type f | wc -l)
if [ "$APK_COUNT" -eq 0 ]; then
    echo "Error: No release APKs found in $RELEASE_DIR"
    exit 1
fi

echo "Found $APK_COUNT APK(s) to upload"

# Check SSH connectivity
if ! ssh -i "$SSH_KEY" -o ConnectTimeout=10 -o BatchMode=yes "$SSH_USER@$DOWNLOAD_SERVER" "echo 'Connection test successful'" 2>/dev/null; then
    echo "Error: Cannot connect to download server $DOWNLOAD_SERVER"
    echo "Please ensure:"
    echo "  - SSH server is accessible"
    echo "  - SSH key $SSH_KEY is valid"
    echo "  - User $SSH_USER has SSH access"
    exit 1
fi

# Create remote directory structure
echo "Setting up remote directories..."
ssh -i "$SSH_KEY" "$SSH_USER@$DOWNLOAD_SERVER" "mkdir -p $DOWNLOAD_PATH/{latest,archive}" || {
    echo "Error: Failed to create remote directories"
    exit 1
}

# Get version information
VERSION_INFO="$RELEASE_DIR/release-info.json"
if [ -f "$VERSION_INFO" ]; then
    VERSION=$(grep -o '"version": *"[^"]*"' "$VERSION_INFO" | cut -d'"' -f4)
    echo "Uploading version: $VERSION"
else
    VERSION="unknown"
    echo "Warning: Version info not found, using 'unknown'"
fi

# Create version-specific remote directory
REMOTE_VERSION_DIR="$DOWNLOAD_PATH/$VERSION"
ssh -i "$SSH_KEY" "$SSH_USER@$DOWNLOAD_SERVER" "mkdir -p $REMOTE_VERSION_DIR" || {
    echo "Error: Failed to create version directory"
    exit 1
}

# Upload APKs
echo "Uploading APKs..."
for apk in "$RELEASE_DIR"/*.apk; do
    if [ -f "$apk" ]; then
        apk_name=$(basename "$apk")
        echo "  Uploading $apk_name..."

        # Upload to version directory
        scp -i "$SSH_KEY" "$apk" "$SSH_USER@$DOWNLOAD_SERVER:$REMOTE_VERSION_DIR/" || {
            echo "Error: Failed to upload $apk_name"
            exit 1
        }

        # Also upload to latest directory (overwriting previous latest)
        if [[ "$apk_name" =~ app-.*-release\.apk ]]; then
            scp -i "$SSH_KEY" "$apk" "$SSH_USER@$DOWNLOAD_SERVER:$DOWNLOAD_PATH/latest/misa-latest.apk" || {
                echo "Warning: Failed to update latest APK"
            }
        fi

        echo "    ✅ Uploaded $apk_name"
    fi
done

# Upload supporting files
echo "Uploading supporting files..."

# Upload release info
if [ -f "$VERSION_INFO" ]; then
    scp -i "$SSH_KEY" "$VERSION_INFO" "$SSH_USER@$DOWNLOAD_SERVER:$REMOTE_VERSION_DIR/release-info.json" || {
        echo "Warning: Failed to upload release info"
    }
fi

# Upload checksums
if [ -f "$RELEASE_DIR/checksums.txt" ]; then
    scp -i "$SSH_KEY" "$RELEASE_DIR/checksums.txt" "$SSH_USER@$DOWNLOAD_SERVER:$REMOTE_VERSION_DIR/" || {
        echo "Warning: Failed to upload checksums"
    }
fi

# Upload distribution archive if exists
for archive in "$RELEASE_DIR"/*.zip; do
    if [ -f "$archive" ]; then
        archive_name=$(basename "$archive")
        scp -i "$SSH_KEY" "$archive" "$SSH_USER@$DOWNLOAD_SERVER:$REMOTE_VERSION_DIR/" || {
            echo "Warning: Failed to upload archive $archive_name"
        }
    fi
done

# Generate download page
echo "Generating download page..."
DOWNLOAD_PAGE="$REMOTE_VERSION_DIR/index.html"
cat > "/tmp/download-page.html" << EOF
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MISA.AI Android Release $VERSION</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }
        .header { text-align: center; margin-bottom: 40px; }
        .download-section { margin: 20px 0; padding: 20px; border: 1px solid #ddd; border-radius: 8px; }
        .download-btn { display: inline-block; background: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin: 10px 0; }
        .download-btn:hover { background: #0056b3; }
        .info { background: #f8f9fa; padding: 15px; border-radius: 6px; margin: 10px 0; }
        .warning { background: #fff3cd; color: #856404; padding: 15px; border-radius: 6px; margin: 10px 0; border: 1px solid #ffeaa7; }
    </style>
</head>
<body>
    <div class="header">
        <h1>MISA.AI Android</h1>
        <h2>Version $VERSION</h2>
        <p>Privacy-First AI Assistant for Android</p>
    </div>

    <div class="warning">
        <strong>⚠️ Installation Note:</strong> You may need to enable "Install from unknown sources" in your Android settings to install this APK.
    </div>

    <div class="download-section">
        <h3>📱 Download APKs</h3>
        <p>Choose the appropriate APK for your device architecture:</p>
EOF

# Add download links for APKs
for apk in "$RELEASE_DIR"/*.apk; do
    if [ -f "$apk" ]; then
        apk_name=$(basename "$apk")
        apk_size=$(stat -f%z "$apk" 2>/dev/null || stat -c%s "$apk" 2>/dev/null)
        apk_size_mb=$((apk_size / 1024 / 1024))

        # Determine architecture from filename
        if [[ "$apk_name" =~ arm64 ]]; then
            arch="ARM64 (recommended for most modern devices)"
        elif [[ "$apk_name" =~ armeabi ]]; then
            arch="ARM (for older devices)"
        else
            arch="Universal"
        fi

        cat >> "/tmp/download-page.html" << EOF
        <div class="download-btn">
            <a href="$apk_name" download>
                📥 Download $apk_name ($arch) - ${apk_size_mb}MB
            </a>
        </div>
EOF
    fi
done

cat >> "/tmp/download-page.html" << EOF
    </div>

    <div class="download-section">
        <h3>📋 Installation Instructions</h3>
        <ol>
            <li>Download the appropriate APK for your device</li>
            <li>Enable "Install from unknown sources" in Android settings</li>
            <li>Tap the downloaded APK to install</li>
            <li>Grant required permissions when prompted</li>
            <li>Launch MISA.AI and complete the setup wizard</li>
        </ol>
    </div>

    <div class="info">
        <h3>🔒 Security</h3>
        <p>All APKs are cryptographically signed by MisaAI Team. You can verify the integrity using the provided checksums.</p>
        <p><a href="checksums.txt">View Checksums</a> | <a href="release-info.json">Build Information</a></p>
    </div>

    <div class="info">
        <h3>📖 Support</h3>
        <p>Need help? Visit our <a href="https://support.misa.ai">support center</a> or <a href="https://github.com/misa-ai/misa.ai/issues">report issues</a>.</p>
    </div>

    <footer style="text-align: center; margin-top: 40px; color: #666;">
        <p>&copy; 2024 MisaAI Team. All rights reserved.</p>
    </footer>
</body>
</html>
EOF

scp -i "$SSH_KEY" "/tmp/download-page.html" "$SSH_USER@$DOWNLOAD_SERVER:$DOWNLOAD_PAGE" || {
    echo "Warning: Failed to upload download page"
}

# Update latest version symlink
echo "Updating version links..."
ssh -i "$SSH_KEY" "$SSH_USER@$DOWNLOAD_SERVER" "cd $DOWNLOAD_PATH && rm -f latest-version && ln -sf $VERSION latest-version" || {
    echo "Warning: Failed to update latest version link"
}

# Update server index if needed
UPDATE_SERVER_SCRIPT=$(cat << 'EOF'
#!/bin/bash
cd "$DOWNLOAD_PATH"

# Create main index.html with link to latest
cat > index.html << 'HTML'
<!DOCTYPE html>
<html>
<head>
    <title>MISA.AI Android Downloads</title>
    <meta http-equiv="refresh" content="0; url=latest/">
</head>
<body>
    <h1>Redirecting to latest release...</h1>
    <p>If you are not redirected, <a href="latest/">click here</a>.</p>
</body>
</html>
HTML

# Create latest/index.html redirect
if [ -L "latest-version" ]; then
    LATEST_VERSION=$(readlink latest-version)
    cat > latest/index.html << HTML
<!DOCTYPE html>
<html>
<head>
    <title>Latest MISA.AI Android Release</title>
    <meta http-equiv="refresh" content="0; url=$LATEST_VERSION/">
</head>
<body>
    <h1>Redirecting to latest release...</h1>
    <p>If you are not redirected, <a href="$LATEST_VERSION/">click here</a>.</p>
</body>
</html>
HTML
fi
EOF
)

ssh -i "$SSH_KEY" "$SSH_USER@$DOWNLOAD_SERVER" "DOWNLOAD_PATH=$DOWNLOAD_PATH; bash -c '$UPDATE_SERVER_SCRIPT'" || {
    echo "Warning: Failed to update server index"
}

echo ""
echo "Upload completed successfully!"
echo ""
echo "Uploaded files:"
echo "  APKs: https://$DOWNLOAD_SERVER/releases/android/$VERSION/"
echo "  Latest: https://$DOWNLOAD_SERVER/releases/android/latest/"
echo "  Main: https://$DOWNLOAD_SERVER/releases/android/"
echo ""
echo "Direct download links:"
echo "  Latest APK: https://$DOWNLOAD_SERVER/releases/android/latest/misa-latest.apk"
echo ""

# Show uploaded files
echo "Uploaded files:"
ssh -i "$SSH_KEY" "$SSH_USER@$DOWNLOAD_SERVER" "ls -la $REMOTE_VERSION_DIR" 2>/dev/null || {
    echo "Could not list remote files"
}

echo ""
echo "Distribution ready! 🎉"
echo "Share the link: https://$DOWNLOAD_SERVER/releases/android/"
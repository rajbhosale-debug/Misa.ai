#!/bin/bash

# MISA.AI Android Release Signing Setup Script
# This script creates the production signing keystore for Android releases

set -e

echo "Setting up MISA.AI Android release signing..."

# Set variables
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SIGNING_DIR="$(dirname "$SCRIPT_DIR")/signing"
KEYSTORE_FILE="$SIGNING_DIR/release.keystore"
PROPERTIES_FILE="$SIGNING_DIR/signing.properties"

# Create signing directory
mkdir -p "$SIGNING_DIR"

# Check if keystore already exists
if [ -f "$KEYSTORE_FILE" ]; then
    echo "Keystore already exists: $KEYSTORE_FILE"
    read -p "Do you want to regenerate it? This will invalidate any existing signed APKs. (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Using existing keystore."
        exit 0
    fi
fi

# Get keystore information
echo "Please enter keystore information (press Enter for defaults):"

read -p "Keystore password (default: misa2024): " KEYSTORE_PASSWORD
KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD:-misa2024}

read -p "Key alias (default: misa-release): " KEY_ALIAS
KEY_ALIAS=${KEY_ALIAS:-misa-release}

read -p "Key password (default: same as keystore): " KEY_PASSWORD
KEY_PASSWORD=${KEY_PASSWORD:-$KEYSTORE_PASSWORD}

read -p "Your name (default: MisaAI Team): " YOUR_NAME
YOUR_NAME=${YOUR_NAME:-MisaAI Team}

read -p "Organizational unit (default: Development): " ORG_UNIT
ORG_UNIT=${ORG_UNIT:-Development}

read -p "Organization (default: MisaAI): " ORGANIZATION
ORGANIZATION=${ORGANIZATION:-MisaAI}

read -p "City (default: San Francisco): " CITY
CITY=${CITY:-San Francisco}

read -p "State (default: California): " STATE
STATE=${STATE:-California}

read -p "Country code (default: US): " COUNTRY
COUNTRY=${COUNTRY:-US}

echo ""
echo "Generating keystore with the following information:"
echo "  Organization: $ORGANIZATION"
echo "  Location: $CITY, $STATE, $COUNTRY"
echo "  Keystore: $KEYSTORE_FILE"
echo "  Key Alias: $KEY_ALIAS"
echo ""

read -p "Continue? (Y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Nn]$ ]]; then
    echo "Cancelled."
    exit 1
fi

# Generate the keystore
echo "Generating keystore..."
keytool -genkeypair \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=$YOUR_NAME, OU=$ORG_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE, C=$COUNTRY"

if [ $? -eq 0 ]; then
    echo "Keystore generated successfully!"
else
    echo "Failed to generate keystore."
    exit 1
fi

# Create signing properties file
cat > "$PROPERTIES_FILE" << EOF
# MISA.AI Android Release Signing Configuration
# Generated on $(date)

# Keystore information
storeFile=release.keystore
storePassword=$KEYSTORE_PASSWORD
keyAlias=$KEY_ALIAS
keyPassword=$KEY_PASSWORD

# Build system environment variables
# Set these in your CI/CD system:
# export KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD
# export KEY_ALIAS=$KEY_ALIAS
# export KEY_PASSWORD=$KEY_PASSWORD
EOF

echo "Signing configuration saved to: $PROPERTIES_FILE"

# Set file permissions
chmod 600 "$KEYSTORE_FILE"
chmod 644 "$PROPERTIES_FILE"

echo ""
echo "Setup completed successfully!"
echo ""
echo "For manual builds, set these environment variables:"
echo "  export KEYSTORE_PASSWORD=\"$KEYSTORE_PASSWORD\""
echo "  export KEY_ALIAS=\"$KEY_ALIAS\""
echo "  export KEY_PASSWORD=\"$KEY_PASSWORD\""
echo ""
echo "For automated builds, add these secrets to your CI/CD system:"
echo "  - KEYSTORE_PASSWORD: $KEYSTORE_PASSWORD"
echo "  - KEY_ALIAS: $KEY_ALIAS"
echo "  - KEY_PASSWORD: $KEY_PASSWORD"
echo "  - KEYSTORE_BASE64: $(base64 -w 0 "$KEYSTORE_FILE")"
echo ""
echo "IMPORTANT: Keep the keystore file and passwords secure!"
echo "           Store the keystore in a secure location and backup it safely."
echo ""

# Show keystore information
echo "Keystore details:"
keytool -list -v -keystore "$KEYSTORE_FILE" -alias "$KEY_ALIAS" -storepass "$KEYSTORE_PASSWORD"
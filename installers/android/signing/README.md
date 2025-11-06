# Android Release Signing Configuration

This directory contains the signing configuration for MISA.AI Android releases.

## Security Notice

⚠️ **IMPORTANT**: This directory contains sensitive cryptographic material.
- Keep the keystore file secure and never commit it to version control
- Store the keystore in a secure location with proper backups
- Use strong, unique passwords
- Limit access to only authorized personnel

## Files

- `release.keystore` - Production signing keystore (DO NOT COMMIT)
- `signing.properties` - Signing configuration (safe to commit)
- `keystore.backup` - Encrypted backup of keystore (if created)

## Setup

### Initial Setup

Run the setup script to create the production keystore:

```bash
./scripts/setup-signing.sh
```

This will:
1. Generate a new RSA 2048-bit keystore
2. Create signing configuration
3. Set proper file permissions
4. Display environment variables for CI/CD

### Environment Variables

For automated builds, set these environment variables:

```bash
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="your_key_alias"
export KEY_PASSWORD="your_key_password"
export KEYSTORE_BASE64="$(base64 -w 0 signing/release.keystore)"
```

### CI/CD Integration

#### GitHub Actions

Add these secrets to your GitHub repository:
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `KEYSTORE_BASE64`

#### GitLab CI/CD

Add these CI/CD variables:
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `KEYSTORE_BASE64`

## Usage

### Manual Build

```bash
cd android
# Set environment variables
export KEYSTORE_PASSWORD="your_password"
export KEY_ALIAS="your_alias"
export KEY_PASSWORD="your_key_password"

# Build release APKs
./gradlew assembleRelease
```

### Automated Build

```bash
# Use the release build script
./scripts/release-build.sh
```

## Security Best Practices

### Keystore Management

1. **Password Security**
   - Use strong, unique passwords
   - Store passwords in secure secret management
   - Never hard-code passwords in scripts

2. **File Protection**
   - Set restrictive file permissions (600 for keystore)
   - Store keystore in encrypted filesystem if possible
   - Regularly backup to secure location

3. **Access Control**
   - Limit access to keystore to necessary personnel only
   - Use access logs to monitor keystore access
   - Rotate keys periodically (though not frequently for app signing)

### Backup Strategy

1. **Primary Backup**
   - Store keystore in secure, off-site location
   - Use encrypted storage
   - Test backup restoration regularly

2. **Recovery Plan**
   - Document keystore recovery process
   - Store passwords in separate secure location
   - Have multiple authorized personnel with access

3. **Contingency**
   - Consider using Google Play App Signing for additional security
   - Keep historical versions for app updates
   - Document key rotation procedures

## Troubleshooting

### Common Issues

1. **Keystore Password Incorrect**
   ```
   ERROR: JAR signer unable to process keystore: keystore password was incorrect
   ```
   Solution: Verify password in signing.properties or environment variables

2. **Key Alias Not Found**
   ```
   ERROR: JAR signer unable to process keystore: key not found
   ```
   Solution: Check key alias matches what's in the keystore

3. **Keystore Corrupted**
   ```
   ERROR: JAR signer unable to process keystore: keystore was tampered with, or password was incorrect
   ```
   Solution: Restore from backup or generate new keystore (requires app republishing)

### Keystore Verification

List contents of keystore:
```bash
keytool -list -v -keystore release.keystore -alias your-key-alias
```

Verify APK signature:
```bash
jarsigner -verify -verbose -certs your-app.apk
```

## Migration

If you need to migrate to a new keystore:

1. Generate new keystore following setup process
2. Update app signing in Google Play Console
3. Upload new APK signed with new keystore
4. Backup old keystore for legacy app updates
5. Update CI/CD with new credentials

## Compliance

- Ensure keystore management complies with your organization's security policies
- Follow mobile app security best practices
- Regular security audits of keystore access and management
- Document key lifecycle management procedures

## Support

For signing-related issues:
1. Check Android documentation for app signing
2. Verify keystore integrity and permissions
3. Review build logs for specific error messages
4. Contact security team for keystore access issues
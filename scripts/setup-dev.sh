#!/bin/bash

# Setup Development Environment for MISA.AI
# This script configures the development environment for all components

set -e

echo "🚀 Setting up MISA.AI Development Environment"
echo "===================================="

# Function to check if directory exists
check_dir() {
    if [ ! -d "$1" ]; then
        echo "Creating directory: $1"
        mkdir -p "$1"
    fi
}

# Function to create symbolic link
create_symlink() {
    if [ -L "$1" ] && [ -e "$2" ]; then
        echo "Creating symbolic link: $1 -> $2"
        ln -sf "$2" "$1"
    elif [ -d "$2" ] && [ ! -e "$1" ]; then
        echo "Creating symbolic link: $1 -> $2"
        ln -s "$2" "$1"
    fi
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if file exists
check_file() {
    if [ -f "$1" ]; then
        return 0
    else
        return 1
    fi
}

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/.."

echo "Project Root: ${PROJECT_ROOT}"

# Check if we're in the right directory
if [ ! -f "$PROJECT_ROOT/core/Cargo.toml" ]; then
    echo "❌ Error: Not in MISA.AI project root directory"
    echo "Please run this script from the project root directory"
    exit 1
fi

# Change to project root
cd "$PROJECT_ROOT"

# Create development directory structure
print_section "Creating Development Directory Structure"

check_dir "dev-data"
check_dir "dev-configs"
check_dir "dev-logs"

# Create development data directory
echo "📁 Creating development data directory..."
mkdir -p dev-data/logs
mkdir -p dev-data/ocr
mkdir -p dev-data/cache

# Create development configuration directory
echo "📝 Creating development configuration files..."

# Rust configuration
echo "🦄 Configuring Rust for development..."
if [ ! -f "$HOME/.cargo/config.toml" ]; then
    echo "📦 Creating default Rust configuration..."
    mkdir -p "$HOME/.cargo"
    cat > "$HOME/.cargo/config.toml" << 'EOF'
[build]
rustflags = ["-D", "warnings"]
target-dir = "target"

[target.x86_64-unknown-linux-gnu]
linker = "clang"
rustflags = ["-D", "warnings"]

[profile.dev]
debug = true
incremental = true
opt-level = 0

[profile.release]
debug = false
incremental = false
opt-level = 3

[target.x86_64-unknown-linux-gnu]
linker = "clang"
rustflags = ["-D", "warnings"]
EOF
fi

# Node.js configuration
echo "📄 Configuring Node.js for development..."
if [ ! -f "$PROJECT_ROOT/shared/node_modules" ] && [ ! -d "$PROJECT_ROOT/web/node_modules" ]; then
    echo "📦 Linking shared node_modules to web/"
    cd web
    rm -rf node_modules
    ln -sf ../shared/node_modules web/node_modules
    cd ..
fi

# Android configuration
echo "📄 Configuring Android for development..."
if [ ! -f "$PROJECT_ROOT/android/app/build.gradle" ]; then
    echo "✅ Android already configured"
else
    echo "✅ Android build configuration found"
fi

# Desktop configuration
echo "📄 Configuring Desktop for development..."
if [ ! -f "$PROJECT_ROOT/desktop/src-tauri/Cargo.toml" ]; then
    echo "✅ Desktop already configured"
else
    echo "✅ Desktop build configuration found"
fi

# Web configuration
echo "📄 Configuring Web for development..."
if [ ! -f "$PROJECT_ROOT/web/vite.config.ts" ]; then
    echo "✅ Web already configured"
else
    echo "✅ Web build configuration found"
fi

# Create development configuration files
echo "📄 Creating development configuration..."

# Environment variables
if [ ! -f "$PROJECT_ROOT/.env.dev" ]; then
    echo "📄 Creating .env.development..."
    cat > "$PROJECT_ROOT/.env.dev" << 'EOF'
# MISA.AI Development Environment Variables
NODE_ENV=development
REACT_APP_API_URL=http://localhost:5173
MISA_KERNEL_URL=http://localhost:8080

# Database
DATABASE_URL=postgresql://localhost:5432/misa_ai_dev
REDIS_URL=redis://localhost:6379

# AI Models (empty - users add their keys)
OPENAI_API_KEY=
ANTHROPIC_API_KEY=
GOOGLE_AI_API_KEY=

# Security
JWT_SECRET=dev-jwt-secret-key-change-in-production
ENCRYPTION_KEY=dev-encryption-key-32-bytes-here

# Cloud Services (empty - users add their keys)
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_REGION=us-east-1

# Monitoring
SENTRY_DSN=

# Development specific
MISA_LOG_LEVEL=debug
MISA_DEV_MODE=true
MISA_DATA_DIR=${PROJECT_ROOT}/dev-data
EOF
fi

# Android development configuration
if [ ! -f "$PROJECT_ROOT/android/gradle.properties" ]; then
    echo "📄 Creating Android gradle.properties..."
    cat > "$PROJECT_ROOT/android/gradle.properties" << 'EOF
# MISA.AI Development Configuration
org.gradle.jvmargs=-Xmx4G -XX:MaxPermSize=512m
org.gradle.configureondemand=true
org.gradle.caching=true
org.gradle.parallel=true

android {
    compileSdkVersion = 34
    buildToolsVersion = "34.0.0"
    ndkVersion = "34"

    defaultConfig {
        applicationId "com.misa.dev"
        versionName "1.0.0-dev"
        minSdkVersion 24
        targetSdkVersion 34
        versionCode 1

        testInstrumentationRunner "androidx.test.InstrumentationRunner"
        testInstrumentationArguments {
            includeAndroidX=true
            includeJunit=true
        }

        debuggable {
            debuggable true
            renderscriptOptimization = true
        }
    }
}
EOF
fi

# Create development scripts
echo "📄 Creating development scripts..."

# Make scripts executable
echo "📔 Making development scripts executable..."
chmod +x scripts/*.sh

# Create log directory for development
echo "📄 Creating development log directory..."
mkdir -p dev-data/logs

# Create development SSL certificates (self-signed for development)
echo "📄 Creating development SSL certificates..."
mkdir -p dev-data/ssl

# Create example environment file
if [ ! -f "$PROJECT_ROOT/.env.example" ]; then
    echo "📄 Creating .env.example..."
    cat > "$PROJECT_ROOT/.env.example" << 'EOF'
# MISA.AI Environment Variables Template
NODE_ENV=production
REACT_APP_API_URL=https://api.misa.ai
MISA_KERNEL_URL=https://kernel.misa.ai

# Database
DATABASE_URL=postgresql://user:password@localhost:5432/misa_ai
REDIS_URL=redis://localhost:6379

# AI Models
OPENAI_API_KEY=your_openai_api_key_here
ANTHROPIC_API_KEY=your_anthropic_api_key_here
GOOGLE_AI_API_KEY=your_gemini_api_key_here

# Security
JWT_SECRET=your-production-jwt-secret
ENCRYPTION_KEY=your-production-encryption-key-32-bytes-here

# Cloud Services
AWS_ACCESS_KEY=your_aws_access_key_here
AWS_SECRET_KEY=your_aws_secret_key_here
AWS_REGION=us-east-1

# Monitoring
SENTRY_DSN=your_sentry_dsn
EOF
fi

# Create build configuration
echo "📄 Creating build configuration..."

# Rust development profile
echo "📄 Configuring Rust development profile..."
if ! grep -q "profile.dev" "$HOME/.cargo/config.toml"; then
    echo "📦 Adding dev profile to Cargo.toml..."
    cat >> "$HOME/.cargo/config.toml" << 'EOF'

[profile.dev]
debug = true
incremental = true
opt-level = 0

[profile.dev.build]
debug = true
EOF
fi

# Node.js development configuration
echo "📦 Configuring Node.js development profile..."
cat > "$PROJECT_ROOT/shared/vite.config.ts" << 'EOF
import { defineConfig } from 'vite';

export default build = defineConfig({
  plugins: [react()],
  optimizeDeps: buildVite,
  resolve: {
    alias: {
      '@': path.resolve(__dirname, '..', 'shared/src'),
      '@shared': path.resolve(__dirname, '..', 'shared/src'),
      '@android': path.resolve(__dirname, '..', 'android/app/src/main/java/com/misa'),
      '@core': path.resolve(__dirname '..', 'core/src'),
    }
  },
  server: {
    port: 5173,
    strictPort: true,
    host: true,
    hmr: {
      overlay: true
      overlayPort: 5174
    },
  },
  build: {
    outDir: "dist",
    sourcemap: true,
    rollupOptions: {
      sourcemap: true,
      minify: true,
      sourcemapRoot: "../"
    }
  }
});

export default export default
EOF
fi

# Android development configuration
echo "📄 Configuring Android development profile..."
if ! grep -q "signingConfig" "$PROJECT_ROOT/android/app/build.gradle" ]; then
    echo "📦 Adding Android release signing configuration..."
    cat >> "$PROJECT_ROOT/android/app/build.gradle" << 'EOF'

android {
    signingConfigs {
        debug {
            storeFile "../app/keystore/debug.keystore"
            keyAlias "key0"
            keyPassword "android"
            keyStorePassword "android"
            storeType JKS
            v1SigningEnabled true
            v2SigningEnabled true
        }

        release {
            storeFile "../app/keystore/release.keystore"
            keyAlias "key0"
            keyPassword "android"
            keyStorePassword "android"
            storeType JKS
            v1SigningEnabled true
            v2SigningEnabled true
        }
    }
}
EOF
fi

# Desktop development configuration
echo "📄 Configuring Desktop development profile..."
if ! grep -q "tauri.build" "$PROJECT_ROOT/desktop/src-tauri/Cargo.toml" ]; then
    echo "📄 Adding Desktop development configuration..."
    cat >> "$PROJECT_ROOT/desktop/src-tauri/Cargo.toml" << 'EOF'

[build]
target-dir = "dist"
cargo = ["relpath", "../core", "../shared"]

[target.x86_64-unknown-linux-gnu]
linker = "clang"
runner = "cargo"

[target.x86_64-unknown-linux-gnu]
rustflags = ["-D", "warnings"]
EOF
fi

# Create logo and branding assets
echo "🎨 Creating assets directory..."
mkdir -p assets/icons
mkdir -p assets/images

# Create .editorconfig for consistent editor setup
echo "⚙️ Creating .editorconfig for code editors..."
cat > "$PROJECT_ROOT/.editorconfig" << 'EOF
# MISA.AI Editor Configuration
root: true
editor:
  preferred: "vscode"
  settings:
    "editor.formatOnSave": true
    "editor.guides.enable": true
    "editor.minimap.maxFileSize": 1000000

    "[typescript]": {
      "editor.defaultFormatter": "esben-eslint",
      "editor.formatOnSave": true,
      "editor.codeActionsOnSave": ["source.fixAll", "source.organizeImports"]
    }

    "[javascript]": {
      "editor.defaultFormatter": "prettier",
      "editor.formatOnSave": true,
      "editor.codeActionsOnSave": ["source.fixAll", "source.organizeImports"]
    }

    "[rust]": {
      "editor.defaultFormatter": "rustfmt",
      "editor.formatOnSave": true
    }

    "[json]": {
      "editor.defaultFormatter": "prettier",
      "editor.formatOnSave": true
      "editor.snippets": {
        "javascript": {
          "prefix": "console.",
          "body": [
            "console.log();",
            "console.error();",
            "console.info();",
            "console.warn();",
            "console.debug();"
          ]
        }
      }
    }
    }

    "[markdown]": {
      "editor.defaultFormatter": "prettier",
      "editor.formatOnSave": true
      "editor.quickSuggestions": true
    }

    "[yaml]": {
      "editor.defaultFormatter": "prettier",
      "editor.formatOnSave": true
    }

    "[toml]": {
      "editor.defaultFormatter": "prettier",
      "editor.formatOnSave": true
    }

    "[dockerfile]": {
      "editor.defaultFormatter": "prettier",
      "editor.formatOnSave": true
    }
}
EOF
fi

print_section "Environment Setup Complete!"
echo "✅ Development environment configured!"
echo ""
echo "📁 Environment variables:"
echo "  MISA_LOG_LEVEL: ${MISA_LOG_LEVEL:-debug}"
echo "  MISA_DEV_MODE: ${MISA_DEV_MODE:-false}"
echo "  MISA_DATA_DIR: ${MISA_DATA_DIR:-$HOME/.local/share/misa-ai}"
echo ""

# Export environment variables for current session
export MISA_LOG_LEVEL="${MISA_LOG_LEVEL:-debug}"
export MISA_DEV_MODE="${MISA_DEV_MODE:-false}"
export MISA_DATA_DIR="${MISA_DATA_DIR:-$HOME/.local/share/misa-ai}"

# Add scripts to PATH
export PATH="$PATH:$PATH:$PROJECT_ROOT/scripts:$PATH"

# Create development aliases for common tasks
echo "🔗 Creating development aliases..."
cat >> "$HOME/.bashrc" << 'EOF
# MISA.AI Development Aliases
alias misa-build="$PROJECT_ROOT/scripts/build-all.sh"
alias misa-install="$PROJECT_ROOT/scripts/install-dependencies.sh"
alias misa-test="$PROJECT_ROOT/scripts/test.sh"
alias misa-clean="$PROJECT_ROOT/scripts/clean.sh"

alias misa-kernel="cargo run --bin misa-kernel --data-dir $MISA_DATA_DIR"
alias misa-web="cd $PROJECT_ROOT/web && npm run dev"
alias misa-desktop="cd $PROJECT_ROOT/desktop && npm run tauri dev"
alias misa-android="cd $PROJECT_ROOT/android && ./gradlew installDebug"
EOF

# Apply the bash aliases
source "$HOME/.bashrc"

print_section "Development Setup Complete!"
echo "✅ Ready for development!"
echo ""
echo "🚀 Quick Development Commands:"
echo "  misa-kernel      # Start kernel service"
echo "  misa-web         # Start web development"
echo "  misa-desktop    # Start desktop development"
echo "  misa-android    # Build Android app"
echo ""
echo "📚 Test Everything:"
echo "  misa-test       # Run all tests"
echo "  misa-build       # Build all components"
echo ""
echo "🚀 Quick Deploy:"
echo "  sudo ./dist/install.sh    # Install system-wide"
echo ""
echo "🔗 Learn More:"
echo "  dist/docs/README.md    # Usage guide"
echo "  CONTRIBUTING.md              # Contributing guide"
echo ""

# Success message
echo ""
echo "🎉 MISA.AI development environment is ready!"
echo ""
echo "💻 Happy coding! 🤖"
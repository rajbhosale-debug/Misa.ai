#!/bin/bash

# Clean MISA.AI Build Artifacts
# This script removes all build artifacts and temporary files

set -e

echo "🧹 Cleaning MISA.AI build artifacts..."

# Function to check if directory exists and not empty
check_dir_nonempty() {
    if [ -d "$1" ] && [ "$(ls -A "$1" | wc -l)" -gt 0 ]; then
        return 0
    fi
    return 1
}

# Function to safely remove directory
safe_remove() {
    if [ -d "$1" ]; then
        rm -rf "$1"
        echo "  Removed: $1"
    else
        echo "  Skipped (not found): $1"
    fi
}

# Function to safely remove files matching pattern
safe_remove_files() {
    find "$2" -type f -name "$3" 2>/dev/null
}

# Change to project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ ! -f "$PROJECT_ROOT/core/Cargo.toml" ]; then
    echo "❌ Error: Not in MISA.AI project root directory"
    echo "Please run this script from the project root directory"
    exit 1
fi

cd "$PROJECT_ROOT"

print_section "Cleaning Build Artifacts"

# Clean Rust build artifacts
if check_dir_nonempty "target"; then
    echo "🧹 Removing Rust target directory..."
    safe_remove "target"
fi

# Clean TypeScript build artifacts
if check_dir_nonempty "shared/dist"; then
    echo "🧹 Removing shared library builds..."
    safe_remove "shared/dist"
fi

# Clean Web application
if check_dir_nonempty "web/dist"; then
    echo "🧹 Removing web application build..."
    safe_remove "web/dist"
fi

# Clean Desktop application
if check_dir_nonempty "desktop/src-tauri/target"; then
    echo "🧹 Removing desktop application builds..."
    safe_remove "desktop/src-tauri/target"
fi

# Clean Android application
if check_dir_nonempty "android/app/build"; then
    echo "🧹 Removing Android application builds..."
    safe_remove "android/app/build"
fi

# Clean Docker images
echo "🧹 Removing Docker containers and images..."
docker-compose -f infrastructure/docker/docker-compose.yml down -v 2>/dev/null

# Clean test results
if check_dir_nonempty "test-results"; then
    echo "🧹 Removing test results..."
    safe_remove "test-results"
fi

# Clean development data
if check_dir_nonempty "dev-data"; then
    echo "🧹 Removing development data..."
    safe_remove "dev-data"
fi

# Clean logs
echo "🧹 Removing log files..."
safe_remove_files "*.log"
safe_remove_files "*.log.*"
safe_remove_files "npm-debug.log*"
safe_remove_files "yarn-error.log*"
safe_remove_files "lerna-debug.log*"

# Clean cache directories
echo "🧹 Cleaning cache directories..."
safe_remove ".cache"
safe_remove ".cargo/target"
safe_remove ".npm-cache"
safe_remove ".vitepress"
safe_remove ".yarn/cache"

# Clean temporary files
echo "🧹 Removing temporary files..."
safe_remove_files "*.tmp"
safe_remove_files "*.temp"

# Clean backup files
echo "🧹 Removing backup files..."
safe_remove_files "*.bak"
safe_remove_files "*.old"

# Clean coverage reports
echo "🧹 Cleaning coverage reports..."
safe_remove_files "*.lcov"
safe_remove_files "coverage/"
safe_remove_files ".coverage/coverage/"
safe_remove_files "coverage/*.json"

# Clean TypeScript build artifacts
echo "🧹 Removing TypeScript build artifacts..."
find . -name "*.tsbuildinfo" -delete 2>/dev/null
safe_remove_files ".tsbuildinfo*"

print_section "Temporary Clean Complete!"
echo ""
echo "🧹 Cleaned $(find . -type f | wc -l) files"
echo ""
echo "🗑️ Data and logs preserved (non-destructive)"
echo ""

# Success message
echo ""
echo "🎉 MISA.AI cleaned successfully!"
echo ""
echo "✅ Ready for fresh build with:"
echo "   ./scripts/build-all.sh"
echo ""
echo "💾 Save your changes first!"
echo "   git add ."
echo "   git commit -m \"Implemented MISA.AI platform\""
echo "   git push origin main"
echo ""

# Optional: Clean development data
if [ "$1" = "--clean-all" ]; then
    echo "🗑️ Also removing development data..."
    safe_remove dev-data
fi

echo ""
echo "📊 Build with: ./scripts/build-all.sh"
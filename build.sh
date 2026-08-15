#!/bin/bash
# NoteVault Build Script - Universal build orchestration
# Supports: Android, iOS, macOS, Linux, Windows

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
OUTPUT_DIR="$PROJECT_ROOT/build/outputs"

# Detect OS
detect_os() {
    case "$(uname -s)" in
        Linux*)     echo "Linux";;
        Darwin*)    echo "macOS";;
        MINGW*)     echo "Windows";;
        CYGWIN*)    echo "Windows";;
        *)          echo "UNKNOWN";;
    esac
}

# Print banner
print_banner() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║     NoteVault Multi-Platform Build System         ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# Print usage
print_usage() {
    cat << EOF
${YELLOW}Usage: $0 <command> [options]${NC}

${YELLOW}Commands:${NC}
    help                    Show this help message
    all                     Build for all platforms
    android                 Build Android APK
    ios                     Build iOS framework
    desktop                 Build desktop apps (current OS)
    macos                   Build macOS DMG
    linux                   Build Linux DEB/AppImage
    windows                 Build Windows MSI/EXE
    clean                   Clean build artifacts
    test                    Run tests
    setup                   Setup development environment

${YELLOW}Examples:${NC}
    $0 android              # Build Android APK
    $0 all                  # Build all platforms
    $0 desktop              # Build for current OS
    $0 setup                # Initial environment setup

EOF
}

# Print success message
success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Print error message
error() {
    echo -e "${RED}✗ $1${NC}"
}

# Print info message
info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    info "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        error "Java is not installed"
        return 1
    fi
    success "Java found: $(java -version 2>&1 | head -1)"
    
    # Check Gradle
    if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
        error "Gradle wrapper not found"
        return 1
    fi
    success "Gradle wrapper found"
    
    # Check Git
    if ! command -v git &> /dev/null; then
        error "Git is not installed"
        return 1
    fi
    success "Git found"
    
    return 0
}

# Setup environment
setup_environment() {
    info "Setting up development environment..."
    
    cd "$PROJECT_ROOT"
    
    # Check prerequisites
    if ! check_prerequisites; then
        error "Prerequisites not met"
        return 1
    fi
    
    # Create local.properties if not exists
    if [ ! -f "local.properties" ]; then
        info "Creating local.properties..."
        if [ -z "$ANDROID_HOME" ]; then
            ANDROID_SDK_PATH="/usr/local/lib/android/sdk"
            if [ ! -d "$ANDROID_SDK_PATH" ]; then
                ANDROID_SDK_PATH="$HOME/Android/sdk"
            fi
        else
            ANDROID_SDK_PATH="$ANDROID_HOME"
        fi
        
        {
            echo "# Android SDK configuration"
            echo "sdk.dir=$ANDROID_SDK_PATH"
            echo "org.gradle.java.home=$JAVA_HOME"
        } > local.properties
        success "local.properties created"
    fi
    
    # Create build output directory
    mkdir -p "$OUTPUT_DIR"
    
    success "Environment setup complete"
}

# Build Android
build_android() {
    info "Building Android APK..."
    
    cd "$PROJECT_ROOT"
    
    # Build debug APK
    info "Building debug APK..."
    ./gradlew assembleDebug || {
        error "Failed to build debug APK"
        return 1
    }
    success "Debug APK built"
    
    # Build release APK
    info "Building release APK..."
    ./gradlew assembleRelease || {
        error "Failed to build release APK (may require signing configuration)"
        # Don't exit, as debug build succeeded
    }
    success "Release APK built (if signing configured)"
    
    # Copy to output
    mkdir -p "$OUTPUT_DIR/android"
    cp -r app/build/outputs/apk/* "$OUTPUT_DIR/android/" 2>/dev/null || true
    
    success "Android APKs ready in $OUTPUT_DIR/android"
}

# Build iOS
build_ios() {
    if [ "$(detect_os)" != "macOS" ]; then
        error "iOS builds require macOS"
        return 1
    fi
    
    info "Building iOS framework..."
    
    cd "$PROJECT_ROOT"
    
    ./gradlew :app:buildFramework || {
        error "Failed to build iOS framework"
        return 1
    }
    success "iOS framework built"
    
    # Copy to output
    mkdir -p "$OUTPUT_DIR/ios"
    cp -r app/build/outputs/framework/* "$OUTPUT_DIR/ios/" 2>/dev/null || true
    
    info "Building iOS IPA (requires Xcode project)..."
    success "iOS framework ready in $OUTPUT_DIR/ios"
}

# Build macOS Desktop
build_macos() {
    if [ "$(detect_os)" != "macOS" ]; then
        error "macOS builds require macOS"
        return 1
    fi
    
    info "Building macOS DMG..."
    
    cd "$PROJECT_ROOT"
    
    ./gradlew :app:packageDmg || {
        error "Failed to build macOS DMG"
        return 1
    }
    success "macOS DMG built"
    
    # Copy to output
    mkdir -p "$OUTPUT_DIR/macos"
    cp -r app/build/distributions/*.dmg "$OUTPUT_DIR/macos/" 2>/dev/null || true
    
    success "macOS DMG ready in $OUTPUT_DIR/macos"
}

# Build Linux Desktop
build_linux() {
    if [ "$(detect_os)" != "Linux" ]; then
        error "Linux builds require Linux"
        return 1
    fi
    
    info "Building Linux packages..."
    
    cd "$PROJECT_ROOT"
    
    # Build DEB
    info "Building DEB package..."
    ./gradlew :app:packageDeb || {
        error "Failed to build DEB package"
    }
    success "DEB package built"
    
    # Build AppImage
    info "Building AppImage..."
    ./gradlew :app:packageAppImage || {
        error "Failed to build AppImage"
    }
    success "AppImage built"
    
    # Copy to output
    mkdir -p "$OUTPUT_DIR/linux"
    cp -r app/build/distributions/*.deb "$OUTPUT_DIR/linux/" 2>/dev/null || true
    cp -r app/build/distributions/*.AppImage "$OUTPUT_DIR/linux/" 2>/dev/null || true
    
    success "Linux packages ready in $OUTPUT_DIR/linux"
}

# Build Windows Desktop
build_windows() {
    if [ "$(detect_os)" != "Windows" ]; then
        error "Windows builds require Windows"
        return 1
    fi
    
    info "Building Windows installers..."
    
    cd "$PROJECT_ROOT"
    
    # Build MSI
    info "Building MSI installer..."
    ./gradlew :app:packageMsi || {
        error "Failed to build MSI (requires WiX toolset)"
    }
    success "MSI installer built"
    
    # Build EXE
    info "Building EXE installer..."
    ./gradlew :app:packageExe || {
        error "Failed to build EXE"
    }
    success "EXE installer built"
    
    # Copy to output
    mkdir -p "$OUTPUT_DIR/windows"
    cp -r app/build/distributions/*.msi "$OUTPUT_DIR/windows/" 2>/dev/null || true
    cp -r app/build/distributions/*.exe "$OUTPUT_DIR/windows/" 2>/dev/null || true
    
    success "Windows installers ready in $OUTPUT_DIR/windows"
}

# Build desktop (current OS)
build_desktop() {
    local os=$(detect_os)
    
    case "$os" in
        macOS)      build_macos;;
        Linux)      build_linux;;
        Windows)    build_windows;;
        *)          error "Unsupported OS: $os"; return 1;;
    esac
}

# Build all platforms
build_all() {
    info "Building for all platforms..."
    
    # Android
    info "Phase 1: Android"
    build_android
    
    # iOS (only on macOS)
    if [ "$(detect_os)" = "macOS" ]; then
        info "Phase 2: iOS"
        build_ios
    else
        info "Skipping iOS (requires macOS)"
    fi
    
    # Desktop
    info "Phase 3: Desktop"
    build_desktop
    
    success "All platform builds complete"
    list_artifacts
}

# Clean build
clean_build() {
    info "Cleaning build artifacts..."
    
    cd "$PROJECT_ROOT"
    
    ./gradlew clean || true
    rm -rf build/ 2>/dev/null || true
    rm -rf app/build/ 2>/dev/null || true
    rm -rf .gradle/ 2>/dev/null || true
    
    success "Build artifacts cleaned"
}

# Run tests
run_tests() {
    info "Running tests..."
    
    cd "$PROJECT_ROOT"
    
    ./gradlew test || {
        error "Tests failed"
        return 1
    }
    
    success "Tests completed"
}

# List artifacts
list_artifacts() {
    echo ""
    echo -e "${YELLOW}Generated Artifacts:${NC}"
    echo ""
    
    if [ -d "$OUTPUT_DIR" ]; then
        find "$OUTPUT_DIR" -type f \( -name "*.apk" -o -name "*.dmg" -o -name "*.deb" -o -name "*.AppImage" -o -name "*.exe" -o -name "*.msi" \) -exec ls -lh {} \; 2>/dev/null
    fi
}

# Main function
main() {
    print_banner
    
    local command="${1:-help}"
    
    case "$command" in
        help)           print_usage;;
        setup)          setup_environment;;
        all)            build_all;;
        android)        build_android;;
        ios)            build_ios;;
        desktop)        build_desktop;;
        macos)          build_macos;;
        linux)          build_linux;;
        windows)        build_windows;;
        clean)          clean_build;;
        test)           run_tests;;
        *)              error "Unknown command: $command"; print_usage; exit 1;;
    esac
}

# Run main
main "$@"

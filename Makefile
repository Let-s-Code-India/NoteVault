.PHONY: help build-all build-android build-ios build-macos build-linux build-windows clean test setup

# Colors for output
YELLOW := \033[0;33m
GREEN := \033[0;32m
BLUE := \033[0;34m
NC := \033[0m # No Color

GRADLE := ./gradlew
JAVA_VERSION := 17
ANDROID_SDK_VERSION := 36

help:
	@echo "$(BLUE)═══════════════════════════════════════════════════════════$(NC)"
	@echo "$(BLUE)  NoteVault Multi-Platform Build System$(NC)"
	@echo "$(BLUE)═══════════════════════════════════════════════════════════$(NC)"
	@echo ""
	@echo "$(YELLOW)Build Targets:$(NC)"
	@echo "  $(GREEN)make build-all$(NC)              Build for all platforms"
	@echo "  $(GREEN)make build-android$(NC)         Build Android APK (debug & release)"
	@echo "  $(GREEN)make build-ios$(NC)             Build iOS framework & IPA"
	@echo "  $(GREEN)make build-macos$(NC)           Build macOS DMG"
	@echo "  $(GREEN)make build-linux$(NC)           Build Linux DEB & AppImage"
	@echo "  $(GREEN)make build-windows$(NC)         Build Windows MSI & EXE"
	@echo ""
	@echo "$(YELLOW)Android Targets:$(NC)"
	@echo "  $(GREEN)make android-debug$(NC)        Build debug APK only"
	@echo "  $(GREEN)make android-release$(NC)      Build release APK only"
	@echo "  $(GREEN)make android-install$(NC)      Install debug APK on device"
	@echo "  $(GREEN)make android-test$(NC)         Run Android unit tests"
	@echo ""
	@echo "$(YELLOW)Desktop Targets:$(NC)"
	@echo "  $(GREEN)make desktop-macos$(NC)        Build macOS DMG"
	@echo "  $(GREEN)make desktop-linux-deb$(NC)    Build Linux DEB package"
	@echo "  $(GREEN)make desktop-linux-appimage$(NC) Build Linux AppImage"
	@echo "  $(GREEN)make desktop-windows-msi$(NC)  Build Windows MSI"
	@echo "  $(GREEN)make desktop-windows-exe$(NC)  Build Windows EXE"
	@echo ""
	@echo "$(YELLOW)Utility Targets:$(NC)"
	@echo "  $(GREEN)make clean$(NC)                Clean build artifacts"
	@echo "  $(GREEN)make test$(NC)                 Run all tests"
	@echo "  $(GREEN)make setup$(NC)                Setup local environment"
	@echo "  $(GREEN)make dependencies$(NC)        List all dependencies"
	@echo "  $(GREEN)make info$(NC)                Show build environment info"
	@echo ""

# ==========================================
# Setup & Configuration
# ==========================================

setup:
	@echo "$(BLUE)Setting up NoteVault development environment...$(NC)"
	@command -v java >/dev/null 2>&1 || { echo "$(YELLOW)Java not found. Please install Java $(JAVA_VERSION)+$(NC)"; exit 1; }
	@echo "✓ Java: $$(java -version 2>&1 | head -1)"
	@command -v $(GRADLE) >/dev/null 2>&1 || { echo "$(YELLOW)Gradle not found. Installing...$(NC)"; }
	@echo "✓ Gradle: $$($(GRADLE) --version | grep Gradle | awk '{print $$2}')"
	@[ -f local.properties ] || echo "sdk.dir=$${ANDROID_HOME}" > local.properties
	@echo "✓ local.properties configured"
	@echo "$(GREEN)✓ Environment ready for building$(NC)"

info:
	@echo "$(BLUE)Build Environment Information:$(NC)"
	@echo "Java version:"
	@java -version 2>&1 | head -3
	@echo ""
	@echo "Gradle version:"
	@$(GRADLE) --version | grep "Gradle"
	@echo ""
	@echo "Android SDK (from local.properties):"
	@grep "sdk.dir" local.properties || echo "Not configured"
	@echo ""
	@echo "System OS: $$(uname -s)"
	@echo "System Architecture: $$(uname -m)"

# ==========================================
# Build Targets
# ==========================================

build-all: build-android build-ios build-macos build-linux build-windows
	@echo "$(GREEN)✓ All platforms built successfully!$(NC)"

# ==========================================
# Android
# ==========================================

build-android: android-debug android-release
	@echo "$(GREEN)✓ Android builds complete$(NC)"

android-debug:
	@echo "$(BLUE)Building Android Debug APK...$(NC)"
	$(GRADLE) clean assembleDebug
	@echo "$(GREEN)✓ Debug APK: app/build/outputs/apk/debug/$(NC)"

android-release:
	@echo "$(BLUE)Building Android Release APK...$(NC)"
	$(GRADLE) assembleRelease
	@echo "$(GREEN)✓ Release APK: app/build/outputs/apk/release/$(NC)"

android-install:
	@echo "$(BLUE)Installing Debug APK on device...$(NC)"
	$(GRADLE) installDebug
	@echo "$(GREEN)✓ APK installed$(NC)"

android-test:
	@echo "$(BLUE)Running Android tests...$(NC)"
	$(GRADLE) :app:testDebugUnitTest

# ==========================================
# iOS
# ==========================================

build-ios:
	@echo "$(BLUE)Building iOS framework...$(NC)"
	$(GRADLE) :app:buildFramework
	@echo "$(GREEN)✓ iOS Framework: app/build/outputs/framework/$(NC)"
	@echo "$(YELLOW)Note: IPA build requires Xcode project configuration$(NC)"

# ==========================================
# Desktop - macOS
# ==========================================

build-macos: desktop-macos

desktop-macos:
	@echo "$(BLUE)Building macOS DMG...$(NC)"
	$(GRADLE) :app:packageDmg
	@mkdir -p app/build/outputs/package/macos
	@[ -f app/build/distributions/*.dmg ] && mv app/build/distributions/*.dmg app/build/outputs/package/macos/ || true
	@echo "$(GREEN)✓ macOS DMG: app/build/outputs/package/macos/$(NC)"

# ==========================================
# Desktop - Linux
# ==========================================

build-linux: desktop-linux-deb desktop-linux-appimage

desktop-linux-deb:
	@echo "$(BLUE)Building Linux DEB package...$(NC)"
	@command -v $(GRADLE) >/dev/null 2>&1 || { echo "$(YELLOW)Gradle not found$(NC)"; exit 1; }
	$(GRADLE) :app:packageDeb
	@mkdir -p app/build/outputs/package/linux
	@[ -f app/build/distributions/*.deb ] && mv app/build/distributions/*.deb app/build/outputs/package/linux/ || true
	@echo "$(GREEN)✓ Linux DEB: app/build/outputs/package/linux/$(NC)"

desktop-linux-appimage:
	@echo "$(BLUE)Building Linux AppImage...$(NC)"
	$(GRADLE) :app:packageAppImage
	@mkdir -p app/build/outputs/package/linux
	@[ -f app/build/distributions/*.AppImage ] && mv app/build/distributions/*.AppImage app/build/outputs/package/linux/ || true
	@echo "$(GREEN)✓ Linux AppImage: app/build/outputs/package/linux/$(NC)"

# ==========================================
# Desktop - Windows
# ==========================================

build-windows: desktop-windows-msi desktop-windows-exe

desktop-windows-msi:
	@echo "$(BLUE)Building Windows MSI...$(NC)"
	$(GRADLE) :app:packageMsi
	@mkdir -p app/build/outputs/package/windows
	@if [ -f app/build/distributions/*.msi ]; then mv app/build/distributions/*.msi app/build/outputs/package/windows/; fi || true
	@echo "$(GREEN)✓ Windows MSI: app/build/outputs/package/windows/$(NC)"

desktop-windows-exe:
	@echo "$(BLUE)Building Windows EXE...$(NC)"
	$(GRADLE) :app:packageExe
	@mkdir -p app/build/outputs/package/windows
	@if [ -f app/build/distributions/*.exe ]; then mv app/build/distributions/*.exe app/build/outputs/package/windows/; fi || true
	@echo "$(GREEN)✓ Windows EXE: app/build/outputs/package/windows/$(NC)"

# ==========================================
# Testing
# ==========================================

test: test-unit test-integration
	@echo "$(GREEN)✓ All tests completed$(NC)"

test-unit:
	@echo "$(BLUE)Running unit tests...$(NC)"
	$(GRADLE) test

test-integration:
	@echo "$(BLUE)Running integration tests...$(NC)"
	$(GRADLE) connectedAndroidTest

# ==========================================
# Cleaning & Maintenance
# ==========================================

clean:
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	$(GRADLE) clean
	@rm -rf build/
	@rm -rf app/build/
	@rm -rf .gradle/
	@echo "$(GREEN)✓ Clean complete$(NC)"

clean-deps:
	@echo "$(BLUE)Updating dependencies...$(NC)"
	$(GRADLE) --refresh-dependencies clean

dependencies:
	@echo "$(BLUE)Gradle dependency tree:$(NC)"
	$(GRADLE) dependencies

# ==========================================
# Development Build (Fast)
# ==========================================

dev-build:
	@echo "$(BLUE)Building for development (optimized for speed)...$(NC)"
	$(GRADLE) assembleDebug -x lint --build-cache

dev-build-release:
	@echo "$(BLUE)Building release (optimized)...$(NC)"
	$(GRADLE) assembleRelease --build-cache

# ==========================================
# CI/CD Simulation
# ==========================================

ci-build: clean
	@echo "$(BLUE)Running CI build simulation...$(NC)"
	$(GRADLE) build

# ==========================================
# Artifact Management
# ==========================================

artifacts-list:
	@echo "$(BLUE)Generated Artifacts:$(NC)"
	@echo ""
	@echo "Android:"
	@find app/build/outputs/apk -name "*.apk" 2>/dev/null | xargs ls -lh || echo "  No APKs found"
	@echo ""
	@echo "iOS:"
	@find app/build/outputs -name "*.framework" -o -name "*.ipa" 2>/dev/null | xargs ls -lh || echo "  No iOS artifacts found"
	@echo ""
	@echo "Desktop:"
	@find app/build/distributions app/build/outputs/package -type f 2>/dev/null | xargs ls -lh || echo "  No desktop artifacts found"

artifacts-clean:
	@echo "$(BLUE)Cleaning old artifacts...$(NC)"
	@rm -rf app/build/outputs/
	@echo "$(GREEN)✓ Artifacts cleaned$(NC)"

# ==========================================
# Version Management
# ==========================================

version:
	@echo "NoteVault Version Info"
	@grep -E "versionName|versionCode" app/build.gradle.kts || echo "Version info not found"

version-bump:
	@echo "Version bumping not implemented in Makefile"
	@echo "Edit app/build.gradle.kts manually"

# ==========================================
# Documentation
# ==========================================

docs:
	@echo "$(BLUE)NoteVault Documentation:$(NC)"
	@echo "  - README.md: Main project documentation"
	@echo "  - MULTIPLATFORM_SETUP.md: Multi-platform setup guide"
	@echo "  - BUILD_GUIDE.md: Detailed build instructions"

# ==========================================
# Platform Detection & Targets
# ==========================================

current-platform:
	@uname -s | grep -q Darwin && echo "macOS" || \
	uname -s | grep -q Linux && echo "Linux" || \
	uname -s | grep -q MINGW && echo "Windows" || \
	echo "Unknown"

# ==========================================
# Debug Targets
# ==========================================

gradle-debug:
	@echo "$(BLUE)Gradle debug build...$(NC)"
	$(GRADLE) build --debug

gradle-info:
	@$(GRADLE) buildEnvironment

# ==========================================
# Default Target
# ==========================================

.DEFAULT_GOAL := help

# NoteVault Complete Build Guide

## Table of Contents
1. [Quick Start](#quick-start)
2. [Platform-Specific Guides](#platform-specific-guides)
3. [Build Outputs](#build-outputs)
4. [CI/CD Pipeline](#cicd-pipeline)
5. [Troubleshooting](#troubleshooting)
6. [Advanced Configuration](#advanced-configuration)

---

## Quick Start

### One-Command Build (All Platforms)
```bash
make build-all
# Or with shell script
./build.sh all
```

### Quick Android Build
```bash
make android-debug
# Or with shell script
./build.sh android
```

### Environment Setup
```bash
make setup
# Or with shell script
./build.sh setup
```

---

## Platform-Specific Guides

### 🤖 Android

#### Prerequisites
- **JDK**: Java 17+ (OpenJDK or Oracle)
- **Android SDK**: API Level 36 (Android 15)
- **Gradle**: 8.7+ (included in repo)
- **Memory**: 4GB+ recommended
- **Storage**: 8GB+ free space

#### Environment Setup
```bash
# Install Java 17
# On Ubuntu/Debian:
sudo apt-get install openjdk-17-jdk

# On macOS:
brew install openjdk@17

# On Windows:
# Download from adoptium.net

# Set ANDROID_HOME
export ANDROID_HOME=$HOME/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
```

#### Build Commands
```bash
# Debug APK
./gradlew assembleDebug
make android-debug

# Release APK (requires keystore)
./gradlew assembleRelease
make android-release

# Install on emulator/device
./gradlew installDebug
make android-install

# Run tests
./gradlew :app:testDebugUnitTest
make android-test

# Run UI tests
./gradlew :app:connectedAndroidTest
```

#### Output Locations
```
app/build/outputs/apk/debug/         # Debug APK
app/build/outputs/apk/release/       # Release APK
app/build/test-results/              # Test results
app/build/reports/                   # Build reports
```

#### Release Signing Setup
```bash
# Generate keystore
keytool -genkey -v -keystore release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release -storepass password

# Create signing configuration in app/build.gradle.kts
signingConfigs {
    release {
        storeFile = file("$rootDir/release.keystore")
        storePassword = "password"
        keyAlias = "release"
        keyPassword = "password"
    }
}
```

#### Testing on Device
```bash
# List connected devices
adb devices

# Install app
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Run app
adb shell am start -n com.letscodeinda.notevault/.MainActivity

# View logs
adb logcat | grep "notevault"
```

---

### 🍎 iOS

#### Prerequisites
- **macOS**: 12.0+ (Monterey or later)
- **Xcode**: 15.0+ (includes iOS SDK 17+)
- **CocoaPods**: Optional, for dependency management
- **Command Line Tools**: `xcode-select --install`

#### Environment Setup
```bash
# Install Xcode Command Line Tools
xcode-select --install

# Verify Xcode
xcode-select -p
# Output: /Applications/Xcode.app/Contents/Developer

# Install CocoaPods (optional)
sudo gem install cocoapods
```

#### Build Commands
```bash
# Build Kotlin framework for iOS
./gradlew :app:buildFramework
make build-ios

# The framework will be in:
# app/build/outputs/framework/
```

#### iOS Project Structure Setup
```
iosApp/                          # iOS Xcode project
├── iosApp.xcodeproj/            # Xcode project
├── iosApp/
│   ├── ContentView.swift        # Main SwiftUI view
│   ├── iosApp.swift             # App entry point
│   └── Assets.xcassets/
├── iosApp.xcworkspace/          # (if using CocoaPods)
└── Podfile                      # (if using CocoaPods)
```

#### Building and Running
```bash
# Open in Xcode
open iosApp/iosApp.xcodeproj

# Build for simulator
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  build

# Build for device
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -sdk iphoneos \
  build

# Archive for App Store
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -sdk iphoneos \
  -archivePath iosApp.xcarchive \
  archive
```

#### Code Signing
```bash
# Set development team in Xcode or via command line
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -allowProvisioningUpdates \
  build
```

---

### 🖥️ Desktop (macOS)

#### Prerequisites
- **macOS**: 11.0+ (Big Sur or later)
- **Java**: 17+
- **Xcode Command Line Tools**: For compilation

#### Build Commands
```bash
# Build DMG (macOS distribution format)
./gradlew :app:packageDmg
make build-macos

# Output location
app/build/distributions/app-*.dmg
```

#### Distribution
```bash
# Sign DMG (optional)
codesign -s "Developer ID Application" app.dmg

# Notarize for Gatekeeper (required for distribution)
xcrun notarytool submit app.dmg \
  --apple-id your-id@apple.com \
  --password your-app-password \
  --team-id TEAM_ID

# Staple notarization
xcrun stapler staple app.dmg
```

---

### 🐧 Desktop (Linux)

#### Prerequisites
- **Linux**: Ubuntu 20.04+ or equivalent
- **Java**: 17+
- **Build tools**: 
  ```bash
  sudo apt-get install -y \
    build-essential \
    libgconf-2-4 \
    libx11-dev \
    libxext-dev \
    libxrender-dev \
    libxinerama-dev
  ```

#### Build Commands
```bash
# Build DEB package
./gradlew :app:packageDeb
make desktop-linux-deb

# Build AppImage
./gradlew :app:packageAppImage
make desktop-linux-appimage

# Output locations
app/build/distributions/app-*.deb
app/build/distributions/app-*.AppImage
```

#### Installation
```bash
# Install DEB
sudo dpkg -i app-*.deb
sudo apt-get install -f  # Fix dependencies if needed

# Run AppImage
./app-*.AppImage
# Or make executable
chmod +x app-*.AppImage
./app-*.AppImage
```

---

### 🪟 Desktop (Windows)

#### Prerequisites
- **Windows**: 10 or later
- **Java**: 17+
- **WiX Toolset**: For MSI generation (optional)

#### Build Commands
```bash
# Build EXE installer
.\gradlew :app:packageExe
make desktop-windows-exe

# Build MSI installer
.\gradlew :app:packageMsi
make desktop-windows-msi

# Output locations
app\build\distributions\app-*.exe
app\build\distributions\app-*.msi
```

#### Installation
```powershell
# Run EXE installer
.\app-*.exe

# Install MSI
msiexec /i app-*.msi
```

---

## Build Outputs

### Output Directory Structure
```
build/outputs/
├── android/
│   ├── debug/
│   │   └── *.apk              # Debug APK files
│   └── release/
│       └── *.apk              # Release APK files
├── ios/
│   └── *.framework            # iOS framework
├── macos/
│   └── *.dmg                  # macOS app bundle
├── linux/
│   ├── *.deb                  # Debian package
│   └── *.AppImage             # AppImage executable
└── windows/
    ├── *.exe                  # Windows executable
    └── *.msi                  # Windows installer
```

### Size Estimates
| Platform | Format | Size |
|----------|--------|------|
| Android | APK | 15-25 MB |
| iOS | IPA | 20-30 MB |
| macOS | DMG | 25-40 MB |
| Linux | DEB | 20-30 MB |
| Linux | AppImage | 22-32 MB |
| Windows | EXE/MSI | 25-40 MB |

---

## CI/CD Pipeline

### GitHub Actions Workflow

#### Automatic Builds
- **Triggers**: Every push to `main` or `develop` branches
- **Platforms**: All (Android, iOS, macOS, Linux, Windows)
- **Artifacts**: Retained for 30 days

#### Manual Workflow Dispatch
```bash
# Via GitHub Web UI
1. Go to Actions tab
2. Select "Build NoteVault Multi-Platform"
3. Click "Run workflow"
4. Choose platform:
   - all (default)
   - android
   - ios
   - desktop
```

#### Pull Request Builds
- Runs on every PR to `main` or `develop`
- Builds all platforms
- Artifacts available in PR checks

#### Artifact Access
```bash
# Download from GitHub Actions
1. Go to Actions tab
2. Select workflow run
3. Scroll to "Artifacts" section
4. Download desired artifacts
```

### Local CI Simulation
```bash
# Run CI-like build locally
make ci-build

# Or with script
./build.sh all
```

---

## Troubleshooting

### Common Issues & Solutions

#### 1. Android SDK Not Found
```
Error: SDK location not found. Define a valid SDK location...
```

**Solution:**
```bash
# Option 1: Set ANDROID_HOME
export ANDROID_HOME=$HOME/Android/sdk

# Option 2: Create local.properties
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Option 3: Using Gradle wrapper
./gradlew --init
```

#### 2. Gradle Daemon Issues
```
Error: Could not connect to Kotlin compile daemon
```

**Solution:**
```bash
# Clean daemon
./gradlew --stop

# Rebuild
./gradlew clean build
```

#### 3. Out of Memory
```
Error: Java heap space
```

**Solution:**
```bash
# Increase heap size
export GRADLE_OPTS="-Xmx4g"

# Or in gradle.properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
```

#### 4. iOS Build Fails
```
Error: Xcode not found
```

**Solution:**
```bash
# Install Xcode Command Line Tools
xcode-select --install

# Verify installation
xcode-select -p
```

#### 5. Linux Package Build Issues
```
Error: dpkg or AppImage tools not found
```

**Solution:**
```bash
# Install required packages
sudo apt-get install -y build-essential dpkg

# For AppImage
sudo apt-get install -y libgconf-2-4 libx11-dev
```

### Getting Help

1. **Check Build Logs**
   ```bash
   # Verbose output
   ./gradlew build --stacktrace

   # Debug output
   ./gradlew build --debug
   ```

2. **View Gradle Reports**
   ```
   app/build/reports/
   ```

3. **Platform-Specific Logs**
   - Android: `adb logcat`
   - iOS: Xcode console
   - Desktop: System logs

---

## Advanced Configuration

### Custom Build Variants
```bash
# Create product flavor in app/build.gradle.kts
flavorDimensions("version")
productFlavors {
    free {
        dimension = "version"
        applicationIdSuffix = ".free"
    }
    pro {
        dimension = "version"
        applicationIdSuffix = ".pro"
    }
}

# Build specific variant
./gradlew assembleFreeDebug
./gradlew assembleProRelease
```

### Performance Optimization
```gradle
// In gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.workers.max=4
```

### Custom Signing
```gradle
// app/build.gradle.kts
signingConfigs {
    debug { ... }
    release {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "keystore.jks")
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

### Multi-Module Builds
```bash
# Build specific module
./gradlew :module-name:build

# List all tasks
./gradlew tasks
```

---

## Build System Commands Reference

### Gradle Commands
```bash
# Core build
./gradlew build                    # Full build
./gradlew clean                    # Clean build artifacts
./gradlew check                    # Run all checks

# Android specific
./gradlew assembleDebug            # Debug APK
./gradlew assembleRelease          # Release APK
./gradlew installDebug             # Install on device
./gradlew connectedAndroidTest     # UI tests

# Testing
./gradlew test                     # Unit tests
./gradlew testReport               # Generate report

# Desktop
./gradlew packageDmg               # macOS DMG
./gradlew packageDeb               # Linux DEB
./gradlew packageAppImage          # Linux AppImage
./gradlew packageMsi               # Windows MSI
./gradlew packageExe               # Windows EXE
```

### Make Commands
```bash
make help                    # Show help
make setup                   # Setup environment
make build-all              # Build all platforms
make clean                  # Clean artifacts
make test                   # Run tests
make info                   # Show environment info
```

### Shell Script Commands
```bash
./build.sh help             # Show help
./build.sh setup            # Setup environment
./build.sh all              # Build all platforms
./build.sh android          # Build Android
./build.sh clean            # Clean artifacts
```

---

## Version & Release Management

### Update Version
```gradle
// app/build.gradle.kts
android {
    defaultConfig {
        versionCode = 2        // Increment for each release
        versionName = "1.1"    // User-visible version
    }
}
```

### Create Release
```bash
# Tag version
git tag -a v1.1 -m "Release version 1.1"
git push origin v1.1

# Build release
./gradlew assembleRelease

# Sign release
# (Signing configuration per platform)
```

---

## Performance Tips

1. **Use Build Cache**
   ```bash
   ./gradlew build --build-cache
   ```

2. **Enable Parallel Builds**
   - Already enabled in `gradle.properties`

3. **Use Configuration Cache**
   - Already enabled in `gradle.properties`

4. **Incremental Builds**
   - Gradle automatically detects changes

5. **Disable Unused Tasks**
   ```bash
   ./gradlew assembleDebug -x lint -x test
   ```

---

## Next Steps

1. ✅ Set up local development environment
2. ✅ Build Android APK
3. ✅ Set up iOS Xcode project
4. ✅ Configure desktop packaging
5. ✅ Set up CI/CD pipeline
6. 📋 Configure release signing
7. 📋 Set up app distribution
8. 📋 Configure monitoring & analytics

---

## Additional Resources

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Gradle Documentation](https://docs.gradle.org/)
- [Android Developer Guide](https://developer.android.com/)
- [Apple Developer Documentation](https://developer.apple.com/documentation/)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)

---

**Last Updated**: 2026-08-15  
**Version**: 1.0

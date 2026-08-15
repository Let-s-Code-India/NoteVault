# NoteVault Multi-Platform Setup Guide

## Overview
NoteVault is set up as a Kotlin Multiplatform (KMP) project supporting:
- **Android** (.apk)
- **iOS** (.ipa, .framework)
- **macOS Desktop** (.dmg)
- **Linux Desktop** (.deb, .AppImage)
- **Windows Desktop** (.msi, .exe)

## Project Structure

```
NoteVault/
├── app/                          # Shared + Platform-specific code
│   ├── src/
│   │   ├── main/                 # Android-specific code
│   │   │   ├── java/com/example/
│   │   │   ├── kotlin/           # Android Kotlin code
│   │   │   └── res/              # Android resources
│   │   ├── androidMain/          # Shared Android code (KMP)
│   │   ├── commonMain/           # Shared code for all platforms
│   │   ├── desktopMain/          # Desktop (macOS/Linux/Windows)
│   │   │   └── kotlin/
│   │   ├── iosMain/              # iOS-specific code
│   │   │   └── kotlin/
│   │   └── nativeMain/           # Native platform code
│   ├── build.gradle.kts          # Build configuration
│   └── proguard-rules.pro        # ProGuard rules for Android
├── gradle/
│   └── libs.versions.toml        # Dependency versions
├── .github/workflows/
│   ├── build.yml                 # Original Android workflow
│   └── build-multiplatform.yml   # New multi-platform workflow
├── local.properties              # Android SDK configuration
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Gradle settings
└── gradle.properties             # Gradle properties
```

## Setup Instructions

### 1. Android Setup

**Prerequisites:**
- Java 17+
- Android SDK API 36 (configured via `local.properties`)

**Build:**
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Install on emulator/device
./gradlew installDebug

# Output location
app/build/outputs/apk/debug/
app/build/outputs/apk/release/
```

### 2. iOS Setup

**Prerequisites:**
- macOS with Xcode 15+
- iOS SDK 17+
- CocoaPods (optional, for dependency management)

**Build:**
```bash
# Build Kotlin framework for iOS
./gradlew :app:buildFramework

# Build IPA (requires Xcode project configuration)
# Using Xcode command line:
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -sdk iphoneos \
  -archivePath app/build/outputs/ipa/iosApp.xcarchive

# Output locations
app/build/outputs/framework/      # Kotlin framework
app/build/outputs/ipa/            # iOS app archive
```

**iOS Integration Steps:**
1. Create `iosApp/` directory with Xcode project
2. Reference the Kotlin framework in Xcode build settings
3. Update Xcode project to use KMP Kotlin framework

### 3. macOS Desktop Setup

**Prerequisites:**
- macOS 11+
- Xcode Command Line Tools
- Java 17+

**Build:**
```bash
# Build macOS DMG
./gradlew :app:packageDmg

# Output location
app/build/distributions/
app/build/outputs/package/macos/
```

### 4. Linux Desktop Setup

**Prerequisites:**
- Linux (Ubuntu 20.04+ recommended)
- Java 17+
- Required build tools:
  ```bash
  sudo apt-get install -y \
    libgconf-2-4 libx11-dev libxext-dev \
    libxrender-dev libxinerama-dev
  ```

**Build:**
```bash
# Build DEB package
./gradlew :app:packageDeb

# Build AppImage
./gradlew :app:packageAppImage

# Output location
app/build/distributions/
app/build/outputs/package/linux/
```

### 5. Windows Desktop Setup

**Prerequisites:**
- Windows 10+
- Java 17+
- WiX Toolset (for MSI generation, optional)

**Build:**
```bash
# Build Windows MSI
.\gradlew :app:packageMsi

# Build Windows EXE
.\gradlew :app:packageExe

# Output location
app\build\distributions\
app\build\outputs\package\windows\
```

## Build Commands

### Quick Build
```bash
# Android Debug
./gradlew buildAndroid

# All platforms (CI/CD)
./gradlew buildAll
```

### Individual Platform Builds
```bash
# Android
./gradlew assembleDebug
./gradlew assembleRelease

# iOS Framework
./gradlew :app:buildFramework

# Desktop
./gradlew :app:packageDmg      # macOS
./gradlew :app:packageDeb      # Linux
./gradlew :app:packageExe      # Windows
```

### Testing
```bash
# Run all tests
./gradlew test

# Android specific tests
./gradlew :app:testDebugUnitTest

# UI tests
./gradlew :app:connectedAndroidTest
```

## CI/CD Pipeline

### GitHub Actions Workflow
The `build-multiplatform.yml` workflow:
- Builds on every push to `main` and `develop`
- Triggered by pull requests
- Manual workflow dispatch with platform selection
- Generates platform-specific artifacts:
  - **Android**: `notevault-debug-apk`, `notevault-release-apk`
  - **iOS**: `notevault-ios-framework`, `notevault-ios-ipa`
  - **macOS**: `notevault-macos-dmg`
  - **Linux**: `notevault-linux-deb`, `notevault-linux-appimage`
  - **Windows**: `notevault-windows-msi`, `notevault-windows-exe`

### Artifact Management
- Build artifacts are retained for 30 days
- Build logs retained for 7 days
- Artifacts available in GitHub Actions "Artifacts" section

## Configuration Files

### local.properties
```properties
sdk.dir=/path/to/android/sdk
org.gradle.java.home=/path/to/java/home
```

### gradle.properties
Key configurations:
- Parallel builds enabled
- Configuration cache enabled
- Kotlin compiler execution: in-process
- Max workers: 4

### .env (Secrets)
```
FIREBASE_APPCHECK_DEBUG_TOKEN=your_token
ANDROID_SIGNING_KEY=base64_encoded_key
ANDROID_KEY_ALIAS=your_alias
```

## Dependency Management

Dependencies are managed in `gradle/libs.versions.toml`:
- Android libraries (Jetpack, Compose, Room, etc.)
- Firebase integration
- Kotlin coroutines
- Networking (Retrofit, OkHttp)
- JSON serialization (Moshi)
- Database (SQLCipher)
- Testing frameworks

## Platform-Specific Code

### Structure
```kotlin
// src/commonMain/kotlin/ - Shared code
interface DatabaseManager
class NoteRepository

// src/androidMain/kotlin/ - Android only
actual class DatabaseManager : AndroidDatabaseImpl()

// src/iosMain/kotlin/ - iOS only
actual class DatabaseManager : IOSDatabaseImpl()

// src/desktopMain/kotlin/ - Desktop (macOS/Linux/Windows)
actual class DatabaseManager : DesktopDatabaseImpl()
```

### Platform Bridges
Located in:
- `src/main/java/com/example/platform/AndroidPermissionManager.kt`
- `src/desktopMain/kotlin/com/example/platform/DesktopPlatformBridge.kt`
- `src/iosMain/kotlin/com/example/platform/IOSPlatformBridge.kt`

## Troubleshooting

### Android SDK Not Found
```bash
# Set ANDROID_HOME
export ANDROID_HOME=/path/to/android/sdk

# Or update local.properties
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### Gradle Build Fails
```bash
# Clean build
./gradlew clean

# With debugging
./gradlew build --debug

# Check configuration
./gradlew buildEnvironment
```

### Platform-Specific Issues
- **iOS**: Ensure Xcode 15+ is installed
- **macOS**: Install Xcode Command Line Tools: `xcode-select --install`
- **Linux**: Install required dev packages
- **Windows**: Use PowerShell or Command Prompt with admin rights

## Output Artifacts

### Build Outputs
| Platform | Format | Location |
|----------|--------|----------|
| Android | .apk | app/build/outputs/apk/ |
| iOS | .ipa, .framework | app/build/outputs/ipa/, .framework/ |
| macOS | .dmg | app/build/distributions/ |
| Linux | .deb, .AppImage | app/build/distributions/ |
| Windows | .msi, .exe | app/build/distributions/ |

### Size Estimates (Approximate)
- Android APK: 15-25 MB
- iOS IPA: 20-30 MB
- macOS DMG: 25-40 MB
- Linux DEB: 20-30 MB
- Windows EXE/MSI: 25-40 MB

## Next Steps

1. **Set up iOS Xcode Project**
   - Create `iosApp/` directory
   - Configure to use Kotlin framework
   - Set up code signing

2. **Configure Desktop Package**
   - Set application name and version
   - Configure installer options
   - Add signing certificates

3. **Add Platform-Specific Features**
   - Implement platform-specific APIs
   - Add native integrations
   - Configure permissions per platform

4. **Security & Signing**
   - Configure release signing keys
   - Set up certificate management
   - Configure notarization (macOS)

## Resources

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Android Build System](https://developer.android.com/build)
- [iOS Build System](https://developer.apple.com/xcode/)

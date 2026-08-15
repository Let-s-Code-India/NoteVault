# NoteVault Multi-Platform Build Quick Reference

## 🚀 Quick Start (Choose One)

### Android
```bash
make android-debug          # Build debug APK
make android-release        # Build release APK
./build.sh android         # Using shell script
```

### iOS (macOS only)
```bash
make build-ios             # Build iOS framework
./build.sh ios             # Using shell script
```

### Desktop
```bash
make build-macos           # macOS DMG
make build-linux           # Linux DEB/AppImage
make build-windows         # Windows EXE/MSI
./build.sh desktop         # Current OS only
```

### All Platforms
```bash
make build-all             # Using Makefile
./build.sh all             # Using shell script
```

## 📁 Output Locations

| Platform | Output Path | Format |
|----------|------------|--------|
| Android Debug | `app/build/outputs/apk/debug/` | `.apk` |
| Android Release | `app/build/outputs/apk/release/` | `.apk` |
| iOS | `app/build/outputs/framework/` | `.framework` |
| macOS | `app/build/outputs/package/macos/` | `.dmg` |
| Linux DEB | `app/build/outputs/package/linux/` | `.deb` |
| Linux AppImage | `app/build/outputs/package/linux/` | `.AppImage` |
| Windows | `app/build/outputs/package/windows/` | `.exe` / `.msi` |

## 🛠️ Build System Commands

### Using Make
```bash
make help                  # Show all commands
make setup                 # Setup environment
make clean                 # Clean artifacts
make test                  # Run tests
make info                  # Show environment info
```

### Using Gradle Directly
```bash
./gradlew assembleDebug   # Android debug
./gradlew assembleRelease # Android release
./gradlew :app:packageDmg # macOS
./gradlew :app:packageDeb # Linux DEB
```

### Using Shell Script
```bash
./build.sh help           # Show help
./build.sh setup          # Setup environment
./build.sh all            # Build all
./build.sh clean          # Clean artifacts
```

## 📚 Documentation

- **BUILD_GUIDE.md** - Complete build instructions for each platform
- **MULTIPLATFORM_SETUP.md** - Project structure and detailed setup
- **Makefile** - Make-based build automation
- **build.sh** - Universal shell build script

## 🔧 Setup (First Time)

```bash
# 1. Setup environment
make setup
# or
./build.sh setup

# 2. Check configuration
make info

# 3. Build for your platform
make build-android  # or iOS, desktop, etc.
```

## ⚡ Common Tasks

```bash
# Build and install Android APK
make android-debug && make android-install

# Build debug APK and run UI tests
make android-debug && make android-test

# Clean and rebuild everything
make clean && make build-all

# Check if all tools are available
make info
```

## 🐛 Troubleshooting

### Android SDK not found
```bash
export ANDROID_HOME=$HOME/Android/sdk
make setup
```

### Gradle daemon issues
```bash
./gradlew --stop
make clean && make build-android
```

### Out of memory
```bash
export GRADLE_OPTS="-Xmx4g"
make build-android
```

## 📊 GitHub Actions CI/CD

- **Automatic**: Builds on push to `main` or `develop`
- **Manual**: Via "Actions" tab → "Run workflow" → Choose platform
- **Artifacts**: 30-day retention in workflow run
- **All Platforms**: Android, iOS, macOS, Linux, Windows

## 💾 File Sizes (Approximate)

- Android APK: 15-25 MB
- iOS Framework: 20-30 MB
- macOS DMG: 25-40 MB
- Linux DEB/AppImage: 20-32 MB
- Windows EXE/MSI: 25-40 MB

## 📝 Configuration Files

- **local.properties** - Android SDK path (auto-created)
- **gradle.properties** - Gradle settings (already configured)
- **.env** - Secrets and API keys
- **.github/workflows/build-multiplatform.yml** - CI/CD pipeline

## 🎯 Next Steps

1. ✅ Gradle wrapper added (reproducible builds)
2. ✅ Multi-platform build system configured
3. ✅ CI/CD workflow for all platforms
4. ⏭️ Configure Android SDK for CI runner
5. ⏭️ Set up iOS Xcode project
6. ⏭️ Configure code signing for releases
7. ⏭️ Set up release distribution

---

**For detailed information, see:**
- `BUILD_GUIDE.md` - Full build instructions
- `MULTIPLATFORM_SETUP.md` - Project structure
- `Makefile` - All available commands

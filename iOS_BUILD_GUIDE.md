# iOS IPA Generation via GitHub Actions

## 🚀 Status: iOS Build Now Running on GitHub Actions

The iOS app has been successfully set up with a native Xcode project and GitHub Actions workflow to automatically generate the IPA file.

## 📍 Where to Find the iOS IPA

### Option 1: Download from GitHub Actions Artifacts (Recommended)

1. **Go to GitHub Actions Runs**:
   - Navigate to: https://github.com/Let-s-Code-India/NoteVault/actions
   - Select the latest workflow run "Build NoteVault Multi-Platform"

2. **Download iOS IPA**:
   - Look for the artifact named: `notevault-ios-ipa`
   - Click "Download" to get the IPA file(s)
   - The file will be ready for distribution or TestFlight upload

3. **Download Archive (for TestFlight)**:
   - Artifact named: `notevault-ios-ipa`
   - Contains: `.xcarchive` file for TestFlight distribution

### Option 2: Manual Trigger Workflow (Optional)

To manually trigger just the iOS build:

1. Go to GitHub Actions: https://github.com/Let-s-Code-India/NoteVault/actions
2. Click "Build NoteVault Multi-Platform"
3. Click "Run workflow" → Select `ios` from platform dropdown
4. Click "Run workflow" button
5. Wait for completion (usually 10-15 minutes)
6. Download artifacts from that run

## 📦 iOS App Details

**App Name**: NoteVault  
**Bundle ID**: com.letscodeinda.notevault  
**Deployment Target**: iOS 14.0 and later  
**Minimum iOS Version**: 14.0  
**Supported Devices**: iPhone & iPad  
**Author/Organization**: LET'S CODE INDIA

## 🏗️ iOS Project Structure

```
iosApp/
├── iosApp.xcodeproj/           # Xcode project file
│   └── project.pbxproj         # Project configuration
├── iosApp/
│   ├── AppDelegate.swift       # App lifecycle handler
│   ├── SceneDelegate.swift     # Scene lifecycle handler
│   ├── ViewController.swift    # Main view controller
│   ├── Info.plist              # App configuration
│   ├── Assets.xcassets/        # App icons & images
│   └── Base.lproj/
│       ├── Main.storyboard     # UI layout
│       └── LaunchScreen.storyboard  # Launch screen
└── ExportOptions.plist         # Export configuration for IPA
```

## 🔧 Build Configuration

### GitHub Actions Workflow

- **File**: `.github/workflows/build-multiplatform.yml`
- **Runs On**: macOS latest (Xcode environment)
- **Build Method**: `xcodebuild archive` + `xcodebuild export`
- **Signing**: Automatic code signing for ad-hoc distribution
- **Artifacts**: IPA + XCArchive files

### Build Steps

1. Checkout code
2. Set up Xcode environment
3. Build iOS app archive (`.xcarchive`)
4. Export archive to IPA format
5. Upload artifacts to GitHub

## 📲 Using the IPA File

### Installation Methods

**Method 1: TestFlight (Recommended for Testing)**
```bash
# Upload via App Store Connect
# Use Xcode or Transporter tool
```

**Method 2: Direct Installation via AltStore**
```bash
# Requirements: AltStore app on device
# Process:
# 1. Download IPA from GitHub Actions
# 2. Open AltStore on iOS device
# 3. Add IPA file via AltStore
```

**Method 3: Direct Installation via Xcode**
```bash
# Connect iPhone to Mac
xcode-select --install
# Open Devices and Simulators
# Drag IPA to device
```

## ✅ What's Generated

### Artifacts from GitHub Actions

1. **notevault-ios-ipa** - Contains:
   - `iosApp.ipa` - Ready for distribution
   - `iosApp.xcarchive` - For TestFlight upload

2. **notevault-android-publish-bundle** - Contains:
   - Debug & Release APK files

3. **Build Logs** - If build fails

## 🔐 Signing & Distribution

**Current Configuration**: Ad-hoc signing (automatic in CI)

**For Production/TestFlight**:
1. Add Apple Developer Team ID to workflow
2. Configure provisioning profiles
3. Update signing certificate
4. Upload to App Store Connect

**Environment Variables Needed** (for production):
```
APPLE_TEAM_ID
APPLE_DEVELOPER_CERTIFICATE_BASE64
APPLE_DEVELOPER_CERTIFICATE_PASSWORD
APPLE_PROVISIONING_PROFILE_BASE64
```

## 🆘 Troubleshooting

### IPA Not Generated?

1. **Check workflow run**: https://github.com/Let-s-Code-India/NoteVault/actions
2. **View logs**: Click the failed workflow → "Build iOS Framework/IPA" step
3. **Common issues**:
   - Xcode not found (very rare on macOS runners)
   - Project file path incorrect
   - Missing entitlements

### If Build Fails

The workflow has `continue-on-error: true`, so it won't fail the entire workflow. Check:
- Xcode version compatibility
- Swift syntax errors
- Missing files or resources

## 📝 Next Steps

1. **Download IPA**: Visit GitHub Actions artifacts page
2. **Test on device**: Use TestFlight or direct installation
3. **Distribute**: Upload to App Store or use enterprise distribution
4. **Monitor**: Check workflows for build status on each push

## 🎯 Workflow Automation

Every push to `main` branch automatically triggers:
- ✅ Android APK build (Ubuntu runner)
- ✅ iOS IPA build (macOS runner)  
- ✅ macOS desktop build (macOS runner)
- ✅ Artifact upload to GitHub

To skip iOS build for a specific commit, add `[skip-ios]` to commit message.

---

**Last Updated**: 2025-08-16  
**Organization**: LET'S CODE INDIA  
**Repository**: https://github.com/Let-s-Code-India/NoteVault

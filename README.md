# NoteVault

A powerful, offline-first note-taking and visual planning application, built as **two fully native apps from one repository** — Android (Kotlin + Jetpack Compose) and iOS (Swift + SwiftUI) — combining traditional note management with advanced features like a Logic Board diagram canvas, task management, reminders, and full-text search, all while keeping your data private through local encryption.

## 🎓 Why this repo exists

NoteVault doubles as a teaching example for one specific idea: **you do not need a laptop, a Mac, Android Studio, or Xcode to build a real Android + iOS app.** Every commit to this repo is compiled, packaged, and turned into installable `.apk` and `.ipa` files entirely on GitHub's own servers, using nothing but the free GitHub Actions minutes every account gets — you can do all of this from the GitHub app on a phone.

If you're reading this repo to learn:
- **`.github/workflows/build-multiplatform.yml`** is the whole pipeline — read it top to bottom, it's the actual lesson.
- **["Building Without a Laptop"](#-building-without-a-laptop-github-actions)** below walks through the exact clicks.
- Both platforms build **unsigned** by default (`CODE_SIGNING_ALLOWED=NO` on iOS, a debug keystore fallback on Android) specifically so a brand-new GitHub account with zero paid developer accounts can still produce a working, installable build on day one.
- The repo is intentionally kept to **zero build errors and zero build warnings** on both platforms (two narrow, documented exceptions below) — so if you fork this and suddenly see red, you introduced something, and the diff will show you exactly what.

## 📋 Features

Every feature below works identically on **both Android and iOS** — this is not an Android app with an iOS placeholder; both platforms ship the full experience.

### Core Note Management
- **Full-Featured Notes**: Create, edit, and organize notes with rich formatting
- **Folder Organization**: Organize notes into folders for better structure
- **Tags & Categorization**: Tag notes for flexible organization and discovery
- **Full-Text Search**: Search across all notes, including folder and tag metadata
- **Offline-First Architecture**: All data stored locally; network connectivity optional

### Logic Board Diagram Canvas
- **Visual Diagram Editor**: Create flowcharts, decision trees, roadmaps, and structured diagrams
- **Multiple Template Types**: FREEFORM, DECISION_TREE, FLOWCHART, ROADMAP, MIND_MAP, ORG_CHART, SWIMLANE
- **Freehand Drawing Support**: Draw custom shapes and connections
- **Template-Based Diagram Generation**: Instantly generate a starter diagram layout from any template — fully offline, zero network calls
- **Node & Edge Customization**: Fully customizable colors, styles, and layouts

### Design Canvas *(Android)*
- **Image Editing**: Built-in image editor for creating and modifying designs
- **Freehand Drawing**: Draw directly on your images
- **Undo/Redo Support**: Full undo/redo stack for editing operations

### Task Management
- **Create & Track Tasks**: Manage to-do items with completion tracking
- **Task Organization**: Link tasks to notes and folders
- **Visual Task Status**: Track task progress at a glance

### Reminders & Scheduling
- **Natural Language Parsing** *(Android)*: Parse reminders from natural language (e.g., "Pay rent tomorrow at 9am")
- **Local Notifications** *(iOS)*: Scheduled via `UNUserNotificationCenter`, survive app restarts
- **Flexible Scheduling**: One-time and recurring reminders
- **Boot-Time Persistence** *(Android)*: Reminders survive device reboots

### Data Security & Privacy
- **Biometric Authentication**: Fingerprint/Face unlock on Android, Face ID/Touch ID on iOS
- **Encrypted Database**: SQLCipher + AES on Android, CryptoKit (AES-GCM) + Keychain on iOS
- **Local-Only Storage**: No cloud sync; complete privacy control
- **No Telemetry**: Your data stays on your device

### Data Management *(Android)*
- **Export/Backup**: Export notes and data for backup and portability
- **Multiple Export Formats**: Support for various export formats
- **Restore Functionality**: Restore from exported backups

## 🛠️ Tech Stack

### Android

**Language & Framework**
- **Kotlin** — modern, concise language with null safety
- **Jetpack Compose** — modern declarative UI framework

**Data & Storage**
- **Room** — type-safe database abstraction over SQLite
- **SQLCipher** — encrypted local database
- **DataStore Preferences** — encrypted shared preferences replacement

**Architecture & UI**
- **MVVM** — Model-View-ViewModel architecture pattern
- **Navigation Compose** — type-safe navigation with Compose
- **Lifecycle** — Android lifecycle-aware components
- **Coil** — image loading and caching library

**Local Data & Serialization**
- **Moshi** — local JSON serialization for diagram node/edge data (on-device only, never transmitted)

**Security & Authentication**
- **Biometric** — biometric authentication support
- **Android Security Crypto** — encrypted preferences
- **Firebase App Check** — protect backend APIs from abuse (bundled but unused by default; safe to remove if you don't add Firebase-backed services)

**Testing**
- **JUnit 4, Robolectric, Roborazzi, Espresso, Coroutines Test**

**Build & Tools**
- **Gradle (Kotlin DSL)** — build automation
- **Firebase Services Plugin** — set to `WARN` instead of `FAIL` when no config file is present, so the build never breaks if you skip Firebase entirely
- **KSP (Kotlin Symbol Processing)** — compilation-time code generation

### iOS

**Language & Framework**
- **Swift** — native language, no bridging layer
- **SwiftUI** — declarative UI, `@main App` lifecycle (no storyboard-based app delegate)

**Data & Storage**
- **Core Data** — with a fully **programmatic `NSManagedObjectModel`** (built in code in `PersistenceController.swift`), so the project needs no `.xcdatamodeld` file
- **Keychain Services** — secure storage for the local database encryption key
- **CryptoKit (AES-GCM)** — field-level encryption via `EncryptionManager`

**Architecture & UI**
- **MVVM** — Model → Repository → ViewModel → View, mirroring the Android architecture 1:1 so the two codebases stay easy to compare
- **NavigationStack / TabView** — native SwiftUI navigation

**Security & Authentication**
- **LocalAuthentication** — Face ID / Touch ID via `BiometricAuthManager`
- **UserNotifications** — local reminder notifications via `NotificationManager`

**Build & Tools**
- **Xcode project (`.xcodeproj`)** — checked into the repo; no CocoaPods/SPM dependencies, so `xcodebuild` works on a stock GitHub-hosted macOS runner with no extra setup step
- **iOS 17.0+** deployment target

## 📦 Project Structure

```
NoteVault/
├── app/                                  # Android application module
│   ├── build.gradle.kts                 # App-level dependencies and configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt      # Main application entry point
│   │   │   │   ├── data/                # Data layer
│   │   │   │   │   ├── db/             # Room database, DAOs
│   │   │   │   │   ├── model/          # Entity models
│   │   │   │   │   ├── repository/     # Repository pattern implementations
│   │   │   │   │   └── security/       # Encryption and security utilities
│   │   │   │   ├── export/             # Export/backup functionality
│   │   │   │   ├── platform/           # Platform-specific code (permissions, settings)
│   │   │   │   ├── reminder/           # Reminder scheduling and management
│   │   │   │   └── ui/                 # UI layer (Compose screens)
│   │   │   │       ├── canvas/         # Logic Board diagram canvas
│   │   │   │       ├── components/     # Reusable UI components
│   │   │   │       ├── imageeditor/    # Design canvas
│   │   │   │       ├── legal/          # Legal/consent screens
│   │   │   │       ├── notes/          # Notes management screens
│   │   │   │       ├── reminder/       # Reminder scheduling UI
│   │   │   │       ├── settings/       # Settings screens
│   │   │   │       ├── tasks/          # Task management UI
│   │   │   │       ├── theme/          # Theme and styling
│   │   │   │       └── viewmodel/      # MVVM ViewModels
│   │   │   └── res/                    # Android resources (drawables, strings, colors)
│   │   ├── androidTest/                # Instrumented Android tests
│   │   └── test/                       # Unit tests
│   └── proguard-rules.pro              # ProGuard/R8 obfuscation rules
├── gradle/                              # Gradle configuration
│   └── libs.versions.toml              # Centralized dependency version management
├── branding/                             # App icon source
│   └── notevault-icon.svg               # Master vector icon (also used for Android + iOS launcher art)
├── iosApp/                              # iOS application (Xcode project)
│   ├── iosApp.xcodeproj/               # Xcode project file
│   ├── ExportOptions.plist             # Export options for archiving (unsigned CI builds)
│   └── iosApp/
│       ├── App/                        # @main SwiftUI App entry point + app-wide state
│       ├── Core/
│       │   ├── Diagrams/               # Offline template-based diagram generator
│       │   ├── Extensions/             # Color/Date helpers shared across views
│       │   ├── Notifications/          # Local reminder scheduling
│       │   └── Security/               # Biometric auth, Keychain, encryption
│       ├── Models/Domain/              # Note, Folder, Tag, TaskItem, LogicBoard, etc.
│       ├── Persistence/                # Core Data model (built in code) + entities + DAO
│       ├── Repositories/               # Repository layer (mirrors Android's repository/)
│       ├── ViewModels/                 # MVVM ViewModels (mirrors Android's viewmodel/)
│       ├── Views/                      # SwiftUI screens, grouped the same way as Android's ui/
│       │   ├── FoldersAndTags/
│       │   ├── LogicBoard/
│       │   ├── Navigation/
│       │   ├── Notes/
│       │   ├── Search/
│       │   ├── Security/
│       │   └── Tasks/
│       ├── Assets.xcassets/            # App icon (AppIcon) & accent color (AccentColor)
│       ├── Base.lproj/                 # LaunchScreen storyboard (splash only — app itself is pure SwiftUI)
│       └── Info.plist
├── scripts/
│   └── setup-ios.sh                    # Local iOS environment bootstrap helper (optional — CI needs none of this)
├── .github/workflows/
│   └── build-multiplatform.yml         # CI: builds Android APKs + iOS IPA — read this to learn the whole pipeline
├── build.gradle.kts                    # Root build configuration
├── settings.gradle.kts                 # Gradle settings
├── gradle.properties                   # Gradle properties
├── .env.example                        # Example environment variables
├── LICENSE                             # MIT License
└── README.md                           # This file
```

## 🚀 Getting Started

### Prerequisites (only if building locally — see below for the no-tools path)
- **Android Studio**: Latest version (Android Studio Hedgehog or newer), Android SDK API 24+
- **Xcode**: 16 or newer, iOS 17.0+ SDK (macOS only)

### Setup & Build (Android, locally)

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd NoteVault
   ```
2. **Open in Android Studio** — select **Open**, choose the NoteVault directory, let it sync.
3. **Run** — connect a device or emulator, `Shift + F10`.

```bash
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK
./gradlew test               # Run unit tests
./gradlew connectedAndroidTest
./gradlew roborazzi          # Screenshot tests
```

### Setup & Build (iOS, locally — requires a Mac)

```bash
open iosApp/iosApp.xcodeproj
```
Select the `iosApp` scheme and a simulator or device, then `Cmd+R`. No CocoaPods install, no SPM resolution step — the project has zero external dependencies.

### 🌍 Building Without a Laptop (GitHub Actions)

This is the part that matters most if you're here to learn. **This repo builds itself on GitHub's own servers — you never need Android Studio, Xcode, or even a laptop.** Every step below can be done from the GitHub app on a phone.

1. Push any change to the `main` branch (or open the repo → **Actions** tab →
   **Build NoteVault - Android & iOS** → **Run workflow**).
2. GitHub spins up a Linux machine (for Android) and a macOS machine (for iOS) —
   for free, on GitHub's own hardware — and runs
   `.github/workflows/build-multiplatform.yml` on both, in parallel.
3. Open the workflow run → **Collect All Build Artifacts** job → scroll to
   **Artifacts** at the bottom.
4. Download `notevault-release-package` — it contains:
   - `android/app-debug.apk` and `android/app-release.apk` — install directly on
     an Android device.
   - `ios/iosApp.ipa` — an **unsigned** iOS build. Re-sign it with a real Apple
     Developer certificate (via Xcode, Sideloadly, AltStore, Feather, etc.) before
     installing on a physical iPhone/iPad.

**Note on the iOS job**: it archives with `CODE_SIGNING_ALLOWED=NO`, so it does
not require an Apple Developer account or any signing secrets in the repo. The
job packages the built `.app` bundle into a standard `Payload/iosApp.app` zip
(`.ipa` format) itself, since `xcodebuild -exportArchive` needs a real signing
identity that CI runners don't have. If you don't have a paid Apple Developer
account, third-party re-signing tools can sign this unsigned `.ipa` with a free
Apple ID for on-device testing (free-tier signing typically expires after 7
days — a paid account signs for a full year).

**Two messages you will see in the logs that are expected, not bugs:**
- An `AWT-EventQueue` exception from the Kotlin Symbol Processing (KSP) tool
  during the Android build — this is a background-thread message from the
  compiler plugin itself, unrelated to your code, and does not fail the build.
- A "Node.js 20 is deprecated" warning on the artifact-collection job — this
  comes from GitHub forcing a newer Node runtime on an action that still
  declares Node 20 in its own metadata. It's on GitHub's side, not fixable
  from this repo, and disappears once the upstream action is updated.

Anything in the logs beyond those two is a real problem worth fixing — that's
the whole point of keeping this repo at zero otherwise.

## 🔐 Security Features

- **Biometric Authentication**: Fingerprint/face unlock (Android), Face ID/Touch ID (iOS)
- **Database Encryption**: SQLCipher (Android), CryptoKit AES-GCM + Keychain (iOS)
- **Secure Preferences**: Encrypted storage for sensitive settings on both platforms
- **Fully Offline**: No network permission requested at all on Android (`INTERNET` is not in the manifest); no networking code anywhere on iOS. Every feature, including diagram generation, runs entirely on-device.
- **No Cloud Storage**: All data remains local for maximum privacy

## 📝 Legal & Licensing

**Copyright © 2026 LET'S CODE INDIA. All rights reserved.**

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

**Publisher/Organization**: LET'S CODE INDIA

### Third-Party Libraries

Android dependencies are listed in `gradle/libs.versions.toml`. The iOS app has **zero external dependencies** — everything is built on Apple's own frameworks (SwiftUI, Core Data, CryptoKit, LocalAuthentication, UserNotifications).

## 🤝 Contributing

NoteVault is maintained by **LET'S CODE INDIA** as a learning resource for developers who want to see a real, working example of shipping to both app stores' platforms without owning a Mac or a dedicated dev machine. Reading the workflow file, opening issues, and submitting improvements are all welcome — that's the point.

## 📞 Support

For issues, feature requests, or general questions about NoteVault, please open an issue in the project repository.

## 📚 Additional Resources

- **Android Documentation**: https://developer.android.com/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Room Database**: https://developer.android.com/training/data-storage/room
- **Kotlin Language**: https://kotlinlang.org/
- **Swift Language**: https://www.swift.org/documentation/
- **SwiftUI**: https://developer.apple.com/documentation/swiftui/
- **Core Data**: https://developer.apple.com/documentation/coredata
- **GitHub Actions**: https://docs.github.com/actions

---

**Last Updated**: August 2026 — Replaced the iOS placeholder wrapper with a full native SwiftUI app (Core Data, Keychain, CryptoKit, Face ID, local notifications) sharing feature parity with Android; removed the old `AppDelegate`/`SceneDelegate`/storyboard-based app lifecycle in favor of a pure SwiftUI `@main App`; then removed the optional AI-assisted diagram feature entirely on both platforms (including the `INTERNET` permission on Android) so NoteVault is a fully offline, zero-network app end to end — diagram generation is now purely template-based on both platforms.

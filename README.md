# NoteVault

A powerful, offline-first note-taking and visual planning application built with modern Android and Kotlin technologies. NoteVault combines traditional note management with advanced features like Logic Board diagram canvas, task management, reminders, and full-text search—all while maintaining complete privacy through local encryption.

## 📋 Features

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
- **AI-Assisted Diagram Generation**: Optionally generate diagrams from natural language prompts via Gemini API (requires API key)
- **Node & Edge Customization**: Fully customizable colors, styles, and layouts

### Design Canvas
- **Image Editing**: Built-in image editor for creating and modifying designs
- **Freehand Drawing**: Draw directly on your images
- **Undo/Redo Support**: Full undo/redo stack for editing operations

### Task Management
- **Create & Track Tasks**: Manage to-do items with completion tracking
- **Task Organization**: Link tasks to notes and folders
- **Visual Task Status**: Track task progress at a glance

### Reminders & Scheduling
- **Natural Language Parsing**: Parse reminders from natural language (e.g., "Pay rent tomorrow at 9am")
- **Flexible Scheduling**: One-time and recurring reminders
- **Multiple Reminder Types**: Support for various scheduling patterns
- **Boot-Time Persistence**: Reminders survive device reboots

### Data Security & Privacy
- **Biometric Authentication**: Secure access via fingerprint or face recognition
- **Encrypted Database**: All data encrypted using SQLCipher with AES encryption
- **Local-Only Storage**: No cloud sync; complete privacy control
- **No Telemetry**: Your data stays on your device

### Data Management
- **Export/Backup**: Export notes and data for backup and portability
- **Multiple Export Formats**: Support for various export formats
- **Restore Functionality**: Restore from exported backups

## 🛠️ Tech Stack

### Language & Framework
- **Kotlin**: Modern, concise language with null safety
- **Jetpack Compose**: Modern declarative UI framework

### Data & Storage
- **Room**: Type-safe database abstraction over SQLite
- **SQLCipher**: Encrypted local database
- **DataStore Preferences**: Encrypted shared preferences replacement

### Architecture & UI
- **MVVM**: Model-View-ViewModel architecture pattern
- **Navigation Compose**: Type-safe navigation with Compose
- **Lifecycle**: Android lifecycle-aware components
- **Coil**: Image loading and caching library

### Networking & API Integration
- **Retrofit**: Type-safe HTTP client
- **OkHttp**: HTTP client with logging and interceptors
- **Moshi**: JSON serialization/deserialization
- **Firebase Generative AI**: Optional integration with Gemini API for diagram generation

### Security & Authentication
- **Biometric**: Biometric authentication support
- **Android Security Crypto**: Encrypted preferences
- **Firebase App Check**: Protect backend APIs from abuse

### Testing
- **JUnit 4**: Unit testing framework
- **Robolectric**: Android unit testing
- **Roborazzi**: Screenshot testing with Compose
- **Espresso**: UI testing framework
- **Coroutines Test**: Coroutine testing utilities

### Build & Tools
- **Gradle (Kotlin DSL)**: Build automation
- **Google Services Plugin**: Firebase integration
- **Google Secrets Plugin**: Secure environment variable management
- **KSP (Kotlin Symbol Processing)**: Compilation-time code generation

## 📦 Project Structure

```
NoteVault/
├── app/                                  # Main Android application module
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
│   │   ├── test/                       # Unit tests
│   │   ├── desktopMain/               # Desktop platform-specific code
│   │   └── iosMain/                   # iOS platform-specific code
│   └── proguard-rules.pro              # ProGuard/R8 obfuscation rules
├── gradle/                              # Gradle configuration
│   └── libs.versions.toml              # Centralized dependency version management
├── build.gradle.kts                    # Root build configuration
├── settings.gradle.kts                 # Gradle settings
├── gradle.properties                   # Gradle properties
├── .env.example                        # Example environment variables
├── LICENSE                             # MIT License
└── README.md                           # This file
```

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Latest version (Android Studio Hedgehog or newer)
- **Android SDK**: Minimum API level 24, target API level 36
- **Kotlin**: 2.2.10 or newer (managed by Gradle)

### Setup & Build

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd NoteVault
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select **Open** and choose the NoteVault directory
   - Allow Android Studio to synchronize dependencies and fix any incompatibilities

3. **Configure Environment Variables** (optional, for Gemini API)
   - Create a `.env` file in the project root (copy from `.env.example`)
   - Set `GEMINI_API_KEY` to your Gemini API key (get one at https://aistudio.google.com/app/apikey)
   - This is only needed if you want to use the AI-assisted diagram generation feature

4. **Run the Application**
   - Connect an Android device (physical or emulator) via ADB
   - Press `Shift + F10` in Android Studio, or select **Run** → **Run 'app'**
   - The app will compile, install, and launch on your device

### Build Variants

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run screenshot tests
./gradlew roborazzi
```

### Building Without a Laptop (GitHub Actions)

This repo builds itself on GitHub's servers — you don't need Android Studio, Xcode,
or even a laptop. Everything below can be done from the GitHub app on your phone.

1. Push any change to the `main` branch (or open the repo → **Actions** tab →
   **Build NoteVault - Android & iOS** → **Run workflow**).
2. GitHub spins up a Linux machine (for Android) and a macOS machine (for iOS) and
   runs `.github/workflows/build-multiplatform.yml` on both.
3. Open the workflow run → **Collect All Build Artifacts** job → scroll to
   **Artifacts** at the bottom.
4. Download `notevault-release-package` — it contains:
   - `android/app-debug.apk` and `android/app-release.apk` — install directly on
     an Android device.
   - `ios/iosApp.ipa` — an **unsigned** iOS build. Re-sign it with a real Apple
     Developer certificate (via Xcode, Sideloadly, AltStore, etc.) before
     installing on a physical iPhone.

No local build tools required — the whole pipeline runs in the cloud.

## 🔐 Security Features

- **Biometric Authentication**: Fingerprint and face recognition support
- **Database Encryption**: All local data encrypted with SQLCipher
- **Secure Preferences**: Encrypted storage for sensitive settings
- **Optional API Key Security**: Gemini API key managed via environment variables
- **No Cloud Storage**: All data remains local for maximum privacy

## 🌐 Optional: Gemini API Integration

NoteVault includes an optional feature to generate diagrams using Google's Gemini API. This feature is entirely optional and only contacts the network when you explicitly click "Generate AI Diagram."

**What Data is Sent**: Only your diagram prompt text is sent to the Gemini API.
**What Data is NOT Sent**: Your notes, vault contents, personal identifiers, or any other application data.
**Privacy**: If you don't use this feature, zero network requests are made.

To enable this feature:
1. Get a free Gemini API key at https://aistudio.google.com/app/apikey
2. Add it to your `.env` file: `GEMINI_API_KEY=your_actual_api_key`
3. Rebuild the app

## 📝 Legal & Licensing

**Copyright © 2026 LET'S CODE INDIA. All rights reserved.**

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

**Publisher/Organization**: LET'S CODE INDIA

### Third-Party Libraries

NoteVault uses several open-source libraries. See `gradle/libs.versions.toml` for the complete list of dependencies and their respective licenses.

## 🤝 Contributing

This is a private project. For questions, issues, or contributions, please contact the maintainers directly.

## 📞 Support

For issues, feature requests, or general questions about NoteVault, please open an issue in the project repository (if public) or contact the development team.

## 📚 Additional Resources

- **Android Documentation**: https://developer.android.com/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Room Database**: https://developer.android.com/training/data-storage/room
- **Kotlin Language**: https://kotlinlang.org/
- **Gemini API**: https://ai.google.dev/

---

**Last Updated**: August 2026

# 👗 Digital Wardrobe (Virtual Outfit Creator)

A native Android application that allows users to digitize their wardrobe and virtually try on different clothing combinations using AI-powered computer vision.

## ✨ Features (Planned)

- **User Authentication** - Secure login with Firebase
- **Wardrobe Digitization** - Upload and tag clothing items
- **AI Segmentation** - Automatic clothing isolation using TensorFlow Lite
- **Virtual Try-On** - Overlay clothes onto your body photo
- **Outfit Generation** - Smart outfit combinations based on compatibility rules
- **Save & Share** - Keep favorite outfits and share with friends

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material Design 3
- **Architecture:** MVVM
- **Backend:** Firebase (Auth, Firestore, Cloud Storage)
- **AI/ML:** TensorFlow Lite (on-device)
- **Min SDK:** API 26 (Android 8.0 Oreo)
- **Target SDK:** API 35

## 📁 Project Structure

```
DigitalWardrobe/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/digitalwardrobe/
│   │   │   │   ├── ui/theme/        # Compose theme (colors, typography)
│   │   │   │   ├── MainActivity.kt  # Entry point
│   │   │   │   └── DigitalWardrobeApp.kt
│   │   │   ├── res/                 # Resources (layouts, strings, drawables)
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                    # Unit tests
│   │   └── androidTest/             # Instrumentation tests
│   ├── build.gradle.kts             # App-level Gradle config
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml           # Version catalog
│   └── wrapper/
├── build.gradle.kts                 # Project-level Gradle config
├── settings.gradle.kts
├── gradle.properties
├── pr.md                            # Project Requirements Document
└── TASKS.md                         # Task tracking
```

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 17** or higher
- **Android SDK** with API 35 installed

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/DigitalWardrobe.git
   cd DigitalWardrobe
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `DigitalWardrobe` folder
   - Let Android Studio sync Gradle (it will download the Gradle wrapper automatically)

3. **Sync Gradle**
   - Android Studio should automatically prompt you to sync
   - If not, click **File → Sync Project with Gradle Files**

4. **Run the app**
   - Connect an Android device or start an emulator (API 26+)
   - Click the **Run** button or press `Shift + F10`

### Important Notes

- The `gradle-wrapper.jar` binary will be downloaded automatically when you first open the project in Android Studio
- Firebase configuration (`google-services.json`) will be added in Task 1.2

## 📋 Development Progress

See [TASKS.md](TASKS.md) for detailed progress tracking.

| Phase | Status |
|-------|--------|
| Phase 1: Setup & Authentication | 🔄 In Progress |
| Phase 2: Image Capture & Storage | ⬜ Not Started |
| Phase 3: AI Core Integration | ⬜ Not Started |
| Phase 4: Visualization & Logic | ⬜ Not Started |
| Phase 5: Testing & Launch | ⬜ Not Started |

## 📄 License

This project is for personal/educational use.

## 📞 Contact

For questions about this project, please refer to the project requirements in [pr.md](pr.md).

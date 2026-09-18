# Sleep Tracker 💤

A modern, low-power Android application for tracking sleep duration, built with the latest Jetpack Compose and Material 3 guidelines.

## 🚀 Features

- **Duration-based Tracking**: Start and stop sleep sessions manually or via notification controls.
- **Health Connect Integration**: Seamlessly sync your sleep data with Android's Health Connect ecosystem.
- **Bedtime Reminders**: Set scheduled reminders to maintain a consistent sleep routine.
- **Room Persistence**: Local database storage for all your sleep history.
- **Modern UI**: Fully declarative UI built with Jetpack Compose and Material 3.
- **Privacy First**: All data is stored locally and only shared with Health Connect if explicitly enabled.

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Preferences**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Health Data**: [Health Connect Client](https://developer.android.com/health-connect)
- **Testing**:
  - Unit tests with [Robolectric](http://robolectric.org/)
  - Screenshot testing with [Roborazzi](https://github.com/takahirom/roborazzi)
  - Coverage reporting with [Kover](https://github.com/Kotlin/kotlinx-kover)

## 📥 Installation & Updates

### Using Obtainium (Recommended)

To stay updated with the latest versions, we recommend using **[Obtainium](https://github.com/ImranR98/Obtainium)**.

1. Install Obtainium on your Android device.
2. Click **Add App**.
3. Paste this repository's URL: `https://github.com/felixbrucker/simple-sleep-tracker`
4. Obtainium will notify you and help you install updates automatically whenever a new build is available on GitHub.

### Manual Download

You can find the latest APKs in the [Releases](https://github.com/felixbrucker/simple-sleep-tracker/releases) section.

## 🏗 Development

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 37
- JDK 25

### Build Commands
```bash
# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Generate coverage report
./gradlew koverHtmlReport
```

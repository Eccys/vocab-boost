# Installation Guide

This guide will walk you through the process of installing Vocab on your Android device. Vocab is compatible with Android 7.0 (Nougat) and above.

## Option 1: Install from Google Play Store (Recommended)

1. Open the Google Play Store on your Android device
2. Search for "Vocab - Boost Your Vocabulary"
3. Tap the "Install" button
4. Wait for the installation to complete
5. Launch the app from your home screen or app drawer

## Option 2: Direct APK Installation

If you prefer to install the app manually or Google Play is not accessible:

1. On your Android device, go to **Settings > Security** (or **Settings > Privacy**)
2. Enable **Install from Unknown Sources** or **Install Unknown Apps** (the exact wording may vary depending on your device)
3. Download the latest APK file from our [GitHub Releases page](https://github.com/Eccys/vocab-boost/releases)
4. Once downloaded, tap on the APK file from your notifications panel or file manager
5. Follow the on-screen instructions to complete the installation
6. After installation, you can disable "Install from Unknown Sources" for security

## Option 3: Build from Source

For developers who want to build the app from source:

### Prerequisites
- Android Studio Arctic Fox (2021.3.1) or newer
- JDK 11 or newer
- Git

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/Eccys/vocab-boost.git
   ```

2. Open the project in Android Studio:
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository and select it

3. Sync project with Gradle files:
   - Android Studio should automatically sync the project
   - If not, go to File > Sync Project with Gradle Files

4. Build the project:
   - Go to Build > Make Project
   - Alternatively, use the keyboard shortcut Ctrl+F9 (Windows/Linux) or Cmd+F9 (macOS)

5. Run the app:
   - Connect your Android device via USB with USB debugging enabled
   - Go to Run > Run 'app'
   - Select your device from the list and click OK

## Setting Up Firebase (Optional)

Vocab uses Firebase for cloud synchronization and analytics. To enable these features:

1. Create a Firebase project at [firebase.google.com](https://firebase.google.com/)
2. Add an Android app to your Firebase project
3. Download the `google-services.json` file
4. Place the file in the app directory of the project
5. Ensure the Firebase dependencies are properly configured in the build.gradle files

## Troubleshooting Installation Issues

### Common Issues

#### "App not installed" error
- Make sure you have enough storage space on your device
- Check that you're installing the correct version for your Android OS
- Try restarting your device and attempt the installation again

#### Crashes after installation
- Check that your device meets the minimum requirements (Android 7.0+)
- Try clearing the app data and cache through your device settings
- Make sure you have the latest version of the app

For further assistance, please check the [Troubleshooting](Troubleshooting) page or contact our support team at help.vocabboost@gmail.com. 
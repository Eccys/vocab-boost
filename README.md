# Vocab - Vocabulary Booster App

## Overview

Vocab is a feature-rich, fully open source Android application designed to help users enhance their vocabulary through daily learning and interactive quizzes. The app combines spaced repetition learning techniques with engaging quizzes to make vocabulary acquisition effective and enjoyable.

![Vocab App](v-bold.svg)

## Key Features

- **100% Open Source**: Full transparency with all code available for review, modification, and contribution
- **Daily Word**: Learn a new vocabulary word every day with comprehensive definitions, examples, and usage contexts
- **Interactive Quizzes**: Test your knowledge with customizable quizzes based on your learning history
- **Spaced Repetition Learning**: Advanced algorithm that optimizes vocabulary retention by scheduling reviews at optimal intervals
- **Progress Tracking**: Detailed statistics to monitor your learning progress over time
- **Cross-Device Synchronization**: Seamlessly sync your progress, bookmarks, and settings across multiple devices
- **Bookmarks**: Save favorite words for quick access and additional study
- **Multiple Synonyms**: Each word comes with multiple synonyms to broaden vocabulary understanding
- **Offline Mode**: Full functionality available without internet connection

## Installation

### Requirements
- Android 8.0 (API level 26) or higher
- Minimum 100MB of free storage space

### Download Options
- [Download from Google Play Store](#) (link will be added when published)
- Install the provided APK file directly:
  - Vocab-latest.apk
  - Enable "Install from Unknown Sources" in your device settings if installing via APK

## Documentation

### Wiki
For comprehensive documentation, visit our [Wiki](https://github.com/Eccys/vocab-boost/wiki) which includes:
- [Installation Guide](https://github.com/Eccys/vocab-boost/wiki/Installation-Guide)
- [User Interface Guide](https://github.com/Eccys/vocab-boost/wiki/User-Interface)
- [Spaced Repetition System](https://github.com/Eccys/vocab-boost/wiki/Spaced-Repetition-System)
- [Developer Guide](https://github.com/Eccys/vocab-boost/wiki/Developer-Guide)
- [API Integration](https://github.com/Eccys/vocab-boost/wiki/API-Integration)
- [FAQ](https://github.com/Eccys/vocab-boost/wiki/FAQ)

### Additional Resources
- [Official Website](https://vocab.ecys.xyz)
- [Version History](VERSION_HISTORY.md)

## Detailed Features

### Daily Word
The app presents a new word each day with:
- Pronunciation guide
- Comprehensive definition
- Example sentences showing proper usage
- Word category (noun, verb, adjective, etc.)
- Usage context
- Additional "Did You Know" information

### Quiz System
- **Multiple quiz types**: Definition matching, synonym identification, fill-in-the-blank
- **Customizable difficulty**: Easy, Medium, Hard options
- **Quiz history**: Review past performance with detailed analysis
- **Smart question generation**: Questions adapt based on your past performance

### Learning Algorithm
Vocab implements a modified version of the SuperMemo-2 spaced repetition algorithm that:
- Adjusts review intervals based on your performance
- Prioritizes words you find challenging
- Optimizes long-term retention through strategically timed reviews

### Statistics and Progress Tracking
- Daily usage time tracking
- Word mastery percentage
- Performance trends over time
- Streak counters to encourage regular use

### User Account System
- **Cloud synchronization**: Automatically sync your learning progress, quiz history, bookmarks, and settings across all your devices
- **Real-time updates**: Changes made on one device immediately reflect on others when connected
- **Conflict resolution**: Smart merging of data when changes are made on multiple devices while offline
- **Secure authentication**: Firebase-based authentication system with email/password and social login options
- **Backup and restore**: Manual backup options for additional peace of mind
- **Settings synchronization**: Preferences and customizations follow you to any device

## Usage Guide

### Getting Started
1. **First Launch**: Create an account or use the app in offline mode
2. **Daily Word**: Check the daily word from the main screen
3. **Take a Quiz**: Start with a beginner quiz to get familiar with the format
4. **Review Progress**: Check your statistics after completing quizzes

### Quiz Best Practices
- Take regular quizzes to reinforce learning
- Review incorrect answers to improve retention
- Use bookmarks for words you want to focus on

### Settings Customization
- Adjust notification preferences for daily reminders
- Customize quiz difficulty and length
- Set learning goals and daily targets

## Technical Information

### Built With
- Kotlin - Primary programming language
- Jetpack Compose - Modern UI toolkit
- Room Database - Local data storage
- Firebase - Cloud synchronization and authentication
- Material 3 Design - UI components and styling

### Architecture
- MVVM (Model-View-ViewModel) architecture
- Repository pattern for data management
- Coroutines for asynchronous operations

## Privacy and Data Usage

Vocab respects user privacy:
- Optional account creation
- Local storage of learning data by default
- Optional cloud synchronization
- No third-party analytics or tracking

## Support and Feedback

- Report issues via email: help.vocabboost@gmail.com

## Open Source Contribution

Vocab is proudly open source! We welcome contributions from developers of all skill levels.

### How to Contribute
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

### Development Setup
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the app in an emulator or physical device

See our [CONTRIBUTING.md](CONTRIBUTING.md) file for detailed guidelines.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. Being fully open source, you're free to use, modify, and distribute the code following the terms of the license.

## Acknowledgments

- Special thanks to all contributors and testers
- Word definitions sourced from open linguistic databases
- Icon and design elements created by our talented design team 

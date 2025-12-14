# Skywriter

<div align="center">

<img src="skywriter_full_logo.png" alt="Skywriter Logo" width="600"/>

**A beautiful Android app for writing character NFC data to physical tags**

[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org/)
[![Material Design 3](https://img.shields.io/badge/Material%20Design-3-blue.svg)](https://m3.material.io/)

</div>

Skywriter is a modern Android application that allows you to browse characters organized by game series, select them visually, and write their NFC data to physical Mifare Classic tags using your Android device's NFC capabilities. Perfect for creating custom character tags compatible with Skylanders portal devices.

## ✨ Features

### Core Functionality
- **📱 Browse Characters**: View all characters organized by game series with collapsible headers
- **🔍 Smart Search**: Optional search functionality to quickly find specific characters
- **📝 Character Details**: View detailed information about each character including UID and game series
- **💾 NFC Writing**: Write character data to Mifare Classic 1K tags with full portal compatibility
- **✅ Tag Compatibility Checker**: Standalone tool to verify tag compatibility before writing
- **🔐 Automatic Authentication**: Extracts and uses authentication keys from source data
- **📊 Real-time Feedback**: Progress indicators and detailed status messages

### User Experience
- **🎨 Beautiful UI**: Clean white background with navy blue accents (Material Design 3)
- **📱 Modern Design**: Collapsible game sections, intuitive navigation, and smooth animations
- **🛡️ Safety First**: User-initiated actions only - tap buttons before any NFC operations
- **⚠️ Smart Warnings**: Compatibility checker warns about potential issues before writing
- **🔄 Error Handling**: Graceful handling of locked tags, authentication failures, and incompatible tags

### Technical Excellence
- **🏗️ MVVM Architecture**: Clean separation of concerns with ViewModels and LiveData
- **🧪 Comprehensive Testing**: Unit tests, instrumented tests, and E2E test coverage
- **📦 Portal Compatible**: Writes ALL blocks including sector trailers for full compatibility
- **🔑 Key Extraction**: Automatically extracts authentication keys from source data
- **📱 Multi-density Support**: Optimized icons and resources for all screen densities

## 🎨 Design

Skywriter features a clean, modern design with:
- **White Background**: Easy on the eyes, professional appearance
- **Navy Blue Accents**: Primary color (#001F3F) for buttons, headers, and highlights
- **Material Design 3**: Latest Material Design components and patterns
- **Custom Branding**: Skywriter logo in app icon and header

## 📸 Screenshots

*Screenshots coming soon*

## 🚀 Quick Start

### Prerequisites

- **Android Device**: Android 7.0 (API 24) or higher with NFC support
- **NFC Tags**: Mifare Classic 1K compatible tags
- **Android Studio**: Latest version recommended (for development)

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/BitcoinErrorLog/skywriter.git
   cd skywriter
   ```

2. **Prepare Character Data**:
   - Convert `.nfc` files to JSON format using the conversion script from the FlipperSkylanders repository
   - Place JSON files in `app/src/main/assets/Android_NFC_Data/` maintaining directory structure
   - The app will automatically load all characters on first launch

3. **Build the APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be in `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on Device**:
   - Transfer the APK to your Android device
   - Enable "Install from Unknown Sources" if needed
   - Install and launch the app

## 📖 Usage Guide

### Browsing Characters

1. **Launch the app** - You'll see all characters organized by game series
2. **Expand/Collapse Games** - Tap game headers to expand or collapse character lists
3. **Search (Optional)** - Tap the search FAB to search for specific characters
4. **View Details** - Tap any character card to see detailed information

### Writing to NFC Tags

1. **Select a Character**:
   - Browse or search to find your desired character
   - Tap the character card to open details

2. **Start Write Process**:
   - Tap "Write to Tag" in the character details dialog
   - You'll be taken to the write screen

3. **Initiate Write**:
   - **Tap the "Write" button** (user action required)
   - Then tap your phone to the NFC tag
   - The app will automatically check tag compatibility

4. **Compatibility Check**:
   - ✅ **Compatible**: Writing begins automatically
   - ⚠️ **Warning**: You can choose to write anyway or cancel
   - ❌ **Incompatible**: Detailed error message with recommendations

5. **Wait for Completion**:
   - Watch the progress indicator
   - Success message appears when complete
   - Tag is ready to use with portal devices

### Checking Tag Compatibility

1. **Open Tag Checker**:
   - Tap the menu (three dots) in the toolbar
   - Select "Check Tag Compatibility"

2. **Check a Tag**:
   - **Tap the "Check Tag" button** (user action required)
   - Then tap your phone to the NFC tag
   - View detailed compatibility report

3. **Review Results**:
   - See tag type, block count, authentication status
   - Read issues and recommendations
   - Use "Check Again" to test another tag

## 🔧 Technical Details

### App Information
- **Package Name**: `com.bitcoinerrorlog.skywriter`
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Build Tools**: Latest Android Gradle Plugin
- **Language**: Kotlin 100%

### NFC Support
- **Tag Type**: Mifare Classic 1K (64 blocks, 16 sectors)
- **Authentication**: Automatic key extraction from source data
- **Block Writing**: All 64 blocks including sector trailers
- **UID Handling**: Graceful handling of locked UID blocks

### Architecture
- **Pattern**: MVVM (Model-View-ViewModel)
- **UI**: Material Design 3 Components
- **Navigation**: AndroidX Navigation Component
- **Async**: Kotlin Coroutines
- **State**: LiveData and ViewModel
- **Database**: JSON-based asset loading (no SQLite)

## 🎯 Portal Compatibility

### Why It Matters

Skylanders portal devices require complete and accurate NFC data to recognize characters. This app ensures full compatibility by:

### ✅ What the App Does

- **Writes ALL Blocks**: Including sector trailers (blocks 3, 7, 11, 15, etc.)
- **Extracts Keys**: Automatically extracts Key A and Key B from source data
- **Proper Authentication**: Uses extracted keys for sector authentication
- **Complete Data**: All 64 blocks written in correct order
- **Error Handling**: Continues writing even if Block 0 (UID) is locked

### 🔑 Authentication Keys

The app extracts authentication keys from the sector trailer blocks in your source data:
- Reads Key A (bytes 0-5) and Key B (bytes 10-15) from each sector trailer
- Uses these keys for authentication before writing each sector
- Falls back to default keys if extraction fails

### 📋 Testing with Portal

To verify compatibility:
1. Write a character to a blank Mifare Classic 1K tag
2. Place the tag on a Skylanders portal device
3. The portal should recognize the character and load it into the game

### ⚠️ UID Limitations

**Important**: Most standard NFC tags have locked UIDs (Block 0) that cannot be changed. This is normal and expected:
- ✅ Character data in blocks 1-63 is what the portal primarily reads
- ✅ The app handles locked UIDs gracefully
- ⚠️ For 100% compatibility, you may need UID-changeable tags (genuine Skylanders tags have specific UIDs)

## 🧪 Testing

### Running Tests

**Unit Tests** (run on JVM):
```bash
./gradlew test
```

**Instrumented Tests** (run on device/emulator):
```bash
./gradlew connectedAndroidTest
```

**All Tests**:
```bash
./gradlew test connectedAndroidTest
```

### Test Coverage

- ✅ **Unit Tests**: Data models, NFC writer utilities, WriteResult types
- ✅ **Instrumented Tests**: Database operations, NFC manager functionality
- ✅ **E2E Tests**: Simulated NFC write flow (`NFCWriteE2ETest.kt`)

### E2E Testing

The `NFCWriteE2ETest` class provides end-to-end testing:
- Character data structure validity
- Block data format correctness
- WriteResult type handling
- NFC adapter availability checks

**Note**: For actual hardware testing, you need:
- A physical NFC-enabled Android device
- A Mifare Classic 1K compatible tag
- Proper NFC permissions enabled

## 🛠️ Development

### Building the Project

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Run lint checks
./gradlew lint

# Clean build
./gradlew clean
```

### Project Structure

```
skywriter/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/...          # Kotlin source files
│   │   │   ├── res/              # Resources (layouts, strings, icons)
│   │   │   └── assets/           # JSON character data
│   │   ├── test/                 # Unit tests
│   │   └── androidTest/          # Instrumented tests
│   └── build.gradle.kts          # App-level build config
├── gradle/                        # Gradle wrapper
├── images/                        # Source logo images
├── process_images.py             # Image processing script
└── README.md                      # This file
```

### Key Components

**Data Layer**:
- `CharacterModel`: Parcelable data class representing a character
- `NFCDatabase`: JSON file parsing and character loading
- `CharacterRepository`: Data access abstraction

**NFC Layer**:
- `MifareClassicWriter`: Core NFC writing logic with authentication
- `NFCManager`: NFC adapter and tag detection management
- `TagCompatibilityChecker`: Detailed tag compatibility analysis
- `WriteResult`: Sealed class for operation results

**UI Layer**:
- `MainActivity`: Host activity with navigation and NFC handling
- `CharacterListFragment`: Character browsing with search
- `CharacterDetailDialog`: Character information display
- `WriteNFCFragment`: NFC writing interface
- `TagCheckFragment`: Standalone tag compatibility checker

**ViewModel**:
- `CharacterViewModel`: State management for character list and search

### Dependencies

**Core Android**:
- AndroidX Core KTX
- AppCompat
- Material Design Components 3
- Constraint Layout

**Architecture**:
- Lifecycle (ViewModel, LiveData)
- Navigation Component
- Kotlin Coroutines

**Testing**:
- JUnit 4
- Mockito
- AndroidX Test
- Espresso

## 🐛 Troubleshooting

### NFC Not Available
- ✅ Ensure NFC is enabled in device settings
- ✅ Check that device supports Mifare Classic (not all NFC devices do)
- ✅ Verify NFC permissions in app settings
- ✅ Some devices require NFC to be enabled in quick settings

### Write Failures
- ✅ Tag may be locked or use non-default keys (app handles this automatically)
- ✅ Tag may not be Mifare Classic compatible (use tag checker first)
- ✅ Ensure tag is properly positioned during write (keep steady)
- ✅ Try a different tag if issues persist

### No Characters Displayed
- ✅ Verify JSON files are in `app/src/main/assets/Android_NFC_Data/`
- ✅ Check JSON file format matches expected structure
- ✅ Review logcat for parsing errors: `adb logcat | grep Skywriter`
- ✅ Ensure files are properly formatted JSON (not corrupted)

### Tag Compatibility Issues
- ✅ Use the tag checker before writing to verify compatibility
- ✅ Ensure tag is Mifare Classic 1K (not NTAG or other types)
- ✅ Check that tag is not write-protected
- ✅ Some tags may require specific authentication keys

### Build Issues
- ✅ Ensure you have JDK 17 or higher installed
- ✅ Check that Android SDK is properly configured
- ✅ Try `./gradlew clean` then rebuild
- ✅ See [BUILD.md](BUILD.md) for detailed build instructions

## 📚 Documentation

- **[BUILD.md](BUILD.md)**: Detailed build instructions and troubleshooting
- **[TESTING.md](TESTING.md)**: Comprehensive testing guide
- **[ARCHITECTURE.md](ARCHITECTURE.md)**: Architecture overview and design patterns

## 🔍 Project Verification

Run the verification script to check project structure:

```bash
./verify_project.sh
```

This script verifies:
- Required directories exist
- JSON files are present in assets
- Build configuration is correct
- Key files are in place

## 🤝 Contributing

We welcome contributions! Here's how:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Make your changes**
4. **Add tests** for new functionality
5. **Run tests**: `./gradlew test connectedAndroidTest`
6. **Commit your changes**: `git commit -m 'Add amazing feature'`
7. **Push to branch**: `git push origin feature/amazing-feature`
8. **Open a Pull Request**

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Write tests for new features

## 📝 License

[Add your license here]

## 🙏 Acknowledgments

- Built with ❤️ using Kotlin and Material Design 3
- NFC data format based on Flipper Zero `.nfc` file structure
- Designed for compatibility with Skylanders portal devices

## 📞 Support

For issues, questions, or contributions:
- Open an issue on GitHub
- Check existing documentation
- Review troubleshooting section above

---

<div align="center">

**Made with ❤️ for the Skylanders community**

[⭐ Star this repo](https://github.com/BitcoinErrorLog/skywriter) if you find it useful!

</div>

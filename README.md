# Skywriter

<div align="center">

**A beautiful Android app for writing character NFC data to physical tags**

[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org/)
[![Material Design 3](https://img.shields.io/badge/Material%20Design-3-blue.svg)](https://m3.material.io/)

</div>

Skywriter is a modern Android application that allows you to browse characters organized by game series, select them visually, and write their NFC data to physical tags using your Android device's NFC capabilities. The app supports multiple tag types including Mifare Classic 1K and NTAG215 tags.

## ✨ Features

### Core Functionality
- **📱 Dual Mode Support**: Choose between different character databases and tag types
- **🏠 Home Screen**: Intuitive mode selection to switch between character types
- **📋 Browse Characters**: View all characters organized by game series with collapsible headers
- **🔍 Smart Search**: Fast partial matching search to quickly find specific characters
- **📝 Character Details**: View detailed information including biography, abilities, and element types
- **💾 NFC Writing**: Write character data to compatible NFC tags with full verification
- **✅ Tag Compatibility Checker**: Comprehensive tool that automatically detects tag type and verifies compatibility
- **🧹 Tag Eraser**: Clear tags to a known blank state before writing
- **🔐 Automatic Authentication**: Extracts and uses authentication keys from source data
- **📊 Real-time Feedback**: Progress indicators and detailed status messages
- **✓ Write Verification**: Reads back written data to ensure successful writes

### User Experience
- **🎨 Beautiful UI**: Clean white background with navy blue accents (Material Design 3)
- **📱 Modern Design**: Collapsible game sections, intuitive navigation, and smooth animations
- **🛡️ Safety First**: User-initiated actions only - tap buttons before any NFC operations
- **⚠️ Smart Warnings**: Compatibility checker warns about potential issues before writing
- **🔄 Error Handling**: Graceful handling of locked tags, authentication failures, and incompatible tags
- **📱 Persistent Header**: Logo and menu accessible from all screens
- **🏠 Quick Navigation**: Logo click returns to home screen

### Technical Excellence
- **🏗️ MVVM Architecture**: Clean separation of concerns with ViewModels and LiveData
- **🧪 Comprehensive Testing**: Unit tests, instrumented tests, and E2E test coverage
- **📦 Portal Compatible**: Writes ALL blocks including sector trailers for full compatibility
- **🔑 Key Extraction**: Automatically extracts authentication keys from source data
- **📱 Multi-density Support**: Optimized icons and resources for all screen densities
- **🔋 Power Management**: Keeps screen on during NFC operations to prevent interruptions
- **🔌 Connection Management**: Automatic reconnection handling for stable NFC operations

## 🎨 Design

Skywriter features a clean, modern design with:
- **White Background**: Easy on the eyes, professional appearance
- **Navy Blue Accents**: Primary color (#001F3F) for buttons, headers, and highlights
- **Material Design 3**: Latest Material Design components and patterns
- **Custom Branding**: Skywriter logo in app icon and header
- **No Drop Shadows**: Flat design with clean edges

## 📸 Screenshots

*Screenshots coming soon*

## 🚀 Quick Start

### Prerequisites

- **Android Device**: Android 7.0 (API 24) or higher with NFC support
- **NFC Tags**: 
  - Mifare Classic 1K compatible tags (for character mode)
  - NTAG215 tags (for compatible tag mode)
- **Android Studio**: Latest version recommended (for development)

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/BitcoinErrorLog/skywriter.git
   cd skywriter
   ```

2. **Prepare Character Data**:
   - Character JSON files should be in `app/src/main/assets/Android_NFC_Data/`
   - Compatible tag JSON files should be in `app/src/main/assets/Amiibo_NFC_Data/`
   - The app will automatically load all characters on first launch

3. **Build the APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be in `app/build/outputs/apk/debug/skywriter.apk`

4. **Install on Device**:
   - Transfer the APK to your Android device
   - Enable "Install from Unknown Sources" if needed
   - Install and launch the app

## 📖 Usage Guide

### Getting Started

1. **Launch the app** - You'll see the home screen with mode selection
2. **Choose a Mode**:
   - **Character Mode**: For writing character data to Mifare Classic tags
   - **Compatible Tag Mode**: For writing compatible tag data to NTAG215 tags
3. **Navigate**: Use the logo to return home, or the menu button for quick access

### Browsing Characters

1. **Select Mode** - Choose your desired mode from the home screen
2. **Browse** - View all characters organized by game series
3. **Expand/Collapse Games** - Tap game headers to expand or collapse character lists
4. **Search** - Tap the search FAB to quickly find specific characters by name
5. **View Details** - Tap any character card to see detailed information including biography and abilities

### Writing to NFC Tags

1. **Select a Character**:
   - Browse or search to find your desired character
   - Tap the character card to open details

2. **Start Write Process**:
   - Tap "Write to Tag" in the character details dialog
   - You'll be taken to the write screen

3. **Initiate Write**:
   - **Tap the "Write" button** (user action required)
   - The app will automatically check tag compatibility when tag is detected
   - Compatibility check runs automatically - no need to tap again

4. **Compatibility Check**:
   - ✅ **Compatible**: Writing begins automatically
   - ⚠️ **Warning**: You can choose to write anyway or cancel
   - ❌ **Incompatible**: Detailed error message with recommendations

5. **Wait for Completion**:
   - Watch the progress indicator
   - The app verifies writes by reading back data
   - Success message appears when complete
   - Tag is ready to use

### Checking Tag Compatibility

1. **Open Tag Checker**:
   - Tap the menu (three dots) in the toolbar
   - Select "Check Tag Compatibility"

2. **Check a Tag**:
   - The checker automatically detects tag type (Mifare Classic or NTAG215)
   - **Tap the "Check Tag" button** (user action required)
   - Then tap your phone to the NFC tag
   - View detailed compatibility report

3. **Review Results**:
   - See tag type, block/page count, authentication status
   - View tag contents and current data
   - Read issues and recommendations
   - Use "Check Again" to test another tag

### Erasing Tags

1. **Open Tag Checker**:
   - Navigate to the tag checker from the menu

2. **Check Tag First**:
   - Check the tag to see its current state

3. **Erase Tag**:
   - Tap "Erase Tag" button
   - Confirm the erase operation
   - Tag will be cleared to a blank state
   - Verification ensures data was actually erased

## 🔧 Technical Details

### App Information
- **Package Name**: `com.bitcoinerrorlog.skywriter`
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Build Tools**: Latest Android Gradle Plugin
- **Language**: Kotlin 100%

### NFC Support

**Mifare Classic 1K Tags**:
- **Tag Type**: Mifare Classic 1K (64 blocks, 16 sectors)
- **Authentication**: Automatic key extraction from source data
- **Block Writing**: All 64 blocks including sector trailers
- **UID Handling**: Graceful handling of locked UID blocks
- **Verification**: Reads back critical blocks to verify writes

**NTAG215 Tags**:
- **Tag Type**: NTAG215 (135 pages, 540 bytes)
- **Page-based Writing**: Writes all 135 pages including UID pages
- **Compatibility Check**: Verifies ATQA/SAK and read/write capability
- **Write Verification**: Reads back critical pages to ensure data was written
- **Erase Support**: Can erase all data pages to blank state

### Architecture
- **Pattern**: MVVM (Model-View-ViewModel)
- **UI**: Material Design 3 Components
- **Navigation**: AndroidX Navigation Component
- **Async**: Kotlin Coroutines
- **State**: LiveData and ViewModel
- **Database**: JSON-based asset loading (no SQLite)

## 🎯 Portal Compatibility

### Why It Matters

Portal devices require complete and accurate NFC data to recognize characters. This app ensures full compatibility by:

### ✅ What the App Does

**For Mifare Classic Tags**:
- **Writes ALL Blocks**: Including sector trailers (blocks 3, 7, 11, 15, etc.)
- **Extracts Keys**: Automatically extracts Key A and Key B from source data
- **Proper Authentication**: Uses extracted keys for sector authentication
- **Complete Data**: All 64 blocks written in correct order
- **Error Handling**: Continues writing even if Block 0 (UID) is locked
- **Write Verification**: Verifies critical blocks were written correctly

**For NTAG215 Tags**:
- **Writes ALL Pages**: All 135 pages including UID pages (if not locked)
- **Compatibility Check**: Verifies tag is genuine NTAG215 before writing
- **Write Verification**: Reads back critical pages to ensure data persistence
- **Erase Support**: Can clear tags to blank state for fresh writes

### 🔑 Authentication Keys

**Mifare Classic**:
The app extracts authentication keys from the sector trailer blocks in your source data:
- Reads Key A (bytes 0-5) and Key B (bytes 10-15) from each sector trailer
- Uses these keys for authentication before writing each sector
- Falls back to default keys if extraction fails

**NTAG215**:
- No authentication required (uses NfcA technology)
- Verifies tag type using ATQA and SAK values
- Checks read/write capability before writing

### 📋 Testing with Portal

To verify compatibility:
1. Write a character to a compatible tag
2. Use the tag checker to verify the write was successful
3. Place the tag on the portal device
4. The portal should recognize the character

### ⚠️ UID Limitations

**Important**: Most standard NFC tags have locked UIDs that cannot be changed. This is normal and expected:
- ✅ Character data in data blocks/pages is what the portal primarily reads
- ✅ The app handles locked UIDs gracefully
- ✅ Write verification ensures data was written even if UID is locked
- ⚠️ For 100% compatibility, you may need UID-changeable tags

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

- ✅ **Unit Tests**: Data models, NFC writer utilities, erase operations, WriteResult types
- ✅ **Instrumented Tests**: Database operations, NFC manager functionality, UI navigation
- ✅ **E2E Tests**: Simulated NFC write flow

**Note**: Unit tests for NFC operations are limited because Android framework classes cannot be mocked. For comprehensive NFC testing, use instrumented tests with actual hardware or manual testing.

See [TESTING_NFC.md](TESTING_NFC.md) and [RUN_TESTS.md](RUN_TESTS.md) for detailed testing documentation.

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
├── archive/                       # Archived files (gitignored)
└── README.md                      # This file
```

### Key Components

**Data Layer**:
- `CharacterModel`: Parcelable data class representing a character
- `AmiiboModel`: Data class for compatible tag data
- `NFCDatabase`: JSON file parsing and character loading
- `AmiiboDatabase`: JSON file parsing for compatible tag data
- `CharacterRepository`: Data access abstraction
- `AmiiboRepository`: Data access for compatible tags

**NFC Layer**:
- `MifareClassicWriter`: Core NFC writing logic with authentication
- `NTAG215Writer`: Writing logic for NTAG215 tags
- `NFCManager`: NFC adapter and tag detection management
- `TagCompatibilityChecker`: Detailed tag compatibility analysis for Mifare Classic
- `NTAG215CompatibilityChecker`: Compatibility checking for NTAG215 tags
- `TagEraser`: Erase functionality for Mifare Classic tags
- `AmiiboTagEraser`: Erase functionality for NTAG215 tags
- `TagReader`: Read tag contents and identify characters
- `AmiiboTagReader`: Read NTAG215 tag contents
- `WriteResult`: Sealed class for operation results

**UI Layer**:
- `MainActivity`: Host activity with navigation and NFC handling
- `HomeFragment`: Mode selection screen
- `CharacterListFragment`: Character browsing with search
- `AmiiboListFragment`: Compatible tag browsing with search
- `CharacterDetailDialog`: Character information display
- `AmiiboDetailDialog`: Compatible tag information display
- `WriteNFCFragment`: NFC writing interface for characters
- `WriteAmiiboFragment`: NFC writing interface for compatible tags
- `TagCheckFragment`: Standalone tag compatibility checker with erase

**ViewModel**:
- `CharacterViewModel`: State management for character list and search
- `AmiiboViewModel`: State management for compatible tag list and search

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
- ✅ Check that device supports the required tag type
- ✅ Verify NFC permissions in app settings
- ✅ Some devices require NFC to be enabled in quick settings

### Write Failures
- ✅ Tag may be locked or use non-default keys (app handles this automatically)
- ✅ Tag may not be compatible (use tag checker first)
- ✅ Ensure tag is properly positioned during write (keep steady)
- ✅ Try a different tag if issues persist
- ✅ Check that write verification passed (app verifies writes automatically)

### No Characters Displayed
- ✅ Verify JSON files are in the correct assets directory
- ✅ Check JSON file format matches expected structure
- ✅ Review logcat for parsing errors: `adb logcat | grep Skywriter`
- ✅ Ensure files are properly formatted JSON (not corrupted)

### Tag Compatibility Issues
- ✅ Use the tag checker before writing to verify compatibility
- ✅ Ensure tag matches the selected mode (Mifare Classic or NTAG215)
- ✅ Check that tag is not write-protected
- ✅ Some tags may require specific authentication keys
- ✅ Verify tag type is correctly detected by the checker

### Build Issues
- ✅ Ensure you have JDK 17 or higher installed
- ✅ Check that Android SDK is properly configured
- ✅ Try `./gradlew clean` then rebuild
- ✅ See [DEBUGGING.md](DEBUGGING.md) for debugging tips

## 📚 Documentation

- **[TESTING_NFC.md](TESTING_NFC.md)**: Comprehensive NFC testing guide
- **[RUN_TESTS.md](RUN_TESTS.md)**: Quick reference for running tests
- **[DEBUGGING.md](DEBUGGING.md)**: Debugging tips and log capture

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
- Designed for compatibility with portal devices

## 📞 Support

For issues, questions, or contributions:
- Open an issue on GitHub
- Check existing documentation
- Review troubleshooting section above

---

<div align="center">

**Made with ❤️ for the NFC community**

[⭐ Star this repo](https://github.com/BitcoinErrorLog/skywriter) if you find it useful!

</div>

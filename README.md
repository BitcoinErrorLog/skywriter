# Skywriter

A fun Android app that lets you browse characters organized by game series, select them visually, and write their NFC data to physical tags using your Android device's NFC capabilities.

## Features

- Browse characters organized by game series
- Search and filter characters
- Write character NFC data to Mifare Classic tags
- Material Design 3 UI
- Real-time write progress and feedback

## Setup

### Prerequisites

1. Android Studio (latest version recommended)
2. Android device with NFC support (tested on Google Pixel 6)
3. Mifare Classic 1K compatible NFC tags

### Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/BitcoinErrorLog/skywriter.git
   cd skywriter
   ```

2. Convert NFC files to JSON format:
   - The app expects JSON files in the `app/src/main/assets/Android_NFC_Data/` directory
   - Use the conversion script from the FlipperSkylanders repository to convert `.nfc` files to JSON
   - Place the converted JSON files in `app/src/main/assets/Android_NFC_Data/` maintaining the original directory structure

3. Open the project in Android Studio

4. Build and run on your device

## Usage

1. Launch the app
2. Browse or search for a character
3. Tap on a character to view details
4. Tap "Write to Tag"
5. Hold your phone near an NFC tag
6. Wait for the write operation to complete

## Technical Details

- **Package**: `com.bitcoinerrorlog.skywriter`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **NFC Support**: Mifare Classic 1K tags

## Portal Compatibility

**IMPORTANT**: This app writes ALL blocks including sector trailers to ensure compatibility with Skylanders portal devices. The portal requires:
- Complete block data including sector trailers with correct authentication keys
- Proper authentication using keys extracted from the source data
- All 64 blocks written (except Block 0 UID which may be locked on some tags)

The app:
- ✅ Extracts authentication keys from sector trailers in the source data
- ✅ Writes ALL blocks including sector trailers (required for portal recognition)
- ✅ Uses extracted keys for authentication before writing each sector
- ✅ Handles Block 0 UID gracefully (attempts to write but continues if locked)

**Testing with Portal**: To verify compatibility:
1. Write a character to a blank Mifare Classic 1K tag using this app
2. Place the tag on a Skylanders portal
3. The portal should recognize the character and load it into the game

**Note**: The tag's UID (Block 0) cannot be changed on most standard tags. For full compatibility, you may need:
- UID-changeable tags (genuine Skylanders tags have specific UIDs)
- OR use tags that already have compatible UIDs
- The character data in blocks 1-63 is what the portal primarily reads for character identification

## Testing

### Running Tests

The project includes both unit tests and instrumented tests:

**Unit Tests** (run on JVM):
```bash
./gradlew test
```

**Instrumented Tests** (run on device/emulator):
```bash
./gradlew connectedAndroidTest
```

### Test Coverage

- **Unit Tests**: Data models, NFC writer utilities, WriteResult types
- **Instrumented Tests**: Database operations, NFC manager functionality
- **E2E Tests**: Simulated NFC write flow (see `NFCWriteE2ETest.kt`)

### E2E Testing with Simulated Tags

The `NFCWriteE2ETest` class provides end-to-end testing of the NFC writing flow:

```kotlin
// Tests verify:
// 1. Character data structure validity
// 2. Block data format correctness
// 3. WriteResult type handling
// 4. NFC adapter availability checks
```

**Note**: For actual hardware testing, you need:
- A physical NFC-enabled Android device
- A Mifare Classic 1K compatible tag
- Proper NFC permissions enabled

### Test Data

Test data is generated programmatically in test classes. For full testing with real character data:
1. Convert NFC files to JSON using the conversion script
2. Place JSON files in `app/src/main/assets/Android_NFC_Data/`
3. Run instrumented tests to verify JSON parsing

## Development

### Building the Project

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run lint checks
./gradlew lint
```

### Project Structure

```
app/
├── src/
│   ├── main/           # Main application code
│   │   ├── java/...    # Kotlin source files
│   │   ├── res/        # Resources (layouts, strings, etc.)
│   │   └── assets/     # JSON character data
│   ├── test/           # Unit tests
│   └── androidTest/    # Instrumented tests
```

### Key Components

- **Data Layer**: `CharacterModel`, `NFCDatabase`, `CharacterRepository`
- **NFC Layer**: `MifareClassicWriter`, `NFCManager`, `WriteResult`
- **UI Layer**: `MainActivity`, `CharacterListFragment`, `WriteNFCFragment`
- **ViewModel**: `CharacterViewModel` for state management

## Troubleshooting

### NFC Not Available
- Ensure NFC is enabled in device settings
- Check that device supports Mifare Classic (not all NFC devices do)
- Verify NFC permissions in app settings

### Write Failures
- Tag may be locked or use non-default keys
- Tag may not be Mifare Classic compatible
- Ensure tag is properly positioned during write

### No Characters Displayed
- Verify JSON files are in `app/src/main/assets/Android_NFC_Data/`
- Check JSON file format matches expected structure
- Review logcat for parsing errors

## Documentation

- **[BUILD.md](BUILD.md)**: Detailed build instructions and troubleshooting
- **[TESTING.md](TESTING.md)**: Comprehensive testing guide
- **[ARCHITECTURE.md](ARCHITECTURE.md)**: Architecture overview and design patterns

## Project Verification

Run the verification script to check project structure:

```bash
./verify_project.sh
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run tests: `./gradlew test connectedAndroidTest`
6. Submit a pull request

## License

[Add your license here]


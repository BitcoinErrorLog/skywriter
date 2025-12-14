# Building Skywriter

## Prerequisites

1. **Android Studio** (latest version recommended)
   - Download from: https://developer.android.com/studio
   - Includes Android SDK, Gradle, and build tools

2. **Java Development Kit (JDK) 17**
   - Android Studio includes a bundled JDK, or install separately

3. **Android SDK**
   - Minimum SDK: 24 (Android 7.0)
   - Target SDK: 34 (Android 14)
   - Build Tools: Included with Android Studio

## Building with Android Studio

1. **Open the Project**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `skywriter` directory
   - Click "OK"

2. **Sync Gradle**
   - Android Studio will automatically sync Gradle files
   - Wait for dependencies to download (first time only)

3. **Build the Project**
   - **Debug Build**: `Build > Make Project` or `Ctrl+F9` (Windows/Linux) / `Cmd+F9` (Mac)
   - **Release Build**: `Build > Generate Signed Bundle / APK`

4. **Run Tests**
   - **Unit Tests**: Right-click `app/src/test` > "Run 'Tests in 'app''"
   - **Instrumented Tests**: Connect device/emulator, then right-click `app/src/androidTest` > "Run 'Tests in 'app''"

## Building from Command Line

### Setup Gradle Wrapper

If Gradle wrapper is not present, Android Studio will create it automatically when you open the project.

### Build Commands

```bash
# Navigate to project directory
cd skywriter

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run all tests
./gradlew test connectedAndroidTest

# Clean build
./gradlew clean

# Build and run lint
./gradlew lint
```

### Output Locations

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Test Results**: `app/build/test-results/`

## Testing

### Unit Tests

Unit tests run on the JVM and don't require an Android device:

```bash
./gradlew test
```

Test files are located in: `app/src/test/java/`

### Instrumented Tests

Instrumented tests require an Android device or emulator:

```bash
# Connect device via USB or start emulator
adb devices

# Run instrumented tests
./gradlew connectedAndroidTest
```

Test files are located in: `app/src/androidTest/java/`

### E2E Test

The `NFCWriteE2ETest` class provides end-to-end testing:

- Tests character data structure validity
- Verifies block data format
- Checks NFC adapter availability
- Simulates write flow (without actual hardware)

**Note**: For actual hardware testing with real NFC tags, you need:
- Physical NFC-enabled Android device
- Mifare Classic 1K compatible tag
- NFC enabled in device settings

## Troubleshooting Build Issues

### Gradle Sync Failed

1. Check internet connection (dependencies need to download)
2. Verify Android SDK is installed
3. Try: `File > Invalidate Caches / Restart`

### Build Errors

1. **Missing dependencies**: Run `./gradlew --refresh-dependencies`
2. **SDK issues**: Check `local.properties` has correct `sdk.dir`
3. **Java version**: Ensure JDK 17 is configured

### Test Failures

1. **Unit tests**: Check JVM compatibility
2. **Instrumented tests**: Ensure device/emulator is connected
3. **NFC tests**: Some tests may fail on emulators (NFC not available)

## Continuous Integration

For CI/CD pipelines:

```yaml
# Example GitHub Actions
- name: Run tests
  run: ./gradlew test

- name: Build APK
  run: ./gradlew assembleDebug
```

## Release Build

1. Generate keystore (first time only):
   ```bash
   keytool -genkey -v -keystore skywriter.keystore -alias skywriter -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Create `keystore.properties`:
   ```properties
   storePassword=your_store_password
   keyPassword=your_key_password
   keyAlias=skywriter
   storeFile=../skywriter.keystore
   ```

3. Update `app/build.gradle.kts` with signing config

4. Build release:
   ```bash
   ./gradlew assembleRelease
   ```


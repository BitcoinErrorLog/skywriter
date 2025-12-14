# Testing Guide

## Test Structure

The project includes comprehensive tests at multiple levels:

### Unit Tests (`app/src/test/`)

Run on JVM, no Android device required:

- **CharacterModelTest**: Tests data model properties and metadata
- **MifareClassicWriterTest**: Tests WriteResult types and basic utilities

### Instrumented Tests (`app/src/androidTest/`)

Require Android device or emulator:

- **NFCDatabaseInstrumentedTest**: Tests JSON parsing and database operations
- **MifareClassicWriterInstrumentedTest**: Tests NFC writer initialization
- **NFCWriteE2ETest**: End-to-end test simulating NFC write flow

## Running Tests

### In Android Studio

1. **Unit Tests**: Right-click `app/src/test` → "Run 'Tests in 'app''"
2. **Instrumented Tests**: Connect device, right-click `app/src/androidTest` → "Run 'Tests in 'app''"
3. **Single Test**: Right-click test class/method → "Run"

### From Command Line

```bash
# All unit tests
./gradlew test

# All instrumented tests (requires device)
./gradlew connectedAndroidTest

# Specific test class
./gradlew test --tests "CharacterModelTest"

# With coverage
./gradlew test jacocoTestReport
```

## Test Coverage

### Current Coverage

- ✅ Data models (CharacterModel, CharacterMetadata)
- ✅ WriteResult types
- ✅ Database initialization
- ✅ NFC writer initialization
- ✅ E2E flow simulation

### Areas for Expansion

- [ ] ViewModel testing
- [ ] Repository testing with mock data
- [ ] UI component testing (Espresso)
- [ ] NFC manager testing with mocked tags
- [ ] Error handling edge cases

## E2E Testing

### Simulated E2E Test

The `NFCWriteE2ETest` class provides:

1. **Character Data Validation**
   - Verifies UID format
   - Checks block count (64 for 1K)
   - Validates hex string format

2. **Block Structure Testing**
   - Tests block 0 (UID block)
   - Verifies sector trailer positions
   - Checks data block format

3. **NFC Adapter Checks**
   - Tests NFC availability detection
   - Handles emulator scenarios gracefully

### Real Hardware Testing

For actual NFC tag writing:

1. **Prerequisites**:
   - Physical Android device with NFC
   - Mifare Classic 1K tag
   - NFC enabled in device settings

2. **Test Steps**:
   ```
   1. Install app on device
   2. Launch app
   3. Select a character
   4. Tap "Write to Tag"
   5. Hold device near tag
   6. Verify success message
   7. Test tag with Skylanders game/portal
   ```

3. **Manual Test Checklist**:
   - [ ] App launches successfully
   - [ ] Characters load from JSON files
   - [ ] Search functionality works
   - [ ] Character detail dialog displays
   - [ ] NFC write screen appears
   - [ ] Tag detection works
   - [ ] Write operation completes
   - [ ] Success feedback shown
   - [ ] Error handling for invalid tags
   - [ ] Error handling for locked tags

## Mock Data

### Test Character

The E2E test creates a test character programmatically:

```kotlin
CharacterModel(
    uid = "21B589A3",
    atqa = "0004",
    sak = "08",
    mifareType = "1K",
    blocks = createTestBlocks(), // 64 blocks
    metadata = CharacterMetadata(...)
)
```

### JSON Test Files

For testing JSON parsing, create test files in:
`app/src/androidTest/assets/Android_NFC_Data/`

Example test JSON:
```json
{
  "uid": "21B589A3",
  "atqa": "0004",
  "sak": "08",
  "mifare_type": "1K",
  "blocks": ["21B589A3BE81010FC433000000000012", ...],
  "metadata": {
    "original_filename": "Test.nfc",
    "original_path": "test/Test.nfc",
    "category": "Test Game",
    "subcategory": "Test"
  }
}
```

## Continuous Integration

### GitHub Actions Example

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run unit tests
        run: ./gradlew test
      - name: Upload test results
        uses: actions/upload-artifact@v2
        if: always()
        with:
          name: test-results
          path: app/build/test-results/
```

## Debugging Tests

### View Test Output

```bash
# Verbose output
./gradlew test --info

# Debug output
./gradlew test --debug
```

### Test Reports

After running tests, view reports:
- HTML: `app/build/reports/tests/test/index.html`
- XML: `app/build/test-results/test/`

### Common Issues

1. **Tests fail on emulator**: Some NFC tests may fail (expected)
2. **Missing assets**: Ensure test assets are in correct location
3. **Mock issues**: Check Mockito setup for instrumented tests


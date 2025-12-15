# Running NFC Tests

## Quick Test Commands

Run all unit tests (no hardware required):
```bash
./gradlew test
```

Run specific test classes:
```bash
# Mifare erase tests
./gradlew test --tests "TagEraserTest"

# Amiibo erase tests  
./gradlew test --tests "AmiiboTagEraserTest"

# Mifare write tests
./gradlew test --tests "MifareClassicWriterEraseWriteTest"

# NTAG215 write tests
./gradlew test --tests "NTAG215WriterEraseWriteTest"
```

View test results:
```bash
# HTML report
open app/build/reports/tests/testDebugUnitTest/index.html

# Or view in terminal
./gradlew test --info | grep -E "(PASSED|FAILED|test)"
```

## Test Coverage

### TagEraserTest
- ✅ Not Mifare Classic tag → Error
- ✅ Cannot connect → Error
- ✅ Wrong block count → Error
- ✅ Successful erase → Success
- ✅ Authentication fails → Skips sector
- ✅ Write block fails → Continues with other blocks
- ✅ No blocks erased → Error
- ✅ Closes connection

### AmiiboTagEraserTest
- ✅ Not NfcA tag → Error
- ✅ Cannot connect → Error
- ✅ Successful erase → Success
- ✅ Write page fails → Continues with other pages
- ✅ No pages erased → Error
- ✅ Closes connection
- ✅ Skips UID pages (0-2)

### MifareClassicWriterEraseWriteTest
- ✅ Write after erase → Success
- ✅ Connection lost → Reconnects
- ✅ Authentication fails → Tries default keys
- ✅ Block 0 locked → Continues
- ✅ Verifies connection before write

### NTAG215WriterEraseWriteTest
- ✅ Write after erase → Success
- ✅ Connection lost → Reconnects
- ✅ Verifies connection before write
- ✅ UID pages locked → Continues
- ✅ Connection test fails → Error
- ✅ Closes and reconnects before write

## Fixing Test Failures

If tests fail, check:
1. Mockito static mocks are properly closed in `@After` methods
2. All mocks are properly initialized in `@Before` methods
3. Coroutine test dispatcher is set up correctly

## Manual Testing Checklist

After tests pass, manually verify:
- [ ] Mifare erase works on real tag
- [ ] Mifare write works after erase
- [ ] NTAG215 erase works on real tag
- [ ] NTAG215 write works after erase
- [ ] No re-tap required when tag stays close
- [ ] Connection recovery works when tag moves


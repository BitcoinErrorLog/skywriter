#!/bin/bash

# Project Verification Script for Skywriter
# Checks that all required files and directories are present

echo "Verifying Skywriter project structure..."

ERRORS=0

# Check required directories
check_dir() {
    if [ ! -d "$1" ]; then
        echo "❌ Missing directory: $1"
        ERRORS=$((ERRORS + 1))
    else
        echo "✅ Found directory: $1"
    fi
}

# Check required files
check_file() {
    if [ ! -f "$1" ]; then
        echo "❌ Missing file: $1"
        ERRORS=$((ERRORS + 1))
    else
        echo "✅ Found file: $1"
    fi
}

# Check directories
echo ""
echo "Checking directories..."
check_dir "app"
check_dir "app/src/main"
check_dir "app/src/main/java/com/bitcoinerrorlog/skywriter"
check_dir "app/src/main/res"
check_dir "app/src/main/assets"
check_dir "app/src/test/java/com/bitcoinerrorlog/skywriter"
check_dir "app/src/androidTest/java/com/bitcoinerrorlog/skywriter"

# Check key files
echo ""
echo "Checking key files..."
check_file "build.gradle.kts"
check_file "settings.gradle.kts"
check_file "app/build.gradle.kts"
check_file "app/src/main/AndroidManifest.xml"
check_file "README.md"
check_file "BUILD.md"
check_file "TESTING.md"

# Check source files
echo ""
echo "Checking source files..."
check_file "app/src/main/java/com/bitcoinerrorlog/skywriter/MainActivity.kt"
check_file "app/src/main/java/com/bitcoinerrorlog/skywriter/data/CharacterModel.kt"
check_file "app/src/main/java/com/bitcoinerrorlog/skywriter/data/NFCDatabase.kt"
check_file "app/src/main/java/com/bitcoinerrorlog/skywriter/nfc/MifareClassicWriter.kt"
check_file "app/src/main/java/com/bitcoinerrorlog/skywriter/nfc/NFCManager.kt"

# Check test files
echo ""
echo "Checking test files..."
check_file "app/src/test/java/com/bitcoinerrorlog/skywriter/data/CharacterModelTest.kt"
check_file "app/src/test/java/com/bitcoinerrorlog/skywriter/nfc/MifareClassicWriterTest.kt"
check_file "app/src/androidTest/java/com/bitcoinerrorlog/skywriter/NFCWriteE2ETest.kt"

# Summary
echo ""
if [ $ERRORS -eq 0 ]; then
    echo "✅ All checks passed! Project structure is complete."
    exit 0
else
    echo "❌ Found $ERRORS issue(s). Please fix them before building."
    exit 1
fi


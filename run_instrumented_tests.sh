#!/bin/bash
# Script to run instrumented tests on a connected device or emulator

set -e

echo "=== Skywriter Instrumented Test Runner ==="
echo ""

# Check if device is connected
echo "Checking for connected devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l | tr -d ' ')

if [ "$DEVICES" -eq "0" ]; then
    echo "❌ No devices found!"
    echo ""
    echo "Please either:"
    echo "  1. Connect your Pixel 6 via USB with USB debugging enabled"
    echo "  2. Start an Android emulator"
    echo ""
    echo "Then run: adb devices"
    exit 1
fi

echo "✓ Found $DEVICES device(s)"
adb devices
echo ""

# Build the test APK and app APK
echo "Building debug APK and test APK..."
./gradlew assembleDebug assembleDebugAndroidTest

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo ""
echo "✓ Build successful"
echo ""

# Install the app
echo "Installing app on device..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -ne 0 ]; then
    echo "❌ Installation failed!"
    exit 1
fi

echo "✓ App installed"
echo ""

# Run the MainActivity launch test specifically
echo "Running MainActivityLaunchTest..."
echo "This will launch the app and verify it loads correctly"
echo ""

./gradlew connectedAndroidTest --tests "com.bitcoinerrorlog.skywriter.MainActivityLaunchTest"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ All tests passed!"
    echo ""
    echo "Test results saved to: app/build/outputs/androidTest-results/connected/"
else
    echo ""
    echo "❌ Tests failed!"
    echo ""
    echo "Check the logs above for details."
    echo "You can also view detailed logs with:"
    echo "  adb logcat | grep -E '(MainActivity|AndroidRuntime|FATAL|ERROR)'"
    exit 1
fi


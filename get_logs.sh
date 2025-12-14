#!/bin/bash
# Connect your Pixel 6 via USB and run this script to get logs

echo "Make sure your Pixel 6 is connected via USB with USB debugging enabled"
echo "Press Enter when ready..."
read

echo "Clearing old logs..."
adb logcat -c

echo "Starting log capture. The app will show errors here..."
echo "Launch the app now, then press Ctrl+C to stop logging"
echo ""
adb logcat | grep -E "(Skywriter|MainActivity|AndroidRuntime|FATAL|ERROR|Exception)"

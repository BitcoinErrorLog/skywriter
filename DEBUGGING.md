# Debugging Guide

## Getting Logs from Your Pixel 6

### Prerequisites
1. Enable **Developer Options** on your Pixel 6:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
2. Enable **USB Debugging**:
   - Go to Settings → System → Developer Options
   - Enable "USB Debugging"
3. Connect your phone via USB to your computer

### Method 1: Using the Script (Easiest)

```bash
cd skywriter
./get_logs.sh
```

Then launch the app on your phone. The script will show all errors.

### Method 2: Manual ADB Commands

```bash
# Clear old logs
adb logcat -c

# Watch logs in real-time (filter for errors)
adb logcat | grep -E "(Skywriter|MainActivity|AndroidRuntime|FATAL|ERROR|Exception)"

# Or save to a file
adb logcat > app_logs.txt
```

### Method 3: Android Studio Logcat
1. Open Android Studio
2. Connect your phone
3. Open the Logcat tab
4. Filter by "MainActivity" or "Skywriter"

### What to Look For
- `FATAL EXCEPTION` - App crashes
- `MainActivity` - Our app's logs
- `AndroidRuntime` - System errors
- Any `Exception` or `Error` messages

### Common Issues

**"device unauthorized"**
- Check your phone for a popup asking to allow USB debugging
- Click "Allow" or "Always allow"

**"adb: command not found"**
- Install Android SDK Platform Tools
- On macOS: `brew install android-platform-tools`
- Or download from: https://developer.android.com/studio/releases/platform-tools


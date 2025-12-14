# Building the APK

## Quick Build Instructions

To build the APK, you need Android Studio installed. The Gradle wrapper will be created automatically when you open the project in Android Studio.

### Option 1: Using Android Studio (Recommended)

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `skywriter` directory
4. Wait for Gradle sync to complete
5. Build → Build Bundle(s) / APK(s) → Build APK(s)
6. APK will be in: `app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Using Command Line (After Android Studio Setup)

Once Android Studio has created the Gradle wrapper:

```bash
cd skywriter
./gradlew assembleDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Option 3: Using GitHub Actions

You can set up GitHub Actions to automatically build APKs on push. See BUILD.md for CI/CD examples.

## Note

The APK is not included in the repository (see .gitignore). You need to build it locally or use CI/CD.


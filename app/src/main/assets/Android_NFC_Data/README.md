# Character Data Files

This directory should contain JSON files converted from the Flipper Zero `.nfc` files.

## Setup Instructions

1. Use the conversion script from the FlipperSkylanders repository to convert `.nfc` files to JSON format
2. Place the converted JSON files in this directory, maintaining the original folder structure:
   ```
   Android_NFC_Data/
   ├── Skylanders 1 Spyro's Adventure/
   │   ├── 1) Figures/
   │   │   ├── Spyro.json
   │   │   ├── Bash.json
   │   │   └── ...
   │   └── 2) Magic Items/
   │       └── ...
   ├── Skylanders 2 Giants/
   └── ...
   ```

3. The app will automatically load all JSON files on startup
4. Characters will be organized by game series in the UI
5. All character names, game names, and subcategories will be visible

## JSON Format

Each JSON file should have this structure:
```json
{
  "uid": "21B589A3",
  "atqa": "0004",
  "sak": "08",
  "mifare_type": "1K",
  "blocks": ["21B589A3BE81010FC433000000000012", ...],
  "metadata": {
    "original_filename": "Spyro.nfc",
    "original_path": "Skylanders 1 Spyro's Adventure/1) Figures/Spyro.nfc",
    "category": "Skylanders 1 Spyro's Adventure",
    "subcategory": "1) Figures"
  }
}
```

## Testing

Run the instrumented tests to verify loading:
```bash
./gradlew connectedAndroidTest
```

The tests verify:
- All characters load successfully
- All character names are accessible
- All game names are visible
- All subcategories (Figures, Magic Items, Traps, etc.) are accessible
- Search functionality works
- Characters are properly organized by game


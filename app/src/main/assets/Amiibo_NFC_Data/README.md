# Amiibo NFC Data

This directory contains **760+ Amiibo NFC data files** in JSON format, automatically downloaded from GitHub repositories and organized by game series.

## Current Database

The database includes:
- **Animal Crossing**: 471 Amiibo cards and figures
- **Super Smash Bros.**: 104 characters
- **Mario Series**: 12 characters
- **The Legend of Zelda**: 6 characters
- **Pokemon**: 2 characters
- **Monster Hunter**: 3 characters
- **Kirby, Metroid, Yoshi**: Various characters
- **Unknown/Other**: 159 files (may need manual categorization)

**Total: 760+ unique Amiibo files**

## Structure

Organize Amiibo JSON files by game series in subdirectories:

```
Amiibo_NFC_Data/
  ├── Super Smash Bros./
  │   ├── Mario.json
  │   ├── Link.json
  │   └── ...
  ├── The Legend of Zelda/
  │   ├── Link (Ocarina of Time).json
  │   └── ...
  ├── Mario/
  │   └── ...
  └── ...
```

## JSON Format

Each JSON file should have the following structure:

```json
{
  "uid": "04XXXXXXXXXXXX",
  "pages": [
    "04XXXXXXXX",
    "XXXXXXXX",
    ...
  ],
  "metadata": {
    "original_filename": "mario.bin",
    "original_path": "/path/to/mario.bin",
    "character_name": "Mario",
    "game_series": "Super Smash Bros.",
    "character_id": "00000000",
    "game_id": "00000000",
    "biography": "Mario is the main character...",
    "release_date": "November 21, 2014",
    "amiibo_type": "Figure"
  }
}
```

## Updating the Database

The database was compiled using automated scripts that download from GitHub repositories.

### Automatic Compilation

To update or rebuild the database:

```bash
# Download from GitHub and convert to JSON
python3 download_amiibo_from_github.py ./amiibo_dumps_temp
python3 convert_amiibo_to_json.py ./amiibo_dumps_temp app/src/main/assets/Amiibo_NFC_Data

# Organize by game series
python3 organize_amiibo_by_series.py app/src/main/assets/Amiibo_NFC_Data

# (Optional) Enrich with additional metadata
python3 enrich_amiibo_data.py app/src/main/assets/Amiibo_NFC_Data --interactive
```

### Manual Addition

To add your own Amiibo files:

1. Place your .bin files (540 bytes) in a directory
2. Run the conversion script:
   ```bash
   python3 convert_amiibo_to_json.py <input_dir> app/src/main/assets/Amiibo_NFC_Data
   ```
3. Organize by series:
   ```bash
   python3 organize_amiibo_by_series.py app/src/main/assets/Amiibo_NFC_Data
   ```
4. (Optional) Enrich with profile data:
   ```bash
   python3 enrich_amiibo_data.py app/src/main/assets/Amiibo_NFC_Data --interactive
   ```

## Notes

- Each Amiibo file must be exactly 540 bytes (135 pages × 4 bytes)
- UID is extracted from the first 7 bytes
- Character ID and Game ID are extracted from pages 21-22
- Profile data (names, descriptions) can be added via the enrichment script


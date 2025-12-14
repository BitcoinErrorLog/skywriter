# Image Processing Instructions

## Adding Your Logo Images

1. **App Icon** (the 'S' with wings):
   - Place your image file in the `images` folder
   - Name it: `app_icon.png` or `app_icon.jpg` (or `icon.png`/`icon.jpg`)
   - This will be used for the app launcher icon

2. **Header Logo** (the 'SKYWRITER' text with wings):
   - Place your image file in the `images` folder  
   - Name it: `header_logo.png` or `header_logo.jpg` (or `logo.png`/`logo.jpg`)
   - This will be displayed at the top of the character list screen

3. **Process the images**:
   ```bash
   python3 process_images.py
   ```

   This script will:
   - Resize the app icon to all required Android densities
   - Create the header logo drawable
   - Place everything in the correct resource directories

## Manual Alternative

If you prefer to add images manually:

- **App Icon**: Add `ic_launcher_foreground.png` files to:
  - `app/src/main/res/mipmap-mdpi/` (144x144px)
  - `app/src/main/res/mipmap-hdpi/` (216x216px)
  - `app/src/main/res/mipmap-xhdpi/` (288x288px)
  - `app/src/main/res/mipmap-xxhdpi/` (432x432px)
  - `app/src/main/res/mipmap-xxxhdpi/` (576x576px)
  - `app/src/main/res/mipmap/` (432x432px base)

- **Header Logo**: Add `header_logo.png` to:
  - `app/src/main/res/drawable/` (recommended: 400px width, maintain aspect ratio)

## Notes

- The app icon background color has been set to dark blue (`skywriter_dark_blue`) to match your logo
- The header logo will appear at the top of the character list screen
- If the header logo is not found, the app will fall back to showing the title text


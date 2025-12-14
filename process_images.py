#!/usr/bin/env python3
"""
Script to process images for Android app icon and header logo.
Place your source images in the 'images' folder:
- app_icon.png (or .jpg) - The "S" with wings logo for the app icon
- header_logo.png (or .jpg) - The "SKYWRITER" text logo for the header

This script will resize them to all required Android densities.
"""

import os
import sys
from PIL import Image

# Android density multipliers
DENSITIES = {
    'mdpi': 1.0,
    'hdpi': 1.5,
    'xhdpi': 2.0,
    'xxhdpi': 3.0,
    'xxxhdpi': 4.0,
}

# Base sizes for different resources
ICON_SIZES = {
    'launcher': 192,  # Base size for adaptive icons (will be cropped to safe zone)
    'launcher_foreground': 432,  # Foreground layer for adaptive icons
    'header_logo': 400,  # Header logo width (height will maintain aspect ratio)
}

def resize_image(input_path, output_path, size, maintain_aspect=True):
    """Resize an image to the specified size."""
    try:
        img = Image.open(input_path)
        if maintain_aspect:
            img.thumbnail((size, size), Image.Resampling.LANCZOS)
        else:
            img = img.resize((size, size), Image.Resampling.LANCZOS)
        img.save(output_path, 'PNG', optimize=True)
        print(f"Created: {output_path} ({img.size[0]}x{img.size[1]})")
        return True
    except Exception as e:
        print(f"Error processing {input_path}: {e}")
        return False

def process_app_icon(source_path):
    """Process the app icon for all densities."""
    base_dir = "app/src/main/res"
    
    # Process foreground (the "S" with wings)
    for density, multiplier in DENSITIES.items():
        size = int(ICON_SIZES['launcher_foreground'] * multiplier)
        output_dir = f"{base_dir}/mipmap-{density}"
        os.makedirs(output_dir, exist_ok=True)
        output_path = f"{output_dir}/ic_launcher_foreground.png"
        resize_image(source_path, output_path, size)
    
    # Also create a base version
    os.makedirs(f"{base_dir}/mipmap", exist_ok=True)
    resize_image(source_path, f"{base_dir}/mipmap/ic_launcher_foreground.png", 
                 ICON_SIZES['launcher_foreground'])

def process_header_logo(source_path):
    """Process the header logo."""
    base_dir = "app/src/main/res"
    output_dir = f"{base_dir}/drawable"
    os.makedirs(output_dir, exist_ok=True)
    
    # For header logo, we'll use a reasonable size that maintains aspect ratio
    # Android will scale it as needed
    output_path = f"{output_dir}/header_logo.png"
    resize_image(source_path, output_path, ICON_SIZES['header_logo'])
    print(f"Header logo created at: {output_path}")

def main():
    images_dir = "images"
    
    if not os.path.exists(images_dir):
        print(f"Creating {images_dir} directory...")
        os.makedirs(images_dir, exist_ok=True)
        print(f"\nPlease place your images in the '{images_dir}' folder:")
        print("  - app_icon.png (or .jpg) - The 'S' with wings logo")
        print("  - header_logo.png (or .jpg) - The 'SKYWRITER' text logo")
        return
    
    # Process app icon
    app_icon_paths = [
        f"{images_dir}/app_icon.png",
        f"{images_dir}/app_icon.jpg",
        f"{images_dir}/icon.png",
        f"{images_dir}/icon.jpg",
    ]
    
    app_icon = None
    for path in app_icon_paths:
        if os.path.exists(path):
            app_icon = path
            break
    
    if app_icon:
        print("Processing app icon...")
        process_app_icon(app_icon)
    else:
        print("App icon not found. Looking for: app_icon.png, app_icon.jpg, icon.png, or icon.jpg")
    
    # Process header logo
    header_logo_paths = [
        f"{images_dir}/header_logo.png",
        f"{images_dir}/header_logo.jpg",
        f"{images_dir}/logo.png",
        f"{images_dir}/logo.jpg",
    ]
    
    header_logo = None
    for path in header_logo_paths:
        if os.path.exists(path):
            header_logo = path
            break
    
    if header_logo:
        print("\nProcessing header logo...")
        process_header_logo(header_logo)
    else:
        print("Header logo not found. Looking for: header_logo.png, header_logo.jpg, logo.png, or logo.jpg")
    
    if app_icon and header_logo:
        print("\n✓ All images processed successfully!")
        print("Next steps:")
        print("1. The app icon resources have been created in mipmap-* directories")
        print("2. The header logo has been created in drawable/header_logo.png")
        print("3. Run the app build to see the changes")

if __name__ == "__main__":
    try:
        main()
    except ImportError:
        print("Error: PIL (Pillow) is required. Install it with: pip install Pillow")
        sys.exit(1)


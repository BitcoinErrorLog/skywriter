#!/usr/bin/env python3
"""
Compile Amiibo database from downloaded files.

This script:
1. Downloads Amiibo files from GitHub
2. Converts them to JSON
3. Enriches with metadata
4. Organizes into the app's assets directory
"""

import os
import sys
import subprocess
from pathlib import Path

def main():
    script_dir = Path(__file__).parent
    assets_dir = script_dir / "app" / "src" / "main" / "assets" / "Amiibo_NFC_Data"
    temp_download_dir = script_dir / "amiibo_dumps_temp"
    
    print("=" * 60)
    print("Amiibo Database Compilation Script")
    print("=" * 60)
    
    # Step 1: Download from GitHub
    print("\n[1/4] Downloading Amiibo files from GitHub...")
    try:
        subprocess.run([
            sys.executable,
            str(script_dir / "download_amiibo_from_github.py"),
            str(temp_download_dir)
        ], check=True)
    except subprocess.CalledProcessError as e:
        print(f"Error downloading: {e}")
        print("Continuing with existing files if any...")
    
    if not temp_download_dir.exists() or not list(temp_download_dir.glob("*.bin")):
        print("\n⚠️  No Amiibo files found. Please:")
        print("   1. Manually download Amiibo .bin files")
        print("   2. Place them in a directory")
        print("   3. Run: python3 convert_amiibo_to_json.py <your_dir> app/src/main/assets/Amiibo_NFC_Data")
        return
    
    # Step 2: Convert to JSON
    print("\n[2/4] Converting .bin files to JSON...")
    try:
        subprocess.run([
            sys.executable,
            str(script_dir / "convert_amiibo_to_json.py"),
            str(temp_download_dir),
            str(assets_dir)
        ], check=True)
    except subprocess.CalledProcessError as e:
        print(f"Error converting: {e}")
        return
    
    # Step 3: Enrich with metadata
    print("\n[3/4] Enriching with metadata...")
    try:
        subprocess.run([
            sys.executable,
            str(script_dir / "enrich_amiibo_data.py"),
            str(assets_dir),
            "--interactive"
        ], check=False)  # Don't fail if enrichment has issues
    except Exception as e:
        print(f"Enrichment completed with warnings: {e}")
    
    # Step 4: Organize by game series
    print("\n[4/4] Organizing by game series...")
    organize_by_series(assets_dir)
    
    # Cleanup
    if temp_download_dir.exists():
        print(f"\nCleaning up temporary files...")
        import shutil
        shutil.rmtree(temp_download_dir)
    
    print("\n" + "=" * 60)
    print("✓ Amiibo database compilation complete!")
    print(f"  Files are in: {assets_dir}")
    print("=" * 60)

def organize_by_series(json_dir: Path):
    """Organize JSON files into subdirectories by game series."""
    json_dir = Path(json_dir)
    
    if not json_dir.exists():
        return
    
    json_files = list(json_dir.glob("*.json"))
    
    for json_file in json_files:
        try:
            import json
            with open(json_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            game_series = data.get("metadata", {}).get("game_series")
            if not game_series:
                # Try to infer from filename or use "Unknown"
                game_series = "Unknown"
            
            # Create series directory
            series_dir = json_dir / game_series
            series_dir.mkdir(parents=True, exist_ok=True)
            
            # Move file to series directory
            dest_file = series_dir / json_file.name
            if dest_file != json_file:
                json_file.rename(dest_file)
                
        except Exception as e:
            print(f"Error organizing {json_file.name}: {e}")

if __name__ == '__main__':
    main()


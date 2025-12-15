#!/usr/bin/env python3
"""
Convert Amiibo .bin files (540 bytes) to JSON format for the Skywriter app.

Amiibo files are 540 bytes (135 pages × 4 bytes each).
Each page is 4 bytes, represented as 8 hex characters.

Usage:
    python3 convert_amiibo_to_json.py <input_dir> <output_dir>
    
Example:
    python3 convert_amiibo_to_json.py ./amiibo_dumps ./app/src/main/assets/Amiibo_NFC_Data
"""

import os
import sys
import json
from pathlib import Path


def parse_amiibo_bin_file(file_path: Path) -> dict:
    """
    Parse an Amiibo .bin file and convert to JSON structure.
    
    Args:
        file_path: Path to the .bin file
        
    Returns:
        Dictionary with uid, pages, and metadata
    """
    with open(file_path, 'rb') as f:
        data = f.read()
    
    if len(data) != 540:
        raise ValueError(f"Invalid Amiibo file size: {len(data)} bytes (expected 540)")
    
    # Extract UID from first 3 pages (pages 0-2, 12 bytes total)
    # UID is typically 7 bytes, but stored in first 3 pages (12 bytes)
    uid_bytes = data[0:7]  # First 7 bytes are the UID
    uid_hex = ''.join(f'{b:02X}' for b in uid_bytes)
    
    # Convert all 135 pages (4 bytes each) to hex strings
    pages = []
    for i in range(135):
        page_start = i * 4
        page_end = page_start + 4
        page_bytes = data[page_start:page_end]
        page_hex = ''.join(f'{b:02X}' for b in page_bytes)
        pages.append(page_hex)
    
    # Extract character ID and game ID from Amiibo data structure
    # These are typically in pages 21-22 (bytes 84-91)
    # Character ID: bytes 84-87 (4 bytes)
    # Game ID: bytes 88-91 (4 bytes)
    character_id = None
    game_id = None
    
    if len(data) >= 92:
        character_id_bytes = data[84:88]
        game_id_bytes = data[88:92]
        character_id = ''.join(f'{b:02X}' for b in character_id_bytes)
        game_id = ''.join(f'{b:02X}' for b in game_id_bytes)
    
    # Create metadata
    filename = file_path.name
    metadata = {
        "original_filename": filename,
        "original_path": str(file_path),
        "character_name": filename.replace('.bin', '').replace('.nfc', ''),
        "game_series": None,  # Will be filled by enrichment script
        "character_id": character_id,
        "game_id": game_id,
        "biography": None,  # Will be filled by enrichment script
        "release_date": None,  # Will be filled by enrichment script
        "amiibo_type": None  # Will be filled by enrichment script
    }
    
    return {
        "uid": uid_hex,
        "pages": pages,
        "metadata": metadata
    }


def convert_all_amiibo_files(source_dir: Path, output_dir: Path):
    """
    Convert all .bin files in source directory to JSON files in output directory.
    
    Args:
        source_dir: Directory containing .bin files
        output_dir: Directory to write JSON files
    """
    source_dir = Path(source_dir)
    output_dir = Path(output_dir)
    
    if not source_dir.exists():
        print(f"Error: Source directory does not exist: {source_dir}")
        return
    
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Find all .bin and .nfc files
    bin_files = list(source_dir.glob('*.bin')) + list(source_dir.glob('*.nfc'))
    
    if not bin_files:
        print(f"No .bin or .nfc files found in {source_dir}")
        return
    
    print(f"Found {len(bin_files)} Amiibo files to convert")
    
    converted = 0
    errors = 0
    
    for bin_file in bin_files:
        try:
            print(f"Converting: {bin_file.name}")
            amiibo_data = parse_amiibo_bin_file(bin_file)
            
            # Create output filename
            json_filename = bin_file.stem + '.json'
            json_path = output_dir / json_filename
            
            # Write JSON file
            with open(json_path, 'w', encoding='utf-8') as f:
                json.dump(amiibo_data, f, indent=2, ensure_ascii=False)
            
            converted += 1
            print(f"  ✓ Created: {json_path.name}")
            
        except Exception as e:
            errors += 1
            print(f"  ✗ Error converting {bin_file.name}: {e}")
    
    print(f"\nConversion complete:")
    print(f"  Converted: {converted}")
    print(f"  Errors: {errors}")
    print(f"  Output directory: {output_dir}")


def main():
    if len(sys.argv) < 3:
        print("Usage: python3 convert_amiibo_to_json.py <input_dir> <output_dir>")
        print("\nExample:")
        print("  python3 convert_amiibo_to_json.py ./amiibo_dumps ./app/src/main/assets/Amiibo_NFC_Data")
        sys.exit(1)
    
    source_dir = Path(sys.argv[1])
    output_dir = Path(sys.argv[2])
    
    convert_all_amiibo_files(source_dir, output_dir)


if __name__ == '__main__':
    main()


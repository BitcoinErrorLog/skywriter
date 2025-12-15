#!/usr/bin/env python3
"""
Enrich Amiibo JSON files with profile metadata.

Matches Amiibo files by character ID and game ID, then adds:
- Character name
- Game series
- Biography
- Release date
- Amiibo type

Usage:
    python3 enrich_amiibo_data.py <json_dir> [--interactive]
"""

import json
import sys
import argparse
from pathlib import Path
from typing import Dict, Optional
from scrape_amiibo_profiles import get_amiibo_profile, interactive_entry


def load_json_file(file_path: Path) -> Optional[Dict]:
    """Load and parse a JSON file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception as e:
        print(f"Error loading {file_path}: {e}")
        return None


def save_json_file(file_path: Path, data: Dict):
    """Save data to JSON file."""
    try:
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        return True
    except Exception as e:
        print(f"Error saving {file_path}: {e}")
        return False


def enrich_json_file(file_path: Path, interactive: bool = False) -> bool:
    """
    Enrich a single Amiibo JSON file with profile data.
    
    Args:
        file_path: Path to JSON file
        interactive: Whether to prompt for manual entry if data not found
        
    Returns:
        True if file was updated, False otherwise
    """
    data = load_json_file(file_path)
    if not data:
        return False
    
    metadata = data.get("metadata", {})
    character_id = metadata.get("character_id")
    game_id = metadata.get("game_id")
    
    if not character_id:
        print(f"Skipping {file_path.name}: No character_id found")
        return False
    
    # Check if already enriched
    if metadata.get("character_name") and metadata.get("character_name") != file_path.stem:
        # Already has a proper character name, might be enriched
        if metadata.get("game_series") or metadata.get("biography"):
            print(f"Skipping {file_path.name}: Already enriched")
            return False
    
    # Try to get profile data
    profile = get_amiibo_profile(character_id, game_id)
    
    if not profile and interactive:
        # Try interactive entry
        profile = interactive_entry(character_id, game_id, file_path.name)
    
    if not profile:
        print(f"No profile data found for {file_path.name} (ID: {character_id})")
        return False
    
    # Update metadata
    if profile.get("character_name"):
        metadata["character_name"] = profile["character_name"]
    if profile.get("game_series"):
        metadata["game_series"] = profile["game_series"]
    if profile.get("biography"):
        metadata["biography"] = profile["biography"]
    if profile.get("release_date"):
        metadata["release_date"] = profile["release_date"]
    if profile.get("amiibo_type"):
        metadata["amiibo_type"] = profile["amiibo_type"]
    
    data["metadata"] = metadata
    
    # Save updated file
    if save_json_file(file_path, data):
        print(f"✓ Enriched: {file_path.name}")
        return True
    
    return False


def enrich_all_json_files(json_dir: Path, interactive: bool = False):
    """
    Enrich all JSON files in a directory.
    
    Args:
        json_dir: Directory containing JSON files
        interactive: Whether to prompt for manual entry
    """
    json_dir = Path(json_dir)
    
    if not json_dir.exists():
        print(f"Error: Directory does not exist: {json_dir}")
        return
    
    json_files = list(json_dir.glob("*.json"))
    
    if not json_files:
        print(f"No JSON files found in {json_dir}")
        return
    
    print(f"Found {len(json_files)} JSON files to enrich")
    print("=" * 50)
    
    enriched = 0
    skipped = 0
    errors = 0
    
    for json_file in json_files:
        try:
            if enrich_json_file(json_file, interactive):
                enriched += 1
            else:
                skipped += 1
        except Exception as e:
            errors += 1
            print(f"✗ Error processing {json_file.name}: {e}")
    
    print("\n" + "=" * 50)
    print(f"Enrichment complete:")
    print(f"  Enriched: {enriched}")
    print(f"  Skipped: {skipped}")
    print(f"  Errors: {errors}")


def main():
    parser = argparse.ArgumentParser(description="Enrich Amiibo JSON files with profile data")
    parser.add_argument(
        "json_dir",
        type=str,
        help="Directory containing Amiibo JSON files"
    )
    parser.add_argument(
        "--interactive",
        action="store_true",
        help="Interactive mode for manual data entry when profile not found"
    )
    args = parser.parse_args()
    
    enrich_all_json_files(Path(args.json_dir), args.interactive)


if __name__ == '__main__':
    main()


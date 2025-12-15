#!/usr/bin/env python3
"""
Organize Amiibo JSON files by game series based on filename patterns.
"""

import json
import re
from pathlib import Path
from typing import Dict, List

# Game series patterns based on common Amiibo naming
SERIES_PATTERNS = {
    "Super Smash Bros.": [
        r"mario", r"luigi", r"peach", r"bowser", r"yoshi", r"rosalina",
        r"donkey kong", r"diddy kong", r"link", r"zelda", r"sheik", r"ganondorf",
        r"toon link", r"samus", r"zero suit samus", r"pit", r"palutena",
        r"marth", r"ike", r"robin", r"lucina", r"roy", r"corrin",
        r"kirby", r"king dedede", r"meta knight", r"fox", r"falco", r"wolf",
        r"pikachu", r"charizard", r"lucario", r"greninja", r"jigglypuff",
        r"ness", r"lucas", r"captain falcon", r"villager", r"olimar",
        r"wii fit trainer", r"little mac", r"shulk", r"duck hunt",
        r"pac-man", r"mega man", r"sonic", r"mii", r"r.o.b.", r"game & watch",
        r"dr. mario", r"dark pit", r"lucina", r"pac-man", r"shulk",
        r"bowser jr", r"duck hunt", r"ryu", r"cloud", r"bayonetta",
        r"inkling", r"ridley", r"king k. rool", r"isabelle", r"ken",
        r"incineroar", r"piranha plant", r"joker", r"hero", r"banjo",
        r"terry", r"byleth", r"min min", r"steve", r"sephiroth",
        r"pyra", r"mythra", r"kazuya", r"sora"
    ],
    "The Legend of Zelda": [
        r"link", r"zelda", r"ganondorf", r"guardian", r"bokoblin",
        r"mipha", r"daruk", r"revali", r"urbosa", r"loftwing",
        r"ocarina", r"majora", r"wind waker", r"twilight", r"skyward",
        r"awakening", r"8-bit", r"wolf link"
    ],
    "Mario": [
        r"mario", r"luigi", r"peach", r"bowser", r"yoshi", r"rosalina",
        r"toad", r"daisy", r"wario", r"waluigi", r"donkey kong",
        r"diddy kong", r"boo", r"koopa", r"goomba", r"shy guy"
    ],
    "Animal Crossing": [
        r"\[AC\]", r"tom nook", r"isabelle", r"k.k. slider", r"resetti",
        r"digby", r"lottie", r"mabel", r"celeste", r"kicks", r"labelle",
        r"reese", r"cyrus", r"timmy", r"tommy", r"blathers", r"celeste",
        r"kapp'n", r"rover", r"sable", r"harriet", r"pascal", r"copper",
        r"booker", r"phineas", r"pelly", r"phyllis", r"pete", r"porter",
        r"wendell", r"gulliver", r"redd", r"katrina", r"gracie", r"tortimer",
        r"dr. shrunk", r"don", r"isabelle", r"digby", r"k.k. slider",
        r"resetti", r"joan", r"pete", r"pelly", r"phyllis", r"copper",
        r"booker", r"kapp'n", r"rover", r"leilani", r"ena", r"rio"
    ],
    "Kirby": [
        r"kirby", r"meta knight", r"king dedede", r"waddle dee"
    ],
    "Pokemon": [
        r"pikachu", r"charizard", r"lucario", r"greninja", r"jigglypuff",
        r"mewtwo", r"eevee", r"pokemon trainer"
    ],
    "Splatoon": [
        r"inkling", r"squid", r"octoling"
    ],
    "Fire Emblem": [
        r"marth", r"ike", r"robin", r"lucina", r"roy", r"corrin", r"byleth"
    ],
    "Metroid": [
        r"samus", r"zero suit", r"metroid"
    ],
    "Yoshi": [
        r"yarn yoshi", r"yoshi", r"poochy"
    ],
    "Monster Hunter": [
        r"monster hunter", r"rathalos", r"rathian"
    ],
    "Shovel Knight": [
        r"shovel knight"
    ],
    "BoxBoy!": [
        r"qbby", r"boxboy"
    ],
    "Mega Man": [
        r"mega man"
    ],
    "Sonic": [
        r"sonic"
    ],
    "Pac-Man": [
        r"pac-man", r"pacman"
    ],
    "Bayonetta": [
        r"bayonetta"
    ],
    "Dark Souls": [
        r"dark souls", r"solaire"
    ],
    "Diablo": [
        r"diablo", r"lich king"
    ],
    "Skylanders": [
        r"skylanders"
    ],
    "Shovel Knight": [
        r"shovel knight"
    ]
}

def infer_game_series(filename: str) -> str:
    """Infer game series from filename."""
    filename_lower = filename.lower()
    
    # Check patterns in order of specificity
    for series, patterns in SERIES_PATTERNS.items():
        for pattern in patterns:
            if re.search(pattern, filename_lower, re.IGNORECASE):
                return series
    
    # Default to Unknown if no pattern matches
    return "Unknown"

def organize_amiibo_files(json_dir: Path):
    """Organize JSON files into subdirectories by game series."""
    json_dir = Path(json_dir)
    
    if not json_dir.exists():
        print(f"Error: Directory does not exist: {json_dir}")
        return
    
    json_files = list(json_dir.glob("*.json"))
    
    if not json_files:
        print(f"No JSON files found in {json_dir}")
        return
    
    print(f"Organizing {len(json_files)} Amiibo files by game series...")
    
    organized = 0
    errors = 0
    
    for json_file in json_files:
        try:
            # Load JSON to check if it already has game_series
            with open(json_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            metadata = data.get("metadata", {})
            game_series = metadata.get("game_series")
            
            # If no game_series in metadata, infer from filename
            if not game_series:
                game_series = infer_game_series(json_file.stem)
                # Update metadata
                metadata["game_series"] = game_series
                data["metadata"] = metadata
                
                # Save updated file
                with open(json_file, 'w', encoding='utf-8') as f:
                    json.dump(data, f, indent=2, ensure_ascii=False)
            
            # Create series directory
            series_dir = json_dir / game_series
            series_dir.mkdir(parents=True, exist_ok=True)
            
            # Move file to series directory (if not already there)
            dest_file = series_dir / json_file.name
            if dest_file != json_file and not dest_file.exists():
                json_file.rename(dest_file)
                organized += 1
            elif dest_file == json_file:
                # Already in correct location
                organized += 1
                
        except Exception as e:
            errors += 1
            print(f"Error organizing {json_file.name}: {e}")
    
    print(f"\n✓ Organized {organized} files")
    if errors > 0:
        print(f"  Errors: {errors}")

def main():
    import sys
    
    if len(sys.argv) < 2:
        json_dir = Path("app/src/main/assets/Amiibo_NFC_Data")
    else:
        json_dir = Path(sys.argv[1])
    
    organize_amiibo_files(json_dir)

if __name__ == '__main__':
    main()


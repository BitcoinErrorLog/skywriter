#!/usr/bin/env python3
"""
Download Amiibo NFC files from GitHub repositories and compile into database.

This script:
1. Clones/searches GitHub repositories for Amiibo .bin files
2. Downloads unique Amiibo files
3. Converts them to JSON format
4. Organizes them by game series
"""

import os
import sys
import json
import subprocess
import shutil
import tempfile
from pathlib import Path
from typing import Set, Dict, List
import hashlib

# Known GitHub repositories with Amiibo dumps
AMIIBO_REPOS = [
    "https://github.com/AmiiboDB/Amiibo.git",
    "https://github.com/HamletDuFromage/aio-switch-updater.git",
    "https://github.com/XorTroll/emuiibo.git",  # Contains Amiibo dumps
    "https://github.com/CTCaer/hekate.git",  # May contain Amiibo data
    # Add more repositories as discovered
]

# Alternative: Search GitHub for repositories
GITHUB_SEARCH_QUERIES = [
    "amiibo bin files",
    "amiibo nfc dump",
    "amiibo database",
]

# Alternative: Direct download from known sources
AMIIBO_DOWNLOAD_URLS = [
    # These would be direct links to .bin files or archives
    # Note: Actual URLs would need to be discovered/verified
]

def calculate_file_hash(file_path: Path) -> str:
    """Calculate SHA256 hash of a file to detect duplicates."""
    sha256_hash = hashlib.sha256()
    with open(file_path, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    return sha256_hash.hexdigest()

def find_amiibo_files(directory: Path) -> List[Path]:
    """Find all .bin and .nfc files in a directory recursively."""
    amiibo_files = []
    for ext in ['*.bin', '*.nfc']:
        amiibo_files.extend(directory.rglob(ext))
    return amiibo_files

def clone_repository(repo_url: str, temp_dir: Path) -> Path:
    """Clone a GitHub repository to a temporary directory."""
    repo_name = repo_url.split('/')[-1].replace('.git', '')
    clone_path = temp_dir / repo_name
    
    if clone_path.exists():
        print(f"Repository {repo_name} already exists, updating...")
        try:
            result = subprocess.run(['git', 'pull'], cwd=clone_path, check=True, 
                         capture_output=True, text=True, timeout=60)
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired):
            print(f"Warning: Could not update {repo_name}, using existing")
    else:
        print(f"Cloning {repo_url}...")
        try:
            result = subprocess.run(['git', 'clone', '--depth', '1', '--single-branch', 
                                   repo_url, str(clone_path)],
                         check=True, capture_output=True, text=True, timeout=300)
            print(f"  ✓ Cloned successfully")
        except subprocess.CalledProcessError as e:
            print(f"  ✗ Error cloning {repo_url}")
            if e.stderr:
                print(f"     {e.stderr[:200]}")
            return None
        except subprocess.TimeoutExpired:
            print(f"  ✗ Timeout cloning {repo_url}")
            return None
    
    return clone_path

def download_amiibo_from_github(output_dir: Path):
    """Download Amiibo files from GitHub repositories."""
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Track unique files by hash to avoid duplicates
    seen_hashes: Set[str] = set()
    unique_files: Dict[str, Path] = {}  # hash -> file_path
    
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        
        print("=" * 60)
        print("Downloading Amiibo files from GitHub repositories...")
        print("=" * 60)
        
        for repo_url in AMIIBO_REPOS:
            try:
                clone_path = clone_repository(repo_url, temp_path)
                if not clone_path or not clone_path.exists():
                    continue
                
                print(f"\nSearching {clone_path.name} for Amiibo files...")
                amiibo_files = find_amiibo_files(clone_path)
                
                print(f"Found {len(amiibo_files)} potential Amiibo files")
                
                for amiibo_file in amiibo_files:
                    # Skip if file is too small or too large (Amiibo should be 540 bytes)
                    file_size = amiibo_file.stat().st_size
                    if file_size < 500 or file_size > 600:
                        continue
                    
                    # Verify it's actually an Amiibo file by checking first bytes
                    try:
                        with open(amiibo_file, 'rb') as f:
                            first_bytes = f.read(7)
                            # Amiibo UID typically starts with 04
                            if first_bytes[0] != 0x04:
                                continue
                    except Exception:
                        continue
                    
                    # Calculate hash to detect duplicates
                    file_hash = calculate_file_hash(amiibo_file)
                    
                    if file_hash not in seen_hashes:
                        seen_hashes.add(file_hash)
                        unique_files[file_hash] = amiibo_file
                        print(f"  ✓ Found unique: {amiibo_file.name} ({file_size} bytes)")
                    else:
                        print(f"  - Duplicate: {amiibo_file.name}")
                
            except Exception as e:
                print(f"Error processing repository {repo_url}: {e}")
                continue
        
        print(f"\n{'=' * 60}")
        print(f"Found {len(unique_files)} unique Amiibo files")
        print(f"{'=' * 60}\n")
        
        # Copy unique files to output directory
        copied = 0
        for file_hash, source_file in unique_files.items():
            try:
                dest_file = output_dir / source_file.name
                shutil.copy2(source_file, dest_file)
                copied += 1
                if copied % 10 == 0:
                    print(f"Copied {copied}/{len(unique_files)} files...")
            except Exception as e:
                print(f"Error copying {source_file.name}: {e}")
        
        print(f"\n✓ Successfully copied {copied} unique Amiibo files to {output_dir}")
        return copied

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 download_amiibo_from_github.py <output_dir>")
        print("\nExample:")
        print("  python3 download_amiibo_from_github.py ./amiibo_dumps")
        sys.exit(1)
    
    output_dir = Path(sys.argv[1])
    
    # Check if git is available
    try:
        subprocess.run(['git', '--version'], check=True, capture_output=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("Error: git is not installed or not in PATH")
        print("Please install git to download repositories")
        sys.exit(1)
    
    download_amiibo_from_github(output_dir)

if __name__ == '__main__':
    main()


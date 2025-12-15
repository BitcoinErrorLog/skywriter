package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import com.bitcoinerrorlog.skywriter.data.NFCDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and identifies character data from Mifare Classic NFC tags.
 */
class TagReader(private val database: NFCDatabase) {
    
    private val TAG = "TagReader"
    
    /**
     * Reads all blocks from a tag and attempts to identify the character.
     * 
     * Uses a two-pass approach:
     * 1. First, try default keys to read sector trailers
     * 2. Extract keys from sector trailers and use them to read the rest
     * 
     * @param tag The NFC tag to read from
     * @return TagReadResult with character identification if found
     */
    suspend fun readAndIdentifyTag(tag: Tag): TagReadResult = withContext(Dispatchers.IO) {
        val mifare = MifareClassic.get(tag) ?: return@withContext TagReadResult.Error("Not a Mifare Classic tag")
        
        try {
            mifare.connect()
            
            if (!mifare.isConnected) {
                return@withContext TagReadResult.Error("Cannot connect to tag")
            }
            
            val blockCount = mifare.blockCount
            if (blockCount != 64) {
                return@withContext TagReadResult.Error("Tag has $blockCount blocks (expected 64 for 1K)")
            }
            
            // Default keys to try
            val defaultKeys = arrayOf(
                byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
                byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
                byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
                byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte())
            )
            
            // Step 1: Try to read sector trailers with default keys to extract actual keys
            val sectorKeys = mutableMapOf<Int, ByteArray>()
            val readBlocks = MutableList(blockCount) { "00000000000000000000000000000000" }
            
            // Read sector trailers first to extract keys
            for (sector in 0 until mifare.sectorCount) {
                val sectorTrailerBlock = mifare.sectorToBlock(sector) + (mifare.getBlockCountInSector(sector) - 1)
                
                // Try default keys to read sector trailer
                var sectorKey: ByteArray? = null
                for (key in defaultKeys) {
                    try {
                        if (mifare.authenticateSectorWithKeyA(sector, key)) {
                            sectorKey = key
                            // Try to read the sector trailer to extract the actual key
                            try {
                                val trailerData = mifare.readBlock(sectorTrailerBlock)
                                if (trailerData != null && trailerData.size == 16) {
                                    // Extract Key A from bytes 0-5
                                    val extractedKey = trailerData.sliceArray(0..5)
                                    sectorKeys[sector] = extractedKey
                                    Log.d(TAG, "Extracted key for sector $sector")
                                }
                            } catch (e: Exception) {
                                // If we can't read trailer, use the default key that worked
                                sectorKeys[sector] = key
                            }
                            break
                        }
                    } catch (e: Exception) {
                        // Try next key
                    }
                }
                
                // If no default key worked, try to read with any key we extracted from previous sectors
                if (sectorKey == null) {
                    // Try keys we've already extracted
                    for ((prevSector, prevKey) in sectorKeys) {
                        try {
                            if (mifare.authenticateSectorWithKeyA(sector, prevKey)) {
                                sectorKeys[sector] = prevKey
                                sectorKey = prevKey
                                break
                            }
                        } catch (e: Exception) {
                            // Continue
                        }
                    }
                }
            }
            
            // Step 2: Read all blocks using extracted or default keys
            var totalRead = 0
            var totalAuthenticated = 0
            
            for (blockIndex in 0 until blockCount) {
                val sector = mifare.blockToSector(blockIndex)
                val key = sectorKeys[sector] ?: defaultKeys[0] // Use extracted key or default
                
                try {
                    if (mifare.authenticateSectorWithKeyA(sector, key)) {
                        totalAuthenticated++
                        try {
                            val blockData = mifare.readBlock(blockIndex)
                            if (blockData != null && blockData.size == 16) {
                                val hexString = blockData.joinToString("") { "%02X".format(it) }
                                readBlocks[blockIndex] = hexString
                                totalRead++
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to read block $blockIndex: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to authenticate sector $sector for block $blockIndex: ${e.message}")
                }
            }
            
            mifare.close()
            
            Log.d(TAG, "Read $totalRead blocks, authenticated $totalAuthenticated sectors")
            
            // Try to identify the character by comparing blocks
            val identifiedCharacter = identifyCharacter(readBlocks)
            
            TagReadResult.Success(
                blocks = readBlocks,
                identifiedCharacter = identifiedCharacter,
                uid = tag.id.joinToString("") { "%02X".format(it) },
                blocksRead = totalRead,
                sectorsAuthenticated = totalAuthenticated
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reading tag", e)
            try {
                mifare.close()
            } catch (e2: Exception) {
                // Ignore
            }
            TagReadResult.Error("Error reading tag: ${e.message ?: "Unknown error"}")
        }
    }
    
    /**
     * Attempts to identify which character is on the tag by comparing block data.
     * 
     * Compares blocks 1-63 (excluding block 0 which contains UID) to find matching characters.
     */
    private suspend fun identifyCharacter(blocks: List<String>): CharacterModel? {
        if (blocks.size != 64) return null
        
        // Get all characters from database
        val allCharacters = database.loadCharacters()
        
        // Compare blocks 1-63 (skip block 0 which is UID and may differ)
        val tagDataBlocks = blocks.subList(1, 64)
        
        // Find best match by comparing data blocks
        var bestMatch: CharacterModel? = null
        var bestMatchCount = 0
        
        for (character in allCharacters) {
            if (character.blocks.size != 64) continue
            
            // Compare blocks 1-63
            val characterDataBlocks = character.blocks.subList(1, 64)
            var matchCount = 0
            
            for (i in tagDataBlocks.indices) {
                if (tagDataBlocks[i] == characterDataBlocks[i] && 
                    tagDataBlocks[i] != "00000000000000000000000000000000") {
                    matchCount++
                }
            }
            
            // If we have a high match rate (80%+), consider it a match
            if (matchCount > bestMatchCount && matchCount >= (63 * 0.8)) {
                bestMatchCount = matchCount
                bestMatch = character
            }
        }
        
        return bestMatch
    }
}

/**
 * Result of reading a tag.
 */
sealed class TagReadResult {
    data class Success(
        val blocks: List<String>,
        val identifiedCharacter: CharacterModel?,
        val uid: String,
        val blocksRead: Int = 0,
        val sectorsAuthenticated: Int = 0
    ) : TagReadResult()
    
    data class Error(val message: String) : TagReadResult()
}


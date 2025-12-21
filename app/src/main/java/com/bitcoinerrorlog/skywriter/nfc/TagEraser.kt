package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Erases a Mifare Classic tag by writing zeros or default values to all blocks.
 * This allows starting from a fresh, known state.
 */
class TagEraser {
    
    private val TAG = "TagEraser"
    
    // Default keys to try for authentication
    private val defaultKeys = arrayOf(
        byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
        byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
        byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
        byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte())
    )
    
    /**
     * Erases a tag by writing zeros to all data blocks and default values to sector trailers.
     * 
     * @param tag The NFC tag to erase
     * @return EraseResult indicating success or failure
     */
    suspend fun eraseTag(tag: Tag): EraseResult = withContext(Dispatchers.IO) {
        val mifare = MifareClassic.get(tag) ?: return@withContext EraseResult.Error("Not a Mifare Classic tag")
        
        try {
            // Close any existing connection first
            try {
                if (mifare.isConnected) {
                    mifare.close()
                }
            } catch (e: Exception) {
                // Ignore
            }
            
            mifare.connect()
            
            if (!mifare.isConnected) {
                return@withContext EraseResult.Error("Cannot connect to tag")
            }
            
            val blockCount = mifare.blockCount
            if (blockCount != 64) {
                return@withContext EraseResult.Error("Tag has $blockCount blocks (expected 64 for 1K)")
            }
            
            val sectorCount = mifare.sectorCount
            var blocksErased = 0
            var sectorsErased = 0
            
            // Erase each sector
            for (sector in 0 until sectorCount) {
                var authenticated = false
                var authKey: ByteArray? = null
                
                // Try to authenticate with default keys
                // After erase, tags should use default keys, but we try all common keys
                for (key in defaultKeys) {
                    try {
                        if (mifare.authenticateSectorWithKeyA(sector, key)) {
                            authenticated = true
                            authKey = key
                            Log.d(TAG, "Authenticated sector $sector with key ${key.joinToString("") { "%02X".format(it) }}")
                            break
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Authentication failed for sector $sector with key ${key.joinToString("") { "%02X".format(it) }}: ${e.message}")
                        // Try next key
                    }
                }
                
                if (!authenticated) {
                    Log.w(TAG, "Cannot authenticate sector $sector with any default key, skipping...")
                    continue
                }
                
                val firstBlock = mifare.sectorToBlock(sector)
                val blocksInSector = mifare.getBlockCountInSector(sector)
                val sectorTrailerBlock = firstBlock + blocksInSector - 1
                
                // Erase data blocks (not sector trailers)
                for (blockOffset in 0 until (blocksInSector - 1)) {
                    val blockIndex = firstBlock + blockOffset
                    
                    // Skip block 0 (UID) - it's typically locked
                    if (blockIndex == 0) {
                        Log.d(TAG, "Skipping block 0 (UID is locked)")
                        continue
                    }
                    
                    try {
                        // Write zeros to data block
                        val zeroBlock = ByteArray(16) { 0 }
                        mifare.writeBlock(blockIndex, zeroBlock)
                        
                        // Verify (Industrial-grade integrity)
                        try {
                            val verifiedData = mifare.readBlock(blockIndex)
                            if (verifiedData != null && verifiedData.all { it == 0.toByte() }) {
                                blocksErased++
                                Log.d(TAG, "Erased and verified block $blockIndex")
                            } else {
                                Log.w(TAG, "Block $blockIndex write succeeded but verification failed (write-protected?)")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not verify block $blockIndex: ${e.message}")
                            blocksErased++ // Still count as erased if write didn't throw
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to erase block $blockIndex: ${e.message}")
                        // Continue with other blocks
                    }
                }
                
                // Reset sector trailer to default values (if possible)
                try {
                    // Default sector trailer: FF FF FF FF FF FF | 00 00 00 00 | FF | 00 00 00 00 00 00
                    val defaultTrailer = byteArrayOf(
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // Key A (default)
                        0x00, 0x00, 0x00, 0x00, // Access bits
                        0xFF.toByte(), // GPB
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00 // Key B (default) or user data
                    )
                    
                    mifare.writeBlock(sectorTrailerBlock, defaultTrailer)
                    Log.d(TAG, "Reset sector trailer for sector $sector")
                    sectorsErased++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to reset sector trailer for sector $sector: ${e.message}")
                    // Continue - at least data blocks are erased
                }
            }
            
            mifare.close()
            
            if (blocksErased > 0) {
                EraseResult.Success(
                    blocksErased = blocksErased,
                    sectorsErased = sectorsErased,
                    message = "Erased $blocksErased blocks and reset $sectorsErased sector trailers"
                )
            } else {
                EraseResult.Error("Could not erase any blocks. Tag may be locked or use unknown keys.")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error erasing tag", e)
            try {
                mifare.close()
            } catch (e2: Exception) {
                // Ignore
            }
            EraseResult.Error("Error erasing tag: ${e.message ?: "Unknown error"}")
        }
    }
}

/**
 * Result of erasing a tag.
 */
sealed class EraseResult {
    data class Success(
        val blocksErased: Int,
        val sectorsErased: Int,
        val message: String
    ) : EraseResult()
    
    data class Error(val message: String) : EraseResult()
}


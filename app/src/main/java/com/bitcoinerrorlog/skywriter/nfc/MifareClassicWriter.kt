package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles writing character data to Mifare Classic NFC tags.
 * 
 * This class writes ALL blocks including sector trailers to ensure
 * compatibility with Skylanders portal devices. It extracts authentication
 * keys from the source data's sector trailers and uses them for authentication.
 */
class MifareClassicWriter {
    
    private val TAG = "MifareClassicWriter"
    
    // Default keys to try for authentication (fallback)
    private val defaultKeys = arrayOf(
        byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
        byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
        byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
        byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte())
    )
    
    /**
     * Writes character data to a Mifare Classic tag.
     * 
     * IMPORTANT: This writes ALL blocks including sector trailers to ensure
     * compatibility with Skylanders portal devices. The portal requires the
     * sector trailers with correct keys for authentication.
     * 
     * @param tag The NFC tag to write to
     * @param character The character data to write
     * @return WriteResult indicating success or failure
     */
    suspend fun writeCharacter(tag: Tag, character: CharacterModel): WriteResult = withContext(Dispatchers.IO) {
        val mifare = MifareClassic.get(tag) ?: return@withContext WriteResult.TagNotSupported
        
        try {
            // Close any existing connection first to ensure fresh connection
            try {
                if (mifare.isConnected) {
                    mifare.close()
                }
            } catch (e: Exception) {
                // Ignore - connection might not be open
            }
            
            // Try to connect - this may fail if tag is out of range or invalid
            try {
                mifare.connect()
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Failed to connect to tag: ${e.message}")
                return@withContext WriteResult.Error("Tag connection failed. Please keep the tag close and try again.")
            }
            
            if (!mifare.isConnected) {
                return@withContext WriteResult.Error("Tag is not connected. Please keep the tag close and try again.")
            }
            
            // Verify connection is still alive by reading block 0
            try {
                mifare.readBlock(0)
            } catch (e: Exception) {
                Log.e(TAG, "Tag connection test failed: ${e.message}")
                return@withContext WriteResult.Error("Tag connection lost. Please keep the tag close and try again.")
            }
            
            val type = mifare.type
            if (type != MifareClassic.TYPE_CLASSIC && type != MifareClassic.TYPE_PLUS) {
                return@withContext WriteResult.TagNotSupported
            }
            
            // Convert hex strings to byte arrays
            val blocks = character.blocks.map { hexStringToByteArray(it) }
            
            // Extract keys from sector trailers in source data
            val sectorKeys = extractKeysFromSectorTrailers(blocks)
            Log.d(TAG, "Extracted keys for ${sectorKeys.size} sectors")
            
            // Track which sectors we've authenticated to avoid re-authenticating
            val authenticatedSectors = mutableSetOf<Int>()
            
            // Write each block (including sector trailers)
            for ((index, blockData) in blocks.withIndex()) {
                if (index >= mifare.blockCount) {
                    break
                }
                
                val sector = mifare.blockToSector(index)
                
                // Authenticate for the sector only if we haven't already
                var authenticated = authenticatedSectors.contains(sector)
                if (!authenticated) {
                    // Try extracted keys first, then default keys
                    val authKey = sectorKeys[sector]
                    if (authKey != null) {
                        authenticated = authenticateSector(mifare, sector, authKey)
                    }
                    
                    if (!authenticated) {
                        Log.w(TAG, "Failed to authenticate sector $sector with extracted key, trying default keys...")
                        // Try default keys as fallback - this allows overwriting tags with custom keys
                        authenticated = authenticateSectorWithDefaults(mifare, sector)
                    }
                    
                    if (authenticated) {
                        authenticatedSectors.add(sector)
                        Log.d(TAG, "Authenticated sector $sector")
                    } else {
                        Log.w(TAG, "Cannot authenticate sector $sector, but continuing to try writing...")
                        // Don't fail immediately - try to write anyway, some tags may allow it
                    }
                }
                
                // Write the block
                // Block 0 contains UID which is typically locked, but we try anyway
                if (index == 0) {
                    try {
                        mifare.writeBlock(index, blockData)
                        Log.d(TAG, "Successfully wrote Block 0")
                    } catch (e: Exception) {
                        Log.w(TAG, "Block 0 write failed (UID may be locked): ${e.message}")
                        // Continue - UID lock is expected on many tags
                    }
                    continue // Move to next block
                }
                
                // For all other blocks, ensure connection and authentication
                // Check connection before each write
                if (!mifare.isConnected) {
                    Log.w(TAG, "Connection lost, attempting reconnect for block $index")
                    try {
                        mifare.close()
                        mifare.connect()
                        // Re-authenticate if needed
                        if (!authenticatedSectors.contains(sector)) {
                            if (!authenticateSectorWithDefaults(mifare, sector)) {
                                return@withContext WriteResult.Error("Cannot authenticate sector $sector. Tag may be locked or use custom keys.")
                            }
                            authenticatedSectors.add(sector)
                        }
                    } catch (e: Exception) {
                        return@withContext WriteResult.Error("Tag connection lost at block $index. Please keep the tag close and try again.")
                    }
                }
                
                // Ensure we're authenticated for this sector
                if (!authenticatedSectors.contains(sector)) {
                    if (!authenticateSectorWithDefaults(mifare, sector)) {
                        return@withContext WriteResult.Error("Cannot authenticate sector $sector for block $index. Tag may be locked.")
                    }
                    authenticatedSectors.add(sector)
                }
                
                // Attempt to write the block
                try {
                    mifare.writeBlock(index, blockData)
                    Log.d(TAG, "Successfully wrote block $index")
                } catch (e: java.io.IOException) {
                    // Connection error - try to reconnect once
                    Log.w(TAG, "IO error writing block $index, attempting reconnect: ${e.message}")
                    val errorMsg = e.message?.lowercase() ?: ""
                    if (errorMsg.contains("out of date") || errorMsg.contains("tag is out of date") || 
                        errorMsg.contains("connection") || errorMsg.contains("ioexception")) {
                        try {
                            if (!mifare.isConnected) {
                                mifare.connect()
                                // Re-authenticate
                                if (!authenticatedSectors.contains(sector)) {
                                    if (!authenticateSectorWithDefaults(mifare, sector)) {
                                        return@withContext WriteResult.Error("Cannot authenticate sector $sector after reconnect. Please keep the tag close and try again.")
                                    }
                                    authenticatedSectors.add(sector)
                                }
                                mifare.writeBlock(index, blockData)
                                Log.d(TAG, "Successfully wrote block $index after reconnect")
                            } else {
                                return@withContext WriteResult.Error("Tag connection lost. Please keep the tag close and try again.")
                            }
                        } catch (e2: Exception) {
                            return@withContext WriteResult.Error("Tag connection lost. Please keep the tag close and try again.")
                        }
                    } else {
                        // Non-connection IO error - could be write-protected
                        if (isSectorTrailer(index)) {
                            Log.w(TAG, "Sector trailer write failed for block $index: ${e.message}")
                            // Continue - sector trailer might be locked but data blocks should work
                        } else {
                            return@withContext WriteResult.Error("Failed to write block $index: ${e.message}. Tag may be write-protected.")
                        }
                    }
                } catch (e: Exception) {
                    // General exception - could be authentication or write-protection
                    Log.e(TAG, "Failed to write block $index: ${e.message}")
                    if (isSectorTrailer(index)) {
                        Log.w(TAG, "Sector trailer write failed for block $index, continuing...")
                        // Continue - sector trailer might be locked
                    } else {
                        return@withContext WriteResult.Error("Failed to write block $index: ${e.message}")
                    }
                }
            }
            
            WriteResult.Success
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            Log.e(TAG, "IO error during write: ${e.message}")
            val errorMsg = e.message?.lowercase() ?: ""
            if (errorMsg.contains("out of date") || errorMsg.contains("tag is out of date")) {
                return@withContext WriteResult.Error("Tag connection lost. Please keep the tag close and try again.")
            } else {
                return@withContext WriteResult.Error("Tag connection failed. Please keep the tag close and try again.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error during write: ${e.message}")
            WriteResult.Error(e.message ?: "Unknown error")
        } finally {
            try {
                mifare.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Extracts Key A from sector trailer blocks in the source data.
     * 
     * Sector trailer structure (16 bytes):
     * - Bytes 0-5: Key A (6 bytes)
     * - Bytes 6-9: Access bits (4 bytes)
     * - Byte 10: GPB (1 byte)
     * - Bytes 11-15: Key B or user data (6 bytes)
     * 
     * @param blocks List of block data (16 bytes each)
     * @return Map of sector number to Key A (6 bytes)
     */
    private fun extractKeysFromSectorTrailers(blocks: List<ByteArray>): Map<Int, ByteArray> {
        val keys = mutableMapOf<Int, ByteArray>()
        
        for ((index, blockData) in blocks.withIndex()) {
            if (isSectorTrailer(index)) {
                if (blockData.size >= 6) {
                    // Extract Key A (first 6 bytes)
                    val keyA = blockData.sliceArray(0..5)
                    val sector = blockToSector(index)
                    keys[sector] = keyA
                    Log.d(TAG, "Extracted Key A for sector $sector from block $index")
                }
            }
        }
        
        return keys
    }
    
    /**
     * Converts block index to sector number.
     * 
     * Sectors 0-31: 4 blocks each (blocks 0-127)
     * For 1K cards: 16 sectors total
     */
    private fun blockToSector(blockIndex: Int): Int {
        return blockIndex / 4
    }
    
    /**
     * Attempts to authenticate with a sector using a specific key.
     * 
     * @param mifare The MifareClassic instance
     * @param sector The sector number to authenticate
     * @param key The key to use for authentication
     * @return true if authentication succeeded, false otherwise
     */
    private fun authenticateSector(mifare: MifareClassic, sector: Int, key: ByteArray): Boolean {
        return try {
            mifare.authenticateSectorWithKeyA(sector, key)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Attempts to authenticate with a sector using default keys (fallback).
     */
    private fun authenticateSectorWithDefaults(mifare: MifareClassic, sector: Int): Boolean {
        for (key in defaultKeys) {
            if (authenticateSector(mifare, sector, key)) {
                return true
            }
        }
        return false
    }
    
    /**
     * Gets a default key to try for a sector (for initial authentication).
     */
    private fun getDefaultKeyForSector(sector: Int): ByteArray {
        // Try default keys in order
        return defaultKeys[0]
    }
    
    /**
     * Checks if a block index is a sector trailer block.
     * 
     * Sector trailers are at blocks: 3, 7, 11, 15, 19, 23, 27, 31, 35, 39, 43, 47, 51, 55, 59, 63
     * 
     * @param blockIndex The block index to check
     * @return true if the block is a sector trailer
     */
    private fun isSectorTrailer(blockIndex: Int): Boolean {
        // Sector trailers are at blocks: 3, 7, 11, 15, 19, 23, 27, 31, 35, 39, 43, 47, 51, 55, 59, 63
        return (blockIndex + 1) % 4 == 0
    }
    
    /**
     * Converts a hex string to a byte array.
     * 
     * @param hex The hex string (e.g., "21B589A3")
     * @return Byte array representation
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        // Clean hex string: remove spaces, newlines, and non-hex characters
        val cleanHex = hex.replace(Regex("[^0-9A-Fa-f]"), "")
        val len = cleanHex.length
        if (len % 2 != 0) {
            Log.w(TAG, "Hex string has odd length: $hex")
            return ByteArray(0)
        }
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            val char1 = Character.digit(cleanHex[i], 16)
            val char2 = Character.digit(cleanHex[i + 1], 16)
            if (char1 == -1 || char2 == -1) {
                Log.w(TAG, "Invalid hex character in: $hex")
                return ByteArray(0)
            }
            data[i / 2] = ((char1 shl 4) + char2).toByte()
            i += 2
        }
        return data
    }
}

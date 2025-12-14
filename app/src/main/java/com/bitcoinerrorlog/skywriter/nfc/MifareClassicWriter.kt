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
            mifare.connect()
            
            if (!mifare.isConnected) {
                return@withContext WriteResult.WriteFailed
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
            
            // Write each block (including sector trailers)
            for ((index, blockData) in blocks.withIndex()) {
                if (index >= mifare.blockCount) {
                    break
                }
                
                val sector = mifare.blockToSector(index)
                
                // Authenticate for the sector
                val authKey = sectorKeys[sector] ?: getDefaultKeyForSector(sector)
                if (!authenticateSector(mifare, sector, authKey)) {
                    Log.w(TAG, "Failed to authenticate sector $sector, trying default keys...")
                    // Try default keys as fallback
                    if (!authenticateSectorWithDefaults(mifare, sector)) {
                        return@withContext WriteResult.AuthenticationFailed
                    }
                }
                
                // Write the block
                try {
                    // Block 0 contains UID which is typically locked, but we try anyway
                    // The tag will reject it if locked, which is fine
                    if (index == 0) {
                        try {
                            mifare.writeBlock(index, blockData)
                            Log.d(TAG, "Successfully wrote Block 0")
                        } catch (e: Exception) {
                            Log.w(TAG, "Block 0 write failed (UID may be locked): ${e.message}")
                            // Continue - UID lock is expected on many tags
                        }
                    } else {
                        mifare.writeBlock(index, blockData)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "Failed to write block $index: ${e.message}")
                    // For sector trailers, this might fail if keys don't match
                    // But we continue to try writing other blocks
                    if (isSectorTrailer(index)) {
                        Log.w(TAG, "Sector trailer write failed for block $index, continuing...")
                    } else {
                        return@withContext WriteResult.Error("Failed to write block $index: ${e.message}")
                    }
                }
            }
            
            WriteResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
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

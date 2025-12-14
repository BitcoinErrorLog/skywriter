package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles writing character data to Mifare Classic NFC tags.
 * 
 * This class manages authentication and block writing operations.
 * It skips sector trailer blocks to preserve tag security.
 */
class MifareClassicWriter {
    
    // Default keys to try for authentication
    private val defaultKeys = arrayOf(
        byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
        byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
        byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
        byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte())
    )
    
    /**
     * Writes character data to a Mifare Classic tag.
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
            
            // Write each block
            for ((index, blockData) in blocks.withIndex()) {
                if (index >= mifare.blockCount) {
                    break
                }
                
                // Skip sector trailer blocks (blocks 3, 7, 11, 15, 19, 23, 27, 31, 35, 39, 43, 47, 51, 55, 59, 63)
                // These contain keys and access bits
                if (isSectorTrailer(index)) {
                    continue
                }
                
                // Authenticate for the sector containing this block
                val sector = mifare.blockToSector(index)
                if (!authenticateSector(mifare, sector)) {
                    return@withContext WriteResult.AuthenticationFailed
                }
                
                // Write the block
                try {
                    mifare.writeBlock(index, blockData)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext WriteResult.Error("Failed to write block $index: ${e.message}")
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
     * Attempts to authenticate with a sector using default keys.
     * 
     * @param mifare The MifareClassic instance
     * @param sector The sector number to authenticate
     * @return true if authentication succeeded, false otherwise
     */
    private fun authenticateSector(mifare: MifareClassic, sector: Int): Boolean {
        for (key in defaultKeys) {
            try {
                if (mifare.authenticateSectorWithKeyA(sector, key)) {
                    return true
                }
            } catch (e: Exception) {
                // Try next key
            }
        }
        return false
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
        val cleanHex = hex.replace(" ", "").replace("-", "")
        val len = cleanHex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4) + Character.digit(cleanHex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}

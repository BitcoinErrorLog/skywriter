package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.NfcA
import android.util.Log
import com.bitcoinerrorlog.skywriter.data.AmiiboModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles writing Amiibo data to NTAG215 NFC tags.
 * 
 * NTAG215 tags have 135 pages, each 4 bytes (540 bytes total).
 * Uses NfcA technology with page-based writes.
 */
class NTAG215Writer {
    
    private val TAG = "NTAG215Writer"
    
    // NTAG215 write command: 0xA2 + page address + 4 bytes of data
    private val WRITE_COMMAND = 0xA2.toByte()
    
    // NTAG215 has 135 pages (0-134), each 4 bytes
    private val TOTAL_PAGES = 135
    private val BYTES_PER_PAGE = 4
    
    /**
     * Writes Amiibo data to an NTAG215 tag.
     * 
     * @param tag The NFC tag to write to
     * @param amiibo The Amiibo data to write
     * @return WriteResult indicating success or failure
     */
    suspend fun writeAmiibo(tag: Tag, amiibo: AmiiboModel): WriteResult = withContext(Dispatchers.IO) {
        val nfcA = NfcA.get(tag) ?: return@withContext WriteResult.TagNotSupported
        
        try {
            // Close any existing connection first to ensure fresh connection
            try {
                if (nfcA.isConnected) {
                    nfcA.close()
                }
            } catch (e: Exception) {
                // Ignore - connection might not be open
            }
            
            // Try to connect - this may fail if tag is out of range or invalid
            try {
                nfcA.connect()
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Failed to connect to tag: ${e.message}")
                return@withContext WriteResult.Error("Tag connection failed. Please keep the tag close and try again.")
            }
            
            if (!nfcA.isConnected) {
                return@withContext WriteResult.Error("Tag is not connected. Please keep the tag close and try again.")
            }
            
            // Verify connection is still alive by reading a page
            try {
                val testCommand = byteArrayOf(0x30, 0x03) // READ page 3
                nfcA.transceive(testCommand)
            } catch (e: Exception) {
                Log.e(TAG, "Tag connection test failed: ${e.message}")
                return@withContext WriteResult.Error("Tag connection lost. Please keep the tag close and try again.")
            }
            
            // Convert hex strings to byte arrays for each page
            val pages = amiibo.pages.map { hexStringToByteArray(it) }
            
            if (pages.size != TOTAL_PAGES) {
                Log.e(TAG, "Invalid page count: ${pages.size}, expected $TOTAL_PAGES")
                return@withContext WriteResult.Error("Invalid Amiibo data: expected $TOTAL_PAGES pages, got ${pages.size}")
            }
            
            var pagesWritten = 0
            var pagesVerified = 0
            var criticalPagesFailed = mutableListOf<Int>()
            
            // Write each page
            for (pageIndex in 0 until TOTAL_PAGES) {
                val pageData = pages[pageIndex]
                
                if (pageData.size != BYTES_PER_PAGE) {
                    Log.e(TAG, "Invalid page $pageIndex size: ${pageData.size}, expected $BYTES_PER_PAGE")
                    return@withContext WriteResult.Error("Invalid page $pageIndex data")
                }
                
                // Pages 0-2 contain UID which may be locked
                if (pageIndex <= 2) {
                    try {
                        writePage(nfcA, pageIndex, pageData)
                        // Verify UID pages if possible (may fail if locked)
                        try {
                            val readCommand = byteArrayOf(0x30.toByte(), pageIndex.toByte())
                            val readResponse = nfcA.transceive(readCommand)
                            if (readResponse != null && readResponse.size >= BYTES_PER_PAGE) {
                                val writtenData = readResponse.sliceArray(0 until BYTES_PER_PAGE)
                                if (writtenData.contentEquals(pageData)) {
                                    pagesVerified++
                                    Log.d(TAG, "Successfully wrote and verified page $pageIndex (UID page)")
                                } else {
                                    Log.d(TAG, "Page $pageIndex written but UID is locked (expected)")
                                }
                            }
                        } catch (e: Exception) {
                            // UID verification may fail if locked - this is expected
                            Log.d(TAG, "Could not verify UID page $pageIndex (may be locked): ${e.message}")
                        }
                        pagesWritten++
                    } catch (e: Exception) {
                        Log.w(TAG, "Page $pageIndex write failed (UID may be locked): ${e.message}")
                        // Continue - UID lock is expected on many tags
                    }
                } else {
                    // Write data pages (3-134) - these are critical for Amiibo
                    try {
                        // Check connection before each write
                        if (!nfcA.isConnected) {
                            Log.w(TAG, "Connection lost, attempting reconnect for page $pageIndex")
                            try {
                                nfcA.close()
                                nfcA.connect()
                            } catch (e: Exception) {
                                return@withContext WriteResult.Error("Tag connection lost at page $pageIndex. Please keep the tag close and try again.")
                            }
                        }
                        
                        writePage(nfcA, pageIndex, pageData)
                        pagesWritten++
                        
                        // Verify EVERY page written (Industrial-grade integrity)
                        try {
                            val readCommand = byteArrayOf(0x30.toByte(), pageIndex.toByte())
                            val readResponse = nfcA.transceive(readCommand)
                            if (readResponse != null && readResponse.size >= BYTES_PER_PAGE) {
                                val writtenData = readResponse.sliceArray(0 until BYTES_PER_PAGE)
                                if (writtenData.contentEquals(pageData)) {
                                    pagesVerified++
                                    Log.d(TAG, "Successfully wrote and verified page $pageIndex")
                                } else {
                                    Log.e(TAG, "Page $pageIndex write command succeeded but data not written (write-protected?)")
                                    if (pageIndex <= 10) { // Pages 3-10 are critical for identification
                                        criticalPagesFailed.add(pageIndex)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not verify page $pageIndex: ${e.message}")
                        }
                    } catch (e: java.io.IOException) {
                        Log.e(TAG, "IO error writing page $pageIndex: ${e.message}")
                        val errorMsg = e.message?.lowercase() ?: ""
                        if (errorMsg.contains("out of date") || errorMsg.contains("tag is out of date") || 
                            errorMsg.contains("connection") || errorMsg.contains("ioexception")) {
                            // Try to reconnect and continue
                            try {
                                if (!nfcA.isConnected) {
                                    nfcA.connect()
                                    writePage(nfcA, pageIndex, pageData)
                                    pagesWritten++
                                    Log.d(TAG, "Successfully wrote page $pageIndex after reconnect")
                                } else {
                                    return@withContext WriteResult.Error("Tag connection lost. Please keep the tag close and try again.")
                                }
                            } catch (e2: Exception) {
                                return@withContext WriteResult.Error("Tag connection lost. Please keep the tag close and try again.")
                            }
                        } else {
                            return@withContext WriteResult.Error("Failed to write page $pageIndex. Please keep the tag close and try again.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to write page $pageIndex: ${e.message}")
                        return@withContext WriteResult.Error("Failed to write page $pageIndex: ${e.message}")
                    }
                }
            }
            
            // Check if critical pages failed verification
            if (criticalPagesFailed.isNotEmpty()) {
                return@withContext WriteResult.Error(
                    "Write commands succeeded but critical pages (${criticalPagesFailed.joinToString()}) were not written. " +
                    "Tag may be write-protected. Erase may have given false positive."
                )
            }
            
            // Verify at least some pages were written and verified
            if (pagesWritten == 0) {
                return@withContext WriteResult.Error("No pages were written. Tag may be write-protected.")
            }
            
            if (pagesVerified < 5) {
                Log.w(TAG, "Only $pagesVerified pages verified out of $pagesWritten written. Tag may be partially write-protected.")
            }
            
            WriteResult.Success
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            Log.e(TAG, "IO error writing Amiibo: ${e.message}")
            val errorMsg = e.message?.lowercase() ?: ""
            if (errorMsg.contains("out of date") || errorMsg.contains("tag is out of date")) {
                return@withContext WriteResult.Error("Tag connection lost. Please keep the tag close and try again.")
            } else {
                return@withContext WriteResult.Error("Tag connection failed. Please keep the tag close and try again.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error writing Amiibo: ${e.message}")
            WriteResult.Error(e.message ?: "Unknown error")
        } finally {
            try {
                nfcA.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Writes a single page (4 bytes) to the NTAG215 tag.
     * 
     * @param nfcA The NfcA instance
     * @param pageAddress Page address (0-134)
     * @param data 4 bytes of data to write
     * @throws Exception if write fails or tag connection is lost
     */
    private fun writePage(nfcA: NfcA, pageAddress: Int, data: ByteArray) {
        if (data.size != BYTES_PER_PAGE) {
            throw IllegalArgumentException("Page data must be exactly $BYTES_PER_PAGE bytes")
        }
        
        if (pageAddress < 0 || pageAddress >= TOTAL_PAGES) {
            throw IllegalArgumentException("Page address must be between 0 and ${TOTAL_PAGES - 1}")
        }
        
        // NTAG215 write command format: [0xA2, page_address, byte0, byte1, byte2, byte3]
        val command = byteArrayOf(
            WRITE_COMMAND,
            pageAddress.toByte(),
            data[0],
            data[1],
            data[2],
            data[3]
        )
        
        val response = try {
            nfcA.transceive(command)
        } catch (e: java.io.IOException) {
            // Tag connection lost during transceive
            throw java.io.IOException("Tag connection lost during write: ${e.message}", e)
        }
        
        // NTAG215 returns ACK (0x0A) on success, NAK (0x00) on failure
        // Note: Some write-protected tags may return ACK even when write fails
        // Verification by reading back is required to confirm actual write
        if (response == null || response.isEmpty() || response[0] != 0x0A.toByte()) {
            val responseHex = response?.joinToString("") { "%02X".format(it) } ?: "null"
            throw Exception("Write command failed for page $pageAddress. Response: $responseHex")
        }
    }
    
    /**
     * Reads a single page (4 bytes) from the NTAG215 tag.
     * 
     * @param nfcA The NfcA instance
     * @param pageAddress Page address (0-134)
     * @return 4 bytes of data from the page
     * @throws Exception if read fails
     */
    private fun readPage(nfcA: NfcA, pageAddress: Int): ByteArray {
        if (pageAddress < 0 || pageAddress >= TOTAL_PAGES) {
            throw IllegalArgumentException("Page address must be between 0 and ${TOTAL_PAGES - 1}")
        }
        
        val readCommand = byteArrayOf(0x30.toByte(), pageAddress.toByte())
        val response = try {
            nfcA.transceive(readCommand)
        } catch (e: java.io.IOException) {
            throw java.io.IOException("Tag connection lost during read: ${e.message}", e)
        }
        
        if (response == null || response.size < BYTES_PER_PAGE) {
            throw Exception("Read command failed for page $pageAddress. Response: ${response?.joinToString("") { "%02X".format(it) }}")
        }
        
        return response.sliceArray(0 until BYTES_PER_PAGE)
    }
    
    /**
     * Converts a hex string to a byte array.
     * 
     * @param hex The hex string (e.g., "04A1B2C3")
     * @return Byte array representation (4 bytes for a page)
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        // Clean hex string: remove spaces, newlines, and non-hex characters
        val cleanHex = hex.replace(Regex("[^0-9A-Fa-f]"), "")
        val len = cleanHex.length
        
        if (len != BYTES_PER_PAGE * 2) {
            Log.w(TAG, "Hex string has incorrect length: $len (expected ${BYTES_PER_PAGE * 2}) for: $hex")
            // Pad or truncate to 8 hex chars (4 bytes)
            val paddedHex = when {
                len < BYTES_PER_PAGE * 2 -> cleanHex.padEnd(BYTES_PER_PAGE * 2, '0')
                else -> cleanHex.substring(0, BYTES_PER_PAGE * 2)
            }
            return hexStringToByteArrayInternal(paddedHex)
        }
        
        return hexStringToByteArrayInternal(cleanHex)
    }
    
    private fun hexStringToByteArrayInternal(hex: String): ByteArray {
        val data = ByteArray(BYTES_PER_PAGE)
        for (i in 0 until BYTES_PER_PAGE) {
            val char1 = Character.digit(hex[i * 2], 16)
            val char2 = Character.digit(hex[i * 2 + 1], 16)
            if (char1 == -1 || char2 == -1) {
                throw IllegalArgumentException("Invalid hex character in: $hex")
            }
            data[i] = ((char1 shl 4) + char2).toByte()
        }
        return data
    }
}


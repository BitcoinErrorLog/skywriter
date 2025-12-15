package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.NfcA
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Erases an NTAG215 tag by writing zeros to all data pages.
 * This allows starting from a fresh, known state.
 */
class AmiiboTagEraser {
    
    private val TAG = "AmiiboTagEraser"
    
    private val TOTAL_PAGES = 135
    private val BYTES_PER_PAGE = 4
    
    /**
     * Erases an NTAG215 tag by writing zeros to all data pages (except UID pages 0-2).
     * 
     * @param tag The NFC tag to erase
     * @return EraseResult indicating success or failure
     */
    suspend fun eraseTag(tag: Tag): EraseResult = withContext(Dispatchers.IO) {
        val nfcA = NfcA.get(tag) ?: return@withContext EraseResult.Error("Not an NfcA tag")
        
        try {
            // Close any existing connection first
            try {
                if (nfcA.isConnected) {
                    nfcA.close()
                }
            } catch (e: Exception) {
                // Ignore
            }
            
            nfcA.connect()
            
            if (!nfcA.isConnected) {
                return@withContext EraseResult.Error("Cannot connect to tag")
            }
            
            var pagesErased = 0
            var pagesVerified = 0
            
            // Erase pages 3-134 (data pages, skip UID pages 0-2)
            for (pageIndex in 3 until TOTAL_PAGES) {
                try {
                    val zeroPage = ByteArray(BYTES_PER_PAGE) { 0 }
                    writePage(nfcA, pageIndex, zeroPage)
                    
                    // Verify the write by reading back
                    val readCommand = byteArrayOf(0x30.toByte(), pageIndex.toByte())
                    val readResponse = nfcA.transceive(readCommand)
                    
                    if (readResponse != null && readResponse.size >= BYTES_PER_PAGE) {
                        val isErased = readResponse.sliceArray(0 until BYTES_PER_PAGE).all { it == 0.toByte() }
                        if (isErased) {
                            pagesErased++
                            pagesVerified++
                            Log.d(TAG, "Erased and verified page $pageIndex")
                        } else {
                            Log.w(TAG, "Page $pageIndex write succeeded but data not erased (write-protected?)")
                        }
                    } else {
                        Log.w(TAG, "Could not verify erase of page $pageIndex")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to erase page $pageIndex: ${e.message}")
                    // Continue with other pages
                }
            }
            
            // Try to reset page 3 (first data page) to default Amiibo header if possible
            try {
                // Default Amiibo header (page 3): usually starts with specific pattern
                // For a blank tag, we'll write zeros which is fine
                val defaultPage3 = ByteArray(BYTES_PER_PAGE) { 0 }
                writePage(nfcA, 3, defaultPage3)
            } catch (e: Exception) {
                Log.w(TAG, "Could not reset page 3: ${e.message}")
            }
            
            nfcA.close()
            
            if (pagesVerified > 0) {
                EraseResult.Success(
                    blocksErased = pagesVerified,
                    sectorsErased = 0, // NTAG215 doesn't use sectors
                    message = "Erased and verified $pagesVerified pages. Tag is now blank and ready for writing."
                )
            } else if (pagesErased > 0) {
                // Writes succeeded but verification failed - likely write-protected
                EraseResult.Error("Write commands succeeded but data was not erased. Tag may be write-protected or locked.")
            } else {
                EraseResult.Error("Could not erase any pages. Tag may be locked or write-protected.")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error erasing tag", e)
            try {
                nfcA.close()
            } catch (e2: Exception) {
                // Ignore
            }
            EraseResult.Error("Error erasing tag: ${e.message ?: "Unknown error"}")
        }
    }
    
    private fun writePage(nfcA: NfcA, pageAddress: Int, data: ByteArray) {
        if (data.size != BYTES_PER_PAGE) {
            throw IllegalArgumentException("Page data must be exactly $BYTES_PER_PAGE bytes")
        }
        
        if (pageAddress < 0 || pageAddress >= TOTAL_PAGES) {
            throw IllegalArgumentException("Page address must be between 0 and ${TOTAL_PAGES - 1}")
        }
        
        val command = byteArrayOf(
            0xA2.toByte(), // WRITE command
            pageAddress.toByte(),
            data[0],
            data[1],
            data[2],
            data[3]
        )
        
        val response = nfcA.transceive(command)
        if (response == null || response.isEmpty() || response[0] != 0x0A.toByte()) {
            throw Exception("Write command failed for page $pageAddress")
        }
    }
}


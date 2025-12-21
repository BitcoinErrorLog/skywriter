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
            
            // Erase pages 4-134 (data pages, skip UID pages 0-2 and CC page 3 for special handling)
            for (pageIndex in 4 until TOTAL_PAGES) {
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
            
            // Special handling for Page 3 (Capability Container)
            // Reset to default NTAG215 CC: E1 10 3E 00
            try {
                val ntag215CC = byteArrayOf(0xE1.toByte(), 0x10.toByte(), 0x3E.toByte(), 0x00.toByte())
                writePage(nfcA, 3, ntag215CC)
                
                // Verify CC page
                val readCommand = byteArrayOf(0x30.toByte(), 0x03.toByte())
                val readResponse = nfcA.transceive(readCommand)
                if (readResponse != null && readResponse.size >= BYTES_PER_PAGE) {
                    val verifiedData = readResponse.sliceArray(0 until BYTES_PER_PAGE)
                    if (verifiedData.contentEquals(ntag215CC)) {
                        pagesVerified++
                        Log.d(TAG, "Reset and verified Page 3 (CC) to NTAG215 defaults")
                    } else {
                        Log.w(TAG, "Page 3 (CC) reset succeeded but data not verified")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not reset Page 3 (CC): ${e.message}")
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


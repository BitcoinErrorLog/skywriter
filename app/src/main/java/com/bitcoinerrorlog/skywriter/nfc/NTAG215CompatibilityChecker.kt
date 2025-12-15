package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.NfcA
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Checks NFC tag compatibility for Amiibo use.
 * 
 * Verifies:
 * - Tag type (must be NTAG215)
 * - Page count (must be 135 pages = 540 bytes)
 * - Read/write capability
 * - UID changeability (warning if locked)
 */
class NTAG215CompatibilityChecker {
    
    private val TAG = "NTAG215CompatibilityChecker"
    
    // NTAG215 identification constants
    private val NTAG215_ATQA = 0x0044
    private val NTAG215_SAK = 0x00
    private val NTAG215_TOTAL_PAGES = 135
    private val BYTES_PER_PAGE = 4
    
    /**
     * Performs a comprehensive compatibility check on a tag.
     * 
     * @param tag The NFC tag to check
     * @return TagCompatibilityInfo with detailed analysis
     */
    suspend fun checkCompatibility(tag: Tag): TagCompatibilityInfo = withContext(Dispatchers.IO) {
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // Get tag ID (UID)
        val tagId = tag.id
        val uidHex = tagId.joinToString("") { "%02X".format(it) }
        
        // Check if it supports NfcA technology
        val nfcA = NfcA.get(tag)
        if (nfcA == null) {
            // Try to identify tag type
            val techList = tag.techList.joinToString(", ")
            return@withContext TagCompatibilityInfo(
                result = CompatibilityResult.Incompatible(
                    reason = "Not an NTAG215 tag",
                    details = listOf("Tag type: $techList", "Amiibo requires NTAG215 tags")
                ),
                tagType = techList,
                blockCount = 0,
                sectorCount = 0,
                uid = uidHex,
                uidChangeable = null,
                authenticationTest = false,
                readable = false,
                writable = false,
                issues = listOf("Tag does not support NfcA technology", "Amiibo requires NTAG215"),
                recommendations = listOf(
                    "Use NTAG215 tags (540 bytes, 135 pages)",
                    "Common incompatible types: Mifare Classic, NTAG213/216, FeliCa"
                )
            )
        }
        
        try {
            nfcA.connect()
            
            if (!nfcA.isConnected) {
                return@withContext TagCompatibilityInfo(
                    result = CompatibilityResult.Incompatible(
                        reason = "Cannot connect to tag",
                        details = listOf("Tag may be out of range or damaged")
                    ),
                    tagType = "NfcA (unknown)",
                    blockCount = 0,
                    sectorCount = 0,
                    uid = uidHex,
                    uidChangeable = null,
                    authenticationTest = false,
                    readable = false,
                    writable = false,
                    issues = listOf("Cannot establish connection with tag"),
                    recommendations = listOf("Ensure tag is close to device", "Try a different tag")
                )
            }
            
            // Get ATQA and SAK for identification
            val atqa = nfcA.atqa
            val sak = nfcA.sak
            
            // Check if it's NTAG215
            val isNTAG215 = (atqa != null && atqa.size >= 2 && 
                            ((atqa[0].toInt() and 0xFF) or ((atqa[1].toInt() and 0xFF) shl 8)) == NTAG215_ATQA) &&
                            (sak.toInt() == NTAG215_SAK)
            
            if (!isNTAG215) {
                val atqaHex = atqa?.joinToString("") { "%02X".format(it) } ?: "unknown"
                issues.add("Tag ATQA/SAK does not match NTAG215 (ATQA: $atqaHex, SAK: ${sak.toString(16)})")
                recommendations.add("Use NTAG215 tags (ATQA: 0x0044, SAK: 0x00)")
            }
            
            // Test read capability
            var readable = false
            try {
                // Try to read page 3 (first data page, not UID)
                val readCommand = byteArrayOf(0x30.toByte(), 0x03) // READ command, page 3
                val response = nfcA.transceive(readCommand)
                if (response != null && response.size >= BYTES_PER_PAGE) {
                    readable = true
                    Log.d(TAG, "Successfully read page 3")
                }
            } catch (e: Exception) {
                issues.add("Cannot read from tag: ${e.message}")
                recommendations.add("Tag may be damaged or incompatible")
            }
            
            // Test write capability with verification
            var writable = false
            var uidChangeable: Boolean? = null
            
            if (readable) {
                try {
                    // Try to read page 3 first to get original data
                    val readCommand = byteArrayOf(0x30.toByte(), 0x03)
                    val originalData = nfcA.transceive(readCommand)
                    
                    if (originalData != null && originalData.size >= BYTES_PER_PAGE) {
                        // Create test data (invert first byte to ensure we're actually writing)
                        val testData = byteArrayOf(
                            (originalData[0].toInt() xor 0xFF).toByte(), // Invert first byte
                            originalData[1],
                            originalData[2],
                            originalData[3]
                        )
                        
                        // Try to write test data
                        val writeCommand = byteArrayOf(
                            0xA2.toByte(), // WRITE command
                            0x03.toByte(), // Page 3
                            testData[0],
                            testData[1],
                            testData[2],
                            testData[3]
                        )
                        val writeResponse = nfcA.transceive(writeCommand)
                        
                        if (writeResponse != null && writeResponse.isNotEmpty() && writeResponse[0] == 0x0A.toByte()) {
                            // Verify the write by reading back
                            val verifyRead = nfcA.transceive(readCommand)
                            if (verifyRead != null && verifyRead.size >= BYTES_PER_PAGE) {
                                val writtenData = verifyRead.sliceArray(0 until BYTES_PER_PAGE)
                                val writeVerified = writtenData.contentEquals(testData)
                                
                                if (writeVerified) {
                                    writable = true
                                    Log.d(TAG, "Successfully wrote and verified page 3 (test write)")
                                    
                                    // Restore original data
                                    val restoreCommand = byteArrayOf(
                                        0xA2.toByte(),
                                        0x03.toByte(),
                                        originalData[0],
                                        originalData[1],
                                        originalData[2],
                                        originalData[3]
                                    )
                                    nfcA.transceive(restoreCommand)
                                } else {
                                    issues.add("Write command succeeded but data was not written (write-protected)")
                                    recommendations.add("Tag appears to be write-protected despite successful write command")
                                    Log.w(TAG, "Write command succeeded but verification failed - tag may be write-protected")
                                }
                            } else {
                                issues.add("Write succeeded but could not verify by reading back")
                                recommendations.add("Tag may be write-protected or damaged")
                            }
                        } else {
                            issues.add("Write command failed: ${writeResponse?.joinToString { "%02X".format(it) }}")
                            recommendations.add("Tag may be write-protected")
                        }
                    }
                } catch (e: Exception) {
                    issues.add("Cannot write to tag: ${e.message}")
                    recommendations.add("Tag may be write-protected")
                }
                
                // Test UID changeability (pages 0-2)
                try {
                    val readUidCommand = byteArrayOf(0x30.toByte(), 0x00) // Read page 0
                    val uidData = nfcA.transceive(readUidCommand)
                    
                    if (uidData != null && uidData.size >= BYTES_PER_PAGE) {
                        // Try to write page 0 (this will fail if UID is locked)
                        try {
                            val writeUidCommand = byteArrayOf(
                                0xA2.toByte(),
                                0x00.toByte(),
                                uidData[0],
                                uidData[1],
                                uidData[2],
                                uidData[3]
                            )
                            val writeUidResponse = nfcA.transceive(writeUidCommand)
                            
                            if (writeUidResponse != null && writeUidResponse.isNotEmpty() && writeUidResponse[0] == 0x0A.toByte()) {
                                uidChangeable = true
                            } else {
                                uidChangeable = false
                            }
                        } catch (e: Exception) {
                            uidChangeable = false
                            // This is expected and not a critical issue
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Could not determine UID changeability: ${e.message}")
                }
            }
            
            nfcA.close()
            
            // Determine overall compatibility result
            val result = when {
                !isNTAG215 -> {
                    CompatibilityResult.Incompatible(
                        reason = "Tag type incompatible",
                        details = issues
                    )
                }
                !readable -> {
                    CompatibilityResult.Incompatible(
                        reason = "Tag is not readable",
                        details = issues
                    )
                }
                !writable -> {
                    CompatibilityResult.Incompatible(
                        reason = "Tag is not writable",
                        details = issues
                    )
                }
                uidChangeable == false -> {
                    CompatibilityResult.Warning(
                        message = "Tag is compatible but UID is locked",
                        details = listOf(
                            "Amiibo data will work correctly",
                            "UID cannot be changed to match original Amiibo",
                            "Nintendo Switch should still recognize the Amiibo"
                        )
                    )
                }
                else -> {
                    CompatibilityResult.Compatible
                }
            }
            
            TagCompatibilityInfo(
                result = result,
                tagType = if (isNTAG215) "NTAG215" else "NfcA (unknown)",
                blockCount = NTAG215_TOTAL_PAGES, // Using blockCount field to represent pages
                sectorCount = 0, // NTAG215 doesn't use sectors
                uid = uidHex,
                uidChangeable = uidChangeable,
                authenticationTest = true, // NTAG215 doesn't require authentication like Mifare
                readable = readable,
                writable = writable,
                issues = issues,
                recommendations = recommendations
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking compatibility", e)
            TagCompatibilityInfo(
                result = CompatibilityResult.Incompatible(
                    reason = "Error checking tag: ${e.message}",
                    details = listOf(e.message ?: "Unknown error")
                ),
                tagType = "NfcA (error)",
                blockCount = 0,
                sectorCount = 0,
                uid = uidHex,
                uidChangeable = null,
                authenticationTest = false,
                readable = false,
                writable = false,
                issues = listOf("Error during compatibility check: ${e.message}"),
                recommendations = listOf("Try again", "Ensure tag is close to device")
            )
        }
    }
}


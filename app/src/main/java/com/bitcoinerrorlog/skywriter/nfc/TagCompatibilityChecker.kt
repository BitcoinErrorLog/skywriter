package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of a tag compatibility check.
 */
sealed class CompatibilityResult {
    /**
     * Tag is fully compatible and ready to use.
     */
    data object Compatible : CompatibilityResult()
    
    /**
     * Tag has issues but may still work (e.g., UID locked).
     */
    data class Warning(
        val message: String,
        val details: List<String> = emptyList()
    ) : CompatibilityResult()
    
    /**
     * Tag is incompatible and cannot be used.
     */
    data class Incompatible(
        val reason: String,
        val details: List<String> = emptyList()
    ) : CompatibilityResult()
}

/**
 * Detailed information about a tag's compatibility status.
 */
data class TagCompatibilityInfo(
    val result: CompatibilityResult,
    val tagType: String,
    val blockCount: Int,
    val sectorCount: Int,
    val uid: String?,
    val uidChangeable: Boolean?,
    val authenticationTest: Boolean,
    val readable: Boolean,
    val writable: Boolean,
    val issues: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

/**
 * Checks NFC tag compatibility for Skylanders portal use.
 * 
 * Verifies:
 * - Tag type (must be Mifare Classic 1K)
 * - Block count (must be 64 blocks)
 * - Authentication capability
 * - Read/write capability
 * - UID changeability (warning if locked)
 */
class TagCompatibilityChecker {
    
    private val TAG = "TagCompatibilityChecker"
    
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
        
        // Check if it's Mifare Classic
        val mifare = MifareClassic.get(tag)
        if (mifare == null) {
            // Try to identify tag type
            val techList = tag.techList.joinToString(", ")
            return@withContext TagCompatibilityInfo(
                result = CompatibilityResult.Incompatible(
                    reason = "Not a Mifare Classic tag",
                    details = listOf("Tag type: $techList", "Skylanders portal requires Mifare Classic 1K tags")
                ),
                tagType = techList,
                blockCount = 0,
                sectorCount = 0,
                uid = uidHex,
                uidChangeable = null,
                authenticationTest = false,
                readable = false,
                writable = false,
                issues = listOf("Tag is not Mifare Classic", "Portal requires Mifare Classic 1K"),
                recommendations = listOf(
                    "Use Mifare Classic 1K tags (64 blocks)",
                    "Common incompatible types: NTAG213/215/216, FeliCa, ISO-DEP"
                )
            )
        }
        
        try {
            mifare.connect()
            
            if (!mifare.isConnected) {
                return@withContext TagCompatibilityInfo(
                    result = CompatibilityResult.Incompatible(
                        reason = "Cannot connect to tag",
                        details = listOf("Tag may be out of range or damaged")
                    ),
                    tagType = "Mifare Classic (unknown size)",
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
            
            val type = mifare.type
            val blockCount = mifare.blockCount
            val sectorCount = mifare.sectorCount
            
            // Check tag type
            val isClassic = type == MifareClassic.TYPE_CLASSIC || type == MifareClassic.TYPE_PLUS
            if (!isClassic) {
                issues.add("Tag is not Mifare Classic (type: $type)")
                recommendations.add("Use Mifare Classic 1K tags")
            }
            
            // Check block count (must be 64 for 1K)
            val is1K = blockCount == 64
            if (!is1K) {
                issues.add("Tag has $blockCount blocks (need 64 for 1K)")
                if (blockCount == 256) {
                    recommendations.add("This appears to be a 4K tag. Use 1K tags instead.")
                } else {
                    recommendations.add("Use Mifare Classic 1K tags with exactly 64 blocks")
                }
            }
            
            // Test authentication on first sector
            var authenticationTest = false
            val defaultKeys = arrayOf(
                byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
                byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
                byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte())
            )
            
            for (key in defaultKeys) {
                try {
                    if (mifare.authenticateSectorWithKeyA(0, key)) {
                        authenticationTest = true
                        break
                    }
                } catch (e: Exception) {
                    // Try next key
                }
            }
            
            if (!authenticationTest) {
                issues.add("Cannot authenticate with default keys")
                recommendations.add("Tag may use custom keys or be locked")
            }
            
            // Test read capability
            var readable = false
            try {
                val testBlock = mifare.readBlock(0)
                readable = testBlock != null && testBlock.size == 16
            } catch (e: Exception) {
                issues.add("Cannot read from tag: ${e.message}")
            }
            
            // Test write capability (try writing block 1, not block 0 which contains UID)
            var writable = false
            var uidChangeable: Boolean? = null
            
            try {
                // First, try to read block 0 to check UID
                val block0 = mifare.readBlock(0)
                if (block0 != null) {
                    // Check if we can write to block 0 (UID changeability test)
                    try {
                        // Don't actually write, just check if we can authenticate for write
                        // We'll test write on block 1 instead
                        uidChangeable = null // Can't determine without actually trying to write
                    } catch (e: Exception) {
                        // UID likely locked
                    }
                }
                
                // Test write on block 1 (data block)
                if (blockCount > 1 && authenticationTest) {
                    try {
                        val originalData = mifare.readBlock(1)
                        // Try to write the same data back
                        mifare.writeBlock(1, originalData)
                        writable = true
                    } catch (e: Exception) {
                        issues.add("Cannot write to tag: ${e.message}")
                        recommendations.add("Tag may be write-protected")
                    }
                }
            } catch (e: Exception) {
                issues.add("Error testing write capability: ${e.message}")
            }
            
            // Determine UID changeability (block 0 write test)
            if (authenticationTest && blockCount > 0) {
                try {
                    val block0 = mifare.readBlock(0)
                    // Try to write block 0 (this will fail if UID is locked)
                    try {
                        mifare.writeBlock(0, block0)
                        uidChangeable = true
                    } catch (e: Exception) {
                        uidChangeable = false
                        // This is expected and not a critical issue
                    }
                } catch (e: Exception) {
                    // Can't determine
                }
            }
            
            mifare.close()
            
            // Determine overall compatibility result
            val result = when {
                !isClassic || !is1K -> {
                    CompatibilityResult.Incompatible(
                        reason = "Tag type or size incompatible",
                        details = issues
                    )
                }
                !authenticationTest -> {
                    CompatibilityResult.Incompatible(
                        reason = "Cannot authenticate with tag",
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
                            "Character data will work correctly",
                            "UID cannot be changed to match original figures",
                            "Portal should still recognize the character"
                        )
                    )
                }
                else -> {
                    CompatibilityResult.Compatible
                }
            }
            
            TagCompatibilityInfo(
                result = result,
                tagType = when {
                    !isClassic -> "Unknown/Incompatible"
                    is1K -> "Mifare Classic 1K"
                    blockCount == 256 -> "Mifare Classic 4K"
                    else -> "Mifare Classic (unknown size)"
                },
                blockCount = blockCount,
                sectorCount = sectorCount,
                uid = uidHex,
                uidChangeable = uidChangeable,
                authenticationTest = authenticationTest,
                readable = readable,
                writable = writable,
                issues = issues,
                recommendations = recommendations
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking tag compatibility", e)
            TagCompatibilityInfo(
                result = CompatibilityResult.Incompatible(
                    reason = "Error checking tag: ${e.message}",
                    details = listOf(e.toString())
                ),
                tagType = "Unknown",
                blockCount = 0,
                sectorCount = 0,
                uid = uidHex,
                uidChangeable = null,
                authenticationTest = false,
                readable = false,
                writable = false,
                issues = listOf("Error: ${e.message}"),
                recommendations = listOf("Try again or use a different tag")
            )
        } finally {
            try {
                if (mifare.isConnected) {
                    mifare.close()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}


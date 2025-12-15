package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.NfcA
import android.util.Log
import com.bitcoinerrorlog.skywriter.data.AmiiboModel
import com.bitcoinerrorlog.skywriter.data.AmiiboDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and identifies Amiibo data from NTAG215 NFC tags.
 */
class AmiiboTagReader(private val database: AmiiboDatabase) {
    
    private val TAG = "AmiiboTagReader"
    
    private val TOTAL_PAGES = 135
    private val BYTES_PER_PAGE = 4
    
    /**
     * Reads all pages from an NTAG215 tag and attempts to identify the Amiibo.
     * 
     * @param tag The NFC tag to read from
     * @return AmiiboReadResult with Amiibo identification if found
     */
    suspend fun readAndIdentifyTag(tag: Tag): AmiiboReadResult = withContext(Dispatchers.IO) {
        val nfcA = NfcA.get(tag) ?: return@withContext AmiiboReadResult.Error("Not an NfcA tag")
        
        try {
            nfcA.connect()
            
            if (!nfcA.isConnected) {
                return@withContext AmiiboReadResult.Error("Cannot connect to tag")
            }
            
            // Read all 135 pages
            val readPages = mutableListOf<String>()
            var pagesRead = 0
            
            for (pageIndex in 0 until TOTAL_PAGES) {
                try {
                    val pageData = readPage(nfcA, pageIndex)
                    val hexString = pageData.joinToString("") { "%02X".format(it) }
                    readPages.add(hexString)
                    pagesRead++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read page $pageIndex: ${e.message}")
                    readPages.add("00000000") // Placeholder
                }
            }
            
            nfcA.close()
            
            // Try to identify the Amiibo by comparing page data
            val identifiedAmiibo = identifyAmiibo(readPages)
            
            AmiiboReadResult.Success(
                pages = readPages,
                identifiedAmiibo = identifiedAmiibo,
                uid = tag.id.joinToString("") { "%02X".format(it) },
                pagesRead = pagesRead
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reading tag", e)
            try {
                nfcA.close()
            } catch (e2: Exception) {
                // Ignore
            }
            AmiiboReadResult.Error("Error reading tag: ${e.message ?: "Unknown error"}")
        }
    }
    
    /**
     * Attempts to identify which Amiibo is on the tag by comparing page data.
     * 
     * Compares pages 21-22 (character ID and game ID) and other key pages.
     */
    private suspend fun identifyAmiibo(pages: List<String>): AmiiboModel? {
        if (pages.size != TOTAL_PAGES) return null
        
        // Extract character ID and game ID from pages 21-22
        val characterId = if (pages.size > 21) {
            pages[21].substring(0, 8) // First 4 bytes of page 21
        } else null
        
        val gameId = if (pages.size > 22) {
            pages[22].substring(0, 8) // First 4 bytes of page 22
        } else null
        
        if (characterId == null || characterId == "00000000") {
            return null
        }
        
        // Get all Amiibos from database
        val allAmiibos = database.loadAmiibos()
        
        // Try to find match by character ID and game ID
        for (amiibo in allAmiibos) {
            val amiiboCharId = amiibo.metadata.characterId
            val amiiboGameId = amiibo.metadata.gameId
            
            if (amiiboCharId != null && amiiboCharId.equals(characterId, ignoreCase = true)) {
                // If game ID matches too, it's a perfect match
                if (gameId != null && amiiboGameId != null && 
                    amiiboGameId.equals(gameId, ignoreCase = true)) {
                    return amiibo
                }
                // Character ID match is good enough
                if (gameId == null || amiiboGameId == null) {
                    return amiibo
                }
            }
        }
        
        // If no exact match, try comparing more pages (pages 3-20 contain Amiibo data)
        var bestMatch: AmiiboModel? = null
        var bestMatchCount = 0
        
        val tagDataPages = pages.subList(3, 21) // Pages 3-20
        
        for (amiibo in allAmiibos) {
            if (amiibo.pages.size != TOTAL_PAGES) continue
            
            val amiiboDataPages = amiibo.pages.subList(3, 21)
            var matchCount = 0
            
            for (i in tagDataPages.indices) {
                if (tagDataPages[i] == amiiboDataPages[i] && 
                    tagDataPages[i] != "00000000") {
                    matchCount++
                }
            }
            
            // If we have a high match rate (80%+), consider it a match
            if (matchCount > bestMatchCount && matchCount >= (18 * 0.8)) {
                bestMatchCount = matchCount
                bestMatch = amiibo
            }
        }
        
        return bestMatch
    }
    
    private fun readPage(nfcA: NfcA, pageAddress: Int): ByteArray {
        val command = byteArrayOf(0x30, pageAddress.toByte()) // READ command
        val response = nfcA.transceive(command)
        if (response == null || response.size != BYTES_PER_PAGE) {
            throw Exception("Failed to read page $pageAddress")
        }
        return response
    }
}

/**
 * Result of reading an Amiibo tag.
 */
sealed class AmiiboReadResult {
    data class Success(
        val pages: List<String>,
        val identifiedAmiibo: AmiiboModel?,
        val uid: String,
        val pagesRead: Int = 0
    ) : AmiiboReadResult()
    
    data class Error(val message: String) : AmiiboReadResult()
}


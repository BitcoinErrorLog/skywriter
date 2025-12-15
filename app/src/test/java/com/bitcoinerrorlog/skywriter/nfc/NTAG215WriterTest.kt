package com.bitcoinerrorlog.skywriter.nfc

import com.bitcoinerrorlog.skywriter.data.AmiiboModel
import com.bitcoinerrorlog.skywriter.data.AmiiboMetadata
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NTAG215WriterTest {
    
    private lateinit var writer: NTAG215Writer
    
    @Before
    fun setup() {
        writer = NTAG215Writer()
    }
    
    @Test
    fun testWriterCreation() {
        // When creating NTAG215Writer
        // Then should not be null
        assertNotNull("Writer should be created", writer)
    }
    
    @Test
    fun testAmiiboModelStructure() {
        // Create a test Amiibo model
        val pages = List(135) { "00000000" } // 135 pages, each 4 bytes (8 hex chars)
        val metadata = AmiiboMetadata(
            originalFilename = "test.bin",
            originalPath = "/test/test.bin",
            characterName = "Test Character",
            gameSeries = "Test Series"
        )
        val amiibo = AmiiboModel(
            uid = "04000000000000",
            pages = pages,
            metadata = metadata
        )
        
        // Then should have correct structure
        assertEquals("Should have 135 pages", 135, amiibo.pages.size)
        assertEquals("Should have correct UID", "04000000000000", amiibo.uid)
        assertEquals("Should have correct character name", "Test Character", amiibo.metadata.characterName)
    }
    
    @Test
    fun testPageFormat() {
        // Each page should be 8 hex characters (4 bytes)
        val testPage = "04A1B2C3"
        
        // Then should be valid format
        assertEquals("Page should be 8 hex characters", 8, testPage.length)
        assertTrue("Page should contain only hex characters", testPage.matches(Regex("[0-9A-Fa-f]{8}")))
    }
}


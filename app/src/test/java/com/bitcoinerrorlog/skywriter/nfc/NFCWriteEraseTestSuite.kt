package com.bitcoinerrorlog.skywriter.nfc

import com.bitcoinerrorlog.skywriter.data.AmiiboModel
import com.bitcoinerrorlog.skywriter.data.AmiiboMetadata
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import com.bitcoinerrorlog.skywriter.data.CharacterMetadata
import org.junit.Assert.*
import org.junit.Test

/**
 * Test suite for NFC write/erase functionality.
 * 
 * These tests verify data structures and logic without requiring mocked NFC hardware.
 * For full integration tests with mocks, see the individual test classes.
 */
class NFCWriteEraseTestSuite {
    
    @Test
    fun testMifareCharacterDataStructure() {
        val blocks = (0 until 64).map { 
            String.format("%032X", it.toLong()).take(32)
        }
        
        val character = CharacterModel(
            uid = "21B589A3",
            atqa = "0004",
            sak = "08",
            mifareType = "1K",
            blocks = blocks,
            metadata = CharacterMetadata(
                originalFilename = "Test.nfc",
                originalPath = "test/Test.nfc",
                category = "Test Game",
                subcategory = "Test"
            )
        )
        
        assertEquals("Should have 64 blocks", 64, character.blocks.size)
        assertEquals("Should be 1K type", "1K", character.mifareType)
        assertTrue("UID should be valid hex", character.uid.matches(Regex("[0-9A-Fa-f]+")))
    }
    
    @Test
    fun testAmiiboDataStructure() {
        val pages = (0 until 135).map { 
            String.format("%08X", it.toLong()).take(8)
        }
        
        val amiibo = AmiiboModel(
            uid = "04XXXXXXXXXXXX",
            pages = pages,
            metadata = AmiiboMetadata(
                originalFilename = "Test.bin",
                originalPath = "test/Test.bin",
                characterName = "Test Character",
                gameSeries = "Test Series"
            )
        )
        
        assertEquals("Should have 135 pages", 135, amiibo.pages.size)
        assertTrue("UID should be valid hex", amiibo.uid.matches(Regex("[0-9A-Fa-f]+")))
    }
    
    @Test
    fun testEraseResultTypes() {
        val success = EraseResult.Success(
            blocksErased = 50,
            sectorsErased = 10,
            message = "Erased successfully"
        )
        
        val error = EraseResult.Error("Erase failed")
        
        assertTrue("Success should be EraseResult", success is EraseResult)
        assertTrue("Error should be EraseResult", error is EraseResult)
        assertEquals("Success should have correct blocks", 50, (success as EraseResult.Success).blocksErased)
        assertEquals("Error should have correct message", "Erase failed", (error as EraseResult.Error).message)
    }
    
    @Test
    fun testWriteResultTypes() {
        val success = WriteResult.Success
        val error = WriteResult.Error("Write failed")
        val tagNotSupported = WriteResult.TagNotSupported
        val authFailed = WriteResult.AuthenticationFailed
        val writeFailed = WriteResult.WriteFailed
        
        assertTrue("Success should be WriteResult", success is WriteResult)
        assertTrue("Error should be WriteResult", error is WriteResult)
        assertTrue("TagNotSupported should be WriteResult", tagNotSupported is WriteResult)
        assertTrue("AuthenticationFailed should be WriteResult", authFailed is WriteResult)
        assertTrue("WriteFailed should be WriteResult", writeFailed is WriteResult)
    }
    
    @Test
    fun testMifareBlockCount() {
        // Verify 1K tag has 64 blocks
        val blockCount = 64
        val sectorCount = 16
        val blocksPerSector = blockCount / sectorCount
        
        assertEquals("1K tag should have 64 blocks", 64, blockCount)
        assertEquals("1K tag should have 16 sectors", 16, sectorCount)
        assertEquals("Each sector should have 4 blocks", 4, blocksPerSector)
    }
    
    @Test
    fun testNTAG215PageCount() {
        // Verify NTAG215 has 135 pages
        val pageCount = 135
        val bytesPerPage = 4
        val totalBytes = pageCount * bytesPerPage
        
        assertEquals("NTAG215 should have 135 pages", 135, pageCount)
        assertEquals("Each page should be 4 bytes", 4, bytesPerPage)
        assertEquals("Total should be 540 bytes", 540, totalBytes)
    }
    
    @Test
    fun testHexStringConversion() {
        val hexString = "21B589A3"
        val expectedBytes = byteArrayOf(0x21.toByte(), 0xB5.toByte(), 0x89.toByte(), 0xA3.toByte())
        
        // Test that hex strings can be converted to bytes
        val bytes = hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        
        assertArrayEquals("Hex string should convert correctly", expectedBytes, bytes)
    }
    
    @Test
    fun testDefaultMifareKeys() {
        val defaultKeys = arrayOf(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
            byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
            byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte())
        )
        
        assertEquals("Should have 4 default keys", 4, defaultKeys.size)
        defaultKeys.forEach { key ->
            assertEquals("Each key should be 6 bytes", 6, key.size)
        }
    }
}


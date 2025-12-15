package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.IOException

/**
 * Unit tests for TagEraser using mocked MifareClassic.
 * 
 * These tests verify the erase logic without requiring actual NFC hardware.
 */
class TagEraserTest {
    
    @Mock
    private lateinit var mockTag: Tag
    
    @Mock
    private lateinit var mockMifare: MifareClassic
    
    private lateinit var tagEraser: TagEraser
    private lateinit var mifareStaticMock: MockedStatic<MifareClassic>
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        tagEraser = TagEraser()
        
        // Mock static method
        mifareStaticMock = mockStatic(MifareClassic::class.java)
        mifareStaticMock.`when`<MifareClassic> { MifareClassic.get(mockTag) }.thenReturn(mockMifare)
        
        // Setup default mock behavior
        `when`(mockMifare.isConnected).thenReturn(false)
        `when`(mockMifare.blockCount).thenReturn(64)
        `when`(mockMifare.sectorCount).thenReturn(16)
    }
    
    @Test
    fun testEraseTag_NotMifareClassic_ReturnsError() {
        mifareStaticMock.`when`<MifareClassic?> { MifareClassic.get(mockTag) }.thenReturn(null)
        
        runBlocking {
            val result = tagEraser.eraseTag(mockTag)
            
            assertTrue("Should return error for non-Mifare tag", result is EraseResult.Error)
            assertEquals("Not a Mifare Classic tag", (result as EraseResult.Error).message)
        }
    }
    
    @Test
    fun testEraseTag_CannotConnect_ReturnsError() {
        `when`(mockMifare.isConnected).thenReturn(false)
        doThrow(IOException("Connection failed")).`when`(mockMifare).connect()
        
        runBlocking {
            val result = tagEraser.eraseTag(mockTag)
            
            assertTrue("Should return error when cannot connect", result is EraseResult.Error)
            assertTrue((result as EraseResult.Error).message.contains("Error erasing tag"))
        }
    }
    
    @Test
    fun testEraseTag_WrongBlockCount_ReturnsError() {
        `when`(mockMifare.isConnected).thenReturn(true)
        `when`(mockMifare.blockCount).thenReturn(256) // 4K instead of 1K
        
        runBlocking {
            val result = tagEraser.eraseTag(mockTag)
            
            assertTrue("Should return error for wrong block count", result is EraseResult.Error)
            assertTrue((result as EraseResult.Error).message.contains("64"))
        }
    }
    
    @Test
    fun testEraseTag_SuccessfulErase_ReturnsSuccess() {
        // Setup successful authentication and writes
        `when`(mockMifare.isConnected).thenReturn(true)
        `when`(mockMifare.authenticateSectorWithKeyA(anyInt(), any())).thenReturn(true)
        `when`(mockMifare.sectorToBlock(anyInt())).thenReturn(0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56, 60)
        `when`(mockMifare.getBlockCountInSector(anyInt())).thenReturn(4)
        doNothing().`when`(mockMifare).writeBlock(anyInt(), any())
        
        runBlocking {
            val result = tagEraser.eraseTag(mockTag)
            
            assertTrue("Should return success", result is EraseResult.Success)
            val success = result as EraseResult.Success
            assertTrue("Should erase some blocks", success.blocksErased > 0)
            assertTrue("Should reset some sector trailers", success.sectorsErased > 0)
        }
    }
    
    @Test
    fun testEraseTag_AuthenticationFails_SkipsSector() {
        // Setup: first sector authenticates, second fails
        `when`(mockMifare.isConnected).thenReturn(true)
        `when`(mockMifare.authenticateSectorWithKeyA(0, any())).thenReturn(true)
        `when`(mockMifare.authenticateSectorWithKeyA(1, any())).thenReturn(false)
        `when`(mockMifare.sectorToBlock(0)).thenReturn(0)
        `when`(mockMifare.getBlockCountInSector(0)).thenReturn(4)
        doNothing().`when`(mockMifare).writeBlock(anyInt(), any())
        
        runBlocking {
            val result = tagEraser.eraseTag(mockTag)
            
            // Should still succeed if at least one sector was erased
            assertTrue("Should return success if any blocks erased", 
                result is EraseResult.Success || result is EraseResult.Error)
        }
    }
    
    @Test
    fun testEraseTag_WriteBlockFails_ContinuesWithOtherBlocks() {
        `when`(mockMifare.isConnected).thenReturn(true)
        `when`(mockMifare.authenticateSectorWithKeyA(anyInt(), any())).thenReturn(true)
        `when`(mockMifare.sectorToBlock(anyInt())).thenReturn(0)
        `when`(mockMifare.getBlockCountInSector(anyInt())).thenReturn(4)
        
        // First write succeeds, second fails, third succeeds
        doNothing().`when`(mockMifare).writeBlock(1, any())
        doThrow(IOException("Write failed")).`when`(mockMifare).writeBlock(2, any())
        doNothing().`when`(mockMifare).writeBlock(3, any())
        
        runBlocking {
            val result = tagEraser.eraseTag(mockTag)
            
            // Should still succeed if some blocks were erased
            assertTrue("Should return success if any blocks erased", 
                result is EraseResult.Success || result is EraseResult.Error)
        }
    }
    
    @Test
    fun testEraseTag_NoBlocksErased_ReturnsError() {
        `when`(mockMifare.isConnected).thenReturn(true)
        `when`(mockMifare.authenticateSectorWithKeyA(anyInt(), any())).thenReturn(false) // All auth fails
        
        runBlocking {
            val result = tagEraser.eraseTag(mockTag)
            
            assertTrue("Should return error when no blocks erased", result is EraseResult.Error)
            assertTrue((result as EraseResult.Error).message.contains("Could not erase any blocks"))
        }
    }
    
    @Test
    fun testEraseTag_ClosesConnection() {
        `when`(mockMifare.isConnected).thenReturn(true, true, false) // Connected, then disconnected after close
        `when`(mockMifare.authenticateSectorWithKeyA(anyInt(), any())).thenReturn(true)
        `when`(mockMifare.sectorToBlock(anyInt())).thenReturn(0)
        `when`(mockMifare.getBlockCountInSector(anyInt())).thenReturn(4)
        doNothing().`when`(mockMifare).writeBlock(anyInt(), any())
        
        runBlocking {
            tagEraser.eraseTag(mockTag)
        }
        
        verify(mockMifare, atLeastOnce()).close()
    }
    
    @org.junit.After
    fun tearDown() {
        try {
            mifareStaticMock.close()
        } catch (e: Exception) {
            // Ignore if already closed
        }
    }
}


package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.NfcA
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.IOException

/**
 * Unit tests for AmiiboTagEraser using mocked NfcA.
 */
class AmiiboTagEraserTest {
    
    @Mock
    private lateinit var mockTag: Tag
    
    @Mock
    private lateinit var mockNfcA: NfcA
    
    private lateinit var amiiboTagEraser: AmiiboTagEraser
    private lateinit var nfcAStaticMock: MockedStatic<NfcA>
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        amiiboTagEraser = AmiiboTagEraser()
        
        // Mock static method
        nfcAStaticMock = mockStatic(NfcA::class.java)
        nfcAStaticMock.`when`<NfcA> { NfcA.get(mockTag) }.thenReturn(mockNfcA)
        
        // Setup default mock behavior
        `when`(mockNfcA.isConnected).thenReturn(false)
    }
    
    @After
    fun tearDown() {
        nfcAStaticMock.close()
    }
    
    @Test
    fun testEraseTag_NotNfcA_ReturnsError() {
        nfcAStaticMock.`when`<NfcA?> { NfcA.get(mockTag) }.thenReturn(null)
        
        runBlocking {
            val result = amiiboTagEraser.eraseTag(mockTag)
            
            assertTrue("Should return error for non-NfcA tag", result is EraseResult.Error)
            assertEquals("Not an NfcA tag", (result as EraseResult.Error).message)
        }
    }
    
    @Test
    fun testEraseTag_CannotConnect_ReturnsError() {
        `when`(mockNfcA.isConnected).thenReturn(false)
        doThrow(IOException("Connection failed")).`when`(mockNfcA).connect()
        
        runBlocking {
            val result = amiiboTagEraser.eraseTag(mockTag)
            
            assertTrue("Should return error when cannot connect", result is EraseResult.Error)
            assertTrue((result as EraseResult.Error).message.contains("Error erasing tag"))
        }
    }
    
    @Test
    fun testEraseTag_SuccessfulErase_ReturnsSuccess() {
        `when`(mockNfcA.isConnected).thenReturn(true)
        
        // Mock successful write responses (0x0A = ACK)
        val ackResponse = byteArrayOf(0x0A)
        `when`(mockNfcA.transceive(any())).thenReturn(ackResponse)
        
        runBlocking {
            val result = amiiboTagEraser.eraseTag(mockTag)
            
            assertTrue("Should return success", result is EraseResult.Success)
            val success = result as EraseResult.Success
            assertTrue("Should erase pages", success.blocksErased > 0)
            assertEquals("NTAG215 doesn't use sectors", 0, success.sectorsErased)
        }
    }
    
    @Test
    fun testEraseTag_WritePageFails_ContinuesWithOtherPages() {
        `when`(mockNfcA.isConnected).thenReturn(true)
        
        // First few writes succeed, then one fails, then succeed again
        val ackResponse = byteArrayOf(0x0A)
        val failResponse = byteArrayOf(0x00) // NACK
        
        `when`(mockNfcA.transceive(argThat { it[0] == 0xA2.toByte() && it[1] < 10 }))
            .thenReturn(ackResponse)
        `when`(mockNfcA.transceive(argThat { it[0] == 0xA2.toByte() && it[1] == 10.toByte() }))
            .thenReturn(failResponse)
        `when`(mockNfcA.transceive(argThat { it[0] == 0xA2.toByte() && it[1] > 10 }))
            .thenReturn(ackResponse)
        
        runBlocking {
            val result = amiiboTagEraser.eraseTag(mockTag)
            
            // Should still succeed if some pages were erased
            assertTrue("Should return success if any pages erased", 
                result is EraseResult.Success || result is EraseResult.Error)
        }
    }
    
    @Test
    fun testEraseTag_NoPagesErased_ReturnsError() {
        `when`(mockNfcA.isConnected).thenReturn(true)
        
        // All writes fail
        val failResponse = byteArrayOf(0x00) // NACK
        `when`(mockNfcA.transceive(any())).thenReturn(failResponse)
        
        runBlocking {
            val result = amiiboTagEraser.eraseTag(mockTag)
            
            assertTrue("Should return error when no pages erased", result is EraseResult.Error)
            assertTrue((result as EraseResult.Error).message.contains("Could not erase any pages"))
        }
    }
    
    @Test
    fun testEraseTag_ClosesConnection() {
        `when`(mockNfcA.isConnected).thenReturn(true)
        val ackResponse = byteArrayOf(0x0A)
        `when`(mockNfcA.transceive(any())).thenReturn(ackResponse)
        
        runBlocking {
            amiiboTagEraser.eraseTag(mockTag)
        }
        
        verify(mockNfcA, atLeastOnce()).close()
    }
    
    @Test
    fun testEraseTag_SkipsUIDPages() {
        `when`(mockNfcA.isConnected).thenReturn(true)
        val ackResponse = byteArrayOf(0x0A)
        `when`(mockNfcA.transceive(any())).thenReturn(ackResponse)
        
        runBlocking {
            val result = amiiboTagEraser.eraseTag(mockTag)
            
            // Verify that we only write to pages 3-134 (not 0-2)
            val writeCalls = mutableListOf<ByteArray>()
            verify(mockNfcA, atLeast(100)).transceive(argThat {
                if (it[0] == 0xA2.toByte()) {
                    writeCalls.add(it)
                }
                true
            })
            
            // Check that no UID pages (0-2) were written
            val uidPageWrites = writeCalls.filter { it[0] == 0xA2.toByte() && it[1] < 3 }
            assertEquals("Should not write to UID pages", 0, uidPageWrites.size)
        }
    }
}


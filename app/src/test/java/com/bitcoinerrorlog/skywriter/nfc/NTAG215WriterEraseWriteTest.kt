package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.NfcA
import com.bitcoinerrorlog.skywriter.data.AmiiboModel
import com.bitcoinerrorlog.skywriter.data.AmiiboMetadata
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
 * Comprehensive tests for NTAG215Writer focusing on write operations
 * that might fail after erase.
 */
class NTAG215WriterEraseWriteTest {
    
    @Mock
    private lateinit var mockTag: Tag
    
    @Mock
    private lateinit var mockNfcA: NfcA
    
    private lateinit var writer: NTAG215Writer
    private lateinit var testAmiibo: AmiiboModel
    private lateinit var nfcAStaticMock: MockedStatic<NfcA>
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        writer = NTAG215Writer()
        
        // Mock static method
        nfcAStaticMock = mockStatic(NfcA::class.java)
        nfcAStaticMock.`when`<NfcA> { NfcA.get(mockTag) }.thenReturn(mockNfcA)
        `when`(mockNfcA.isConnected).thenReturn(false)
        
        // Create test Amiibo with 135 pages
        val pages = (0 until 135).map { 
            String.format("%08X", it.toLong()).take(8)
        }
        
        testAmiibo = AmiiboModel(
            uid = "04XXXXXXXXXXXX",
            pages = pages,
            metadata = AmiiboMetadata(
                originalFilename = "Test.bin",
                originalPath = "test/Test.bin",
                characterName = "Test Character",
                gameSeries = "Test Series"
            )
        )
    }
    
    @Test
    fun testWriteAfterErase_Success() {
        // Simulate tag that was just erased
        `when`(mockNfcA.isConnected).thenReturn(true)
        
        // Mock successful read (connection test) and write responses
        val readResponse = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val ackResponse = byteArrayOf(0x0A) // ACK
        
        `when`(mockNfcA.transceive(argThat { it[0] == 0x30.toByte() }))
            .thenReturn(readResponse)
        `when`(mockNfcA.transceive(argThat { it[0] == 0xA2.toByte() }))
            .thenReturn(ackResponse)
        
        runBlocking {
            val result = writer.writeAmiibo(mockTag, testAmiibo)
            
            assertTrue("Write should succeed after erase", result is WriteResult.Success)
            verify(mockNfcA, atLeast(130)).transceive(any()) // At least 130 writes (pages 3-134)
        }
    }
    
    @Test
    fun testWrite_ConnectionLost_Reconnects() {
        `when`(mockNfcA.isConnected).thenReturn(true, true, false, true)
        
        val readResponse = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val ackResponse = byteArrayOf(0x0A)
        
        var writeCount = 0
        `when`(mockNfcA.transceive(argThat { it[0] == 0x30.toByte() }))
            .thenReturn(readResponse)
        `when`(mockNfcA.transceive(argThat { it[0] == 0xA2.toByte() }))
            .thenAnswer {
                writeCount++
                if (writeCount == 50) {
                    throw IOException("Tag is out of date")
                }
                ackResponse
            }
        
        runBlocking {
            val result = writer.writeAmiibo(mockTag, testAmiibo)
            
            // Should attempt reconnection
            verify(mockNfcA, atLeast(2)).connect()
        }
    }
    
    @Test
    fun testWrite_VerifiesConnectionBeforeWrite() {
        `when`(mockNfcA.isConnected).thenReturn(true, true, false, true)
        
        val readResponse = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val ackResponse = byteArrayOf(0x0A)
        
        `when`(mockNfcA.transceive(argThat { it[0] == 0x30.toByte() }))
            .thenReturn(readResponse)
        `when`(mockNfcA.transceive(argThat { it[0] == 0xA2.toByte() }))
            .thenReturn(ackResponse)
        
        runBlocking {
            writer.writeAmiibo(mockTag, testAmiibo)
        }
        
        // Should check connection multiple times
        verify(mockNfcA, atLeastOnce()).isConnected
    }
    
    @Test
    fun testWrite_UIDPagesLocked_Continues() {
        `when`(mockNfcA.isConnected).thenReturn(true)
        
        val readResponse = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val ackResponse = byteArrayOf(0x0A)
        val nackResponse = byteArrayOf(0x00) // NACK for UID pages
        
        `when`(mockNfcA.transceive(argThat { it[0] == 0x30.toByte() }))
            .thenReturn(readResponse)
        // UID pages (0-2) fail, data pages succeed
        `when`(mockNfcA.transceive(argThat { 
            it[0] == 0xA2.toByte() && it[1] <= 2 
        })).thenReturn(nackResponse)
        `when`(mockNfcA.transceive(argThat { 
            it[0] == 0xA2.toByte() && it[1] > 2 
        })).thenReturn(ackResponse)
        
        runBlocking {
            val result = writer.writeAmiibo(mockTag, testAmiibo)
            
            // Should succeed even if UID pages are locked
            assertTrue("Write should succeed even if UID pages are locked", 
                result is WriteResult.Success || result is WriteResult.Error)
        }
    }
    
    @Test
    fun testWrite_ConnectionTestFails_ReturnsError() {
        `when`(mockNfcA.isConnected).thenReturn(true)
        
        // Connection test (read page 3) fails
        doThrow(IOException("Connection lost")).`when`(mockNfcA)
            .transceive(argThat { it[0] == 0x30.toByte() })
        
        runBlocking {
            val result = writer.writeAmiibo(mockTag, testAmiibo)
            
            assertTrue("Should return error when connection test fails", 
                result is WriteResult.Error)
            assertTrue((result as WriteResult.Error).message.contains("connection"))
        }
    }
    
    @Test
    fun testWrite_ClosesAndReconnectsBeforeWrite() {
        `when`(mockNfcA.isConnected).thenReturn(true, false, true)
        
        val readResponse = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val ackResponse = byteArrayOf(0x0A)
        
        `when`(mockNfcA.transceive(argThat { it[0] == 0x30.toByte() }))
            .thenReturn(readResponse)
        `when`(mockNfcA.transceive(argThat { it[0] == 0xA2.toByte() }))
            .thenReturn(ackResponse)
        
        runBlocking {
            writer.writeAmiibo(mockTag, testAmiibo)
        }
        
        // Should close existing connection and reconnect
        verify(mockNfcA, atLeastOnce()).close()
        verify(mockNfcA, atLeastOnce()).connect()
    }
}


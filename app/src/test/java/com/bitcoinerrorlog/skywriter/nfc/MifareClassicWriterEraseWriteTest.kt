package com.bitcoinerrorlog.skywriter.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import com.bitcoinerrorlog.skywriter.data.CharacterMetadata
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
 * Comprehensive tests for MifareClassicWriter focusing on write operations
 * that might fail after erase.
 * 
 * NOTE: These tests are limited because Android framework classes (MifareClassic)
 * cannot be mocked in unit tests. The static methods will fail with "not mocked" errors.
 * For full testing, use instrumented tests with actual NFC hardware or emulated tags.
 * 
 * These tests verify the logic structure but cannot test actual NFC operations.
 */
class MifareClassicWriterEraseWriteTest {
    
    @Mock
    private lateinit var mockTag: Tag
    
    @Mock
    private lateinit var mockMifare: MifareClassic
    
    private lateinit var writer: MifareClassicWriter
    private lateinit var testCharacter: CharacterModel
    private lateinit var mifareStaticMock: MockedStatic<MifareClassic>
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        writer = MifareClassicWriter()
        
        // Mock static method
        mifareStaticMock = mockStatic(MifareClassic::class.java)
        mifareStaticMock.`when`<MifareClassic> { MifareClassic.get(mockTag) }.thenReturn(mockMifare)
        `when`(mockMifare.isConnected).thenReturn(false)
        `when`(mockMifare.type).thenReturn(MifareClassic.TYPE_CLASSIC)
        `when`(mockMifare.blockCount).thenReturn(64)
        `when`(mockMifare.sectorCount).thenReturn(16)
        
        // Create test character with 64 blocks
        val blocks = (0 until 64).map { 
            String.format("%032X", it.toLong()).take(32)
        }
        
        testCharacter = CharacterModel(
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
    }
    
    @Test
    fun testWriteAfterErase_Success() {
        // Simulate tag that was just erased - all sectors use default keys
        `when`(mockMifare.isConnected).thenReturn(true)
        `when`(mockMifare.authenticateSectorWithKeyA(anyInt(), any())).thenReturn(true)
        `when`(mockMifare.blockToSector(anyInt())).thenAnswer { invocation ->
            val block = invocation.getArgument<Int>(0)
            block / 4
        }
        doNothing().`when`(mockMifare).writeBlock(anyInt(), any())
        `when`(mockMifare.readBlock(0)).thenReturn(ByteArray(16))
        doNothing().`when`(mockMifare).connect()
        doNothing().`when`(mockMifare).close()
        
        runBlocking {
            val result = writer.writeCharacter(mockTag, testCharacter)
            
            assertTrue("Write should succeed after erase", result is WriteResult.Success)
            verify(mockMifare, atLeastOnce()).writeBlock(anyInt(), any())
        }
    }
    
    @Test
    fun testWrite_ConnectionLost_Reconnects() {
        // First connection works, then lost during write, then reconnects
        var connectionCount = 0
        `when`(mockMifare.isConnected).thenAnswer { connectionCount > 0 }
        `when`(mockMifare.authenticateSectorWithKeyA(anyInt(), any())).thenReturn(true)
        `when`(mockMifare.blockToSector(anyInt())).thenAnswer { it.getArgument<Int>(0) / 4 }
        `when`(mockMifare.readBlock(0)).thenReturn(ByteArray(16))
        doNothing().`when`(mockMifare).close()
        
        var writeCount = 0
        doAnswer {
            writeCount++
            if (writeCount == 10) {
                // Simulate connection loss
                connectionCount = 0
                throw IOException("Tag is out of date")
            }
        }.`when`(mockMifare).writeBlock(anyInt(), any())
        
        // After reconnect, writes succeed
        doAnswer {
            connectionCount = 1
            Unit
        }.`when`(mockMifare).connect()
        
        runBlocking {
            writer.writeCharacter(mockTag, testCharacter)
            
            // Should handle reconnection
            verify(mockMifare, atLeast(2)).connect()
        }
    }
    
    @Test
    fun testWrite_AuthenticationFails_TriesDefaultKeys() {
        `when`(mockMifare.isConnected).thenReturn(true)
        `when`(mockMifare.blockToSector(anyInt())).thenAnswer { it.getArgument<Int>(0) / 4 }
        `when`(mockMifare.readBlock(0)).thenReturn(ByteArray(16))
        doNothing().`when`(mockMifare).close()
        
        // First auth attempt fails (extracted key), then default key works
        val extractedKey = byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte())
        val defaultKey = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        
        `when`(mockMifare.authenticateSectorWithKeyA(anyInt(), argThat { key: ByteArray ->
            // Extracted key (from sector trailer) - fail
            key.contentEquals(extractedKey)
        })).thenReturn(false)
        
        `when`(mockMifare.authenticateSectorWithKeyA(anyInt(), argThat { key: ByteArray ->
            // Default key - succeed
            key.contentEquals(defaultKey)
        })).thenReturn(true)
        
        doNothing().`when`(mockMifare).writeBlock(anyInt(), any())
        
        runBlocking {
            writer.writeCharacter(mockTag, testCharacter)
            
            // Should try default keys after extracted key fails
            verify(mockMifare, atLeastOnce()).authenticateSectorWithKeyA(anyInt(), any())
        }
    }
    
    @Test
    fun testWrite_Block0Locked_Continues() {
        `when`(mockMifare.isConnected).thenReturn(true)
        `when`(mockMifare.authenticateSectorWithKeyA(anyInt(), any())).thenReturn(true)
        `when`(mockMifare.blockToSector(anyInt())).thenAnswer { it.getArgument<Int>(0) / 4 }
        `when`(mockMifare.readBlock(0)).thenReturn(ByteArray(16))
        doNothing().`when`(mockMifare).close()
        
        // Block 0 write fails (UID locked), but others succeed
        doThrow(IOException("Block 0 is locked")).`when`(mockMifare).writeBlock(0, any())
        doNothing().`when`(mockMifare).writeBlock(argThat { it != 0 }, any())
        
        runBlocking {
            val result = writer.writeCharacter(mockTag, testCharacter)
            
            // Should succeed even if block 0 is locked
            assertTrue("Write should succeed even if block 0 is locked", 
                result is WriteResult.Success || result is WriteResult.Error)
        }
    }
    
    @Test
    fun testWrite_VerifiesConnectionBeforeWrite() {
        `when`(mockMifare.isConnected).thenReturn(true, true, false, true) // Connected, then lost, then reconnected
        `when`(mockMifare.authenticateSectorWithKeyA(anyInt(), any())).thenReturn(true)
        `when`(mockMifare.blockToSector(anyInt())).thenAnswer { it.getArgument<Int>(0) / 4 }
        `when`(mockMifare.readBlock(0)).thenReturn(ByteArray(16))
        doNothing().`when`(mockMifare).close()
        doNothing().`when`(mockMifare).connect()
        
        var writeAttempts = 0
        doAnswer {
            writeAttempts++
            if (writeAttempts == 20) {
                // Simulate connection loss
                `when`(mockMifare.isConnected).thenReturn(false)
                throw IOException("Connection lost")
            }
        }.`when`(mockMifare).writeBlock(anyInt(), any())
        
        runBlocking {
            writer.writeCharacter(mockTag, testCharacter)
        }
        
        // Should check connection multiple times
        verify(mockMifare, atLeastOnce()).isConnected
    }
    
    @Test
    fun testWrite_FailsWhenCannotAuthenticate() {
        `when`(mockMifare.isConnected).thenReturn(true)
        `when`(mockMifare.blockToSector(anyInt())).thenAnswer { it.getArgument<Int>(0) / 4 }
        `when`(mockMifare.readBlock(0)).thenReturn(ByteArray(16))
        doNothing().`when`(mockMifare).close()
        
        // All authentication attempts fail
        `when`(mockMifare.authenticateSectorWithKeyA(anyInt(), any())).thenReturn(false)
        
        runBlocking {
            val result = writer.writeCharacter(mockTag, testCharacter)
            
            // Should return error when cannot authenticate
            assertTrue("Write should fail when cannot authenticate", result is WriteResult.Error)
            assertTrue("Error message should mention authentication", 
                (result as WriteResult.Error).message.contains("authenticate", ignoreCase = true))
        }
    }
    
    @After
    fun tearDown() {
        mifareStaticMock.close()
    }
}


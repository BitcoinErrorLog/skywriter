package com.bitcoinerrorlog.skywriter.nfc

import org.junit.Test
import org.junit.Assert.*

class MifareClassicWriterTest {
    
    @Test
    fun `test WriteResult sealed class`() {
        val success = WriteResult.Success
        val error = WriteResult.Error("Test error")
        val nfcNotAvailable = WriteResult.NFCNotAvailable
        val tagNotSupported = WriteResult.TagNotSupported
        val authFailed = WriteResult.AuthenticationFailed
        val writeFailed = WriteResult.WriteFailed
        
        assertTrue(success is WriteResult)
        assertTrue(error is WriteResult)
        assertTrue(nfcNotAvailable is WriteResult)
        assertTrue(tagNotSupported is WriteResult)
        assertTrue(authFailed is WriteResult)
        assertTrue(writeFailed is WriteResult)
        
        assertEquals("Test error", (error as WriteResult.Error).message)
    }
    
    @Test
    fun `test WriteResult error message`() {
        val errorMessage = "Authentication failed"
        val error = WriteResult.Error(errorMessage)
        
        assertEquals(errorMessage, error.message)
    }
}


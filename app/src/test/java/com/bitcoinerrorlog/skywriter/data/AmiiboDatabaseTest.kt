package com.bitcoinerrorlog.skywriter.data

import org.junit.Assert.*
import org.junit.Test

class AmiiboDatabaseTest {
    
    @Test
    fun `test AmiiboMetadata displayName removes bin extension`() {
        val metadata = AmiiboMetadata(
            originalFilename = "mario.bin",
            originalPath = "path/to/mario.bin",
            characterName = "Mario"
        )
        
        assertEquals("Mario", metadata.displayName)
    }
    
    @Test
    fun `test AmiiboMetadata gameSeriesDisplay returns gameSeries`() {
        val metadata = AmiiboMetadata(
            originalFilename = "mario.bin",
            originalPath = "path/to/mario.bin",
            characterName = "Mario",
            gameSeries = "Super Smash Bros."
        )
        
        assertEquals("Super Smash Bros.", metadata.gameSeriesDisplay)
    }
    
    @Test
    fun `test AmiiboMetadata gameSeriesDisplay returns Unknown when gameSeries is null`() {
        val metadata = AmiiboMetadata(
            originalFilename = "mario.bin",
            originalPath = "path/to/mario.bin",
            characterName = "Mario",
            gameSeries = null
        )
        
        assertEquals("Unknown", metadata.gameSeriesDisplay)
    }
    
    @Test
    fun `test AmiiboModel creation`() {
        val metadata = AmiiboMetadata(
            originalFilename = "mario.bin",
            originalPath = "path/to/mario.bin",
            characterName = "Mario",
            gameSeries = "Super Smash Bros."
        )
        
        val pages = List(135) { "00000000" } // 135 pages, each 4 bytes (8 hex chars)
        val amiibo = AmiiboModel(
            uid = "04000000000000",
            pages = pages,
            metadata = metadata
        )
        
        assertEquals("04000000000000", amiibo.uid)
        assertEquals(135, amiibo.pages.size)
        assertEquals("Mario", amiibo.metadata.displayName)
        assertEquals("Super Smash Bros.", amiibo.metadata.gameSeriesDisplay)
    }
}


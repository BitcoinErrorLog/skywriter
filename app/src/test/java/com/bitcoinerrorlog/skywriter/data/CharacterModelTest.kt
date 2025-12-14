package com.bitcoinerrorlog.skywriter.data

import org.junit.Assert.*
import org.junit.Test

class CharacterModelTest {
    
    @Test
    fun `test CharacterMetadata displayName removes nfc extension`() {
        val metadata = CharacterMetadata(
            originalFilename = "Spyro.nfc",
            originalPath = "path/to/Spyro.nfc",
            category = "Game 1",
            subcategory = "Figures"
        )
        
        assertEquals("Spyro", metadata.displayName)
    }
    
    @Test
    fun `test CharacterMetadata gameSeries returns category`() {
        val metadata = CharacterMetadata(
            originalFilename = "Spyro.nfc",
            originalPath = "path/to/Spyro.nfc",
            category = "Skylanders 1 Spyro's Adventure",
            subcategory = "Figures"
        )
        
        assertEquals("Skylanders 1 Spyro's Adventure", metadata.gameSeries)
    }
    
    @Test
    fun `test CharacterMetadata gameSeries returns Unknown when category is null`() {
        val metadata = CharacterMetadata(
            originalFilename = "Spyro.nfc",
            originalPath = "path/to/Spyro.nfc",
            category = null,
            subcategory = "Figures"
        )
        
        assertEquals("Unknown", metadata.gameSeries)
    }
    
    @Test
    fun `test CharacterModel creation`() {
        val metadata = CharacterMetadata(
            originalFilename = "Spyro.nfc",
            originalPath = "path/to/Spyro.nfc",
            category = "Game 1",
            subcategory = null
        )
        
        val character = CharacterModel(
            uid = "21B589A3",
            atqa = "0004",
            sak = "08",
            mifareType = "1K",
            blocks = listOf("21B589A3BE81010FC433000000000012"),
            metadata = metadata
        )
        
        assertEquals("21B589A3", character.uid)
        assertEquals("0004", character.atqa)
        assertEquals("08", character.sak)
        assertEquals("1K", character.mifareType)
        assertEquals(1, character.blocks.size)
        assertEquals("Spyro", character.metadata.displayName)
    }
}


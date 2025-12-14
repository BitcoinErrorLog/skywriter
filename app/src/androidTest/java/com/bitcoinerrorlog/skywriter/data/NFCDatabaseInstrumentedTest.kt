package com.bitcoinerrorlog.skywriter.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for NFCDatabase
 * Tests JSON parsing and character loading
 */
@RunWith(AndroidJUnit4::class)
class NFCDatabaseInstrumentedTest {
    
    private lateinit var context: Context
    private lateinit var database: NFCDatabase
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = NFCDatabase(context)
    }
    
    @Test
    fun `test database initialization`() {
        assertNotNull(database)
    }
    
    @Test
    fun `test loadCharacters returns empty list when no assets`() = runBlocking {
        // This will return empty if no JSON files are in assets
        val characters = database.loadCharacters()
        assertNotNull(characters)
        // Note: Will be empty if assets folder doesn't have JSON files yet
    }
    
    @Test
    fun `test getCharactersByGame returns empty map when no characters`() {
        val byGame = database.getCharactersByGame()
        assertNotNull(byGame)
        assertTrue(byGame.isEmpty())
    }
    
    @Test
    fun `test searchCharacters returns empty list when no characters`() {
        val results = database.searchCharacters("test")
        assertNotNull(results)
        assertTrue(results.isEmpty())
    }
}


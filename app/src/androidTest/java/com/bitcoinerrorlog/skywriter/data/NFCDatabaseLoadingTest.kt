package com.bitcoinerrorlog.skywriter.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log

/**
 * Comprehensive test for NFCDatabase loading functionality
 * Tests that characters can be loaded from assets and accessed properly
 */
@RunWith(AndroidJUnit4::class)
class NFCDatabaseLoadingTest {
    
    private lateinit var context: Context
    private lateinit var database: NFCDatabase
    
    companion object {
        private const val TAG = "NFCDatabaseLoadingTest"
    }
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = NFCDatabase(context)
    }
    
    @Test
    fun testDatabaseInitialization() {
        assertNotNull("Database should be initialized", database)
    }
    
    @Test
    fun testLoadCharactersReturnsList() = runBlocking {
        val characters = database.loadCharacters()
        assertNotNull("Characters list should not be null", characters)
        Log.d(TAG, "Loaded ${characters.size} characters")
    }
    
    @Test
    fun testGetCharactersByGameReturnsMap() = runBlocking {
        database.loadCharacters()
        val byGame = database.getCharactersByGame()
        assertNotNull("Characters by game should not be null", byGame)
        Log.d(TAG, "Found ${byGame.size} games")
        
        // Log all games and character counts
        byGame.forEach { (game, chars) ->
            Log.d(TAG, "Game: $game - ${chars.size} characters")
            assertTrue("Game name should not be empty", game.isNotBlank())
            assertTrue("Game should have characters", chars.isNotEmpty())
        }
    }
    
    @Test
    fun testAllCharactersHaveRequiredFields() = runBlocking {
        val characters = database.loadCharacters()
        
        characters.forEach { character ->
            assertTrue("Character should have UID", character.uid.isNotBlank())
            assertTrue("Character should have display name", 
                character.metadata.displayName.isNotBlank())
            assertTrue("Character should have game series", 
                character.metadata.gameSeries.isNotBlank())
            assertTrue("Character should have blocks", character.blocks.isNotEmpty())
            assertEquals("Character should have 64 blocks", 64, character.blocks.size)
            
            // Verify block format (32 hex chars = 16 bytes)
            character.blocks.forEach { block ->
                assertTrue("Block should be 32 hex characters", block.length == 32)
                assertTrue("Block should be valid hex", 
                    block.matches(Regex("[0-9A-Fa-f]{32}")))
            }
        }
        
        Log.d(TAG, "Verified ${characters.size} characters have all required fields")
    }
    
    @Test
    fun testCharacterNamesAreVisible() = runBlocking {
        val characters = database.loadCharacters()
        
        assertTrue("Should have characters to test", characters.isNotEmpty())
        
        characters.forEach { character ->
            val displayName = character.metadata.displayName
            assertTrue("Display name should not be empty", displayName.isNotBlank())
            assertTrue("Display name should not be just whitespace", 
                displayName.trim().isNotBlank())
            Log.d(TAG, "Character: $displayName (${character.metadata.gameSeries})")
        }
    }
    
    @Test
    fun testGameNamesAreVisible() = runBlocking {
        database.loadCharacters()
        val byGame = database.getCharactersByGame()
        
        byGame.keys.forEach { gameName ->
            assertTrue("Game name should not be empty", gameName.isNotBlank())
            assertTrue("Game name should not be 'Unknown' if characters exist", 
                gameName != "Unknown" || byGame[gameName]?.isEmpty() == true)
            Log.d(TAG, "Game: $gameName")
        }
    }
    
    @Test
    fun testSearchFunctionality() = runBlocking {
        database.loadCharacters()
        val allCharacters = database.loadCharacters()
        
        if (allCharacters.isNotEmpty()) {
            // Test search with first character's name
            val firstChar = allCharacters.first()
            val searchResults = database.searchCharacters(firstChar.metadata.displayName)
            assertTrue("Search should find matching character", 
                searchResults.any { it.uid == firstChar.uid })
            
            // Test search with game name
            val gameSearch = database.searchCharacters(firstChar.metadata.gameSeries)
            assertTrue("Search should find characters from game", gameSearch.isNotEmpty())
            
            Log.d(TAG, "Search test passed - found ${searchResults.size} results")
        }
    }
    
    @Test
    fun testSubcategoriesAreAccessible() = runBlocking {
        val characters = database.loadCharacters()
        
        val withSubcategories = characters.filter { 
            it.metadata.subcategory != null && it.metadata.subcategory!!.isNotBlank() 
        }
        
        Log.d(TAG, "${withSubcategories.size} characters have subcategories")
        
        withSubcategories.forEach { character ->
            assertNotNull("Subcategory should not be null", character.metadata.subcategory)
            assertTrue("Subcategory should not be empty", 
                character.metadata.subcategory!!.isNotBlank())
            Log.d(TAG, "${character.metadata.displayName}: ${character.metadata.subcategory}")
        }
    }
    
    @Test
    fun testGetCharacterByUid() = runBlocking {
        val characters = database.loadCharacters()
        
        if (characters.isNotEmpty()) {
            val testChar = characters.first()
            val found = database.getCharacterByUid(testChar.uid)
            assertNotNull("Should find character by UID", found)
            assertEquals("Found character should match", testChar.uid, found?.uid)
        }
    }
    
    @Test
    fun testTotalCountAndGameCount() = runBlocking {
        database.loadCharacters()
        val totalCount = database.getTotalCount()
        val gameCount = database.getGameCount()
        
        assertTrue("Total count should be >= 0", totalCount >= 0)
        assertTrue("Game count should be >= 0", gameCount >= 0)
        
        Log.d(TAG, "Total characters: $totalCount, Games: $gameCount")
    }
}


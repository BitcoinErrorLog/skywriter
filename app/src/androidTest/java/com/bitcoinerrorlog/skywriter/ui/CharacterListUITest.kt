package com.bitcoinerrorlog.skywriter.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitcoinerrorlog.skywriter.data.CharacterRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-level tests to verify characters can be accessed through the repository
 * and ViewModel would work correctly
 */
@RunWith(AndroidJUnit4::class)
class CharacterListUITest {
    
    private lateinit var repository: CharacterRepository
    
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = CharacterRepository(context)
    }
    
    @Test
    fun testRepositoryLoadsCharacters() = runBlocking {
        val characters = repository.getAllCharacters()
        assertNotNull("Characters should not be null", characters)
        // Note: Will be empty if assets folder is empty, but should not crash
    }
    
    @Test
    fun testRepositoryOrganizesByGame() = runBlocking {
        val byGame = repository.getCharactersByGame()
        assertNotNull("Characters by game should not be null", byGame)
        
        // If characters exist, verify structure
        if (byGame.isNotEmpty()) {
            byGame.forEach { (game, chars) ->
                assertTrue("Game name should not be empty", game.isNotBlank())
                assertTrue("Game should have characters", chars.isNotEmpty())
            }
        }
    }
    
    @Test
    fun testRepositorySearchWorks() = runBlocking {
        val allCharacters = repository.getAllCharacters()
        
        if (allCharacters.isNotEmpty()) {
            val firstChar = allCharacters.first()
            val results = repository.searchCharacters(firstChar.metadata.displayName)
            assertTrue("Search should return results", results.isNotEmpty())
        } else {
            // Empty search should return empty list
            val results = repository.searchCharacters("test")
            assertTrue("Search with no data should return empty", results.isEmpty())
        }
    }
    
    @Test
    fun testAllCharacterNamesAreAccessible() = runBlocking {
        val characters = repository.getAllCharacters()
        
        characters.forEach { character ->
            assertTrue("Character should have display name", 
                character.metadata.displayName.isNotBlank())
            assertTrue("Character should have game series", 
                character.metadata.gameSeries.isNotBlank())
        }
    }
}


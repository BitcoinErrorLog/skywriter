package com.bitcoinerrorlog.skywriter.data

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.InputStream

class NFCDatabase(private val context: Context) {
    
    private val charactersCache = mutableListOf<CharacterModel>()
    private var isLoaded = false
    
    companion object {
        private const val TAG = "NFCDatabase"
        private const val ASSETS_PATH = "Android_NFC_Data"
    }
    
    suspend fun loadCharacters(): List<CharacterModel> {
        if (isLoaded) {
            Log.d(TAG, "Returning cached characters: ${charactersCache.size}")
            return charactersCache
        }
        
        val characters = mutableListOf<CharacterModel>()
        
        try {
            Log.d(TAG, "Loading characters from assets: $ASSETS_PATH")
            val files = context.assets.list(ASSETS_PATH)
            
            if (files == null || files.isEmpty()) {
                Log.w(TAG, "No files found in assets/$ASSETS_PATH - assets folder may be empty")
                // Return empty list but don't mark as loaded so it can retry
                return emptyList()
            }
            
            Log.d(TAG, "Found ${files.size} items in assets directory")
            
            files.forEach { file ->
                if (file.endsWith(".json")) {
                    try {
                        val inputStream = context.assets.open("$ASSETS_PATH/$file")
                        val character = parseCharacterJSON(inputStream, file, ASSETS_PATH)
                        if (character != null) {
                            characters.add(character)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading file $file", e)
                    }
                } else {
                    // Handle subdirectories recursively
                    loadCharactersFromDirectory("$ASSETS_PATH/$file", characters)
                }
            }
            
            charactersCache.clear()
            charactersCache.addAll(characters)
            isLoaded = true
            
            Log.d(TAG, "Successfully loaded ${characters.size} characters")
            Log.d(TAG, "Games found: ${getCharactersByGame().keys}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading characters", e)
            isLoaded = false
        }
        
        return charactersCache
    }
    
    private fun loadCharactersFromDirectory(directory: String, characters: MutableList<CharacterModel>) {
        try {
            val files = context.assets.list(directory) ?: emptyArray()
            Log.d(TAG, "Loading from directory: $directory (${files.size} items)")
            
            files.forEach { file ->
                if (file.endsWith(".json")) {
                    try {
                        val inputStream = context.assets.open("$directory/$file")
                        val character = parseCharacterJSON(inputStream, file, directory)
                        if (character != null) {
                            characters.add(character)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading file $directory/$file", e)
                    }
                } else {
                    // Recursively load from subdirectories
                    loadCharactersFromDirectory("$directory/$file", characters)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading directory: $directory", e)
        }
    }
    
    private fun parseCharacterJSON(
        inputStream: InputStream,
        filename: String,
        @Suppress("UNUSED_PARAMETER") directory: String
    ): CharacterModel? {
        return try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            val metadataJson = json.getJSONObject("metadata")
            val metadata = CharacterMetadata(
                originalFilename = metadataJson.getString("original_filename"),
                originalPath = metadataJson.getString("original_path"),
                category = metadataJson.optString("category").takeIf { it.isNotEmpty() },
                subcategory = metadataJson.optString("subcategory").takeIf { it.isNotEmpty() }
            )
            
            CharacterModel(
                uid = json.getString("uid"),
                atqa = json.getString("atqa"),
                sak = json.getString("sak"),
                mifareType = json.getString("mifare_type"),
                blocks = parseBlocks(json.getJSONArray("blocks")),
                metadata = metadata
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON file: $filename", e)
            null
        }
    }
    
    private fun parseBlocks(blocksArray: org.json.JSONArray): List<String> {
        val blocks = mutableListOf<String>()
        for (i in 0 until blocksArray.length()) {
            blocks.add(blocksArray.getString(i))
        }
        return blocks
    }
    
    fun getCharactersByGame(): Map<String, List<CharacterModel>> {
        val grouped = charactersCache.groupBy { it.metadata.gameSeries }
        Log.d(TAG, "Characters by game: ${grouped.map { "${it.key}: ${it.value.size}" }}")
        return grouped
    }
    
    fun searchCharacters(query: String): List<CharacterModel> {
        if (query.isBlank()) {
            return charactersCache
        }
        
        val lowerQuery = query.lowercase().trim()
        val results = charactersCache.filter { character ->
            val name = character.metadata.displayName.lowercase()
            val game = character.metadata.gameSeries.lowercase()
            val subcategory = character.metadata.subcategory?.lowercase() ?: ""
            
            // Check if query matches any part of name, game, or subcategory
            name.contains(lowerQuery) ||
            game.contains(lowerQuery) ||
            subcategory.contains(lowerQuery) ||
            // Also check if any word in the name starts with the query (for faster typing)
            name.split(" ").any { it.startsWith(lowerQuery) }
        }
        
        // Sort results: exact matches first, then by relevance
        val sortedResults = results.sortedWith(compareBy<CharacterModel> { character ->
            val name = character.metadata.displayName.lowercase()
            when {
                name.startsWith(lowerQuery) -> 0  // Exact start match
                name.contains(lowerQuery) -> 1    // Contains match
                else -> 2                         // Other matches
            }
        }.thenBy { it.metadata.displayName })
        
        Log.d(TAG, "Search '$query' returned ${sortedResults.size} results")
        return sortedResults
    }
    
    fun getCharacterByUid(uid: String): CharacterModel? {
        return charactersCache.find { it.uid.equals(uid, ignoreCase = true) }
    }
    
    fun getTotalCount(): Int = charactersCache.size
    
    fun getGameCount(): Int = getCharactersByGame().size
}

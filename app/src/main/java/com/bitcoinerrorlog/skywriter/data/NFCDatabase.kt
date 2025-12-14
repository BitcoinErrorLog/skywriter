package com.bitcoinerrorlog.skywriter.data

import android.content.Context
import org.json.JSONObject
import java.io.InputStream

class NFCDatabase(private val context: Context) {
    
    private val charactersCache = mutableListOf<CharacterModel>()
    private var isLoaded = false
    
    suspend fun loadCharacters(): List<CharacterModel> {
        if (isLoaded) {
            return charactersCache
        }
        
        val characters = mutableListOf<CharacterModel>()
        
        try {
            val assetsPath = "Android_NFC_Data"
            val files = context.assets.list(assetsPath) ?: emptyArray()
            
            files.forEach { file ->
                if (file.endsWith(".json")) {
                    try {
                        val inputStream = context.assets.open("$assetsPath/$file")
                        val character = parseCharacterJSON(inputStream, file, assetsPath)
                        if (character != null) {
                            characters.add(character)
                        }
                    } catch (e: Exception) {
                        // Skip files that can't be parsed
                        e.printStackTrace()
                    }
                } else {
                    // Handle subdirectories recursively
                    loadCharactersFromDirectory("$assetsPath/$file", characters)
                }
            }
            
            charactersCache.clear()
            charactersCache.addAll(characters)
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return charactersCache
    }
    
    private fun loadCharactersFromDirectory(directory: String, characters: MutableList<CharacterModel>) {
        try {
            val files = context.assets.list(directory) ?: emptyArray()
            files.forEach { file ->
                if (file.endsWith(".json")) {
                    try {
                        val inputStream = context.assets.open("$directory/$file")
                        val character = parseCharacterJSON(inputStream, file, directory)
                        if (character != null) {
                            characters.add(character)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // Recursively load from subdirectories
                    loadCharactersFromDirectory("$directory/$file", characters)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun parseCharacterJSON(
        inputStream: InputStream,
        filename: String,
        directory: String
    ): CharacterModel? {
        return try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            val metadataJson = json.getJSONObject("metadata")
            val metadata = CharacterMetadata(
                originalFilename = metadataJson.getString("original_filename"),
                originalPath = metadataJson.getString("original_path"),
                category = metadataJson.optString("category", null),
                subcategory = metadataJson.optString("subcategory", null)
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
            e.printStackTrace()
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
        return charactersCache.groupBy { it.metadata.gameSeries }
    }
    
    fun searchCharacters(query: String): List<CharacterModel> {
        val lowerQuery = query.lowercase()
        return charactersCache.filter {
            it.metadata.displayName.lowercase().contains(lowerQuery) ||
            it.metadata.gameSeries.lowercase().contains(lowerQuery)
        }
    }
    
    fun getCharacterByUid(uid: String): CharacterModel? {
        return charactersCache.find { it.uid.equals(uid, ignoreCase = true) }
    }
}


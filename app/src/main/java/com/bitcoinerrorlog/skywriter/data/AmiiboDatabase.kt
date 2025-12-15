package com.bitcoinerrorlog.skywriter.data

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.InputStream

class AmiiboDatabase(private val context: Context) {
    
    private val amiibosCache = mutableListOf<AmiiboModel>()
    private var isLoaded = false
    
    companion object {
        private const val TAG = "AmiiboDatabase"
        private const val ASSETS_PATH = "Amiibo_NFC_Data"
    }
    
    suspend fun loadAmiibos(): List<AmiiboModel> {
        if (isLoaded) {
            Log.d(TAG, "Returning cached Amiibos: ${amiibosCache.size}")
            return amiibosCache
        }
        
        val amiibos = mutableListOf<AmiiboModel>()
        
        try {
            Log.d(TAG, "Loading Amiibos from assets: $ASSETS_PATH")
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
                        val amiibo = parseAmiiboJSON(inputStream, file, ASSETS_PATH)
                        if (amiibo != null) {
                            amiibos.add(amiibo)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading file $file", e)
                    }
                } else {
                    // Handle subdirectories recursively
                    loadAmiibosFromDirectory("$ASSETS_PATH/$file", amiibos)
                }
            }
            
            amiibosCache.clear()
            amiibosCache.addAll(amiibos)
            isLoaded = true
            
            Log.d(TAG, "Successfully loaded ${amiibos.size} Amiibos")
            Log.d(TAG, "Game series found: ${getAmiibosByGame().keys}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading Amiibos", e)
            isLoaded = false
        }
        
        return amiibosCache
    }
    
    private fun loadAmiibosFromDirectory(directory: String, amiibos: MutableList<AmiiboModel>) {
        try {
            val files = context.assets.list(directory) ?: emptyArray()
            Log.d(TAG, "Loading from directory: $directory (${files.size} items)")
            
            files.forEach { file ->
                if (file.endsWith(".json")) {
                    try {
                        val inputStream = context.assets.open("$directory/$file")
                        val amiibo = parseAmiiboJSON(inputStream, file, directory)
                        if (amiibo != null) {
                            amiibos.add(amiibo)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading file $directory/$file", e)
                    }
                } else {
                    // Recursively load from subdirectories
                    loadAmiibosFromDirectory("$directory/$file", amiibos)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading directory: $directory", e)
        }
    }
    
    private fun parseAmiiboJSON(
        inputStream: InputStream,
        filename: String,
        directory: String
    ): AmiiboModel? {
        return try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            val metadataJson = json.getJSONObject("metadata")
            
            val metadata = AmiiboMetadata(
                originalFilename = metadataJson.getString("original_filename"),
                originalPath = metadataJson.getString("original_path"),
                characterName = metadataJson.getString("character_name"),
                gameSeries = metadataJson.optString("game_series").takeIf { it.isNotEmpty() },
                characterId = metadataJson.optString("character_id").takeIf { it.isNotEmpty() },
                gameId = metadataJson.optString("game_id").takeIf { it.isNotEmpty() },
                biography = metadataJson.optString("biography").takeIf { it.isNotEmpty() },
                releaseDate = metadataJson.optString("release_date").takeIf { it.isNotEmpty() },
                amiiboType = metadataJson.optString("amiibo_type").takeIf { it.isNotEmpty() }
            )
            
            AmiiboModel(
                uid = json.getString("uid"),
                pages = parsePages(json.getJSONArray("pages")),
                metadata = metadata
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON file: $filename", e)
            null
        }
    }
    
    private fun parsePages(pagesArray: org.json.JSONArray): List<String> {
        val pages = mutableListOf<String>()
        for (i in 0 until pagesArray.length()) {
            pages.add(pagesArray.getString(i))
        }
        return pages
    }
    
    fun getAmiibosByGame(): Map<String, List<AmiiboModel>> {
        val grouped = amiibosCache.groupBy { it.metadata.gameSeriesDisplay }
        Log.d(TAG, "Amiibos by game: ${grouped.map { "${it.key}: ${it.value.size}" }}")
        return grouped
    }
    
    fun searchAmiibos(query: String): List<AmiiboModel> {
        if (query.isBlank()) {
            return amiibosCache
        }
        
        val lowerQuery = query.lowercase().trim()
        val exactMatches = mutableListOf<AmiiboModel>()
        val wordStartMatches = mutableListOf<AmiiboModel>()
        val containsMatches = mutableListOf<AmiiboModel>()
        
        amiibosCache.forEach { amiibo ->
            val displayNameLower = amiibo.metadata.displayName.lowercase()
            val gameSeriesLower = amiibo.metadata.gameSeriesDisplay.lowercase()
            val characterNameLower = amiibo.metadata.characterName.lowercase()
            
            val isDisplayNameExact = displayNameLower == lowerQuery
            val isGameSeriesExact = gameSeriesLower == lowerQuery
            val isCharacterNameExact = characterNameLower == lowerQuery
            
            val isDisplayNameWordStart = displayNameLower.startsWith(lowerQuery) ||
                                         displayNameLower.split(" ").any { it.startsWith(lowerQuery) }
            val isGameSeriesWordStart = gameSeriesLower.startsWith(lowerQuery) ||
                                        gameSeriesLower.split(" ").any { it.startsWith(lowerQuery) }
            val isCharacterNameWordStart = characterNameLower.startsWith(lowerQuery) ||
                                           characterNameLower.split(" ").any { it.startsWith(lowerQuery) }
            
            val isDisplayNameContains = displayNameLower.contains(lowerQuery)
            val isGameSeriesContains = gameSeriesLower.contains(lowerQuery)
            val isCharacterNameContains = characterNameLower.contains(lowerQuery)
            
            when {
                isDisplayNameExact || isGameSeriesExact || isCharacterNameExact -> exactMatches.add(amiibo)
                isDisplayNameWordStart || isGameSeriesWordStart || isCharacterNameWordStart -> wordStartMatches.add(amiibo)
                isDisplayNameContains || isGameSeriesContains || isCharacterNameContains -> containsMatches.add(amiibo)
            }
        }
        
        val results = (exactMatches + wordStartMatches + containsMatches).distinctBy { it.uid }
        Log.d(TAG, "Search '$query' returned ${results.size} results")
        return results
    }
    
    fun getAmiiboByUid(uid: String): AmiiboModel? {
        return amiibosCache.find { it.uid.equals(uid, ignoreCase = true) }
    }
    
    fun getTotalCount(): Int = amiibosCache.size
    
    fun getGameCount(): Int = getAmiibosByGame().size
}


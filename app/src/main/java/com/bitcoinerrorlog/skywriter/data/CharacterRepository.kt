package com.bitcoinerrorlog.skywriter.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CharacterRepository(private val context: Context) {
    
    private val database = NFCDatabase(context)
    
    suspend fun getAllCharacters(): List<CharacterModel> = withContext(Dispatchers.IO) {
        database.loadCharacters()
    }
    
    suspend fun getCharactersByGame(): Map<String, List<CharacterModel>> = withContext(Dispatchers.IO) {
        database.loadCharacters()
        database.getCharactersByGame()
    }
    
    suspend fun searchCharacters(query: String): List<CharacterModel> = withContext(Dispatchers.IO) {
        database.loadCharacters()
        database.searchCharacters(query)
    }
    
    suspend fun getCharacterByUid(uid: String): CharacterModel? = withContext(Dispatchers.IO) {
        database.loadCharacters()
        database.getCharacterByUid(uid)
    }
}


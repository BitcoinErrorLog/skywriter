package com.bitcoinerrorlog.skywriter.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AmiiboRepository(private val context: Context) {
    
    private val database = AmiiboDatabase(context)
    
    suspend fun getAllAmiibos(): List<AmiiboModel> = withContext(Dispatchers.IO) {
        database.loadAmiibos()
    }
    
    suspend fun getAmiibosByGame(): Map<String, List<AmiiboModel>> = withContext(Dispatchers.IO) {
        database.loadAmiibos()
        database.getAmiibosByGame()
    }
    
    suspend fun searchAmiibos(query: String): List<AmiiboModel> = withContext(Dispatchers.IO) {
        database.loadAmiibos()
        database.searchAmiibos(query)
    }
    
    suspend fun getAmiiboByUid(uid: String): AmiiboModel? = withContext(Dispatchers.IO) {
        database.loadAmiibos()
        database.getAmiiboByUid(uid)
    }
}


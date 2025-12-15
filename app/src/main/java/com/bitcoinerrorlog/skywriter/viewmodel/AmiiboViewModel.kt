package com.bitcoinerrorlog.skywriter.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bitcoinerrorlog.skywriter.data.AmiiboModel
import com.bitcoinerrorlog.skywriter.data.AmiiboRepository
import kotlinx.coroutines.launch

class AmiiboViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = AmiiboRepository(application)
    
    private val _amiibos = MutableLiveData<List<AmiiboModel>>()
    val amiibos: LiveData<List<AmiiboModel>> = _amiibos
    
    private val _amiibosByGame = MutableLiveData<Map<String, List<AmiiboModel>>>()
    val amiibosByGame: LiveData<Map<String, List<AmiiboModel>>> = _amiibosByGame
    
    private val _selectedAmiibo = MutableLiveData<AmiiboModel?>()
    val selectedAmiibo: LiveData<AmiiboModel?> = _selectedAmiibo
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    companion object {
        private const val TAG = "AmiiboViewModel"
    }
    
    init {
        Log.d(TAG, "ViewModel initialized, loading Amiibos...")
        loadAmiibos()
    }
    
    fun loadAmiibos() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                Log.d(TAG, "Loading all Amiibos...")
                val allAmiibos = repository.getAllAmiibos()
                Log.d(TAG, "Loaded ${allAmiibos.size} Amiibos")
                
                _amiibos.value = allAmiibos
                
                val byGame = repository.getAmiibosByGame()
                Log.d(TAG, "Organized into ${byGame.size} game series")
                _amiibosByGame.value = byGame
                
                if (allAmiibos.isEmpty()) {
                    _errorMessage.value = "No Amiibos found. Please add JSON files to assets/Amiibo_NFC_Data/"
                    Log.w(TAG, "No Amiibos loaded - assets folder may be empty")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Amiibos", e)
                _errorMessage.value = "Error loading Amiibos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchAmiibos(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                loadAmiibos()
            } else {
                _isLoading.value = true
                try {
                    val results = repository.searchAmiibos(query)
                    _amiibos.value = results
                    Log.d(TAG, "Search '$query' returned ${results.size} results")
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching Amiibos", e)
                    _errorMessage.value = "Error searching: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun selectAmiibo(amiibo: AmiiboModel) {
        _selectedAmiibo.value = amiibo
    }
    
    fun clearSelection() {
        _selectedAmiibo.value = null
    }
}


package com.bitcoinerrorlog.skywriter.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import com.bitcoinerrorlog.skywriter.data.CharacterRepository
import kotlinx.coroutines.launch

class CharacterViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = CharacterRepository(application)
    
    private val _characters = MutableLiveData<List<CharacterModel>>()
    val characters: LiveData<List<CharacterModel>> = _characters
    
    private val _charactersByGame = MutableLiveData<Map<String, List<CharacterModel>>>()
    val charactersByGame: LiveData<Map<String, List<CharacterModel>>> = _charactersByGame
    
    private val _selectedCharacter = MutableLiveData<CharacterModel?>()
    val selectedCharacter: LiveData<CharacterModel?> = _selectedCharacter
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    companion object {
        private const val TAG = "CharacterViewModel"
    }
    
    init {
        Log.d(TAG, "ViewModel initialized, loading characters...")
        loadCharacters()
    }
    
    fun loadCharacters() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                Log.d(TAG, "Loading all characters...")
                val allCharacters = repository.getAllCharacters()
                Log.d(TAG, "Loaded ${allCharacters.size} characters")
                
                _characters.value = allCharacters
                
                val byGame = repository.getCharactersByGame()
                Log.d(TAG, "Organized into ${byGame.size} games")
                _charactersByGame.value = byGame
                
                if (allCharacters.isEmpty()) {
                    _errorMessage.value = "No characters found. Please add JSON files to assets/Android_NFC_Data/"
                    Log.w(TAG, "No characters loaded - assets folder may be empty")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading characters", e)
                _errorMessage.value = "Error loading characters: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchCharacters(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                loadCharacters()
            } else {
                _isLoading.value = true
                try {
                    val results = repository.searchCharacters(query)
                    _characters.value = results
                    Log.d(TAG, "Search '$query' returned ${results.size} results")
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching characters", e)
                    _errorMessage.value = "Error searching: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun selectCharacter(character: CharacterModel) {
        _selectedCharacter.value = character
    }
    
    fun clearSelection() {
        _selectedCharacter.value = null
    }
}

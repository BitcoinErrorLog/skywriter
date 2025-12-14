package com.bitcoinerrorlog.skywriter.viewmodel

import android.app.Application
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
    
    init {
        loadCharacters()
    }
    
    fun loadCharacters() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allCharacters = repository.getAllCharacters()
                _characters.value = allCharacters
                _charactersByGame.value = repository.getCharactersByGame()
            } catch (e: Exception) {
                e.printStackTrace()
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
                } catch (e: Exception) {
                    e.printStackTrace()
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


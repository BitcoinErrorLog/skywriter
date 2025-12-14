package com.bitcoinerrorlog.skywriter.ui.list

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bitcoinerrorlog.skywriter.R
import com.bitcoinerrorlog.skywriter.databinding.FragmentCharacterListBinding
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import com.bitcoinerrorlog.skywriter.ui.detail.CharacterDetailDialog
import com.bitcoinerrorlog.skywriter.viewmodel.CharacterViewModel

class CharacterListFragment : Fragment() {
    
    private var _binding: FragmentCharacterListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CharacterViewModel by activityViewModels()
    private lateinit var adapter: CharacterAdapter
    private var isSearchVisible = false
    
    companion object {
        private const val TAG = "CharacterListFragment"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCharacterListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(TAG, "Fragment view created")
        
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupSearchFab()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    toggleSearch()
                    true
                }
                R.id.action_check_tag -> {
                    findNavController().navigate(R.id.tagCheckFragment)
                    true
                }
                else -> false
            }
        }
        
        // Show header logo if available (will be visible by default if PNG exists)
        // The logo is in the layout above the toolbar, so it will show automatically
        // if the drawable resource exists
    }
    
    private fun setupRecyclerView() {
        adapter = CharacterAdapter { character ->
            showCharacterDetail(character)
        }
        
        adapter.setOnHeaderToggleListener { position ->
            adapter.toggleHeader(position)
        }
        
        val layoutManager = GridLayoutManager(context, 2)
        binding.charactersRecyclerView.layoutManager = layoutManager
        binding.charactersRecyclerView.adapter = adapter
    }
    
    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                if (query.isBlank()) {
                    // Show all characters organized by game
                    viewModel.loadCharacters()
                } else {
                    // Show search results
                    viewModel.searchCharacters(query)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun setupSearchFab() {
        binding.searchFab.setOnClickListener {
            toggleSearch()
        }
    }
    
    private fun toggleSearch() {
        isSearchVisible = !isSearchVisible
        if (isSearchVisible) {
            binding.searchCard.visibility = View.VISIBLE
            binding.searchEditText.requestFocus()
            binding.searchFab.hide()
        } else {
            binding.searchCard.visibility = View.GONE
            binding.searchEditText.text?.clear()
            binding.searchFab.show()
            // Clear search and show all characters
            viewModel.loadCharacters()
        }
    }
    
    private fun observeViewModel() {
        // Observe characters by game for organized display
        viewModel.charactersByGame.observe(viewLifecycleOwner) { charactersByGame ->
            Log.d(TAG, "charactersByGame updated: ${charactersByGame.size} games")
            if (charactersByGame.isNotEmpty()) {
                adapter.setCharactersByGame(charactersByGame)
                adapter.submitList(charactersByGame)
                binding.emptyStateText.visibility = View.GONE
                binding.charactersRecyclerView.visibility = View.VISIBLE
                
                // Log for debugging
                charactersByGame.forEach { (game, chars) ->
                    Log.d(TAG, "Game: $game - ${chars.size} characters")
                    chars.take(5).forEach { char ->
                        Log.d(TAG, "  - ${char.metadata.displayName} (${char.metadata.subcategory})")
                    }
                }
            } else {
                binding.emptyStateText.visibility = View.VISIBLE
                binding.charactersRecyclerView.visibility = View.GONE
                adapter.submitList(emptyMap())
            }
        }
        
        // Observe search results (flat list)
        viewModel.characters.observe(viewLifecycleOwner) { characters ->
            if (binding.searchCard.visibility == View.VISIBLE && 
                binding.searchEditText.text?.isNotBlank() == true) {
                // In search mode, show flat list
                Log.d(TAG, "Search results: ${characters.size} characters")
                if (characters.isNotEmpty()) {
                    val searchResults = mapOf("Search Results" to characters)
                    adapter.submitList(searchResults)
                    binding.emptyStateText.visibility = View.GONE
                    binding.charactersRecyclerView.visibility = View.VISIBLE
                } else {
                    adapter.submitList(emptyMap())
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.charactersRecyclerView.visibility = View.GONE
                }
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.emptyStateText.visibility = View.GONE
            }
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Log.e(TAG, "Error: $error")
                binding.emptyStateText.text = error
                binding.emptyStateText.visibility = View.VISIBLE
            }
        }
    }
    
    private fun showCharacterDetail(character: CharacterModel) {
        Log.d(TAG, "Showing character: ${character.metadata.displayName}")
        val dialog = CharacterDetailDialog().apply {
            arguments = Bundle().apply {
                putParcelable("character", character)
            }
        }
        dialog.show(
            childFragmentManager,
            CharacterDetailDialog.TAG
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.bitcoinerrorlog.skywriter.ui.list

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
                else -> false
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = CharacterAdapter { character ->
            showCharacterDetail(character)
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
            if (charactersByGame.isNotEmpty()) {
                adapter.submitList(charactersByGame)
                binding.emptyStateText.visibility = View.GONE
            } else {
                binding.emptyStateText.visibility = View.VISIBLE
            }
        }
        
        // Observe search results (flat list)
        viewModel.characters.observe(viewLifecycleOwner) { characters ->
            if (binding.searchCard.visibility == View.VISIBLE && 
                binding.searchEditText.text?.isNotBlank() == true) {
                // In search mode, show flat list
                if (characters.isNotEmpty()) {
                    val searchResults = mapOf("Search Results" to characters)
                    adapter.submitList(searchResults)
                    binding.emptyStateText.visibility = View.GONE
                } else {
                    adapter.submitList(emptyMap())
                    binding.emptyStateText.visibility = View.VISIBLE
                }
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.emptyStateText.visibility = View.GONE
            }
        }
    }
    
    private fun showCharacterDetail(character: CharacterModel) {
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

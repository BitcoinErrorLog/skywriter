package com.bitcoinerrorlog.skywriter.ui.amiibo

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
import com.bitcoinerrorlog.skywriter.databinding.FragmentAmiiboListBinding
import com.bitcoinerrorlog.skywriter.data.AmiiboModel
import com.bitcoinerrorlog.skywriter.ui.amiibo.AmiiboDetailDialog
import com.bitcoinerrorlog.skywriter.viewmodel.AmiiboViewModel

class AmiiboListFragment : Fragment() {
    
    private var _binding: FragmentAmiiboListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AmiiboViewModel by activityViewModels()
    private lateinit var adapter: AmiiboAdapter
    private var isSearchVisible = false
    
    companion object {
        private const val TAG = "AmiiboListFragment"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAmiiboListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(TAG, "Fragment view created")
        
        setupRecyclerView()
        setupSearch()
        setupSearchFab()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        adapter = AmiiboAdapter { amiibo ->
            showAmiiboDetail(amiibo)
        }
        
        adapter.setOnHeaderToggleListener { position ->
            adapter.toggleHeader(position)
        }
        
        val layoutManager = GridLayoutManager(context, 2)
        binding.amiibosRecyclerView.layoutManager = layoutManager
        binding.amiibosRecyclerView.adapter = adapter
    }
    
    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                Log.d(TAG, "Search query changed: '$query'")
                if (query.isBlank()) {
                    // Show all Amiibos organized by game
                    viewModel.loadAmiibos()
                } else {
                    // Show search results immediately
                    viewModel.searchAmiibos(query)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Also handle search button press
        binding.searchEditText.setOnEditorActionListener { _, _, _ ->
            val query = binding.searchEditText.text?.toString()?.trim() ?: ""
            if (query.isNotBlank()) {
                viewModel.searchAmiibos(query)
            }
            true
        }
    }
    
    private fun setupSearchFab() {
        binding.searchFab.setOnClickListener {
            toggleSearch()
        }
    }
    
    fun toggleSearch() {
        isSearchVisible = !isSearchVisible
        if (isSearchVisible) {
            binding.searchCard.visibility = View.VISIBLE
            binding.searchEditText.requestFocus()
            binding.searchFab.hide()
        } else {
            binding.searchCard.visibility = View.GONE
            binding.searchEditText.text?.clear()
            binding.searchFab.show()
            // Clear search and show all Amiibos
            viewModel.loadAmiibos()
        }
    }
    
    private fun observeViewModel() {
        // Observe Amiibos by game for organized display
        viewModel.amiibosByGame.observe(viewLifecycleOwner) { amiibosByGame ->
            Log.d(TAG, "amiibosByGame updated: ${amiibosByGame.size} game series")
            if (amiibosByGame.isNotEmpty()) {
                adapter.setAmiibosByGame(amiibosByGame)
                adapter.submitList(amiibosByGame)
                binding.emptyStateText.visibility = View.GONE
                binding.amiibosRecyclerView.visibility = View.VISIBLE
                
                // Log for debugging
                amiibosByGame.forEach { (game, amiibos) ->
                    Log.d(TAG, "Game: $game - ${amiibos.size} Amiibos")
                    amiibos.take(5).forEach { amiibo ->
                        Log.d(TAG, "  - ${amiibo.metadata.displayName}")
                    }
                }
            } else {
                binding.emptyStateText.visibility = View.VISIBLE
                binding.amiibosRecyclerView.visibility = View.GONE
                adapter.submitList(emptyMap())
            }
        }
        
        // Observe search results (flat list)
        viewModel.amiibos.observe(viewLifecycleOwner) { amiibos ->
            val isSearchMode = binding.searchCard.visibility == View.VISIBLE && 
                              binding.searchEditText.text?.toString()?.trim()?.isNotBlank() == true
            
            if (isSearchMode) {
                // In search mode, show flat list
                Log.d(TAG, "Search results: ${amiibos.size} Amiibos")
                if (amiibos.isNotEmpty()) {
                    val searchResults = mapOf("Search Results (${amiibos.size})" to amiibos)
                    adapter.submitList(searchResults)
                    binding.emptyStateText.visibility = View.GONE
                    binding.amiibosRecyclerView.visibility = View.VISIBLE
                } else {
                    adapter.submitList(emptyMap())
                    binding.emptyStateText.text = "No Amiibo found matching your search"
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.amiibosRecyclerView.visibility = View.GONE
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
    
    private fun showAmiiboDetail(amiibo: AmiiboModel) {
        Log.d(TAG, "Showing Amiibo: ${amiibo.metadata.displayName}")
        val dialog = AmiiboDetailDialog().apply {
            arguments = Bundle().apply {
                putParcelable("amiibo", amiibo)
            }
        }
        dialog.show(
            childFragmentManager,
            AmiiboDetailDialog.TAG
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


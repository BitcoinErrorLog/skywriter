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
        
        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        adapter = CharacterAdapter { character ->
            showCharacterDetail(character)
        }
        
        binding.charactersRecyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.charactersRecyclerView.adapter = adapter
    }
    
    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchCharacters(s?.toString() ?: "")
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun observeViewModel() {
        viewModel.characters.observe(viewLifecycleOwner) { characters ->
            adapter.submitList(characters)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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


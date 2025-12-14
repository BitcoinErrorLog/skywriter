package com.bitcoinerrorlog.skywriter.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bitcoinerrorlog.skywriter.R
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import com.bitcoinerrorlog.skywriter.databinding.DialogCharacterDetailBinding
import com.bitcoinerrorlog.skywriter.viewmodel.CharacterViewModel

class CharacterDetailDialog : DialogFragment() {
    
    private var _binding: DialogCharacterDetailBinding? = null
    private val binding get() = _binding!!
    
    private var character: CharacterModel? = null
    
    companion object {
        const val TAG = "CharacterDetailDialog"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Skywriter)
        arguments?.let {
            character = it.getParcelable("character")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCharacterDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        character?.let { char ->
            binding.characterName.text = char.metadata.displayName
            binding.characterGame.text = char.metadata.gameSeries
            binding.characterUid.text = "UID: ${char.uid}"
            
            binding.writeButton.setOnClickListener {
                // Navigate to write fragment using parent fragment's nav controller
                parentFragment?.findNavController()?.let { navController ->
                    val bundle = Bundle().apply {
                        putParcelable("character", char)
                    }
                    navController.navigate(R.id.writeNFCFragment, bundle)
                }
                dismiss()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


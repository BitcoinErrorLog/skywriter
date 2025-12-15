package com.bitcoinerrorlog.skywriter.ui.amiibo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.bitcoinerrorlog.skywriter.R
import com.bitcoinerrorlog.skywriter.data.AmiiboModel
import com.bitcoinerrorlog.skywriter.databinding.DialogAmiiboDetailBinding

class AmiiboDetailDialog : DialogFragment() {
    
    private var _binding: DialogAmiiboDetailBinding? = null
    private val binding get() = _binding!!
    
    private var amiibo: AmiiboModel? = null
    
    companion object {
        const val TAG = "AmiiboDetailDialog"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Skywriter)
        arguments?.let {
            amiibo = it.getParcelable("amiibo")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAmiiboDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Add close button handler
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        amiibo?.let { amiibo ->
            binding.amiiboName.text = amiibo.metadata.displayName
            binding.amiiboGame.text = amiibo.metadata.gameSeriesDisplay
            binding.amiiboUid.text = "UID: ${amiibo.uid}"
            
            // Display character ID if available
            if (!amiibo.metadata.characterId.isNullOrBlank()) {
                binding.characterId.text = "Character ID: ${amiibo.metadata.characterId}"
                binding.characterId.visibility = View.VISIBLE
            } else {
                binding.characterId.visibility = View.GONE
            }
            
            // Display game ID if available
            if (!amiibo.metadata.gameId.isNullOrBlank()) {
                binding.gameId.text = "Game ID: ${amiibo.metadata.gameId}"
                binding.gameId.visibility = View.VISIBLE
            } else {
                binding.gameId.visibility = View.GONE
            }
            
            // Display Amiibo type if available
            if (!amiibo.metadata.amiiboType.isNullOrBlank()) {
                binding.amiiboType.text = "Type: ${amiibo.metadata.amiiboType}"
                binding.amiiboType.visibility = View.VISIBLE
            } else {
                binding.amiiboType.visibility = View.GONE
            }
            
            // Display release date if available
            if (!amiibo.metadata.releaseDate.isNullOrBlank()) {
                binding.releaseDate.text = "Released: ${amiibo.metadata.releaseDate}"
                binding.releaseDate.visibility = View.VISIBLE
            } else {
                binding.releaseDate.visibility = View.GONE
            }
            
            // Display biography if available
            if (!amiibo.metadata.biography.isNullOrBlank()) {
                binding.amiiboBiography.text = amiibo.metadata.biography
                binding.amiiboBiography.visibility = View.VISIBLE
            } else {
                binding.amiiboBiography.visibility = View.GONE
            }
            
            binding.writeButton.setOnClickListener {
                // Navigate to write fragment using parent fragment's nav controller
                parentFragment?.findNavController()?.let { navController ->
                    val bundle = Bundle().apply {
                        putParcelable("amiibo", amiibo)
                    }
                    navController.navigate(R.id.writeAmiiboFragment, bundle)
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


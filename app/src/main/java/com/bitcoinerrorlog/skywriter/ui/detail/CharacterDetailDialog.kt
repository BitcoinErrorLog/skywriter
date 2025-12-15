package com.bitcoinerrorlog.skywriter.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitcoinerrorlog.skywriter.R
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import com.bitcoinerrorlog.skywriter.databinding.DialogCharacterDetailBinding
import com.bitcoinerrorlog.skywriter.viewmodel.CharacterViewModel
import com.google.android.material.chip.Chip

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
        
        // Add close button handler
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        character?.let { char ->
            binding.characterName.text = char.metadata.displayName
            binding.characterGame.text = char.metadata.gameSeries
            binding.characterUid.text = "UID: ${char.uid}"
            
            // Display element if available
            if (!char.metadata.element.isNullOrBlank()) {
                binding.characterElement.text = char.metadata.element.uppercase()
                binding.characterElement.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.white))
                binding.characterElement.setChipBackgroundColorResource(
                    when (char.metadata.element.lowercase()) {
                        "fire" -> R.color.fire_element
                        "water" -> R.color.water_element
                        "earth" -> R.color.earth_element
                        "air" -> R.color.air_element
                        "life" -> R.color.life_element
                        "undead" -> R.color.undead_element
                        "tech" -> R.color.tech_element
                        "magic" -> R.color.magic_element
                        "light" -> R.color.light_element
                        "dark" -> R.color.dark_element
                        else -> R.color.navy_blue
                    }
                )
                binding.characterElement.visibility = View.VISIBLE
            } else {
                binding.characterElement.visibility = View.GONE
            }
            
            // Display character type if available
            if (!char.metadata.characterType.isNullOrBlank()) {
                binding.characterType.text = "Type: ${char.metadata.characterType}"
                binding.characterType.visibility = View.VISIBLE
            } else {
                binding.characterType.visibility = View.GONE
            }
            
            // Display biography if available
            if (!char.metadata.biography.isNullOrBlank()) {
                binding.characterBiography.text = char.metadata.biography
                binding.characterBiography.visibility = View.VISIBLE
            } else {
                binding.characterBiography.visibility = View.GONE
            }
            
            // Display abilities if available
            if (char.metadata.abilities.isNotEmpty()) {
                binding.abilitiesTitle.visibility = View.VISIBLE
                binding.abilitiesList.visibility = View.VISIBLE
                
                // Setup RecyclerView for abilities
                val layoutManager = LinearLayoutManager(context).apply {
                    orientation = LinearLayoutManager.HORIZONTAL
                }
                binding.abilitiesList.layoutManager = layoutManager
                
                // Create chips for abilities
                val abilitiesAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<AbilityViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbilityViewHolder {
                        val chip = Chip(parent.context)
                        chip.setChipBackgroundColorResource(R.color.navy_blue_light)
                        chip.chipTextColor = parent.context.getColorStateList(R.color.white)
                        chip.setPadding(
                            resources.getDimensionPixelSize(R.dimen.chip_padding),
                            resources.getDimensionPixelSize(R.dimen.chip_padding),
                            resources.getDimensionPixelSize(R.dimen.chip_padding),
                            resources.getDimensionPixelSize(R.dimen.chip_padding)
                        )
                        return AbilityViewHolder(chip)
                    }
                    
                    override fun onBindViewHolder(holder: AbilityViewHolder, position: Int) {
                        holder.chip.text = char.metadata.abilities[position]
                    }
                    
                    override fun getItemCount() = char.metadata.abilities.size
                }
                binding.abilitiesList.adapter = abilitiesAdapter
            } else {
                binding.abilitiesTitle.visibility = View.GONE
                binding.abilitiesList.visibility = View.GONE
            }
            
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
    
    private class AbilityViewHolder(val chip: Chip) : androidx.recyclerview.widget.RecyclerView.ViewHolder(chip)
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


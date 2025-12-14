package com.bitcoinerrorlog.skywriter.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import com.bitcoinerrorlog.skywriter.databinding.ItemCharacterBinding

class CharacterAdapter(
    private val onCharacterClick: (CharacterModel) -> Unit
) : ListAdapter<CharacterModel, CharacterAdapter.CharacterViewHolder>(CharacterDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val binding = ItemCharacterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CharacterViewHolder(binding, onCharacterClick)
    }
    
    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class CharacterViewHolder(
        private val binding: ItemCharacterBinding,
        private val onCharacterClick: (CharacterModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(character: CharacterModel) {
            binding.characterName.text = character.metadata.displayName
            binding.characterGame.text = character.metadata.gameSeries
            
            // TODO: Load character icon when available
            // For now, using a placeholder
            
            binding.root.setOnClickListener {
                onCharacterClick(character)
            }
        }
    }
    
    private class CharacterDiffCallback : DiffUtil.ItemCallback<CharacterModel>() {
        override fun areItemsTheSame(oldItem: CharacterModel, newItem: CharacterModel): Boolean {
            return oldItem.uid == newItem.uid
        }
        
        override fun areContentsTheSame(oldItem: CharacterModel, newItem: CharacterModel): Boolean {
            return oldItem == newItem
        }
    }
}


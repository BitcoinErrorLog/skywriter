package com.bitcoinerrorlog.skywriter.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitcoinerrorlog.skywriter.R
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import com.bitcoinerrorlog.skywriter.databinding.ItemCharacterBinding
import com.bitcoinerrorlog.skywriter.databinding.ItemGameHeaderBinding

sealed class ListItem {
    data class Header(val gameTitle: String) : ListItem()
    data class Character(val character: CharacterModel) : ListItem()
}

class CharacterAdapter(
    private val onCharacterClick: (CharacterModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private val items = mutableListOf<ListItem>()
    
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CHARACTER = 1
    }
    
    fun submitList(charactersByGame: Map<String, List<CharacterModel>>) {
        val newItems = mutableListOf<ListItem>()
        
        charactersByGame.forEach { (gameTitle, characters) ->
            newItems.add(ListItem.Header(gameTitle))
            characters.forEach { character ->
                newItems.add(ListItem.Character(character))
            }
        }
        
        val diffCallback = ListItemDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.Character -> TYPE_CHARACTER
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemGameHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }
            TYPE_CHARACTER -> {
                val binding = ItemCharacterBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                CharacterViewHolder(binding, onCharacterClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item.gameTitle)
            is ListItem.Character -> (holder as CharacterViewHolder).bind(item.character)
        }
    }
    
    override fun getItemCount(): Int = items.size
    
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager
        layoutManager?.let { manager ->
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (items[position]) {
                        is ListItem.Header -> manager.spanCount
                        is ListItem.Character -> 1
                    }
                }
            }
        }
    }
    
    class HeaderViewHolder(
        private val binding: ItemGameHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(gameTitle: String) {
            binding.gameTitle.text = gameTitle
        }
    }
    
    class CharacterViewHolder(
        private val binding: ItemCharacterBinding,
        private val onCharacterClick: (CharacterModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(character: CharacterModel) {
            binding.characterName.text = character.metadata.displayName
            binding.characterGame.text = character.metadata.subcategory ?: ""
            
            binding.root.setOnClickListener {
                onCharacterClick(character)
            }
        }
    }
    
    private class ListItemDiffCallback(
        private val oldList: List<ListItem>,
        private val newList: List<ListItem>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            
            return when {
                oldItem is ListItem.Header && newItem is ListItem.Header -> 
                    oldItem.gameTitle == newItem.gameTitle
                oldItem is ListItem.Character && newItem is ListItem.Character -> 
                    oldItem.character.uid == newItem.character.uid
                else -> false
            }
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

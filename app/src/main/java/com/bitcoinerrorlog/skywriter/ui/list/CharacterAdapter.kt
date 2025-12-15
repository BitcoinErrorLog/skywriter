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
    data class Header(val gameTitle: String, var isExpanded: Boolean = false) : ListItem()
    data class Character(val character: CharacterModel) : ListItem()
}

class CharacterAdapter(
    private val onCharacterClick: (CharacterModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var onHeaderToggle: ((Int) -> Unit)? = null
    
    fun setOnHeaderToggleListener(listener: (Int) -> Unit) {
        onHeaderToggle = listener
    }
    
    private val items = mutableListOf<ListItem>()
    
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CHARACTER = 1
    }
    
    fun submitList(charactersByGame: Map<String, List<CharacterModel>>) {
        val newItems = mutableListOf<ListItem>()
        
        charactersByGame.forEach { (gameTitle, characters) ->
            // Find existing header state or default to collapsed
            // BUT: Search results should always be expanded
            val isSearchResult = gameTitle.startsWith("Search Results")
            val existingHeader = items.find { it is ListItem.Header && it.gameTitle == gameTitle } as? ListItem.Header
            val isExpanded = if (isSearchResult) {
                true // Always expand search results
            } else {
                existingHeader?.isExpanded ?: false
            }
            
            newItems.add(ListItem.Header(gameTitle, isExpanded))
            if (isExpanded) {
                characters.forEach { character ->
                    newItems.add(ListItem.Character(character))
                }
            }
        }
        
        val diffCallback = ListItemDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }
    
    fun toggleHeader(position: Int) {
        val item = items[position]
        if (item is ListItem.Header) {
            val wasExpanded = item.isExpanded
            item.isExpanded = !item.isExpanded
            
            // Get the characters for this game
            val characters = charactersByGameCache[item.gameTitle] ?: return
            
            if (wasExpanded) {
                // Collapse: Remove characters
                repeat(characters.size) {
                    if (position + 1 < items.size) {
                        items.removeAt(position + 1)
                    }
                }
                notifyItemRangeRemoved(position + 1, characters.size)
            } else {
                // Expand: Insert characters
                val characterItems = characters.map { ListItem.Character(it) }
                items.addAll(position + 1, characterItems)
                notifyItemRangeInserted(position + 1, characters.size)
            }
            
            // Update header icon
            notifyItemChanged(position)
        }
    }
    
    private var charactersByGameCache: Map<String, List<CharacterModel>> = emptyMap()
    
    fun setCharactersByGame(charactersByGame: Map<String, List<CharacterModel>>) {
        charactersByGameCache = charactersByGame
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
                HeaderViewHolder(binding) { position ->
                    onHeaderToggle?.invoke(position)
                }
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
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item)
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
        private val binding: ItemGameHeaderBinding,
        private val onHeaderClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: ListItem.Header) {
            binding.gameTitle.text = header.gameTitle
            binding.root.setOnClickListener {
                onHeaderClick(adapterPosition)
            }
            // Update expand icon (up arrow when expanded to collapse, down arrow when collapsed to expand)
            val iconRes = if (header.isExpanded) {
                android.R.drawable.arrow_up_float
            } else {
                android.R.drawable.arrow_down_float
            }
            binding.expandIcon.setImageResource(iconRes)
            // Ensure icon is white (tint is set in XML, but ensure it's applied)
            binding.expandIcon.setColorFilter(android.graphics.Color.WHITE)
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

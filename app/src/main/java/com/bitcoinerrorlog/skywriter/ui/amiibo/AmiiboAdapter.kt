package com.bitcoinerrorlog.skywriter.ui.amiibo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitcoinerrorlog.skywriter.data.AmiiboModel
import com.bitcoinerrorlog.skywriter.databinding.ItemAmiiboBinding
import com.bitcoinerrorlog.skywriter.databinding.ItemGameHeaderBinding

sealed class AmiiboListItem {
    data class Header(val gameTitle: String, var isExpanded: Boolean = false) : AmiiboListItem()
    data class Amiibo(val amiibo: AmiiboModel) : AmiiboListItem()
}

class AmiiboAdapter(
    private val onAmiiboClick: (AmiiboModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var onHeaderToggle: ((Int) -> Unit)? = null
    
    fun setOnHeaderToggleListener(listener: (Int) -> Unit) {
        onHeaderToggle = listener
    }
    
    private val items = mutableListOf<AmiiboListItem>()
    
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_AMIIBO = 1
    }
    
    fun submitList(amiibosByGame: Map<String, List<AmiiboModel>>) {
        val newItems = mutableListOf<AmiiboListItem>()
        
        amiibosByGame.forEach { (gameTitle, amiibos) ->
            // Find existing header state or default to collapsed
            // BUT: Search results should always be expanded
            val isSearchResult = gameTitle.startsWith("Search Results")
            val existingHeader = items.find { it is AmiiboListItem.Header && it.gameTitle == gameTitle } as? AmiiboListItem.Header
            val isExpanded = if (isSearchResult) {
                true // Always expand search results
            } else {
                existingHeader?.isExpanded ?: false
            }
            
            newItems.add(AmiiboListItem.Header(gameTitle, isExpanded))
            if (isExpanded) {
                amiibos.forEach { amiibo ->
                    newItems.add(AmiiboListItem.Amiibo(amiibo))
                }
            }
        }
        
        val diffCallback = AmiiboListItemDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }
    
    fun toggleHeader(position: Int) {
        val item = items[position]
        if (item is AmiiboListItem.Header) {
            val wasExpanded = item.isExpanded
            item.isExpanded = !item.isExpanded
            
            // Get the Amiibos for this game
            val amiibos = amiibosByGameCache[item.gameTitle] ?: return
            
            if (wasExpanded) {
                // Collapse: Remove Amiibos
                repeat(amiibos.size) {
                    if (position + 1 < items.size) {
                        items.removeAt(position + 1)
                    }
                }
                notifyItemRangeRemoved(position + 1, amiibos.size)
            } else {
                // Expand: Insert Amiibos
                val amiiboItems = amiibos.map { AmiiboListItem.Amiibo(it) }
                items.addAll(position + 1, amiiboItems)
                notifyItemRangeInserted(position + 1, amiibos.size)
            }
            
            // Update header icon
            notifyItemChanged(position)
        }
    }
    
    private var amiibosByGameCache: Map<String, List<AmiiboModel>> = emptyMap()
    
    fun setAmiibosByGame(amiibosByGame: Map<String, List<AmiiboModel>>) {
        amiibosByGameCache = amiibosByGame
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AmiiboListItem.Header -> TYPE_HEADER
            is AmiiboListItem.Amiibo -> TYPE_AMIIBO
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
            TYPE_AMIIBO -> {
                val binding = ItemAmiiboBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AmiiboViewHolder(binding, onAmiiboClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AmiiboListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is AmiiboListItem.Amiibo -> (holder as AmiiboViewHolder).bind(item.amiibo)
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
                        is AmiiboListItem.Header -> manager.spanCount
                        is AmiiboListItem.Amiibo -> 1
                    }
                }
            }
        }
    }
    
    class HeaderViewHolder(
        private val binding: ItemGameHeaderBinding,
        private val onHeaderClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: AmiiboListItem.Header) {
            binding.gameTitle.text = header.gameTitle
            binding.root.setOnClickListener {
                onHeaderClick(adapterPosition)
            }
            // Update expand icon
            val iconRes = if (header.isExpanded) {
                android.R.drawable.arrow_up_float
            } else {
                android.R.drawable.arrow_down_float
            }
            binding.expandIcon.setImageResource(iconRes)
            binding.expandIcon.setColorFilter(android.graphics.Color.WHITE)
        }
    }
    
    class AmiiboViewHolder(
        private val binding: ItemAmiiboBinding,
        private val onAmiiboClick: (AmiiboModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(amiibo: AmiiboModel) {
            binding.amiiboName.text = amiibo.metadata.displayName
            binding.amiiboGame.text = amiibo.metadata.gameSeriesDisplay
            
            binding.root.setOnClickListener {
                onAmiiboClick(amiibo)
            }
        }
    }
    
    private class AmiiboListItemDiffCallback(
        private val oldList: List<AmiiboListItem>,
        private val newList: List<AmiiboListItem>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            
            return when {
                oldItem is AmiiboListItem.Header && newItem is AmiiboListItem.Header -> 
                    oldItem.gameTitle == newItem.gameTitle
                oldItem is AmiiboListItem.Amiibo && newItem is AmiiboListItem.Amiibo -> 
                    oldItem.amiibo.uid == newItem.amiibo.uid
                else -> false
            }
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}


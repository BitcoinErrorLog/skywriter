package com.bitcoinerrorlog.skywriter.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data model representing a character with NFC data.
 * 
 * @param uid Unique identifier of the NFC tag (hex string)
 * @param atqa Answer to request type A (hex string)
 * @param sak Select acknowledge (hex string)
 * @param mifareType Type of Mifare card (e.g., "1K")
 * @param blocks List of 64 hex strings, each representing 16 bytes (32 hex chars)
 * @param metadata Character metadata including name and game information
 */
@Parcelize
data class CharacterModel(
    val uid: String,
    val atqa: String,
    val sak: String,
    val mifareType: String,
    val blocks: List<String>, // List of hex strings, each representing 16 bytes
    val metadata: CharacterMetadata
) : Parcelable

/**
 * Metadata about a character.
 * 
 * @param originalFilename Original filename from the NFC file
 * @param originalPath Path to the original file
 * @param category Game series/category (e.g., "Skylanders 1 Spyro's Adventure")
 * @param subcategory Subcategory within the game (e.g., "Figures", "Magic Items")
 * @param element Character element (Fire, Water, Earth, Air, Life, Undead, Tech, Magic, Light, Dark)
 * @param biography Character biography/description
 * @param abilities List of character abilities/special moves
 * @param characterType Type of character (Core, Giant, Swapper, Trap Master, Supercharger, Sensei, etc.)
 */
@Parcelize
data class CharacterMetadata(
    val originalFilename: String,
    val originalPath: String,
    val category: String?,
    val subcategory: String?,
    val element: String? = null,
    val biography: String? = null,
    val abilities: List<String> = emptyList(),
    val characterType: String? = null
) : Parcelable {
    /**
     * Display name derived from filename (without .nfc extension)
     */
    val displayName: String
        get() = originalFilename.removeSuffix(".nfc")
    
    /**
     * Game series name, or "Unknown" if category is null
     */
    val gameSeries: String
        get() = category ?: "Unknown"
    
    /**
     * Get element color for UI display
     */
    val elementColor: Int
        get() = when (element?.lowercase()) {
            "fire" -> android.graphics.Color.parseColor("#FF6B35")
            "water" -> android.graphics.Color.parseColor("#4A90E2")
            "earth" -> android.graphics.Color.parseColor("#8B4513")
            "air" -> android.graphics.Color.parseColor("#87CEEB")
            "life" -> android.graphics.Color.parseColor("#90EE90")
            "undead" -> android.graphics.Color.parseColor("#9370DB")
            "tech" -> android.graphics.Color.parseColor("#C0C0C0")
            "magic" -> android.graphics.Color.parseColor("#FF69B4")
            "light" -> android.graphics.Color.parseColor("#FFD700")
            "dark" -> android.graphics.Color.parseColor("#2F2F2F")
            else -> android.graphics.Color.parseColor("#808080")
        }
}

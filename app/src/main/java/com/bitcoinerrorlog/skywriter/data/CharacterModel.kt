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
 */
@Parcelize
data class CharacterMetadata(
    val originalFilename: String,
    val originalPath: String,
    val category: String?,
    val subcategory: String?
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
}

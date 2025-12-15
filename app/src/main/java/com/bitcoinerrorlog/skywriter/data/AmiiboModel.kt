package com.bitcoinerrorlog.skywriter.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data model representing an Amiibo with NFC data.
 * 
 * @param uid Unique identifier of the NFC tag (hex string)
 * @param pages List of 135 pages, each representing 4 bytes (8 hex chars)
 * @param metadata Amiibo metadata including character name and game information
 */
@Parcelize
data class AmiiboModel(
    val uid: String,
    val pages: List<String>, // List of hex strings, each representing 4 bytes (8 hex chars)
    val metadata: AmiiboMetadata
) : Parcelable

/**
 * Metadata about an Amiibo.
 * 
 * @param originalFilename Original filename from the NFC file
 * @param originalPath Path to the original file
 * @param characterName Character name (e.g., "Mario", "Link")
 * @param gameSeries Game series (e.g., "Super Smash Bros.", "The Legend of Zelda", "Mario")
 * @param characterId Character ID from Amiibo data (hex string)
 * @param gameId Game ID from Amiibo data (hex string)
 * @param biography Character biography/description
 * @param releaseDate Release date of the Amiibo
 * @param amiiboType Type of Amiibo (Figure, Card, etc.)
 */
@Parcelize
data class AmiiboMetadata(
    val originalFilename: String,
    val originalPath: String,
    val characterName: String,
    val gameSeries: String? = null,
    val characterId: String? = null,
    val gameId: String? = null,
    val biography: String? = null,
    val releaseDate: String? = null,
    val amiiboType: String? = null
) : Parcelable {
    /**
     * Display name derived from character name or filename
     */
    val displayName: String
        get() = characterName.ifBlank { originalFilename.removeSuffix(".bin").removeSuffix(".nfc") }
    
    /**
     * Game series name, or "Unknown" if gameSeries is null
     */
    val gameSeriesDisplay: String
        get() = gameSeries ?: "Unknown"
}


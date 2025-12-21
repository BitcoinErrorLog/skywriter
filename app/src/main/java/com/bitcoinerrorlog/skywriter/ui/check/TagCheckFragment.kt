package com.bitcoinerrorlog.skywriter.ui.check

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bitcoinerrorlog.skywriter.MainActivity
import com.bitcoinerrorlog.skywriter.R
import com.bitcoinerrorlog.skywriter.databinding.FragmentTagCheckBinding
import com.bitcoinerrorlog.skywriter.data.NFCDatabase
import com.bitcoinerrorlog.skywriter.data.AmiiboDatabase
import com.bitcoinerrorlog.skywriter.nfc.CompatibilityResult
import com.bitcoinerrorlog.skywriter.nfc.NFCManager
import com.bitcoinerrorlog.skywriter.nfc.TagCompatibilityChecker
import com.bitcoinerrorlog.skywriter.nfc.NTAG215CompatibilityChecker
import com.bitcoinerrorlog.skywriter.nfc.TagCompatibilityInfo
import com.bitcoinerrorlog.skywriter.nfc.TagReader
import com.bitcoinerrorlog.skywriter.nfc.TagReadResult
import com.bitcoinerrorlog.skywriter.nfc.AmiiboTagReader
import com.bitcoinerrorlog.skywriter.nfc.AmiiboReadResult
import com.bitcoinerrorlog.skywriter.nfc.TagEraser
import com.bitcoinerrorlog.skywriter.nfc.AmiiboTagEraser
import com.bitcoinerrorlog.skywriter.nfc.EraseResult
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class TagCheckFragment : Fragment(), MainActivity.OnNfcTagDetectedListener {
    
    private var _binding: FragmentTagCheckBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var nfcManager: NFCManager
    private val skylandersChecker = TagCompatibilityChecker()
    private val amiiboChecker = NTAG215CompatibilityChecker()
    private lateinit var skylandersReader: TagReader
    private lateinit var amiiboReader: AmiiboTagReader
    private val skylandersEraser = TagEraser()
    private val amiiboEraser = AmiiboTagEraser()
    private var lastCompatibilityInfo: TagCompatibilityInfo? = null
    private var waitingForTag = false
    private var currentTag: android.nfc.Tag? = null
    private var identifiedCharacter: com.bitcoinerrorlog.skywriter.data.CharacterModel? = null
    private var identifiedAmiibo: com.bitcoinerrorlog.skywriter.data.AmiiboModel? = null
    private var lastReadResult: TagReadResult? = null
    private var lastAmiiboReadResult: AmiiboReadResult? = null
    private var isAmiiboMode = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTagCheckBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        nfcManager = NFCManager(requireActivity())
        
        if (!nfcManager.isNFCAvailable) {
            showError(getString(R.string.nfc_not_available))
            return
        }
        
        // Initialize tag readers with databases
        val skylandersDatabase = NFCDatabase(requireContext())
        val amiiboDatabase = AmiiboDatabase(requireContext())
        skylandersReader = TagReader(skylandersDatabase)
        amiiboReader = AmiiboTagReader(amiiboDatabase)
        
        // Detect current mode from navigation
        detectCurrentMode()
        
        binding.checkButton.setOnClickListener {
            if (currentTag != null) {
                // Tag already detected, check it
                checkTag(currentTag!!)
            } else {
                // Start waiting for tag
                waitingForTag = true
                updateUI(CheckState.WaitingForTag)
            }
        }
        
        binding.checkAgainButton.setOnClickListener {
            lastCompatibilityInfo = null
            currentTag = null
            identifiedCharacter = null
            identifiedAmiibo = null
            lastReadResult = null
            lastAmiiboReadResult = null
            waitingForTag = false
            detectCurrentMode() // Re-detect mode
            updateUI(CheckState.Ready)
        }
        
        binding.eraseButton.setOnClickListener {
            if (currentTag != null) {
                // Show confirmation dialog
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Erase Tag?")
                    .setMessage("This will erase all data on the tag. This action cannot be undone.\n\nAre you sure you want to continue?")
                    .setPositiveButton("Erase") { _, _ ->
                        eraseTag(currentTag!!)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                // Start waiting for tag
                waitingForTag = true
                updateUI(CheckState.WaitingForTag)
                binding.statusText.text = "Tap tag to erase..."
                binding.detailsText.text = "Place the tag you want to erase on your device"
            }
        }
        
        updateUI(CheckState.Ready)
    }
    
    override fun onResume() {
        super.onResume()
        // NFC ReaderMode is managed by MainActivity and dispatched to us
    }
    
    override fun onPause() {
        super.onPause()
        // Clear screen-on flag when leaving fragment
        if (isAdded) {
            requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    override fun onNfcTagDetected(intent: Intent) {
        val tag = nfcManager.getTagFromIntent(intent) ?: return
        currentTag = tag
        
        // Auto-detect tag type and set mode accordingly
        val isMifare = MifareClassic.get(tag) != null
        val isNfcA = NfcA.get(tag) != null
        
        // If tag is NTAG215 (NfcA but not Mifare), switch to Amiibo mode
        if (isNfcA && !isMifare) {
            android.util.Log.d("TagCheckFragment", "Auto-detected Amiibo mode from tag type (NTAG215)")
            isAmiiboMode = true
        } else if (isMifare) {
            android.util.Log.d("TagCheckFragment", "Auto-detected Skylanders mode from tag type (Mifare Classic)")
            isAmiiboMode = false
        }
        
        // If waiting for erase, show confirmation dialog
        if (waitingForTag && binding.eraseButton.visibility == View.VISIBLE) {
            waitingForTag = false
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Erase Tag?")
                .setMessage("This will erase all data on the tag. This action cannot be undone.\n\nAre you sure you want to continue?")
                .setPositiveButton("Erase") { _, _ ->
                    eraseTag(tag)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    updateUI(CheckState.Ready)
                    currentTag = null
                }
                .show()
            return
        }
        
        // Auto-check tag when detected (more reliable flow)
        checkTag(tag)
    }
    
    private fun detectCurrentMode() {
        // First, try to detect from navigation destination
        try {
            val currentDestination = findNavController().currentDestination?.id
            isAmiiboMode = when (currentDestination) {
                R.id.amiiboListFragment, R.id.writeAmiiboFragment -> true
                else -> false
            }
            android.util.Log.d("TagCheckFragment", "Detected mode from navigation: ${if (isAmiiboMode) "Amiibo" else "Skylanders"}")
        } catch (e: Exception) {
            android.util.Log.w("TagCheckFragment", "Could not detect mode from navigation: ${e.message}")
            isAmiiboMode = false
        }
        
        // If we have a current tag, also check its type to auto-detect
        currentTag?.let { tag ->
            val isMifare = MifareClassic.get(tag) != null
            val isNfcA = NfcA.get(tag) != null
            
            // If tag is NTAG215 (NfcA but not Mifare), switch to Amiibo mode
            if (isNfcA && !isMifare) {
                android.util.Log.d("TagCheckFragment", "Auto-detected Amiibo mode from tag type (NTAG215)")
                isAmiiboMode = true
            } else if (isMifare) {
                android.util.Log.d("TagCheckFragment", "Auto-detected Skylanders mode from tag type (Mifare Classic)")
                isAmiiboMode = false
            }
        }
    }
    
    private fun checkTag(tag: android.nfc.Tag) {
        waitingForTag = false
        updateUI(CheckState.Checking)
        identifiedCharacter = null
        identifiedAmiibo = null
        
        // Re-detect mode in case user navigated
        detectCurrentMode()
        
        // Keep screen on during read/check operations
        requireActivity().window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        lifecycleScope.launch {
            try {
                val compatibilityInfo: TagCompatibilityInfo
                var readResult: TagReadResult? = null
                var amiiboReadResult: AmiiboReadResult? = null
                
                if (isAmiiboMode) {
                    // Amiibo mode - check NTAG215 compatibility and read Amiibo data
                    binding.statusText.text = "Reading Amiibo data..."
                    try {
                        amiiboReadResult = amiiboReader.readAndIdentifyTag(tag)
                        lastAmiiboReadResult = amiiboReadResult
                        if (amiiboReadResult is AmiiboReadResult.Success) {
                            identifiedAmiibo = amiiboReadResult.identifiedAmiibo
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("TagCheckFragment", "Could not read Amiibo data: ${e.message}")
                    }
                    
                    binding.statusText.text = "Checking NTAG215 compatibility..."
                    compatibilityInfo = amiiboChecker.checkCompatibility(tag)
                    lastCompatibilityInfo = compatibilityInfo
                } else {
                    // Skylanders mode - check Mifare Classic compatibility and read character data
                    binding.statusText.text = "Reading tag data..."
                    try {
                        readResult = skylandersReader.readAndIdentifyTag(tag)
                        lastReadResult = readResult
                        if (readResult is TagReadResult.Success) {
                            identifiedCharacter = readResult.identifiedCharacter
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("TagCheckFragment", "Could not read character data: ${e.message}")
                    }
                    
                    binding.statusText.text = "Checking Mifare Classic compatibility..."
                    compatibilityInfo = skylandersChecker.checkCompatibility(tag)
                    lastCompatibilityInfo = compatibilityInfo
                }
                
                // Clear screen-on flag when done
                if (isAdded) {
                    requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                
                if (isAdded && view != null) {
                    if (isAmiiboMode) {
                        displayCompatibilityInfo(compatibilityInfo, null, amiiboReadResult)
                    } else {
                        displayCompatibilityInfo(compatibilityInfo, readResult, null)
                    }
                }
            } catch (e: Exception) {
                // Clear screen-on flag on error
                if (isAdded) {
                    requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                android.util.Log.e("TagCheckFragment", "Error checking tag", e)
                if (isAdded && view != null) {
                    showError("Error checking tag: ${e.message ?: "Unknown error"}")
                    updateUI(CheckState.Ready)
                }
            }
        }
    }
    
    private fun displayCompatibilityInfo(
        info: TagCompatibilityInfo, 
        readResult: TagReadResult?,
        amiiboReadResult: AmiiboReadResult?
    ) {
        if (!isAdded || view == null) return
        
        updateUI(CheckState.Complete)
        
        // Show read status based on mode
        if (isAmiiboMode && amiiboReadResult is AmiiboReadResult.Success) {
            val readStatus = buildString {
                append("Data Read: ${amiiboReadResult.pagesRead}/135 pages\n")
                if (amiiboReadResult.pagesRead < 135) {
                    append("⚠ Partial read - tag may be damaged or partially written\n")
                }
            }
            binding.readStatusText.text = readStatus
            binding.readStatusText.visibility = View.VISIBLE
            binding.readStatusTitle.visibility = View.VISIBLE
        } else if (!isAmiiboMode && readResult is TagReadResult.Success) {
            val readStatus = buildString {
                append("Data Read: ${readResult.blocksRead}/64 blocks\n")
                append("Sectors Authenticated: ${readResult.sectorsAuthenticated}/16\n")
                if (readResult.blocksRead < 64) {
                    append("⚠ Partial read - tag may have custom keys or be partially written\n")
                }
            }
            binding.readStatusText.text = readStatus
            binding.readStatusText.visibility = View.VISIBLE
            binding.readStatusTitle.visibility = View.VISIBLE
        } else {
            binding.readStatusText.visibility = View.GONE
            binding.readStatusTitle.visibility = View.GONE
        }
        
        // Set overall status
        when (val result = info.result) {
            is CompatibilityResult.Compatible -> {
                binding.statusIcon.setImageResource(android.R.drawable.ic_dialog_info)
                binding.statusText.text = getString(R.string.tag_compatible)
                binding.statusText.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                )
            }
            is CompatibilityResult.Warning -> {
                binding.statusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                binding.statusText.text = result.message
                binding.statusText.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                )
            }
            is CompatibilityResult.Incompatible -> {
                binding.statusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                binding.statusText.text = result.reason
                binding.statusText.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                )
            }
        }
        
        // Display character/Amiibo identification if found
        if (isAmiiboMode && identifiedAmiibo != null) {
            val amiiboInfo = buildString {
                append("Amiibo Found:\n")
                val amiibo = identifiedAmiibo!!
                append("• Name: ${amiibo.metadata.displayName}\n")
                val gameSeries = amiibo.metadata.gameSeries
                if (!gameSeries.isNullOrEmpty()) {
                    append("• Game Series: $gameSeries\n")
                }
                val amiiboType = amiibo.metadata.amiiboType
                if (!amiiboType.isNullOrEmpty()) {
                    append("• Type: $amiiboType\n")
                }
                append("\n")
            }
            binding.characterInfoText.text = amiiboInfo
            binding.characterInfoText.visibility = View.VISIBLE
            binding.characterInfoTitle.visibility = View.VISIBLE
        } else if (!isAmiiboMode && identifiedCharacter != null) {
            val characterInfo = buildString {
                append("Character Found:\n")
                val char = identifiedCharacter!!
                append("• Name: ${char.metadata.displayName}\n")
                val gameSeries = char.metadata.gameSeries
                if (!gameSeries.isNullOrEmpty()) {
                    append("• Game: $gameSeries\n")
                }
                val subcategory = char.metadata.subcategory
                if (!subcategory.isNullOrEmpty()) {
                    append("• Type: $subcategory\n")
                }
                append("\n")
            }
            binding.characterInfoText.text = characterInfo
            binding.characterInfoText.visibility = View.VISIBLE
            binding.characterInfoTitle.visibility = View.VISIBLE
        } else {
            binding.characterInfoText.visibility = View.GONE
            binding.characterInfoTitle.visibility = View.GONE
        }
        
        // Display tag details
        val details = buildString {
            append("Tag Type: ${info.tagType}\n")
            if (isAmiiboMode) {
                // NTAG215 has 135 pages, 540 bytes total
                append("Page Count: 135\n")
                append("Total Size: 540 bytes\n")
            } else {
                append("Block Count: ${info.blockCount}\n")
                append("Sector Count: ${info.sectorCount}\n")
            }
            append("UID: ${info.uid ?: "Unknown"}\n")
            append("\n")
            append("Capabilities:\n")
            append("• Authentication: ${if (info.authenticationTest) "✓" else "✗"}\n")
            append("• Readable: ${if (info.readable) "✓" else "✗"}\n")
            append("• Writable: ${if (info.writable) "✓" else "✗"}\n")
            info.uidChangeable?.let {
                append("• UID Changeable: ${if (it) "✓" else "✗ (Locked)"}\n")
            }
            append("\nMode: ${if (isAmiiboMode) "Amiibo (NTAG215)" else "Skylanders (Mifare Classic 1K)"}\n")
        }
        binding.detailsText.text = details
        
        // Display issues if any
        if (info.issues.isNotEmpty()) {
            binding.issuesTitle.visibility = View.VISIBLE
            binding.issuesText.visibility = View.VISIBLE
            binding.issuesText.text = info.issues.joinToString("\n") { "• $it" }
        } else {
            binding.issuesTitle.visibility = View.GONE
            binding.issuesText.visibility = View.GONE
        }
        
        // Display recommendations if any
        if (info.recommendations.isNotEmpty()) {
            binding.recommendationsTitle.visibility = View.VISIBLE
            binding.recommendationsText.visibility = View.VISIBLE
            binding.recommendationsText.text = info.recommendations.joinToString("\n") { "• $it" }
        } else {
            binding.recommendationsTitle.visibility = View.GONE
            binding.recommendationsText.visibility = View.GONE
        }
        
        binding.checkAgainButton.visibility = View.VISIBLE
        
        // Show erase button if tag is writable
        if (info.writable || info.result is CompatibilityResult.Warning) {
            binding.eraseButton.visibility = View.VISIBLE
        } else {
            binding.eraseButton.visibility = View.GONE
        }
        
        // Show tag contents summary based on mode
        if (isAmiiboMode && amiiboReadResult is AmiiboReadResult.Success) {
            val amiibo = amiiboReadResult.identifiedAmiibo
            val summary = buildString {
                append("Tag Contents:\n")
                if (amiibo != null) {
                    append("• Amiibo: ${amiibo.metadata.displayName}\n")
                    val gameSeries = amiibo.metadata.gameSeries
                    if (!gameSeries.isNullOrEmpty()) {
                        append("• Game Series: $gameSeries\n")
                    }
                } else {
                    append("• Amiibo: Unknown or blank\n")
                }
                append("• Pages Read: ${amiiboReadResult.pagesRead}/135\n")
                if (amiiboReadResult.pagesRead < 135) {
                    append("• Status: Partial data\n")
                } else if (amiiboReadResult.pagesRead == 135 && amiibo == null) {
                    append("• Status: Data present but not recognized\n")
                } else {
                    append("• Status: Complete Amiibo data\n")
                }
            }
            binding.tagContentsText.text = summary
            binding.tagContentsText.visibility = View.VISIBLE
            binding.tagContentsTitle.visibility = View.VISIBLE
            
            // Populate Hex Viewer for Amiibo
            displayHexData(amiiboReadResult.pages, 4) // NTAG215: 4 bytes per page
        } else if (!isAmiiboMode && readResult is TagReadResult.Success) {
            val char = readResult.identifiedCharacter
            val summary = buildString {
                append("Tag Contents:\n")
                if (char != null) {
                    append("• Character: ${char.metadata.displayName}\n")
                    val gameSeries = char.metadata.gameSeries
                    if (!gameSeries.isNullOrEmpty()) {
                        append("• Game: $gameSeries\n")
                    }
                } else {
                    append("• Character: Unknown or blank\n")
                }
                append("• Blocks Read: ${readResult.blocksRead}/64\n")
                append("• Sectors Authenticated: ${readResult.sectorsAuthenticated}/16\n")
                if (readResult.blocksRead < 64) {
                    append("• Status: Partial data or custom keys\n")
                } else if (readResult.blocksRead == 64 && char == null) {
                    append("• Status: Data present but not recognized\n")
                } else {
                    append("• Status: Complete character data\n")
                }
            }
            binding.tagContentsText.text = summary
            binding.tagContentsText.visibility = View.VISIBLE
            binding.tagContentsTitle.visibility = View.VISIBLE
            
            // Populate Hex Viewer for Skylanders
            displayHexData(readResult.blocks, 16) // Mifare Classic: 16 bytes per block
        } else {
            binding.tagContentsText.visibility = View.GONE
            binding.tagContentsTitle.visibility = View.GONE
            binding.hexViewerTitle.visibility = View.GONE
            binding.hexViewerCard.visibility = View.GONE
        }
    }
    
    private fun displayHexData(data: List<String>, bytesPerRow: Int) {
        if (data.isEmpty()) {
            binding.hexViewerTitle.visibility = View.GONE
            binding.hexViewerCard.visibility = View.GONE
            return
        }
        
        val hexString = buildString {
            for ((index, rowData) in data.withIndex()) {
                val prefix = if (bytesPerRow == 4) "Page %03d: ".format(index) else "Block %02d: ".format(index)
                append(prefix)
                
                // Group hex string into bytes (2 chars each)
                val formattedRow = rowData.chunked(2).joinToString(" ")
                append(formattedRow)
                
                if (index < data.size - 1) {
                    append("\n")
                }
            }
        }
        
        binding.hexViewerText.text = hexString
        binding.hexViewerTitle.visibility = View.VISIBLE
        binding.hexViewerCard.visibility = View.VISIBLE
    }
    
    private fun eraseTag(tag: android.nfc.Tag) {
        waitingForTag = false
        updateUI(CheckState.Erasing)
        binding.statusText.text = "Erasing tag..."
        binding.detailsText.text = "Keep the tag close to your device..."
        
        // Re-detect mode to use correct eraser
        detectCurrentMode()
        
        // Keep screen on during erase operation
        requireActivity().window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        lifecycleScope.launch {
            try {
                val result = if (isAmiiboMode) {
                    amiiboEraser.eraseTag(tag)
                } else {
                    skylandersEraser.eraseTag(tag)
                }
                
                // Clear screen-on flag when done
                if (isAdded) {
                    requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                
                if (isAdded && view != null) {
                    when (result) {
                        is EraseResult.Success -> {
                            binding.statusText.text = "✓ Tag erased successfully!"
                            binding.statusText.setTextColor(
                                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                            )
                            binding.detailsText.text = result.message + "\n\nTag is now blank and ready for writing."
                            binding.progressBar.visibility = View.GONE
                            
                            // Reset state
                            identifiedCharacter = null
                            lastReadResult = null
                            currentTag = null
                            
                            // Show check again button
                            binding.checkAgainButton.visibility = View.VISIBLE
                            binding.eraseButton.visibility = View.GONE
                        }
                        is EraseResult.Error -> {
                            // Clear screen-on flag on error
                            if (isAdded) {
                                requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                            binding.statusText.text = "✗ Erase failed"
                            binding.statusText.setTextColor(
                                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                            )
                            binding.detailsText.text = result.message
                            binding.progressBar.visibility = View.GONE
                            binding.checkAgainButton.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                // Clear screen-on flag on error
                if (isAdded) {
                    requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                android.util.Log.e("TagCheckFragment", "Error erasing tag", e)
                if (isAdded && view != null) {
                    binding.statusText.text = "✗ Error erasing tag"
                    binding.statusText.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    )
                    binding.detailsText.text = "Error: ${e.message ?: "Unknown error"}"
                    binding.progressBar.visibility = View.GONE
                    binding.checkAgainButton.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun updateUI(state: CheckState) {
        if (!isAdded || view == null) return
        
        when (state) {
            CheckState.Ready -> {
                binding.progressBar.visibility = View.GONE
                binding.statusIcon.visibility = View.VISIBLE
                binding.statusText.text = getString(R.string.tap_check_button)
                binding.statusText.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                )
                binding.detailsText.text = ""
                binding.characterInfoTitle.visibility = View.GONE
                binding.characterInfoText.visibility = View.GONE
                binding.readStatusTitle.visibility = View.GONE
                binding.readStatusText.visibility = View.GONE
                binding.tagContentsTitle.visibility = View.GONE
                binding.tagContentsText.visibility = View.GONE
                binding.issuesTitle.visibility = View.GONE
                binding.issuesText.visibility = View.GONE
                binding.recommendationsTitle.visibility = View.GONE
                binding.recommendationsText.visibility = View.GONE
                binding.checkButton.isEnabled = true
                binding.checkButton.visibility = View.VISIBLE
                binding.checkAgainButton.visibility = View.GONE
                binding.eraseButton.visibility = View.GONE
            }
            CheckState.WaitingForTag -> {
                binding.progressBar.visibility = View.GONE
                binding.statusIcon.visibility = View.VISIBLE
                binding.statusText.text = getString(R.string.tap_tag_to_check)
                binding.statusText.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                )
                binding.checkButton.isEnabled = false
            }
            CheckState.TagDetected -> {
                binding.progressBar.visibility = View.GONE
                binding.statusIcon.visibility = View.VISIBLE
                binding.statusText.text = getString(R.string.tag_detected)
                binding.statusText.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
                )
                binding.checkButton.isEnabled = true
            }
            CheckState.Checking -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusIcon.visibility = View.GONE
                binding.statusText.text = getString(R.string.checking_compatibility)
                binding.checkButton.isEnabled = false
            }
            CheckState.Complete -> {
                binding.progressBar.visibility = View.GONE
                binding.statusIcon.visibility = View.VISIBLE
                binding.checkButton.visibility = View.GONE
                binding.checkAgainButton.visibility = View.VISIBLE
            }
            CheckState.Erasing -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusIcon.visibility = View.GONE
                binding.checkButton.visibility = View.GONE
                binding.checkAgainButton.visibility = View.GONE
                binding.eraseButton.visibility = View.GONE
            }
        }
    }
    
    private fun showError(message: String) {
        if (!isAdded || view == null) return
        
        binding.progressBar.visibility = View.GONE
        binding.statusText.text = message
        binding.statusText.setTextColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Ensure screen-on flag is cleared
        if (isAdded) {
            requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        _binding = null
    }
    
    private enum class CheckState {
        Ready, WaitingForTag, TagDetected, Checking, Complete, Erasing
    }
}


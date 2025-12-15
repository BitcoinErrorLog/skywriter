package com.bitcoinerrorlog.skywriter.ui.write

import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bitcoinerrorlog.skywriter.MainActivity
import com.bitcoinerrorlog.skywriter.R
import com.bitcoinerrorlog.skywriter.data.CharacterModel
import com.bitcoinerrorlog.skywriter.databinding.FragmentWriteNfcBinding
import com.bitcoinerrorlog.skywriter.nfc.MifareClassicWriter
import com.bitcoinerrorlog.skywriter.nfc.NFCManager
import com.bitcoinerrorlog.skywriter.nfc.CompatibilityResult
import com.bitcoinerrorlog.skywriter.nfc.TagCompatibilityChecker
import com.bitcoinerrorlog.skywriter.nfc.TagCompatibilityInfo
import com.bitcoinerrorlog.skywriter.nfc.WriteResult
import kotlinx.coroutines.launch

class WriteNFCFragment : Fragment(), MainActivity.OnNfcTagDetectedListener {
    
    private var _binding: FragmentWriteNfcBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var nfcManager: NFCManager
    private val writer = MifareClassicWriter()
    private val compatibilityChecker = TagCompatibilityChecker()
    private var character: CharacterModel? = null
    private var waitingForTag = false
    private var currentTag: android.nfc.Tag? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            character = it.getParcelable("character")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWriteNfcBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        nfcManager = NFCManager(requireActivity())
        
        if (!nfcManager.isNFCAvailable) {
            showError(getString(R.string.nfc_not_available))
            return
        }
        
        character?.let { char ->
            binding.characterName.text = char.metadata.displayName
            binding.characterGame.text = char.metadata.gameSeries
        }
        
        binding.writeButton.setOnClickListener {
            if (currentTag != null) {
                // Tag already detected, check compatibility then write
                character?.let { char ->
                    checkCompatibilityThenWrite(currentTag!!)
                } ?: run {
                    showError("No character selected")
                }
            } else {
                // Start waiting for tag
                waitingForTag = true
                updateUI(WriteState.WaitingForTag)
            }
        }
        
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Reset state when fragment is created
        waitingForTag = false
        currentTag = null
        pendingWriteAction = null
        updateUI(WriteState.Ready)
    }
    
    override fun onResume() {
        super.onResume()
        nfcManager.enableForegroundDispatch()
    }
    
    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch()
        // Clear screen-on flag when leaving fragment
        if (isAdded) {
            requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    override fun onNfcTagDetected(intent: Intent) {
        val tag = nfcManager.getTagFromIntent(intent) ?: return
        // Always update current tag to get fresh connection
        currentTag = tag
        
        // If we have a pending write action (from compatibility warning), execute it immediately
        if (pendingWriteAction != null && waitingForTag) {
            waitingForTag = false
            binding.statusText.text = "Tag detected! Starting write..."
            binding.instructionText.text = "Keep the tag close during writing..."
            binding.progressBar.visibility = View.VISIBLE
            requireActivity().window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            pendingWriteAction?.invoke()
            pendingWriteAction = null
            return
        }
        
        // If waiting for tag and no pending action, check compatibility then write
        if (waitingForTag) {
            binding.statusText.text = "Tag detected! Checking compatibility..."
            binding.instructionText.text = "Please wait..."
            checkCompatibilityThenWrite(tag)
        } else {
            // Just store the tag, wait for user to tap write button
            updateUI(WriteState.TagDetected)
        }
    }
    
    private fun checkCompatibilityThenWrite(tag: android.nfc.Tag) {
        waitingForTag = false
        updateUI(WriteState.Checking)
        binding.statusText.text = "Checking tag compatibility..."
        binding.instructionText.text = "Please wait while we check the tag..."
        
        // Keep screen on during operations
        requireActivity().window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        lifecycleScope.launch {
            val compatibilityInfo = compatibilityChecker.checkCompatibility(tag)
            
            when (val result = compatibilityInfo.result) {
                is CompatibilityResult.Compatible -> {
                    // Tag is compatible, proceed with write
                    character?.let { char ->
                        writeToTag(tag, char)
                    }
                }
                is CompatibilityResult.Warning -> {
                    // Show warning but allow write
                    // Store character for later write (will use newly detected tag)
                    val charToWrite = character
                    showCompatibilityWarning(compatibilityInfo, result) {
                        // This will be called after tag is re-detected
                        charToWrite?.let { char ->
                            currentTag?.let { freshTag ->
                                writeToTag(freshTag, char)
                            } ?: run {
                                showError("Tag not detected. Please keep the tag close and try again.")
                            }
                        }
                    }
                }
                is CompatibilityResult.Incompatible -> {
                    // Tag is incompatible, show error
                    showCompatibilityError(compatibilityInfo, result)
                    currentTag = null
                }
            }
        }
    }
    
    private fun showCompatibilityWarning(info: TagCompatibilityInfo, warning: CompatibilityResult.Warning, onContinue: () -> Unit) {
        val message = buildString {
            append(warning.message)
            if (warning.details.isNotEmpty()) {
                append("\n\n")
                append(warning.details.joinToString("\n"))
            }
        }
        
        binding.statusText.text = message
        binding.statusText.setTextColor(
            resources.getColor(android.R.color.holo_orange_dark, null)
        )
        binding.instructionText.text = "Tap 'Write Anyway' below to overwrite this tag"
        binding.progressBar.visibility = View.GONE
        
        // Update write button to continue - write immediately if tag is present
        binding.writeButton.text = getString(R.string.write_anyway)
        binding.writeButton.visibility = View.VISIBLE
        binding.writeButton.isEnabled = true
        binding.writeButton.setOnClickListener {
            // Write immediately using current tag if available
            if (currentTag != null) {
                // Tag is still present, write immediately
                onContinue()
            } else {
                // Tag was lost, wait for re-detection
                waitingForTag = true
                updateUI(WriteState.WaitingForTag)
                binding.statusText.text = "Ready to write"
                binding.statusText.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                )
                binding.instructionText.text = "Keep the tag close to your device and tap it again to start writing"
                binding.progressBar.visibility = View.GONE
                // Store the continue action to call after tag is re-detected
                pendingWriteAction = onContinue
            }
        }
    }
    
    private var pendingWriteAction: (() -> Unit)? = null
    
    private fun showCompatibilityError(info: TagCompatibilityInfo, error: CompatibilityResult.Incompatible) {
        val message = buildString {
            append(error.reason)
            if (error.details.isNotEmpty()) {
                append("\n\n")
                append("Issues:\n")
                error.details.forEach { detail ->
                    append("• $detail\n")
                }
            }
            if (info.recommendations.isNotEmpty()) {
                append("\nRecommendations:\n")
                info.recommendations.forEach { rec ->
                    append("• $rec\n")
                }
            }
        }
        
        showError(message)
    }
    
    private fun writeToTag(tag: android.nfc.Tag, character: CharacterModel) {
        // Verify tag is still valid before writing
        if (tag.id == null || tag.id.isEmpty()) {
            showError("Tag is no longer valid. Please keep the tag close and try again.")
            currentTag = null
            waitingForTag = false
            requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            return
        }
        
        // Reset waiting state since we're writing now
        waitingForTag = false
        pendingWriteAction = null
        
        // Keep screen on during write operation
        requireActivity().window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        updateUI(WriteState.Writing)
        binding.statusText.text = "Writing ${character.metadata.displayName}..."
        binding.statusText.setTextColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
        )
        binding.instructionText.text = "Writing character data to tag...\nKeep the tag close - do not move it!"
        
        lifecycleScope.launch {
            val result = writer.writeCharacter(tag, character)
            
            when (result) {
                is WriteResult.Success -> {
                    // Clear screen-on flag on success
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    binding.statusText.text = "✓ Successfully written!"
                    binding.statusText.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                    )
                    binding.instructionText.text = "${character.metadata.displayName} has been written to the tag.\nYou can now use it on a Skylanders portal!"
                    binding.progressBar.visibility = View.GONE
                    showSuccess()
                    vibrate()
                }
                is WriteResult.Error -> {
                    // Clear screen-on flag on error
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    binding.progressBar.visibility = View.GONE
                    // Check for common error messages
                    val errorMsg = result.message.lowercase()
                    val userFriendlyMessage = when {
                        errorMsg.contains("out of date") || errorMsg.contains("tag is out of date") -> {
                            "Tag connection lost. Please keep the tag close to your device and try again."
                        }
                        errorMsg.contains("ioexception") || errorMsg.contains("connection") -> {
                            "Tag connection failed. Please keep the tag close and try again."
                        }
                        else -> result.message
                    }
                    binding.statusText.text = "✗ Write failed"
                    binding.statusText.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    )
                    binding.instructionText.text = userFriendlyMessage
                    binding.writeButton.visibility = View.VISIBLE
                    binding.writeButton.isEnabled = true
                    binding.writeButton.text = "Try Again"
                    showError(userFriendlyMessage)
                }
                is WriteResult.AuthenticationFailed -> {
                    // Clear screen-on flag on error
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = "✗ Authentication failed"
                    binding.statusText.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    )
                    binding.instructionText.text = "Could not authenticate with tag. The tag may be locked or use custom keys."
                    binding.writeButton.visibility = View.VISIBLE
                    binding.writeButton.isEnabled = true
                    binding.writeButton.text = "Try Again"
                    showError(getString(R.string.authentication_failed))
                }
                is WriteResult.TagNotSupported -> {
                    // Clear screen-on flag on error
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = "✗ Tag not supported"
                    binding.statusText.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    )
                    binding.instructionText.text = "This tag type is not supported. Please use a Mifare Classic 1K tag."
                    binding.writeButton.visibility = View.VISIBLE
                    binding.writeButton.isEnabled = true
                    showError(getString(R.string.tag_not_supported))
                }
                is WriteResult.WriteFailed -> {
                    // Clear screen-on flag on error
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = "✗ Write failed"
                    binding.statusText.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    )
                    binding.instructionText.text = "Write failed. Please keep the tag close and try again."
                    binding.writeButton.visibility = View.VISIBLE
                    binding.writeButton.isEnabled = true
                    binding.writeButton.text = "Try Again"
                    showError("Write failed. Please keep the tag close and try again.")
                }
                else -> {
                    // Clear screen-on flag on error
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = "✗ Error"
                    binding.statusText.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    )
                    binding.instructionText.text = getString(R.string.write_error)
                    binding.writeButton.visibility = View.VISIBLE
                    binding.writeButton.isEnabled = true
                    binding.writeButton.text = "Try Again"
                    showError(getString(R.string.write_error))
                }
            }
        }
    }
    
    private fun updateUI(state: WriteState) {
        when (state) {
            WriteState.Ready -> {
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = ""
                binding.instructionText.text = getString(R.string.tap_write_button)
                binding.writeButton.isEnabled = true
                binding.writeButton.text = getString(R.string.write_to_tag)
            }
            WriteState.WaitingForTag -> {
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = ""
                binding.instructionText.text = getString(R.string.tap_nfc_tag)
                binding.writeButton.isEnabled = false
            }
            WriteState.TagDetected -> {
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = getString(R.string.tag_detected)
                binding.instructionText.text = getString(R.string.tap_write_to_continue)
                binding.writeButton.isEnabled = true
            }
            WriteState.Checking -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusText.text = "Checking tag compatibility..."
                binding.statusText.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                )
                binding.instructionText.text = "Please wait while we check the tag..."
                binding.writeButton.isEnabled = false
                binding.writeButton.visibility = View.VISIBLE
            }
            WriteState.Writing -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusText.text = "Writing to tag..."
                binding.statusText.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
                )
                binding.instructionText.text = "Keep the tag close to your device!\nDo not move it away."
                binding.writeButton.isEnabled = false
                binding.writeButton.visibility = View.GONE
            }
        }
    }
    
    private fun showSuccess() {
        binding.progressBar.visibility = View.GONE
        binding.statusText.text = getString(R.string.write_success)
        binding.statusText.setTextColor(
            resources.getColor(android.R.color.holo_green_dark, null)
        )
        binding.instructionText.text = ""
        binding.writeButton.isEnabled = false
        currentTag = null
        waitingForTag = false
    }
    
    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.statusText.text = message
        binding.statusText.setTextColor(
            resources.getColor(android.R.color.holo_red_dark, null)
        )
        binding.instructionText.text = ""
        binding.writeButton.isEnabled = true
        binding.writeButton.text = getString(R.string.write_to_tag)
        currentTag = null
        waitingForTag = false
    }
    
    private fun vibrate() {
        val vibrator = getSystemService(requireContext(), Vibrator::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(200)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Ensure screen-on flag is cleared
        if (isAdded) {
            requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        _binding = null
    }
    
    private enum class WriteState {
        Ready, WaitingForTag, TagDetected, Checking, Writing
    }
}


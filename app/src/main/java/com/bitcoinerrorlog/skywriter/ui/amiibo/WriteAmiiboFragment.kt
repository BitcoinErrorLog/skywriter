package com.bitcoinerrorlog.skywriter.ui.amiibo

import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bitcoinerrorlog.skywriter.MainActivity
import com.bitcoinerrorlog.skywriter.R
import com.bitcoinerrorlog.skywriter.data.AmiiboModel
import com.bitcoinerrorlog.skywriter.databinding.FragmentWriteAmiiboBinding
import com.bitcoinerrorlog.skywriter.nfc.NTAG215Writer
import com.bitcoinerrorlog.skywriter.nfc.NFCManager
import com.bitcoinerrorlog.skywriter.nfc.CompatibilityResult
import com.bitcoinerrorlog.skywriter.nfc.NTAG215CompatibilityChecker
import com.bitcoinerrorlog.skywriter.nfc.TagCompatibilityInfo
import com.bitcoinerrorlog.skywriter.nfc.WriteResult
import kotlinx.coroutines.launch

class WriteAmiiboFragment : Fragment(), MainActivity.OnNfcTagDetectedListener {
    
    private var _binding: FragmentWriteAmiiboBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var nfcManager: NFCManager
    private val writer = NTAG215Writer()
    private val compatibilityChecker = NTAG215CompatibilityChecker()
    private var amiibo: AmiiboModel? = null
    private var waitingForTag = false
    private var currentTag: android.nfc.Tag? = null
    private var pendingWriteAction: (() -> Unit)? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            amiibo = it.getParcelable("amiibo")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWriteAmiiboBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        nfcManager = NFCManager(requireActivity())
        
        if (!nfcManager.isNFCAvailable) {
            showError(getString(R.string.nfc_not_available))
            return
        }
        
        amiibo?.let { amiibo ->
            binding.amiiboName.text = amiibo.metadata.displayName
            binding.amiiboGame.text = amiibo.metadata.gameSeriesDisplay
        }
        
        binding.writeButton.setOnClickListener {
            if (currentTag != null) {
                // Tag already detected, write immediately
                amiibo?.let { amiibo ->
                    writeToTag(currentTag!!, amiibo)
                } ?: run {
                    showError("No Amiibo selected")
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
        // Always update current tag to get fresh connection
        currentTag = tag
        
        // If we have a pending write action (from compatibility warning), execute it immediately
        if (pendingWriteAction != null && waitingForTag) {
            waitingForTag = false
            binding.statusText.text = "Tag detected! Starting write..."
            binding.instructionText.text = "Keep the tag close during writing..."
            binding.progressBar.visibility = View.VISIBLE
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            pendingWriteAction?.invoke()
            pendingWriteAction = null
            return
        }
        
        // If user tapped write button and is waiting, proceed
        if (waitingForTag) {
            checkCompatibilityThenWrite(tag)
        } else {
            // Just store the tag, wait for user to tap write button
            updateUI(WriteState.TagDetected)
        }
    }
    
    private fun checkCompatibilityThenWrite(tag: android.nfc.Tag) {
        waitingForTag = false
        updateUI(WriteState.Checking)
        
        // Keep screen on during operations
        requireActivity().window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        lifecycleScope.launch {
            val compatibilityInfo = compatibilityChecker.checkCompatibility(tag)
            
            when (val result = compatibilityInfo.result) {
                is CompatibilityResult.Compatible -> {
                    // Tag is compatible, proceed with write
                    amiibo?.let { amiibo ->
                        writeToTag(tag, amiibo)
                    }
                }
                is CompatibilityResult.Warning -> {
                    // Show warning but allow write
                    // Store amiibo for later write (will use newly detected tag)
                    val amiiboToWrite = amiibo
                    showCompatibilityWarning(compatibilityInfo, result) {
                        // This will be called after tag is re-detected
                        amiiboToWrite?.let { amiibo ->
                            currentTag?.let { freshTag ->
                                writeToTag(freshTag, amiibo)
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
        binding.instructionText.text = getString(R.string.compatibility_warning_continue)
        
        // Update write button to continue - but require tag to be re-detected
        binding.writeButton.text = getString(R.string.write_anyway)
        binding.writeButton.setOnClickListener {
            // Write immediately using current tag if available
            if (currentTag != null) {
                // Tag is still present, write immediately
                onContinue()
            } else {
                // Tag was lost, wait for re-detection
                waitingForTag = true
                updateUI(WriteState.WaitingForTag)
                binding.instructionText.text = getString(R.string.keep_tag_close_and_tap_again)
                // Store the continue action to call after tag is re-detected
                pendingWriteAction = onContinue
            }
        }
    }
    
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
    
    private fun writeToTag(tag: android.nfc.Tag, amiibo: AmiiboModel) {
        // Verify tag is still valid before writing
        if (tag.id == null || tag.id.isEmpty()) {
            showError("Tag is no longer valid. Please keep the tag close and try again.")
            currentTag = null
            waitingForTag = false
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            return
        }
        
        // Reset waiting state since we're writing now
        waitingForTag = false
        pendingWriteAction = null
        
        // Keep screen on during write operation
        requireActivity().window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        updateUI(WriteState.Writing)
        binding.instructionText.text = getString(R.string.keep_tag_close_during_write)
        
        lifecycleScope.launch {
            val result = writer.writeAmiibo(tag, amiibo)
            
            when (result) {
                is WriteResult.Success -> {
                    // Clear screen-on flag on success
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    showSuccess()
                    vibrate()
                }
                is WriteResult.Error -> {
                    // Clear screen-on flag on error
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
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
                    showError(userFriendlyMessage)
                }
                is WriteResult.AuthenticationFailed -> {
                    // Clear screen-on flag on error
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    showError(getString(R.string.authentication_failed))
                }
                is WriteResult.TagNotSupported -> {
                    // Clear screen-on flag on error
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    showError(getString(R.string.tag_not_supported_amiibo))
                }
                is WriteResult.WriteFailed -> {
                    // Clear screen-on flag on error
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    showError("Write failed. Please keep the tag close and try again.")
                }
                else -> {
                    // Clear screen-on flag on error
                    if (isAdded) {
                        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
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
                binding.writeButton.text = getString(R.string.write_amiibo)
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
                binding.statusText.text = getString(R.string.checking_compatibility)
                binding.instructionText.text = getString(R.string.checking_compatibility)
                binding.writeButton.isEnabled = false
            }
            WriteState.Writing -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusText.text = getString(R.string.writing)
                binding.instructionText.text = getString(R.string.writing)
                binding.writeButton.isEnabled = false
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
        binding.writeButton.text = getString(R.string.write_amiibo)
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


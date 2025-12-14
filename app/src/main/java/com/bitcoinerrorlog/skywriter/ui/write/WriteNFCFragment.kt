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
                // Tag already detected, check and write
                checkCompatibilityThenWrite(currentTag!!)
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
        updateUI(WriteState.Ready)
    }
    
    override fun onResume() {
        super.onResume()
        nfcManager.enableForegroundDispatch()
    }
    
    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch()
    }
    
    override fun onNfcTagDetected(intent: Intent) {
        val tag = nfcManager.getTagFromIntent(intent) ?: return
        currentTag = tag
        
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
                    showCompatibilityWarning(compatibilityInfo, result) {
                        character?.let { char ->
                            writeToTag(tag, char)
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
        
        // Update write button to continue
        binding.writeButton.text = getString(R.string.write_anyway)
        binding.writeButton.setOnClickListener {
            onContinue()
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
    
    private fun writeToTag(tag: android.nfc.Tag, character: CharacterModel) {
        updateUI(WriteState.Writing)
        
        lifecycleScope.launch {
            val result = writer.writeCharacter(tag, character)
            
            when (result) {
                is WriteResult.Success -> {
                    showSuccess()
                    vibrate()
                }
                is WriteResult.Error -> {
                    showError(result.message)
                }
                is WriteResult.AuthenticationFailed -> {
                    showError(getString(R.string.authentication_failed))
                }
                is WriteResult.TagNotSupported -> {
                    showError(getString(R.string.tag_not_supported))
                }
                else -> {
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
        _binding = null
    }
    
    private enum class WriteState {
        Ready, WaitingForTag, TagDetected, Checking, Writing
    }
}


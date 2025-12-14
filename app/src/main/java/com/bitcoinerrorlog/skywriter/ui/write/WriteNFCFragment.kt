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
    private var compatibilityChecked = false
    
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
        
        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // Reset compatibility check when fragment is created
        compatibilityChecked = false
        updateUI(WriteState.Waiting)
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
        
        if (!nfcManager.isMifareClassicTag(tag)) {
            showError(getString(R.string.tag_not_supported))
            return
        }
        
        // Check compatibility before writing
        if (!compatibilityChecked) {
            checkCompatibilityThenWrite(tag)
        } else {
            character?.let { char ->
                writeToTag(tag, char)
            }
        }
    }
    
    private fun checkCompatibilityThenWrite(tag: android.nfc.Tag) {
        updateUI(WriteState.Checking)
        
        lifecycleScope.launch {
            val compatibilityInfo = compatibilityChecker.checkCompatibility(tag)
            
            when (val result = compatibilityInfo.result) {
                is CompatibilityResult.Compatible -> {
                    // Tag is compatible, proceed with write
                    compatibilityChecked = true
                    character?.let { char ->
                        writeToTag(tag, char)
                    }
                }
                is CompatibilityResult.Warning -> {
                    // Show warning but allow write
                    showCompatibilityWarning(compatibilityInfo, result)
                    compatibilityChecked = true
                }
                is CompatibilityResult.Incompatible -> {
                    // Tag is incompatible, show error
                    showCompatibilityError(compatibilityInfo, result)
                }
            }
        }
    }
    
    private fun showCompatibilityWarning(@Suppress("UNUSED_PARAMETER") info: TagCompatibilityInfo, warning: CompatibilityResult.Warning) {
        val message = buildString {
            append(warning.message)
            if (warning.details.isNotEmpty()) {
                append("\n\n")
                append(warning.details.joinToString("\n"))
            }
            append("\n\n")
            append(getString(R.string.compatibility_warning_continue))
        }
        
        binding.statusText.text = message
        binding.statusText.setTextColor(
            resources.getColor(android.R.color.holo_orange_dark, null)
        )
        binding.instructionText.text = getString(R.string.tap_again_to_write)
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
            WriteState.Waiting -> {
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = ""
                binding.instructionText.text = getString(R.string.tap_nfc_tag)
            }
            WriteState.Checking -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusText.text = getString(R.string.checking_compatibility)
                binding.instructionText.text = getString(R.string.checking_compatibility)
            }
            WriteState.Writing -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusText.text = getString(R.string.writing)
                binding.instructionText.text = getString(R.string.writing)
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
    }
    
    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.statusText.text = message
        binding.statusText.setTextColor(
            resources.getColor(android.R.color.holo_red_dark, null)
        )
        binding.instructionText.text = ""
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
        Waiting, Checking, Writing
    }
}


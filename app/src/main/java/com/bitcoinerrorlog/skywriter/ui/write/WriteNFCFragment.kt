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
import com.bitcoinerrorlog.skywriter.nfc.WriteResult
import kotlinx.coroutines.launch

class WriteNFCFragment : Fragment(), MainActivity.OnNfcTagDetectedListener {
    
    private var _binding: FragmentWriteNfcBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var nfcManager: NFCManager
    private val writer = MifareClassicWriter()
    private var character: CharacterModel? = null
    
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
        
        character?.let { char ->
            writeToTag(tag, char)
        }
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
        Waiting, Writing
    }
}


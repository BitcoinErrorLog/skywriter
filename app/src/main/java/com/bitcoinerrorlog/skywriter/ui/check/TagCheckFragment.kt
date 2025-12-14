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
import com.bitcoinerrorlog.skywriter.nfc.CompatibilityResult
import com.bitcoinerrorlog.skywriter.nfc.NFCManager
import com.bitcoinerrorlog.skywriter.nfc.TagCompatibilityChecker
import com.bitcoinerrorlog.skywriter.nfc.TagCompatibilityInfo
import kotlinx.coroutines.launch

class TagCheckFragment : Fragment(), MainActivity.OnNfcTagDetectedListener {
    
    private var _binding: FragmentTagCheckBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var nfcManager: NFCManager
    private val compatibilityChecker = TagCompatibilityChecker()
    private var lastCompatibilityInfo: TagCompatibilityInfo? = null
    
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
        
        binding.checkAgainButton.setOnClickListener {
            lastCompatibilityInfo = null
            updateUI(CheckState.Waiting)
        }
        
        updateUI(CheckState.Waiting)
    }
    
    override fun onResume() {
        super.onResume()
        nfcManager.enableForegroundDispatch()
        
        // Check if we have a pending NFC intent
        (requireActivity() as? MainActivity)?.let { activity ->
            activity.intent?.let { intent ->
                if (intent.action == android.nfc.NfcAdapter.ACTION_TECH_DISCOVERED || 
                    intent.action == android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED) {
                    onNfcTagDetected(intent)
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch()
    }
    
    override fun onNfcTagDetected(intent: Intent) {
        val tag = nfcManager.getTagFromIntent(intent) ?: return
        checkTag(tag)
    }
    
    private fun checkTag(tag: android.nfc.Tag) {
        updateUI(CheckState.Checking)
        
        lifecycleScope.launch {
            val compatibilityInfo = compatibilityChecker.checkCompatibility(tag)
            lastCompatibilityInfo = compatibilityInfo
            displayCompatibilityInfo(compatibilityInfo)
        }
    }
    
    private fun displayCompatibilityInfo(info: TagCompatibilityInfo) {
        updateUI(CheckState.Complete)
        
        // Set overall status
        when (val result = info.result) {
            is CompatibilityResult.Compatible -> {
                binding.statusIcon.setImageResource(android.R.drawable.ic_dialog_info)
                binding.statusText.text = getString(R.string.tag_compatible)
                binding.statusText.setTextColor(
                    resources.getColor(android.R.color.holo_green_dark, null)
                )
            }
            is CompatibilityResult.Warning -> {
                binding.statusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                binding.statusText.text = result.message
                binding.statusText.setTextColor(
                    resources.getColor(android.R.color.holo_orange_dark, null)
                )
            }
            is CompatibilityResult.Incompatible -> {
                binding.statusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                binding.statusText.text = result.reason
                binding.statusText.setTextColor(
                    resources.getColor(android.R.color.holo_red_dark, null)
                )
            }
        }
        
        // Display tag details
        val details = buildString {
            append("Tag Type: ${info.tagType}\n")
            append("Block Count: ${info.blockCount}\n")
            append("Sector Count: ${info.sectorCount}\n")
            append("UID: ${info.uid ?: "Unknown"}\n")
            append("\n")
            append("Capabilities:\n")
            append("• Authentication: ${if (info.authenticationTest) "✓" else "✗"}\n")
            append("• Readable: ${if (info.readable) "✓" else "✗"}\n")
            append("• Writable: ${if (info.writable) "✓" else "✗"}\n")
            info.uidChangeable?.let {
                append("• UID Changeable: ${if (it) "✓" else "✗ (Locked)"}\n")
            }
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
    }
    
    private fun updateUI(state: CheckState) {
        when (state) {
            CheckState.Waiting -> {
                binding.progressBar.visibility = View.GONE
                binding.statusIcon.visibility = View.VISIBLE
                binding.statusText.text = getString(R.string.tap_tag_to_check)
                binding.statusText.setTextColor(
                    resources.getColor(android.R.color.darker_gray, null)
                )
                binding.detailsText.text = ""
                binding.issuesTitle.visibility = View.GONE
                binding.issuesText.visibility = View.GONE
                binding.recommendationsTitle.visibility = View.GONE
                binding.recommendationsText.visibility = View.GONE
                binding.checkAgainButton.visibility = View.GONE
            }
            CheckState.Checking -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusIcon.visibility = View.GONE
                binding.statusText.text = getString(R.string.checking_compatibility)
            }
            CheckState.Complete -> {
                binding.progressBar.visibility = View.GONE
                binding.statusIcon.visibility = View.VISIBLE
            }
        }
    }
    
    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.statusText.text = message
        binding.statusText.setTextColor(
            resources.getColor(android.R.color.holo_red_dark, null)
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private enum class CheckState {
        Waiting, Checking, Complete
    }
}


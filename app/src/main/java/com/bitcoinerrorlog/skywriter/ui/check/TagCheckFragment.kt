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
    private var waitingForTag = false
    private var currentTag: android.nfc.Tag? = null
    
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
            waitingForTag = false
            updateUI(CheckState.Ready)
        }
        
        updateUI(CheckState.Ready)
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
        
        // If user tapped check button and is waiting, proceed
        if (waitingForTag) {
            checkTag(tag)
        } else {
            // Just store the tag, wait for user to tap check button
            updateUI(CheckState.TagDetected)
        }
    }
    
    private fun checkTag(tag: android.nfc.Tag) {
        waitingForTag = false
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
            CheckState.Ready -> {
                binding.progressBar.visibility = View.GONE
                binding.statusIcon.visibility = View.VISIBLE
                binding.statusText.text = getString(R.string.tap_check_button)
                binding.statusText.setTextColor(
                    resources.getColor(android.R.color.darker_gray, null)
                )
                binding.detailsText.text = ""
                binding.issuesTitle.visibility = View.GONE
                binding.issuesText.visibility = View.GONE
                binding.recommendationsTitle.visibility = View.GONE
                binding.recommendationsText.visibility = View.GONE
                binding.checkButton.isEnabled = true
                binding.checkButton.visibility = View.VISIBLE
                binding.checkAgainButton.visibility = View.GONE
            }
            CheckState.WaitingForTag -> {
                binding.progressBar.visibility = View.GONE
                binding.statusIcon.visibility = View.VISIBLE
                binding.statusText.text = getString(R.string.tap_tag_to_check)
                binding.statusText.setTextColor(
                    resources.getColor(android.R.color.darker_gray, null)
                )
                binding.checkButton.isEnabled = false
            }
            CheckState.TagDetected -> {
                binding.progressBar.visibility = View.GONE
                binding.statusIcon.visibility = View.VISIBLE
                binding.statusText.text = getString(R.string.tag_detected)
                binding.statusText.setTextColor(
                    resources.getColor(android.R.color.holo_blue_dark, null)
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
        Ready, WaitingForTag, TagDetected, Checking, Complete
    }
}


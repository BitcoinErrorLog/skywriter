package com.bitcoinerrorlog.skywriter.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic

class NFCManager(private val activity: Activity) {
    
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val pendingIntent: PendingIntent = PendingIntent.getActivity(
        activity,
        0,
        Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_MUTABLE
    )
    
    val isNFCAvailable: Boolean
        get() = nfcAdapter != null && nfcAdapter.isEnabled
    
    fun enableForegroundDispatch() {
        // Listen for all NFC tag types, not just Mifare Classic
        val intentFilters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        )
        // Tech lists: null means we accept all technologies
        nfcAdapter?.enableForegroundDispatch(
            activity,
            pendingIntent,
            intentFilters,
            null // No tech lists - we want all tags
        )
    }
    
    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }
    
    fun getTagFromIntent(intent: Intent): Tag? {
        return intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
    }
    
    fun isMifareClassicTag(tag: Tag?): Boolean {
        return tag?.let {
            val techList = it.techList
            techList.any { tech -> tech.contains("MifareClassic") }
        } ?: false
    }
}


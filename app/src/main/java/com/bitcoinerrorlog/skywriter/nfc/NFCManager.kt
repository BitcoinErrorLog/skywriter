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
        nfcAdapter?.enableForegroundDispatch(
            activity,
            pendingIntent,
            arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)),
            arrayOf(arrayOf(MifareClassic::class.java.name))
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


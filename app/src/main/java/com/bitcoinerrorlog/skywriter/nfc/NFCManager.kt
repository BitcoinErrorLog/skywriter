package com.bitcoinerrorlog.skywriter.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log

/**
 * Manages NFC adapter and reader mode for stable tag detection.
 */
class NFCManager(private val activity: Activity) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private var tagListener: ((Tag) -> Unit)? = null

    val isNFCAvailable: Boolean
        get() = nfcAdapter != null && nfcAdapter.isEnabled

    /**
     * Enables reader mode for stable, exclusive access to NFC tags.
     * This is more robust than foreground dispatch and avoids "tag out of date" errors.
     */
    fun enableReaderMode(callback: (Tag) -> Unit) {
        if (nfcAdapter == null || !nfcAdapter.isEnabled) return
        
        tagListener = callback
        
        val options = Bundle()
        // Reduce presence check delay to detect removal faster
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)

        nfcAdapter.enableReaderMode(
            activity,
            { tag ->
                Log.d("NFCManager", "Tag detected via ReaderMode: ${tag.id.joinToString("") { "%02X".format(it) }}")
                activity.runOnUiThread {
                    tagListener?.invoke(tag)
                }
            },
            NfcAdapter.FLAG_READER_NFC_A or 
            NfcAdapter.FLAG_READER_NFC_B or 
            NfcAdapter.FLAG_READER_NFC_F or 
            NfcAdapter.FLAG_READER_NFC_V or 
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            options
        )
    }

    /**
     * Disables reader mode.
     */
    fun disableReaderMode() {
        nfcAdapter?.disableReaderMode(activity)
        tagListener = null
    }

    fun getTagFromIntent(intent: android.content.Intent): Tag? {
        return intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
    }
}

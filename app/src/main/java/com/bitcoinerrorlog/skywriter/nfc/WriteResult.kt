package com.bitcoinerrorlog.skywriter.nfc

sealed class WriteResult {
    data object Success : WriteResult()
    data class Error(val message: String) : WriteResult()
    data object NFCNotAvailable : WriteResult()
    data object TagNotSupported : WriteResult()
    data object AuthenticationFailed : WriteResult()
    data object WriteFailed : WriteResult()
}


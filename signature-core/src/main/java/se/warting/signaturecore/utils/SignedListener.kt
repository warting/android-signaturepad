package se.warting.signaturecore.utils

interface SignedListener {
    fun onStartSigning()
    fun onSigning()
    fun onSigned()
    fun onClear()
}

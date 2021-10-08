package se.warting.signatureview.views

interface SignedListener {
    fun onStartSigning()
    fun onSigned()
    fun onClear()
}

package se.warting.signatureview.views

interface SignedListener {
    fun onStartSigning()
    fun onSigning()
    fun onSigned()
    fun onClear()
}

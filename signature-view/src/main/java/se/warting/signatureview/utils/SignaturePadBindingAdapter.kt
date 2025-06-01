package se.warting.signatureview.utils

import androidx.databinding.BindingAdapter
import se.warting.signaturecore.utils.SignedListener
import se.warting.signatureview.views.SignaturePad

object SignaturePadBindingAdapter {

    @BindingAdapter("onStartSigning")
    fun setOnSignedListener(view: SignaturePad, onStartSigningListener: OnStartSigningListener?) {
        setOnSignedListener(view, onStartSigningListener, null, null, null)
    }

    @BindingAdapter("onSigning")
    fun setOnSigningListener(view: SignaturePad, onSigningListener: OnSigningListener?) {
        setOnSignedListener(view, null, onSigningListener, null, null)
    }

    @BindingAdapter("onSigned")
    fun setOnSignedListener(view: SignaturePad, onSignedListener: OnSignedListener?) {
        setOnSignedListener(view, null, null, onSignedListener, null)
    }

    @BindingAdapter("onClear")
    fun setOnSignedListener(view: SignaturePad, onClearListener: OnClearListener?) {
        setOnSignedListener(view, null, null, null, onClearListener)
    }

    @BindingAdapter(value = ["onStartSigning", "onSigning", "onSigned", "onClear"], requireAll = false)
    fun setOnSignedListener(
        view: SignaturePad,
        onStartSigningListener: OnStartSigningListener?,
        onSigningListener: OnSigningListener?,
        onSignedListener: OnSignedListener?,
        onClearListener: OnClearListener?
    ) {
        view.setOnSignedListener(object :
            SignedListener {
            override fun onStartSigning() {
                onStartSigningListener?.onStartSigning()
            }

            override fun onSigning() {
                onSigningListener?.onSigning()
            }

            override fun onSigned() {
                onSignedListener?.onSigned()
            }

            override fun onClear() {
                onClearListener?.onClear()
            }
        })
    }

    interface OnStartSigningListener {
        fun onStartSigning()
    }

    interface OnSigningListener {
        fun onSigning()
    }

    interface OnSignedListener {
        fun onSigned()
    }

    interface OnClearListener {
        fun onClear()
    }
}

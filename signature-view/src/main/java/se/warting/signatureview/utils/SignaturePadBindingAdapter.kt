package se.warting.signatureview.utils

import androidx.databinding.BindingAdapter
import se.warting.signatureview.views.SignaturePad
import se.warting.signatureview.views.SignedListener

object SignaturePadBindingAdapter {
    @BindingAdapter("onStartSigning")
    fun setOnSignedListener(view: SignaturePad, onStartSigningListener: OnStartSigningListener?) {
        setOnSignedListener(view, onStartSigningListener, null, null)
    }

    @BindingAdapter("onSigned")
    fun setOnSignedListener(view: SignaturePad, onSignedListener: OnSignedListener?) {
        setOnSignedListener(view, null, onSignedListener, null)
    }

    @BindingAdapter("onClear")
    fun setOnSignedListener(view: SignaturePad, onClearListener: OnClearListener?) {
        setOnSignedListener(view, null, null, onClearListener)
    }

    @BindingAdapter(value = ["onStartSigning", "onSigned", "onClear"], requireAll = false)
    fun setOnSignedListener(
        view: SignaturePad,
        onStartSigningListener: OnStartSigningListener?,
        onSignedListener: OnSignedListener?,
        onClearListener: OnClearListener?
    ) {
        view.setOnSignedListener(object :
            SignedListener {
            override fun onStartSigning() {
                onStartSigningListener?.onStartSigning()
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

    interface OnSignedListener {
        fun onSigned()
    }

    interface OnClearListener {
        fun onClear()
    }
}

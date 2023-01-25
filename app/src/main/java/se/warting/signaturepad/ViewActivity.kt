package se.warting.signaturepad

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import se.warting.signatureview.views.SignaturePad
import se.warting.signatureview.views.SignedListener

class ViewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_view)
        val mSaveButton = findViewById<View>(R.id.save_button) as Button
        val mClearButton = findViewById<View>(R.id.clear_button) as Button
        val mSignaturePad = findViewById<View>(R.id.signature_pad) as SignaturePad
        mSignaturePad.setOnSignedListener(object : SignedListener {

            override fun onStartSigning() {
                Log.d("SignedListener", "OnStartSigning")
            }

            override fun onSigning() {
                Log.d("SignedListener", "OnSigning")
            }

            override fun onSigned() {
                Log.d("SignedListener", "OnSigned")
                mSaveButton.isEnabled = !mSignaturePad.isEmpty
                mClearButton.isEnabled = !mSignaturePad.isEmpty
            }

            override fun onClear() {

                Toast.makeText(this@ViewActivity, "OnClear", Toast.LENGTH_SHORT)
                    .show()

                mSaveButton.isEnabled = !mSignaturePad.isEmpty
                mClearButton.isEnabled = !mSignaturePad.isEmpty
            }
        })
        mClearButton.setOnClickListener { mSignaturePad.clear() }
        mSaveButton.setOnClickListener {
            val signatureBitmap = mSignaturePad.getSignatureBitmap()
            val signatureSvg = mSignaturePad.getSignatureSvg()
            val transparentSignatureBitmap = mSignaturePad.getTransparentSignatureBitmap()
            if (BuildConfig.DEBUG) {
                Log.d("ViewActivity", "Bitmap size: " + signatureBitmap.byteCount)
                Log.d(
                    "ViewActivity",
                    "Bitmap trasparent size: " + transparentSignatureBitmap.byteCount
                )
                Log.d("ViewActivity", "Svg length: " + signatureSvg.length)
            }
        }
    }
}

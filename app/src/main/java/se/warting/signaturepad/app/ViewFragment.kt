package se.warting.signaturepad.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import se.warting.signaturecore.utils.SignedListener
import se.warting.signatureview.views.SignaturePad

class ViewFragment : Fragment() {

    private lateinit var mSaveButton: Button
    private lateinit var mClearButton: Button
    private lateinit var mUndoButton: Button
    private lateinit var mSignaturePad: SignaturePad

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val root = view.findViewById<View>(R.id.view_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }

        mSaveButton = view.findViewById<View>(R.id.save_button) as Button
        mClearButton = view.findViewById<View>(R.id.clear_button) as Button
        mUndoButton = view.findViewById<View>(R.id.undo_button) as Button
        mSignaturePad = view.findViewById<View>(R.id.signature_pad) as SignaturePad

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
                mUndoButton.isEnabled = mSignaturePad.canUndo()
            }

            override fun onClear() {
                context?.let {
                    Toast.makeText(it, "OnClear", Toast.LENGTH_SHORT).show()
                }

                mSaveButton.isEnabled = !mSignaturePad.isEmpty
                mClearButton.isEnabled = !mSignaturePad.isEmpty
                mUndoButton.isEnabled = mSignaturePad.canUndo()
            }
        })

        mUndoButton.setOnClickListener { 
            mSignaturePad.undo()
            mUndoButton.isEnabled = mSignaturePad.canUndo()
            mSaveButton.isEnabled = !mSignaturePad.isEmpty
            mClearButton.isEnabled = !mSignaturePad.isEmpty
        }
        mClearButton.setOnClickListener { mSignaturePad.clear() }
        mSaveButton.setOnClickListener {
            val signatureBitmap = mSignaturePad.getSignatureBitmap()
            val signatureSvg = mSignaturePad.getSignatureSvg()
            val transparentSignatureBitmap = mSignaturePad.getTransparentSignatureBitmap()
            if (BuildConfig.DEBUG) {
                Log.d("ViewFragment", "Bitmap size: " + signatureBitmap.byteCount)
                Log.d(
                    "ViewFragment",
                    "Bitmap trasparent size: " + transparentSignatureBitmap.byteCount
                )
                Log.d("ViewFragment", "Svg length: " + signatureSvg.length)
            }
        }
    }
}

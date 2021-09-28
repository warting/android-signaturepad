/*
 * MIT License
 *
 * Copyright (c) 2021. Stefan Wärting
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package se.warting.signaturepad

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import se.warting.signaturepad.databinding.ActivityDatabindBinding
import se.warting.signatureview.views.SignedListener

class DataBindingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The layout for this activity is a Data Binding layout so it needs to be inflated using
        // DataBindingUtil.
        val binding: ActivityDatabindBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_databind
        )
        val onStartSigning: SignedListener = object : SignedListener {
            override fun onStartSigning() {
                Toast.makeText(this@DataBindingActivity, "OnStartSigning", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onSigned() {
                binding.saveButton.isEnabled = true
                binding.clearButton.isEnabled = true
            }

            override fun onClear() {
                binding.saveButton.isEnabled = false
                binding.clearButton.isEnabled = false
            }
        }
        binding.binding = onStartSigning
        binding.clearButton.setOnClickListener { binding.signaturePad.clear() }
        binding.saveButton.setOnClickListener {
            val signatureBitmap = binding.signaturePad.getSignatureBitmap()
            val signatureSvg = binding.signaturePad.getSignatureSvg()
            val transparentSignatureBitmap = binding.signaturePad.getTransparentSignatureBitmap()

            if (BuildConfig.DEBUG) {
                Log.d("DataBindingActivity", "Bitmap size: " + signatureBitmap.byteCount)
                Log.d(
                    "DataBindingActivity",
                    "Bitmap trasparent size: " + transparentSignatureBitmap.byteCount
                )
                Log.d("DataBindingActivity", "Svg length: " + signatureSvg.length)
            }
        }
    }
}

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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import se.warting.signatureview.views.SignaturePad
import se.warting.signatureview.views.SignedListener

class ViewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verifyStoragePermissions(this)
        setContentView(R.layout.activity_view)
        val mSaveButton = findViewById<View>(R.id.save_button) as Button
        val mClearButton = findViewById<View>(R.id.clear_button) as Button
        val mSignaturePad = findViewById<View>(R.id.signature_pad) as SignaturePad
        mSignaturePad.setOnSignedListener(object : SignedListener {
            override fun onStartSigning() {
                Toast.makeText(this@ViewActivity, "OnStartSigning", Toast.LENGTH_SHORT).show()
            }

            override fun onSigned() {
                mSaveButton.isEnabled = true
                mClearButton.isEnabled = true
            }

            override fun onClear() {
                mSaveButton.isEnabled = false
                mClearButton.isEnabled = false
            }
        })
        mClearButton.setOnClickListener { mSignaturePad.clear() }
        mSaveButton.setOnClickListener {
            val signatureBitmap = mSignaturePad.signatureBitmap
            if (addJpgSignatureToGallery(signatureBitmap)) {
                Toast.makeText(
                    this@ViewActivity,
                    "Signature saved into the Gallery",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@ViewActivity,
                    "Unable to store the signature",
                    Toast.LENGTH_SHORT
                ).show()
            }
            if (addSvgSignatureToGallery(mSignaturePad.signatureSvg)) {
                Toast.makeText(
                    this@ViewActivity,
                    "SVG Signature saved into the Gallery",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@ViewActivity,
                    "Unable to store the SVG signature",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_EXTERNAL_STORAGE -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size <= 0 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(
                        this@ViewActivity,
                        "Cannot write images to external storage",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun getAlbumStorageDir(albumName: String): File {
        // Get the directory for the user's public pictures directory.
        val file = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), albumName
        )
        if (!file.mkdirs()) {
            Log.e("SignaturePad", "Directory not created")
        }
        return file
    }

    @SuppressWarnings("MagicNumber")
    @Throws(IOException::class)
    fun saveBitmapToJPG(bitmap: Bitmap, photo: File) {
        val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val stream: OutputStream = FileOutputStream(photo)
        newBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        stream.close()
    }

    private fun addJpgSignatureToGallery(signature: Bitmap): Boolean {
        var result = false

        val photo = File(
            getAlbumStorageDir("SignaturePad"),
            "Signature_" + System.currentTimeMillis() + ".jpg"
        )
        saveBitmapToJPG(signature, photo)
        scanMediaFile(photo)
        result = true

        return result
    }

    private fun scanMediaFile(photo: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val contentUri = Uri.fromFile(photo)
        mediaScanIntent.data = contentUri
        this@ViewActivity.sendBroadcast(mediaScanIntent)
    }

    private fun addSvgSignatureToGallery(signatureSvg: String): Boolean {
        var result = false

        val svgFile = File(
            getAlbumStorageDir("SignaturePad"),
            "Signature_" + System.currentTimeMillis() + ".svg"
        )
        val stream: OutputStream = FileOutputStream(svgFile)
        val writer = OutputStreamWriter(stream)
        writer.write(signatureSvg)
        writer.close()
        stream.flush()
        stream.close()
        scanMediaFile(svgFile)
        result = true

        return result
    }

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        /**
         * Checks if the app has permission to write to device storage
         *
         *
         * If the app does not has permission then the user will be prompted to grant permissions
         *
         * @param activity the activity from which permissions are checked
         */
        fun verifyStoragePermissions(activity: Activity) {
            // Check if we have write permission
            val permission = ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
                )
            }
        }
    }
}

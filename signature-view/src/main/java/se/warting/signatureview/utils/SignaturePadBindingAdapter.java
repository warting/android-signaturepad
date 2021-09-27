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

package se.warting.signatureview.utils;

import androidx.databinding.BindingAdapter;

import se.warting.signatureview.views.SignaturePad;

public final class SignaturePadBindingAdapter {

    @BindingAdapter("onStartSigning")
    public static void setOnSignedListener(SignaturePad view, final OnStartSigningListener onStartSigningListener) {
        setOnSignedListener(view, onStartSigningListener, null, null);
    }

    @BindingAdapter("onSigned")
    public static void setOnSignedListener(SignaturePad view, final OnSignedListener onSignedListener) {
        setOnSignedListener(view, null, onSignedListener, null);
    }

    @BindingAdapter("onClear")
    public static void setOnSignedListener(SignaturePad view, final OnClearListener onClearListener) {
        setOnSignedListener(view, null, null, onClearListener);
    }

    @BindingAdapter(value = {"onStartSigning", "onSigned", "onClear"}, requireAll = false)
    public static void setOnSignedListener(SignaturePad view, final OnStartSigningListener onStartSigningListener, final OnSignedListener onSignedListener, final OnClearListener onClearListener) {
        view.setOnSignedListener(new SignaturePad.OnSignedListener() {
            @Override
            public void onStartSigning() {
                if (onStartSigningListener != null) {
                    onStartSigningListener.onStartSigning();
                }
            }

            @Override
            public void onSigned() {
                if (onSignedListener != null) {
                    onSignedListener.onSigned();
                }
            }

            @Override
            public void onClear() {
                if (onClearListener != null) {
                    onClearListener.onClear();
                }
            }
        });
    }

    public interface OnStartSigningListener {
        void onStartSigning();
    }

    public interface OnSignedListener {
        void onSigned();
    }

    public interface OnClearListener {
        void onClear();
    }

}

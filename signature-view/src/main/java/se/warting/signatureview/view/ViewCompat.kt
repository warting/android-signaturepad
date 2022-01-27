package se.warting.signatureview.view

import android.view.View

object ViewCompat {
    /**
     * Returns true if `view` has been through at least one layout since it
     * was last attached to or detached from a window.
     *
     * See http://developer.android.com/reference/android/support/v4/view/ViewCompat.html#isLaidOut%28android.view.View%29
     *
     * @param view the view
     * @return true if this view has been through at least one layout
     * since it was last attached to or detached from a window.
     */
    fun isLaidOut(view: View): Boolean {
        return view.isLaidOut
    }
}

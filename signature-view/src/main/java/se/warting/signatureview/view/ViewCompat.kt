package se.warting.signatureview.view

import android.os.Build
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
        // Future (API19+)...
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.isLaidOut
        } else view.width > 0 && view.height > 0
        // Legacy...
    }
}

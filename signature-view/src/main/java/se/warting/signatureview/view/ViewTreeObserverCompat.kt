package se.warting.signatureview.view

import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener

internal object ViewTreeObserverCompat {
    /**
     * Remove a previously installed global layout callback.
     * @param observer the view observer
     * @param victim the victim
     */
    @SuppressLint("NewApi")
    internal fun removeOnGlobalLayoutListener(
        observer: ViewTreeObserver,
        victim: OnGlobalLayoutListener?
    ) {
        // Future (API16+)...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            observer.removeOnGlobalLayoutListener(victim)
        } else {
            observer.removeGlobalOnLayoutListener(victim)
        }
    }
}

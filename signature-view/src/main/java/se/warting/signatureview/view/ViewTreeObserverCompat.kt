package se.warting.signatureview.view

import android.annotation.SuppressLint
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
        observer.removeOnGlobalLayoutListener(victim)
    }
}

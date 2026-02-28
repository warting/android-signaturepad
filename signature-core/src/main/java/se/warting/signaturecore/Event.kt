package se.warting.signaturecore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Event(
    val timestamp: Long,
    val action: Int,
    val x: Float,
    val y: Float,
) : Parcelable {
    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_MOVE = 2
        const val ACTION_UP = 1
    }
}

package se.warting.signaturecore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Event(
    val timestamp: Long,
    val action: Int,
    val x: Float,
    val y: Float,
) : Parcelable

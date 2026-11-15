package se.warting.signatureink

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor

/** Optional Android Ink surface. Existing SignaturePad renderers are unchanged. */
class InkSignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private val renderer = CanvasStrokeRenderer.create()
    private val transform = Matrix()
    private val wet = InProgressStrokesView(context)
    private val predictor = MotionEventPredictor.newInstance(this)
    private var activeId: InProgressStrokeId? = null
    private var pointerId = -1
    private var observedGeneration = 0
    private val samples = linkedMapOf<InProgressStrokeId, PendingInkStroke>()
    private val issued = linkedMapOf<InProgressStrokeId, Int>()
    private val dry = object : View(context) {
        override fun onDraw(canvas: Canvas) {
            state.strokes.forEach { renderer.draw(canvas, it, transform) }
        }
    }
    private val changed: () -> Unit = {
        if (observedGeneration != state.generation) {
            observedGeneration = state.generation
            cancelInput()
            discardSamples()
            issued.clear()
            wet.cancelUnfinishedStrokes()
            wet.removeFinishedStrokes(wet.getFinishedStrokes().keys)
        }
        dry.invalidate()
    }

    var state: InkSignatureState = InkSignatureState()
        set(value) {
            if (field === value) return
            cancelInput()
            commitEndedStrokes()
            field.observers.remove(changed)
            issued.keys.toList().forEach(field::cancel)
            discardSamples()
            issued.clear()
            wet.cancelUnfinishedStrokes()
            wet.removeFinishedStrokes(wet.getFinishedStrokes().keys)
            field = value
            observedGeneration = value.generation
            if (isAttachedToWindow) value.observers.add(changed)
            dry.invalidate()
        }

    /** Pressure-sensitive pen by default. Changes apply to the next stroke. */
    internal val defaultBrush: Brush = Brush.createWithColorIntArgb(
        StockBrushes.pressurePen(), Color.BLACK, 5f * resources.displayMetrics.density, 0.1f
    )
    var brush: Brush = defaultBrush

    init {
        clipToPadding = false
        addView(dry, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(wet, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        wet.addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
            override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                val accepted = linkedMapOf<InProgressStrokeId, Stroke>()
                issued.keys.toList().forEach { id ->
                    val stroke = strokes[id] ?: return@forEach
                    samples.remove(id)?.recycle()
                    if (issued.remove(id) == state.generation) accepted[id] = stroke
                }
                state.finish(accepted)
                dry.invalidate()
                // Both layers update in this UI-thread callback, as required by Ink's handoff.
                wet.removeFinishedStrokes(strokes.keys)
            }
        })
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled && activeId != null) cancelInput()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Touch and export coordinates are wrapper-local, even when XML applies padding.
        val width = MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY)
        val height = MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        dry.measure(width, height)
        wet.measure(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        dry.layout(0, 0, right - left, bottom - top)
        wet.layout(0, 0, right - left, bottom - top)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean = true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            cancelInput()
            return false
        }
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return handleInput(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        state.observers.add(changed)
        changed()
    }

    override fun onDetachedFromWindow() {
        cancelInput()
        // A detached surface cannot complete the asynchronous renderer handoff.
        // Commit ended strokes from retained real input through Ink's public model API.
        commitEndedStrokes()
        state.observers.remove(changed)
        super.onDetachedFromWindow()
    }

    private fun commitEndedStrokes() {
        val ended = linkedMapOf<InProgressStrokeId, Stroke>()
        samples.entries.toList().forEach { (id, input) ->
            if (input.ended && issued[id] == state.generation) ended[id] = input.toStroke()
        }
        ended.keys.forEach { id ->
            issued.remove(id)
            samples.remove(id)?.recycle()
        }
        if (ended.isNotEmpty()) state.finish(ended)
    }

    private fun discardSamples() {
        samples.values.forEach(PendingInkStroke::recycle)
        samples.clear()
    }

    private fun handleInput(event: MotionEvent): Boolean {
        predictor.record(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                requestUnbufferedDispatch(event)
                parent?.requestDisallowInterceptTouchEvent(true)
                pointerId = event.getPointerId(event.actionIndex)
                val id = wet.startStroke(event, pointerId, brush)
                samples[id] = PendingInkStroke(brush, pointerId).also { it.add(event) }
                activeId = id
                issued[id] = state.generation
                state.begin(id)
            }
            MotionEvent.ACTION_MOVE -> {
                val id = activeId ?: return true
                if (event.findPointerIndex(pointerId) < 0) { cancelInput(); return true }
                samples[id]?.add(event)
                val predicted = predictor.predict()
                try { wet.addToStroke(event, pointerId, id, predicted) }
                finally { predicted?.recycle() }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) != pointerId) return true
                val id = activeId ?: return true
                if (Build.VERSION.SDK_INT >= 33 && event.flags and MotionEvent.FLAG_CANCELED != 0) {
                    cancelInput()
                } else {
                    samples[id]?.apply { add(event); ended = true }
                    wet.finishStroke(event, pointerId, id)
                    activeId = null
                    pointerId = -1
                }
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelInput()
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun cancelInput() {
        val id = activeId
        activeId = null
        pointerId = -1
        parent?.requestDisallowInterceptTouchEvent(false)
        id?.let {
            wet.cancelStroke(id)
            issued.remove(id)
            samples.remove(id)?.recycle()
            state.cancel(id)
        }
    }
}

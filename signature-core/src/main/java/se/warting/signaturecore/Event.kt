package se.warting.signaturecore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Parcelable
import android.util.Log
import android.view.MotionEvent
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import se.warting.signatureview.utils.Bezier
import se.warting.signatureview.utils.ControlTimedPoints
import se.warting.signatureview.utils.SvgBuilder
import se.warting.signatureview.utils.TimedPoint
import se.warting.signatureview.views.SignedListener
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

@Parcelize
data class Event(
    val timestamp: Long,
    val action: Int,
    val x: Float,
    val y: Float,
) : Parcelable

@Entity
data class DBEvent(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "signatureId") val signatureId: UUID,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "action") val action: Int,
    @ColumnInfo(name = "x") val x: Float,
    @ColumnInfo(name = "y") val y: Float,
)

private fun DBEvent.toEvent(): Event {
    return Event(timestamp, action, x, y)
}

private fun Event.toDBEvent(id: Int = 0, signatureId: UUID): DBEvent {
    return DBEvent(id, signatureId, timestamp, action, x, y)
}

fun createDb(context: Context): AppDatabase {
    return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    /*
    val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "database-name"
    ).build()*/
}

interface CanvasChangedListener {
    fun onCanvasChanged()
    fun invalidate2()
}

data class DrawEvent(
    val x: Float,
    val y: Float,
    val action: Int,
    val strokeWidth: Float,
    val color: Int
)

class EventManager(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    context: Context,
    private val canvasChangedListener: CanvasChangedListener
) {

    private val svgBezier: MutableList<Pair<Bezier, Float>> = mutableListOf()

    private var signatureId: UUID = UUID.randomUUID()

    val points = mutableListOf<TimedPoint>()

    private val db = createDb(context)
    private val eventDao = db.eventDao()

    private val incomingEvents = Channel<DBEvent>(Channel.UNLIMITED)
    private val persistEvents = Channel<DBEvent>(Channel.UNLIMITED)

    val drawThis: MutableList<DrawEvent> = mutableListOf()

    var mLastWidth = 0f

    // Configurable parameters
    var mMinWidth = 0
    var mMaxWidth = 0

    init {
        @OptIn(DelicateCoroutinesApi::class)

        GlobalScope.launch(dispatcher) {
            launch {
                for (event in incomingEvents) {
                    processEvent(event)
                    persistEvents.send(event)
                }
            }
            launch {
                for (event in persistEvents) {
                    eventDao.insert(event)
                }
            }
        }
    }

    @SuppressWarnings("LongMethod", "ComplexMethod", "ReturnCount")
    fun getTransparentSignatureBitmap(trimBlankSpace: Boolean = false): Bitmap {
        if (!trimBlankSpace) {
            return mSignatureTransparentBitmap!!
        }
        val imgHeight = mSignatureTransparentBitmap!!.height
        val imgWidth = mSignatureTransparentBitmap!!.width
        val backgroundColor = Color.TRANSPARENT
        var xMin = Int.MAX_VALUE
        var xMax = Int.MIN_VALUE
        var yMin = Int.MAX_VALUE
        var yMax = Int.MIN_VALUE
        var foundPixel = false

        // Find xMin
        for (x in 0 until imgWidth) {
            var stop = false
            for (y in 0 until imgHeight) {
                if (mSignatureTransparentBitmap!!.getPixel(x, y) != backgroundColor) {
                    xMin = x
                    stop = true
                    foundPixel = true
                    break
                }
            }
            if (stop) break
        }

        // Image is empty...
        if (!foundPixel) return mSignatureTransparentBitmap!!

        // Find yMin
        for (y in 0 until imgHeight) {
            var stop = false
            for (x in xMin until imgWidth) {
                if (mSignatureTransparentBitmap!!.getPixel(x, y) != backgroundColor) {
                    yMin = y
                    stop = true
                    break
                }
            }
            if (stop) break
        }

        // Find xMax
        for (x in imgWidth - 1 downTo xMin) {
            var stop = false
            for (y in yMin until imgHeight) {
                if (mSignatureTransparentBitmap!!.getPixel(x, y) != backgroundColor) {
                    xMax = x
                    stop = true
                    break
                }
            }
            if (stop) break
        }

        // Find yMax
        for (y in imgHeight - 1 downTo yMin) {
            var stop = false
            for (x in xMin..xMax) {
                if (mSignatureTransparentBitmap!!.getPixel(x, y) != backgroundColor) {
                    yMax = y
                    stop = true
                    break
                }
            }
            if (stop) break
        }
        return Bitmap.createBitmap(
            mSignatureTransparentBitmap!!,
            xMin,
            yMin,
            xMax - xMin,
            yMax - yMin
        )
    }

    private fun processEvent(event: DBEvent) {
        // Your logic to handle each event sequentially
        Log.d("EventManager", "Processing event: $event")
        current(event.toEvent())
    }

    fun insertEvent(event: Event) {
        runBlocking {
            incomingEvents.send(event.toDBEvent(0, signatureId))
        }
    }

    fun getAllEvents(signatureId: UUID): Flow<List<Event>> {
        return eventDao.getAll(signatureId).map {
            it.map {
                it.toEvent()
            }
        }
    }

    fun addAll(events: List<Event>) {
        runBlocking {
            events.forEach {
                incomingEvents.send(it.toDBEvent(0, signatureId))
            }
        }
    }

    fun restoreSession(signatureId: UUID) {
        clearView()
        this.signatureId = signatureId
        runBlocking {
            getAllEvents(signatureId).first().forEach {
                incomingEvents.send(it.toDBEvent(0, signatureId))
            }
        }
    }

    fun sessionId(): UUID {
        return signatureId
    }

    fun addEvent(event: Event) {
        processEvent(event.toDBEvent(0, signatureId))
        // runBlocking {
        //    incomingEvents.send(event.toDBEvent(0, signatureId))
        // }
    }

    fun getSignature(): Signature {
        val originalEvents: List<Event> = runBlocking {
            eventDao.getAll(signatureId).first().map { it.toEvent() }
        }
        return Signature(BuildConfig.VERSION_CODE, originalEvents)
    }

    fun setSignature(signature: Signature) {
        runBlocking {
            signature.events.forEach {
                incomingEvents.send(it.toDBEvent(0, signatureId))
            }
        }
    }

    var mSignedListener: SignedListener? = null

    fun clearView() {
        svgBezier.clear()
        points.clear()
        signatureId = UUID.randomUUID()
        mLastVelocity = 0f
        mLastWidth = (mMinWidth + mMaxWidth) / 2f
        mLastWidth = ((mMinWidth + mMaxWidth) / 2).toFloat()
        mPointsCache.clear()
        mSignatureTransparentBitmap = null
    }

    var mSignatureTransparentBitmap: Bitmap? = null

    fun ensureSignatureBitmapInOnDraw(width: Int, height: Int): Boolean {
        if (mSignatureTransparentBitmap == null && width > 0 && height > 0) {
            mSignatureTransparentBitmap = Bitmap.createBitmap(
                width, height,
                Bitmap.Config.ARGB_8888
            ).also {
                mSignatureBitmapCanvas = Canvas(it)
            }
        }
        return mSignatureTransparentBitmap != null
    }

    fun drawBitmap(canvas: Canvas) {
        synchronized(drawThis) {
            drawThis.forEach {
                mPaint.strokeWidth = it.strokeWidth
                mPaint.color = it.color
                canvas.drawPoint(it.x, it.y, mPaint)
            }
        }
        // drawThis.clear()
    }

    fun getSignatureSvg(): String {
        val mSvgBuilder = SvgBuilder()
        val width = mSignatureTransparentBitmap!!.width
        val height = mSignatureTransparentBitmap!!.height
        svgBezier.forEach {
            mSvgBuilder.append(it.first, it.second)
        }
        return mSvgBuilder.build(width, height)
    }

    private fun current(event: Event) {
        val timestamp = event.timestamp
        val eventX = event.x
        val eventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                points.clear()

                addTimedPoint(getNewTimedPoint(eventX, eventY, timestamp))

                mSignedListener?.onStartSigning()
                addTimedPoint(getNewTimedPoint(eventX, eventY, timestamp))
            }

            MotionEvent.ACTION_MOVE -> {
                addTimedPoint(getNewTimedPoint(eventX, eventY, timestamp))

                mSignedListener?.onSigning()
            }

            MotionEvent.ACTION_UP -> {
                addTimedPoint(getNewTimedPoint(eventX, eventY, timestamp))

                mSignedListener?.onSigned()
            }

            else -> {
                throw IllegalStateException("Unknown Motion " + event.action)
            }
        }
    }

    var mLastVelocity = 0f

    var mVelocityFilterWeight = 0f

    @SuppressWarnings("MagicNumber")
    private fun addTimedPoint(timedPoint: TimedPoint) {
        points.add(timedPoint)
        val pointsCount = points.size
        if (pointsCount > 3) {
            var tmp: ControlTimedPoints =
                calculateCurveControlPoints(points[0], points[1], points[2], timedPoint.timestamp)
            val c2 = tmp.c2
            recyclePoint(tmp.c1)
            tmp = calculateCurveControlPoints(points[1], points[2], points[3], timedPoint.timestamp)
            val c3 = tmp.c1
            recyclePoint(tmp.c2)
            val curve = Bezier(points[1], c2, c3, points[2])
            val startPoint = curve.startPoint
            val endPoint = curve.endPoint
            var velocity = endPoint.velocityFrom(startPoint)

            velocity = (
                mVelocityFilterWeight * velocity +
                    (1 - mVelocityFilterWeight) * mLastVelocity
                )

            // The new width is a function of the velocity. Higher velocities
            // correspond to thinner strokes.
            val newWidth = strokeWidth(velocity)

            // The Bezier's width starts out as last curve's final width, and
            // gradually changes to the stroke width just calculated. The new
            // width calculation is based on the velocity between the Bezier's
            // start and end mPoints.
            addBezier(curve, mLastWidth, newWidth)
            mLastVelocity = velocity
            mLastWidth = newWidth

            // Remove the first element from the list,
            // so that we always have no more than 4 mPoints in mPoints array.
            recyclePoint(points.removeAt(0))
            recyclePoint(c2)
            recyclePoint(c3)
        } else if (pointsCount == 1) {
            // To reduce the initial lag make it work with 3 mPoints
            // by duplicating the first point
            val firstPoint = points[0]
            points.add(getNewTimedPoint(firstPoint.x, firstPoint.y, timedPoint.timestamp))
        }
    }

    /**
     * Set the velocity filter weight.
     *
     * @param velocityFilterWeight the weight.
     */
    fun setVelocityFilterWeight(velocityFilterWeight: Float) {
        mVelocityFilterWeight = velocityFilterWeight
    }

    private fun strokeWidth(velocity: Float): Float {
        return max(mMaxWidth / (velocity + 1), mMinWidth.toFloat())
    }

    // Cache
    private val mPointsCache: MutableList<TimedPoint?> = ArrayList()

    private fun calculateCurveControlPoints(
        s1: TimedPoint,
        s2: TimedPoint,
        s3: TimedPoint,
        timestamp: Long
    ): ControlTimedPoints {
        val dx1 = s1.x - s2.x
        val dy1 = s1.y - s2.y
        val dx2 = s2.x - s3.x
        val dy2 = s2.y - s3.y
        val m1X = (s1.x + s2.x) / 2.0f
        val m1Y = (s1.y + s2.y) / 2.0f
        val m2X = (s2.x + s3.x) / 2.0f
        val m2Y = (s2.y + s3.y) / 2.0f
        val l1 = sqrt((dx1 * dx1 + dy1 * dy1).toDouble()).toFloat()
        val l2 = sqrt((dx2 * dx2 + dy2 * dy2).toDouble()).toFloat()
        val dxm = m1X - m2X
        val dym = m1Y - m2Y
        var k = l2 / (l1 + l2)
        if (k.isNaN()) k = 0.0f
        val cmX = m2X + dxm * k
        val cmY = m2Y + dym * k
        val tx = s2.x - cmX
        val ty = s2.y - cmY
        return ControlTimedPoints(
            getNewTimedPoint(m1X + tx, m1Y + ty, timestamp),
            getNewTimedPoint(m2X + tx, m2Y + ty, timestamp)
        )
    }

    private fun getNewTimedPoint(x: Float, y: Float, timestamp: Long): TimedPoint {
        val cacheSize = mPointsCache.size
        val timedPoint: TimedPoint? = if (cacheSize == 0) {
            // Cache is empty, create a new point
            TimedPoint()
        } else {
            // Get point from cache
            mPointsCache.removeAt(cacheSize - 1)
        }
        return timedPoint!!.set(x, y, timestamp)
    }

    private fun recyclePoint(point: TimedPoint?) {
        mPointsCache.add(point)
    }

    val mPaint = Paint()

    @SuppressWarnings("MagicNumber")
    private fun addBezier(curve: Bezier, startWidth: Float, endWidth: Float) {
        synchronized(drawThis) {
            Log.d("EventManager", "addBezier")
            svgBezier.add(curve to (startWidth + endWidth) / 2)

            val originalWidth = mPaint.strokeWidth
            val widthDelta = endWidth - startWidth
            val drawSteps = ceil(curve.length().toDouble()).toFloat()
            var i = 0
            while (i < drawSteps) {
                // Calculate the Bezier (x, y) coordinate for this step.
                val t = i.toFloat() / drawSteps
                val tt = t * t
                val ttt = tt * t
                val u = 1 - t
                val uu = u * u
                val uuu = uu * u
                var x = uuu * curve.startPoint.x
                x += 3 * uu * t * curve.control1.x
                x += 3 * u * tt * curve.control2.x
                x += ttt * curve.endPoint.x
                var y = uuu * curve.startPoint.y
                y += 3 * uu * t * curve.control1.y
                y += 3 * u * tt * curve.control2.y
                y += ttt * curve.endPoint.y

                // Set the incremental stroke width and draw.
                mPaint.strokeWidth = startWidth + ttt * widthDelta
                // mSignatureBitmapCanvas!!.drawPoint(x, y, mPaint)
                drawThis.add(
                    DrawEvent(
                        x,
                        y,
                        MotionEvent.ACTION_MOVE,
                        mPaint.strokeWidth,
                        mPaint.color
                    )
                )
                i++
            }
            mPaint.strokeWidth = originalWidth
            Log.d("EventManager", "onCanvasChanged")
            canvasChangedListener.onCanvasChanged()
        }
    }

    fun getSignatureBitmap(): Bitmap {
        val originalBitmap = mSignatureTransparentBitmap!!
        val whiteBgBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(whiteBgBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        return whiteBgBitmap
    }

    var mSignatureBitmapCanvas: Canvas? = null

    private fun clearCanvas(canvas: Canvas) {
        val clearPaint = Paint()
        clearPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), clearPaint)
    }

    fun setMaxSize(i: Int, i1: Int, velocityFilterWeight: Float) {
        mVelocityFilterWeight = velocityFilterWeight
        mMinWidth = i
        mMaxWidth = i1
        mLastWidth = (mMinWidth + mMaxWidth) / 2f
    }
}

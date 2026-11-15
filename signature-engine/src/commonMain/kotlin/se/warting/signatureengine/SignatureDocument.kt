package se.warting.signatureengine

/** A curve and its interpolated pen widths in logical canvas coordinates. */
data class SignatureSegment(val curve: CubicCurve, val startWidth: Float, val endWidth: Float)

/**
 * Portable editable signature. Store [events] to persist a signature; transient gesture and redo
 * state intentionally are not part of that snapshot. Rendering adapters observe [onChange].
 */
class SignatureDocument(
    minWidth: Int = 3,
    maxWidth: Int = 7,
    velocityFilterWeight: Float = 0.9f,
    private val onChange: () -> Unit = {},
) {
    private val samples = mutableListOf<SignatureEvent>()
    private val curves = mutableListOf<SignatureSegment>()
    private data class RemovedStroke(val index: Int, val events: List<SignatureEvent>)
    private val undone = mutableListOf<RemovedStroke>()
    private val svg = SvgBuilder()
    private val builder = StrokeBuilder { curve, start, end ->
        curves.add(SignatureSegment(curve, start, end))
        svg.append(curve, (start + end) / 2)
    }.apply { configure(minWidth, maxWidth, velocityFilterWeight) }
    val input = SignatureInput(::add)
    val events: List<SignatureEvent> get() = samples.toList()
    val segments: List<SignatureSegment> get() = curves.toList()
    val isEmpty: Boolean get() = builder.isEmpty
    val canUndo: Boolean get() = completedStroke() != null
    val canRedo: Boolean get() = undone.isNotEmpty()

    fun add(event: SignatureEvent) {
        val valid = sanitize(event) ?: return
        undone.clear()
        samples.add(valid)
        process(valid)
        onChange()
    }

    fun clear() = restore(emptyList())

    fun restore(events: List<SignatureEvent>) {
        input.cancel()
        undone.clear()
        samples.clear()
        events.forEach { event -> sanitize(event)?.let(samples::add) }
        replay()
    }

    fun undo() {
        val range = completedStroke() ?: return
        undone.add(RemovedStroke(range.first, samples.subList(range.first, range.last + 1).toList()))
        samples.subList(range.first, range.last + 1).clear()
        replay()
    }

    fun redo() {
        if (undone.isEmpty()) return
        val stroke = undone.removeAt(undone.lastIndex)
        samples.addAll(stroke.index.coerceAtMost(samples.size), stroke.events)
        replay()
    }

    fun toSvg(width: Int, height: Int, penColor: Int? = null, backgroundColor: Int? = null): String =
        svg.build(width, height, penColor, backgroundColor)

    private fun sanitize(event: SignatureEvent): SignatureEvent? {
        require(event.action in SignatureEvent.DOWN..SignatureEvent.MOVE) { "Unknown action ${event.action}" }
        if (event.x.isFinite() && event.y.isFinite()) return event
        if (event.action != SignatureEvent.UP) return null
        val previous = samples.lastOrNull()
        return event.copy(x = previous?.x ?: 0f, y = previous?.y ?: 0f)
    }

    private fun process(event: SignatureEvent) {
        if (event.action == SignatureEvent.DOWN) {
            builder.beginStroke()
            builder.add(event.x, event.y, event.timestamp)
        }
        builder.add(event.x, event.y, event.timestamp)
    }

    private fun replay() {
        builder.reset()
        curves.clear()
        svg.clear()
        samples.forEach(::process)
        onChange()
    }

    private fun completedStroke(): IntRange? {
        val end = samples.indexOfLast { it.action == SignatureEvent.UP }
        if (end < 0) return null
        val start = (end downTo 0).firstOrNull { samples[it].action == SignatureEvent.DOWN } ?: return null
        return start..end
    }
}

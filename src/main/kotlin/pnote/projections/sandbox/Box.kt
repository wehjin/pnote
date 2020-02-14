package pnote.projections.sandbox


fun <T, U> Box<T>.packRight(width: Int, box: Box<U>): Box<Void> = box(
    name = "${this.name}|${box.name}",
    render = {
        val (leftBounds, rightBounds) = edge.bounds.partitionRight(width)
        this@packRight.render(withEdgeBounds(leftBounds))
        box.render(withEdgeBounds(rightBounds))
    },
    focus = {
        val (leftBounds, rightBounds) = edge.bounds.partitionRight(width)
        this@packRight.focus(withEdgeBounds(leftBounds))
        box.focus(withEdgeBounds(rightBounds))
    },
    setContent = noContent
)

fun <T, U> Box<T>.packLeft(width: Int, box: Box<U>): Box<Void> = box(
    name = "${box.name}|${this.name}",
    render = {
        val (leftBounds, rightBounds) = edge.bounds.partitionLeft(width)
        this@packLeft.render(withEdgeBounds(rightBounds))
        box.render(withEdgeBounds(leftBounds))
    },
    focus = {
        val (leftBounds, rightBounds) = edge.bounds.partitionLeft(width)
        this@packLeft.focus(withEdgeBounds(rightBounds))
        box.focus(withEdgeBounds(leftBounds))
    },
    setContent = noContent
)

fun <T, U> Box<T>.packTop(height: Int, box: Box<U>): Box<Void> = box(
    name = "${box.name}/$name",
    render = {
        val (topBounds, bottomBounds) = edge.bounds.partitionTop(height)
        box.render(withEdgeBounds(topBounds))
        this@packTop.render(withEdgeBounds(bottomBounds))
    },
    focus = {
        val (topBounds, bottomBounds) = edge.bounds.partitionTop(height)
        box.focus(withEdgeBounds(topBounds))
        this@packTop.focus(withEdgeBounds(bottomBounds))
    },
    setContent = noContent
)

fun <T, U> Box<T>.packBottom(height: Int, box: Box<U>): Box<Void> = box(
    name = "$name/${box.name}",
    render = {
        val (topBounds, bottomBounds) = edge.bounds.partitionBottom(height)
        this@packBottom.render(withEdgeBounds(topBounds))
        box.render(withEdgeBounds(bottomBounds))
    },
    focus = {
        val (topBounds, bottomBounds) = edge.bounds.partitionBottom(height)
        this@packBottom.focus(withEdgeBounds(topBounds))
        box.focus(withEdgeBounds(bottomBounds))
    },
    setContent = noContent
)

// TODO: Clean up duplication with a multi-box mapEdge
fun <T, U> Box<T>.before(box: Box<U>): Box<Void> = box(
    name = "${this.name}\\${box.name}",
    render = {
        val foreBounds = edge.bounds.shiftZ(-1)
        box.render(this)
        this@before.render(this.withEdgeBounds(foreBounds))
    },
    focus = {
        val foreBounds = edge.bounds.shiftZ(-1)
        box.focus(this)
        this@before.focus(this.withEdgeBounds(foreBounds))
    },
    setContent = noContent
)

fun <T> Box<T>.pad(size: Int): Box<T> =
    mapEdge { it.inset(size) }

fun <T> Box<T>.maxHeight(maxHeight: Int, snap: Snap = Snap.CENTER): Box<T> =
    mapEdge { it.confine(it.width, maxHeight, snap) }

fun <T> Box<T>.maxWidth(maxWidth: Int, snap: Float = 0.5f): Box<T> =
    mapEdge { it.confine(maxWidth, it.height, Snap(snap, 0.5f)) }

fun <T> Box<T>.mapEdge(map: (edgeBounds: BoxBounds) -> BoxBounds): Box<T> = box(
    name = name,
    render = {
        val bounds = map(edge.bounds)
        render(this.withEdgeBounds(bounds))
    },
    focus = {
        val bounds = map(edge.bounds)
        this@mapEdge.focus(this.withEdgeBounds(bounds))
    },
    setContent = this::setContent
)

interface Box<in T> : BoxContext {
    val name: String
    fun render(spotScope: SpotScope)
    fun focus(focusScope: FocusScope)
    fun setContent(content: T)
}

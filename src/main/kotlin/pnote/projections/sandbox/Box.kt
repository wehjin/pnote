package pnote.projections.sandbox

import com.googlecode.lanterna.input.KeyStroke


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
fun <T, U> Box<T>.before(aftBox: Box<U>): Box<Void> = box(
    name = "${this.name}\\${aftBox.name}",
    render = {
        aftBox.render(this.withEdgeBounds(edge.bounds.shiftZ(1)))
        this@before.render(this.withEdgeBounds(edge.bounds.shiftZ(0)))
    },
    focus = {
        aftBox.focus(this.withEdgeBounds(edge.bounds.shiftZ(1)))
        this@before.focus(this.withEdgeBounds(edge.bounds.shiftZ(0)))
    },
    setContent = noContent
)

fun <T> Box<T>.pad(size: Int): Box<T> = mapEdge { it.insetXY(size) }
fun <T> Box<T>.pad(cols: Int, rows: Int): Box<T> = mapEdge { it.insetX(cols).insetY(rows) }
fun <T> Box<T>.padX(left: Int, right: Int): Box<T> = mapEdge { it.insetX(left, right) }
fun <T> Box<T>.padX(cols: Int): Box<T> = mapEdge { it.insetX(cols) }
fun <T> Box<T>.padY(rows: Int): Box<T> = mapEdge { it.insetY(rows) }


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

fun <T> Box<T>.focusable(id: Long, onKey: FocusKeyScope.() -> Boolean): Box<T> = box(
    name = name,
    render = this::render,
    focus = {
        val focusScope = this
        setFocusable(Focusable(id, edge.bounds, keyReader(id) { keyStroke ->
            val scope = object : FocusKeyScope {
                override val edge: BoxEdge by lazy { focusScope.edge }
                override val keyStroke: KeyStroke = keyStroke
                override fun setChanged(bounds: BoxBounds) = focusScope.setChanged(bounds)
            }
            scope.onKey()
        }))
    },
    setContent = this::setContent
)

interface Box<in T> : BoxContext {
    val name: String
    fun render(spotScope: SpotScope)
    fun focus(focusScope: FocusScope)
    fun setContent(content: T)
}

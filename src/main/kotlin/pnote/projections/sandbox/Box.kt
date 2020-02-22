package pnote.projections.sandbox

import com.googlecode.lanterna.input.KeyStroke
import java.lang.Integer.min


fun <T, U> Box<T>.pack(
    name: String,
    packBox: Box<U>,
    partition: BoxBounds.() -> Pair<BoxBounds, BoxBounds>
): Box<Void> {
    return box(
        name = name,
        render = {
            val (aBounds, bBounds) = edge.bounds.partition()
            val aZ = this@pack.render(withEdgeBounds(aBounds))
            val bZ = packBox.render(withEdgeBounds(bBounds))
            min(aZ, bZ)
        },
        focus = {
            val (aBounds, bBounds) = edge.bounds.partition()
            this@pack.focus(withEdgeBounds(aBounds))
            packBox.focus(withEdgeBounds(bBounds))
        },
        setContent = noContent
    )
}

fun <T, U> Box<T>.packRight(width: Int, box: Box<U>): Box<Void> {
    return pack(
        name = "$name|${box.name}",
        packBox = box,
        partition = { this.partitionRight(width) }
    )
}

fun <T, U> Box<T>.packBottom(height: Int, box: Box<U>): Box<Void> {
    return pack(
        name = "$name/${box.name}",
        packBox = box,
        partition = { this.partitionBottom(height) }
    )
}

fun <T, U> Box<T>.packLeft(width: Int, box: Box<U>): Box<Void> {
    return pack(
        name = "${box.name}|$name",
        packBox = box,
        partition = { partitionLeft(width).let { Pair(it.second, it.first) } }
    )
}

fun <T, U> Box<T>.packTop(height: Int, box: Box<U>): Box<Void> {
    return pack(
        name = "${box.name}/$name",
        packBox = box,
        partition = { partitionTop(height).let { Pair(it.second, it.first) } }
    )
}

fun <T, U> Box<T>.before(aftBox: Box<U>): Box<Void> = box(
    name = "$name\\${aftBox.name}",
    render = {
        val aftZ = aftBox.render(this)
        val foreZ = this@before.render(withEdgeBounds(edge.bounds.copy(z = aftZ - 1)))
        min(aftZ, foreZ)
    },
    focus = {
        // TODO: Fix z handling for focus. This is wrong. Should probably behave like render.
        // do something different from render to calculating z.
        aftBox.focus(this)
        this@before.focus(withEdgeBounds(edge.bounds.shiftZ(-1)))
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
    setContent = this::update
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
    setContent = this::update
)

interface Box<in T> : BoxContext {
    val name: String
    fun render(spotScope: SpotScope): Int
    fun focus(focusScope: FocusScope)
    fun update(motion: T)
}

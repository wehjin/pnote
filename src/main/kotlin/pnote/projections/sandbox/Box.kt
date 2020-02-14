package pnote.projections.sandbox


fun Box.packRight(width: Int, box: Box): Box {
    return box(
        "${this.name}|${box.name}",
        render = {
            val (leftBounds, rightBounds) = edge.bounds.partitionRight(width)
            this@packRight.render(withEdgeBounds(leftBounds))
            box.render(withEdgeBounds(rightBounds))
        },
        focus = {
            val (leftBounds, rightBounds) = edge.bounds.partitionRight(width)
            this@packRight.focus(withEdgeBounds(leftBounds))
            box.focus(withEdgeBounds(rightBounds))
        }
    )
}

fun Box.packLeft(width: Int, box: Box): Box {
    return box("${box.name}|${this.name}",
        render = {
            val (leftBounds, rightBounds) = edge.bounds.partitionLeft(width)
            this@packLeft.render(withEdgeBounds(rightBounds))
            box.render(withEdgeBounds(leftBounds))
        },
        focus = {
            val (leftBounds, rightBounds) = edge.bounds.partitionLeft(width)
            this@packLeft.focus(withEdgeBounds(rightBounds))
            box.focus(withEdgeBounds(leftBounds))
        })
}

fun Box.packBottom(height: Int, box: Box): Box {
    return box("${this.name}/${box.name}",
        render = {
            val (topBounds, bottomBounds) = edge.bounds.partitionBottom(height)
            this@packBottom.render(withEdgeBounds(topBounds))
            box.render(withEdgeBounds(bottomBounds))
        },
        focus = {
            val (topBounds, bottomBounds) = edge.bounds.partitionBottom(height)
            this@packBottom.focus(withEdgeBounds(topBounds))
            box.focus(withEdgeBounds(bottomBounds))
        })
}


fun Box.before(box: Box): Box {
    // TODO: Clean up duplication with a multi-box mapEdge
    return box("${this.name}\\${box.name}",
        render = {
            val foreBounds = edge.bounds.shiftZ(-1)
            box.render(this)
            this@before.render(this.withEdgeBounds(foreBounds))
        },
        focus = {
            val foreBounds = edge.bounds.shiftZ(-1)
            box.focus(this)
            this@before.focus(this.withEdgeBounds(foreBounds))
        })
}

fun Box.pad(size: Int): Box {
    return mapEdge { it.inset(size) }
}

fun Box.maxHeight(maxHeight: Int): Box {
    return mapEdge { it.confine(it.width, maxHeight, 0.5f) }
}

fun Box.maxWidth(maxWidth: Int, snap: Float = 0.0f): Box {
    return mapEdge { it.confine(maxWidth, it.height, snap) }
}

fun Box.mapEdge(map: (edgeBounds: BoxBounds) -> BoxBounds): Box = box(
    name = name,
    render = {
        val bounds = map(edge.bounds)
        render(this.withEdgeBounds(bounds))
    },
    focus = {
        val bounds = map(edge.bounds)
        this@mapEdge.focus(this.withEdgeBounds(bounds))
    }
)

interface Box : BoxContext {
    val name: String
    fun render(spotScope: SpotScope)
    fun focus(focusScope: FocusScope)
}

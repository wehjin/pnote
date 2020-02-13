package pnote.projections.sandbox


fun Box.pad(size: Int): Box {
    return box(this.name) {
        val padBounds = edge.bounds.inset(size)
        this@pad.render(withEdgeBounds(padBounds))
    }
}

fun Box.packRight(width: Int, box: Box): Box {
    return box("${this.name}|${box.name}") {
        val (leftBounds, rightBounds) = edge.bounds.partitionRight(width)
        this@packRight.render(withEdgeBounds(leftBounds))
        box.render(withEdgeBounds(rightBounds))
    }
}

fun Box.packLeft(width: Int, box: Box): Box {
    return box("${box.name}|${this.name}") {
        val (leftBounds, rightBounds) = edge.bounds.partitionLeft(width)
        this@packLeft.render(withEdgeBounds(rightBounds))
        box.render(withEdgeBounds(leftBounds))
    }
}

fun Box.before(box: Box): Box {
    return box("${this.name}\\${box.name}") {
        val foreBounds = edge.bounds.shiftZ(-1)
        box.render(this)
        this@before.render(this.withEdgeBounds(foreBounds))
    }
}

fun Box.maxHeight(maxHeight: Int): Box {
    return box(name) {
        val confined = edge.bounds.confine(edge.bounds.width, maxHeight, 0.5f)
        render(this.withEdgeBounds(confined))
    }
}

fun Box.maxWidth(maxWidth: Int): Box {
    return box(name) {
        val confined = edge.bounds.confine(maxWidth, edge.bounds.height, 0.0f)
        render(this.withEdgeBounds(confined))
    }
}

interface Box : BoxContext {
    val name: String
    fun render(spotScope: SpotScope)
}

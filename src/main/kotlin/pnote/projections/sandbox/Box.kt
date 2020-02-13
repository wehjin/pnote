package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor


fun labelBox(label: String, color: TextColor, snapX: Float = 0.5f): Box {
    return box("LabelBox") {
        val labelBounds = edge.bounds.clipCenter(label.length, 1, snapX)
        if (labelBounds.hits(col, row, glyphMinZ)) {
            setGlyph(label[labelBounds.leftInset(col)], color, labelBounds.z)
        }
    }
}

fun colorBox(color: TextColor): Box = box("ColorBox") {
    if (edge.bounds.contains(col, row) && edge.bounds.z <= colorMinZ) {
        setColor(color, edge.bounds.z)
    }
}

fun box(name: String, render: SpotScope.() -> Unit): Box =
    object : Box {
        override val name: String = name
        override fun render(spotScope: SpotScope) = spotScope.render()
    }

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

fun Box.before(box: Box): Box {
    return box("${this.name}\\${box.name}") {
        val foreBounds = edge.bounds.shiftZ(-1)
        box.render(this)
        this@before.render(this.withEdgeBounds(foreBounds))
    }
}

interface Box {
    val name: String
    fun render(spotScope: SpotScope)
}

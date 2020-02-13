package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor

interface BoxContext {
    val primarySwatch: ColorSwatch
    val primaryLightSwatch: ColorSwatch
    val surfaceSwatch: ColorSwatch
}

fun BoxContext.inputBox(): Box {
    val content = "***"
    val label = labelBox(content, primaryLightSwatch.glyphColor, 0f)
    val cursor = labelBox("_", primaryLightSwatch.glyphColor, 0f)
    val color = colorBox(primaryLightSwatch.color)
    return cursor.packLeft(content.length, label).before(color)
}

fun BoxContext.labelBox(label: String, color: TextColor, snapX: Float = 0.5f): Box {
    return box("LabelBox") {
        val labelBounds = edge.bounds.confine(label.length, 1, snapX)
        if (labelBounds.hits(col, row, glyphMinZ)) {
            setGlyph(label[labelBounds.leftInset(col)], color, labelBounds.z)
        }
    }
}

fun BoxContext.colorBox(color: TextColor): Box = box("ColorBox") {
    if (edge.bounds.contains(col, row) && edge.bounds.z <= colorMinZ) {
        setColor(color, edge.bounds.z)
    }
}

fun BoxContext.box(name: String, render: SpotScope.() -> Unit): Box =
    object : Box, BoxContext by this {
        override val name: String = name
        override fun render(spotScope: SpotScope) = spotScope.render()
    }

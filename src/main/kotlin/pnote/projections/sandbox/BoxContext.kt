package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import kotlin.math.absoluteValue
import kotlin.random.Random

interface BoxContext {
    val primarySwatch: ColorSwatch
    val primaryLightSwatch: ColorSwatch
    val surfaceSwatch: ColorSwatch
}

fun BoxContext.inputBox(): Box {
    var content = "***"
    val readerId = Random.nextLong().absoluteValue
    return box("InputBox") {
        if (edge.bounds.hits(col, row, colorMinZ)) {
            setColor(primaryLightSwatch.color, edge.bounds.z)
        }
        if (edge.bounds.hits(col, row, glyphMinZ)) {
            val inset = edge.bounds.leftInset(col)
            val maxContentLength = edge.bounds.width - 1
            val displayContent =
                if (content.length > maxContentLength) {
                    content.substring(content.length - maxContentLength)
                } else {
                    content
                }
            if (inset < displayContent.length)
                setGlyph(displayContent[inset], primaryLightSwatch.glyphColor, edge.bounds.z)
            else {
                setGlyph(' ', primaryLightSwatch.glyphColor, edge.bounds.z)
            }
            val cursorInset = displayContent.length
            if (edge.bounds.isTopLeftCorner(col - cursorInset, row)) {
                setCursor(col, row)
                addKeyReader(object : KeyReader {
                    override val readerId: Long = readerId
                    override fun receiveKey(keyStroke: KeyStroke) {
                        println("Received key $keyStroke in inputBox")
                        when (keyStroke.keyType) {
                            KeyType.Character -> {
                                content += keyStroke.character.toString()
                                setChanged(edge.bounds)
                            }
                            KeyType.Backspace, KeyType.Delete -> {
                                content = if (content.isNotEmpty()) content.substring(0, content.lastIndex) else ""
                                setChanged(edge.bounds)
                            }
                            else -> Unit
                        }
                    }
                })
            }
        }
    }
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

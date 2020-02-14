package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.random.Random

interface BoxContext {
    val primarySwatch: ColorSwatch
    val primaryLightSwatch: ColorSwatch
    val primaryDarkSwatch: ColorSwatch
    val surfaceSwatch: ColorSwatch
    val secondarySwatch: ColorSwatch
}

fun BoxContext.buttonBox(text: String, swatch: ColorSwatch, onPress: () -> Unit): Box {
    val focusableId = randomId()
    var pressed = false
    return box(
        name = "ButtonBox",
        render = {
            val label = if (activeFocusId == focusableId) "[ $text ]" else "  $text  "
            val stateSwatch = if (pressed) {
                primarySwatch
            } else {
                swatch
            }
            labelBox(label, stateSwatch.glyphColor).before(colorBox(stateSwatch.color))
                .maxWidth(label.length, 0.5f)
                .render(this)
            if (activeFocusId == focusableId && edge.bounds.isTopLeftCorner(col, row)) {
                setCursor(-1, -1)
            }
        },
        focus = {
            setFocusable(Focusable(focusableId, edge.bounds, object : KeyReader {
                override val readerId: Long = focusableId
                override fun receiveKey(keyStroke: KeyStroke) {
                    println("KEYSTROKE: $keyStroke")
                    val isPress = when {
                        keyStroke.keyType == KeyType.Enter -> true
                        keyStroke.character == ' ' -> true
                        else -> false
                    }
                    if (isPress) {
                        GlobalScope.launch {
                            pressed = true
                            setChanged(edge.bounds)
                            delay(200)
                            pressed = false
                            setChanged(edge.bounds)
                            delay(100)
                            onPress()
                        }
                    }
                }
            }))
        }
    )
}

fun BoxContext.inputBox(): Box {
    var content = ""
    val focusableId = randomId()
    return box("InputBox",
        render = {
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
                if (edge.bounds.isTopLeftCorner(col - cursorInset, row) && activeFocusId == focusableId) {
                    setCursor(col, row)
                }
            }
        },
        focus = {
            setFocusable(Focusable(focusableId, edge.bounds, object : KeyReader {
                override val readerId: Long = focusableId
                override fun receiveKey(keyStroke: KeyStroke) {
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
            }))
        }
    )
}

private fun randomId(): Long = Random.nextLong().absoluteValue

fun BoxContext.labelBox(label: String, color: TextColor, snapX: Float = 0.5f): Box {
    return box("LabelBox", {
        val labelBounds = edge.bounds.confine(label.length, 1, snapX)
        if (labelBounds.hits(col, row, glyphMinZ)) {
            setGlyph(label[labelBounds.leftInset(col)], color, labelBounds.z)
        }
    }, focus = noFocus)
}

fun BoxContext.colorBox(color: TextColor?): Box = box("ColorBox", {
    if (edge.bounds.contains(col, row) && edge.bounds.z <= colorMinZ && color != null) {
        setColor(color, edge.bounds.z)
    }
}, focus = noFocus)

fun BoxContext.box(
    name: String,
    render: SpotScope.() -> Unit,
    focus: FocusScope.() -> Unit
): Box = object : Box, BoxContext by this {
    override val name: String = name
    override fun focus(focusScope: FocusScope) = focusScope.focus()
    override fun render(spotScope: SpotScope) = spotScope.render()
}

val noFocus = { _: FocusScope -> Unit }

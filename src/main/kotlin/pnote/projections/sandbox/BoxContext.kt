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

@Suppress("unused")
fun BoxContext.columnBox(levelHeight: Int, snap: Snap, vararg boxes: Box<*>): Box<*> {
    val reversed = boxes.toList()
    val stack = reversed.drop(1).fold(
        initial = reversed.first(),
        operation = { stack, box -> stack.packBottom(levelHeight, box) }
    )
    return stack.maxHeight(levelHeight * boxes.size, snap)
}

fun BoxContext.messageBox(message: String, swatch: ColorSwatch, snap: Snap = Snap.CENTER): Box<String> {
    val label = labelBox(message, swatch.strokeColor, snap)
    val labelAndFill = label.before(fillBox(swatch.fillColor))
    return box(
        name = "MessageBox",
        render = labelAndFill::render,
        focus = noFocus,
        setContent = label::setContent
    )
}

fun BoxContext.buttonBox(
    text: String,
    swatch: ColorSwatch = surfaceSwatch,
    onPress: () -> Unit
): Box<Void> {
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
            labelBox(label, stateSwatch.strokeColor).before(fillBox(stateSwatch.fillColor))
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
                    if (keyStroke.character == ' ' || keyStroke.keyType == KeyType.Enter) {
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
        },
        setContent = noContent
    )
}

fun BoxContext.inputBox(onInput: ((String) -> Unit)? = null): Box<Void> {
    var content = ""
    val focusableId = randomId()
    return box(
        name = "InputBox",
        render = {
            if (edge.bounds.hits(col, row, colorMinZ)) {
                setColor(primaryLightSwatch.fillColor, edge.bounds.z)
            }
            if (edge.bounds.hits(col, row, glyphMinZ)) {
                val inset = edge.bounds.leftInset(col)
                val maxContentLength = edge.bounds.width - 1
                val displayContent = if (content.length > maxContentLength) {
                    content.substring(content.length - maxContentLength)
                } else {
                    content
                }
                if (inset < displayContent.length)
                    setGlyph(displayContent[inset], primaryLightSwatch.strokeColor, edge.bounds.z)
                else {
                    setGlyph(' ', primaryLightSwatch.strokeColor, edge.bounds.z)
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
                    val changed = when (keyStroke.keyType) {
                        KeyType.Character -> content + keyStroke.character.toString()
                        KeyType.Backspace, KeyType.Delete -> when {
                            content.isEmpty() -> null
                            else -> content.substring(0, content.lastIndex)
                        }
                        else -> null
                    }
                    changed?.let {
                        content = it
                        setChanged(edge.bounds)
                        onInput?.let { GlobalScope.launch { it.invoke(content) } }
                    }
                }
            }))
        },
        setContent = noContent
    )
}

private fun randomId(): Long = Random.nextLong().absoluteValue

fun BoxContext.labelBox(text: String, textColor: TextColor, snap: Snap = Snap.CENTER): Box<String> {
    var label: String = text
    return box(
        name = "LabelBox",
        render = {
            val labelBounds = edge.bounds.confine(label.length, 1, snap)
            if (labelBounds.hits(col, row, glyphMinZ)) {
                setGlyph(label[labelBounds.leftInset(col)], textColor, labelBounds.z)
            }
        },
        focus = noFocus,
        setContent = { label = it }
    )
}


fun BoxContext.gapBox() = fillBox(null)

fun BoxContext.fillBox(color: TextColor?): Box<Void> = box(
    name = "ColorBox",
    render = {
        if (edge.bounds.contains(col, row) && edge.bounds.z <= colorMinZ && color != null) {
            setColor(color, edge.bounds.z)
        }
    },
    focus = noFocus,
    setContent = noContent
)

fun BoxContext.glyphBox(glyph: Char, color: TextColor): Box<Void> = box(
    name = "GlyphBox",
    render = {
        if (edge.bounds.contains(col, row) && edge.bounds.z <= glyphMinZ) {
            setGlyph(glyph, color, edge.bounds.z)
        }
    },
    focus = noFocus,
    setContent = noContent
)

fun <T> BoxContext.box(
    name: String,
    render: SpotScope.() -> Unit,
    focus: FocusScope.() -> Unit,
    setContent: (content: T) -> Unit
): Box<T> = object : Box<T>, BoxContext by this {
    override val name: String = name
    override fun focus(focusScope: FocusScope) = focusScope.focus()
    override fun render(spotScope: SpotScope) = spotScope.render()
    override fun setContent(content: T) = setContent(content)
}

val noFocus = { _: FocusScope -> Unit }
val noContent = { _: Void -> Unit }
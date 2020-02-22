package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.input.KeyType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.random.Random

interface BoxContext {
    val boxScreen: BoxScreen
    val backgroundSwatch: ColorSwatch
    val surfaceSwatch: ColorSwatch
    val primarySwatch: ColorSwatch
    val primaryLightSwatch: ColorSwatch
    val primaryDarkSwatch: ColorSwatch
    val secondarySwatch: ColorSwatch
    val errorSwatch: ColorSwatch
}

fun BoxContext.columnBox(vararg rows: Pair<Int, Box<*>>): Box<*> {
    val (head, tail) =
        rows.firstOrNull { it.first < 0 }?.let { heightBox ->
            Pair(heightBox.second, rows.filter { it.first >= 0 })
        } ?: Pair(gapBox(), rows.drop(0))
    return tail.reversed().fold(
        initial = head,
        operation = { sum, (height, box) -> sum.packTop(height, box) }
    )
}

fun BoxContext.messageBox(message: String, swatch: ColorSwatch, snap: Snap = Snap.CENTER): Box<String> {
    val label = labelBox(message, swatch.strokeColor, snap)
    val labelAndFill = label.before(fillBox(swatch.fillColor))
    return box(
        name = "MessageBox",
        render = labelAndFill::render,
        focus = noFocus,
        setContent = label::update
    )
}

fun BoxContext.inputBox(onInput: ((String) -> Unit)? = null): Box<Void> {
    var content = ""
    val id = randomId()
    return box(
        name = "InputBox",
        render = {
            if (edge.bounds.hits(col, row, colorMinZ)) {
                setColor(primaryLightSwatch.fillColor, edge.bounds.z)
            }
            if (edge.bounds.hits(col, row, glyphMinZ)) {
                val inset = edge.bounds.leftInset(col)
                val maxContentLength = edge.bounds.width - 1
                val (displayContent, cursorX) = if (content.length > maxContentLength) {
                    Pair(content.substring(content.length - maxContentLength), edge.bounds.right - 1)
                } else {
                    Pair(content, edge.bounds.left + content.length)
                }
                if (inset < displayContent.length)
                    setGlyph(displayContent[inset], primaryLightSwatch.strokeColor, edge.bounds.z)
                else {
                    setGlyph(' ', primaryLightSwatch.strokeColor, edge.bounds.z)
                }
                if (activeFocusId == id && col == cursorX && row == edge.bounds.centerY) {
                    setCursor(col, row)
                }
            }
            edge.bounds.z
        },
        focus = {
            setFocusable(Focusable(id, edge.bounds, keyReader(id) { keyStroke ->
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
                    true
                } ?: false
            }))
        },
        setContent = noContent
    )
}

fun randomId(): Long = Random.nextLong().absoluteValue

fun BoxContext.labelBox(text: CharSequence, textColor: TextColor, snap: Snap = Snap.CENTER): Box<String> {
    var label: CharSequence = text
    return box(
        name = "LabelBox",
        render = {
            val labelBounds = edge.bounds.confine(label.length, 1, snap)
            if (labelBounds.hits(col, row, glyphMinZ)) {
                setGlyph(label[labelBounds.leftInset(col)], textColor, labelBounds.z)
            }
            edge.bounds.z
        },
        focus = noFocus,
        setContent = { label = it }
    )
}


fun BoxContext.gapBox() = fillBox(null)

fun BoxContext.fillBox(color: TextColor?): Box<Void> = box(
    name = "ColorBox",
    render = {
        if (edge.bounds.contains(col, row) && color != null) {
            setColor(color, edge.bounds.z)
        }
        edge.bounds.z
    },
    focus = noFocus,
    setContent = noContent
)

fun BoxContext.glyphBox(glyph: Char, color: TextColor): Box<Void> = box(
    name = "GlyphBox",
    render = {
        if (edge.bounds.contains(col, row)) {
            setGlyph(glyph, color, edge.bounds.z)
        }
        edge.bounds.z
    },
    focus = noFocus,
    setContent = noContent
)

fun <T> BoxContext.box(
    name: String,
    render: SpotScope.() -> Int,
    focus: FocusScope.() -> Unit,
    setContent: (content: T) -> Unit
): Box<T> = object : Box<T>, BoxContext by this {
    override val name: String = name
    override fun focus(focusScope: FocusScope) = focusScope.focus()
    override fun render(spotScope: SpotScope) = spotScope.render()
    override fun update(motion: T) = setContent(motion)
}

val noFocus = { _: FocusScope -> Unit }
val noContent = { _: Void -> Unit }
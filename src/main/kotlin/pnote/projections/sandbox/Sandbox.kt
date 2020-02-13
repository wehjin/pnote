package pnote.projections.sandbox

import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory

fun main(args: Array<String>) {
    LanternaProjector().start()
}

class LanternaProjector {
    fun start() {

        val box = labelBox("Hello")

        val terminal = DefaultTerminalFactory().createTerminal()
        val screen = TerminalScreen(terminal).apply { startScreen() }
        with(screen) {
            val size = terminalSize
            val widthRange = 0 until size.columns
            val heightRange = 0 until size.rows
            val screenBounds = BoxBounds(size.columns, size.rows)
            box.onEdgeBounds(screenBounds)
            widthRange.forEach { col ->
                heightRange.forEach { row ->
                    val rendition = Rendition()
                    box.render(col, row, -1, rendition)
                    if (rendition.isWritten) {
                        val oldCharacter = getFrontCharacter(col, row)
                        val nextGlyph = rendition.glyph ?: oldCharacter.character
                        val nextFore = rendition.foreColor ?: oldCharacter.foregroundColor
                        val nextBack = rendition.backColor ?: oldCharacter.backgroundColor
                        val nextCharacter = TextCharacter(nextGlyph, nextFore, nextBack)
                        setCharacter(col, row, nextCharacter)
                    }
                }
            }
            refresh()
        }
    }
}

fun labelBox(label: String): LabelBox =
    object : LabelBox {
        private val range: BoxBounds = BoxBounds()
        private var z: Int = 0

        override fun onEdgeBounds(value: BoxBounds) {
            val labelWidth = label.length
            val extraWidth = value.width - labelWidth
            range.left = value.left + extraWidth / 2
            range.right = range.left + labelWidth
            val extraHeight = value.height - 1
            range.top = value.top + extraHeight / 2
            range.bottom = range.top + 1
        }

        override fun render(col: Int, row: Int, maxZ: Int, rendition: Rendition) {
            if (range.contains(col, row)) {
                rendition.update(label[col - range.left], TextColor.ANSI.WHITE, null, z)
            }
        }
    }

data class Rendition(
    var glyph: Char? = null,
    var foreColor: TextColor? = null,
    var backColor: TextColor? = null,
    var maxZ: Int = 1000
) {
    val isWritten: Boolean
        get() = glyph != null || foreColor != null || backColor != null

    fun update(glyph: Char?, foreColor: TextColor?, backColor: TextColor?, z: Int) {
        if (z <= maxZ) {
            glyph?.let { this.glyph = it }
            foreColor?.let { this.foreColor = it }
            backColor?.let { this.backColor = it }
            maxZ = z
        }
    }
}

interface LabelBox : Box {

}

interface Box {
    fun onEdgeBounds(value: BoxBounds)
    fun render(col: Int, row: Int, maxZ: Int, rendition: Rendition)
}

data class BoxBounds(
    var right: Int = 0,
    var bottom: Int = 0,
    var left: Int = 0,
    var top: Int = 0
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    fun contains(col: Int, row: Int): Boolean {
        return col >= left && row >= top && col < right && row < bottom
    }
}


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

        val sideBox = labelBox("World!", TextColor.ANSI.BLACK).before(colorBox(TextColor.ANSI.MAGENTA))
        val box = labelBox("Hello", TextColor.ANSI.WHITE).packRight(20, sideBox)

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
                    box.updateRendition(col, row, rendition)
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

fun labelBox(label: String, color: TextColor): Box = object : Box {

    override fun onEdgeBounds(value: BoxBounds) {
        val labelWidth = label.length
        val extraWidth = value.width - labelWidth
        bounds.left = value.left + extraWidth / 2
        bounds.right = bounds.left + labelWidth
        val extraHeight = value.height - 1
        bounds.top = value.top + extraHeight / 2
        bounds.bottom = bounds.top + 1
        bounds.z = value.z
    }

    override fun updateRendition(col: Int, row: Int, rendition: Rendition) {
        if (bounds.contains(col, row)) {
            rendition.update(label[col - bounds.left], color, null, bounds.z)
        }
    }

    private val bounds: BoxBounds = BoxBounds()
}

fun colorBox(color: TextColor): Box = object : Box {

    override fun onEdgeBounds(value: BoxBounds) = bounds.set(value)

    override fun updateRendition(col: Int, row: Int, rendition: Rendition) {
        if (bounds.contains(col, row)) {
            rendition.update(null, null, color, bounds.z)
        }
    }

    private val bounds = BoxBounds()
}

interface Box {
    fun onEdgeBounds(value: BoxBounds)
    fun updateRendition(col: Int, row: Int, rendition: Rendition)
}

fun Box.before(box: Box): Box {
    return object : Box {
        override fun onEdgeBounds(value: BoxBounds) {
            val aftBounds = BoxBounds().apply {
                set(value)
                z = value.z + 1
            }
            box.onEdgeBounds(aftBounds)
            this@before.onEdgeBounds(value)
        }

        override fun updateRendition(col: Int, row: Int, rendition: Rendition) {
            box.updateRendition(col, row, rendition)
            this@before.updateRendition(col, row, rendition)
        }
    }
}

fun Box.packRight(size: Int, box: Box): Box {
    return object : Box {
        override fun onEdgeBounds(value: BoxBounds) {
            val (left, right) = value.partitionRight(size)
            leftBox.onEdgeBounds(left)
            box.onEdgeBounds(right)
        }

        override fun updateRendition(col: Int, row: Int, rendition: Rendition) {
            leftBox.updateRendition(col, row, rendition)
            box.updateRendition(col, row, rendition)
        }

        private val leftBox = this@packRight
    }
}


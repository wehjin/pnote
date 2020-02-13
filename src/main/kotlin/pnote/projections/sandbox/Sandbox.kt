package pnote.projections.sandbox

import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory

fun main(args: Array<String>) {
    LanternaProjector().start()
}

class LanternaProjector : BoxContext {

    override val primarySwatch: ColorSwatch =
        ColorSwatch(TextColor.ANSI.WHITE, TextColor.Indexed.fromRGB(0x62, 0x00, 0xEE))
    override val primaryLightSwatch: ColorSwatch =
        ColorSwatch(TextColor.ANSI.BLACK, TextColor.Indexed.fromRGB(0xBB, 0x86, 0xFC))
    override val surfaceSwatch: ColorSwatch =
        ColorSwatch(TextColor.ANSI.BLACK, TextColor.Indexed.fromRGB(0xFF, 0xFF, 0xFF))

    fun start() {

        val sideBox = inputBox().maxHeight(1).pad(2).before(colorBox(primarySwatch.color))
        val contentBox = labelBox("Import Password", surfaceSwatch.glyphColor).before(colorBox(surfaceSwatch.color))
        val box = contentBox.packRight(30, sideBox)

        val terminal = DefaultTerminalFactory().createTerminal()
        val screen = TerminalScreen(terminal).apply { startScreen() }
        with(screen) {
            val size = terminalSize
            val boxEdge = object : BoxEdge {
                override val bounds: BoxBounds = BoxBounds(size.columns, size.rows)
            }
            val widthRange = 0 until size.columns
            val heightRange = 0 until size.rows
            widthRange.forEach { col ->
                heightRange.forEach { row ->
                    val spotScope = object : SpotScope {
                        override val col: Int = col
                        override val row: Int = row
                        override val edge: BoxEdge = boxEdge
                        override var colorMinZ: Int = Int.MAX_VALUE
                        override var glyphMinZ: Int = Int.MAX_VALUE

                        override fun setGlyph(glyph: Char, glyphColor: TextColor, glyphMinZ: Int) {
                            if (glyphMinZ <= this.glyphMinZ) {
                                val color = getOldColor()
                                setCharacter(col, row, TextCharacter(glyph, glyphColor, color))
                                this.glyphMinZ = glyphMinZ
                            }
                        }

                        override fun setColor(color: TextColor, colorMinZ: Int) {
                            if (colorMinZ <= this.colorMinZ) {
                                val (glyph, glyphColor) = getOldGlyph()
                                setCharacter(col, row, TextCharacter(glyph, glyphColor, color))
                                this.colorMinZ = colorMinZ
                            }
                        }

                        private fun getOldColor(): TextColor = oldCharacter(colorMinZ).backgroundColor

                        private fun getOldGlyph(): Pair<Char, TextColor> =
                            oldCharacter(glyphMinZ).let { Pair(it.character, it.foregroundColor) }

                        private fun oldCharacter(glyphMinZ1: Int): TextCharacter =
                            when (glyphMinZ1) {
                                Int.MIN_VALUE -> getFrontCharacter(col, row)
                                else -> getBackCharacter(col, row)
                            }
                    }
                    box.render(spotScope)
                }
            }
            refresh()
        }
    }
}

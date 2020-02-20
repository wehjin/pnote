package pnote.projections.sandbox

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.screen.TerminalScreen
import kotlinx.coroutines.channels.SendChannel

class ScreenSpot(
    private val screen: TerminalScreen,
    private val channel: SendChannel<RenderAction>,
    override val edge: BoxEdge,
    override val col: Int, override val row: Int,
    activeFocusId: Long?
) : SpotScope {
    override var colorMinZ: Int = Int.MAX_VALUE
    override var glyphMinZ: Int = Int.MAX_VALUE
    override val activeFocusId: Long = activeFocusId ?: 0

    override fun setChanged(bounds: BoxBounds) {
        channel.offer(RenderAction.Refresh)
    }

    override fun setCursor(col: Int, row: Int) {
        screen.cursorPosition = if (col < 0 && row < 0) null else TerminalPosition(
            col,
            row
        )
    }

    override fun setGlyph(glyph: Char, glyphColor: TextColor, glyphZ: Int) {
        if (glyphZ <= glyphMinZ) {
            val color = getOldColor()
            screen.setCharacter(
                col, row,
                TextCharacter(glyph, glyphColor, color)
            )
            glyphMinZ = glyphZ
        }
    }

    override fun setColor(color: TextColor, colorZ: Int) {
        if (colorZ <= colorMinZ) {
            val (glyph, glyphColor) = getOldGlyph()
            screen.setCharacter(
                col, row,
                TextCharacter(glyph, glyphColor, color)
            )
            colorMinZ = colorZ
        }
    }

    private fun getOldColor(): TextColor = oldCharacter(colorMinZ).backgroundColor

    private fun getOldGlyph(): Pair<Char, TextColor> {
        val oldCharacter = oldCharacter(glyphMinZ)
        return oldCharacter.let { Pair(it.character, it.foregroundColor) }
    }

    private fun oldCharacter(glyphMinZ1: Int): TextCharacter =
        when (glyphMinZ1) {
            Int.MIN_VALUE -> screen.getFrontCharacter(col, row)
            else -> screen.getBackCharacter(col, row)
        }
}
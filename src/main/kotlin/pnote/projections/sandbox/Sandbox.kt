package pnote.projections.sandbox

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel

fun main() {
    LanternaProjector().start()
}

class LanternaProjector : BoxContext {

    override val primarySwatch: ColorSwatch =
        ColorSwatch(TextColor.ANSI.WHITE, TextColor.Indexed.fromRGB(0x62, 0x00, 0xEE))
    override val primaryLightSwatch: ColorSwatch =
        ColorSwatch(TextColor.ANSI.BLACK, TextColor.Indexed.fromRGB(0xBB, 0x86, 0xFC))
    override val surfaceSwatch: ColorSwatch =
        ColorSwatch(TextColor.ANSI.BLACK, TextColor.Indexed.fromRGB(0xFF, 0xFF, 0xFF))

    sealed class RenderAction {
        object Refresh : RenderAction()
        data class KeyPress(val keyStroke: KeyStroke) : RenderAction()
    }

    fun start() {

        val cluster = inputBox()
            .packBottom(1, colorBox(primarySwatch.color))
            .packBottom(1, inputBox())
            .packBottom(1, colorBox(primarySwatch.color))
            .packBottom(1, inputBox())
            .maxHeight(5)
        val sideBox = cluster.pad(2).before(colorBox(primarySwatch.color))

        val contentBox = labelBox("Import Password", surfaceSwatch.glyphColor).before(colorBox(surfaceSwatch.color))
        val box = contentBox.packRight(30, sideBox)

        val terminal = DefaultTerminalFactory().createTerminal()
        val screen = TerminalScreen(terminal).apply { startScreen() }
        val renderChannel = Channel<RenderAction>(10).apply { offer(RenderAction.Refresh) }
        GlobalScope.launch {
            while (true) {
                val keyStroke = withContext(Dispatchers.IO) { screen.readInput() }
                when (keyStroke.keyType) {
                    KeyType.Unknown -> Unit
                    else -> renderChannel.send(RenderAction.KeyPress(keyStroke))
                }
            }
        }
        runBlocking {
            val activeFocus = ActiveFocus(renderChannel)
            for (action in renderChannel) {
                when (action) {
                    RenderAction.Refresh -> screen.renderBox(box, activeFocus, renderChannel)
                    is RenderAction.KeyPress -> activeFocus.routeKey(action.keyStroke)
                }
            }
        }
    }

    private fun TerminalScreen.renderBox(box: Box, activeFocus: ActiveFocus, channel: SendChannel<RenderAction>) {
        val size = terminalSize
        val boxEdge = object : BoxEdge {
            override val bounds: BoxBounds = BoxBounds(size.columns, size.rows)
        }

        activeFocus.focusables.clear()
        box.focus(object : FocusScope {
            override val edge: BoxEdge = boxEdge
            override fun setFocusable(focusable: Focusable) {
                activeFocus.focusables[focusable.focusableId] = focusable
            }

            override fun setChanged(bounds: BoxBounds) {
                channel.offer(RenderAction.Refresh)
            }
        })
        activeFocus.selectFocus()

        (0 until size.columns).forEach { col ->
            (0 until size.rows).forEach { row ->
                val spotScope = object : SpotScope {
                    override val col: Int = col
                    override val row: Int = row
                    override val edge: BoxEdge = boxEdge
                    override var colorMinZ: Int = Int.MAX_VALUE
                    override var glyphMinZ: Int = Int.MAX_VALUE
                    override val activeFocusId: Long = activeFocus.focusId ?: 0

                    override fun setChanged(bounds: BoxBounds) {
                        channel.offer(RenderAction.Refresh)
                    }

                    override fun setCursor(col: Int, row: Int) {
                        cursorPosition = TerminalPosition(col, row)
                    }

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

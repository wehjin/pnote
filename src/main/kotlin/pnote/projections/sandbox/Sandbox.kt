package pnote.projections.sandbox

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.TerminalScreen
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel

fun main() {
    LanternaProjector().start()
}

fun CoroutineScope.projectBox(init: BoxInitScope.() -> Box): Job = launch(Dispatchers.IO) {
    terminalScreen().use { screen ->
        val renderChannel = Channel<RenderAction>(10).apply { offer(RenderAction.Refresh) }
        val box = RenderChannelBoxInitScope(renderChannel).init()
        val inputJob = launch(Dispatchers.IO) {
            while (true) {
                val keyStroke = screen.readInput()
                when (keyStroke.keyType) {
                    KeyType.Unknown -> Unit
                    else -> renderChannel.send(RenderAction.KeyPress(keyStroke))
                }
            }
        }
        val activeFocus = ActiveFocus(renderChannel)
        actions@ for (action in renderChannel) {
            when (action) {
                RenderAction.Refresh -> updateScreen(screen, box, activeFocus, renderChannel)
                is RenderAction.KeyPress -> activeFocus.routeKey(action.keyStroke)
                RenderAction.Quit -> break@actions
            }
        }
        inputJob.cancel()
    }
}

class LanternaProjector : BoxContext {
    override val primarySwatch: ColorSwatch =
        ColorSwatch(TextColor.ANSI.WHITE, TextColor.Indexed.fromRGB(0x34, 0x49, 0x55))
    override val primaryDarkSwatch: ColorSwatch =
        ColorSwatch(TextColor.ANSI.WHITE, TextColor.Indexed.fromRGB(0x23, 0x2f, 0x34))
    override val primaryLightSwatch: ColorSwatch =
        ColorSwatch(TextColor.ANSI.WHITE, TextColor.Indexed.fromRGB(0x4a, 0x65, 0x72))
    override val surfaceSwatch: ColorSwatch =
        ColorSwatch(TextColor.ANSI.BLACK, TextColor.Indexed.fromRGB(0xFF, 0xFF, 0xFF))
    override val secondarySwatch: ColorSwatch =
        ColorSwatch(TextColor.ANSI.BLACK, TextColor.Indexed.fromRGB(0xf9, 0xaa, 0x33))

    fun start() {
        GlobalScope.projectBox {
            val passwordInput = inputBox()
            val checkInput = inputBox()
            val importButton = buttonBox("Import", secondarySwatch) { endProjection() }
            val inputCluster = passwordInput
                .packBottom(1, colorBox(null))
                .packBottom(1, checkInput)
                .packBottom(1, colorBox(null))
                .packBottom(1, importButton)
                .maxHeight(5)
            val sideBox = inputCluster.pad(2).before(colorBox(primaryDarkSwatch.color))
            val contentMessage = labelBox("Import Password", surfaceSwatch.glyphColor)
            val contentBackground = colorBox(surfaceSwatch.color)
            val contentBox = contentMessage.before(contentBackground)
            contentBox.packRight(30, sideBox)
        }
    }
}

private fun updateScreen(
    screen: TerminalScreen,
    box: Box,
    activeFocus: ActiveFocus,
    channel: SendChannel<RenderAction>
) {
    val edge = ScreenBoxEdge(screen)
    activeFocus.update(box, edge)
    edge.bounds.map { col, row ->
        val scope = ScreenSpot(screen, channel, edge, col, row, activeFocus.focusId ?: 0)
        box.render(scope)
    }
    screen.refresh()
}

private class ScreenBoxEdge(screen: TerminalScreen) : BoxEdge {
    private val size: TerminalSize by lazy { screen.terminalSize }
    override val bounds: BoxBounds = BoxBounds(size.columns, size.rows)
}

private class ScreenSpot(
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
        screen.cursorPosition = if (col < 0 && row < 0) null else TerminalPosition(col, row)
    }

    override fun setGlyph(glyph: Char, glyphColor: TextColor, glyphZ: Int) {
        if (glyphZ <= glyphMinZ) {
            val color = getOldColor()
            screen.setCharacter(col, row, TextCharacter(glyph, glyphColor, color))
            glyphMinZ = glyphZ
        }
    }

    override fun setColor(color: TextColor, colorZ: Int) {
        if (colorZ <= colorMinZ) {
            val (glyph, glyphColor) = getOldGlyph()
            screen.setCharacter(col, row, TextCharacter(glyph, glyphColor, color))
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
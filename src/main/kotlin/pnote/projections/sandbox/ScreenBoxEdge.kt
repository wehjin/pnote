package pnote.projections.sandbox

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.screen.TerminalScreen

class ScreenBoxEdge(screen: TerminalScreen) :
    BoxEdge {
    private val size: TerminalSize by lazy { screen.terminalSize }
    override val bounds: BoxBounds =
        BoxBounds(size.columns, size.rows)
}
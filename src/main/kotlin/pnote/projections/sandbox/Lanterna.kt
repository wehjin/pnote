package pnote.projections.sandbox

import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory

fun terminalScreen(): TerminalScreen {
    val terminal = DefaultTerminalFactory().createTerminal()
    return TerminalScreen(terminal).apply {
        startScreen()
        cursorPosition = null
    }
}

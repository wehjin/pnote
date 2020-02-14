package pnote.projections.sandbox

import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory

fun terminalScreen(): TerminalScreen {
    return TerminalScreen(DefaultTerminalFactory().createTerminal()).apply {
        startScreen()
        cursorPosition = null
    }
}

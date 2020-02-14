package pnote.projections.sandbox

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

private fun updateScreen(
    screen: TerminalScreen,
    box: Box,
    activeFocus: ActiveFocus,
    channel: SendChannel<RenderAction>
) {
    val edge = ScreenBoxEdge(screen)
    activeFocus.update(box, edge)
    runBlocking {
        edge.bounds.map { col, row ->
            launch {
                val scope = ScreenSpot(screen, channel, edge, col, row, activeFocus.focusId ?: 0)
                box.render(scope)
            }
        }.joinAll()
    }
    screen.refresh()
}

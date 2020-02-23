package pnote.projections.sandbox

import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.TerminalScreen
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import java.io.Closeable

fun main() {
    LanternaProjector().start()
}

class BoxScreen : BoxInitScope, Closeable {
    private val renderChannel = Channel<RenderAction>(10)
    private var renderBox: Box<*>? = null
    private val activeFocus = ActiveFocus(renderChannel)

    override fun refreshScreen() {
        renderChannel.offer(RenderAction.Refresh)
    }

    override fun endProjection() {
        renderChannel.offer(RenderAction.Quit)
    }

    override fun setBox(box: Box<*>) {
        activeFocus.focusId = null
        renderBox = box
        renderChannel.offer(RenderAction.Refresh)
    }

    override fun clearFocus() {
        activeFocus.focusId = null
        renderChannel.offer(RenderAction.Refresh)
    }

    private val job = GlobalScope.launch {
        terminalScreen().use { screen ->
            val inputJob = launch(Dispatchers.IO) {
                while (true) {
                    val keyStroke = screen.readInput()
                    if (renderBox != null) {
                        when (keyStroke.keyType) {
                            KeyType.Unknown -> Unit
                            else -> renderChannel.send(RenderAction.KeyPress(keyStroke))
                        }
                    }
                }
            }
            actions@ for (action in renderChannel) {
                when (action) {
                    RenderAction.Refresh -> renderBox?.let { updateScreen(screen, it, activeFocus, renderChannel) }
                    is RenderAction.KeyPress -> activeFocus.routeKey(action.keyStroke)
                    RenderAction.Quit -> break@actions
                }
            }
            inputJob.cancel()
        }
    }

    override fun close() {
        renderChannel.offer(RenderAction.Quit)
    }

    fun onEnd(onCompletion: () -> Unit) {
        job.invokeOnCompletion { onCompletion() }
    }

    fun joinBlocking() = runBlocking {
        job.join()
    }
}

fun lanternaBoxScreen() = BoxScreen()

private fun updateScreen(
    screen: TerminalScreen,
    box: Box<*>,
    activeFocus: ActiveFocus,
    channel: SendChannel<RenderAction>
) {
    val edge = ScreenBoxEdge(screen)
    activeFocus.update(box, edge)
    runBlocking {
        screen.clear()
        screen.cursorPosition = null
        edge.bounds.map { col, row ->
            launch {
                val scope = ScreenSpot(screen, channel, edge, col, row, activeFocus.focusId ?: 0)
                box.render(scope)
            }
        }.joinAll()
    }
    screen.refresh()
}

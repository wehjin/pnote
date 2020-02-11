package pnote

import kotlinx.coroutines.Job
import pnote.scopes.ProjectorScope
import kotlin.coroutines.CoroutineContext

class Projector : ProjectorScope {
    override val coroutineContext: CoroutineContext =
        Job()

    override fun promptLine(prompt: String, errorSubject: String): String = print("$prompt: ").let {
        readLine() ?: error("Failed to read $errorSubject")
    }

    override fun screenError(error: String) = println("ERROR: $error")
    override fun screenLine(line: String) = println(line)
}
package pnote.scopes

import kotlinx.coroutines.CoroutineScope

interface ProjectorScope : CoroutineScope {
    fun promptLine(prompt: String, errorSubject: String): String
    fun screenError(error: String)
    fun screenLine(line: String = "")
}
package pnote.scopes

import kotlinx.coroutines.CoroutineScope

interface LineProjectorScope : CoroutineScope {
    fun promptLine(prompt: String, errorSubject: String): String
    fun screenError(error: String)
    fun screenLine(line: String = "")
}
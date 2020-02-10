package pnote.scopes

interface ProjectorScope {
    fun promptLine(prompt: String, subject: String): String
    fun screenError(error: String)
    fun screenLine(line: String = "")
}
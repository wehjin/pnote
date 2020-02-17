package pnote.tools

sealed class Banner {
    abstract val noteId: Long

    data class Basic(override val noteId: Long, val title: String) : Banner()
}
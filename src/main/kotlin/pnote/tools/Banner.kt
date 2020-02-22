package pnote.tools

import pnote.tools.security.plain.PlainDocument

sealed class Banner {
    abstract val noteId: Long

    data class Basic(override val noteId: Long, val plainDoc: PlainDocument) : Banner()
}
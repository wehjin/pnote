package pnote.tools

import pnote.projections.sandbox.randomId
import pnote.tools.security.plain.PlainDocument

sealed class Note {
    abstract val noteId: Long

    data class Basic(
        override val noteId: Long = randomId(),
        val plainDoc: PlainDocument
    ) : Note()
}
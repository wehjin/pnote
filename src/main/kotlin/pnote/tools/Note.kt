package pnote.tools

import pnote.projections.StringHandle
import pnote.projections.sandbox.randomId

sealed class Note {
    abstract val noteId: Long

    data class Basic(
        val title: StringHandle,
        val body: StringHandle = StringHandle(""),
        override val noteId: Long = randomId()
    ) : Note()
}
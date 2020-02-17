package pnote.tools

import pnote.projections.sandbox.randomId

sealed class Note {
    abstract val noteId: Long

    data class Basic(
        val title: String,
        val body: String = "",
        override val noteId: Long = randomId()
    ) : Note()
}
package pnote.tools

import pnote.projections.StringHandle

sealed class Banner {
    abstract val noteId: Long

    data class Basic(override val noteId: Long, val title: StringHandle) : Banner()
}
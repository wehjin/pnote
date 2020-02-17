package pnote.projections

import java.io.Closeable

class StringHandle(private val string: String) : Closeable {

    fun toCharSequence(): CharSequence = string

    override fun close() {
        // TODO: Replace string with string buffer and randomize it
    }
}
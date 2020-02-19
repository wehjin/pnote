package pnote.projections

import java.io.Closeable

class StringHandle(private var string: String) : Closeable {

    fun isNotEmpty(): Boolean = string.isNotEmpty()

    fun toCharSequence(): CharSequence = string

    fun set(chars: List<Char>, trim: Boolean = false) {
        string = chars.joinToString("").let { if (trim) it.trim() else it }
    }

    override fun close() {
        // TODO: Replace string with string buffer and randomize it
    }

}
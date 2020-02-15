package pnote.projections

class StringHandle(private val string: String) {
    fun toCharSequence(): CharSequence = string
}
package pnote.tools.security.plain

import java.io.Closeable
import java.nio.CharBuffer
import kotlin.random.Random

class PlainDocument(chars: CharArray) : Closeable {
    constructor(string: String) : this(string.toCharArray())

    override fun close() {
        charBuffer.indices.forEach { charBuffer.put(it, Random.nextInt().toChar()) }
    }

    fun asLines(): List<Line> {
        return paragraphs.map {
            Line(this, LineLength.Paragraph, it.start, it.end)
        }
    }

    data class Line(
        private val document: PlainDocument,
        val maxLength: LineLength, val start: Int, val end: Int
    ) {
        fun toCharSequence(): CharSequence = document.toCharSequence(start, end)
    }

    sealed class LineLength {
        object Paragraph : LineLength()
        data class Columns(val cols: Int) : LineLength()
    }

    private val paragraphs: List<Paragraph> by lazy {
        charBuffer.toHardBreaks().fold(emptyList<Paragraph>()) { results, breakIndex ->
            val start = if (results.isEmpty()) 0 else results.last().end + 1
            val nextResult = Paragraph(start, breakIndex)
            results + nextResult
        }
    }

    data class Paragraph(val start: Int, val end: Int)

    fun toCharSequence(): CharSequence {
        return charBuffer
    }

    fun toCharSequence(start: Int, end: Int): CharSequence {
        return charBuffer.subSequence(start until end)
    }

    private val charBuffer = CharBuffer.wrap(chars.copyOf())

}

private fun CharSequence.toHardBreaks(): List<Int> {
    val interior = this.foldIndexed(emptyList<Int>()) { i, newlines, char ->
        if (char == '\n') newlines + i else newlines
    }
    return interior + this.length
}

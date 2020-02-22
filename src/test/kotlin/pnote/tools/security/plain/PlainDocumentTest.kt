package pnote.tools.security.plain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PlainDocumentTest {

    private val paragraphs = listOf(
        "So now that you know the elements of a good cheer, let's hear one!",
        "",
        "What fun is there in making sense?"
    )

    private val doc = PlainDocument(paragraphs.joinToString("\n").toCharArray())

    @Test
    internal fun `document converts to char-sequence`() {
        val charSequence = doc.asCharSequence()
        assertEquals(paragraphs.joinToString("\n"), charSequence.toString())
    }

    @Test
    internal fun `document converts to full-paragraph lines when no width is provided`() {
        val lines = doc.asLines()
        assertEquals(paragraphs, lines.map { it.asCharSequence().toString() })
    }
}
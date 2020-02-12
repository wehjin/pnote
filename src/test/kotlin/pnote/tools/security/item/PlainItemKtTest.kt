package pnote.tools.security.item

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.charset.Charset

internal class PlainItemKtTest {

    @Test
    internal fun `bytes are available`() {
        val initValue = "hey"
        val value = plainItem(initValue).use { it.bytes.toString(Charset.defaultCharset()) }
        assertEquals(initValue, value)
    }

    @Test
    internal fun `using a plain-value randomizes its bytes`() {
        val before = "hey"
        val plainItem = plainItem(before)
        plainItem.use {}
        val after = plainItem.bytes.toString(Charset.defaultCharset())
        assert(after != before)
    }
}
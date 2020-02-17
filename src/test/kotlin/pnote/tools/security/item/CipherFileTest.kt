package pnote.tools.security.item

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

internal class CipherFileTest {

    @Test
    internal fun `cipher item has same id as plain`() {
        val dir = File(createTempDir(), "cipher-item-test").apply { mkdirs() }
        println("HOST: $dir")
        val password = "hey".toCharArray()
        val text = "Hello world!"
        val plainItem = plainItem(text)
        val cipherItem = writeCipherFile(dir, password, plainItem)
        assertEquals(plainItem.id, cipherItem.id)
    }

    @Test
    internal fun `visiting a cipher-item provides the original plain value`() {
        val dir = File(createTempDir(), "cipher-item-test").apply { mkdirs() }
        println("HOST: $dir")
        val password = "hey".toCharArray()
        val text = "Hello world!"
        val plainItem = plainItem(text)
        val cipherItem = writeCipherFile(dir, password, plainItem)
        val afterText = cipherItem.map(password, PlainType.Text) { plainValue }
        assertEquals(text, afterText)
    }

    @Test
    internal fun `visiting a cipher-item with an incorrect password throws an exception`() {
        val dir = createTempDir()
        val cipherItem = writeCipherFile(
            dir,
            "good".toCharArray(),
            plainItem("Hello world!")
        )
        assertThrows<IllegalStateException> {
            cipherItem.map("bad".toCharArray(), PlainType.Text) { plainValue }
        }
    }
}
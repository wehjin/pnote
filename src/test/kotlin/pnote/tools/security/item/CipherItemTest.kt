package pnote.tools.security.item

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

internal class CipherItemTest {

    @Test
    internal fun `cipher item has same id as plain`() {
        val dir = File(createTempDir(), "cipher-item-test").apply { mkdirs() }
        println("HOST: $dir")
        val password = "hey".toCharArray()
        val text = "Hello world!"
        val plainItem = plainItem(text)
        val cipherItem = cipherItem(dir, password, plainItem)
        assertEquals(plainItem.id, cipherItem.id)
    }

    @Test
    internal fun `visiting a cipher-item provides the original plain value`() {
        val dir = File(createTempDir(), "cipher-item-test").apply { mkdirs() }
        println("HOST: $dir")
        val password = "hey".toCharArray()
        val text = "Hello world!"
        val plainItem = plainItem(text)
        val cipherItem = cipherItem(dir, password, plainItem)
        val afterText = cipherItem.visit(password, ItemType.Text) { plainValue }
        assertEquals(text, afterText)
    }

    @Test
    internal fun `visiting a cipher-item with an incorrect password throws an exception`() {
        val dir = createTempDir()
        val cipherItem = cipherItem(
            dir,
            "good".toCharArray(),
            plainItem("Hello world!")
        )
        assertThrows<IllegalStateException> {
            cipherItem.visit("bad".toCharArray(), ItemType.Text) { plainValue }
        }
    }
}
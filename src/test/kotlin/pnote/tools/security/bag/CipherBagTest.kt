package pnote.tools.security.bag

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pnote.tools.security.item.PlainType

internal class CipherBagTest {

    private val dir = createTempDir("cipher-bag-test").also { println(it) }
    private val secret = "1234".toCharArray()
    private val bag = cipherBag(dir)

    @Test
    internal fun `bag starts empty`() {
        val values = bag.values(secret, PlainType.Text)
        assertEquals(0, values.size)
    }

    @Test
    internal fun `bag adds and removes items`() {
        val id = bag.writeCipher(secret, PlainType.Text, "dis")
        assertEquals("dis", bag.unwrap(id, secret, PlainType.Text))
        bag.remove(id, secret, PlainType.Text)
        assertNull(bag.unwrapOrNull(id, secret, PlainType.Text))
    }

    @Test
    internal fun `bag does not remove if the password is wrong`() {
        val id = bag.writeCipher(secret, PlainType.Text, "dis")
        assertEquals("dis", bag.unwrap(id, secret, PlainType.Text))
        assertThrows<IllegalStateException> { bag.remove(id, "4321".toCharArray(), PlainType.Text) }
    }

    @Test
    internal fun `bag maps items`() {
        bag.writeCipher(secret, PlainType.Text, "dis")
        bag.writeCipher(secret, PlainType.Text, "dat")
        val values = bag.map(secret, PlainType.Text) { plainValue }
        assertEquals(setOf("dis", "dat"), values)
    }

    @Test
    internal fun `bag replaces items`() {
        val id = bag.writeCipher(secret, PlainType.Text, "dis")
        val newId = bag.rewriteCipher(id, secret, PlainType.Text, "dat")
        assertEquals(id, newId)
        assertEquals("dat", bag.unwrap(newId, secret, PlainType.Text))
    }
}
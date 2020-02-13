package pnote.tools.security.bag

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pnote.tools.security.item.ItemType

internal class CipherBagTest {

    private val dir = createTempDir("cipher-bag-test").also { println(it) }
    private val secret = "1234".toCharArray()
    private val bag = cipherBag(dir)

    @Test
    internal fun `bag starts empty`() {
        val values = bag.values(secret, ItemType.Text)
        assertEquals(0, values.size)
    }

    @Test
    internal fun `bag adds and removes items`() {
        val id = bag.add(secret, ItemType.Text, "dis")
        assertEquals("dis", bag.get(id, secret, ItemType.Text))
        bag.remove(id, secret, ItemType.Text)
        assertNull(bag.getOrNull(id, secret, ItemType.Text))
    }

    @Test
    internal fun `bag does not remove if the password is wrong`() {
        val id = bag.add(secret, ItemType.Text, "dis")
        assertEquals("dis", bag.get(id, secret, ItemType.Text))
        assertThrows<IllegalStateException> { bag.remove(id, "4321".toCharArray(), ItemType.Text) }
    }

    @Test
    internal fun `bag maps items`() {
        bag.add(secret, ItemType.Text, "dis")
        bag.add(secret, ItemType.Text, "dat")
        val values = bag.map(secret, ItemType.Text) { plainValue }
        assertEquals(setOf("dis", "dat"), values)
    }

    @Test
    internal fun `bag replaces items`() {
        val id = bag.add(secret, ItemType.Text, "dis")
        val newId = bag.replace(id, secret, ItemType.Text, "dat")
        assertNull(bag.getOrNull(id, secret, ItemType.Text))
        assertEquals("dat", bag.get(newId, secret, ItemType.Text))
    }
}
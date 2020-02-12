package pnote.tools.security.bag

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import pnote.tools.security.item.ItemType

internal class CipherBagTest {

    private val dir = createTempDir("cipher-bag-test").also { println(it) }
    private val password = "1234".toCharArray()
    private val bag = cipherBag(dir)

    @Test
    internal fun `bag starts empty`() {
        val values = bag.values(password, ItemType.Text)
        assertEquals(0, values.size)
    }

    @Test
    internal fun `bag adds and removes items`() {
        val id = bag.add(password, ItemType.Text, "dis")
        assertEquals("dis", bag.get(id, password, ItemType.Text))
        bag.remove(id)
        assertNull(bag.getOrNull(id, password, ItemType.Text))
    }

    @Test
    internal fun `bag maps items`() {
        bag.add(password, ItemType.Text, "dis")
        bag.add(password, ItemType.Text, "dat")
        val values = bag.map(password, ItemType.Text) { plainValue }
        assertEquals(setOf("dis", "dat"), values)
    }

    @Test
    internal fun `bag replaces items`() {
        val id = bag.add(password, ItemType.Text, "dis")
        val newId = bag.replace(id, password, ItemType.Text, "dat")
        assertNull(bag.getOrNull(id, password, ItemType.Text))
        assertEquals("dat", bag.get(newId, password, ItemType.Text))
    }
}
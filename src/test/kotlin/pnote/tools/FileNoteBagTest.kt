package pnote.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FileNoteBagTest {

    private val dir = createTempDir("file-note-bag-test")
    private val secret = password("1234")
    private val cryptor = fileCryptor(dir).apply {
        importConfidential(secret)
        unlockConfidential(secret)
    }
    private val bag = FileNoteBag(dir, cryptor)

    @Test
    internal fun `bag starts empty`() {
        val (accessLevel, banners) = bag.readBanners()
        accessLevel as AccessLevel.ConfidentialUnlocked
        assertEquals(0, banners.size)
    }

    @Test
    internal fun `bag adds and removes notes`() {
        val noteId = bag.addNote(secret, Note.Basic("hello"))
        assertEquals(1, bag.readBanners().banners.size)

        bag.removeNote(noteId, secret)
        assertEquals(0, bag.readBanners().banners.size)
    }
}
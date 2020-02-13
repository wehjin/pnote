package pnote.tools

import pnote.tools.security.bag.CipherBag
import pnote.tools.security.item.ItemType
import java.io.File

class FileNoteBag(dir: File, private val cryptor: Cryptor) : NoteBag {

    override fun addNote(password: Password, note: Note): Long {
        val itemId = bag.add(
            password = password.chars,
            itemType = ItemType.Text,
            value = (note as Note.Basic).title,
            id = itemId(note.noteId)
        )
        return noteId(itemId)
    }

    override fun removeNote(noteId: Long, password: Password) {
        bag.remove(itemId(noteId), password.chars, ItemType.Text)
    }

    override fun readBanners(): ReadBannersResult =
        when (val accessLevel = cryptor.accessLevel) {
            AccessLevel.Empty -> ReadBannersResult(
                accessLevel,
                emptySet()
            )
            AccessLevel.ConfidentialLocked -> ReadBannersResult(
                accessLevel,
                emptySet()
            )
            is AccessLevel.ConfidentialUnlocked -> ReadBannersResult(
                accessLevel = accessLevel,
                banners = bag.map(
                    accessLevel.password.chars,
                    ItemType.Text
                ) {
                    Banner.Basic(noteId = itemId.toLong(16), title = plainValue)
                })
        }

    private val bag = CipherBag(dir)
}

fun itemId(noteId: Long) = noteId.toString(16)
fun noteId(itemId: String) = itemId.toLong(16)
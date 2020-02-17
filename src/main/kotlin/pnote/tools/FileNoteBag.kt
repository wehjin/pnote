package pnote.tools

import pnote.projections.StringHandle
import pnote.tools.security.bag.CipherBag
import pnote.tools.security.item.PlainType
import java.io.File

class FileNoteBag(dir: File, private val cryptor: Cryptor) : NoteBag {

    override fun createNote(password: Password, note: Note): Long {
        val itemId = bag.writeCipher(
            password = password.chars,
            plainType = PlainType.Text,
            value = toPlain(note),
            id = itemId(note.noteId)
        )
        return noteId(itemId)
    }

    override fun readNote(password: Password, noteId: Long): Note {
        val plain = bag.unwrap(itemId(noteId), password.chars, PlainType.Text)
        return toNote(noteId, plain)
    }

    override fun updateNote(password: Password, note: Note) {
        bag.rewriteCipher(
            id = itemId(note.noteId),
            password = password.chars,
            plainType = PlainType.Text,
            value = toPlain((note as Note.Basic))
        )
    }

    override fun deleteNote(noteId: Long, password: Password) {
        bag.remove(itemId(noteId), password.chars, PlainType.Text)
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
                banners = bag.map(accessLevel.password.chars, PlainType.Text) {
                    Banner.Basic(
                        noteId = noteId(itemId),
                        title = StringHandle(plainValue)
                    )
                })
        }

    private val bag = CipherBag(dir)
}

private fun toPlain(note: Note): String {
    return (note as Note.Basic).title.toCharSequence().toString()
}

private fun toNote(noteId: Long, plain: String): Note {
    return Note.Basic(
        noteId = noteId,
        title = StringHandle(plain),
        body = StringHandle(plain)
    )
}

private fun itemId(noteId: Long) = noteId.toString(16)
private fun noteId(itemId: String) = itemId.toLong(16)
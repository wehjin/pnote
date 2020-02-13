package pnote.tools

import kotlin.math.absoluteValue
import kotlin.random.Random

interface NoteBag {
    fun addNote(password: Password, note: Note): Long
    fun removeNote(noteId: Long, password: Password)
    fun readBanners(): ReadBannersResult
}

sealed class Note {
    abstract val noteId: Long

    data class Basic(
        val title: String,
        override val noteId: Long = Random.nextLong().absoluteValue
    ) : Note()
}

data class ReadBannersResult(
    val accessLevel: AccessLevel,
    val banners: Set<Banner>
)

sealed class Banner {
    abstract val noteId: Long

    data class Basic(override val noteId: Long, val title: String) : Banner()
}

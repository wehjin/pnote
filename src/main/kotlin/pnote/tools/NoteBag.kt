package pnote.tools

import pnote.tools.security.bag.CipherBag
import pnote.tools.security.item.ItemType
import java.io.File

interface NoteBag {
    fun readBanners(): ReadBannersResult
}

data class ReadBannersResult(
    val accessLevel: AccessLevel,
    val banners: Set<Banner>
)

sealed class Banner {
    abstract val noteId: Long

    data class Basic(override val noteId: Long, val title: String) : Banner()
}

class FileNoteBag(
    dir: File,
    private val cryptor: Cryptor
) : NoteBag {

    override fun readBanners(): ReadBannersResult =
        when (val accessLevel = cryptor.accessLevel) {
            AccessLevel.Empty -> ReadBannersResult(accessLevel, emptySet())
            AccessLevel.ConfidentialLocked -> ReadBannersResult(accessLevel, emptySet())
            is AccessLevel.ConfidentialUnlocked -> ReadBannersResult(
                accessLevel = accessLevel,
                banners = bag.map(accessLevel.password.chars, ItemType.Text) {
                    Banner.Basic(noteId = itemId.toLong(16), title = plainValue)
                })
        }

    private val bag = CipherBag(dir)
}
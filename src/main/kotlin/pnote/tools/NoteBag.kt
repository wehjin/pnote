package pnote.tools

import pnote.AccessLevel

interface NoteBag {
    fun readBanners(): ReadBannersResult
}

sealed class ReadBannersResult {
    object EmptyCryptor : ReadBannersResult()

    data class LockedCryptor(
        val accessLevel: AccessLevel
    ) : ReadBannersResult()

    data class Banners(
        val accessLevel: AccessLevel,
        val banners: Set<Banner>
    ) : ReadBannersResult()
}

sealed class Banner {
    abstract val itemId: Long

    data class Basic(override val itemId: Long, val title: String) : Banner()
}

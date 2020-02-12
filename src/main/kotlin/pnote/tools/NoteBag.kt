package pnote.tools

interface NoteBag {
    fun readBanners(): ReadBannersResult
}


data class ReadBannersResult(val accessLevel: AccessLevel, val banners: Set<Banner>)

sealed class Banner {
    abstract val noteId: Long

    data class Basic(override val noteId: Long, val title: String) : Banner()
}


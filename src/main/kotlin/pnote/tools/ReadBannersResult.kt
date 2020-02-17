package pnote.tools

data class ReadBannersResult(
    val accessLevel: AccessLevel,
    val banners: Set<Banner>
)
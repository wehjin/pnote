package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor

data class Rendition(
    var glyph: Char? = null,
    var foreColor: TextColor? = null,
    var backColor: TextColor? = null,
    var maxZ: Int = 1000
) {
    val isWritten: Boolean
        get() = glyph != null || foreColor != null || backColor != null

    fun update(glyph: Char?, foreColor: TextColor?, backColor: TextColor?, z: Int) {
        if (z <= maxZ) {
            glyph?.let { this.glyph = it }
            foreColor?.let { this.foreColor = it }
            backColor?.let { this.backColor = it }
            maxZ = z
        }
    }
}
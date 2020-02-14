package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.input.KeyStroke

fun SpotScope.withEdgeBounds(bounds: BoxBounds): SpotScope = object : SpotScope by this {
    override val edge: BoxEdge = this@withEdgeBounds.edge.withBounds(bounds)
}

interface SpotScope {
    val col: Int
    val row: Int
    val edge: BoxEdge
    val colorMinZ: Int
    val glyphMinZ: Int
    val activeFocusId: Long

    fun setColor(color: TextColor, colorZ: Int)
    fun setGlyph(glyph: Char, glyphColor: TextColor, glyphZ: Int)
    fun setChanged(bounds: BoxBounds)
    fun setCursor(col: Int, row: Int)
}


fun FocusScope.withEdgeBounds(bounds: BoxBounds): FocusScope = object : FocusScope by this {
    override val edge: BoxEdge = this@withEdgeBounds.edge.withBounds(bounds)
}

interface FocusScope {
    val edge: BoxEdge
    fun setChanged(bounds: BoxBounds)
    fun setFocusable(focusable: Focusable)
}

interface FocusKeyScope {
    val keyStroke: KeyStroke
    val edge: BoxEdge
    fun setChanged(bounds: BoxBounds)
}

data class Focusable(
    val focusableId: Long,
    val bounds: BoxBounds,
    val keyReader: KeyReader
)

interface KeyReader {
    val readerId: Long
    val handlesUpDown: Boolean
    fun receiveKey(keyStroke: KeyStroke)
}

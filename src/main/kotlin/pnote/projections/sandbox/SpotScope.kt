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
    fun setColor(color: TextColor, colorMinZ: Int)
    fun setGlyph(glyph: Char, glyphColor: TextColor, glyphMinZ: Int)
    fun addKeyReader(keyReader: KeyReader)
    fun setChanged(bounds: BoxBounds)
    fun setCursor(col: Int, row: Int)
}


interface KeyReader {
    val readerId: Long
    fun receiveKey(keyStroke: KeyStroke)
}

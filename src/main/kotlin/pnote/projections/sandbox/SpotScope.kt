package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.input.KeyStroke

fun SpotScope.withEdgeBounds(bounds: BoxBounds): SpotScope {
    return object : SpotScope by this {
        override val edge: BoxEdge = this@withEdgeBounds.edge.withBounds(bounds)
    }
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
    fun readSpark(spark: Spark, block: SparkReadScope.() -> Unit)
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

fun keyReader(id: Long, onKey: (keyStroke: KeyStroke) -> Boolean): KeyReader {
    return object : KeyReader {
        override val readerId: Long = id
        override fun receiveKey(keyStroke: KeyStroke) = onKey(keyStroke)
    }
}

interface KeyReader {
    val readerId: Long
    fun receiveKey(keyStroke: KeyStroke): Boolean
}

interface SparkReadScope {
    val spark: Spark
}

enum class Spark { Back }
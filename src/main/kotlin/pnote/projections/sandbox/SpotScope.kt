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

data class Focusable(
    val focusableId: Long,
    val bounds: BoxBounds,
    val role: FocusRole,
    val keyReader: KeyReader
) {
    fun rightDistanceTo(other: Focusable): Int = bounds.rightDistanceTo(other.bounds)
    fun leftDistanceTo(other: Focusable): Int = bounds.leftDistanceTo(other.bounds)

    fun chooseHorizontalNeighbor(
        neighbors: MutableMap<Long, Focusable>,
        measureDistance: (Focusable, Focusable) -> Int
    ): Long? {
        val (answer, _, _) = neighbors.values.fold(
            initial = Triple(null as Focusable?, Int.MIN_VALUE, Int.MAX_VALUE),
            operation = { (answer: Focusable?, answerOverlap: Int, answerDistance), next ->
                val nextOverlap = bounds.yOverlap(next.bounds)
                val nextDistance = measureDistance(this, next)
                when {
                    next == this -> Triple(answer, answerOverlap, answerDistance)
                    nextDistance < 0 -> Triple(answer, answerOverlap, answerDistance)
                    answer == null -> Triple(next, nextOverlap, nextDistance)
                    nextOverlap < answerOverlap -> Triple(answer, answerOverlap, answerDistance)
                    nextOverlap > answerOverlap -> Triple(next, nextOverlap, nextDistance)
                    else -> when {
                        nextDistance > answerDistance -> Triple(answer, answerOverlap, answerDistance)
                        else -> Triple(next, nextOverlap, nextDistance)
                    }
                }
            }
        )
        return answer?.focusableId
    }

}


enum class FocusRole { Edit, Submit }

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
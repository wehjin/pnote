package pnote.projections.sandbox

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

class SparkEmitter {
    private val sparkBlocks = mutableMapOf<Spark, SparkReadScope.() -> Unit>()

    fun addSpark(spark: Spark, reader: SparkReadScope.() -> Unit) {
        sparkBlocks[spark] = reader
    }

    fun clear() = sparkBlocks.clear()

    fun emitSpark(keyStroke: KeyStroke): Boolean {
        return keyStroke.asSpark()?.let { spark ->
            sparkBlocks[spark]?.let { block ->
                true.also {
                    val scope = object : SparkReadScope {
                        override val spark: Spark = spark
                    }
                    scope.run(block)
                }
            } ?: false
        } ?: false
    }

    private fun KeyStroke.asSpark(): Spark? {
        return when {
            keyType == KeyType.Escape && !isCtrlDown && !isShiftDown && !isAltDown -> Spark.Back
            else -> null
        }
    }
}
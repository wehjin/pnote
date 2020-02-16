package pnote.projections.sandbox

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import kotlinx.coroutines.channels.SendChannel
import java.lang.Integer.max
import java.lang.Integer.min

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

class ActiveFocus(private val channel: SendChannel<RenderAction>) {
    var focusId: Long? = null
    val focusables = mutableMapOf<Long, Focusable>()
    val sparkEmitter = SparkEmitter()

    fun update(box: Box<*>, edge: BoxEdge) {
        focusables.clear()
        sparkEmitter.clear()
        box.focus(object : FocusScope {
            override val edge: BoxEdge = edge

            override fun readSpark(spark: Spark, block: SparkReadScope.() -> Unit) {
                sparkEmitter.addSpark(spark, block)
            }

            override fun setFocusable(focusable: Focusable) {
                focusables[focusable.focusableId] = focusable
            }

            override fun setChanged(bounds: BoxBounds) {
                channel.offer(RenderAction.Refresh)
            }
        })
        focusId = when (val oldId = focusId) {
            null -> selectNewFocus(null, true)
            else -> if (focusables.containsKey(oldId)) oldId else selectNewFocus(oldId, true)
        }
    }

    fun routeKey(keyStroke: KeyStroke) {
        val emitted = sparkEmitter.emitSpark(keyStroke)
        if (!emitted) routeKeyToFocus(keyStroke)
    }

    private fun routeKeyToFocus(keyStroke: KeyStroke) {
        when (keyStroke.keyType) {
            KeyType.ArrowDown, KeyType.ArrowUp -> {
                val sendKeyToReader = keyReader?.handlesUpDown ?: false
                if (sendKeyToReader) {
                    keyReader?.receiveKey(keyStroke)
                } else when (keyStroke.keyType) {
                    KeyType.ArrowDown -> moveFocus(true)
                    KeyType.ArrowUp -> moveFocus(false)
                    else -> check(false)
                }
            }
            KeyType.Tab -> moveFocus(true)
            KeyType.ReverseTab -> moveFocus(false)
            else -> keyReader?.receiveKey(keyStroke)
        }
    }

    private fun moveFocus(forward: Boolean) {
        if (focusables.size >= 2) {
            val nextFocusId = selectNewFocus(focusId, forward)
            nextFocusId?.let {
                focusId = it
                channel.offer(RenderAction.Refresh)
            }
        }
    }

    private fun selectNewFocus(blurId: Long?, forward: Boolean): Long? {
        return if (blurId == null) {
            focusables.keys.firstOrNull()
        } else {
            val sorted = focusables.values.sortedBy { it.bounds.top }
            val oldInSorted = sorted.find { it.focusableId == blurId }
            if (oldInSorted == null) {
                // Could do something like store old-focus' top so it is available here
                // even when old-focus is gone.
                sorted.firstOrNull()?.focusableId
            } else {
                val index = sorted.indexOf(oldInSorted)
                val nextIndex = index + if (forward) 1 else -1
                val confinedIndex = min(sorted.lastIndex, max(0, nextIndex))
                sorted[confinedIndex].focusableId
            }
        }
    }

    private val keyReader: KeyReader?
        get() = focusId?.let { focusables[it]?.keyReader }
}
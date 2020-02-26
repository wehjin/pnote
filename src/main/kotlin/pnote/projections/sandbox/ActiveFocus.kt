package pnote.projections.sandbox

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import kotlinx.coroutines.channels.SendChannel
import java.lang.Integer.max
import java.lang.Integer.min

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
            KeyType.ArrowDown, KeyType.ArrowUp -> routeVerticalArrow(keyStroke)
            KeyType.ArrowRight -> routeHorizontalArrow(keyStroke, Focusable::rightDistanceTo)
            KeyType.ArrowLeft -> routeHorizontalArrow(keyStroke, Focusable::leftDistanceTo)
            KeyType.Tab -> moveFocusLinear(true)
            KeyType.ReverseTab -> moveFocusLinear(false)
            else -> keyReader?.receiveKey(keyStroke)
        }
    }

    private fun routeHorizontalArrow(keyStroke: KeyStroke, measureDistance: (Focusable, Focusable) -> Int) {
        val sunk = keyReader?.receiveKey(keyStroke) ?: false
        if (!sunk) {
            moveFocusToHorizontalNeighbor(measureDistance)
        }
    }

    private fun moveFocusToHorizontalNeighbor(measureDistance: (Focusable, Focusable) -> Int) {
        val currentFocus = focusId?.let { focusables[it] }
        currentFocus?.let { start ->
            val nextFocusId = start.chooseHorizontalNeighbor(focusables, measureDistance)
            println("NEXT FOCUS ID: $nextFocusId")
            nextFocusId?.let {
                focusId = it
                channel.offer(RenderAction.Refresh)
            }
        }
    }

    private fun routeVerticalArrow(keyStroke: KeyStroke) {
        val taken = keyReader?.receiveKey(keyStroke) ?: false
        if (!taken) {
            when (keyStroke.keyType) {
                KeyType.ArrowDown -> moveFocusLinear(true)
                KeyType.ArrowUp -> moveFocusLinear(false)
                else -> check(false)
            }
        }
    }

    private fun moveFocusLinear(forward: Boolean) {
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
            val (edits, others) = focusables.values.partition { it.role == FocusRole.Edit }
            val search = if (edits.isNotEmpty()) edits else others
            search.minBy { it.bounds.top }?.focusableId
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

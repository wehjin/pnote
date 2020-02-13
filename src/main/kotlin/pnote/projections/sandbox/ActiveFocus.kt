package pnote.projections.sandbox

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import kotlinx.coroutines.channels.SendChannel
import java.lang.Integer.max
import java.lang.Integer.min

class ActiveFocus(private val channel: SendChannel<LanternaProjector.RenderAction>) {
    var focusId: Long? = null
    val focusables = mutableMapOf<Long, Focusable>()

    private val keyReader: KeyReader?
        get() = focusId?.let { focusables[it]?.keyReader }

    fun selectFocus() {
        focusId = when (val oldId = focusId) {
            null -> selectNewFocus(null, true)
            else -> if (focusables.containsKey(oldId)) oldId else selectNewFocus(oldId, true)
        }
    }

    fun routeKey(keyStroke: KeyStroke) {
        when (keyStroke.keyType) {
            KeyType.Tab, KeyType.ArrowDown -> moveFocus(true)
            KeyType.ReverseTab, KeyType.ArrowUp -> moveFocus(false)
            else -> keyReader?.receiveKey(keyStroke)
        }
    }

    private fun moveFocus(forward: Boolean) {
        if (focusables.size >= 2) {
            val nextFocusId = selectNewFocus(focusId, forward)
            nextFocusId?.let {
                focusId = it
                channel.offer(LanternaProjector.RenderAction.Refresh)
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
}
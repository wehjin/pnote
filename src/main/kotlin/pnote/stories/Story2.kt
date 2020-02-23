@file:Suppress("EXPERIMENTAL_API_USAGE")

package pnote.stories

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import pnote.projections.sandbox.randomId
import pnote.scopes.AppScope

inline fun <reified T : Any> AppScope.story2(
    crossinline first: (story: Story2<T>) -> T,
    crossinline last: (Any) -> Boolean
): Story2<T> {
    val visions = ConflatedBroadcastChannel<T>()
    return object : Story2<T> {
        override val appScope: AppScope = this@story2
        override fun subscribe(): ReceiveChannel<T> = visions.openSubscription()
        override fun update(nextVision: T): Boolean = visions.offer(nextVision)

        override val name: String = T::class.java.simpleName
        override val number: Long = randomId()
        override fun isStoryOver(vision: T): Boolean = last(vision)
        override fun offer(action: Any): Unit = error("Do not use")

        init {
            visions.offer(first(this))
        }
    }
}

interface Story2<T : Any> : Story<T> {
    val appScope: AppScope
    override fun subscribe(): ReceiveChannel<T>
    fun update(nextVision: T): Boolean
}

fun <T : Any> Story<T>.onEnding(block: (ending: T) -> Unit): Job {
    return GlobalScope.launch {
        for (vision in subscribe()) {
            if (isStoryOver(vision)) {
                block.invoke(vision)
                break
            }
        }
    }
}

fun <T : Any> Story2<T>.onEnding(block: (ending: T) -> Unit): Job {
    return GlobalScope.launch {
        for (vision in subscribe()) {
            if (isStoryOver(vision)) {
                block.invoke(vision)
                break
            }
        }
    }
}

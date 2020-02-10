package story.core

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.runBlocking

val neverEnds = { _: Any -> false }

suspend fun <T, V : Any> Story<V>.firstNotNull(filter: (V) -> T?): T? {
    var found: T? = null
    for (vision in subscribe()) {
        val filterResult = filter(vision)
        if (filterResult != null) {
            found = filterResult
            break
        }
    }
    return found
}

fun <V : Any> Story<V>.awaitEnding(): V = runBlocking {
    lateinit var ending: V
    for (vision in subscribe()) {
        if (isStoryOver(vision)) {
            ending = vision
            break
        }
    }
    ending
}

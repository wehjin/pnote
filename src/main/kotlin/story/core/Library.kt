package story.core

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.withTimeout

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

suspend fun <V : Any, F : Any> Story<V>.scanVisions(timeout: Long, filter: (V) -> F?): F = withTimeout(timeout) {
    lateinit var found: F
    for (vision in subscribe()) {
        val result = filter(vision)
        if (result != null) {
            found = result
            break
        }
    }
    found
}

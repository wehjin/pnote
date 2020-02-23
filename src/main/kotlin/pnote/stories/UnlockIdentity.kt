@file:Suppress("EXPERIMENTAL_API_USAGE")

package pnote.stories

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pnote.projections.sandbox.randomId
import pnote.scopes.AppScope
import pnote.tools.Cryptor
import pnote.tools.Password


fun AppScope.unlockIdentityStory(): Story2<UnlockIdentity> {
    return story2(
        first = { UnlockIdentity.Unlocking(story = it, edition = randomId() / 2 + 1, nameError = null) },
        last = { it is UnlockIdentity.Done }
    )
}

sealed class UnlockIdentity {
    abstract val story: Story2<UnlockIdentity>

    val name: String
        get() = "UnlockIdentity/${this.javaClass.simpleName}"

    val cryptor: Cryptor
        get() = story.appScope.cryptor

    data class Unlocking(
        override val story: Story2<UnlockIdentity>,
        val edition: Long,
        val nameError: String?
    ) : UnlockIdentity()

    data class Done(
        override val story: Story2<UnlockIdentity>,
        val wasCancelled: Boolean
    ) : UnlockIdentity()
}

fun UnlockIdentity.Unlocking.setSolName(name: String, secret: CharArray): Job {
    return GlobalScope.launch {
        val nameMatch = nameTest.matchEntire(name.trim())
        val nextVision = if (nameMatch != null) {
            val solName = SolName(name, secret.copyOf())
            // TODO Send sunName itself to cryptor.
            cryptor.unlockConfidential(solName.toPassword())
            UnlockIdentity.Done(story, false)
        } else {
            this@setSolName.copy(edition = edition + 1, nameError = "Invalid name. Ex: jill-lee")
        }
        story.update(nextVision)
    }
}

fun UnlockIdentity.cancel(): Job {
    return GlobalScope.launch {
        story.update(UnlockIdentity.Done(story, true))
    }
}

class SolName(val name: String, private val secret: CharArray) {
    init {
        require(nameTest.matchEntire(name) != null)
    }

    fun toPassword(): Password = Password(secret)
}

private val nameTest = Regex("[a-zA-Z]+([-][a-zA-Z]+)*")

fun isValidSolNamePrefix(chars: CharSequence): Boolean {
    return nameTest.matchEntire(chars) != null
}

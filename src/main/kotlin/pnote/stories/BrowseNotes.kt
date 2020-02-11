package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.AccessLevel
import pnote.scopes.AppScope
import pnote.tools.Banner
import pnote.tools.ReadBannersResult.*

fun AppScope.browseNotes(): Story<BrowseNotesVision> = matchingStory(
    name = "BrowseNotes",
    isLastVision = { it is FinishedBrowsingNotes },
    toFirstVision = {
        when (val result = noteBag.readBanners()) {
            EmptyCryptor -> TODO()
            is LockedCryptor -> TODO()
            is Banners -> BrowsingNotes(offer, result.accessLevel, result.banners)
        }
    },
    updateRules = {
        onAction<Action.Cancel, BrowseNotesVision> {
            FinishedBrowsingNotes
        }
    }
)

sealed class BrowseNotesVision(
    private val offer: ((Any) -> Boolean)? = null
) {
    fun cancel() = offer?.invoke(Action.Cancel)
}

class BrowsingNotes(
    offer: (Any) -> Boolean,
    val accessLevel: AccessLevel,
    val banners: Set<Banner>
) : BrowseNotesVision(offer)

object FinishedBrowsingNotes : BrowseNotesVision()


private sealed class Action {
    object Cancel : Action()
}
package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes.*
import pnote.stories.BrowseNotesAction.Cancel
import pnote.tools.AccessLevel
import pnote.tools.Banner

fun AppScope.browseNotes(): Story<BrowseNotes> = matchingStory(
    name = "BrowseNotes",
    isLastVision = { it is Finished },
    toFirstVision = {
        val (accessLevel, banners) = noteBag.readBanners()
        when (accessLevel) {
            AccessLevel.Empty -> Importing(offer)
            AccessLevel.ConfidentialLocked -> Unlocking(offer)
            AccessLevel.ConfidentialUnlocked -> Browsing(offer, banners)
            AccessLevel.Secret -> TODO()
        }
    },
    updateRules = {
        onAction<Cancel, BrowseNotes> { Finished }
    }
)

sealed class BrowseNotes(private val offer: ((Any) -> Boolean)? = null) {

    fun cancel() = offer?.invoke(Cancel)

    class Importing(
        offer: (Any) -> Boolean
    ) : BrowseNotes(offer)

    class Unlocking(
        offer: (Any) -> Boolean
    ) : BrowseNotes(offer)

    class Browsing(
        offer: (Any) -> Boolean,
        val banners: Set<Banner>
    ) : BrowseNotes(offer)

    object Finished : BrowseNotes()
}


private sealed class BrowseNotesAction {
    object Cancel : BrowseNotesAction()
}

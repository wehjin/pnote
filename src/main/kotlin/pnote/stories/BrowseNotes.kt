package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes.Browsing
import pnote.stories.BrowseNotes.Finished
import pnote.stories.BrowseNotesAction.Cancel
import pnote.tools.AccessLevel
import pnote.tools.Banner
import pnote.tools.ReadBannersResult.*

fun AppScope.browseNotes(): Story<BrowseNotes> = matchingStory(
    name = "BrowseNotes",
    isLastVision = { it is Finished },
    toFirstVision = {
        when (val result = noteBag.readBanners()) {
            EmptyCryptor -> TODO()
            is LockedCryptor -> TODO()
            is Banners -> Browsing(offer, result.accessLevel, result.banners)
        }
    },
    updateRules = {
        onAction<Cancel, BrowseNotes> { Finished }
    }
)

sealed class BrowseNotes(private val offer: ((Any) -> Boolean)? = null) {

    fun cancel() = offer?.invoke(Cancel)

    class Browsing(
        offer: (Any) -> Boolean,
        val accessLevel: AccessLevel,
        val banners: Set<Banner>
    ) : BrowseNotes(offer)

    object Finished : BrowseNotes()
}


private sealed class BrowseNotesAction {
    object Cancel : BrowseNotesAction()
}

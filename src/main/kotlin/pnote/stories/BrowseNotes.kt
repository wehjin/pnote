package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.StoryInitScope
import com.rubyhuntersky.story.core.scopes.offerWhenStoryEnds
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes.*
import pnote.stories.BrowseNotesAction.Cancel
import pnote.stories.BrowseNotesAction.Reload
import pnote.tools.AccessLevel.*
import pnote.tools.Banner

fun AppScope.browseNotes(): Story<BrowseNotes> = matchingStory(
    name = "BrowseNotes",
    isLastVision = { it is Finished },
    toFirstVision = { init(this) },
    updateRules = {
        onAction<Cancel, BrowseNotes> {
            Finished
        }
        onAction<Reload, BrowseNotes> {
            init(this)
        }
    }
)

private fun AppScope.init(storyInitScope: StoryInitScope<BrowseNotes>): BrowseNotes =
    noteBag.readBanners().let { (accessLevel, banners) ->
        when (accessLevel) {
            Empty -> Importing(
                storyInitScope.offer,
                importPassword()
                    .also { storyInitScope.offerWhenStoryEnds(it) { Reload } }
            )
            ConfidentialLocked -> Unlocking(
                storyInitScope.offer,
                unlockConfidential()
                    .also {
                        storyInitScope.offerWhenStoryEnds(it) {
                            val unlockWasCancelled = (ending as UnlockConfidential.Finished).wasCancelled
                            if (unlockWasCancelled) Cancel else Reload
                        }
                    }
            )
            is ConfidentialUnlocked -> Browsing(storyInitScope.offer, banners)
        }
    }

sealed class BrowseNotes(private val offer: ((Any) -> Boolean)? = null) {

    fun cancel() = offer?.invoke(Cancel)

    class Importing(
        offer: (Any) -> Boolean,
        val substory: Story<ImportPasswordVision>
    ) : BrowseNotes(offer)

    class Unlocking(
        offer: (Any) -> Boolean,
        val substory: Story<UnlockConfidential>
    ) : BrowseNotes(offer)

    class Browsing(
        offer: (Any) -> Boolean,
        val banners: Set<Banner>
    ) : BrowseNotes(offer)

    object Finished : BrowseNotes()
}

private sealed class BrowseNotesAction {
    object Cancel : BrowseNotesAction()
    object Reload : BrowseNotesAction()
}

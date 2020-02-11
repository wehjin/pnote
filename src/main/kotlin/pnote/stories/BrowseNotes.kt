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
import pnote.tools.AccessLevel
import pnote.tools.Banner

fun AppScope.browseNotes(): Story<BrowseNotes> = matchingStory(
    name = "BrowseNotes",
    isLastVision = { it is Finished },
    toFirstVision = { initBrowseNotes(this) },
    updateRules = {
        onAction<Cancel, BrowseNotes> { Finished }
        onAction<Reload, BrowseNotes> { initBrowseNotes(this) }
    }
)

private fun AppScope.initBrowseNotes(
    storyInitScope: StoryInitScope<BrowseNotes>
): BrowseNotes = noteBag.readBanners().let {
    val (accessLevel, banners) = it
    when (accessLevel) {
        AccessLevel.Empty -> Importing(storyInitScope.offer, importPassword()).also {
            storyInitScope.offerWhenStoryEnds(it.substory) { Reload }
        }
        AccessLevel.ConfidentialLocked -> Unlocking(storyInitScope.offer, unlockConfidential()).also {
            storyInitScope.offerWhenStoryEnds(it.substory) { Reload }
        }
        AccessLevel.ConfidentialUnlocked -> Browsing(storyInitScope.offer, banners)
        AccessLevel.Secret -> TODO()
    }
}

sealed class BrowseNotes(private val offer: ((Any) -> Boolean)? = null) {

    fun cancel() = offer?.invoke(Cancel)

    class Importing(
        offer: (Any) -> Boolean,
        val substory: Story<ImportPassword.Vision>
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

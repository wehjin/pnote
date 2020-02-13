package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.StoryInitScope
import com.rubyhuntersky.story.core.scopes.offerWhenStoryEnds
import com.rubyhuntersky.story.core.scopes.on
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes.*
import pnote.stories.BrowseNotesAction.*
import pnote.tools.AccessLevel.*
import pnote.tools.Banner
import pnote.tools.Note
import pnote.tools.Password

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
        on<AddNote, BrowseNotes, Browsing> {
            noteBag.addNote(vision.password, Note.Basic(action.title))
            init(this)
        }
    }
)

private fun AppScope.init(storyInitScope: StoryInitScope<BrowseNotes>): BrowseNotes =
    noteBag.readBanners().let { (accessLevel, banners) ->
        when (accessLevel) {
            Empty -> {
                val substory = importPassword().also { storyInitScope.offerWhenStoryEnds(it) { Reload } }
                Importing(storyInitScope.offer, substory)
            }
            ConfidentialLocked -> {
                val substory = unlockConfidential().also {
                    storyInitScope.offerWhenStoryEnds(it) {
                        val unlockWasCancelled = (ending as UnlockConfidential.Finished).wasCancelled
                        if (unlockWasCancelled) Cancel else Reload
                    }
                }
                Unlocking(storyInitScope.offer, substory)
            }
            is ConfidentialUnlocked -> {
                Browsing(storyInitScope.offer, accessLevel.password, banners)
            }
        }
    }

sealed class BrowseNotes(protected val offer: ((Any) -> Boolean)? = null) {

    fun cancel() = offer?.invoke(Cancel)

    class Importing(offer: (Any) -> Boolean, val substory: Story<ImportPasswordVision>) : BrowseNotes(offer)
    class Unlocking(offer: (Any) -> Boolean, val substory: Story<UnlockConfidential>) : BrowseNotes(offer)

    class Browsing(
        offer: (Any) -> Boolean,
        val password: Password,
        val banners: Set<Banner>
    ) : BrowseNotes(offer) {
        fun addNote(title: String) = offer?.invoke(AddNote(title))
    }

    object Finished : BrowseNotes()
}

private sealed class BrowseNotesAction {
    object Cancel : BrowseNotesAction()
    object Reload : BrowseNotesAction()
    data class AddNote(val title: String) : BrowseNotesAction()
}

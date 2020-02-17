package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.StoryInitScope
import com.rubyhuntersky.story.core.scopes.offerWhenStoryEnds
import com.rubyhuntersky.story.core.scopes.on
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.projections.StringHandle
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes.*
import pnote.stories.BrowseNotesAction.*
import pnote.tools.AccessLevel.*
import pnote.tools.Banner
import pnote.tools.Note
import pnote.tools.Password

fun AppScope.browseNotesStory(): Story<BrowseNotes> = matchingStory(
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
            val title = StringHandle(action.title)
            title.use { noteBag.createNote(vision.password, Note.Basic(title)) }
            init(this)
        }
        on<ViewNote, BrowseNotes, Browsing> {
            val banner = vision.banners.firstOrNull { it.noteId == action.noteId } as? Banner.Basic
            if (banner == null) vision
            else {
                val substory = noteDetailsStory(vision.password, action.noteId, banner.title)
                whenSubstoryEnds(substory) { offer(Reload) }
                AwaitingDetails(substory)
            }
        }
    }
)

private fun AppScope.init(storyInitScope: StoryInitScope<BrowseNotes>): BrowseNotes =
    noteBag.readBanners().let { (accessLevel, banners) ->
        when (accessLevel) {
            Empty -> {
                val substory = importPassword().also { storyInitScope.offerWhenStoryEnds(it) { Reload } }
                Importing(substory)
            }
            ConfidentialLocked -> {
                val substory = unlockConfidential().also {
                    storyInitScope.offerWhenStoryEnds(it) {
                        val unlockWasCancelled = (ending as UnlockConfidential.Finished).wasCancelled
                        if (unlockWasCancelled) Cancel else Reload
                    }
                }
                Unlocking(substory)
            }
            is ConfidentialUnlocked -> Browsing(accessLevel.password, banners)
        }
    }

sealed class BrowseNotes() {
    class Importing(val substory: Story<ImportPasswordVision>) : BrowseNotes()
    data class Unlocking(val substory: Story<UnlockConfidential>) : BrowseNotes()
    data class AwaitingDetails(val substory: Story<NoteDetails>) : BrowseNotes()
    object Finished : BrowseNotes()
    class Browsing(val password: Password, val banners: Set<Banner>) : BrowseNotes() {
        fun addNote(title: String): Any = AddNote(title)
        fun viewNote(noteId: Long): Any = ViewNote(noteId)
    }

    fun cancel(): Any = Cancel
}

private sealed class BrowseNotesAction {

    object Cancel : BrowseNotesAction()
    object Reload : BrowseNotesAction()
    data class AddNote(val title: String) : BrowseNotesAction()
    data class ViewNote(val noteId: Long) : BrowseNotesAction()
}

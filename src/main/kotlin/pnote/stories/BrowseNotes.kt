package pnote.stories

import com.rubyhuntersky.story.core.Story
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes.*
import pnote.tools.AccessLevel.ConfidentialLocked
import pnote.tools.AccessLevel.ConfidentialUnlocked
import pnote.tools.Note
import pnote.tools.Password
import pnote.tools.security.plain.PlainDocument


fun AppScope.browseNotesStory(): Story2<BrowseNotes> {
    return story2(first = ::initBrowseNotes, last = { it is Finished })
}

sealed class BrowseNotes(appScope: AppScope) : AppScope by appScope {

    fun cancel() {
        story.update(Finished(this, story))
    }

    abstract val story: Story2<BrowseNotes>

    class Finished(
        appScope: AppScope,
        override val story: Story2<BrowseNotes>
    ) : BrowseNotes(appScope)

    class Unlocking(
        appScope: AppScope,
        override val story: Story2<BrowseNotes>,
        val substory: Story2<UnlockIdentity>
    ) : BrowseNotes(appScope)

    class Browsing(
        appScope: AppScope,
        override val story: Story2<BrowseNotes>,
        val password: Password,
        val notes: Set<Note.Basic>,
        val selectedNote: Long?
    ) : BrowseNotes(appScope)

    class Editing(
        appScope: AppScope,
        override val story: Story2<BrowseNotes>,
        val substory: Story<EditNote>
    ) : BrowseNotes(appScope)
}

fun Browsing.editNote(noteId: Long) {
    val substory = editNoteStory(password, noteId).apply {
        onEnding {
            val nextVision = initBrowseNotes(story)
            story.update(nextVision)
        }
    }
    story.update(Editing(this, story, substory))
}

fun Browsing.addNote(title: String) {
    val note = Note.Basic(plainDoc = PlainDocument(title.toCharArray()))
    noteBag.createNote(password, note)
    story.update(initBrowseNotes(story))
}

fun Browsing.viewNote(noteId: Long) {
    val next = when (notes.firstOrNull { it.noteId == noteId }) {
        null -> this
        else -> Browsing(this, story, password, notes, noteId)
    }
    story.update(next)
}

private fun AppScope.initBrowseNotes(story: Story2<BrowseNotes>): BrowseNotes {
    return noteBag.readNotes().let { (accessLevel, notes) ->
        when (accessLevel) {
            ConfidentialLocked -> {
                val substory = unlockIdentityStory().apply {
                    onEnding { ending ->
                        val nextVision = when ((ending as UnlockIdentity.Done).wasCancelled) {
                            true -> Finished(this@initBrowseNotes, story)
                            else -> initBrowseNotes(story)
                        }
                        story.update(nextVision)
                    }
                }
                Unlocking(this, story, substory)
            }
            is ConfidentialUnlocked -> Browsing(this, story, accessLevel.password, notes, null)
        }
    }
}

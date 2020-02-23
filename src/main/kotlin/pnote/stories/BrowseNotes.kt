package pnote.stories

import com.rubyhuntersky.story.core.Story
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes.*
import pnote.tools.AccessLevel.*
import pnote.tools.Banner
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

    class Importing(
        appScope: AppScope,
        override val story: Story2<BrowseNotes>,
        val substory: Story<ImportPasswordVision>
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
        val banners: Set<Banner>
    ) : BrowseNotes(appScope)

    class AwaitingDetails(
        appScope: AppScope,
        override val story: Story2<BrowseNotes>,
        val substory: Story<NoteDetails>
    ) : BrowseNotes(appScope)
}

private fun AppScope.initBrowseNotes(story: Story2<BrowseNotes>): BrowseNotes {
    return noteBag.readBanners().let { (accessLevel, banners) ->
        when (accessLevel) {
            Empty -> {
                val substory = importPasswordStory().apply {
                    onEnding {
                        val nextVision = initBrowseNotes(story)
                        story.update(nextVision)
                    }
                }
                Importing(this, story, substory)
            }
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
            is ConfidentialUnlocked -> Browsing(this, story, accessLevel.password, banners)
        }
    }
}

fun Browsing.addNote(title: String) {
    val note = Note.Basic(plainDoc = PlainDocument(title.toCharArray()))
    noteBag.createNote(password, note)
    story.update(initBrowseNotes(story))
}

fun Browsing.viewNote(noteId: Long) {
    val banner = banners.firstOrNull { it.noteId == noteId } as? Banner.Basic
    val next = if (banner == null) this else {
        val substory = noteDetailsStory(password, noteId, banner.plainDoc).apply {
            onEnding {
                val nextVision = initBrowseNotes(story)
                story.update(nextVision)
            }
        }
        AwaitingDetails(this, story, substory)
    }
    story.update(next)
}

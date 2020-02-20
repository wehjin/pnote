package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.runBlocking
import pnote.projections.sandbox.*
import pnote.stories.BrowseNotes
import pnote.tools.Banner

fun BoxContext.projectBrowseNotes(story: Story<BrowseNotes>) {
    runBlocking {
        var subProjection: SubProjection? = null
        fun isSubProjection(name: String): Boolean = (subProjection?.name ?: "") == name
        fun clearSubProjection(name: String? = null) {
            if (name == null || subProjection?.name == name) {
                subProjection = null
            }
        }
        visionLoop@ for (vision in story.subscribe()) {
            println("${story.name}: $vision")
            when (vision) {
                BrowseNotes.Finished -> break@visionLoop
                is BrowseNotes.Unlocking -> projectUnlockIdentity(vision.substory)
                is BrowseNotes.Browsing -> projectBrowsing(story, vision, boxScreen).also { clearSubProjection() }
                is BrowseNotes.Importing -> boxScreen.setBox(messageBox("$vision", surfaceSwatch))
                is BrowseNotes.AwaitingDetails ->
                    if (!isSubProjection(vision.substory.name)) {
                        subProjection = projectNoteDetails(vision.substory)
                    }
            }
        }
    }
    boxScreen.close()
}

fun BoxContext.projectBrowsing(story: Story<BrowseNotes>, browsing: BrowseNotes.Browsing, boxScreen: BoxScreen) {
    val pageSwatch = primaryDarkSwatch
    val pageTitle = labelBox("CONFIDENTIAL", pageSwatch.strokeColor, Snap.TOP_RIGHT).pad(1)
    val pageBackground = fillBox(pageSwatch.fillColor)
    val pageUnderlay = pageTitle.before(pageBackground)

    val banners = browsing.banners.toList()
    // TODO: Make items a List<StringHandle>
    val items = banners.map { (it as Banner.Basic).title.toCharSequence().toString() } + "Add Note"
    val itemList = listBox(items) { index, _ ->
        when (index) {
            items.lastIndex -> story.offer(browsing.addNote("Another note"))
            else -> story.offer(browsing.viewNote(noteId = banners[index].noteId))
        }
    }
    val page = itemList.maxWidth(50).before(pageUnderlay)
    boxScreen.setBox(page)
}

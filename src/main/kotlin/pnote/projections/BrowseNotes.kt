package pnote.projections

import pnote.projections.sandbox.*
import pnote.stories.BrowseNotes
import pnote.tools.Banner

fun BoxContext.projectBrowsing(browsing: BrowseNotes.Browsing, boxScreen: BoxScreen) {
    val pageSwatch = primaryDarkSwatch
    val pageTitle = labelBox("CONFIDENTIAL", pageSwatch.strokeColor, Snap.TOP_RIGHT).pad(1)
    val pageBackground = fillBox(pageSwatch.fillColor)
    val pageUnderlay = pageTitle.before(pageBackground)

    val items = browsing.banners.map { (it as Banner.Basic).title } + "Add Note"
    val itemList = listBox(items) { index ->
        when (index) {
            0 -> browsing.cancel()
            items.lastIndex -> browsing.addNote("Another note")
            else -> println("SELECTED ITEM: ${index + 1}")
        }
    }
    val page = itemList.maxWidth(50).before(pageUnderlay)
    boxScreen.setBox(page)
}

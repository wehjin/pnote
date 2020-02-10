@file:Suppress("EXPERIMENTAL_API_USAGE")

package pnote

import com.rubyhuntersky.story.core.matchingStory
import pnote.scopes.AppScope
import story.core.neverEnds

fun AppScope.browseNotesStory() = matchingStory(
    name = "BrowseNotes",
    init = Unit,
    isOver = neverEnds,
    updateRules = {

    }
)

sealed class BrowseNotesVision

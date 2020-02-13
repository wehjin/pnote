package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.launch
import pnote.scopes.ProjectorScope
import pnote.stories.BrowseNotes
import pnote.stories.BrowseNotes.*
import pnote.tools.Banner

fun ProjectorScope.projectBrowseNotes(story: Story<BrowseNotes>) = launch {
    visionLoop@ for (vision in story.subscribe()) {
        when (vision) {
            is Importing -> projectImportPassword(vision.substory)
            is Unlocking -> projectUnlockConfidential(vision.substory)
            is Browsing -> projectBrowsing(vision)
            Finished -> break@visionLoop
        }
    }
}

private fun ProjectorScope.projectBrowsing(vision: Browsing) {
    screenLine()
    vision.banners.forEachIndexed { i, banner ->
        banner as Banner.Basic
        val message = banner.title
        screenLine("${i + 1}:\u2002$message")
    }
    commandLoop@ for (line in generateSequence { promptLine("CONFIDENTIAL", "command") }) {
        val endLoop = when (val command = command(line)) {
            "", "done", "quit", "exit", "lock", "bye" -> true.also { vision.cancel() }
            "add" -> true.also { vision.addNote(line.substring(command.length)) }
            else -> false.also { screenError("Hmm. I fail to understand the significance of this command '$line'") }
        }
        if (endLoop) break@commandLoop
    }
}

private fun command(line: String): String = line.split(" ").firstOrNull()?.trim() ?: ""

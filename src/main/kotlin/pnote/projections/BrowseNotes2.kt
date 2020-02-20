@file:Suppress("EXPERIMENTAL_API_USAGE")

package pnote.projections

import com.googlecode.lanterna.TextColor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.mainBoxContext
import pnote.projections.sandbox.*

fun main() {
    val job = mainBoxContext().projectBrowseNotes2()
    runBlocking {
        job.join()
    }
}

fun BoxContext.projectBrowseNotes2(): Job {
    val visions = ConflatedBroadcastChannel<List<Unit>>(emptyList())
    return GlobalScope.launch {
        for (notes in visions.openSubscription()) {
            if (notes.isEmpty()) {
                val backFill = fillBox(surfaceSwatch.fillColor)
                val addButton = textButtonBox(
                    label = "Add Note",
                    onPress = { visions.offer(visions.value + (1..10).map { Unit }) }
                ).maxWidth(10).maxHeight(1)
                boxScreen.setBox(addButton.before(backFill))
            } else {
                val bodyBox = messageBox("All Hidden", surfaceSwatch)

                val sideSwatch = backgroundSwatch
                val sideOver = columnBox(
                    3 to titleBox("Pnotes"),
                    -1 to listRow(notes, bodyBox, sideSwatch).before(fillBox(TextColor.ANSI.GREEN))
                )
                val sideUnder = fillBox(sideSwatch.fillColor)
                val sideBox = sideOver.before(sideUnder)
                boxScreen.setBox(bodyBox.packLeft(20, sideBox))
            }
        }
    }
}

private fun BoxContext.listRow(notes: List<Unit>, bodyBox: Box<String>, swatch: ColorSwatch): Box<*> {
    val itemLabels = notes.mapIndexed { i, _ -> "Unit${i + 1}" }
    val listSwatch = ListSwatch(swatch, primaryLightSwatch)
    return listBox(itemLabels, listSwatch) { i, box ->
        bodyBox.update("Unit${i + 1} Revealed")
        box.update(ListMotion.Activate)
    }
}

private fun BoxContext.titleBox(title: String): Box<Void> {
    val swatch = primarySwatch
    return columnBox(
        1 to gapBox(),
        1 to labelBox(title, swatch.strokeColor, Snap.LEFT).padX(2),
        1 to glyphBox('_', swatch.disabledColor)
    ).before(fillBox(swatch.fillColor))
}


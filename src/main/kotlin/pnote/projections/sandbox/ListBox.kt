package pnote.projections.sandbox

import com.googlecode.lanterna.input.KeyType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

enum class ListMovement { Up, Down }

fun BoxContext.listBox(items: List<String>, onItemClicked: (Int) -> Unit): Box<ListMovement> {
    var selectedItem = 0
    val list = passiveList(items) { selectedItem = it }
    val focusId = randomId()
    return list.focusable(focusId) {
        val listChanged = when (keyStroke.keyType) {
            KeyType.ArrowDown -> true.also { list.setContent(ListMovement.Down) }
            KeyType.ArrowUp -> true.also { list.setContent(ListMovement.Up) }
            KeyType.Enter -> false.also { GlobalScope.launch { onItemClicked(selectedItem) } }
            else -> false
        }
        if (listChanged) setChanged(edge.bounds)
        listChanged
    }
}

private fun BoxContext.passiveList(items: List<String>, onSelected: (Int) -> Unit): Box<ListMovement> {
    val normalSwatch = primarySwatch
    val dividerColor = primaryDarkSwatch.fillColor
    val maxLevels = 8
    val borderLevels = 3
    var topIndex = -borderLevels
    var selected = 0.also { onSelected(it) }

    fun moveDown(): Boolean =
        if (selected < items.lastIndex) {
            selected++
            val bottomBorder = topIndex + maxLevels - 1
            if (selected == bottomBorder) topIndex++
            true
        } else false

    fun moveUp(): Boolean =
        if (selected > 0) {
            selected--
            val topBorder = topIndex + borderLevels - 1
            if (selected == topBorder) topIndex--
            true
        } else false

    fun itemBox(i: Int): Box<Void> {
        val item = items[i]
        val swatch = if (i == selected) secondarySwatch else normalSwatch
        val titleBox = labelBox(item, swatch.strokeColor)
        val dividerBox = if (i == selected) gapBox() else glyphBox('_', dividerColor)
        val overlay = columnBox(
            1, Snap.CENTER,
            gapBox(),
            titleBox,
            dividerBox
        )
        val underlay = fillBox(swatch.fillColor)
        return overlay.before(underlay).maxHeight(3, Snap.CENTER)
    }

    return box(
        name = "ListBox",
        render = {
            when (items.size) {
                0 -> messageBox("Empty", normalSwatch)
                else -> {
                    val levelBoxes = (-1 until (maxLevels + 1)).map { level ->
                        val i = topIndex + level
                        when {
                            i < 0 -> gapBox()
                            i < items.size -> itemBox(i)
                            else -> gapBox()
                        }
                    }
                    val levelHeight = edge.bounds.height / maxLevels
                    columnBox(levelHeight, Snap.CENTER, *levelBoxes.toTypedArray())
                }
            }.render(this)
        },
        focus = noFocus,
        setContent = {
            val oldSelected = selected
            when (it) {
                ListMovement.Up -> moveUp()
                ListMovement.Down -> moveDown()
            }
            if (selected != oldSelected) GlobalScope.launch { onSelected(selected) }
        }
    )
}

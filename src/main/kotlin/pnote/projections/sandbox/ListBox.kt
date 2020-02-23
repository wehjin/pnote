package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.input.KeyType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

enum class ListMotion { Up, Down, Activate }

data class ListSwatch(
    val fillColor: TextColor,
    val strokeColor: TextColor,
    val dividerColor: TextColor,
    val focusedFillColor: TextColor,
    val pressedFillColor: TextColor,
    val activeFillColor: TextColor,
    val activeStrokeColor: TextColor
) {
    constructor(colorSwatch: ColorSwatch, activeColorSwatch: ColorSwatch) : this(
        fillColor = colorSwatch.fillColor,
        strokeColor = colorSwatch.strokeColor,
        dividerColor = colorSwatch.disabledColor,
        focusedFillColor = colorSwatch.mediumColor,
        pressedFillColor = colorSwatch.highColor,
        activeFillColor = activeColorSwatch.highColor,
        activeStrokeColor = activeColorSwatch.fillColor
    )
}

fun BoxContext.listBox(
    items: List<String>,
    swatch: ListSwatch = ListSwatch(surfaceSwatch, primarySwatch),
    onItemClicked: (Int, Box<ListMotion>) -> Unit
): Box<ListMotion> {
    val focusId = randomId()
    var focusIndex = 0
    val list = passiveList(items, null, swatch) { focusIndex = it }
    return list.focusable(focusId, FocusRole.Edit) {
        val listChanged = when (keyStroke.keyType) {
            KeyType.ArrowDown -> true.also { list.update(ListMotion.Down) }
            KeyType.ArrowUp -> true.also { list.update(ListMotion.Up) }
            KeyType.Enter -> false.also { GlobalScope.launch { onItemClicked(focusIndex, list) } }
            else -> false
        }
        if (listChanged) setChanged(edge.bounds)
        listChanged
    }
}

private fun BoxContext.passiveList(
    itemLabels: List<String>,
    initActiveIndex: Int?,
    swatch: ListSwatch,
    onPress: (Int) -> Unit
): Box<ListMotion> {
    var maxLevels = 0
    var topIndex = 0
    var focusIndex = 0.also { onPress(it) }
    var activeIndex = initActiveIndex

    // TODO Indicate direction of active item when it is out of view.

    fun activate() {
        activeIndex = focusIndex
        boxScreen.refreshScreen()
    }

    fun moveDown(): Boolean =
        if (focusIndex < itemLabels.lastIndex) {
            focusIndex++
            if (focusIndex == topIndex + maxLevels) topIndex++
            boxScreen.refreshScreen()
            true
        } else false

    fun moveUp(): Boolean =
        if (focusIndex > 0) {
            focusIndex--
            if (focusIndex == topIndex - 1) topIndex--
            boxScreen.refreshScreen()
            true
        } else false

    fun fitToWidth(label: String, width: Int) =
        if (label.length <= width) label else (label.substring(0 until width) + '\\')

    val titlePadCols = 2

    fun itemBox(i: Int, width: Int): Box<Void> {
        val itemLabel = itemLabels[i]

        val title = labelBox(
            text = fitToWidth(itemLabel, width - 2 * titlePadCols),
            textColor = if (i == activeIndex) swatch.activeStrokeColor else swatch.strokeColor,
            snap = Snap.LEFT
        )

        val divider = if (i == focusIndex || i == activeIndex) {
            gapBox()
        } else {
            glyphBox('_', swatch.dividerColor)
        }

        val overlay = columnBox(
            1 to gapBox(),
            1 to title.padX(titlePadCols),
            1 to divider
        )

        val underlay = fillBox(
            when {
                i == activeIndex && i == focusIndex -> {
                    val combinedSwatch = ColorSwatch(
                        swatch.focusedFillColor,
                        swatch.activeFillColor
                    )
                    combinedSwatch.mediumColor
                }
                i == activeIndex -> swatch.activeFillColor
                i == focusIndex -> swatch.focusedFillColor
                else -> swatch.fillColor
            }
        )
        return overlay.before(underlay).maxHeight(3, Snap.CENTER)
    }

    return box(
        name = "ListBox",
        render = {
            maxLevels = edge.bounds.height / 3
            val listBox = when (itemLabels.size) {
                0 -> messageBox("Empty", ColorSwatch(swatch.strokeColor, swatch.fillColor))
                else -> {
                    val levelBoxes = (0 until (maxLevels + 1)).map { level ->
                        val i = topIndex + level
                        when {
                            i < 0 -> gapBox()
                            i < itemLabels.size -> itemBox(i, edge.bounds.width)
                            else -> gapBox()
                        }
                    }
                    val levelHeight = 3
                    val levels = levelBoxes.map { levelHeight to it }
                    columnBox(*levels.toTypedArray())
                }
            }
            listBox.render(this)
        },
        focus = noFocus,
        setContent = {
            val oldFocusIndex = focusIndex
            when (it) {
                ListMotion.Up -> moveUp()
                ListMotion.Down -> moveDown()
                ListMotion.Activate -> activate()
            }
            if (focusIndex != oldFocusIndex) GlobalScope.launch { onPress(focusIndex) }
        }
    )
}

/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package pnote

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.input.KeyType
import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.projections.sandbox.*
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes
import pnote.stories.UnlockConfidential
import pnote.stories.browseNotes
import pnote.tools.*
import java.io.File

class App(private val commandName: String) : AppScope {
    private val homeDir = System.getProperty("user.home")!!.also { check(it.isNotBlank()) }
    private val appDir = homeDir.let { home -> File(home, ".$commandName").apply { mkdirs() } }
    private val userDir = appDir.let { app -> File(app, "main").apply { mkdirs() } }
    override val cryptor: Cryptor = fileCryptor(userDir)
    override val noteBag: NoteBag = FileNoteBag(userDir, cryptor)
    override val logTag: String = commandName
}

fun main(args: Array<String>) {
    val commandSuffix = args.getOrNull(0)?.let { "-debug" } ?: ""
    val commandName = "pnote$commandSuffix"
    val app = App(commandName)
    val boxScreen = lanternaBoxScreen()
    val story = app.browseNotes()
    story.project(boxScreen, mainBoxContext())
}


fun Story<BrowseNotes>.project(boxScreen: BoxScreen, boxContext: BoxContext) = boxContext.run {
    runBlocking {
        visionLoop@ for (vision in subscribe()) {
            println("$name: $vision")
            when (vision) {
                BrowseNotes.Finished -> break@visionLoop
                is BrowseNotes.Unlocking -> vision.substory.projectUnlockConfidential(boxScreen, boxContext)
                is BrowseNotes.Browsing -> vision.projectBrowsing(boxScreen, boxContext)
                else -> boxScreen.setBox(messageBox("$vision", surfaceSwatch))
            }
        }
    }
    boxScreen.close()
}

fun BrowseNotes.Browsing.projectBrowsing(boxScreen: BoxScreen, boxContext: BoxContext) = boxContext.run {
    val items = banners.map { (it as Banner.Basic).title } + "Add Note"
    val pageSwatch = primaryDarkSwatch
    val pageTitle = labelBox("CONFIDENTIAL", pageSwatch.strokeColor, Snap.TOP_RIGHT).pad(1)
    val pageBackground = fillBox(pageSwatch.fillColor)
    val pageUnderlay = pageTitle.before(pageBackground)

    var selectedItem = 0
    val list = listBox(items) { selectedItem = it }
    val focusId = randomId()
    val activeList = list.focusable(focusId, true) {
        val listChanged = when (keyStroke.keyType) {
            KeyType.ArrowDown -> true.also { list.setContent(ListMovement.Down) }
            KeyType.ArrowUp -> true.also { list.setContent(ListMovement.Up) }
            KeyType.Enter -> false.also {
                when (selectedItem) {
                    items.lastIndex -> addNote("Another note")
                    else -> println("SELECTED ITEM: $selectedItem")
                }
            }
            KeyType.Escape -> false.also { cancel() }
            else -> false
        }
        if (listChanged) setChanged(edge.bounds)
    }
    val page = activeList.maxWidth(50).packTop(9, gapBox()).before(pageUnderlay)
    boxScreen.setBox(page)
}

enum class ListMovement { Up, Down }

fun BoxContext.listBox(items: List<String>, onSelected: (Int) -> Unit): Box<ListMovement> {
    var selected = 0.also { onSelected(it) }
    return box(
        name = "BannerBox",
        render = {
            val listSwatch = primarySwatch
            val listUnderlay = fillBox(listSwatch.fillColor)
            val listOverlay = when (items.size) {
                0 -> messageBox("Empty", listSwatch)
                else -> {
                    val itemBoxes = items.mapIndexed { i, item ->
                        val swatch = if (i == selected) secondarySwatch else listSwatch
                        val title = labelBox(item, swatch.strokeColor)
                        val divider = if (i == selected) gapBox() else glyphBox('_', primaryDarkSwatch.fillColor)
                        val overlay = columnBox(1, Snap.TOP, gapBox(), title, divider)
                        overlay.before(fillBox(swatch.fillColor)).maxHeight(3, Snap.TOP)
                    }
                    columnBox(3, Snap.TOP, *itemBoxes.toTypedArray())
                }
            }
            listOverlay.before(listUnderlay).render(this)
        },
        focus = noFocus,
        setContent = {
            val oldSelected = selected
            selected = when (it) {
                ListMovement.Up -> if (selected > 0) selected - 1 else selected
                ListMovement.Down -> if (selected < items.lastIndex) selected + 1 else selected
            }
            if (selected != oldSelected) GlobalScope.launch { onSelected(selected) }
        }
    )
}

fun Story<UnlockConfidential>.projectUnlockConfidential(boxScreen: BoxScreen, boxContext: BoxContext) = boxContext.run {
    GlobalScope.launch {
        for (vision in subscribe()) {
            println("$name: $vision")
            when (vision) {
                is UnlockConfidential.Unlocking -> {
                    var password = ""
                    val errorBox = this@run.labelBox(
                        text = if (vision.failCount > 0) "Invalid password" else "",
                        textColor = primaryLightSwatch.fillColor,
                        snap = Snap(0f, 0.5f)
                    )
                    val content = columnBox(1, Snap.CENTER,
                        labelBox("Enter Password", surfaceSwatch.strokeColor),
                        gapBox(),
                        inputBox {
                            password = it
                            errorBox.setContent("")
                            boxScreen.refreshScreen()
                        }.maxWidth(20),
                        errorBox.maxWidth(20),
                        gapBox(),
                        buttonBox("Submit") { vision.setPassword(password) }
                    )
                    val box = content.before(fillBox(surfaceSwatch.fillColor))
                    boxScreen.setBox(box)
                }
                is UnlockConfidential.Finished -> Unit
            }
        }
    }
}

fun mainBoxContext(block: (BoxContext.() -> Unit)? = null): BoxContext {
    val context = object : BoxContext {
        override val primarySwatch: ColorSwatch =
            ColorSwatch(TextColor.ANSI.WHITE, TextColor.Indexed.fromRGB(0x34, 0x49, 0x55))
        override val primaryDarkSwatch: ColorSwatch =
            ColorSwatch(TextColor.ANSI.WHITE, TextColor.Indexed.fromRGB(0x23, 0x2f, 0x34))
        override val primaryLightSwatch: ColorSwatch =
            ColorSwatch(TextColor.ANSI.WHITE, TextColor.Indexed.fromRGB(0x4a, 0x65, 0x72))
        override val surfaceSwatch: ColorSwatch =
            ColorSwatch(TextColor.ANSI.BLACK, TextColor.Indexed.fromRGB(0xFF, 0xFF, 0xFF))
        override val secondarySwatch: ColorSwatch =
            ColorSwatch(TextColor.ANSI.BLACK, TextColor.Indexed.fromRGB(0xf9, 0xaa, 0x33))
    }
    return context.also { block?.invoke(it) }
}

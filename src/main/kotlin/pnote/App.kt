/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package pnote

import com.googlecode.lanterna.TextColor
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

fun userDir(commandName: String, userName: String): File {
    val homeDir = System.getProperty("user.home")!!.also { check(it.isNotBlank()) }
    val appDir = homeDir.let { home -> File(home, ".$commandName").apply { mkdirs() } }
    return appDir.let { app -> File(app, userName).apply { mkdirs() } }
}

class App(commandName: String, userName: String) : AppScope {
    private val userDir = userDir(commandName, userName)
    override val cryptor: Cryptor = fileCryptor(userDir)
    override val noteBag: NoteBag = FileNoteBag(userDir, cryptor)
    override val logTag: String = commandName
}

fun main(args: Array<String>) {
    val commandSuffix = args.getOrNull(0)?.let { "-debug" } ?: ""
    val commandName = "pnote$commandSuffix"
    val app = App(commandName, "main")
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
    val pageSwatch = primaryDarkSwatch
    val pageTitle = labelBox("CONFIDENTIAL", pageSwatch.strokeColor, Snap.TOP_RIGHT).pad(1)
    val pageBackground = fillBox(pageSwatch.fillColor)
    val pageUnderlay = pageTitle.before(pageBackground)

    val items = banners.map { (it as Banner.Basic).title } + "Add Note"
    val itemList = listBox(items) { index ->
        when (index) {
            0 -> cancel()
            items.lastIndex -> addNote("Another note")
            else -> println("SELECTED ITEM: ${index + 1}")
        }
    }
    val page = itemList.maxWidth(50).before(pageUnderlay)
    boxScreen.setBox(page)
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

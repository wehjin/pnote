/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package pnote

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.scopes.AppScope
import pnote.scopes.PasswordRef
import pnote.scopes.ProjectorScope
import pnote.stories.ImportPassword.Action.SetPassword
import pnote.stories.ImportPassword.Vision
import pnote.stories.ImportPassword.Vision.FinishedGetPassword
import pnote.stories.ImportPassword.Vision.GetPassword
import pnote.stories.importPasswordStory
import pnote.tools.NoteBag
import java.io.File
import kotlin.coroutines.CoroutineContext

class App(private val commandName: String) : AppScope {

    override val noteBag: NoteBag
        get() = TODO("not implemented")

    override fun importPassword(password: String): PasswordRef {
        // TODO Use key store
        File(userDir, "key1").writeText(password)
        return 1
    }

    private val userDir: File = System.getProperty("user.home")!!.also { check(it.isNotBlank()) }
        .let { appDir -> File(appDir, ".$commandName").also { it.mkdirs() } }
        .let { userDir ->
            File(userDir, "main").also { it.mkdirs() }
        }

    override val logTag: String = commandName
}

class Projector : ProjectorScope {
    override val coroutineContext: CoroutineContext = Job()

    override fun promptLine(prompt: String, subject: String): String = print("$prompt: ").let {
        readLine() ?: error("Failed to read $subject")
    }

    override fun screenError(error: String) = println("ERROR: $error")
    override fun screenLine(line: String) = println(line)
}

fun main(args: Array<String>) {
    val commandSuffix = args.getOrNull(0)?.let { "-debug" } ?: ""
    val commandName = "pnote$commandSuffix"
    val app = App(commandName)
    val projector = Projector()

    val story = app.importPasswordStory()
    val projection = projector.projectImportPassword(story)
    runBlocking { projection.join() }
}

fun ProjectorScope.projectImportPassword(story: Story<Vision>) = launch {
    loop@ for (vision in story.subscribe()) {
        when (vision) {
            is GetPassword -> {
                screenLine()
                vision.error?.let { screenError("$it") }
                val passwordLine = promptLine("Enter password", "password")
                val checkLine = promptLine("Re-enter password", "password check")
                if (passwordLine.isNotEmpty() && checkLine.isNotEmpty()) {
                    story.offer(SetPassword(passwordLine, checkLine))
                } else break@loop
            }
            is FinishedGetPassword -> {
                screenLine("Got password: ${vision.passwordRef}")
                break@loop
            }
        }
    }
}

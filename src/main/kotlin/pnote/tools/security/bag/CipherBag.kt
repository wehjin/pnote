@file:Suppress("EXPERIMENTAL_API_USAGE")

package pnote.tools.security.bag

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.tools.security.item.*
import java.io.File
import java.io.FileFilter

class CipherBag(dir: File) {
    private val itemsDir = File(dir, "items").apply { mkdirs() }
    private val itemFileFilter = FileFilter { it.isFile }

    fun <T : Any> values(password: CharArray, plainType: PlainType<T>) = map(password, plainType) { plainValue }

    private fun <T : Any, R : Any> mapOrNull(
        itemId: String,
        password: CharArray,
        plainType: PlainType<T>,
        block: ItemVisitScope<T>.() -> R
    ): R? = readCipher(itemId)?.map(password, plainType, block)

    fun <T : Any, R : Any> map(
        password: CharArray,
        plainType: PlainType<T>,
        block: ItemVisitScope<T>.() -> R
    ): Set<R> {
        val itemFiles = itemsDir.listFiles(itemFileFilter)!!
        val values = mutableListOf<R>()
        runBlocking {
            val results = Channel<R?>(itemFiles.size)
            val jobs = itemFiles.map {
                GlobalScope.launch(Dispatchers.IO) {
                    val result = try {
                        CipherFile(it).map(password, plainType, block)
                    } catch (e: Throwable) {
                        null
                    }
                    results.send(result)
                }
            }
            repeat(jobs.size) {
                val result = results.receive()
                if (result != null) values.add(result)
            }
        }
        return values.toSet()
    }

    fun <T : Any> writeCipher(password: CharArray, plainType: PlainType<T>, value: T, id: String? = null): String {
        val existingCipher = id?.let { readCipher(it) }
        existingCipher?.let { error("Cipher exists") }
        return writeCipherFile(password, plainItem(value, plainType, id))
    }

    fun <T : Any> rewriteCipher(id: String, password: CharArray, plainType: PlainType<T>, value: T): String {
        val alteredPlain = plainItem(value, plainType, id)
        val existingCipher = readCipher(id)
        return if (existingCipher == null) {
            writeCipherFile(password, alteredPlain)
        } else {
            // The purpose here is to confirm both that bytes change AND that the password
            // deciphers the existing cipher. The map function confirms the latter by throwing
            // an error if the cipher cannot be deciphered.
            val bytesChanged = existingCipher.map(password, plainType) {
                !plainBytes.contentEquals(alteredPlain.bytes)
            }
            if (bytesChanged) writeCipherFile(password, alteredPlain) else id
        }
    }

    fun <T : Any> unwrapOrNull(id: String, password: CharArray, plainType: PlainType<T>): T? {
        return readCipher(id)?.get(password, plainType)
    }

    fun <T : Any> unwrap(id: String, password: CharArray, plainType: PlainType<T>): T {
        return unwrapOrNull(id, password, plainType) ?: error("No cipher for id $id")
    }

    fun <T : Any> remove(id: String, password: CharArray, plainType: PlainType<T>) {
        if (checkPassword(id, password, plainType)) {
            readCipher(id)?.unlink()
        }
    }

    private fun <T : Any> checkPassword(id: String, password: CharArray, plainType: PlainType<T>): Boolean {
        return mapOrNull(id, password, plainType) { Unit } != null
    }

    @Deprecated("Use version that checks the password")
    fun remove(id: String) {
        readCipher(id)?.unlink()
    }

    private fun <T : Any> writeCipherFile(password: CharArray, plainItem1: PlainItem<T>): String {
        return writeCipherFile(itemsDir, password, plainItem1).id
    }

    private fun readCipher(id: String): CipherFile? {
        val itemFile = File(itemsDir, id)
        val fileExists = itemFile.exists() && itemFile.isFile
        return if (fileExists) CipherFile(itemFile) else null
    }
}

fun cipherBag(dir: File): CipherBag = CipherBag(dir)
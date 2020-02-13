package pnote.tools.security.bag

import pnote.tools.security.item.*
import java.io.File
import java.io.FileFilter

class CipherBag(dir: File) {
    private val itemsDir = File(dir, "items").apply { mkdirs() }
    private val itemFileFilter = FileFilter { it.isFile }

    fun <T : Any> values(password: CharArray, itemType: ItemType<T>) = map(password, itemType) { plainValue }

    fun <T : Any, R : Any> mapOrNull(
        itemId: String,
        password: CharArray,
        itemType: ItemType<T>,
        block: ItemVisitScope<T>.() -> R
    ): R? = cipherItem(itemId)?.map(password, itemType, block)

    fun <T : Any, R : Any> map(password: CharArray, itemType: ItemType<T>, block: ItemVisitScope<T>.() -> R): Set<R> =
        itemsDir.listFiles(itemFileFilter)!!.mapNotNull {
            try {
                CipherItem(it).map(password, itemType, block)
            } catch (e: Throwable) {
                null
            }
        }.toSet()

    fun <T : Any> add(password: CharArray, itemType: ItemType<T>, value: T, id: String? = null): String {
        val cipherItem = cipherItem(itemsDir, password, plainItem(value, itemType, id))
        return cipherItem.id
    }

    fun <T : Any> replace(id: String, password: CharArray, itemType: ItemType<T>, value: T): String {
        return add(password, itemType, value).also {
            cipherItem(id)?.unlink()
        }
    }

    fun <T : Any> getOrNull(id: String, password: CharArray, itemType: ItemType<T>): T? {
        return cipherItem(itemsDir, id)?.get(password, itemType)
    }

    fun <T : Any> get(id: String, password: CharArray, itemType: ItemType<T>): T {
        return getOrNull(id, password, itemType) ?: error("No cipher item for id $id")
    }

    fun <T : Any> remove(id: String, password: CharArray, itemType: ItemType<T>) {
        if (checkPassword(id, password, itemType)) {
            cipherItem(id)?.unlink()
        }
    }

    private fun <T : Any> checkPassword(id: String, password: CharArray, itemType: ItemType<T>): Boolean {
        return mapOrNull(id, password, itemType) { Unit } != null
    }

    @Deprecated("Use version that checks the password")
    fun remove(id: String) {
        val cipherItem = cipherItem(id)
        cipherItem?.unlink()
    }

    private fun cipherItem(id: String): CipherItem? = cipherItem(itemsDir, id)
}

fun cipherBag(dir: File): CipherBag = CipherBag(dir)
package pnote.tools.security.item

import java.io.Closeable
import kotlin.math.absoluteValue
import kotlin.random.Random

class PlainItem<T : Any>(
    val type: ItemType<T>,
    val bytes: ByteArray,
    val id: String = randomId()
) : Closeable {
    fun asValue(): T = type.asValue(bytes)
    override fun close() {
        Random.nextBytes(bytes)
    }
}

fun randomId() = Random.nextLong().absoluteValue.toString(16)

fun <T : Any> PlainItem<*>.asValue(valueClass: Class<T>): T = valueClass.cast(asValue())

sealed class ItemType<T : Any> {

    abstract val valueClass: Class<T>
    abstract fun asValue(bytes: ByteArray): T
    abstract fun asByteArray(value: T): ByteArray

    // TODO: Make this a buffer
    object Text : ItemType<String>() {
        override val valueClass: Class<String> = String::class.java
        override fun asValue(bytes: ByteArray): String = bytes.toString(Charsets.UTF_8)
        override fun asByteArray(value: String): ByteArray = value.toByteArray(Charsets.UTF_8)
    }
}

fun plainItem(value: String): PlainItem<String> = plainItem(value, ItemType.Text)
fun <T : Any> plainItem(
    value: T,
    itemType: ItemType<T>,
    id: String? = null
) = PlainItem(itemType, itemType.asByteArray(value), id ?: randomId())

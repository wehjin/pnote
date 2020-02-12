package pnote.tools.security

import java.io.Closeable
import kotlin.math.absoluteValue
import kotlin.random.Random

class PlainItem<T : Any>(
    val type: ItemType<T>,
    val bytes: ByteArray,
    val id: String = Random.nextLong().absoluteValue.toString(16)
) : Closeable {

    fun asValue(): T = type.asValue(bytes)


    override fun close() {
        Random.nextBytes(bytes)
    }
}

fun <T : Any> PlainItem<*>.asValue(valueClass: Class<T>): T = valueClass.cast(asValue())


sealed class ItemType<T : Any> {

    abstract fun asValue(bytes: ByteArray): T
    abstract val valueClass: Class<T>

    // TODO: Make this a buffer
    object Text : ItemType<String>() {
        override fun asValue(bytes: ByteArray): String = bytes.toString(Charsets.UTF_8)
        override val valueClass: Class<String> = String::class.java
    }
}

fun plainItem(value: String): PlainItem<String> = PlainItem(ItemType.Text, value.toByteArray())
package pnote.tools.security

import com.beust.klaxon.Klaxon
import java.io.File
import java.util.*

class CipherItem(private val file: File) {

    val id: String by lazy { file.nameWithoutExtension }

    private val cipherLoadItemType: Pair<CipherLoad, ItemType<*>> by lazy { decode(file) }
    private val cipherLoad: CipherLoad get() = cipherLoadItemType.first

    fun <T : Any, R> visit(
        password: CharArray,
        itemType: ItemType<T>,
        block: ItemVisitScope<T>.() -> R
    ): R {
        require(itemType == cipherLoadItemType.second)
        val plainLoad = plainLoad(password, cipherLoad) ?: error("Invalid password or item file")
        return PlainItem(itemType, plainLoad.plainBytes, id).use { plainItem ->
            val scope = object : ItemVisitScope<T> {
                override val plainBytes get() = plainItem.bytes
                override val plainValue: T get() = plainItem.asValue(itemType.valueClass)
            }
            scope.block()
        }
    }
}

interface ItemVisitScope<T : Any> {
    val plainBytes: ByteArray
    val plainValue: T
}

fun <T : Any> cipherItem(hostDir: File, password: CharArray, plainItem: PlainItem<T>): CipherItem {
    val cipherLoad = cipherLoad(password, PlainLoad(plainItem.bytes, CipherType.Main))
    return CipherItem(
        file = File(hostDir, plainItem.id).apply { writeText(encode(cipherLoad, plainItem.type)) }
    )
}

fun decode(file: File): Pair<CipherLoad, ItemType<*>> {
    val jsonObject = Klaxon().parseJsonObject(file.bufferedReader()) ?: error("invalid file $file")
    val itemType = decodeItemType(jsonObject.string("itemType"))
    val cipherLoad = CipherLoad(
        cipherBytes = decodeByteArray(jsonObject.string("cipherBytes")),
        cipherType = decodeCipherType(jsonObject.string("cipherType")),
        salt = decodeByteArray(jsonObject.string("salt")),
        iv = decodeByteArray(jsonObject.string("iv"))
    )
    return Pair(cipherLoad, itemType)
}

private fun decodeItemType(string: String?): ItemType<*> =
    when (string) {
        "string" -> ItemType.Text
        else -> error("invalid item type $string")
    }

private fun decodeCipherType(string: String?): CipherType =
    when (string) {
        "main" -> CipherType.Main
        else -> error("invalid cipher type $string")
    }

private fun decodeByteArray(string: String?): ByteArray = try {
    Base64.getDecoder().decode(string!!)
} catch (e: Throwable) {
    error("invalid byte array $string")
}

private fun <T : Any> encode(cipherLoad: CipherLoad, itemType: ItemType<T>): String =
    """
        {
            "itemType": "${encode(itemType)}",
            "cipherType": "${encode(cipherLoad.cipherType)}",
            "iv":   "${encode(cipherLoad.iv)}",
            "salt": "${encode(cipherLoad.salt)}",
            "cipherBytes": "${encode(cipherLoad.cipherBytes)}"
        }
    """.trimIndent()

private fun encode(bytes: ByteArray): String =
    Base64.getEncoder().encodeToString(bytes)

private fun encode(cipherType: CipherType): String =
    when (cipherType) {
        CipherType.Main -> "main"
    }

private fun <T : Any> encode(itemType: ItemType<T>): String =
    when (itemType) {
        ItemType.Text -> "string"
    }

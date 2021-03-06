package pnote.tools.security.item

import com.beust.klaxon.Klaxon
import pnote.tools.security.load.*
import java.io.File
import java.util.*

fun <T : Any> writeCipherFile(hostDir: File, password: CharArray, plainItem: PlainItem<T>): CipherFile {
    val cipherLoad = cipherLoad(password, PlainLoad(plainItem.bytes, CipherType.Main))
    val file = File(hostDir, plainItem.id).apply { writeText(encode(cipherLoad, plainItem.type)) }
    return CipherFile(file)
}

class CipherFile(private val file: File) {

    val id: String by lazy { file.nameWithoutExtension }

    private val cipherLoadPlainType: Pair<CipherLoad, PlainType<*>> by lazy { decode(file) }
    private val cipherLoad: CipherLoad get() = cipherLoadPlainType.first

    fun <T : Any> get(password: CharArray, plainType: PlainType<T>): T = map(password, plainType) { plainValue }

    fun <T : Any, R> map(
        password: CharArray,
        plainType: PlainType<T>,
        block: ItemVisitScope<T>.() -> R
    ): R {
        require(plainType == cipherLoadPlainType.second)
        val plainLoad = plainLoad(password, cipherLoad) ?: error("Invalid password or item file")
        return PlainItem(plainType, plainLoad.plainBytes, id)
            .use { plainItem ->
                val scope = object : ItemVisitScope<T> {
                    override val itemId: String = id
                    override val plainBytes get() = plainItem.bytes
                    override val plainValue: T get() = plainItem.asValue(plainType.valueClass)
                }
                scope.block()
            }
    }

    fun unlink() {
        file.delete()
    }
}

interface ItemVisitScope<T : Any> {
    val itemId: String
    val plainBytes: ByteArray
    val plainValue: T
}


fun decode(file: File): Pair<CipherLoad, PlainType<*>> {
    val jsonObject = Klaxon().parseJsonObject(file.bufferedReader())
    val itemType = decodeItemType(jsonObject.string("itemType"))
    val cipherLoad = CipherLoad(
        cipherBytes = decodeByteArray(jsonObject.string("cipherBytes")),
        cipherType = decodeCipherType(jsonObject.string("cipherType")),
        salt = decodeByteArray(jsonObject.string("salt")),
        iv = decodeByteArray(jsonObject.string("iv"))
    )
    return Pair(cipherLoad, itemType)
}

private fun decodeItemType(string: String?): PlainType<*> =
    when (string) {
        "string" -> PlainType.Text
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

private fun <T : Any> encode(cipherLoad: CipherLoad, plainType: PlainType<T>): String =
    """
        {
            "itemType": "${encode(plainType)}",
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

private fun <T : Any> encode(plainType: PlainType<T>): String =
    when (plainType) {
        PlainType.Text -> "string"
    }

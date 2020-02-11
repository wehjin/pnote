package pnote.tools

import java.io.File
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class FileStringDelegate(private val file: File) :
    ReadWriteProperty<Cryptor, String?> {
    override fun getValue(thisRef: Cryptor, property: KProperty<*>): String? {
        return if (file.exists()) file.readText() else null
    }

    override fun setValue(thisRef: Cryptor, property: KProperty<*>, value: String?) {
        if (value == null) file.delete() else file.writeText(value)
    }
}
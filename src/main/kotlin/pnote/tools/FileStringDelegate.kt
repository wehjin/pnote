package pnote.tools

import java.io.File
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class FilePasswordDelegate(private val file: File) :
    ReadWriteProperty<Cryptor, Password?> {
    override fun getValue(thisRef: Cryptor, property: KProperty<*>): Password? {
        return if (file.exists()) Password(file.readText().toCharArray()) else null
    }

    override fun setValue(thisRef: Cryptor, property: KProperty<*>, value: Password?) {
        if (value == null) file.delete() else file.writeText(String(value.chars))
    }
}
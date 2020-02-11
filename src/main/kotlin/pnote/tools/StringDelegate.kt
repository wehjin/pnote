package pnote.tools

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class StringDelegate(private var string: String? = null) :
    ReadWriteProperty<Cryptor, String?> {
    override fun getValue(thisRef: Cryptor, property: KProperty<*>): String? = string
    override fun setValue(thisRef: Cryptor, property: KProperty<*>, value: String?) {
        string = value
    }
}
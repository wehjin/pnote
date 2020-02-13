package pnote.tools

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PasswordDelegate(private var password: Password? = null) :
    ReadWriteProperty<Cryptor, Password?> {
    override fun getValue(thisRef: Cryptor, property: KProperty<*>): Password? = password
    override fun setValue(thisRef: Cryptor, property: KProperty<*>, value: Password?) {
        password = value
    }
}
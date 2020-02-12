package pnote.tools.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.nio.charset.Charset

internal class SecurityKtTest {
    @Test
    internal fun `same password produces plain text from cipher text`() {
        val password = CharArray(12) { i -> i.toChar() }
        val plainText1 = "Hello"
        val plainLoad1 = PlainLoad(plainText1.toByteArray(Charset.defaultCharset()), CipherType.Main)
        val cipherLoad = cipherLoad(password, plainLoad1)
        val plainLoad2 = plainLoad(password, cipherLoad)
        val plainText2 = plainLoad2!!.plainBytes.toString(Charset.defaultCharset())
        assertEquals(plainLoad1.cipherType, plainLoad2.cipherType)
        assertEquals(plainLoad1.salt, plainLoad2.salt)
        assertEquals(plainLoad1.iv, plainLoad2.iv)
        assertEquals(plainText1, plainText2)
    }

    @Test
    internal fun `different password produces exception from cipher text`() {
        val password1 = "1234".toCharArray()
        val plainLoad1 = PlainLoad("Hello".toByteArray(Charset.defaultCharset()), CipherType.Main)
        val cipherLoad = cipherLoad(password1, plainLoad1)
        val password2 = "2234".toCharArray()
        val plainLoad2 = plainLoad(password2, cipherLoad)
        assertNull(plainLoad2)
    }
}
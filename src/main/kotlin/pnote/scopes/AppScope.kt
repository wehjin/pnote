package pnote.scopes

import log.LogScope
import pnote.tools.NoteBag

interface AppScope : LogScope {
    val noteBag: NoteBag
    fun importPassword(password: String): PasswordRef
}

typealias  PasswordRef = Int


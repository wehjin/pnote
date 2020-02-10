package pnote.scopes

import log.LogScope

interface AppScope : LogScope {
    fun importPassword(password: String): PasswordRef
}

typealias  PasswordRef = Int
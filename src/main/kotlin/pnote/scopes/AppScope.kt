package pnote.scopes

import log.LogScope
import pnote.tools.Cryptor
import pnote.tools.NoteBag

interface AppScope : LogScope {
    val noteBag: NoteBag
    val cryptor: Cryptor
}


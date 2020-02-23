package pnote.projections.sandbox

import com.googlecode.lanterna.input.KeyStroke

interface FocusKeyScope {
    val keyStroke: KeyStroke
    val edge: BoxEdge
    fun setChanged(bounds: BoxBounds)
}
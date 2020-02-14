package pnote.projections.sandbox

import com.googlecode.lanterna.input.KeyStroke

sealed class RenderAction {
    object Refresh : RenderAction()
    data class KeyPress(val keyStroke: KeyStroke) : RenderAction()
    object Quit : RenderAction()
}
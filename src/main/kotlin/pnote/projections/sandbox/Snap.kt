package pnote.projections.sandbox

data class Snap(val x: Float, val y: Float) {

    companion object {
        val CENTER = Snap(0.5f, 0.5f)
        val TOP = Snap(0.5f, 0.0f)
        val TOP_RIGHT = Snap(1.0f, 0.0f)
        val BOTTOM = Snap(0.5f, 1.0f)
    }
}
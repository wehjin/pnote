package pnote.projections.sandbox

data class BoxBounds(
    var right: Int = 0,
    var bottom: Int = 0,
    var left: Int = 0,
    var top: Int = 0,
    var z: Int = 0
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    fun contains(col: Int, row: Int): Boolean = col >= left && row >= top && col < right && row < bottom

    fun set(value: BoxBounds) {
        this.right = value.right
        this.bottom = value.bottom
        this.left = value.left
        this.top = value.top
        this.z = value.z
    }

    fun partitionRight(columns: Int): Pair<BoxBounds, BoxBounds> {
        val middle = right - columns
        val leftBounds = BoxBounds(middle, bottom, left, top)
        val rightBounds = BoxBounds(right, bottom, middle, top)
        return Pair(leftBounds, rightBounds)
    }
}
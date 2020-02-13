package pnote.projections.sandbox

data class BoxBounds(
    var right: Int = 0,
    var bottom: Int = 0,
    var left: Int = 0,
    var top: Int = 0,
    var z: Int = 0
) {
    private val width: Int get() = right - left
    private val height: Int get() = bottom - top

    fun contains(col: Int, row: Int): Boolean =
        col >= left && row >= top && col < right && row < bottom

    fun hits(col: Int, row: Int, minZ: Int): Boolean =
        this.contains(col, row) && this.z <= minZ

    fun leftInset(x: Int): Int =
        x - this.left

    fun set(value: BoxBounds) {
        this.right = value.right
        this.bottom = value.bottom
        this.left = value.left
        this.top = value.top
        this.z = value.z
    }

    fun shiftZ(delta: Int): BoxBounds =
        copy(z = z + delta)

    fun inset(delta: Int): BoxBounds =
        copy(left = left + delta, right = right - delta, top = top + delta, bottom = bottom - delta)

    fun partitionRight(columns: Int): Pair<BoxBounds, BoxBounds> {
        val middle = right - columns
        val leftBounds = BoxBounds(middle, bottom, left, top)
        val rightBounds = BoxBounds(right, bottom, middle, top)
        return Pair(leftBounds, rightBounds)
    }

    fun clipCenter(width: Int, height: Int, snapX: Float): BoxBounds {
        val extraWidth = this.width - width
        val extraHeight = this.height - height
        val nextLeft = this.left + (extraWidth * snapX).toInt()
        val nextRight = nextLeft + width
        val nextTop = this.top + extraHeight / 2
        val nextBottom = nextTop + height
        val nextZ = this.z
        return BoxBounds(nextRight, nextBottom, nextLeft, nextTop, nextZ)
    }
}
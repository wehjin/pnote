package pnote.projections.sandbox

import java.lang.Integer.max

data class BoxBounds(
    val right: Int = 0,
    val bottom: Int = 0,
    val left: Int = 0,
    val top: Int = 0,
    val z: Int = 0
) {
    val width: Int get() = max(0, right - left)
    val height: Int get() = max(0, bottom - top)

    val centerY: Int get() = top + height / 2

    fun contains(col: Int, row: Int): Boolean =
        col >= left && row >= top && col < right && row < bottom

    fun isTopLeftCorner(col: Int, row: Int): Boolean =
        col == left && row == top

    fun hits(col: Int, row: Int, minZ: Int): Boolean =
        this.contains(col, row) && this.z <= minZ

    fun leftInset(x: Int): Int =
        x - this.left

    fun shiftZ(delta: Int): BoxBounds =
        copy(z = z + delta)

    fun insetX(cols: Int): BoxBounds = copy(left = left + cols, right = right - cols)
    fun insetX(colsLeft: Int, colsRight: Int): BoxBounds = copy(left = left + colsLeft, right = right - colsRight)
    fun insetY(rows: Int): BoxBounds = copy(top = top + rows, bottom = bottom - rows)
    fun insetXY(size: Int): BoxBounds = insetX(size).insetY(size)
    fun insetXY(cols: Int, rows: Int): BoxBounds = insetX(cols).insetY(rows)
    fun insetBottom(rows: Int): BoxBounds = copy(bottom = bottom - rows)
    fun insetTop(rows: Int): BoxBounds = copy(top = top + rows)

    fun partitionTop(height: Int): Pair<BoxBounds, BoxBounds> {
        val middle = top + height
        return partitionHeight(middle)
    }

    fun partitionBottom(height: Int): Pair<BoxBounds, BoxBounds> {
        val middle = bottom - height
        return partitionHeight(middle)
    }

    private fun partitionHeight(middle: Int): Pair<BoxBounds, BoxBounds> {
        val topBounds = copy(bottom = middle)
        val bottomBounds = copy(top = middle)
        return Pair(topBounds, bottomBounds)
    }

    fun partitionRight(columns: Int): Pair<BoxBounds, BoxBounds> {
        val middle = right - columns
        return partitionWidth(middle)
    }

    fun partitionLeft(columns: Int): Pair<BoxBounds, BoxBounds> {
        val middle = left + columns
        return partitionWidth(middle)
    }

    private fun partitionWidth(middle: Int): Pair<BoxBounds, BoxBounds> {
        val leftBounds = copy(right = middle)
        val rightBounds = copy(left = middle)
        return Pair(leftBounds, rightBounds)
    }

    fun confineToBottom(): BoxBounds = copy(top = bottom - 1)
    fun confineToTop(): BoxBounds = copy(bottom = top + 1)
    fun confineToY(topOffset: Int): BoxBounds = copy(top = top + topOffset, bottom = top + topOffset + 1)
    fun confineToOffset(x: Int, y: Int): BoxBounds =
        copy(left = left + x, right = left + x + 1, top = top + y, bottom = top + y + 1)

    fun confine(width: Int, height: Int, snap: Snap): BoxBounds {
        val extraWidth = this.width - width
        val extraHeight = this.height - height
        val nextLeft = this.left + (extraWidth * snap.x).toInt()
        val nextRight = nextLeft + width
        val nextTop = this.top + (extraHeight * snap.y).toInt()
        val nextBottom = nextTop + height
        return copy(right = nextRight, bottom = nextBottom, left = nextLeft, top = nextTop)
    }

    fun <T> map(block: (col: Int, row: Int) -> T): List<T> {
        return (left until right).map { col ->
            (top until bottom).map { row -> block(col, row) }
        }.flatten()
    }
}
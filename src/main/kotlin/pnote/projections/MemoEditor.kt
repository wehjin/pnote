package pnote.projections

class MemoEditor(
    private val width: Int, private val height: Int,
    initMemo: MutableList<Char> = mutableListOf()
) {
    private var cursorRowIndex = 0
    private var preferredCursorIndex = 0
    private val lineEditors = mutableListOf(LineEditor(width, initMemo, null))

    val hasChars: Boolean
        get() = lineEditors.firstOrNull { it.charCount > 0 } != null

    fun isCursor(leftInset: Int, topInset: Int): Boolean {
        return if (topInset != this.cursorRowIndex) false else {
            val lineEditor = lineEditors.getOrNull(topInset)
            lineEditor?.isCursor(leftInset) ?: false
        }
    }

    fun getChar(leftInset: Int, topInset: Int): Char? {
        return lineEditors.getOrNull(topInset)?.getDisplayChar(leftInset)
    }

    fun moveRight(): Boolean = lineEditors[cursorRowIndex].moveRight().also {
        updatePreferredCursorIndex()
    }

    private fun updatePreferredCursorIndex() {
        preferredCursorIndex = lineEditors[cursorRowIndex].cursorIndex
    }

    fun moveLeft(): Boolean = lineEditors[cursorRowIndex].moveLeft().also {
        preferredCursorIndex = lineEditors[cursorRowIndex].cursorIndex
    }

    fun moveUp(): Boolean {
        return if (cursorRowIndex > 0) {
            lineEditors[cursorRowIndex - 1].matchCursorIndex(preferredCursorIndex)
            cursorRowIndex--
            true
        } else false
    }

    fun moveDown(): Boolean {
        return if (cursorRowIndex < lineEditors.lastIndex) {
            lineEditors[cursorRowIndex + 1].matchCursorIndex(preferredCursorIndex)
            cursorRowIndex++
            true
        } else false
    }

    fun splitLine() {
        val currentLine = lineEditors.getOrNull(cursorRowIndex)
        val newLineEditor = currentLine?.splitLine() ?: LineEditor(width)
        lineEditors.add(cursorRowIndex + 1, newLineEditor)
        cursorRowIndex++
        updatePreferredCursorIndex()
    }

    fun deletePreviousCharOnLine() {
        val rowIndex = cursorRowIndex
        val lineEditor = lineEditors[rowIndex]
        if (lineEditor.deletePreviousChar()) {
            updatePreferredCursorIndex()
        } else {
            if (rowIndex > 0) {
                val previousLineEditor = lineEditors[rowIndex - 1]
                previousLineEditor.selectEndAndCombineLine(lineEditor)
                lineEditor.wash()
                lineEditors.removeAt(rowIndex)
                cursorRowIndex--
                updatePreferredCursorIndex()
            }
        }
    }

    fun insertChar(char: Char) {
        val rowEditor =
            if (cursorRowIndex < lineEditors.size) {
                lineEditors[cursorRowIndex]
            } else {
                LineEditor(width).also {
                    lineEditors.add(it)
                    cursorRowIndex = lineEditors.lastIndex
                }
            }
        rowEditor.insertChar(char)
        updatePreferredCursorIndex()
    }
}
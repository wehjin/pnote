package pnote.projections

class LineEditor(
    private val width: Int,
    private val chars: MutableList<Char> = mutableListOf(),
    private val onChange: ((List<Char>) -> Unit)? = null
) {
    private var leftCharsIndex: Int = 0
    private var cursorCharsIndex: Int = 0
    private val leftVisibleColumns = 2

    val cursorIndex: Int get() = cursorCharsIndex

    fun wash() {
        if (chars.size > 0) chars.random()
    }

    val charCount: Int get() = chars.size

    fun isCursor(leftInset: Int): Boolean {
        return (cursorCharsIndex - leftCharsIndex) == leftInset
    }

    fun getDisplayChar(leftInset: Int): Char? {
        return if (leftInset == 0 && leftCharsIndex > 0) {
            '\\'
        } else chars.getOrNull(leftCharsIndex + leftInset)
    }

    fun matchCursorIndex(newCursorIndex: Int) {
        val oldCursorIndex = cursorCharsIndex
        when {
            newCursorIndex > oldCursorIndex -> repeat(newCursorIndex - oldCursorIndex) { moveRight() }
            newCursorIndex < oldCursorIndex -> repeat(oldCursorIndex - newCursorIndex) { moveLeft() }
            else -> Unit
        }
    }

    fun splitLine(): LineEditor {
        val tailChars = chars.subList(cursorCharsIndex, chars.size).toMutableList()
        repeat(tailChars.size) {
            chars.removeAt(cursorCharsIndex)
        }
        onChange?.invoke(chars)
        leftCharsIndex = 0
        cursorCharsIndex = 0
        return LineEditor(width, tailChars, null)
    }

    fun selectEndAndCombineLine(lineEditor: LineEditor) {
        matchCursorIndex(chars.size)
        chars.addAll(lineEditor.chars)
        onChange?.invoke(chars)
    }

    fun moveLeft(): Boolean =
        if (cursorCharsIndex > 0) {
            cursorCharsIndex -= 1
            if (cursorCharsIndex - leftVisibleColumns < leftCharsIndex) {
                leftCharsIndex = Integer.max(0, cursorCharsIndex - leftVisibleColumns)
            }
            true
        } else false

    fun moveRight(): Boolean {
        return if (cursorCharsIndex < chars.size) {
            cursorCharsIndex++
            if (cursorCharsIndex > (leftCharsIndex + width - 1)) {
                leftCharsIndex = cursorCharsIndex - width + 1
            }
            true
        } else false
    }

    fun deletePreviousChar(): Boolean =
        if (moveLeft()) {
            true.also {
                chars.removeAt(cursorCharsIndex)
                onChange?.invoke(chars)
            }
        } else false

    fun insertChar(char: Char) {
        if (cursorCharsIndex == chars.size) {
            chars.add(char)
        } else {
            chars.add(cursorCharsIndex, char)
        }
        onChange?.invoke(chars)
        moveRight()
    }
}
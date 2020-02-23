package pnote.tools

interface NoteBag {
    fun createNote(password: Password, note: Note): Long
    fun readNote(password: Password, noteId: Long): Note
    fun updateNote(password: Password, note: Note)
    fun deleteNote(noteId: Long, password: Password)
    fun readNotes(): ReadNotesResult
}

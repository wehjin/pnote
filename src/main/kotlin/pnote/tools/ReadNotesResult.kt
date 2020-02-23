package pnote.tools

data class ReadNotesResult(
    val accessLevel: AccessLevel,
    val notes: Set<Note.Basic>
)
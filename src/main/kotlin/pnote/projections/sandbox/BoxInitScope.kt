package pnote.projections.sandbox

interface BoxInitScope {
    fun refreshScreen()
    fun endProjection()
    fun setBox(box: Box<*>)
}
package pnote.projections.sandbox

fun BoxEdge.withBounds(bounds: BoxBounds): BoxEdge = object : BoxEdge by this {
    override val bounds: BoxBounds = bounds
}

interface BoxEdge {
    val bounds: BoxBounds
}
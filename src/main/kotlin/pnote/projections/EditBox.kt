package pnote.projections

import pnote.projections.sandbox.*

private const val labelInset = 1

fun BoxContext.unfocusedEditFrame(label: String, labelAtTop: Boolean): Box<Void> {
    val fillBox = fillBox(primaryDarkSwatch.fillColor)
    val labelBox = labelBox(label, primarySwatch.fillColor, Snap.LEFT)
    val scoreBox = glyphBox('_', primarySwatch.fillColor)
    return box(
        name = "LineBox",
        render = {
            val bounds = edge.bounds
            fillBox.render(this)
            scoreBox.render(withEdgeBounds(bounds.confineToBottom()))
            if (labelAtTop) {
                labelBox.render(withEdgeBounds(bounds.confineToTop().insetX(labelInset)))
            } else {
                labelBox.render(withEdgeBounds(bounds.confineToY(1).insetX(labelInset)))
            }
        },
        focus = noFocus,
        setContent = noContent
    )
}

fun BoxContext.focusedEditFrame(label: String): Box<Void> {
    val fillBox = fillBox(primaryDarkSwatch.fillColor)
    val focusScoreBox = glyphBox('_', secondarySwatch.fillColor)
    val focusLabelBox = labelBox(label, secondarySwatch.fillColor, Snap.LEFT)
    return box(
        name = "LineBox",
        render = {
            val bounds = edge.bounds
            fillBox.render(this)
            focusScoreBox.render(withEdgeBounds(bounds.confineToBottom()))
            focusLabelBox.render(withEdgeBounds(bounds.confineToTop().insetX(labelInset)))
        },
        focus = noFocus,
        setContent = noContent
    )
}

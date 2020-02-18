package pnote.projections

import pnote.projections.sandbox.*

private const val labelInset = 1

fun BoxContext.unfocusedEditFrame(label: String, labelAtTop: Boolean, swatch: ColorSwatch): Box<Void> {
    val fillBox = fillBox(swatch.disabledColor)
    val labelBox = labelBox(label, swatch.disabledColor, Snap.LEFT)
    val scoreBox = glyphBox('_', swatch.mediumColor)
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

fun BoxContext.focusedEditFrame(label: String, swatch: ColorSwatch, focusSwatch: ColorSwatch): Box<Void> {
    val fillBox = fillBox(swatch.disabledColor)
    val focusScoreBox = glyphBox('_', focusSwatch.fillColor)
    val focusLabelBox = labelBox(label, focusSwatch.fillColor, Snap.LEFT)
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

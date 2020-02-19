package pnote.projections

import pnote.projections.sandbox.*

private const val labelInset = 1

fun BoxContext.unfocusedEditFrame(
    label: String,
    labelAtTop: Boolean,
    swatch: ColorSwatch,
    glyphSwatch: ColorSwatch = swatch
): Box<Void> {
    val glyphColor = glyphSwatch.mediumColor
    val fillColor = swatch.disabledColor
    val fillBox = fillBox(fillColor)
    val labelBox = labelBox(label, glyphColor, Snap.LEFT)
    val scoreBox = glyphBox('_', glyphColor)
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
    val glyphColor = focusSwatch.fillColor
    val fillColor = swatch.disabledColor
    val fillBox = fillBox(fillColor)
    val focusLabelBox = labelBox(label, glyphColor, Snap.LEFT)
    val focusScoreBox = glyphBox('_', glyphColor)
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

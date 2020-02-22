package pnote.projections

import pnote.projections.sandbox.*
import java.lang.Integer.min

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
            val fillZ = fillBox.render(this)
            val scoreZ = scoreBox.render(withEdgeBounds(edge.bounds.confineToBottom()))
            val labelZ = if (labelAtTop) {
                labelBox.render(withEdgeBounds(edge.bounds.confineToTop().insetX(labelInset)))
            } else {
                labelBox.render(withEdgeBounds(edge.bounds.confineToY(1).insetX(labelInset)))
            }
            min(min(fillZ, scoreZ), labelZ)
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
            val fillZ = fillBox.render(this)
            val scoreZ = focusScoreBox.render(withEdgeBounds(edge.bounds.confineToBottom()))
            val labelZ = focusLabelBox.render(withEdgeBounds(edge.bounds.confineToTop().insetX(labelInset)))
            min(min(fillZ, scoreZ), labelZ)
        },
        focus = noFocus,
        setContent = noContent
    )
}

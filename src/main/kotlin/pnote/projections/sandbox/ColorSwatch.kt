package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor
import java.awt.Color

data class ColorSwatch(val strokeColor: TextColor, val fillColor: TextColor) {
    val highColor: TextColor by lazy { blend(.77f) }
    val mediumColor: TextColor by lazy { blend(.60f) }
    val disabledColor: TextColor by lazy { blend(.38f) }
}

fun ColorSwatch.blend(strength: Float): TextColor {
    fun Color.toList() = listOf(red, green, blue)
    val fillRgb = fillColor.toColor().toList()
    val strokeRgb = strokeColor.toColor().toList()
    val rgb = fillRgb.zip(strokeRgb)
    val blend = rgb.map { (fill, stroke) ->
        val delta = stroke - fill
        val move = (strength * delta).toInt()
        fill + move
    }
    return TextColor.Indexed.fromRGB(blend[0], blend[1], blend[2])
}

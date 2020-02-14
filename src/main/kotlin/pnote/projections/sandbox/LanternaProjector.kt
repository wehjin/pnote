package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor
import kotlinx.coroutines.GlobalScope

class LanternaProjector : BoxContext {
    override val primarySwatch: ColorSwatch =
        ColorSwatch(
            TextColor.ANSI.WHITE,
            TextColor.Indexed.fromRGB(0x34, 0x49, 0x55)
        )
    override val primaryDarkSwatch: ColorSwatch =
        ColorSwatch(
            TextColor.ANSI.WHITE,
            TextColor.Indexed.fromRGB(0x23, 0x2f, 0x34)
        )
    override val primaryLightSwatch: ColorSwatch =
        ColorSwatch(
            TextColor.ANSI.WHITE,
            TextColor.Indexed.fromRGB(0x4a, 0x65, 0x72)
        )
    override val surfaceSwatch: ColorSwatch =
        ColorSwatch(
            TextColor.ANSI.BLACK,
            TextColor.Indexed.fromRGB(0xFF, 0xFF, 0xFF)
        )
    override val secondarySwatch: ColorSwatch =
        ColorSwatch(
            TextColor.ANSI.BLACK,
            TextColor.Indexed.fromRGB(0xf9, 0xaa, 0x33)
        )

    fun start() {
        GlobalScope.projectBox {
            val passwordInput = inputBox()
            val checkInput = inputBox()
            val importButton = buttonBox("Import", secondarySwatch) { endProjection() }
            val inputCluster = passwordInput
                .packBottom(1, colorBox(null))
                .packBottom(1, checkInput)
                .packBottom(1, colorBox(null))
                .packBottom(1, importButton)
                .maxHeight(5)
            val sideBox = inputCluster.pad(2).before(colorBox(primaryDarkSwatch.color))
            val contentMessage = labelBox("Import Password", surfaceSwatch.glyphColor)
            val contentBackground = colorBox(surfaceSwatch.color)
            val contentBox = contentMessage.before(contentBackground)
            contentBox.packRight(30, sideBox)
        }
    }
}
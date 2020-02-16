package pnote.projections.sandbox

import com.googlecode.lanterna.TextColor
import pnote.projections.sandbox.ButtonBoxOption.*

class LanternaProjector : BoxContext {
    override val boxScreen: BoxScreen = lanternaBoxScreen()

    override val surfaceSwatch: ColorSwatch =
        ColorSwatch(
            TextColor.ANSI.BLACK,
            TextColor.Indexed.fromRGB(0xFF, 0xFF, 0xFF)
        )
    override val backgroundSwatch: ColorSwatch =
        ColorSwatch(
            TextColor.ANSI.BLACK,
            TextColor.Indexed.fromRGB(0xFF, 0xFF, 0xFF)
        )
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
    override val secondarySwatch: ColorSwatch =
        ColorSwatch(
            TextColor.ANSI.BLACK,
            TextColor.Indexed.fromRGB(0xf9, 0xaa, 0x33)
        )

    fun start() = boxScreen.apply {
        val passwordInput = inputBox()
        val checkInput = inputBox()
        val importButton = buttonBox(
            text = "Import",
            options = setOf(
                PressReader { endProjection() },
                EnabledSwatch(secondarySwatch),
                FocusedSwatch(primaryLightSwatch),
                PressedSwatch(primarySwatch)
            )
        )
        val inputCluster = passwordInput
            .packBottom(1, fillBox(null))
            .packBottom(1, checkInput)
            .packBottom(1, fillBox(null))
            .packBottom(1, importButton)
            .maxHeight(5)
        val sideBox = inputCluster.pad(2).before(fillBox(primaryDarkSwatch.fillColor))
        val contentMessage = labelBox("Import Password", surfaceSwatch.strokeColor)
        val contentBackground = fillBox(surfaceSwatch.fillColor)
        val contentBox = contentMessage.before(contentBackground)
        val box = contentBox.packRight(30, sideBox)
        setBox(box)
    }.joinBlocking()
}
package pnote.projections.sandbox

import com.googlecode.lanterna.input.KeyType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pnote.projections.sandbox.ButtonBoxOption.*

sealed class ButtonBoxOption {
    data class EnabledSwatch(val swatch: ColorSwatch) : ButtonBoxOption()
    data class FocusedSwatch(val swatch: ColorSwatch) : ButtonBoxOption()
    data class PressedSwatch(val swatch: ColorSwatch) : ButtonBoxOption()
    data class SparkReader(val spark: Spark, val block: SparkReadScope.() -> Unit) : ButtonBoxOption()
    data class PressReader(val block: () -> Unit) : ButtonBoxOption()
}

inline fun <reified T> Set<ButtonBoxOption>.get(): T? {
    val optionClass = T::class.java
    return this.firstOrNull { optionClass.isInstance(it) }?.let { optionClass.cast(it) }
}

fun BoxContext.textButtonBox(label: String, onPress: (() -> Unit)? = null): Box<Void> {
    val id = randomId()
    val swatch = secondarySwatch
    val enabledColor = swatch.fillColor
    val focusColor = swatch.highColor
    val pressColor = swatch.disabledColor
    var pressed = false
    val sparkReader: SparkReader? = null
    return box(
        name = "TextButtonBox",
        render = {
            val fillColor = when {
                pressed -> pressColor
                activeFocusId == id -> focusColor
                else -> null
            }
            val fillBox = fillColor?.let { fillBox(fillColor) } ?: gapBox()
            val glyphBox = labelBox(label.trim().toUpperCase(), enabledColor)
            val box = glyphBox.before(fillBox)
            box.render(this)
        },
        focus = {
            sparkReader?.also {
                readSpark(sparkReader.spark, sparkReader.block)
            }
            onPress?.also { onPress ->
                setFocusable(Focusable(id, edge.bounds, keyReader(id) { keyStroke ->
                    if (keyStroke.character == ' ' || keyStroke.keyType == KeyType.Enter) {
                        GlobalScope.launch {
                            pressed = true
                            setChanged(edge.bounds)
                            delay(200)
                            pressed = false
                            setChanged(edge.bounds)
                            delay(100)
                            onPress()
                        }
                        true
                    } else false
                }))
            }
        },
        setContent = noContent
    )
}

fun BoxContext.buttonBox(text: String, options: Set<ButtonBoxOption> = emptySet()): Box<Void> {
    val id = randomId()
    val enabledSwatch = options.get<EnabledSwatch>()?.swatch ?: surfaceSwatch
    val focusedSwatch = options.get<FocusedSwatch>()?.swatch ?: primaryLightSwatch
    val pressedSwatch = options.get<PressedSwatch>()?.swatch ?: primarySwatch
    val pressBlock = options.get<PressReader>()?.block
    val sparkReader = options.get<SparkReader>()
    var pressed = false
    return box(
        name = "ButtonBox",
        render = {
            val swatch = when {
                pressed -> pressedSwatch
                activeFocusId == id -> focusedSwatch
                else -> enabledSwatch
            }
            val fill = fillBox(swatch.fillColor)
            val box = labelBox(text.trim(), swatch.strokeColor).before(fill)
            box.render(this)
        },
        focus = {
            sparkReader?.also {
                readSpark(sparkReader.spark, sparkReader.block)
            }
            pressBlock?.also {
                setFocusable(Focusable(id, edge.bounds, keyReader(id) { keyStroke ->
                    if (keyStroke.character == ' ' || keyStroke.keyType == KeyType.Enter) {
                        GlobalScope.launch {
                            pressed = true
                            setChanged(edge.bounds)
                            delay(200)
                            pressed = false
                            setChanged(edge.bounds)
                            delay(100)
                            pressBlock()
                        }
                        true
                    } else false
                }))
            }
        },
        setContent = noContent
    )
}
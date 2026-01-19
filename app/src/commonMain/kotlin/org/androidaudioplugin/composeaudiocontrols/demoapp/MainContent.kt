package org.androidaudioplugin.composeaudiocontrols.demoapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import composeaudiocontrols.app.generated.resources.*
import composeaudiocontrols.app.generated.resources.Res
import composeaudiocontrols.app.generated.resources.bright_life
import composeaudiocontrols.app.generated.resources.knob_blue
import composeaudiocontrols.app.generated.resources.vst_knob_01_100pix
import dev.atsushieno.ktmidi.EmptyMidiAccess
import dev.atsushieno.ktmidi.MidiAccess
import org.androidaudioplugin.composeaudiocontrols.DefaultKnobTooltip
import org.androidaudioplugin.composeaudiocontrols.ImageStripKnob
import org.androidaudioplugin.composeaudiocontrols.ImageStripKnobScope
import org.androidaudioplugin.composeaudiocontrols.defaultKnobMinSizeInDp
import org.androidaudioplugin.composeaudiocontrols.midi.DiatonicLiveMidiKeyboard
import org.androidaudioplugin.composeaudiocontrols.midi.KtMidiDeviceAccessScope
import org.androidaudioplugin.composeaudiocontrols.midi.MidiDeviceConfigurator
import org.androidaudioplugin.composeaudiocontrols.midi.MidiKnobControllerCombo
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.imageResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

internal fun formatLabelNumber(v: Float, charsInPositiveNumber: Int = 5) = v.toDouble().toString().take(charsInPositiveNumber + if (v < 0) 1 else 0)

var midiAccess: MidiAccess = EmptyMidiAccess()

@Composable
fun MainContent() {
    Column {
        DiatonicMidiKeyboardDemo()
        ImageStripKnobDemo()
    }
}

@Composable
fun DiatonicMidiKeyboardDemo() {
    val scope by remember { mutableStateOf(KtMidiDeviceAccessScope(midiAccess)) }
    SectionLabel("DiatonicKeyboard Demo")
    scope.MidiDeviceConfigurator()
    scope.DiatonicLiveMidiKeyboard()
    scope.MidiKnobControllerCombo(generateVerticalSpriteSheet(64, 64))
}

private fun generateVerticalSpriteSheet(size: Int = 64, frames: Int = 64): ImageBitmap {
    val totalHeight = size * frames

    val bitmap = ImageBitmap(size, totalHeight, ImageBitmapConfig.Argb8888)
    val canvas = Canvas(bitmap)

    val drawScope = CanvasDrawScope()

    drawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = androidx.compose.ui.geometry.Size(size.toFloat(), totalHeight.toFloat())
    ) {
        // Clear background (optional, depends if you want transparency)
        drawRect(color = Color.Transparent, blendMode = BlendMode.Clear)

        for (i in 0 until frames) {
            // Calculate the angle for this frame
            // Range: 240 (Bottom-Left) to -60 (Bottom-Right)
            val angleDeg = 240f + (i.toFloat() / (frames - 1)) * (-60f - 240f)
            val angleRad = angleDeg * (PI.toFloat() / 180f)

            // Center of the current 64x64 box
            val centerY = (i * size) + (size / 2f)
            val centerX = size / 2f
            val center = Offset(centerX, centerY)

            // Calculate end point of the line (radius = 24 pixels)
            val radius = 24f
            val endOffset = Offset(
                x = centerX + radius * cos(angleRad),
                // Subtract sin because Y-axis increases downwards in Compose
                y = centerY - radius * sin(angleRad)
            )

            // Draw the angled line
            drawLine(
                color = Color.Gray, // Or your preferred color
                start = center,
                end = endOffset,
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }

    return bitmap
}

@Composable
fun SectionLabel(text: String) {
    HorizontalDivider()
    Text(
        text, fontSize = 20.sp, color = MaterialTheme.colorScheme.inversePrimary,
        modifier = Modifier
            .padding(10.dp)
            .background(MaterialTheme.colorScheme.inverseSurface)
    )
    Divider()
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ImageStripKnobDemo() {
    Column {
        SectionLabel("ImageStripKnob Demo")

        var minSizeCheckedState by remember { mutableStateOf(false) }
        Row {
            Text("use the original image size w/o min. size?", modifier = Modifier.align(Alignment.CenterVertically))
            Checkbox(
                checked = minSizeCheckedState,
                onCheckedChange = { minSizeCheckedState = !minSizeCheckedState })
        }

        var knobSize: Int? by remember { mutableStateOf(null) }
        KnobSizeSelector(knobSize, onSizeChange = { knobSize = it })

        var knobStyle by remember { mutableStateOf(Res.drawable.bright_life) }
        KnobStyleSelector(knobStyle, onSelectionChange = { knobStyle = it })

        val scrollState = rememberScrollState()
        Column(Modifier.verticalScroll(scrollState)) {
            (0 until 10).forEach { paramIndex ->
                Row {
                    var paramValue by remember { mutableFloatStateOf(0f) }
                    Text("Parameter $paramIndex: ", modifier = Modifier.align(Alignment.CenterVertically))
                    val minSize =
                        if (minSizeCheckedState) 1.dp
                        else if ((48 > (knobSize ?: 48))) knobSize!!.dp
                        else defaultKnobMinSizeInDp
                    ImageStripKnob(drawableRes = knobStyle,
                        value = paramValue,
                        valueRange = 0f..1f * 2f.pow(paramIndex.toFloat()),
                        minSizeInDp = minSize,
                        explicitSizeInDp = knobSize?.dp,
                        //tooltip = { _,_ -> },
                        onValueChange = {
                            paramValue = it
                            println("value at $paramIndex changed: $it")
                        })
                    Text(formatLabelNumber(paramValue, 7))
                    TextButton(onClick = { paramValue /= 2f }) {
                        Text("divide by 2", modifier = Modifier.border(1.dp, Color.Black))
                    }
                }
            }
        }
    }
}

@Composable
fun KnobSizeSelector(size: Int?, onSizeChange: (size: Int?) -> Unit) {
    var checkedState by remember { mutableStateOf(false) }
    var sizeOnSlider by remember { mutableFloatStateOf(size?.toFloat() ?: 48f) }
    val update = {
        if (checkedState)
            onSizeChange(sizeOnSlider.toInt())
        else
            onSizeChange(null)
    }
    Row {
        Text("Use explicit size", modifier = Modifier.align(Alignment.CenterVertically))
        Checkbox(checked = checkedState, onCheckedChange = {
            checkedState = !checkedState
            update()
        })
        Slider(
            modifier = Modifier.widthIn(50.dp, 100.dp),
            value = sizeOnSlider,
            valueRange = 8f.rangeTo(192f),
            onValueChange = {
                sizeOnSlider = it
                update()
            })
        Text("${sizeOnSlider.toInt()} dp", modifier = Modifier.align(Alignment.CenterVertically))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun KnobStyleSelector(currentResId: DrawableResource, onSelectionChange: (id: DrawableResource) -> Unit) {
    val imageOptions = mapOf(
        Res.drawable.bright_life to "bright_life",
        Res.drawable.chromed_knob to "chromed_knob",
        Res.drawable.knob_blue to "knob_blue",
        Res.drawable.vst_knob_01_100pix to "vst_knob_01_100pix")
    var imageOptionsExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = imageOptionsExpanded,
        onExpandedChange = {
            imageOptionsExpanded = !imageOptionsExpanded
        }
    ) {
        TextField(
            imageOptions[currentResId] ?: "WTF!?",
            {},
            readOnly = true,
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = imageOptionsExpanded,
            onDismissRequest = {
                imageOptionsExpanded = false
            }
        ) {
            imageOptions.forEach { selectionOption ->
                DropdownMenuItem(
                    text = {
                        Text(text = selectionOption.value)
                    },
                    onClick = {
                        imageOptionsExpanded = false
                        onSelectionChange(selectionOption.key)
                    }
                )
            }
        }
    }
}


// FIXME: can we have this implementation in library, not app?
//  Currently `DrawableResource` and `imageResource` are not in accessible libraries
//  unless any resource is actually generated...
@OptIn(ExperimentalResourceApi::class)
@Composable
fun ImageStripKnob(modifier: Modifier = Modifier,
                   drawableRes: DrawableResource,
                   value: Float = 0f,
                   valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
                   explicitSizeInDp: Dp? = null,
                   minSizeInDp: Dp = defaultKnobMinSizeInDp,
                   fineModeDelayMs: Int = 1000,
                   tooltipColor: Color = Color.Gray,
                   tooltip: @Composable ImageStripKnobScope.() -> Unit = {
                       // by default, show tooltip only when it is being dragged
                       DefaultKnobTooltip(
                           value = knobValue,
                           showTooltip = knobIsBeingDragged,
                           textColor = tooltipColor
                       )
                   },
                   onValueChange: (value: Float) -> Unit = {}
) {
    val imageBitmap = imageResource(drawableRes)
    ImageStripKnob(
        modifier,
        imageBitmap,
        value,
        valueRange,
        explicitSizeInDp,
        minSizeInDp,
        fineModeDelayMs,
        tooltipColor,
        tooltip,
        onValueChange
    )
}

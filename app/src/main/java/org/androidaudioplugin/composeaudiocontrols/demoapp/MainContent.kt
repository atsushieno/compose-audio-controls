package org.androidaudioplugin.composeaudiocontrols.demoapp

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atsushieno.ktmidi.AndroidMidi2Access
import dev.atsushieno.ktmidi.AndroidMidiAccess
import dev.atsushieno.ktmidi.EmptyMidiAccess
import org.androidaudioplugin.composeaudiocontrols.ImageStripKnob
import org.androidaudioplugin.composeaudiocontrols.defaultKnobMinSizeInDp
import org.androidaudioplugin.composeaudiocontrols.demoapp.ui.theme.ComposeAudioControlsTheme
import org.androidaudioplugin.composeaudiocontrols.midi.DiatonicLiveMidiKeyboard
import org.androidaudioplugin.composeaudiocontrols.midi.KtMidiDeviceAccessScope
import org.androidaudioplugin.composeaudiocontrols.midi.MidiDeviceConfigurator
import kotlin.math.pow

internal fun formatLabelNumber(v: Float, charsInPositiveNumber: Int = 5) = v.toBigDecimal().toPlainString().take(charsInPositiveNumber + if (v < 0) 1 else 0)

@Composable
fun MainContent() {
    Column {
        DiatonicMidiKeyboardDemo()
        ImageStripKnobDemo()
    }
}

@Composable
fun DiatonicMidiKeyboardDemo() {
    val context = LocalContext.current
    val midiAccess by remember { mutableStateOf(AndroidMidi2Access(context, true)) }
    val scope by remember { mutableStateOf(KtMidiDeviceAccessScope(midiAccess)) }
    SectionLabel("DiagnosticKeyboard Demo")
    scope.MidiDeviceConfigurator()
    scope.DiatonicLiveMidiKeyboard()
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

@Preview(showBackground = true)
@Composable
fun KnobPreview() {
    ComposeAudioControlsTheme {
        ImageStripKnobDemo()
    }
}

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

        var knobStyle by remember { mutableIntStateOf(R.drawable.bright_life) }
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
                    ImageStripKnob(drawableResId = knobStyle,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnobStyleSelector(currentResId: Int, onSelectionChange: (id: Int) -> Unit) {
    val imageOptions = mapOf(
        R.drawable.bright_life to "bright_life",
        R.drawable.chromed_knob to "chromed_knob",
        R.drawable.knob_blue to "knob_blue",
        R.drawable.vst_knob_01_100pix to "vst_knob_01_100pix")
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

@Preview(showBackground = true)
@Composable
fun DiatonicKeyboardPreview() {
    ComposeAudioControlsTheme {
        Column {
            val scope by remember { mutableStateOf(KtMidiDeviceAccessScope(EmptyMidiAccess())) }
            scope.MidiDeviceConfigurator()
            scope.DiatonicLiveMidiKeyboard()
        }
    }
}

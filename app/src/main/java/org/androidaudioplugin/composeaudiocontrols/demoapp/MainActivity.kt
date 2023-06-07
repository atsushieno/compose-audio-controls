package org.androidaudioplugin.composeaudiocontrols.demoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.androidaudioplugin.composeaudiocontrols.DiatonicKeyboardNoteExpressionOrigin
import org.androidaudioplugin.composeaudiocontrols.DiatonicKeyboardWithControllers
import org.androidaudioplugin.composeaudiocontrols.ImageStripKnob
import org.androidaudioplugin.composeaudiocontrols.defaultKnobMinSizeInDp
import org.androidaudioplugin.composeaudiocontrols.demoapp.ui.theme.ComposeAudioControlsTheme
import kotlin.math.pow

internal fun formatLabelNumber(v: Float, charsInPositiveNumber: Int = 5) = v.toBigDecimal().toPlainString().take(charsInPositiveNumber + if (v < 0) 1 else 0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeAudioControlsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        DiatonicKeyboardDemo()
                        ImageStripKnobDemo()
                    }
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Divider()
    Text(text, fontSize = 20.sp, color = MaterialTheme.colorScheme.inversePrimary,
        modifier = Modifier.padding(10.dp).background(MaterialTheme.colorScheme.inverseSurface))
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
            Text("use the original image size w/o min. size?")
            Checkbox(checked = minSizeCheckedState, onCheckedChange = { minSizeCheckedState = !minSizeCheckedState })
        }

        var knobSize: Int? by remember { mutableStateOf(null) }
        KnobSizeSelector(knobSize, onSizeChange = { knobSize = it })

        var knobStyle by remember { mutableStateOf(R.drawable.bright_life) }
        KnobStyleSelector(knobStyle, onSelectionChange = { knobStyle = it })

        val scrollState = rememberScrollState()
        Column(Modifier.verticalScroll(scrollState)) {
            (0 until 10).forEach { paramIndex ->
                Row {
                    var paramValue by remember { mutableStateOf(0f) }
                    Text("Parameter $paramIndex: ")
                    ImageStripKnob(drawableResId = knobStyle,
                        value = paramValue,
                        valueRange = 0f..1f * 2f.pow(paramIndex.toFloat()),
                        minSizeInDp = if (minSizeCheckedState) 1.dp else if ((48 > (knobSize
                                ?: 48))
                        ) knobSize!!.dp else defaultKnobMinSizeInDp,
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
    var sizeOnSlider by remember { mutableStateOf(size?.toFloat() ?: 48f) }
    val update = {
        if (checkedState)
            onSizeChange(sizeOnSlider.toInt())
        else
            onSizeChange(null)
    }
    Row {
        Text("Use explicit size")
        Checkbox(checked = checkedState, onCheckedChange = {
            checkedState = !checkedState
            update()
        })
        Slider(modifier = Modifier.widthIn(50.dp, 100.dp), value = sizeOnSlider, valueRange = 8f.rangeTo(192f), onValueChange = {
            sizeOnSlider = it
            update()
        })
        Text("${sizeOnSlider.toInt()} dp")
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
        TextField(imageOptions[currentResId] ?: "WTF!?",
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
        DiatonicKeyboardDemo()
    }
}

@Composable
fun DiatonicKeyboardDemo() {
    Column {
        SectionLabel("DiagnosticKeyboard Demo")

        Text("Show controllers?")
        var showExprToggle by remember { mutableStateOf(true) }
        var showSense by remember { mutableStateOf(true) }
        var showOctave by remember { mutableStateOf(true) }
        Row {
            Text("Expr switch")
            Checkbox(checked = showExprToggle, onCheckedChange = { showExprToggle = !showExprToggle } )
            Text("Expr. Sense")
            Checkbox(checked = showSense, onCheckedChange = { showSense = !showSense } )
            Text("Octave")
            Checkbox(checked = showOctave, onCheckedChange = { showOctave = !showOctave } )
        }

        val noteOnStates = remember { List(128) { 0L }.toMutableStateList() }
        var expressionX by remember { mutableStateOf(0f) }
        var expressionY by remember { mutableStateOf(0f) }
        var expressionP by remember { mutableStateOf(0f) }

        DiatonicKeyboardWithControllers(
            noteOnStates.toList(),
            showNoteExpressionToggle = showExprToggle,
            showExpressionSensitivitySlider = showSense,
            showOctaveSlider = showOctave,
            onNoteOn = { note, _ ->
                println("NoteOn: $note")
                noteOnStates[note] = 1
            },
            onNoteOff = { note, _ ->
                println("NoteOff: $note")
                noteOnStates[note] = 0
            },
            onExpression = { dir, note, data ->
                println("Note Expression at $note: $dir $data")
                when (dir) {
                    DiatonicKeyboardNoteExpressionOrigin.HorizontalDragging -> expressionX = data
                    DiatonicKeyboardNoteExpressionOrigin.VerticalDragging -> expressionY = data
                    DiatonicKeyboardNoteExpressionOrigin.Pressure -> expressionP = data
                    else -> {}
                }
            }
        )
        Text("Expression (latest) X: ${expressionX.toString().take(5)} / Y: ${expressionY.toString().take(5)} / P:  ${expressionP.toString().take(5)}")
    }
}
package org.androidaudioplugin.composeaudiocontrols.midi

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.atsushieno.ktmidi.MidiCC
import dev.atsushieno.ktmidi.MidiChannelStatus
import dev.atsushieno.ktmidi.Ump
import dev.atsushieno.ktmidi.UmpFactory
import dev.atsushieno.ktmidi.WellKnownNames
import dev.atsushieno.ktmidi.toPlatformNativeBytes
import org.androidaudioplugin.composeaudiocontrols.DefaultKnobTooltip
import org.androidaudioplugin.composeaudiocontrols.ImageStripKnob

val midi1Range = 0..127
data class Enumeration(val label: String, val status: Int,
                       val range1: IntRange, val range2: IntRange = IntRange.EMPTY, val range3: IntRange = IntRange.EMPTY,
                       val prefix1: String = "", val prefix2: String = "", val prefix3: String = "")
val controlTargetCatalog = listOf(
    Enumeration("CC", MidiChannelStatus.CC, midi1Range, midi1Range),
    Enumeration("CAf", MidiChannelStatus.CAF, midi1Range),
    Enumeration("PAf", MidiChannelStatus.PAF, midi1Range, 0..127, prefix1 = "key:"),
    Enumeration("PitchBend", MidiChannelStatus.PITCH_BEND, -0x2000 .. 0x1FFF),
    Enumeration("PN-PB", MidiChannelStatus.PER_NOTE_PITCH_BEND, midi1Range, -0x2000 .. 0x1FFF, prefix1 = "key:"),
    Enumeration("RPN", MidiChannelStatus.RPN, midi1Range, midi1Range, midi1Range, "M:", "L:", "D:"),
    Enumeration("NRPN", MidiChannelStatus.NRPN, midi1Range, midi1Range, midi1Range, "M:", "L:", "D:"),
    Enumeration("PN-RCC", MidiChannelStatus.PER_NOTE_RCC, midi1Range, midi1Range, midi1Range, "key:"),
    Enumeration("PN-ACC", MidiChannelStatus.PER_NOTE_ACC, midi1Range, midi1Range, midi1Range, "key:"),
    Enumeration("Program", MidiChannelStatus.PROGRAM, midi1Range, midi1Range, midi1Range, "p:", "BkM:", "BkL:"),
)


@Composable
fun ControlTargetSelector(modifier: Modifier = Modifier,
                          index: Int,
                          controlTargets: List<Enumeration> = controlTargetCatalog,
                          onSelectionChange: (Int) -> Unit = { _ -> }) {
    Column(modifier) {
        var listExpanded by remember { mutableStateOf(false) }
        val currentText = controlTargets[index].label
        Button(onClick = { listExpanded = true }) {
            Text(currentText, color = LocalContentColor.current) }
        DropdownMenu(
            modifier = modifier,
            expanded = listExpanded,
            onDismissRequest = { listExpanded = false }) {
            controlTargets.forEachIndexed { index, device ->
                DropdownMenuItem(text = { Text(device.label, color = LocalContentColor.current) }, onClick = {
                    onSelectionChange(index)
                    listExpanded = false
                })
            }
        }
    }
}

private fun MidiDeviceAccessScope.sendValueChange(status: Int, v1: Int, v2: Int, v3: Int) {
    when(status) {
        MidiChannelStatus.CC -> {
            if (useMidi2Protocol) {
                val ump = UmpFactory.midi2CC(0, 0, v1, v2.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val bytes = arrayOf(MidiChannelStatus.CC.toByte(), v1.toByte(), v2.toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        MidiChannelStatus.CAF -> {
            if (useMidi2Protocol) {
                val ump = UmpFactory.midi2CAf(0, 0, v1.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val bytes = arrayOf(MidiChannelStatus.CAF.toByte(), v1.toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        MidiChannelStatus.PAF -> {
            if (useMidi2Protocol) {
                val ump = UmpFactory.midi2PAf(0, 0, v1, v2.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val bytes = arrayOf(MidiChannelStatus.PAF.toByte(), v1.toByte(), v2.toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        MidiChannelStatus.PITCH_BEND -> {
            if (useMidi2Protocol) {
                val ump = UmpFactory.midi2PitchBendDirect(0, 0, v1.toLong() shl 18)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val p1 = v1 % 0x80
                val p2 = v2 / 0x80
                val bytes = arrayOf(MidiChannelStatus.PITCH_BEND.toByte(), p1.toByte(), p2.toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        MidiChannelStatus.PER_NOTE_PITCH_BEND -> {
            if (useMidi2Protocol) {
                val ump = UmpFactory.midi2PerNotePitchBendDirect(0, 0, v1, v2.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                // not supported in MIDI 1.0
            }
        }
        MidiChannelStatus.RPN -> {
            if (useMidi2Protocol) {
                val ump = UmpFactory.midi2RPN(0, 0, v1, v2, v3.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val bytes = arrayOf(MidiChannelStatus.CC.toByte(), MidiCC.RPN_MSB.toByte(), v1.toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.RPN_LSB.toByte(), v2.toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.DTE_MSB.toByte(), v3.toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.DTE_LSB.toByte(), 0.toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        MidiChannelStatus.NRPN -> {
            if (useMidi2Protocol) {
                val ump = UmpFactory.midi2NRPN(0, 0, v1, v2, v3.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val bytes = arrayOf(MidiChannelStatus.CC.toByte(), MidiCC.NRPN_MSB.toByte(), v1.toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.NRPN_LSB.toByte(), v2.toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.DTE_MSB.toByte(), v3.toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.DTE_LSB.toByte(), 0.toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        MidiChannelStatus.PER_NOTE_RCC -> {
            if (useMidi2Protocol) {
                val ump = UmpFactory.midi2PerNoteRCC(0, 0, v1, v2, v3.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                // not supported in MIDI 1.0
            }
        }
        MidiChannelStatus.PER_NOTE_ACC -> {
            if (useMidi2Protocol) {
                val ump = UmpFactory.midi2PerNoteACC(0, 0, v1, v2, v3.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                // not supported in MIDI 1.0
            }
        }
        MidiChannelStatus.PROGRAM -> {
            if (useMidi2Protocol) {
                val ump = UmpFactory.midi2Program(0, 0, 0, v1, v2, v3)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val bytes = arrayOf(MidiChannelStatus.CC.toByte(), MidiCC.BANK_SELECT.toByte(), v2.toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.BANK_SELECT_LSB.toByte(), v3.toByte(),
                    MidiChannelStatus.PROGRAM.toByte(), v1.toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        else -> {}
    }
}

private val knobPadding = 6.dp

@Composable
fun MidiDeviceAccessScope.MidiKnobControllerCombo(knobBitmap: ImageBitmap) {
    Row {
        var target by remember { mutableStateOf(0) }
        ControlTargetSelector(index = target, onSelectionChange = { target = it })

        var control1 by remember { mutableStateOf(0.0f) }
        val range1 = controlTargetCatalog[target].range1
        var control2 by remember { mutableStateOf(0.0f) }
        val range2 = controlTargetCatalog[target].range2
        var control3 by remember { mutableStateOf(0.0f) }
        val range3 = controlTargetCatalog[target].range3
        var discrete by remember { mutableStateOf(true) }

        ImageStripKnob(imageBitmap = knobBitmap,
            modifier = Modifier.padding(knobPadding, 0.dp),
            value = control1,
            valueRange = range1.first.toFloat()..range1.last.toFloat(),
            tooltip = {
                DefaultKnobTooltip(showTooltip = true, value = control1, valueText = when(target) {
                    0 -> WellKnownNames.ccNames[127.coerceAtMost(knobValue.toInt())] ?: "(N/A)"
                    else -> controlTargetCatalog[target].prefix1 + control1.toInt().toString()
                })
            },
            onValueChange = {
                control1 = it
                if (discrete)
                    sendValueChange(controlTargetCatalog[target].status, control1.toInt(), control2.toInt(), control3.toInt())
            })

        if (range2 != IntRange.EMPTY) {
            ImageStripKnob(imageBitmap = knobBitmap,
                modifier = Modifier.padding(knobPadding, 0.dp),
                value = control2,
                valueRange = range2.first.toFloat()..range2.last.toFloat(),
                tooltip = { DefaultKnobTooltip(showTooltip = true, value = control2,
                    valueText = controlTargetCatalog[target].prefix2 + control2.toInt().toString()) },
                onValueChange = {
                    control2 = it
                    if (discrete)
                        sendValueChange(controlTargetCatalog[target].status, control1.toInt(), control2.toInt(), control3.toInt())
                })
        }

        if (controlTargetCatalog[target].range3 != IntRange.EMPTY) {
            ImageStripKnob(imageBitmap = knobBitmap,
                modifier = Modifier.padding(knobPadding, 0.dp),
                value = control3,
                valueRange = range3.first.toFloat()..range3.last.toFloat(),
                tooltip = { DefaultKnobTooltip(showTooltip = true, value = control3,
                    valueText = controlTargetCatalog[target].prefix3 + control3.toInt().toString()) },
                onValueChange = {
                    control3 = it
                    if (discrete)
                        sendValueChange(controlTargetCatalog[target].status, control1.toInt(), control2.toInt(), control3.toInt())
                })
        }

        Box(Modifier.align(Alignment.CenterVertically).background(Color.White).padding(knobPadding, 0.dp)
            .clickable {
                discrete = !discrete
                if (discrete)
                    sendValueChange(controlTargetCatalog[target].status, control1.toInt(), control2.toInt(), control3.toInt())
            }) {
            Image(imageVector = if (discrete) Icons.Default.CheckCircle else Icons.Default.Close,
                "send", Modifier.size(32.dp))
        }
    }
}

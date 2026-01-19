package org.androidaudioplugin.composeaudiocontrols.midi

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlinedfilled.Block
import com.composables.icons.materialsymbols.outlinedfilled.Circle
import com.composables.icons.materialsymbols.outlinedfilled.Disabled_visible
import dev.atsushieno.ktmidi.MidiCC
import dev.atsushieno.ktmidi.MidiChannelStatus
import dev.atsushieno.ktmidi.Ump
import dev.atsushieno.ktmidi.UmpFactory
import dev.atsushieno.ktmidi.WellKnownNames
import dev.atsushieno.ktmidi.toPlatformNativeBytes
import org.androidaudioplugin.composeaudiocontrols.DefaultKnobTooltip
import org.androidaudioplugin.composeaudiocontrols.ImageStripKnob

val midi1Range = 0 until 0x80
val midi1Range14 = 0 until 0x80 * 0x80

data class ControlSettings(val prefix: String = "", val sendEvent: Boolean = true, val range: IntRange = IntRange.EMPTY)
val midi1Control = ControlSettings(range = midi1Range)
val noteControl = ControlSettings("key:", false, midi1Range)
val noControl = ControlSettings("", false)

data class ControlTargetDefinition(val label: String, val status: Int,
                                   val control1: ControlSettings,
                                   val control2: ControlSettings = noControl,
                                   val control3: ControlSettings = noControl)

val controlTargetCatalog = listOf(
    ControlTargetDefinition("CC", MidiChannelStatus.CC,
        ControlSettings(sendEvent = false, range = midi1Range),
        midi1Control),
    ControlTargetDefinition("CAf", MidiChannelStatus.CAF, midi1Control),
    ControlTargetDefinition("PAf", MidiChannelStatus.PAF, noteControl, midi1Control),
    ControlTargetDefinition("PitchBend", MidiChannelStatus.PITCH_BEND, ControlSettings(range = -0x2000 .. 0x1FFF)),
    ControlTargetDefinition("PN-PB", MidiChannelStatus.PER_NOTE_PITCH_BEND, noteControl, ControlSettings(range = -0x2000 .. 0x1FFF)),
    ControlTargetDefinition("RPN", MidiChannelStatus.RPN,
        ControlSettings("M:", false, midi1Range),
        ControlSettings("L:", false, midi1Range),
        ControlSettings("D:", range = midi1Range14)),
    ControlTargetDefinition("NRPN", MidiChannelStatus.NRPN,
        ControlSettings("M:", false, midi1Range),
        ControlSettings("L:", false, midi1Range),
        ControlSettings("D:", range = midi1Range14)),
    ControlTargetDefinition("PN-RCC", MidiChannelStatus.PER_NOTE_RCC, noteControl,
        ControlSettings("idx:", false, midi1Range),
        ControlSettings("data:", range = midi1Range14)),
    ControlTargetDefinition("PN-ACC", MidiChannelStatus.PER_NOTE_ACC, noteControl,
        ControlSettings("idx:", false, midi1Range),
        ControlSettings("data:", range = midi1Range14)),
    ControlTargetDefinition("Program", MidiChannelStatus.PROGRAM,
        ControlSettings("P:", range = midi1Range),
        ControlSettings("BkM:", range = midi1Range),
        ControlSettings("BkL:", range = midi1Range))
)


@Composable
fun ControlTargetSelector(modifier: Modifier = Modifier,
                          index: Int,
                          controlTargets: List<ControlTargetDefinition> = controlTargetCatalog,
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
            if (isTransportUmp) {
                val ump = UmpFactory.midi2CC(0, 0, v1, v2.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val bytes = arrayOf(MidiChannelStatus.CC.toByte(), v1.toByte(), v2.toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        MidiChannelStatus.CAF -> {
            if (isTransportUmp) {
                val ump = UmpFactory.midi2CAf(0, 0, v1.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val bytes = arrayOf(MidiChannelStatus.CAF.toByte(), v1.toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        MidiChannelStatus.PAF -> {
            if (isTransportUmp) {
                val ump = UmpFactory.midi2PAf(0, 0, v1, v2.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val bytes = arrayOf(MidiChannelStatus.PAF.toByte(), v1.toByte(), v2.toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        MidiChannelStatus.PITCH_BEND -> {
            if (isTransportUmp) {
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
            if (isTransportUmp) {
                val ump = UmpFactory.midi2PerNotePitchBendDirect(0, 0, v1, v2.toLong() shl 25)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                // not supported in MIDI 1.0
            }
        }
        MidiChannelStatus.RPN -> {
            if (isTransportUmp) {
                val ump = UmpFactory.midi2RPN(0, 0, v1, v2, v3.toLong() shl 18)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val bytes = arrayOf(MidiChannelStatus.CC.toByte(), MidiCC.RPN_MSB.toByte(), v1.toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.RPN_LSB.toByte(), v2.toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.DTE_MSB.toByte(), (v3 / 0x80).toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.DTE_LSB.toByte(), (v3 % 0x80).toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        MidiChannelStatus.NRPN -> {
            if (isTransportUmp) {
                val ump = UmpFactory.midi2NRPN(0, 0, v1, v2, v3.toLong() shl 18)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                val bytes = arrayOf(MidiChannelStatus.CC.toByte(), MidiCC.NRPN_MSB.toByte(), v1.toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.NRPN_LSB.toByte(), v2.toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.DTE_MSB.toByte(), (v3 / 0x80).toByte(),
                    MidiChannelStatus.CC.toByte(), MidiCC.DTE_LSB.toByte(), (v3 % 0x80).toByte())
                send(bytes.toByteArray(), 0, bytes.size, 0)
            }
        }
        MidiChannelStatus.PER_NOTE_RCC -> {
            if (isTransportUmp) {
                val ump = UmpFactory.midi2PerNoteRCC(0, 0, v1, v2, v3.toLong() shl 18)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                // not supported in MIDI 1.0
            }
        }
        MidiChannelStatus.PER_NOTE_ACC -> {
            if (isTransportUmp) {
                val ump = UmpFactory.midi2PerNoteACC(0, 0, v1, v2, v3.toLong() shl 18)
                send(Ump(ump).toPlatformNativeBytes(), 0, 8, 0)
            } else {
                // not supported in MIDI 1.0
            }
        }
        MidiChannelStatus.PROGRAM -> {
            if (isTransportUmp) {
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

// Can we split this function to small pieces? those remembered states make it impossible...
@Composable
fun MidiDeviceAccessScope.MidiKnobControllerCombo(knobBitmap: ImageBitmap) {
    Row {
        var targetChanged by remember { mutableStateOf(false) }
        var target by remember { mutableStateOf(0) }
        ControlTargetSelector(index = target, onSelectionChange = {
            target = it
            targetChanged = true
        })

        // Those remembered values help storing and restoring the control values with MidiMachine or Midi2Machine.
        // along with `updateValueState()` lambda (defined later).
        var lastCCIndex by remember { mutableStateOf(0) }
        var lastPAFNote by remember { mutableStateOf(0x40) }
        var lastPNPBNote by remember { mutableStateOf(0x40) }
        var lastRPN by remember { mutableStateOf(0) }
        var lastNRPN by remember { mutableStateOf(0) }
        var lastPNRCCNote by remember { mutableStateOf(0x40) }
        var lastPNRCCIndex by remember { mutableStateOf(0) }
        var lastPNACCNote by remember { mutableStateOf(0x40) }
        var lastPNACCIndex by remember { mutableStateOf(0) }
        var lastBank by remember { mutableStateOf(0) }

        val controlTarget = controlTargetCatalog[target]
        var control1 by remember { mutableStateOf(0.0f) }
        val range1 = controlTarget.control1.range
        var control2 by remember { mutableStateOf(0.0f) }
        val range2 = controlTarget.control2.range
        var control3 by remember { mutableStateOf(0.0f) }
        val range3 = controlTarget.control3.range
        var discrete by remember { mutableStateOf(true) }

        if (targetChanged) {
            if (isTransportUmp) {
                val reg = midi2Machine.channel(0)
                when (controlTarget.status) {
                    MidiChannelStatus.CC -> {
                        control1 = lastCCIndex.toFloat()
                        control2 = (reg.controls[control1.toInt()] shr 25).toFloat()
                    }
                    MidiChannelStatus.CAF -> {
                        control1 = (reg.caf shr 25).toFloat()
                        control2 = 0f
                        control3 = 0f
                    }
                    MidiChannelStatus.PAF -> {
                        control1 = lastPAFNote.toFloat()
                        control2 = (reg.pafVelocity[lastPAFNote] shr 25).toFloat()
                        control3 = 0f
                    }
                    MidiChannelStatus.PITCH_BEND -> {
                        control1 = ((reg.pitchbend shr 18) - 0x2000u).toFloat()
                        control2 = 0f
                        control3 = 0f
                    }
                    MidiChannelStatus.PER_NOTE_PITCH_BEND -> {
                        control1 = lastPNPBNote.toFloat()
                        control2 = ((reg.perNotePitchbend[lastPNPBNote] shr 18) - 0x2000u).toFloat()
                        control3 = 0f
                    }
                    MidiChannelStatus.RPN -> {
                        control1 = (lastRPN / 0x80).toFloat()
                        control2 = (lastRPN % 0x80).toFloat()
                        control3 = (reg.rpns[lastRPN] shr 18).toFloat()
                    }
                    MidiChannelStatus.NRPN -> {
                        control1 = (lastNRPN / 0x80).toFloat()
                        control2 = (lastNRPN % 0x80).toFloat()
                        control3 = (reg.nrpns[lastNRPN] shr 18).toFloat()
                    }
                    MidiChannelStatus.PER_NOTE_RCC -> {
                        control1 = lastPNRCCNote.toFloat()
                        control2 = lastPNRCCIndex.toFloat()
                        control3 = (reg.perNoteRCC[lastPNRCCNote][lastPNRCCIndex] shr 18).toFloat()
                    }
                    MidiChannelStatus.PER_NOTE_ACC -> {
                        control1 = lastPNACCNote.toFloat()
                        control2 = lastPNACCIndex.toFloat()
                        control3 = (reg.perNoteACC[lastPNACCNote][lastPNACCIndex] shr 18).toFloat()
                    }
                    MidiChannelStatus.PROGRAM -> {
                        control1 = midi1Machine.channels[0].program.toFloat()
                        control2 = (lastBank / 0x80).toFloat()
                        control3 = (lastBank % 0x80).toFloat()
                    }
                }
            } else {
                val reg = midi1Machine.channels[0]
                when (controlTarget.status) {
                    MidiChannelStatus.CC -> {
                        control1 = lastCCIndex.toFloat()
                        control2 = reg.controls[control1.toInt()].toFloat()
                    }
                    MidiChannelStatus.CAF -> {
                        control1 = reg.caf.toFloat()
                        control2 = 0f
                        control3 = 0f
                    }
                    MidiChannelStatus.PAF -> {
                        control1 = lastPAFNote.toFloat()
                        control2 = reg.pafVelocity[lastPAFNote].toFloat()
                        control3 = 0f
                    }
                    MidiChannelStatus.PITCH_BEND -> {
                        control1 = (reg.pitchbend - 0x2000).toFloat()
                        control2 = 0f
                        control3 = 0f
                    }
                    MidiChannelStatus.PER_NOTE_PITCH_BEND -> {} // N/A
                    MidiChannelStatus.RPN -> {
                        // The control3 value will not make sense without sending CC for RPN MSB/LSB,
                        // but they will be sent at send() consistently.
                        control1 = (lastRPN / 0x80).toFloat()
                        control2 = (lastRPN % 0x80).toFloat()
                        control3 = reg.rpns[lastRPN].toFloat()
                    }
                    MidiChannelStatus.NRPN -> {
                        // The control3 value will not make sense without sending CC for RPN MSB/LSB,
                        // but they will be sent at send() consistently.
                        control1 = (lastNRPN / 0x80).toFloat()
                        control2 = (lastNRPN % 0x80).toFloat()
                        control3 = reg.nrpns[lastNRPN].toFloat()
                    }
                    MidiChannelStatus.PER_NOTE_RCC -> {} // N/A
                    MidiChannelStatus.PER_NOTE_ACC -> {} // N/A
                    MidiChannelStatus.PROGRAM -> {
                        control1 = midi1Machine.channels[0].program.toFloat()
                        control2 = (lastBank / 0x80).toFloat()
                        control3 = (lastBank % 0x80).toFloat()
                    }
                }
            }
            targetChanged = false
        }

        // update internal states before sending the actual value change
        // (or just update the internal states if it is not discrete).
        val updateValueState by remember { mutableStateOf({ sendEvent: Boolean, controlTarget: ControlTargetDefinition, v1: Int, v2: Int, v3: Int ->
            // FIXME: maybe we could just keep midi2 impl.?
            if (isTransportUmp) {
                when (controlTarget.status) {
                    MidiChannelStatus.CC -> lastCCIndex = v1
                    MidiChannelStatus.PAF -> lastPAFNote = v1
                    MidiChannelStatus.PER_NOTE_PITCH_BEND -> lastPNPBNote = v1
                    MidiChannelStatus.PER_NOTE_RCC -> {
                        lastPNRCCNote = v1
                        lastPNRCCIndex = v2
                    }
                    MidiChannelStatus.PER_NOTE_ACC -> {
                        lastPNACCNote = v1
                        lastPNACCIndex = v2
                    }
                    MidiChannelStatus.RPN -> lastRPN = v1 * 0x80 + v2
                    MidiChannelStatus.NRPN -> lastNRPN = v1 * 0x80 + v2
                    MidiChannelStatus.PROGRAM -> { lastBank = v2 * 0x80 + v3 }
                }
            } else {
                when (controlTarget.status) {
                    MidiChannelStatus.CC -> lastCCIndex = v1
                    MidiChannelStatus.PAF -> lastPAFNote = v1
                    MidiChannelStatus.PER_NOTE_PITCH_BEND,
                    MidiChannelStatus.PER_NOTE_RCC,
                    MidiChannelStatus.PER_NOTE_ACC -> {} // not supported
                    MidiChannelStatus.RPN -> lastRPN = v1 * 0x80 + v2
                    MidiChannelStatus.NRPN -> lastNRPN = v1 * 0x80 + v2
                    MidiChannelStatus.PROGRAM -> lastBank = v2 * 0x80 + v3
                }
            }
            if (sendEvent && discrete)
                sendValueChange(controlTarget.status, v1, v2, v3)
        })}

        ImageStripKnob(imageBitmap = knobBitmap,
            modifier = Modifier.padding(knobPadding, 0.dp),
            value = control1,
            valueRange = range1.first.toFloat()..range1.last.toFloat(),
            tooltip = {
                DefaultKnobTooltip(showTooltip = true, value = control1, valueText = when(target) {
                    0 -> WellKnownNames.ccNames[127.coerceAtMost(knobValue.toInt())] ?: "(N/A)"
                    else -> controlTarget.control1.prefix + control1.toInt().toString()
                })
            },
            onValueChange = {
                control1 = it
                updateValueState(controlTarget.control1.sendEvent, controlTarget, control1.toInt(), control2.toInt(), control3.toInt())
            })

        if (range2 != IntRange.EMPTY) {
            ImageStripKnob(imageBitmap = knobBitmap,
                modifier = Modifier.padding(knobPadding, 0.dp),
                value = control2,
                valueRange = range2.first.toFloat()..range2.last.toFloat(),
                tooltip = { DefaultKnobTooltip(showTooltip = true, value = control2,
                    valueText = controlTarget.control2.prefix + control2.toInt().toString()) },
                onValueChange = {
                    control2 = it
                    updateValueState(controlTarget.control2.sendEvent, controlTarget, control1.toInt(), control2.toInt(), control3.toInt())
                })
        }

        if (controlTarget.control3.range != IntRange.EMPTY) {
            ImageStripKnob(imageBitmap = knobBitmap,
                modifier = Modifier.padding(knobPadding, 0.dp),
                value = control3,
                valueRange = range3.first.toFloat()..range3.last.toFloat(),
                tooltip = { DefaultKnobTooltip(showTooltip = true, value = control3,
                    valueText = controlTarget.control3.prefix + control3.toInt().toString()) },
                onValueChange = {
                    control3 = it
                    updateValueState(controlTarget.control3.sendEvent, controlTarget, control1.toInt(), control2.toInt(), control3.toInt())
                })
        }

        Column(Modifier.align(Alignment.CenterVertically).padding(knobPadding, 8.dp)) {
            Image(imageVector = if (discrete) MaterialSymbols.OutlinedFilled.Circle else MaterialSymbols.OutlinedFilled.Block,
                "send", Modifier.size(32.dp).background(Color.White).clickable {
                    discrete = !discrete
                    updateValueState(true, controlTarget, control1.toInt(), control2.toInt(), control3.toInt())
                }
            )
            Spacer(Modifier.padding(0.dp, 4.dp))
            Text(if (discrete) "discrete" else "", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

package org.androidaudioplugin.composeaudiocontrols.midi

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import dev.atsushieno.ktmidi.MidiCC
import dev.atsushieno.ktmidi.MidiChannelStatus
import dev.atsushieno.ktmidi.Ump
import dev.atsushieno.ktmidi.UmpFactory
import dev.atsushieno.ktmidi.toPlatformNativeBytes
import org.androidaudioplugin.composeaudiocontrols.DiatonicKeyboardNoteExpressionOrigin
import org.androidaudioplugin.composeaudiocontrols.DiatonicKeyboardWithControllers
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun MidiDeviceAccessScope.DiatonicLiveMidiKeyboard() {
    Column {
        val noteOnStates = remember { List(128) { 0L }.toMutableStateList() }
        var expressionX by remember { mutableStateOf(0f) }
        var expressionY by remember { mutableStateOf(0f) }
        var expressionP by remember { mutableStateOf(0f) }

        DiatonicKeyboardWithControllers(
            noteOnStates.toList(),
            showExpressionSensitivitySlider = false,
            onNoteOn = { note, _ ->
                if (useMidi2Protocol) {
                    val i64 = UmpFactory.midi2NoteOn(0, 0, note, 0, 0xF800, 0)
                    currentOutput?.send(Ump(i64).toPlatformNativeBytes(), 0, 8, 0)
                } else {
                    val bytes = byteArrayOf(MidiChannelStatus.NOTE_ON.toByte(), note.toByte(), 120)
                    currentOutput?.send(bytes, 0, bytes.size, 0)
                }
                noteOnStates[note] = 1
            },
            onNoteOff = { note, _ ->
                if (useMidi2Protocol) {
                    val i64 = UmpFactory.midi2NoteOff(0, 0, note, 0, 0xF800, 0)
                    currentOutput?.send(Ump(i64).toPlatformNativeBytes(), 0, 8, 0)
                } else {
                    val bytes = byteArrayOf(MidiChannelStatus.NOTE_OFF.toByte(), note.toByte(), 120)
                    currentOutput?.send(bytes, 0, bytes.size, 0)
                }
                noteOnStates[note] = 0
            },
            onExpression = { dir, note, data ->
                if (useMidi2Protocol) {
                    // MIDI 2.0 mode:
                    // Pitch Bend for horizontal moves
                    if (dir == DiatonicKeyboardNoteExpressionOrigin.HorizontalDragging) {
                        val v32 = min(0xFFFF_FFFFL, (((data + 1.0) / 2.0) * 0x1_0000_0000L).roundToLong())
                        val i64 = UmpFactory.midi2PitchBend(0, 0, v32)
                        currentOutput?.send(Ump(i64).toPlatformNativeBytes(), 0, 8, 0)
                    }
                    // Per-Note Pitch Bend for vertical moves
                    if (dir == DiatonicKeyboardNoteExpressionOrigin.VerticalDragging) {
                        val v32 = min(0xFFFF_FFFFL, (((data + 1.0) / 2.0) * 0x1_0000_0000L).roundToLong())
                        val i64 = UmpFactory.midi2PerNotePitchBend(0, 0, note, v32)
                        currentOutput?.send(Ump(i64).toPlatformNativeBytes(), 0, 8, 0)
                    }
                    // MIDI 2.0 PAf for pressure
                    if (dir == DiatonicKeyboardNoteExpressionOrigin.Pressure) {
                        val v32 = (((data / 2.0) + 0.5) * 0xFFFFFFFF).roundToLong()
                        val i64 = UmpFactory.midi2PAf(0, 0, note, v32)
                        currentOutput?.send(Ump(i64).toPlatformNativeBytes(), 0, 8, 0)
                    }
                } else {
                    // MIDI 1.0 mode:
                    // Pitch Bend for horizontal moves
                    if (dir == DiatonicKeyboardNoteExpressionOrigin.HorizontalDragging) {
                        val dataIn7Bit = min(127, ((data * 64f).roundToInt() + 64)).toByte()
                        val bytes =
                            byteArrayOf(MidiChannelStatus.PITCH_BEND.toByte(), dataIn7Bit, 0)
                        currentOutput?.send(bytes, 0, bytes.size, 0)
                    }
                    // MIDI 1.0 PAf for pressure
                    if (dir == DiatonicKeyboardNoteExpressionOrigin.Pressure) {
                        val dataIn7Bit = min(127, ((data * 64f).roundToInt() + 64)).toByte()
                        val bytes =
                            byteArrayOf(MidiChannelStatus.PAF.toByte(), note.toByte(), dataIn7Bit)
                        currentOutput?.send(bytes, 0, bytes.size, 0)
                    }
                }
                when (dir) {
                    DiatonicKeyboardNoteExpressionOrigin.HorizontalDragging -> expressionX = data
                    DiatonicKeyboardNoteExpressionOrigin.VerticalDragging -> expressionY = data
                    DiatonicKeyboardNoteExpressionOrigin.Pressure -> expressionP = data
                    else -> {}
                }
            }
        )
    }
}

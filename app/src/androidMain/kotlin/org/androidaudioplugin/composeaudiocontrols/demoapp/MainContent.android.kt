package org.androidaudioplugin.composeaudiocontrols.demoapp

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.atsushieno.ktmidi.EmptyMidiAccess
import org.androidaudioplugin.composeaudiocontrols.demoapp.theme.ComposeAudioControlsTheme
import org.androidaudioplugin.composeaudiocontrols.midi.DiatonicLiveMidiKeyboard
import org.androidaudioplugin.composeaudiocontrols.midi.KtMidiDeviceAccessScope
import org.androidaudioplugin.composeaudiocontrols.midi.MidiDeviceConfigurator


@Composable
fun KnobPreview() {
    ComposeAudioControlsTheme {
        ImageStripKnobDemo()
    }
}


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

package org.androidaudioplugin.composeaudiocontrols.midi

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.atsushieno.ktmidi.MidiAccess

@Composable
fun MidiKeyboardMain(access: MidiAccess) {
    val scope by remember { mutableStateOf(KtMidiDeviceAccessScope(access)) }
    scope.MidiKeyboardMain()
}

@Composable
fun MidiDeviceAccessScope.MidiKeyboardMain() {
    Column {
        MidiDeviceConfigurator()
        DiatonicLiveMidiKeyboard()
    }
}
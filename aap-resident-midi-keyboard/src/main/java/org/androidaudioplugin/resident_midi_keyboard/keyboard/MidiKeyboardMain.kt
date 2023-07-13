package org.androidaudioplugin.resident_midi_keyboard.keyboard

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun MidiKeyboardMain() {
    val context = LocalContext.current
    val scope by remember { mutableStateOf(KtMidiDeviceAccessScope(context)) }
    Column {
        scope.MidiDeviceConfigurator()
        scope.DiatonicKeyboardDemo()
    }
}
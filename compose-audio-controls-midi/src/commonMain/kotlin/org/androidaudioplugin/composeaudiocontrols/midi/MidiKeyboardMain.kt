package org.androidaudioplugin.composeaudiocontrols.midi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import dev.atsushieno.ktmidi.MidiAccess

@Deprecated("Use newer overload that takes ImageBitmap?",
    ReplaceWith("MidiKeyboardMain(access, null)")
)
@Composable
fun MidiKeyboardMain(access: MidiAccess) {
    MidiKeyboardMain(access, null)
}

@Composable
fun MidiKeyboardMain(access: MidiAccess, showControllerComboWithBitmap: ImageBitmap? = null) {
    val scope by remember { mutableStateOf(KtMidiDeviceAccessScope(access)) }
    scope.MidiKeyboardMain(showControllerComboWithBitmap)
}

@Deprecated("Use newer overload that takes ImageBitmap?", ReplaceWith("MidiKeyboardMain(null)"))
@Composable
fun MidiDeviceAccessScope.MidiKeyboardMain() {
    MidiKeyboardMain(null)
}

@Composable
fun MidiDeviceAccessScope.MidiKeyboardMain(showControllerComboWithBitmap: ImageBitmap? = null) {
    Column {
        MidiDeviceConfigurator()
        DiatonicLiveMidiKeyboard()
        if (showControllerComboWithBitmap != null) {
            Row {
                MidiKnobControllerCombo(showControllerComboWithBitmap)
            }
        }
    }
}
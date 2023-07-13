package org.androidaudioplugin.resident_midi_keyboard.keyboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.atsushieno.ktmidi.MidiPortDetails

@Composable
fun KtMidiDeviceSelector(modifier: Modifier = Modifier,
                         selectedMidiDeviceIndex: Int,
                         midiOutDeviceList: List<MidiPortDetails>,
                         onSelectionChange: (Int) -> Unit = { _ -> }) {
    Column {
        var listExpanded by remember { mutableStateOf(false) }
        val currentText =
            if (selectedMidiDeviceIndex < 0)
                "(Select MIDI OUT)"
            else
                midiOutDeviceList[selectedMidiDeviceIndex].name ?: "(unknown port)"
        Button(onClick = { listExpanded = true }) { Text(currentText) }
        DropdownMenu(
            modifier = modifier,
            expanded = listExpanded,
            onDismissRequest = { listExpanded = false }) {
            midiOutDeviceList.forEachIndexed { index, device ->
                DropdownMenuItem(text = { Text(device.name ?: "(unknown port)") }, onClick = {
                    onSelectionChange(index)
                    listExpanded = false
                })
            }
        }
    }
}

@Composable
fun KtMidiDeviceAccessScope.MidiDeviceConfigurator() {
    Row {
        // Since KtMidiDeviceAccessScope is composable, this deviceIndex state is hoisted here.
        var deviceIndex by remember { mutableStateOf(-1) }
        KtMidiDeviceSelector(selectedMidiDeviceIndex = deviceIndex,
            midiOutDeviceList = access.outputs.toList(),
            onSelectionChange = {
                deviceIndex = it
                onSelectionChange(it)
            })

        var useUMP by remember { mutableStateOf(false) }
        Checkbox(checked = useUMP, onCheckedChange = {
            useUMP = !useUMP
            onMidiProtocolChange(useUMP)
        })
        Text("Use MIDI 2.0", modifier = Modifier.align(Alignment.CenterVertically))
    }
}
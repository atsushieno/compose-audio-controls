package org.androidaudioplugin.composeaudiocontrols.midi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.atsushieno.ktmidi.MidiPortDetails
import dev.atsushieno.ktmidi.MidiTransportProtocol
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KtMidiDeviceSelector(modifier: Modifier = Modifier,
                         selectedMidiDeviceIndex: Int,
                         midiOutDeviceList: List<MidiPortDetails>,
                         onSelectionChange: (Int) -> Unit = { _ -> },
                         midi1DevicePrefix: String = "",
                         umpDevicePrefix: String = "[2] ") {
    Column {
        var listExpanded by remember { mutableStateOf(false) }
        val currentText =
            if (selectedMidiDeviceIndex < 0)
                "(Select MIDI OUT)"
            else
                midiOutDeviceList[selectedMidiDeviceIndex].name ?: "(unknown port)"
        Button(onClick = { listExpanded = true }) {
            Text(currentText, color = LocalContentColor.current) }
        DropdownMenu(
            modifier = modifier,
            expanded = listExpanded,
            onDismissRequest = { listExpanded = false }) {
            DropdownMenuItem(text = { Text("(Close Output)", color = LocalContentColor.current) }, onClick = {
                onSelectionChange(-1)
                listExpanded = false
            })
            midiOutDeviceList.forEachIndexed { index, device ->
                DropdownMenuItem(text = { Text(
                    (if (device.midiTransportProtocol == MidiTransportProtocol.UMP) umpDevicePrefix else midi1DevicePrefix)
                        + (device.name ?: "(unknown port)"), color = LocalContentColor.current) }, onClick = {
                    onSelectionChange(index)
                    listExpanded = false
                })
            }
            // FIXME: remove it once Google fixed this DropdownMenu bug: https://issuetracker.google.com/issues/346086163
            Text("An extra line to workaround Google bug 346086163", Modifier.height(50.dp))
        }
    }
}

@Composable
fun MidiDeviceAccessScope.MidiDeviceConfigurator() {
    Row {
        // Since KtMidiDeviceAccessScope is composable, this deviceIndex state is hoisted here.
        var deviceIndex by remember { mutableStateOf(-1) }
        KtMidiDeviceSelector(selectedMidiDeviceIndex = deviceIndex,
            midiOutDeviceList = outputs.toList(),
            onSelectionChange = {
                deviceIndex = it
                onSelectionChange(it)
            })

        val coroutineScope = rememberCoroutineScope()
        var umpSwitchBusy by remember { mutableStateOf(false) }
        var useUMP by remember { mutableStateOf(false) }
        Checkbox(enabled = !umpSwitchBusy, checked = useUMP, onCheckedChange = {
            umpSwitchBusy = true
            useUMP = !useUMP
            onMidiProtocolChange(useUMP)
            coroutineScope.launch {
                delay(500)
                umpSwitchBusy = false
            }
        })
        Text("UMP", color = LocalContentColor.current, modifier = Modifier.align(Alignment.CenterVertically))
    }
}
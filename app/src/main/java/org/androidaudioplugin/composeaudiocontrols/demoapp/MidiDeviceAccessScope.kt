package org.androidaudioplugin.composeaudiocontrols.demoapp

import android.content.Context
import dev.atsushieno.ktmidi.AndroidMidiAccess
import dev.atsushieno.ktmidi.MidiOutput
import dev.atsushieno.ktmidi.MidiPortDetails
import dev.atsushieno.ktmidi.UmpFactory
import dev.atsushieno.ktmidi.toPlatformNativeBytes
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface MidiDeviceAccessScope {
    val outputs: List<MidiPortDetails>
    val currentOutput: MidiOutput?
    val useMidi2Protocol: Boolean
    val onSelectionChange: (Int) -> Unit
    val onMidiProtocolChange: (Boolean) -> Unit
}

class KtMidiDeviceAccessScope(context: Context) : MidiDeviceAccessScope {
    val access = AndroidMidiAccess(context)
    private var output: MidiOutput? = null
    private var midi2 = false

    override val outputs: List<MidiPortDetails>
        get() = access.outputs.toList()
    override val currentOutput: MidiOutput?
        get() = output

    override val useMidi2Protocol: Boolean
        get() = midi2

    @OptIn(DelicateCoroutinesApi::class)
    override val onSelectionChange: (Int) -> Unit
        get() = { index ->
            GlobalScope.launch {
                output?.close()
                output = access.openOutput(outputs[index].id)
                println("Opened Midi Output: ${outputs[index].name}")
            }
        }

    private fun midi2EndpointConfiguration(protocol: Byte) =
        UmpFactory.streamConfigRequest(protocol, rxJRTimestamp = false, txJRTimestamp = true)
            .toPlatformNativeBytes()

    override val onMidiProtocolChange: (Boolean) -> Unit
        get() = {
            val bytes = midi2EndpointConfiguration(if (it) 2 else 1)
            output?.send(bytes, 0, bytes.size, 0)
            midi2 = it
        }
}
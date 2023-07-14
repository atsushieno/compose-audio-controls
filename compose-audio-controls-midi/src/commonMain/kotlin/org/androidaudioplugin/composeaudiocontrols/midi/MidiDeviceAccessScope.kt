package org.androidaudioplugin.composeaudiocontrols.midi

import dev.atsushieno.ktmidi.MidiAccess
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
    fun onSelectionChange(index: Int)
    fun onMidiProtocolChange(useMidi2: Boolean)
    fun cleanup()
}

class KtMidiDeviceAccessScope(val access: MidiAccess) : MidiDeviceAccessScope {
    private var output: MidiOutput? = null
    private var midi2 = false

    override val outputs: List<MidiPortDetails>
        get() = access.outputs.toList()
    override val currentOutput: MidiOutput?
        get() = output

    override val useMidi2Protocol: Boolean
        get() = midi2

    @OptIn(DelicateCoroutinesApi::class)
    override fun onSelectionChange(index: Int) {
        GlobalScope.launch {
            output?.close()
            output = access.openOutput(outputs[index].id)
            println("Opened Midi Output: ${outputs[index].name}")
        }
    }

    private fun midi2EndpointConfiguration(protocol: Byte) =
        UmpFactory.streamConfigRequest(protocol, rxJRTimestamp = false, txJRTimestamp = true)
            .toPlatformNativeBytes()

    override fun onMidiProtocolChange (useMidi2: Boolean) {
        val bytes = midi2EndpointConfiguration(if (useMidi2) 2 else 1)
        output?.send(bytes, 0, bytes.size, 0)
        midi2 = useMidi2
    }

    override fun cleanup() {
        output?.close()
    }
}

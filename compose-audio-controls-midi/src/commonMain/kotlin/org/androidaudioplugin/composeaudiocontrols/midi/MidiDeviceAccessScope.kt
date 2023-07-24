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
    val send: MidiEventSender
    val useMidi2Protocol: Boolean
    fun onSelectionChange(index: Int)
    fun onMidiProtocolChange(useMidi2: Boolean)
    fun cleanup()
}

class KtMidiDeviceAccessScope(val access: MidiAccess, val alwaysSendToDispatchers: Boolean = false) : MidiDeviceAccessScope {
    private var openedOutput: MidiOutput? = null
    private var midi2 = false

    override val outputs: List<MidiPortDetails>
        get() = access.outputs.toList()
    override val send: MidiEventSender
        get() = { mevent, offset, length, timestampInNanoseconds ->
            if (openedOutput != null) {
                try {
                    openedOutput?.send(mevent, offset, length, timestampInNanoseconds)
                } catch(ex: Exception) {
                    // FIXME: we should have some logging functionality (needs to be x-plat)
                    println(ex)
                    cleanup()
                }
            }
            if (openedOutput == null || alwaysSendToDispatchers)
                MidiKeyboardInputDispatcher.senders.forEach { it(mevent, offset, length, timestampInNanoseconds) }
        }

    override val useMidi2Protocol: Boolean
        get() = midi2

    @OptIn(DelicateCoroutinesApi::class)
    override fun onSelectionChange(index: Int) {
        GlobalScope.launch {
            cleanup()
            if (index < 0)
                return@launch
            openedOutput = access.openOutput(outputs[index].id)
        }
    }

    private fun midi2EndpointConfiguration(protocol: Byte) =
        UmpFactory.streamConfigRequest(protocol, rxJRTimestamp = false, txJRTimestamp = true)
            .toPlatformNativeBytes()

    override fun onMidiProtocolChange (useMidi2: Boolean) {
        val bytes = midi2EndpointConfiguration(if (useMidi2) 2 else 1)
        send(bytes, 0, bytes.size, 0)
        midi2 = useMidi2
    }

    override fun cleanup() {
        try {
            openedOutput?.close()
        } catch(ex: Exception) {
            // FIXME: we should have some logging functionality (needs to be x-plat)
            println(ex)
        }
        openedOutput = null
    }
}

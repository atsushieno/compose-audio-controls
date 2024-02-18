package org.androidaudioplugin.composeaudiocontrols.midi

import dev.atsushieno.ktmidi.Midi1Machine
import dev.atsushieno.ktmidi.Midi1Message
import dev.atsushieno.ktmidi.Midi2Machine
import dev.atsushieno.ktmidi.MidiAccess
import dev.atsushieno.ktmidi.MidiOutput
import dev.atsushieno.ktmidi.MidiPortDetails
import dev.atsushieno.ktmidi.MidiTransportProtocol
import dev.atsushieno.ktmidi.Ump
import dev.atsushieno.ktmidi.UmpFactory
import dev.atsushieno.ktmidi.toPlatformNativeBytes
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface MidiDeviceAccessScope {
    val outputs: List<MidiPortDetails>
    val send: MidiEventSender
    @Deprecated("use isTransportUmp instead",
        ReplaceWith("isTransportUmp")
    )
    val useMidi2Protocol: Boolean
        get() = isTransportUmp
    val isTransportUmp: Boolean
    val midi1Machine: Midi1Machine
    val midi2Machine: Midi2Machine
    fun onSelectionChange(index: Int)
    fun onMidiProtocolChange(useMidi2: Boolean)
    fun cleanup()
}

class KtMidiDeviceAccessScope(
    val access: MidiAccess,
    val alwaysSendToDispatchers: Boolean = true
) : MidiDeviceAccessScope {
    private var openedOutput: MidiOutput? = null
    private var useUmp = false
    override val midi1Machine by lazy {
        Midi1Machine().apply { channels[0].pitchbend = 0x2000 }
    }
    override val midi2Machine by lazy {
        Midi2Machine().apply {
            with(channel(0)) {
                pitchbend = 0x80000000u
                (0 until 127).forEach { perNotePitchbend[it] = 0x80000000u }
            }
        }
    }

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
            if (isTransportUmp) {
                Ump.fromBytes(mevent, offset, length).forEach {
                    midi2Machine.processEvent(it)
                }
            } else {
                Midi1Message.convert(mevent, offset, length).forEach {
                    midi1Machine.processMessage(it)
                }
            }
        }

    override val isTransportUmp: Boolean
        get() = useUmp

    @OptIn(DelicateCoroutinesApi::class)
    override fun onSelectionChange(index: Int) {
        GlobalScope.launch {
            cleanup()
            if (index < 0)
                return@launch
            try {
                openedOutput = access.openOutput(outputs[index].id)
                if (outputs[index].midiTransportProtocol == MidiTransportProtocol.UMP)
                    // When the transport is UMP, then always send UMP. No need to dare to fall back to MIDI1.
                    useUmp = true
                else {
                    if (useUmp)
                        // The user decided to send UMP over MIDI1 transport. AAPs can process them.
                        // We send UMP Stream Configuration to (kind of) let the receiver know that
                        // they are going to receive UMPs.
                        onMidiProtocolChange(true)
                    useUmp = false
                }
            } catch(ex: Exception) {
                // FIXME: we should have some logging functionality (needs to be x-plat)
                println(ex)
            }
        }
    }

    private fun midi2EndpointConfiguration(protocol: Byte) =
        UmpFactory.streamConfigRequest(protocol, rxJRTimestamp = false, txJRTimestamp = true)
            .toPlatformNativeBytes()

    override fun onMidiProtocolChange (useUmp: Boolean) {
        val bytes = midi2EndpointConfiguration(if (useUmp) 2 else 1)
        send(bytes, 0, bytes.size, 0)
        this.useUmp = useUmp
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

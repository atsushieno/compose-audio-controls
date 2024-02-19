package org.androidaudioplugin.composeaudiocontrols.midi

typealias MidiEventSender = (mevent: ByteArray, offset: Int, length: Int, timestampInNanoseconds: Long) -> Unit

object MidiKeyboardInputDispatcher {
    var useUmp = false
    val senders = mutableListOf<MidiEventSender>()
}

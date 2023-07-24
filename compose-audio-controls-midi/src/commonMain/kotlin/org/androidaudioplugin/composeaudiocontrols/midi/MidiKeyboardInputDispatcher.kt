package org.androidaudioplugin.composeaudiocontrols.midi

typealias MidiEventSender = (mevent: ByteArray, offset: Int, length: Int, timestampInNanoseconds: Long) -> Unit

object MidiKeyboardInputDispatcher {
    val senders = mutableListOf<MidiEventSender>()
}

package org.androidaudioplugin.composeaudiocontrols

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val isBlackKeyFlags = arrayOf(false, true, false, true, false, false, true, false, true, false, true, false)
// manually adjusted offset ratio to the white key width in Dp. 0f for non-existent key.
private val blackKeyOffsets = arrayOf(0.05f, 0.1f, 0f, 0.05f, 0.15f, 0.25f, 0f)
private val whiteKeyToNotes = arrayOf(0, 2, 4, 5, 7, 9, 11)

private fun getNoteFromPosition(noteRectMap: Map<Int,Rect>, pointerType: PointerType, offset: Offset): Int? {
    when (pointerType) {
        PointerType.Mouse, PointerType.Stylus -> { // Expects exact point containment
            // find black keys first to see if the rect contains the point as there are overlaps between black and white.
            return noteRectMap.firstNotNullOfOrNull { entry ->
                return@firstNotNullOfOrNull if (isBlackKeyFlags[entry.key % 12] && entry.value.contains(offset)) entry.key else null
            } ?: noteRectMap.firstNotNullOfOrNull { entry ->
                return@firstNotNullOfOrNull if (!isBlackKeyFlags[entry.key % 12] && entry.value.contains(offset)) return entry.key else null
            }
        }
        PointerType.Touch -> { // Expects nearest match by distance from center.
            return noteRectMap.map {
                it.key to it.value.center.minus(offset).getDistanceSquared()
            }.minBy { it.second }.first
        }
    }
    return null
}

/**
 * Implements a diatonic music keyboard.
 * It reacts to touch (or click) events and fires note on/off callbacks.
 *
 * ### Using DiatonicKeyboard
 *
 * You are supposed to manage your note states of each keys as `noteOnStates`, and pass
 * `onNoteOn` and `onNoteOff` event handlers. They take note number and "details" as the argument.
 * Note "details" is UNUSED in the current version, but will contain (1)velocity for MIDI 1.0, and
 * (2)velocity + note attributes for MIDI 2.0. They can be originated from touch pressure etc.
 *
 * Usage example:
 *
 * ```
 * val noteOnStates = remember { List(128) { 0 }.toMutableStateList() }
 * DiatonicKeyboard(noteOnStates.toList(),
 *     // you will also insert actual musical operations within these lambdas
 *     onNoteOn = { note, _ -> noteOnStates[note] = 1 },
 *     onNoteOff = { note, _ -> noteOnStates[note] = 0 }
 * )
 * ```
 *
 * ### Customizing keyboard
 *
 * The musical key range is calculated from `startOctaveFromZero` argument (`5` by default)
 * and up to `numWhiteKeys` (`14` = 2 octaves by default).
 * ("from zero" means, it does not start from 1 which are used by several DAWs).
 * The start note is fixed to its C key so far.
 *
 * The rendered keys are defined by `numWhiteKeys` above, and the rendered sizes are determined by
 * `whiteKeyWidth` (`30.dp` by default), `blackKeyHeight` (`35.dp` by default),
 * `totalWidth` (`whiteKeyWidth * numWhiteKeys` by default) and `totalHeight` (60.dp by default).
 * You can expand the actual Composable size by `modifier`, but it will be rendered based on these
 * arguments, and pointer inputs are calculated based on those arguments too.
 *
 * You can also customize `whiteNoteOnColor` (`Color.Cyan` by default),`blackNoteOnColor` (defaults
 * to `whiteNoteOnColor` argument), as well as `whiteKeyColor` and `blackKeyColor` (for note-off state).
 *
 * ### UI implementation details
 *
 * This keyboard control is designed for touch UI, and at the same time, for small screen sizes.
 * Our default white key size is `30.dp`. It is smaller than `48.dp`, but on touch events it does not strictly require touching insets.
 * Instead, the target note is calculated based on the nearest to the center of the keys.
 * On the other hand, if the input type is mouse or stylus, it expects exact insets.
 *
 */
@Composable
fun DiatonicKeyboard(noteOnStates: List<Int> = List(128) { 0 },
                     modifier: Modifier = Modifier,
                     onNoteOn: (note: Int, reserved: Long) -> Unit = { _,_ -> },
                     onNoteOff: (note: Int, reserved: Long) -> Unit = { _,_ -> },
                     startOctaveFromZero: Int = 5,
                     numWhiteKeys: Int = 14, // 2 octaves
                     whiteKeyWidth: Dp = 30.dp,
                     blackKeyHeight: Dp = 35.dp,
                     totalWidth: Dp? = whiteKeyWidth * numWhiteKeys,
                     totalHeight: Dp = 60.dp,
                     whiteNoteOnColor: Color = Color.Cyan,
                     blackNoteOnColor: Color = whiteNoteOnColor,
                     whiteKeyColor: Color = Color.White,
                     blackKeyColor: Color = Color.Black) {
    if (noteOnStates.size < 128)
        throw IllegalArgumentException("The `noteOnStates` list must contain at least 128 elements")

    val pointerIdToNote = remember { mutableStateMapOf<PointerId,Int>() }

    val wkWidth = whiteKeyWidth * 1f
    val bkWidth = wkWidth * 0.8f
    val bkHeight = blackKeyHeight * 1f

    val noteRectMap = remember { mutableStateMapOf<Int,Rect>() }

    Column(modifier.pointerInput(Unit) {
        interceptOutOfBoundsChildEvents = true
        awaitPointerEventScope {
            while(true) {
                when(awaitPointerEvent().type) {
                    PointerEventType.Press -> {
                        currentEvent.changes.forEach {

                            val note = getNoteFromPosition(noteRectMap.toMap(), it.type, it.position)
                            if (note != null) {
                                //println("Press: change ${it.id} ${it.type} ${it.position} -> $note")
                                pointerIdToNote[it.id] = note
                                // In the future versions we will support velocity too
                                onNoteOn(note, 127)
                            }
                        }
                    }
                    /* We will implement pitch bend once it's set in stone.
                    PointerEventType.Move -> {
                        currentEvent.changes.forEach {
                            //println("Move: change ${it.id} ${it.type} ${it.position}")
                        }
                    }*/
                    PointerEventType.Release -> {
                        currentEvent.changes.forEach {
                            //println("Release: change ${it.id} ${it.type} ${it.position}")
                            val note = pointerIdToNote.remove(it.id)
                            if (note != null)
                            // In the future versions we might support velocity too
                                onNoteOff(note, 0)
                        }
                    }
                }
            }
        }
    }) {
        Canvas(modifier = Modifier
            .then(if (totalWidth != null) Modifier.width(totalWidth) else Modifier.fillMaxSize(1f))
            .height(totalHeight)) {
            drawRect(color = Color.Black, size = size, style = Stroke(1f))

            // We render white keys first, then black keys to overlay appropriately.
            // If we render both in a single loop, then black keys might be incorrectly overdrawn by note-ons on the white keys.

            for (i in 0 until numWhiteKeys) {
                val x = i * wkWidth.toPx()
                val note = (startOctaveFromZero + i / 7) * 12 + whiteKeyToNotes[i % 7]
                if (note in 0..127) {
                    val rect = Rect(Offset(x = x, y = 0f), Size(wkWidth.toPx() - 1f, size.height))
                    noteRectMap[note] = rect
                    drawRoundRect(color = if (noteOnStates[note] != 0) whiteNoteOnColor else whiteKeyColor,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
                drawLine(start = Offset(x = x, y = 0f), end = Offset(x = x, y = (bkWidth * 0.5f).toPx()), color = blackKeyColor)
            }

            for (i in 0 until numWhiteKeys) {
                val x = i * wkWidth.toPx()
                val bkOffset = blackKeyOffsets[i % 7] * wkWidth.toPx()
                if (bkOffset != 0f) {
                    val note = (startOctaveFromZero + i / 7) * 12 + whiteKeyToNotes[i % 7] + 1
                    val isNoteOn = note in 0..127 && noteOnStates[note] != 0
                    val rect = Rect(Offset(x = x + bkOffset + wkWidth.toPx() / 2, y = 0f), Size(bkWidth.toPx(), bkHeight.toPx()))
                    noteRectMap[note] = rect
                    drawRoundRect(
                        color = if (isNoteOn) blackNoteOnColor else blackKeyColor,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
            }
            // rightmost edge
            drawLine(start = Offset(x = size.width, y = 0f), end = Offset(x = size.width, y = (bkWidth * 0.5f).toPx()), color = Color.Black)
        }
    }
}

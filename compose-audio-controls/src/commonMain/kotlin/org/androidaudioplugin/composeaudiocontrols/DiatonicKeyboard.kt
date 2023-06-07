package org.androidaudioplugin.composeaudiocontrols

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlin.math.max
import kotlin.math.min

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
 * Used by `DiatonicKeyboard` to indicate which type of operation a dragging control should result in.
 */
enum class DiatonicKeyboardMoveAction {
    /**
     * Indicates that it should simply switch to the next note i.e. "note off" on the old note, and "note on" at the new pointer location.
     */
    NoteChange,
    /**
     * Indicates that the dragging sends per-note expression events.
     * The actual semantics are up to the user - it can be per-note pitch bend, expression (CC 11),
     * cutoff frequency (CC 74), etc. We indirectly support MPE in this form too.
     */
    NoteExpression,
}

/**
 * Used by `onExpression` lambda parameter in `DiatonicKeyboard`, as `origin` parameter.
 */
enum class DiatonicKeyboardNoteExpressionOrigin {
    /**
     * Indicates that the data origin is the delta value in horizontal dragging.
     * In `DiatonicKeyboard`, the value range is `-1f..1f`.
     */
    HorizontalDragging,

    /**
     * Indicates that the data origin is the delta value in vertical dragging.
     * In `DiatonicKeyboard`, the value range is `-1f..1f`.
     */
    VerticalDragging,

    /**
     * Indicates that the data origin is the delta value in pressure of the pointer.
     * The value is up to what the target device sends.
     */
    Pressure
}

/**
 * Implements a diatonic music keyboard.
 * It reacts to touch (or click) events and fires note on/off callbacks, as well as note expression
 * callbacks per pointer drag events and pressure events if it is configured as such.
 *
 * ### Using DiatonicKeyboard
 *
 * You are supposed to manage your note states of each keys as `noteOnStates`, and pass
 * `onNoteOn` and `onNoteOff` event handlers. They take note number and "details" as the argument.
 * Note "details" is UNUSED in the current version, but will contain (1)velocity for MIDI 1.0, and
 * (2)velocity + note attributes for MIDI 2.0. They can be originated from touch pressure etc.
 *
 * It can also send "note expressions" alike, if `DiatonicKeyboardMoveAction.NoteExpression` is specified at `moveAction` parameter.
 * Then `onExpression` event will be raised for dragging operation and touch pressure changes.
 * It is done per pointer, so it works like an MPE (or MIDI 2.0) keyboard.
 * Dragging events are sent for both X and Y axes.
 *
 * ### Customizing keyboard
 *
 * The musical key range is calculated from `octaveZeroBased` argument (`4` by default)
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
 * Regarding drag events for note expressions, the valid motion range is -80dp..80dp from the initial point, by default.
 * They are then mapped to -1f..1f value range when `onExpression` callback is invoked.
 *
 * Regarding touch events for note expressions, the value semantics depends on whatever device sends.
 *
 * ### Usage example
 *
 * Here is an example (complicated) use of DiatonicKeyboard():
 *
 * ```
 * val noteOnStates = remember { List(128) { 0 }.toMutableStateList() }
 * DiatonicKeyboard(noteOnStates.toList(),
 *     // you will also insert actual musical operations within these lambdas
 *     onNoteOn = { note, _ -> noteOnStates[note] = 1 },
 *     onNoteOff = { note, _ -> noteOnStates[note] = 0 },
 *     // use below only if you need MIDI 2.0 / MPE compat keyboard
 *     moveAction = DiatonicKeyboardMoveAction.NoteExpression,
 *     onExpression = { origin, note, data ->
 *         when (origin) {
 *             DiatonicKeyboardNoteExpressionOrigin.Horizontal) ->
 *                 perNotePitchBend(note, data / 2f + 0.5f) }
 *             DiatonicKeyboardNoteExpressionOrigin.Vertical) ->
 *                 polyphonicPressure(note, data / 2f + 0.5f) }
 *             else -> {}
 *         }
 *     }
 * )
 * ```
 *
 * @param noteOnStates      a List of Long that holds note states. It must contain 128 elements.
 *                          Currently it only expects that note on state holds non-zero value.
 * @param modifier          a Modifier that applies to its top level Column.
 * @param onNoteOn          an event handler that is called when a key for a note is pressed
 * @param onNoteOff         an event handler that is called when a key for a note is released
 * @param onExpression      an event handler that is called when note expression events occur,
 *                          by dragging (if indicated so by `moveAction`).
 *                          The value range for `data` sent to the handler depends on the target `origin`.
 *                          For `HorizontalDragging` and `VerticalDragging` they are `-1.0f..1.0f`.
 *                          For `Pressure` it is up to device.
 * @param moveAction        indicates that how dragging works. See the documentation on `DiatonicKeyboardMoveAction` enumeration type.
 *                          `NoteChange` by default.
 * @param octaveZeroBased   the octave (in zero-based counting i.e. 0 to 9 or 10 (up to `numWhiteKeys`). `4` by default.
 * @param numWhiteKeys      the number of white keys to be rendered. `14` by default (which means 2 octaves)
 * @param expressionDragSensitivity    a sensitivity parameter over note expression dragging.
 *                                     The value is treated as a `Dp` value that corresponds to the width
 *                                     for "half" of the motion size towards max or min value.
 *                                     `80` (Dp) by default.
 * @param whiteKeyWidth     The display size for one white key width. `30.dp` by default.
 * @param blackKeyHeight    The display size for black key height. `35.dp` by default.
 * @param totalWidth        The display size for the whole keyboard control.
 *                          It is automatically calculated as `whiteKeyWidth * numWhiteKeys` but you can change it.
 *                          Alternatively, you can explicitly specify `null` then it will take use `Modifier.fillMaxSize()`
 *                          (but note that the number of the rendered key is governed by `numWhiteKeys` anyways).
 * @param totalHeight       The display size for the whole keyboard control. It also means white key height. `60.dp` by default.
 * @param whiteNoteOnColor  A `Color` value for the note on indication on the white keys. `Color.Cyan` by default.
 * @param blackNoteOnColor  A `Color` value for the note on indication on the black keys. `whiteNoteOnColor` by default.
 * @param whiteKeyColor     A `Color` value for the white keys (when not at note-on state). `Color.White` by default.
 * @param blackKeyColor     A `Color` value for the black keys (when not at note-on state). `Color.Black` by default.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DiatonicKeyboard(noteOnStates: List<Long> = List(128) { 0L },
                     modifier: Modifier = Modifier,
                     onNoteOn: (note: Int, reserved: Long) -> Unit = { _,_ -> },
                     onNoteOff: (note: Int, reserved: Long) -> Unit = { _,_ -> },
                     onExpression: (origin: DiatonicKeyboardNoteExpressionOrigin, note: Int, data: Float) -> Unit = {_,_,_ -> },
                     moveAction: DiatonicKeyboardMoveAction = DiatonicKeyboardMoveAction.NoteChange,
                     octaveZeroBased: Int = 4,
                     numWhiteKeys: Int = 14, // 2 octaves
                     expressionDragSensitivity: Int = 80, // in dp; 0.5 inch for 0..0.5 value changes (and will run towards both negative and positive)
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
    val pointerIdToInitialOffset = remember { mutableStateMapOf<PointerId, Offset>() }
    val pointerIdToPressure = remember { mutableStateMapOf<PointerId, Float>() }

    val wkWidth = whiteKeyWidth * 1f
    val bkWidth = wkWidth * 0.8f
    val bkHeight = blackKeyHeight * 1f

    val noteRectMap = remember { mutableStateMapOf<Int,Rect>() }

    Column(modifier.pointerInput(octaveZeroBased, pointerIdToNote, noteRectMap, expressionDragSensitivity, moveAction) {
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
                                if (moveAction == DiatonicKeyboardMoveAction.NoteExpression) {
                                    pointerIdToInitialOffset[it.id] = it.position
                                    pointerIdToPressure[it.id] = it.pressure
                                }
                                // In the future versions we will support velocity too
                                onNoteOn(note, 127)
                            }
                        }
                    }
                    PointerEventType.Move -> {
                        currentEvent.changes.forEach {
                            //println("Move: change ${it.id} ${it.type} ${it.position}")
                            if (moveAction == DiatonicKeyboardMoveAction.NoteChange) {
                                val note = getNoteFromPosition(noteRectMap.toMap(), it.type, it.position)
                                if (note != null && note != pointerIdToNote[it.id]) {
                                    pointerIdToInitialOffset.remove(it.id)
                                    onNoteOff(pointerIdToNote.remove(it.id)!!, 0)
                                    pointerIdToNote[it.id] = note
                                    onNoteOn(note, 127)
                                }
                            } else {
                                val note = pointerIdToNote[it.id] ?: return@forEach
                                val deltaMaxDp = expressionDragSensitivity.toFloat() // calculate only once
                                val deltaX = (it.position.x - pointerIdToInitialOffset[it.id]!!.x).toDp().value
                                val dataX = min(deltaMaxDp, max(-deltaMaxDp, deltaX)) / deltaMaxDp
                                onExpression(DiatonicKeyboardNoteExpressionOrigin.HorizontalDragging, note, dataX)
                                val deltaY = (it.position.y - pointerIdToInitialOffset[it.id]!!.y).toDp().value
                                val dataY = min(deltaMaxDp, max(-deltaMaxDp, deltaY)) / deltaMaxDp
                                onExpression(DiatonicKeyboardNoteExpressionOrigin.VerticalDragging, note, dataY)
                                val originalPressure = pointerIdToPressure[it.id]
                                if (originalPressure != null && it.pressure != originalPressure)
                                    onExpression(DiatonicKeyboardNoteExpressionOrigin.Pressure, note, it.pressure - originalPressure)
                            }
                        }
                    }
                    PointerEventType.Release -> {
                        currentEvent.changes.forEach {
                            //println("Release: change ${it.id} ${it.type} ${it.position}")
                            if (moveAction == DiatonicKeyboardMoveAction.NoteExpression) {
                                pointerIdToInitialOffset.remove(it.id)
                                pointerIdToPressure.remove(it.id)
                            }
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

            noteRectMap.clear()
            for (i in 0 until numWhiteKeys) {
                val x = i * wkWidth.toPx()
                val note = (octaveZeroBased + i / 7) * 12 + whiteKeyToNotes[i % 7]
                if (note in 0..127) {
                    val rect = Rect(Offset(x = x, y = 0f), Size(wkWidth.toPx() - 1f, size.height))
                    noteRectMap[note] = rect
                    drawRoundRect(color = if (noteOnStates[note] != 0L) whiteNoteOnColor else whiteKeyColor,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
                drawLine(start = Offset(x = x, y = 0f), end = Offset(x = x, y = (bkWidth * 0.5f).toPx()), color = blackKeyColor)
            }

            // black keys follow.

            for (i in 0 until numWhiteKeys) {
                val x = i * wkWidth.toPx()
                val bkOffset = blackKeyOffsets[i % 7] * wkWidth.toPx()
                if (bkOffset != 0f) {
                    val note = (octaveZeroBased + i / 7) * 12 + whiteKeyToNotes[i % 7] + 1
                    if (note >= 128)
                        break
                    val isNoteOn = note in 0..127 && noteOnStates[note] != 0L
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

/**
 * It is a compound set of `DiatonicKeyboard` with the following controllers:
 *
 * - it can toggle `NoteExpression` mode
 * - it can change the note expression sensitivity using slider. It ranges from`32` to `320` (2 inches).
 * - it can change the base octave.
 *
 * The list of parameters are almost identical to `DiatonicKeyboard`, except:
 *
 * - some parameters are named "initial",
 * - there are showXXX parameters that indicate whether it should render each controllers.
 *
 * For more details, see `DiatonicKeyboard` documentation.
 *
 * @param noteOnStates      a List of Long that holds note states. It must contain 128 elements.
 *                          Currently it only expects that note on state holds non-zero value.
 * @param modifier          a Modifier that applies to its top level Column.
 * @param onNoteOn          an event handler that is called when a key for a note is pressed
 * @param onNoteOff         an event handler that is called when a key for a note is released
 * @param onExpression      an event handler that is called when note expression events occur,
 *                          by dragging (if indicated so by `moveAction`).
 *                          The value range for `data` sent to the handler depends on the target `origin`.
 *                          For `HorizontalDragging` and `VerticalDragging` they are `-1.0f..1.0f`.
 *                          For `Pressure` it is up to device.
 * @param showNoteExpressionToggle   indicates whether the mode (note change vs. note expression) toggle switch is rendered. `true` by default.
 * @param showExpressionSensitivitySlider    indicates whether the expression sensitivity slider is rendered. `true` by default.
 * @param showOctaveSlider  indicates whether the octave slider is rendered. `true` by default.
 * @param initialMoveAction indicates the initial value of `moveAction` that how dragging works. See the documentation on `DiatonicKeyboardMoveAction` enumeration type.
 *                          `NoteChange` by default.
 * @param initialOctaveZeroBased   the initial value of octave (in zero-based counting i.e. 0 to 9 or 10 (up to `numWhiteKeys`). `4` by default.
 * @param numWhiteKeys      the number of white keys to be rendered. `14` by default (which means 2 octaves)
 * @param initialExpressionDragSensitivity    the initial value of the sensitivity parameter over note expression dragging.
 *                                     The value is treated as a `Dp` value that corresponds to the width
 *                                     for "half" of the motion size towards max or min value.
 *                                     `80` (Dp) by default.
 * @param whiteKeyWidth     The display size for one white key width. `30.dp` by default.
 * @param blackKeyHeight    The display size for black key height. `35.dp` by default.
 * @param totalWidth        The display size for the whole keyboard control.
 *                          It is automatically calculated as `whiteKeyWidth * numWhiteKeys` but you can change it.
 *                          Alternatively, you can explicitly specify `null` then it will take use `Modifier.fillMaxSize()`
 *                          (but note that the number of the rendered key is governed by `numWhiteKeys` anyways).
 * @param totalHeight       The display size for the whole keyboard control. It also means white key height. `60.dp` by default.
 * @param whiteNoteOnColor  A `Color` value for the note on indication on the white keys. `Color.Cyan` by default.
 * @param blackNoteOnColor  A `Color` value for the note on indication on the black keys. `whiteNoteOnColor` by default.
 * @param whiteKeyColor     A `Color` value for the white keys (when not at note-on state). `Color.White` by default.
 * @param blackKeyColor     A `Color` value for the black keys (when not at note-on state). `Color.Black` by default.
 */
@Composable
fun DiatonicKeyboardWithControllers(noteOnStates: List<Long> = List(128) { 0L },
                                    modifier: Modifier = Modifier,
                                    onNoteOn: (note: Int, reserved: Long) -> Unit = { _,_ -> },
                                    onNoteOff: (note: Int, reserved: Long) -> Unit = { _,_ -> },
                                    onExpression: (origin: DiatonicKeyboardNoteExpressionOrigin, note: Int, data: Float) -> Unit = {_,_,_ -> },
                                    showNoteExpressionToggle: Boolean = true,
                                    showExpressionSensitivitySlider: Boolean = true,
                                    showOctaveSlider: Boolean = true,
                                    initialMoveAction: DiatonicKeyboardMoveAction = DiatonicKeyboardMoveAction.NoteChange,
                                    initialOctaveZeroBased: Int = 4,
                                    numWhiteKeys: Int = 14, // 2 octaves
                                    initialExpressionDragSensitivity: Int = 80, // in dp; 0.5 inch for 0..0.5 value changes (and will run towards both negative and positive)
                                    whiteKeyWidth: Dp = 30.dp,
                                    blackKeyHeight: Dp = 35.dp,
                                    totalWidth: Dp? = whiteKeyWidth * numWhiteKeys,
                                    totalHeight: Dp = 60.dp,
                                    whiteNoteOnColor: Color = Color.Cyan,
                                    blackNoteOnColor: Color = whiteNoteOnColor,
                                    whiteKeyColor: Color = Color.White,
                                    blackKeyColor: Color = Color.Black) {

    var noteExpressionMode by remember { mutableStateOf(initialMoveAction == DiatonicKeyboardMoveAction.NoteExpression) }
    var expressionDragSensitivity by remember { mutableStateOf(initialExpressionDragSensitivity.toFloat()) }
    var octave by remember { mutableStateOf(initialOctaveZeroBased.toFloat()) }
    val exprAlpha = if (noteExpressionMode) 1.0f else 0.3f
    if (showNoteExpressionToggle || showExpressionSensitivitySlider || showOctaveSlider) {
        Row {
            if (showNoteExpressionToggle && !showExpressionSensitivitySlider)
                Text("Expression Mode", modifier = Modifier.width(140.dp))
            else if (showExpressionSensitivitySlider)
                Text("Expr. Sense", modifier = Modifier.width(110.dp))
            if (showExpressionSensitivitySlider)
                Text("${expressionDragSensitivity.toInt()}", modifier = Modifier.width(30.dp).alpha(exprAlpha))
            if (showNoteExpressionToggle)
                Checkbox(checked = noteExpressionMode, onCheckedChange = { noteExpressionMode = !noteExpressionMode })
            if (showExpressionSensitivitySlider)
                Slider(enabled = noteExpressionMode, modifier = Modifier.width(80.dp),
                    value = expressionDragSensitivity, valueRange = 32f..320f, onValueChange = { expressionDragSensitivity = it })
            if (showOctaveSlider) {
                Text("Oct. ${octave.toInt()}")
                Slider(modifier = Modifier.width(80.dp),
                    value = octave, valueRange = 0f..9f, onValueChange = { octave = it })
            }
        }
    }

    DiatonicKeyboard(noteOnStates, modifier, onNoteOn, onNoteOff, onExpression,
        if (noteExpressionMode) DiatonicKeyboardMoveAction.NoteExpression else DiatonicKeyboardMoveAction.NoteChange,
        octave.toInt(),
        numWhiteKeys,
        expressionDragSensitivity.toInt(),
        whiteKeyWidth, blackKeyHeight, totalWidth, totalHeight, whiteNoteOnColor, blackNoteOnColor, whiteKeyColor, blackKeyColor
    )
}
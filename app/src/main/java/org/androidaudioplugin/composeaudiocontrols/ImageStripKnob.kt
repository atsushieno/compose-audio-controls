package org.androidaudioplugin.composeaudiocontrols

import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlin.math.min
import kotlin.math.roundToInt

// "Consider making touch targets at least 48x48dp, separated by 8dp of space or more, to ensure balanced information density and usability. "
//  https://support.google.com/accessibility/android/answer/7101858?hl=en
private val defaultKnobMinSizeInDp = 48.dp

internal fun formatLabelNumber(v: Float, charsInPositiveNumber: Int = 5) = v.toBigDecimal().toPlainString().take(charsInPositiveNumber + if (v < 0) 1 else 0)

/**
    Implements a knob control that is based on KnobMan image strip. See another ImageStripKnob() for more details.

    This function overload takes a drawable resource ID (`drawableResId`) to ease image loading.
    
    @param modifier      A `Modifier` to be applied to this knob control.
    @param drawableResId The Android Resource Id for this knob.
    @param value         The value that this knob should render for. It should be within the range between `minValue` and `maxValue`. Any value that is out of range is clipped.
    @param minValue      The minimum value, which defines the value range, along with `maxValue`. It defaults to `0f`
    @param maxValue      The maximum value, which defines the value range, along with `minValue`. It defaults to `1f`
    @param minSizeInDp   The minimum rendered widget size in `Dp`. It defaults to `48.dp` which is the minimum recommended widget size by Android Accessibility Help.
    @param tooltipColor  The color of the default implementation of the value label tooltip.
    @param tooltip       The tooltip Composable which may be rendered in response to user's drag action over this knob.
    @param valueChanged  An event handler function that takes the changed value.
 */
@Composable
fun ImageStripKnob(modifier: Modifier = Modifier,
         drawableResId: Int,
         value: Float = 0f,
         minValue: Float = 0f,
         maxValue: Float = 1f, // typical float value range: 0.0 - 1.0
         minSizeInDp: Dp = defaultKnobMinSizeInDp,
         tooltipColor: Color = Color.Gray,
         tooltip: @Composable (value: Float, isBeingDragged: Boolean) -> Unit = { v, isBeingDragged ->
             // by default, show tooltip only when it is being dragged
             DefaultKnobTooltip(
                 value = v,
                 showTooltip = isBeingDragged,
                 textColor = tooltipColor
             )
         },
         valueChanged: (value: Float) -> Unit = {}
) {
    val context = LocalContext.current
    val res = context.resources
    val resValue = remember { TypedValue() }
    res.getValue(drawableResId, resValue, true)
    val path = resValue.string
    val imageBitmap = remember(path, drawableResId, context.theme) {
        ImageBitmap.imageResource(res, drawableResId)
    }
    ImageStripKnob(
        modifier,
        imageBitmap,
        value,
        minValue,
        maxValue,
        minSizeInDp,
        tooltipColor,
        tooltip,
        valueChanged
    )
}

/**
    Implements a knob control that is based on KnobMan image strip.
    If you are not familiar with KnobMan, see this KVR page: https://www.kvraudio.com/product/knobman-by-g200kg
    
    Start dragging vertically from the knob to change the value within the range between `minValue` and `maxValue` arguments.
    You only need one finger. No pinch required.
    
    Value labels are rendered when it is dragged below the knob, by default.
    
    The knob is designed to minimize the space but also usable with human fingertips, by default (48.dp `minSizeInDp`).
    
    We currently do not assign anything for horizontal dragging by intention.
    It is preserved for users who want to move out their finger of the knob and tooltip label.
    Thus it is recommended to NOT assign another single-fingered drag operation over the knob.
    
    The value change size is simply from the delta value that Jetpack Compose foundation sends, so far.
    We may change this behavior in the future.
    
    This function overload takes a `ImageBitmap`.
    For Android drawable resources, you can use another ImageStripKnob() overload that takes the resource ID.
    
    ### value label tooltip
    
    By default, it shows a value label tooltip when the knob is dragged.
    
    It is possible to customize the label behavior by passing `tooltip` argument.
    It is a `Composable` rendered after the `Image` in a `Column`.
    You can use `DefaultKnobTooltip` to slightly modify the default behavior, namely:
    
    - always show label (even not on dragging): `DefaultKnobTooltip(value, true)`
    - customize text color: `DefaultKnobTooltip(value, isBeingDragged, yourPreferredColor)`
    - move up, down, etc. : pass `modifier` to `DefaultKnobTooltip` that is applied to `Text` (visible) or `Box` (invisible space)
    
    ### Usage example
    
    Here is an example use of ImageStripKnob():
    
    ```
        var paramValue by remember { mutableStateOf(0f) }
        Text("Parameter $paramIndex: ")
        ImageStripKnob(drawableResId = R.drawable.knob_image,
            value = paramValue,
            valueChanged = {v ->
                paramValue = v
                println("value at $paramIndex changed: $v")
            })
    ```
    
    @param modifier      A `Modifier` to be applied to this knob control.
    @param imageBitmap   An `ImageBitMap` that contains the knob image strip.
    @param value         The value that this knob should render for. It should be within the range between `minValue` and `maxValue`.
    @param minValue      The minimum value, which defines the value range, along with `maxValue`. It defaults to `0f`
    @param maxValue      The maximum value, which defines the value range, along with `minValue`. It defaults to `1f`
    @param minSizeInDp   The minimum rendered widget size in `Dp`. It defaults to `48.dp` which is the minimum recommended widget size by Android Accessibility Help.
    @param tooltipColor  The color of the default implementation of the value label tooltip.
    @param tooltip       The tooltip Composable which may be rendered in response to user's drag action over this knob.
    @param valueChanged  An event handler function that takes the changed value. See the documentation for `ImageStripKnob` function for details.
 */
@Composable
fun ImageStripKnob(modifier: Modifier = Modifier,
         imageBitmap: ImageBitmap,
         value: Float = 0f,
         minValue: Float = 0f,
         maxValue: Float = 1f,
         minSizeInDp: Dp = defaultKnobMinSizeInDp,
         tooltipColor: Color = Color.Gray,
         tooltip: @Composable (value: Float, isBeingDragged: Boolean) -> Unit = { value, isBeingDragged ->
             // by default, show tooltip only when it is being dragged
             DefaultKnobTooltip(
                 value = value,
                 showTooltip = isBeingDragged,
                 textColor = tooltipColor
             )
         },
         valueChanged: (value: Float) -> Unit = {}
         ) {
    // assuming these properties are cosmetic to acquire and thus can be computed every time...
    val knobSrcSizePx = imageBitmap.width
    val numKnobSlices = imageBitmap.height / imageBitmap.width
    val valueDelta = (maxValue - minValue) / numKnobSlices

    val normalizedValue = if (value > maxValue) maxValue else if (value < minValue) minValue else value
    val imageIndex = min(numKnobSlices - 1, (normalizedValue / valueDelta).toInt())

    with(LocalDensity.current) {
        var isBeingDragged by remember { mutableStateOf(false) }
        val sizePx = if (minSizeInDp.toPx() > knobSrcSizePx) minSizeInDp.toPx() else knobSrcSizePx.toFloat()

        val draggableState = rememberDraggableState(onDelta = {
            val v = value - it * 0.01f
            val next = if (v < minValue) minValue else if (maxValue < v) maxValue else v
            isBeingDragged = true
            if (value != next)
                valueChanged(next)
        })

        Column {
            Image(
                ScalingPainter(
                    imageBitmap,
                    srcSize = IntSize(knobSrcSizePx, knobSrcSizePx),
                    srcOffset = IntOffset(0, knobSrcSizePx * imageIndex),
                    scale = sizePx / knobSrcSizePx
                ),
                contentDescription = "knob image",
                contentScale = ContentScale.Inside,
                alignment = Alignment.TopStart,
                modifier = modifier
                    // our settings take higher priority
                    .draggable(draggableState, Orientation.Vertical,
                        onDragStopped = { isBeingDragged = false })
                    .size(sizePx.toDp())
            )
            tooltip(value, isBeingDragged)
        }
    }
}

/**
    The default tooltip `Composable` implementation for `ImageStripKnob`.
    It is configurable by the arguments.

    @param modifier      a `Modifier` that is applied to the `Text` (when the label is visible) or `Box` (when it is not).
    @param showTooltip   The flag to indicate whether the label is shown or not. By default, it is true only if the user is dragging the knob.
    @param value         The float value to render as the label.
    @param textColor     a `Color` value to specify at `Text` label.
 */
@Composable
fun DefaultKnobTooltip(modifier: Modifier = Modifier, showTooltip: Boolean, value: Float, textColor: Color = Color.Gray) {
    if (showTooltip)
        Text(
            formatLabelNumber(value),
            fontSize = 12.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(8.dp, 0.dp).then(modifier)
        )
    else
        with(LocalDensity.current) {
            Box(Modifier.height(16.sp.toDp()))
        }
}

/**
 * A custom `Painter` implementation for scaling another source image.
 * It is used by ImageStripKnob to ensure minimum on-screen sizes.
 */
class ScalingPainter(private val image: ImageBitmap,
                     private val srcOffset: IntOffset = IntOffset.Zero,
                     private val srcSize: IntSize = IntSize(image.width, image.height),
                     scale: Float = 1f
) : Painter() {
    private val validatedSize = validateSize(srcOffset, srcSize)
    private val width = validatedSize.width * scale
    private val height = validatedSize.height * scale

    override val intrinsicSize: Size
        get() = validatedSize.toSize()

    override fun DrawScope.onDraw() {
        drawImage(
            image,
            srcOffset,
            srcSize,
            dstSize = IntSize(width.roundToInt(), height.roundToInt())
        )
    }

    // idea and code taken from CustomPainterSnippets.kt in android/snippets repo
    private fun validateSize(srcOffset: IntOffset, srcSize: IntSize): IntSize {
        require(
            srcOffset.x >= 0 &&
                    srcOffset.y >= 0 &&
                    srcSize.width >= 0 &&
                    srcSize.height >= 0 &&
                    srcSize.width <= image.width &&
                    srcSize.height <= image.height
        )
        return srcSize
    }
}
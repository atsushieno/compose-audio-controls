package org.androidaudioplugin.composeaudiocontrols

import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import java.time.temporal.ValueRange


/**
 * Implements a knob control that is based on KnobMan image strip. See another ImageStripKnob() for more details.
 *
 * This Android-only function overload takes a drawable resource ID (`drawableResId`) to ease image loading.
 * (Since we use our own `ScalingPainter`, we cannot simply use `painterResource()` to this function.)
 *
 * @param modifier          A `Modifier` to be applied to this knob control.
 * @param drawableResId     The Android Resource Id for this knob.
 * @param value             The value that this knob should render for. It should be within the range between `minValue` and `maxValue`.
 * @param valueRange        The value range. It defaults to `0f..1f`.
 * @param explicitSizeInDp  An optional size in Dp if you want an explicit rendered widget size instead of the sizes in image, in `Dp`.
 * @param minSizeInDp       The minimum rendered widget size in `Dp`. It defaults to `48.dp` which is the minimum recommended widget size by Android Accessibility Help.
 * @param tooltipColor      The color of the default implementation of the value label tooltip.
 * @param tooltip           The tooltip Composable which may be rendered in response to user's drag action over this knob.
 * @param onValueChange     An event handler function that takes the changed value. See the documentation for `ImageStripKnob` function for details.
 */
@Composable
fun ImageStripKnob(modifier: Modifier = Modifier,
                   @DrawableRes drawableResId: Int,
                   value: Float = 0f,
                   valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
                   explicitSizeInDp: Dp? = null,
                   minSizeInDp: Dp = defaultKnobMinSizeInDp,
                   tooltipColor: Color = Color.Gray,
                   tooltip: @Composable ImageStripKnobScope.() -> Unit = {
                       // by default, show tooltip only when it is being dragged
                       DefaultKnobTooltip(
                           value = knobValue,
                           showTooltip = knobIsBeingDragged,
                           textColor = tooltipColor
                       )
                   },
                   onValueChange: (value: Float) -> Unit = {}
) {
    // It is how `painterResource()` works.
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
        valueRange,
        explicitSizeInDp,
        minSizeInDp,
        tooltipColor,
        tooltip,
        onValueChange
    )
}

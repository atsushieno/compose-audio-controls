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


/**
 * Implements a knob control that is based on KnobMan image strip. See another ImageStripKnob() for more details.
 *
 * This Android-only function overload takes a drawable resource ID (`drawableResId`) to ease image loading.
 *
 * @param modifier      A `Modifier` to be applied to this knob control.
 * @param drawableResId The Android Resource Id for this knob.
 * @param value         The value that this knob should render for. It should be within the range between `minValue` and `maxValue`. Any value that is out of range is clipped.
 * @param minValue      The minimum value, which defines the value range, along with `maxValue`. It defaults to `0f`
 * @param maxValue      The maximum value, which defines the value range, along with `minValue`. It defaults to `1f`
 * @param minSizeInDp   The minimum rendered widget size in `Dp`. It defaults to `48.dp` which is the minimum recommended widget size by Android Accessibility Help.
 * @param tooltipColor  The color of the default implementation of the value label tooltip.
 * @param tooltip       The tooltip Composable which may be rendered in response to user's drag action over this knob.
 * @param valueChanged  An event handler function that takes the changed value.
 */
@Composable
fun ImageStripKnob(modifier: Modifier = Modifier,
                   @DrawableRes drawableResId: Int,
                   value: Float = 0f,
                   minValue: Float = 0f,
                   maxValue: Float = 1f, // typical float value range: 0.0 - 1.0
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
                   valueChanged: (value: Float) -> Unit = {}
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
        minValue,
        maxValue,
        minSizeInDp,
        tooltipColor,
        tooltip,
        valueChanged
    )
}

package org.androidaudioplugin.composeaudiocontrols.midi

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource

@Composable
fun MidiDeviceAccessScope.MidiKnobControllerCombo(@DrawableRes drawableResId: Int) {
    MidiKnobControllerCombo(ImageBitmap.imageResource(drawableResId))
}

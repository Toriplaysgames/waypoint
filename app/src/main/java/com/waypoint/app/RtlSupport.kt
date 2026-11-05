package com.waypoint.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** True while the app lays out right to left, as it does in Hebrew. */
@Composable @ReadOnlyComposable
internal fun isRtl(): Boolean = LocalLayoutDirection.current == LayoutDirection.Rtl

/** For directional icons without an auto-mirrored variant, such as chevrons. Turn arrows must not use this. */
internal fun Modifier.mirroredInRtl(rtl: Boolean): Modifier = if (rtl) scale(scaleX = -1f, scaleY = 1f) else this

/** A swipe toward the start of the reading direction moves forward: leftward in English, rightward in Hebrew. */
internal fun swipeStep(distance: Float, rtl: Boolean): Int = if ((distance < 0f) != rtl) 1 else -1

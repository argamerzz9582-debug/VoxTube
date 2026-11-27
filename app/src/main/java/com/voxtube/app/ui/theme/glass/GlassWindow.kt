package com.voxtube.app.ui.theme.glass

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.roundToInt

/**
 * Liquid Glass for things that live in their own window: dialogs, modal bottom sheets and menus.
 *
 * A separate window cannot sample the app's recorded backdrop, so it asks the system compositor to
 * blur what is behind the window instead (Android 12+, when the device has cross-window blur on).
 * The container is then drawn translucent with [glassWindowContainerColor]; without blur it stays
 * solid, so text on it never depends on the blur being there.
 */
object GlassWindowDefaults {
    /** How strongly the app behind a glass dialog or sheet is frosted. */
    val BlurBehindRadius = 36.dp

    /** The dim laid over the frosted app; lighter than Material's because the blur already separates. */
    const val DIM_AMOUNT = 0.22f

    /** Opacity of a glass window's container when the system blurs behind it. */
    const val CONTAINER_ALPHA = 0.78f
}

/** Whether windows can blur what is behind them right now. False below Android 12. */
@Composable
fun rememberCrossWindowBlur(): Boolean {
    val context = LocalContext.current
    val enabled = LocalLiquidGlassEnabled.current
    return remember(context, enabled) {
        enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true
    }
}

/**
 * The container colour of a glass dialog or sheet: [base] made translucent when the window behind is
 * blurred ([rememberCrossWindowBlur]), [base] itself otherwise.
 */
@Composable
fun glassWindowContainerColor(base: Color = MaterialTheme.colorScheme.surfaceContainerHigh): Color =
    if (rememberCrossWindowBlur()) base.copy(alpha = base.alpha * GlassWindowDefaults.CONTAINER_ALPHA) else base

/** The theme's sheet colour for glass windows. */
@Composable
@ReadOnlyComposable
fun glassSheetBase(): Color = MaterialTheme.colorScheme.surfaceContainerLow

/**
 * Turns on the system blur behind the dialog or modal sheet window this is composed in. Call it from
 * inside the dialog's content. Does nothing outside a dialog window or where blur is unavailable.
 */
@Composable
fun GlassWindowBlur() {
    if (!rememberCrossWindowBlur()) return
    val view = LocalView.current
    val radiusPx = with(LocalDensity.current) { GlassWindowDefaults.BlurBehindRadius.toPx() }.roundToInt()
    DisposableEffect(view, radiusPx) {
        val window = view.dialogWindow()
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = radiusPx }
            window.setDimAmount(GlassWindowDefaults.DIM_AMOUNT)
        }
        onDispose { }
    }
}

/** The window of the dialog [this] view is composed in, found through the dialog's root layout. */
private fun View.dialogWindow(): Window? {
    var current: Any? = this
    while (current is View) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent
    }
    return null
}

/** The hairline rim that catches the light on a glass window's edge; null where the window is solid. */
@Composable
fun glassWindowRim(): BorderStroke? {
    if (!rememberCrossWindowBlur()) return null
    return BorderStroke(Dp.Hairline, LiquidGlassDefaults.colors().rim)
}

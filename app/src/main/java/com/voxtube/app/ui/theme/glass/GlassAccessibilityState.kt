package com.voxtube.app.ui.theme.glass

import android.app.UiModeManager
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * The system accessibility states Liquid Glass follows, kept current while the app is open:
 * - "Remove animations" (the animator duration scale at 0) stops the lens refraction;
 * - a raised contrast level (Android 14+) makes glass nearly opaque with a firm edge.
 *
 * Android has no system "Reduce transparency" switch, so that one is the app's own setting
 * (Appearance → Reduce transparency), applied through [LocalLiquidGlassEnabled].
 */
@Composable
fun rememberGlassAccessibility(): GlassAccessibility {
    val context = LocalContext.current
    var reduceMotion by remember(context) { mutableStateOf(animationsOff(context)) }
    var increaseContrast by remember(context) { mutableStateOf(contrastRaised(context)) }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reduceMotion = animationsOff(context)
                }
            }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        val stopContrast =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                listenToContrast(context) { increaseContrast = it }
            } else {
                {}
            }
        onDispose {
            resolver.unregisterContentObserver(observer)
            stopContrast()
        }
    }
    return remember(reduceMotion, increaseContrast) { GlassAccessibility(reduceMotion, increaseContrast) }
}

/** Follows the system contrast level; returns what stops following it. */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun listenToContrast(
    context: Context,
    onChange: (Boolean) -> Unit,
): () -> Unit {
    val uiModeManager = context.getSystemService(UiModeManager::class.java) ?: return {}
    val listener = UiModeManager.ContrastChangeListener { level -> onChange(level > 0f) }
    uiModeManager.addContrastChangeListener(context.mainExecutor, listener)
    return { uiModeManager.removeContrastChangeListener(listener) }
}

private fun animationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private fun contrastRaised(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        (context.getSystemService(UiModeManager::class.java)?.contrast ?: 0f) > 0f

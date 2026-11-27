package com.voxtube.app.ui.components.shared

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** A soft, bouncy spring: the icon overshoots to about 1.2× and settles in roughly 400 ms. */
private val PopSpring = spring<Float>(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)
private const val POP_VELOCITY = 5f

/**
 * Confirms an on/off action such as a like: the moment [on] turns true the element pops past its size and
 * settles, once. Turning off, or the first composition, plays nothing, so a list of already-liked items
 * never bounces as it scrolls in. With system animations off the spring finishes at once.
 */
@Composable
fun Modifier.popWhenTurnedOn(on: Boolean): Modifier {
    val pop = remember { Animatable(1f) }
    val wasOn = remember { booleanArrayOf(on) }
    LaunchedEffect(on) {
        val turnedOn = on && !wasOn[0]
        wasOn[0] = on
        if (turnedOn) pop.animateTo(1f, PopSpring, initialVelocity = POP_VELOCITY)
    }
    return graphicsLayer {
        scaleX = pop.value
        scaleY = pop.value
    }
}

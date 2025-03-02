package xyz.ecys.vocab.ui.theme

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize

object AppAnimations {
    @Composable
    private fun isReducedAnimations(): Boolean {
        val context = LocalContext.current
        val prefs = context.getSharedPreferences("vocab_settings", Context.MODE_PRIVATE)
        return remember {
            prefs.getBoolean("reduced_animations", false)
        }
    }

    @Composable
    fun <T> springSpec(): SpringSpec<T> {
        return if (isReducedAnimations()) {
            spring(
                dampingRatio = 1f,  // Critical damping (no oscillation)
                stiffness = Spring.StiffnessMedium,
                visibilityThreshold = null
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
                visibilityThreshold = null
            )
        }
    }

    @Composable
    fun <T> tweenSpec(): TweenSpec<T> {
        return if (isReducedAnimations()) {
            tween(
                durationMillis = 100,
                easing = LinearEasing
            )
        } else {
            tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        }
    }

    @Composable
    fun contentSizeSpec(): FiniteAnimationSpec<IntSize> {
        return if (isReducedAnimations()) {
            tween(
                durationMillis = 100,
                easing = LinearEasing
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        }
    }
} 
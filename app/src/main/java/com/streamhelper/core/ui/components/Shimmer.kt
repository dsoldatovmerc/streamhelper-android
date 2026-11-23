package com.streamhelper.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.streamhelper.core.ui.theme.AppColors

/**
 * Shimmer placeholder (RN `react-native-reanimated-skeleton`): fills the element with a gray
 * block swept by a diagonal highlight. Give the element an explicit size; it draws no content.
 */
@Composable
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(4.dp)): Modifier {
    // Read only in the draw phase, so the animation redraws without recomposing.
    val progress = rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(SHIMMER_DURATION_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    return clip(shape).drawBehind {
        val span = size.width.coerceAtLeast(size.height)
        // The highlight band travels from below the bottom-left corner to past the top-right one,
        // like RN's `diagonalTopRight`.
        val shift = progress.value * span - span
        val start = Offset(shift, size.height - shift)
        val brush = Brush.linearGradient(
            colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
            start = start,
            end = Offset(start.x + span, start.y - span),
        )
        drawRect(brush)
    }
}

private const val SHIMMER_DURATION_MILLIS = 1200
private val ShimmerBase = AppColors.Gray.copy(alpha = 0.6f)
private val ShimmerHighlight = AppColors.Gray.copy(alpha = 0.2f)

package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class SwipeActionButtonSize { ExtraSmall, Small, Large, ExtraLarge }

/**
 * Circular "glass" action button used in the Tinder-style action row
 * (refresh/dislike/like/view/info). Default colors match the unselected "glass-card" treatment;
 * callers override for the accented dislike/like/view roles.
 */
@Composable
fun SwipeActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: SwipeActionButtonSize = SwipeActionButtonSize.Small,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f),
    contentColor: Color = MaterialTheme.colorScheme.secondary,
    borderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
) {
    val dimension =
        when (size) {
            SwipeActionButtonSize.ExtraLarge -> 80.dp
            SwipeActionButtonSize.Large -> 64.dp
            SwipeActionButtonSize.Small -> 48.dp
            SwipeActionButtonSize.ExtraSmall -> 36.dp
        }
    val iconSize =
        when (size) {
            SwipeActionButtonSize.ExtraLarge -> 40.dp
            SwipeActionButtonSize.Large -> 32.dp
            SwipeActionButtonSize.Small -> 24.dp
            SwipeActionButtonSize.ExtraSmall -> 18.dp
        }
    val borderWidth = if (size == SwipeActionButtonSize.Small || size == SwipeActionButtonSize.ExtraSmall) 1.dp else 2.dp

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "actionButtonPressScale",
    )

    Box(
        modifier =
            modifier
                .size(dimension)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                // Without this clip, clickable()'s ripple ignores the circle below it and draws
                // a square across the button's full bounds instead of a clean circular ripple.
                .clip(CircleShape)
                .background(containerColor, CircleShape)
                .border(borderWidth, borderColor, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

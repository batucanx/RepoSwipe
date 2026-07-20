package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.common.model.SwipeDirection
import com.batuhan.reposwipe.core.designsystem.theme.SwipeLeftOverlay
import com.batuhan.reposwipe.core.designsystem.theme.SwipeRightOverlay
import kotlinx.coroutines.launch

/**
 * Drives the front card's drag/animation. Shared between [SwipeDeck]'s own gesture handling
 * and any external trigger (e.g. the Star/Skip action buttons) that calls [swipeLeft]/[swipeRight]
 * directly, so both paths play the identical animation.
 */
@Stable
class SwipeDeckState {
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)

    var dragDirection by mutableStateOf<SwipeDirection?>(null)
        private set

    internal var hintThresholdPx = 0f
    internal var commitThresholdPx = 0f
    internal var exitDistancePx = 0f
    internal var onCommit: (suspend (SwipeDirection) -> Unit)? = null

    suspend fun onDrag(deltaX: Float, deltaY: Float) {
        offset.snapTo(Offset(offset.value.x + deltaX, offset.value.y + deltaY))
        dragDirection = when {
            offset.value.x > hintThresholdPx -> SwipeDirection.Right
            offset.value.x < -hintThresholdPx -> SwipeDirection.Left
            else -> null
        }
    }

    suspend fun onDragEnd() {
        when {
            offset.value.x > commitThresholdPx -> commit(SwipeDirection.Right)
            offset.value.x < -commitThresholdPx -> commit(SwipeDirection.Left)
            else -> snapBack()
        }
    }

    suspend fun swipeRight() = commit(SwipeDirection.Right)

    suspend fun swipeLeft() = commit(SwipeDirection.Left)

    private suspend fun commit(direction: SwipeDirection) {
        dragDirection = direction
        val targetX = if (direction == SwipeDirection.Right) exitDistancePx else -exitDistancePx
        offset.animateTo(Offset(targetX, offset.value.y), tween(EXIT_DURATION_MS))
        onCommit?.invoke(direction)
        offset.snapTo(Offset.Zero)
        dragDirection = null
    }

    private suspend fun snapBack() {
        offset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 380f))
        dragDirection = null
    }

    private companion object {
        const val EXIT_DURATION_MS = 400
    }
}

@Composable
fun rememberSwipeDeckState(): SwipeDeckState = remember { SwipeDeckState() }

/**
 * Tinder-style card stack: the front card is draggable, up to 2 more peek out behind it for
 * depth. Physics mirror the `code.html` reference prototype — 150dp commit threshold, 50dp
 * swipe-direction hint, rotation = dx/20, spring-back on a cancelled drag. External triggers
 * (e.g. the Star/Skip buttons) call [state]'s `swipeLeft`/`swipeRight` to play the same animation.
 *
 * The drag gesture itself isn't reachable by TalkBack, so the front card also exposes
 * [leftActionLabel]/[rightActionLabel] as accessibility custom actions — matching the visible
 * action buttons the caller renders alongside this deck.
 */
@Composable
fun <T> SwipeDeck(
    items: List<T>,
    itemKey: (T) -> Any,
    onSwiped: (item: T, direction: SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
    state: SwipeDeckState = rememberSwipeDeckState(),
    leftActionLabel: String = "Sola kaydır",
    rightActionLabel: String = "Sağa kaydır",
    content: @Composable (item: T) -> Unit,
) {
    val visible = items.take(MAX_VISIBLE_CARDS)
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        state.hintThresholdPx = with(density) { SWIPE_HINT_DP.dp.toPx() }
        state.commitThresholdPx = with(density) { SWIPE_COMMIT_DP.dp.toPx() }
        state.exitDistancePx = with(density) { maxWidth.toPx() } * 1.5f

        visible.asReversed().forEachIndexed { reversedIndex, item ->
            val stackIndex = visible.size - 1 - reversedIndex
            key(itemKey(item)) {
                if (stackIndex == 0) {
                    state.onCommit = { direction -> onSwiped(item, direction) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = state.offset.value.x
                                translationY = state.offset.value.y
                                rotationZ = state.offset.value.x / ROTATION_DIVISOR
                            }
                            .pointerInput(itemKey(item)) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch { state.onDrag(dragAmount.x, dragAmount.y) }
                                    },
                                    onDragEnd = { coroutineScope.launch { state.onDragEnd() } },
                                    onDragCancel = { coroutineScope.launch { state.onDragEnd() } },
                                )
                            }
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction(rightActionLabel) {
                                        coroutineScope.launch { state.swipeRight() }
                                        true
                                    },
                                    CustomAccessibilityAction(leftActionLabel) {
                                        coroutineScope.launch { state.swipeLeft() }
                                        true
                                    },
                                )
                            },
                    ) {
                        content(item)
                        val overlayColor = when (state.dragDirection) {
                            SwipeDirection.Right -> SwipeRightOverlay
                            SwipeDirection.Left -> SwipeLeftOverlay
                            null -> null
                        }
                        if (overlayColor != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(overlayColor.copy(alpha = 0.15f)),
                            )
                        }
                    }
                } else {
                    val depth = BACKGROUND_DEPTH.getOrElse(stackIndex - 1) { BACKGROUND_DEPTH.last() }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = depth.scale
                                scaleY = depth.scale
                                translationY = with(density) { depth.translateYDp.dp.toPx() }
                                alpha = depth.alpha
                            },
                    ) {
                        content(item)
                    }
                }
            }
        }
    }
}

private data class BackgroundDepth(val scale: Float, val translateYDp: Float, val alpha: Float)

private val BACKGROUND_DEPTH = listOf(
    BackgroundDepth(scale = 0.98f, translateYDp = 8f, alpha = 0.8f),
    BackgroundDepth(scale = 0.95f, translateYDp = 16f, alpha = 0.5f),
)

private const val MAX_VISIBLE_CARDS = 3
private const val SWIPE_HINT_DP = 50f
private const val SWIPE_COMMIT_DP = 150f
private const val ROTATION_DIVISOR = 20f

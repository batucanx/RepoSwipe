package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.input.pointer.util.VelocityTracker
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
    internal var flingVelocityThresholdPx = 0f
    internal var onCommit: (suspend (SwipeDirection) -> Unit)? = null

    suspend fun onDrag(
        deltaX: Float,
        deltaY: Float,
    ) {
        offset.snapTo(Offset(offset.value.x + deltaX, offset.value.y + deltaY))
        dragDirection =
            when {
                offset.value.x > hintThresholdPx -> SwipeDirection.Right
                offset.value.x < -hintThresholdPx -> SwipeDirection.Left
                else -> null
            }
    }

    /**
     * [velocity] is the pointer's release velocity (px/s) — a fast flick past the hint threshold
     * commits even if the drag never reached [commitThresholdPx], matching native fling behavior.
     * The same velocity seeds the follow-up spring so the card keeps the motion it was thrown with
     * instead of restarting from a standstill.
     */
    suspend fun onDragEnd(velocity: Offset) {
        val flungRight = velocity.x > flingVelocityThresholdPx && offset.value.x > hintThresholdPx
        val flungLeft = velocity.x < -flingVelocityThresholdPx && offset.value.x < -hintThresholdPx
        when {
            offset.value.x > commitThresholdPx || flungRight -> commit(SwipeDirection.Right, velocity)
            offset.value.x < -commitThresholdPx || flungLeft -> commit(SwipeDirection.Left, velocity)
            else -> snapBack(velocity)
        }
    }

    suspend fun swipeRight() = commit(SwipeDirection.Right, Offset.Zero)

    suspend fun swipeLeft() = commit(SwipeDirection.Left, Offset.Zero)

    private suspend fun commit(
        direction: SwipeDirection,
        velocity: Offset,
    ) {
        dragDirection = direction
        val targetX = if (direction == SwipeDirection.Right) exitDistancePx else -exitDistancePx
        offset.animateTo(
            targetValue = Offset(targetX, offset.value.y),
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = EXIT_STIFFNESS),
            initialVelocity = velocity,
        )
        onCommit?.invoke(direction)
        offset.snapTo(Offset.Zero)
        dragDirection = null
    }

    private suspend fun snapBack(velocity: Offset) {
        offset.animateTo(
            targetValue = Offset.Zero,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 380f),
            initialVelocity = velocity,
        )
        dragDirection = null
    }

    private companion object {
        const val EXIT_STIFFNESS = 300f
    }
}

@Composable
fun rememberSwipeDeckState(): SwipeDeckState = remember { SwipeDeckState() }

/**
 * Tinder-style card stack: the front card is draggable, up to 2 more peek out behind it for
 * depth. Base physics mirror the `code.html` reference prototype — 150dp commit threshold, 50dp
 * swipe-direction hint, rotation = dx/20 — layered with real release-velocity tracking so a fast
 * flick commits the swipe even under the threshold, and both the exit and the cancelled-drag
 * spring-back inherit that velocity instead of starting from a standstill. Promoting the next card
 * to front animates its scale/offset/alpha from its depth position instead of cutting instantly.
 * External triggers (e.g. the Star/Skip buttons) call [state]'s `swipeLeft`/`swipeRight` to play
 * the same animation.
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
        state.flingVelocityThresholdPx = with(density) { SWIPE_FLING_VELOCITY_DP.dp.toPx() }

        visible.asReversed().forEachIndexed { reversedIndex, item ->
            val stackIndex = visible.size - 1 - reversedIndex
            val isFront = stackIndex == 0
            key(itemKey(item)) {
                if (isFront) state.onCommit = { direction -> onSwiped(item, direction) }

                // Same spring animates a card whether it's settling deeper into the stack or
                // being promoted to front — since this key's composable slot persists across that
                // transition (only the target values change), the stack visibly "advances" instead
                // of hard-cutting into place.
                val depthTarget = depthTargetFor(stackIndex)
                val animatedScale by animateFloatAsState(depthTarget.scale, STACK_SPRING, label = "cardScale")
                val animatedTranslateY by animateFloatAsState(depthTarget.translateYDp, STACK_SPRING, label = "cardTranslateY")
                val animatedAlpha by animateFloatAsState(depthTarget.alpha, STACK_SPRING, label = "cardAlpha")

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                                translationX = if (isFront) state.offset.value.x else 0f
                                translationY = (if (isFront) state.offset.value.y else 0f) +
                                    with(density) { animatedTranslateY.dp.toPx() }
                                rotationZ = if (isFront) state.offset.value.x / ROTATION_DIVISOR else 0f
                                alpha = animatedAlpha
                            }.then(
                                if (isFront) {
                                    Modifier
                                        .pointerInput(itemKey(item)) {
                                            var velocityTracker = VelocityTracker()
                                            detectDragGestures(
                                                onDragStart = { velocityTracker = VelocityTracker() },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                                    coroutineScope.launch { state.onDrag(dragAmount.x, dragAmount.y) }
                                                },
                                                onDragEnd = {
                                                    val velocity = velocityTracker.calculateVelocity()
                                                    coroutineScope.launch {
                                                        state.onDragEnd(Offset(velocity.x, velocity.y))
                                                    }
                                                },
                                                onDragCancel = {
                                                    coroutineScope.launch { state.onDragEnd(Offset.Zero) }
                                                },
                                            )
                                        }.semantics {
                                            customActions =
                                                listOf(
                                                    CustomAccessibilityAction(rightActionLabel) {
                                                        coroutineScope.launch { state.swipeRight() }
                                                        true
                                                    },
                                                    CustomAccessibilityAction(leftActionLabel) {
                                                        coroutineScope.launch { state.swipeLeft() }
                                                        true
                                                    },
                                                )
                                        }
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    content(item)
                    if (isFront) {
                        val overlayColor =
                            when (state.dragDirection) {
                                SwipeDirection.Right -> SwipeRightOverlay
                                SwipeDirection.Left -> SwipeLeftOverlay
                                null -> null
                            }
                        if (overlayColor != null) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.extraLarge)
                                        .background(overlayColor.copy(alpha = 0.15f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class BackgroundDepth(
    val scale: Float,
    val translateYDp: Float,
    val alpha: Float,
)

private val FRONT_DEPTH = BackgroundDepth(scale = 1f, translateYDp = 0f, alpha = 1f)

private val BACKGROUND_DEPTH =
    listOf(
        BackgroundDepth(scale = 0.98f, translateYDp = 8f, alpha = 0.8f),
        BackgroundDepth(scale = 0.95f, translateYDp = 16f, alpha = 0.5f),
    )

private fun depthTargetFor(stackIndex: Int): BackgroundDepth =
    if (stackIndex == 0) FRONT_DEPTH else BACKGROUND_DEPTH.getOrElse(stackIndex - 1) { BACKGROUND_DEPTH.last() }

private val STACK_SPRING = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

private const val MAX_VISIBLE_CARDS = 3
private const val SWIPE_HINT_DP = 50f
private const val SWIPE_COMMIT_DP = 150f
private const val SWIPE_FLING_VELOCITY_DP = 1200f
private const val ROTATION_DIVISOR = 20f

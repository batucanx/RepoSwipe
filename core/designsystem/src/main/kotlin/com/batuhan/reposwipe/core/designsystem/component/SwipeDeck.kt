package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.common.model.SwipeDirection
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.SwipeLeftOverlay
import com.batuhan.reposwipe.core.designsystem.theme.SwipeRightOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Drives the front card's live drag/snap-back. Shared between [SwipeDeck]'s own gesture handling
 * and any external trigger (e.g. the Star/Skip action buttons) that calls [swipeLeft]/[swipeRight]
 * directly, so both paths play the identical animation.
 *
 * [offset]/[dragDirection] only ever describe whichever card is *currently* front — the instant a
 * swipe commits, both reset immediately so the next card is draggable/tappable right away. The
 * committed card's own fly-off-screen animation is handed off to [onExitRequested] instead of
 * being awaited here, which is what [SwipeDeck] uses to keep that card rendered (and animating)
 * independently for as long as its exit takes, without blocking the next card's interactivity on
 * it. See [SwipeDeck]'s doc comment for why this split exists.
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

    /** Synchronous (not suspend) so [SwipeDeck] can snapshot "the item that just committed"
     * before [onCommit] advances the caller's index and that item potentially drops out of
     * `items` — direction/start-offset/release-velocity are everything the exit animation needs. */
    internal var onExitRequested: ((direction: SwipeDirection, startOffset: Offset, velocity: Offset) -> Unit)? = null

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

    /**
     * Frees the live drag slot for the next card *before* [onCommit] runs, instead of after the
     * exit animation finishes — that ordering used to leave a several-hundred-ms window where a
     * fast follow-up tap/swipe landed on a card with no gesture handling yet (the next card only
     * became "front" once the previous one's exit animation had fully played out). The exit flight
     * itself is delegated to [onExitRequested]/[SwipeDeck] so it can keep playing out visually
     * without this state (or the next card) waiting on it.
     */
    private suspend fun commit(
        direction: SwipeDirection,
        velocity: Offset,
    ) {
        val startOffset = offset.value
        offset.snapTo(Offset.Zero)
        dragDirection = null
        onExitRequested?.invoke(direction, startOffset, velocity)
        onCommit?.invoke(direction)
    }

    private suspend fun snapBack(velocity: Offset) {
        offset.animateTo(
            targetValue = Offset.Zero,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 380f),
            initialVelocity = velocity,
        )
        dragDirection = null
    }
}

@Composable
fun rememberSwipeDeckState(): SwipeDeckState = remember { SwipeDeckState() }

/** A card mid-flight after committing, rendered independently of `items`/the front/background
 * stack so its exit animation finishes on its own schedule — see [SwipeDeck]'s doc comment. Plain
 * (not data) class: referential equality is exactly what's wanted for the `key()` each instance is
 * used with below, since two exits of the same [item] are still two distinct flights. [key] is
 * cached from the caller's `itemKey(item)` at commit time so the main stack loop can cheaply check
 * "is this item already exiting" without re-deriving a key from `item` every recomposition. */
private class ExitingCard<T>(
    val item: T,
    val key: Any,
    val direction: SwipeDirection,
    val offset: Animatable<Offset, AnimationVector2D>,
)

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
 * A committed card's exit flight is rendered as a standalone overlay (see [ExitingCard]) instead
 * of being part of the front/background stack: the *next* card is promoted to front — and made
 * draggable/tappable — the instant the swipe commits, rather than waiting for the previous card to
 * finish flying off screen. Without that split, a fast follow-up swipe or tap during the exit
 * animation had nothing to land on (the next card had no gesture handling until the stack
 * advanced), which read as the deck "not registering" quick successive input.
 *
 * The main stack loop below explicitly skips any item already present in [exitingCards] rather
 * than trusting `items` to have dropped it by the time this recomposes. `state.offset` (the front
 * card's live drag position) is snapped back to zero the instant a swipe commits — needed so the
 * *next* card starts from a clean offset once it's promoted — but `items` only reflects that same
 * swipe once the caller's own state (typically a ViewModel index bump) round-trips back through
 * recomposition, which is not guaranteed to land in the same frame. Left unskipped, the
 * just-committed card kept its old stack slot for however many frames that round trip took, now
 * reading `state.offset` as zero — i.e. it visibly snapped back to dead center and sat there,
 * looking exactly like a stuck/ghost duplicate of the card that was otherwise correctly flying
 * away via its [ExitingCard] overlay.
 *
 * The drag gesture itself isn't reachable by TalkBack, so the front card also exposes
 * [leftActionLabel]/[rightActionLabel] as accessibility custom actions — matching the visible
 * action buttons the caller renders alongside this deck.
 *
 * A tap on the front card (one that doesn't turn into a drag) fires [onCardTap] — e.g. to open a
 * detail view — without disturbing the drag/fling physics above.
 */
@Composable
fun <T> SwipeDeck(
    items: List<T>,
    itemKey: (T) -> Any,
    onSwiped: (item: T, direction: SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
    state: SwipeDeckState = rememberSwipeDeckState(),
    leftActionLabel: String = "Swipe left",
    rightActionLabel: String = "Swipe right",
    onCardTap: ((item: T) -> Unit)? = null,
    content: @Composable (item: T) -> Unit,
) {
    val visible = items.take(MAX_VISIBLE_CARDS)
    val density = LocalDensity.current
    // ROTATION_DIVISOR mirrors code.html's `dx / 20`, calibrated against CSS px (≈dp) in that
    // reference. state.offset/exitingCard.offset track raw pointer-drag pixels, which on any
    // screen denser than 1x are a multiple of dp — dividing them by ROTATION_DIVISOR directly
    // scaled the tilt by that same density factor (e.g. ~3x on a 3x-density phone), so a drag
    // that should read as a ~7-20° tilt spun the card 60-85° instead, sweeping a rotated corner
    // (showing that card's own header color) out past the deck's opposite edge mid-swipe/exit —
    // the "notch" reported on the trailing side of a swipe. Folding the density factor into the
    // divisor once here keeps the rotation in the dp-equivalent range the mockup intends,
    // regardless of screen density.
    val rotationDivisorPx = ROTATION_DIVISOR * density.density
    val externalScope = rememberCoroutineScope()
    val exitingCards = remember { mutableStateListOf<ExitingCard<T>>() }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        state.hintThresholdPx = with(density) { SWIPE_HINT_DP.dp.toPx() }
        state.commitThresholdPx = with(density) { SWIPE_COMMIT_DP.dp.toPx() }
        state.exitDistancePx = with(density) { maxWidth.toPx() } * 1.5f
        state.flingVelocityThresholdPx = with(density) { SWIPE_FLING_VELOCITY_DP.dp.toPx() }

        visible.asReversed().forEachIndexed { reversedIndex, item ->
            val stackIndex = visible.size - 1 - reversedIndex
            val isFront = stackIndex == 0
            val keyForItem = itemKey(item)
            // Already animating away via the overlay below — see this function's doc comment for
            // why `items` can still list it here for a stale frame or two after it committed.
            if (exitingCards.any { it.key == keyForItem }) return@forEachIndexed
            key(keyForItem) {
                if (isFront) {
                    state.onCommit = { direction -> onSwiped(item, direction) }
                    state.onExitRequested = { direction, startOffset, velocity ->
                        val exitOffset = Animatable(startOffset, Offset.VectorConverter)
                        val exitingCard = ExitingCard(item = item, key = keyForItem, direction = direction, offset = exitOffset)
                        exitingCards.add(exitingCard)
                        externalScope.launch {
                            val targetX = if (direction == SwipeDirection.Right) state.exitDistancePx else -state.exitDistancePx
                            exitOffset.animateTo(
                                targetValue = Offset(targetX, startOffset.y),
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = EXIT_STIFFNESS),
                                initialVelocity = velocity,
                            )
                            exitingCards.remove(exitingCard)
                        }
                    }
                }

                // Same spring animates a card whether it's settling deeper into the stack or
                // being promoted to front — since this key's composable slot persists across that
                // transition (only the target values change), the stack visibly "advances" instead
                // of hard-cutting into place.
                val depthTarget = depthTargetFor(stackIndex)
                val animatedScale by animateFloatAsState(depthTarget.scale, STACK_SPRING, label = "cardScale")
                val animatedRotation by animateFloatAsState(depthTarget.rotationDeg, STACK_SPRING, label = "cardRotation")
                val animatedAlpha by animateFloatAsState(depthTarget.alpha, STACK_SPRING, label = "cardAlpha")

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                                translationX = if (isFront) state.offset.value.x else 0f
                                translationY = if (isFront) state.offset.value.y else 0f
                                rotationZ = if (isFront) state.offset.value.x / rotationDivisorPx else animatedRotation
                                alpha = animatedAlpha
                            }.then(
                                if (isFront) {
                                    Modifier.frontCardGestures(
                                        key = keyForItem,
                                        item = item,
                                        state = state,
                                        externalScope = externalScope,
                                        leftActionLabel = leftActionLabel,
                                        rightActionLabel = rightActionLabel,
                                        onCardTap = onCardTap,
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    content(item)
                    if (isFront) {
                        // dragDirection is a discrete state that flips at most a couple of times
                        // per swipe, so reading it in composition is fine. The continuously
                        // changing offset deliberately is *not* read here — see [SwipeBadgeOverlay].
                        val direction = state.dragDirection
                        if (direction != null) {
                            SwipeBadgeOverlay(
                                direction = direction,
                                progress = { (abs(state.offset.value.x) / state.commitThresholdPx).coerceIn(0f, 1f) },
                            )
                        }
                    }
                }
            }
        }

        exitingCards.forEach { exitingCard ->
            key(exitingCard) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = exitingCard.offset.value.x
                                translationY = exitingCard.offset.value.y
                                rotationZ = exitingCard.offset.value.x / rotationDivisorPx
                            },
                ) {
                    content(exitingCard.item)
                    SwipeBadgeOverlay(
                        direction = exitingCard.direction,
                        progress = { (abs(exitingCard.offset.value.x) / state.commitThresholdPx).coerceIn(0f, 1f) },
                    )
                }
            }
        }
    }
}

/**
 * The "LIKE"/"PASS" badge + tint that fades in over a card as it crosses the swipe threshold —
 * shared by the live front card (progress driven by drag distance) and each [ExitingCard] in
 * flight (progress driven by its own exit offset, so it reads as fully committed for the whole
 * flight rather than fading back out). Large and dead-center — a corner-anchored "stamp" variant
 * was tried and reverted per feedback: it read as barely-noticeable next to the card's own
 * content, where this app's whole point is a fast, unambiguous like/pass cue mid-swipe.
 *
 * [progress] is a lambda, not a `Float`, specifically so the caller's `Animatable.value` read
 * happens inside the draw/layer phase instead of during composition. Taking it by value meant
 * every frame of a drag or exit flight invalidated the *composition* scope that computed it —
 * which is the scope that also emits the card content — so each frame re-ran the card's
 * composable and rebuilt this overlay's whole modifier chain, just to arrive at the same pixels
 * a layer property change would have produced for free. Deferring the read keeps the entire drag
 * path free of recomposition: only `drawBehind`/`graphicsLayer` re-execute.
 */
@Composable
private fun BoxScope.SwipeBadgeOverlay(
    direction: SwipeDirection,
    progress: () -> Float,
) {
    val isRight = direction == SwipeDirection.Right
    val overlayColor = if (isRight) SwipeRightOverlay else SwipeLeftOverlay
    val badgeIcon = if (isRight) RepoSwipeIcons.SwipeLike else RepoSwipeIcons.SwipeReject

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
                .drawBehind { drawRect(overlayColor.copy(alpha = OVERLAY_TINT_ALPHA * progress())) },
    )
    Box(
        modifier =
            Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    val currentProgress = progress()
                    scaleX = currentProgress
                    scaleY = currentProgress
                    alpha = currentProgress
                }.size(BADGE_SIZE_DP.dp)
                .background(overlayColor, CircleShape)
                .border(BADGE_BORDER_DP.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = badgeIcon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(BADGE_ICON_DP.dp),
        )
    }
}

/**
 * Drag-to-swipe (with velocity-aware fling/spring-back) plus an optional tap-to-[onCardTap],
 * combined in one [pointerInput] so neither gesture steals events from the other. Also exposes
 * [leftActionLabel]/[rightActionLabel] as TalkBack custom actions, since the drag itself isn't
 * reachable by accessibility services.
 */
private fun <T> Modifier.frontCardGestures(
    key: Any,
    item: T,
    state: SwipeDeckState,
    externalScope: CoroutineScope,
    leftActionLabel: String,
    rightActionLabel: String,
    onCardTap: ((item: T) -> Unit)?,
): Modifier =
    this
        .pointerInput(key) {
            coroutineScope {
                launch {
                    var velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = { velocityTracker = VelocityTracker() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            externalScope.launch { state.onDrag(dragAmount.x, dragAmount.y) }
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity()
                            externalScope.launch { state.onDragEnd(Offset(velocity.x, velocity.y)) }
                        },
                        onDragCancel = { externalScope.launch { state.onDragEnd(Offset.Zero) } },
                    )
                }
                if (onCardTap != null) {
                    launch { detectTapGestures(onTap = { onCardTap(item) }) }
                }
            }
        }.semantics {
            customActions =
                listOf(
                    CustomAccessibilityAction(rightActionLabel) {
                        externalScope.launch { state.swipeRight() }
                        true
                    },
                    CustomAccessibilityAction(leftActionLabel) {
                        externalScope.launch { state.swipeLeft() }
                        true
                    },
                )
        }

private data class BackgroundDepth(
    val scale: Float,
    val rotationDeg: Float,
    val alpha: Float,
)

private val FRONT_DEPTH = BackgroundDepth(scale = 1f, rotationDeg = 0f, alpha = 1f)

// code.html's reference (scale-95 -rotate-1 opacity-40) is a plain shadow shape behind the deck,
// not a full rendered card — this app instead peeks the *real* next RepoCard at that opacity,
// and in dark theme its own dark surfaceContainer background all but disappears against the
// page's near-black background at 40% alpha, while its light onSurface text doesn't — so what
// actually showed through was legible ghost text (title/stats/description), not a subtle shadow.
// Dropping the opacity further (down from 0.4/0.2) keeps just enough of a peeking-card silhouette
// for depth without the text underneath being readable.
private val BACKGROUND_DEPTH =
    listOf(
        BackgroundDepth(scale = 0.95f, rotationDeg = -1f, alpha = 0.12f),
        BackgroundDepth(scale = 0.90f, rotationDeg = -2f, alpha = 0.05f),
    )

private fun depthTargetFor(stackIndex: Int): BackgroundDepth =
    if (stackIndex == 0) FRONT_DEPTH else BACKGROUND_DEPTH.getOrElse(stackIndex - 1) { BACKGROUND_DEPTH.last() }

// MediumBouncy (0.2 damping ratio) at StiffnessMediumLow used to take close to a second to fully
// settle its oscillation — every promoted card kept visibly rocking in scale/rotation for a beat
// after a swipe, reading as sluggish rather than the crisp, near-instant snap real Tinder uses.
// A touch of bounce still reads as "alive" rather than mechanical, but at this stiffness it
// settles in well under 150ms instead of trailing off for the better part of a second.
private val STACK_SPRING = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)

private const val MAX_VISIBLE_CARDS = 3
private const val SWIPE_HINT_DP = 50f
private const val SWIPE_COMMIT_DP = 150f
private const val SWIPE_FLING_VELOCITY_DP = 1200f
private const val ROTATION_DIVISOR = 20f
private const val EXIT_STIFFNESS = 300f
private const val OVERLAY_TINT_ALPHA = 0.15f
private const val BADGE_SIZE_DP = 96
private const val BADGE_BORDER_DP = 4
private const val BADGE_ICON_DP = 48

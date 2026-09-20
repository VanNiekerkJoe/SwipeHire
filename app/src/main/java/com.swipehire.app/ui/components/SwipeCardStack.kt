package com.swipehire.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swipehire.app.ui.theme.Coral
import com.swipehire.app.ui.theme.Mint40
import kotlinx.coroutines.launch

enum class SwipeDirection { LEFT, RIGHT }

/**
 * A Tinder-style draggable card stack. Shows up to 3 cards at once — the two
 * behind the top card peek out with a faint alternating tilt so the stack
 * reads as hand-dealt rather than mechanically stamped — and reports a swipe
 * direction once the drag clears the threshold.
 */
@Composable
fun <T> SwipeCardStack(
    items: List<T>,
    modifier: Modifier = Modifier,
    /** Set to trigger the top card swiping programmatically (e.g. from the pass/like buttons). */
    pendingSwipe: SwipeDirection? = null,
    onPendingSwipeHandled: () -> Unit = {},
    onSwiped: (item: T, direction: SwipeDirection) -> Unit,
    emptyContent: @Composable () -> Unit,
    cardContent: @Composable (item: T) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { emptyContent() }
        return
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        items.take(3).reversed().forEachIndexed { reversedIndex, item ->
            val stackIndex = items.take(3).size - 1 - reversedIndex
            val isTop = stackIndex == 0
            val scale = 1f - (stackIndex * 0.045f)
            val yOffset = stackIndex * 16f
            val tilt = if (stackIndex % 2 == 0) stackIndex * 1.6f else -stackIndex * 1.6f

            // Keep animation state attached to the actual job/student. Without
            // this key, the next top card inherits the dismissed card's final
            // off-screen offset and only the translucent cards behind it remain.
            key(item) {
                if (isTop) {
                    DraggableTopCard(
                        pendingSwipe = pendingSwipe,
                        onPendingSwipeHandled = onPendingSwipeHandled,
                        onSwiped = { direction -> onSwiped(item, direction) }
                    ) {
                        cardContent(item)
                    }
                } else {
                    Box(
                        Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationY = yOffset
                                rotationZ = tilt
                                alpha = 1f - (stackIndex * 0.22f)
                            }
                            .clip(RoundedCornerShape(28.dp))
                    ) {
                        cardContent(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableTopCard(
    pendingSwipe: SwipeDirection? = null,
    onPendingSwipeHandled: () -> Unit = {},
    onSwiped: (SwipeDirection) -> Unit,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val swipeThreshold = 260f

    val rotation = (offsetX.value / 34f).coerceIn(-18f, 18f)
    val likeAlpha = (offsetX.value / swipeThreshold).coerceIn(0f, 1f)
    val passAlpha = ((-offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
    val liftScale = 1f + (kotlin.math.max(likeAlpha, passAlpha) * 0.02f)

    // A tap on the pass/like buttons sets pendingSwipe from outside — animate the
    // fling exactly like a real drag release, so button and gesture feel identical.
    LaunchedEffect(pendingSwipe) {
        if (pendingSwipe == null) return@LaunchedEffect
        when (pendingSwipe) {
            SwipeDirection.RIGHT -> offsetX.animateTo(1600f, tween(300))
            SwipeDirection.LEFT -> offsetX.animateTo(-1600f, tween(300))
        }
        // Clear the one-shot command before changing the deck so the new top
        // card can never consume the previous card's swipe request.
        onPendingSwipeHandled()
        onSwiped(pendingSwipe)
    }

    Box(
        Modifier
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = rotation
                scaleX = liftScale
                scaleY = liftScale
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX.value > swipeThreshold -> {
                                    offsetX.animateTo(1600f, tween(300))
                                    onSwiped(SwipeDirection.RIGHT)
                                }
                                offsetX.value < -swipeThreshold -> {
                                    offsetX.animateTo(-1600f, tween(300))
                                    onSwiped(SwipeDirection.LEFT)
                                }
                                else -> {
                                    offsetX.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                    )
                                    offsetY.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                    )
                                }
                            }
                        }
                    },
                    onDragCancel = {},
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y * 0.4f)
                        }
                    }
                )
            }
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = 2.5.dp,
                color = if (likeAlpha > passAlpha) Mint40.copy(alpha = likeAlpha) else Coral.copy(alpha = passAlpha),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        content()

        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .graphicsLayer {
                    alpha = likeAlpha
                    rotationZ = -12f
                }
                .border(2.dp, Mint40, RoundedCornerShape(10.dp))
        ) {
            SwipeStampText("MATCH", Mint40)
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .graphicsLayer {
                    alpha = passAlpha
                    rotationZ = 12f
                }
                .border(2.dp, Coral, RoundedCornerShape(10.dp))
        ) {
            SwipeStampText("PASS", Coral)
        }
    }
}

@Composable
private fun SwipeStampText(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Black,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

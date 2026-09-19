// app/src/main/java/com/corbymaupin/jobsitespanish/ui/components/FlipCard.kt
package com.corbymaupin.jobsitespanish.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteCard

/** Tunables for [FlipCard]. */
object FlipCardDefaults {
    /** Full front-to-back flip. */
    const val DurationMillis: Int = 450


    /**
     * Camera distance per unit of screen density (equivalent to 12.dp.toPx()). Far enough back
     * that a full-width card never distorts at the near edge, close enough that the turn still
     * reads as 3D.
     */
    const val CameraDistancePerDensity: Float = 12f

    val Shape: Shape = RoundedCornerShape(16.dp)
}

/**
 * A two-faced card that turns around its vertical axis.
 *
 * Size: the card's size comes only from [modifier] (give it a fixed height). Both faces fill
 * that same box and never measure the card, so flipping cannot resize it. Faces that might
 * overflow should scroll internally.
 *
 * Motion: [flipped] false -> true animates 0 -> 180 degrees over [durationMillis]. Past 90
 * degrees the back face is shown and the layer is counter-rotated (rotation - 180) so the back
 * text is not mirrored. [flipped] true -> false, or a new [resetKey] (the next card), snaps
 * straight to the front with no reverse animation.
 *
 * Input: tapping the front calls [onClick]. The back is not clickable. Grade buttons belong
 * outside this composable so they never rotate.
 */
@Composable
fun FlipCard(
    flipped: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    resetKey: Any? = null,
    shape: Shape = FlipCardDefaults.Shape,

    containerColor: Color = JobsiteCard,
    durationMillis: Int = FlipCardDefaults.DurationMillis,
    onClickLabel: String? = null,
    front: @Composable BoxScope.() -> Unit,
    back: @Composable BoxScope.() -> Unit
) {
    // A new card (resetKey) gets a fresh Animatable that starts on the correct face, unanimated.
    val rotation = remember(resetKey) { Animatable(if (flipped) 180f else 0f) }

    LaunchedEffect(rotation, flipped) {
        if (flipped) {
            rotation.animateTo(
                targetValue = 180f,
                animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
            )
        } else {
            rotation.snapTo(0f)
        }
    }

    // Recomposes only when the visible face changes, not on every animation frame.
    val showBack by remember(rotation) { derivedStateOf { rotation.value > 90f } }

    val clickModifier = if (!flipped) {
        Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .graphicsLayer {

                val degrees = rotation.value
                // Front: 0..90. Back: counter-rotated so it lands at 0 (readable) at 180.
                rotationY = if (degrees > 90f) degrees - 180f else degrees
                cameraDistance = FlipCardDefaults.CameraDistancePerDensity * density
                transformOrigin = TransformOrigin.Center
            }
            .clip(shape)
            .background(containerColor)
            .then(clickModifier)
    ) {
        if (showBack) {
            Box(Modifier.fillMaxSize(), content = back)
        } else {
            Box(Modifier.fillMaxSize(), content = front)
        }
    }
}


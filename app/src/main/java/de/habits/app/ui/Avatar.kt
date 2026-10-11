package de.habits.app.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.habits.core.AvatarPhase
import de.habits.core.BodyShape
import de.habits.core.SPRITE_H
import de.habits.core.SPRITE_SCALE
import de.habits.core.SPRITE_W
import de.habits.core.avatarPhase
import de.habits.core.avatarSparkles
import de.habits.core.avatarSprite
import de.habits.core.sparkleSpots
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/*
 * Pixel-art avatar. The pixels come from core (avatarSprite); this only blits them without
 * smoothing on a fixed "screen" colour, so the figure looks the same in light and dark mode.
 * Tapping it plays a little jump with squash & stretch and a burst in the colour of the phase
 * (the wrestling goose honks).
 */
private val Screen = Color(0xFFDCE6F2)
private val Ink = Color(0xFF2A1E38)
private val Gold = Color(0xFFF2C94C)

private fun burstColor(phase: AvatarPhase) = when (phase) {
    AvatarPhase.HUMAN -> Color(0xFFFFD23F)
    AvatarPhase.MOSQUITO -> Color(0xFFB454F0)
    AvatarPhase.GOOSE -> Color(0xFFF08A24)
}

/** Jump offset (fraction of the height), x/y scale and rotation for animation time [t] in 0..1. */
private class Pose(val lift: Float = 0f, val sx: Float = 1f, val sy: Float = 1f, val degrees: Float = 0f)

private fun pose(t: Float): Pose = when {
    t <= 0f || t >= 1f -> Pose()
    t < 0.15f -> (t / 0.15f).let { Pose(sx = 1 + 0.12f * it, sy = 1 - 0.15f * it) } // crouch
    t < 0.7f -> ((t - 0.15f) / 0.55f).let { u ->
        Pose(lift = sin(PI * u).toFloat() * 0.2f, sx = 1 - 0.06f * (1 - u), sy = 1 + 0.1f * (1 - u), degrees = sin(2 * PI * u).toFloat() * 8f)
    }
    t < 0.85f -> sin(PI * (t - 0.7f) / 0.15f).toFloat().let { Pose(sx = 1 + 0.12f * it, sy = 1 - 0.14f * it) } // land
    else -> Pose()
}

@Composable
fun Avatar(level: Int, shape: BodyShape, modifier: Modifier = Modifier, animate: Boolean = true) {
    val image = remember(level, shape) {
        val s = avatarSprite(level, shape)
        Bitmap.createBitmap(s.argb, s.width, s.height, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    val phase = avatarPhase(level)
    val sparkles = minOf(avatarSparkles(level), sparkleSpots.size)
    val twinkle = if (animate && sparkles > 0 && !LocalInspectionMode.current) {
        rememberInfiniteTransition(label = "twinkle").animateFloat(
            initialValue = 0.3f, targetValue = 1f, label = "alpha",
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        ).value
    } else 1f
    val jump = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val text = rememberTextMeasurer()

    Canvas(
        modifier
            .aspectRatio(SPRITE_W.toFloat() / SPRITE_H)
            .clip(RoundedCornerShape(12))
            .background(Screen)
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                scope.launch {
                    jump.snapTo(0f)
                    jump.animateTo(1f, tween(900, easing = LinearEasing))
                }
            },
    ) {
        // Whole-number scale keeps the pixels even; small avatars may scale fractionally.
        val raw = minOf(size.width / SPRITE_W, size.height / SPRITE_H)
        val scale = if (raw >= 2) floor(raw) else raw
        val w = scale * SPRITE_W
        val h = scale * SPRITE_H
        val origin = Offset((size.width - w) / 2, (size.height - h) / 2)
        val t = jump.value
        val p = pose(t)

        withTransform({
            translate(top = -p.lift * h)
            scale(p.sx, p.sy, pivot = Offset(size.width / 2, origin.y + h * 0.9f))
            rotate(p.degrees, pivot = Offset(size.width / 2, origin.y + h * 0.5f))
        }) {
            drawImage(
                image,
                dstOffset = IntOffset(origin.x.toInt(), origin.y.toInt()),
                dstSize = IntSize(w.toInt(), h.toInt()),
                filterQuality = FilterQuality.None,
            )
        }

        val pixel = scale * SPRITE_SCALE
        repeat(sparkles) { i ->
            val (x, y) = sparkleSpots[i]
            sparkle(Offset(origin.x + x * scale, origin.y + y * scale), pixel, if (i % 2 == 0) twinkle else 1.3f - twinkle)
        }

        if (t > 0.15f && t < 1f) {
            // Burst of pixels flying out from the figure.
            val u = (t - 0.15f) / 0.85f
            val centre = Offset(size.width / 2, origin.y + h * 0.45f)
            val color = burstColor(phase).copy(alpha = 1 - u)
            repeat(10) { i ->
                val a = 2 * PI * i / 10
                val r = u * 0.48f * minOf(w, h)
                drawRect(color, Offset(centre.x + cos(a).toFloat() * r, centre.y + sin(a).toFloat() * r), Size(pixel * 1.5f, pixel * 1.5f))
            }
            if (phase == AvatarPhase.GOOSE && t < 0.9f) {
                val layout = text.measure("HONK!", TextStyle(color = Ink, fontWeight = FontWeight.Black, fontSize = (h * 0.07f).toSp()))
                val pad = pixel * 2
                val topLeft = Offset(size.width - layout.size.width - pad * 3, origin.y + pad * 2)
                drawRoundRect(Color.White, topLeft - Offset(pad, pad / 2), Size(layout.size.width + 2 * pad, layout.size.height + pad), CornerRadius(pad * 2))
                drawRoundRect(Gold, topLeft - Offset(pad, pad / 2), Size(layout.size.width + 2 * pad, layout.size.height + pad), CornerRadius(pad * 2), style = androidx.compose.ui.graphics.drawscope.Stroke(pixel))
                drawText(layout, topLeft = topLeft)
            }
        }
    }
}

/** Four-pointed star: bright core, one-pixel arms (pixel = one figure unit on screen). */
private fun DrawScope.sparkle(at: Offset, pixel: Float, alpha: Float) {
    val a = alpha.coerceIn(0f, 1f)
    fun px(dx: Int, dy: Int, c: Color) = drawRect(c.copy(alpha = a), Offset(at.x + dx * pixel, at.y + dy * pixel), Size(pixel, pixel))
    px(0, 0, Color.White)
    px(-1, 0, Gold); px(1, 0, Gold); px(0, -1, Gold); px(0, 1, Gold)
}

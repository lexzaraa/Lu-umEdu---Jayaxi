package mx.aula360.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Pantalla de bienvenida LUUM EDU. Se dibuja con Compose y no requiere PNG externos. */
@Composable
internal fun IntroLuumEdu(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 4_000, easing = LinearEasing)
        )
        fade.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    val p = progress.value
    val pyramidReveal = ((p - 0.08f) / 0.40f).coerceIn(0f, 1f)
    val titleAlpha = ((p - 0.40f) / 0.13f).coerceIn(0f, 1f)
    val taglineAlpha = ((p - 0.55f) / 0.15f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(fade.value)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B1E29),
                        Color(0xFF080E19),
                        Color(0xFF0B1624),
                        Color(0xFF080E19)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.47f)
            val glowRadius = size.minDimension * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3322C5B5), Color(0x110F766E), Color.Transparent),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )

            val stars = listOf(
                0.13f to 0.23f, 0.22f to 0.38f, 0.79f to 0.22f,
                0.86f to 0.43f, 0.11f to 0.65f, 0.91f to 0.72f,
                0.30f to 0.78f, 0.70f to 0.82f, 0.50f to 0.16f,
                0.05f to 0.48f, 0.95f to 0.30f, 0.42f to 0.88f
            )
            stars.forEachIndexed { index, point ->
                val pulse = (0.24f + 0.52f * ((sin(p * 9f + index * 1.7f) + 1f) / 2f))
                drawCircle(
                    color = Color(0xFF5EEAD4).copy(alpha = pulse * (0.35f + p * 0.45f)),
                    radius = (1.1f + (index % 3) * 0.55f).dp.toPx(),
                    center = Offset(size.width * point.first, size.height * point.second)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(Modifier.size(width = 236.dp, height = 174.dp)) {
                val cx = size.width / 2f
                val apexY = 18.dp.toPx()
                val baseY = 139.dp.toPx()
                val halfWidth = 62.dp.toPx()
                val left = Offset(cx - halfWidth, baseY)
                val middle = Offset(cx, baseY)
                val right = Offset(cx + halfWidth, baseY)
                val apex = Offset(cx, apexY)
                val centerY = 78.dp.toPx()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3D14B8A6), Color.Transparent),
                        center = Offset(cx, centerY),
                        radius = 100.dp.toPx()
                    ),
                    radius = 100.dp.toPx(),
                    center = Offset(cx, centerY)
                )

                val pyramid = Path().apply {
                    moveTo(apex.x, apex.y)
                    lineTo(left.x, left.y)
                    lineTo(right.x, right.y)
                    close()
                }
                val leftFace = Path().apply {
                    moveTo(apex.x, apex.y)
                    lineTo(left.x, left.y)
                    lineTo(middle.x, middle.y)
                    close()
                }
                val rightFace = Path().apply {
                    moveTo(apex.x, apex.y)
                    lineTo(middle.x, middle.y)
                    lineTo(right.x, right.y)
                    close()
                }

                val revealTop = baseY - (baseY - apexY) * pyramidReveal
                clipRect(
                    left = cx - halfWidth - 3.dp.toPx(),
                    top = revealTop,
                    right = cx + halfWidth + 3.dp.toPx(),
                    bottom = baseY + 3.dp.toPx()
                ) {
                    drawPath(leftFace, Color(0xFF019485).copy(alpha = 0.98f))
                    drawPath(rightFace, Color(0xFF02A215).copy(alpha = 0.94f))
                    drawPath(
                        pyramid,
                        Color(0xFF5EEAD4).copy(alpha = 0.85f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                val shine = ((p - 0.57f) / 0.22f).coerceIn(0f, 1f)
                if (shine > 0f && shine < 1f) {
                    val x = cx - halfWidth - 24.dp.toPx() +
                        (halfWidth * 2f + 48.dp.toPx()) * shine
                    clipPath(pyramid) {
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.9f),
                                    Color.Transparent
                                ),
                                start = Offset(x - 17.dp.toPx(), apexY),
                                end = Offset(x + 17.dp.toPx(), baseY)
                            ),
                            start = Offset(x - 17.dp.toPx(), apexY),
                            end = Offset(x + 17.dp.toPx(), baseY),
                            strokeWidth = 8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                val orbit = ((p - 0.10f) / 0.49f).coerceIn(0f, 1f)
                if (orbit > 0f && orbit < 1f) {
                    val angle = orbit * (PI.toFloat() * 3.1f) - PI.toFloat() / 2f
                    val rx = 91.dp.toPx()
                    val ry = 69.dp.toPx()
                    val comet = Offset(cx + cos(angle) * rx, centerY + sin(angle) * ry)
                    val tail = Offset(
                        cx + cos(angle - 0.28f) * rx,
                        centerY + sin(angle - 0.28f) * ry
                    )
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color(0xFF5EEAD4)),
                            start = tail,
                            end = comet
                        ),
                        start = tail,
                        end = comet,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFF5EEAD4), Color.Transparent),
                            center = comet,
                            radius = 11.dp.toPx()
                        ),
                        radius = 11.dp.toPx(),
                        center = comet
                    )
                    drawCircle(Color.White, radius = 2.7.dp.toPx(), center = comet)
                }
            }

            Spacer(Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(titleAlpha)
            ) {
                Text(
                    text = "LUUM",
                    color = Color(0xFF5EEAD4),
                    fontSize = 35.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
                Text(
                    text = " EDU",
                    color = Color(0xFF22C55E),
                    fontSize = 35.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = "GEOSPATIAL EDUCATION INTELLIGENCE",
                color = Color(0xFFA5B3C7),
                fontSize = 9.sp,
                letterSpacing = 1.35.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(taglineAlpha)
            )
        }
    }
}

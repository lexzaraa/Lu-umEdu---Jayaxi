package mx.luumedu.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mx.luumedu.app.ui.theme.LuumAzulProfundo
import mx.luumedu.app.ui.theme.LuumFondo
import mx.luumedu.app.ui.theme.LuumTurquesa
import mx.luumedu.app.ui.theme.LuumTurquesaClaro
import mx.luumedu.app.ui.theme.LuumVerde
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// =====================================================
// INTRO ANIMADA DE LUUM EDU
// Secuencia (segundos):
//   0.0 - 1.0  Brillo ambiental y polvo de luz
//   0.2 - 2.4  Cometa de luz orbitando alrededor del logo
//   0.5 - 1.8  La pirámide "se construye" de abajo hacia arriba
//   1.5 - 2.5  El nombre LuumEdu se revela de izquierda a derecha
//   2.3 - 3.0  Aparece el lema
//   2.6 - 3.6  Destello que recorre todo el logo
//   3.6 - 4.4  Pausa y salida con desvanecimiento
// Tocar la pantalla omite la intro.
// =====================================================

private const val DURACION_S = 4.4f

private val FondoIntro = LuumFondo
private val Turquesa = LuumTurquesa
private val TurquesaClaro = LuumTurquesaClaro
private val VerdeLuum = LuumVerde

private data class Particula(
    val x: Float,
    val y: Float,
    val velocidad: Float,
    val radio: Float,
    val fase: Float,
    val verde: Boolean
)

/** Progreso 0..1 con suavizado dentro del tramo [inicioS, finS] (en segundos). */
private fun tramo(p: Float, inicioS: Float, finS: Float): Float {
    val x = ((p * DURACION_S - inicioS) / (finS - inicioS)).coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(x)
}

/** Progreso 0..1 lineal dentro del tramo [inicioS, finS] (en segundos). */
private fun tramoLineal(p: Float, inicioS: Float, finS: Float): Float {
    return ((p * DURACION_S - inicioS) / (finS - inicioS)).coerceIn(0f, 1f)
}

@Composable
fun IntroLuumEdu(alTerminar: () -> Unit) {
    val progreso = remember { Animatable(0f) }
    val salida = remember { Animatable(0f) }
    var saltado by remember { mutableStateOf(false) }

    LaunchedEffect(saltado) {
        if (!saltado) {
            progreso.animateTo(
                1f,
                tween((DURACION_S * 1000).toInt(), easing = LinearEasing)
            )
        }
        salida.animateTo(1f, tween(if (saltado) 250 else 450, easing = LinearEasing))
        alTerminar()
    }

    val p = progreso.value
    val s = salida.value

    val particulas = remember {
        val azar = Random(11)
        List(42) {
            Particula(
                x = azar.nextFloat(),
                y = azar.nextFloat(),
                velocidad = 0.05f + azar.nextFloat() * 0.18f,
                radio = 0.8f + azar.nextFloat() * 1.9f,
                fase = azar.nextFloat(),
                verde = azar.nextFloat() < 0.3f
            )
        }
    }

    // Tiempos de cada elemento
    val revelarIcono = tramo(p, 0.5f, 1.8f)
    val revelarNombre = tramo(p, 1.5f, 2.5f)
    val aparecerLema = tramo(p, 2.3f, 3.0f)
    val destello = tramoLineal(p, 2.6f, 3.6f)
    val escalaBase = 0.90f + 0.10f * tramo(p, 0.3f, 3.2f)
    val escala = escalaBase + 0.08f * s

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoIntro)
            .graphicsLayer { alpha = 1f - s }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { saltado = true },
        contentAlignment = Alignment.Center
    ) {
        // ---- Fondo: brillo ambiental, polvo de luz y cometa orbital ----
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val brillo = tramo(p, 0f, 1.0f)

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Turquesa.copy(alpha = 0.30f * brillo), Color.Transparent),
                    center = Offset(w / 2f, h * 0.46f),
                    radius = max(w, h) * 0.62f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(LuumAzulProfundo.copy(alpha = 0.16f * brillo), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.85f),
                    radius = max(w, h) * 0.55f
                )
            )

            // Polvo de luz que asciende
            val visible = tramo(p, 0.1f, 1.2f) * (1f - tramo(p, 3.6f, 4.4f))
            particulas.forEach { q ->
                val avance = q.y - p * q.velocidad * 4f
                val yy = ((avance % 1f) + 1f) % 1f
                val titilar = 0.35f + 0.65f * (0.5f + 0.5f * sin(2f * PI.toFloat() * (p * 2f + q.fase)))
                val base = if (q.verde) VerdeLuum else Color.White
                drawCircle(
                    color = base.copy(alpha = (0.55f * titilar * visible).coerceIn(0f, 1f)),
                    radius = q.radio.dp.toPx(),
                    center = Offset(q.x * w, yy * h)
                )
            }

            // Cometa de luz en órbita elíptica
            val orbita = tramoLineal(p, 0.2f, 2.4f)
            if (orbita > 0f && orbita < 1f) {
                val alfa = sin(PI.toFloat() * orbita)
                val rx = min(w * 0.47f, 270.dp.toPx())
                val ry = rx * 0.58f
                val centro = Offset(w / 2f, h * 0.47f)

                drawOval(
                    color = Turquesa.copy(alpha = 0.10f * alfa),
                    topLeft = Offset(centro.x - rx, centro.y - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = 1.dp.toPx())
                )

                val anguloCabeza = (-PI / 2.0).toFloat() +
                        2f * PI.toFloat() * 1.4f * FastOutSlowInEasing.transform(orbita)
                val pasos = 60
                for (i in 0 until pasos) {
                    val fraccion = i / pasos.toFloat()
                    val angulo = anguloCabeza - fraccion * 0.9f
                    val punto = Offset(
                        centro.x + rx * cos(angulo),
                        centro.y + ry * sin(angulo)
                    )
                    val color = if (i < 6) Color.White else TurquesaClaro
                    drawCircle(
                        color = color.copy(
                            alpha = (alfa * (1f - fraccion) * (1f - fraccion)).coerceIn(0f, 1f)
                        ),
                        radius = (3.4f * (1f - fraccion) + 0.6f).dp.toPx(),
                        center = punto
                    )
                }
            }
        }

        // ---- Logo por capas: pirámide, nombre y lema ----
        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .widthIn(max = 460.dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    scaleX = escala
                    scaleY = escala
                }
                .drawWithContent {
                    drawContent()
                    // Destello diagonal que recorre el logo (solo sobre las partes visibles)
                    if (destello > 0f && destello < 1f) {
                        val ancho = size.width
                        val x = -0.4f * ancho + destello * 1.8f * ancho
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xCCFFFFFF),
                                    Color.Transparent
                                ),
                                start = Offset(x - 0.22f * ancho, 0f),
                                end = Offset(x + 0.22f * ancho, size.height * 0.5f)
                            ),
                            blendMode = BlendMode.SrcAtop
                        )
                    }
                }
        ) {
            // Pirámide: se revela de abajo hacia arriba con una línea de luz
            Image(
                painter = painterResource(R.drawable.intro_logo_icono),
                contentDescription = "LUUM EDU",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        val corte = size.height * (1f - revelarIcono)
                        clipRect(top = corte) {
                            this@drawWithContent.drawContent()
                        }
                        if (revelarIcono > 0.01f && revelarIcono < 0.99f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        TurquesaClaro.copy(alpha = 0.85f),
                                        Color.Transparent
                                    ),
                                    startY = corte - 36f,
                                    endY = corte + 36f
                                )
                            )
                        }
                    }
            )
            // Nombre: se revela de izquierda a derecha mientras sube suavemente
            Image(
                painter = painterResource(R.drawable.intro_logo_nombre),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = revelarNombre
                        translationY = (1f - revelarNombre) * 22.dp.toPx()
                    }
                    .drawWithContent {
                        clipRect(right = size.width * revelarNombre) {
                            this@drawWithContent.drawContent()
                        }
                    }
            )
            // Lema: aparece con un leve desplazamiento
            Image(
                painter = painterResource(R.drawable.intro_logo_lema),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = aparecerLema
                        translationY = (1f - aparecerLema) * 14.dp.toPx()
                    }
            )
        }
    }
}

package mx.aula360.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode

// =====================================================
// 1. IDENTIDAD VISUAL - DARK GLASS
// =====================================================

// Paleta principal
private val LuumTurquesa = Color(0xFF14B8A6)        // Botones principales, acciones y selecciones
private val LuumTurquesaClaro = Color(0xFF5EEAD4)   // Títulos, textos de acento y detalles
private val LuumTurquesaLogo = Color(0xFF019485)    // Color de la pirámide del logotipo
private val LuumVerde = Color(0xFF22C55E)           // Acentos secundarios
private val LuumVerdeLogo = Color(0xFF02A215)       // Verde de EDU en el logotipo
private val LuumAmbar = Color(0xFFFFB74D)           // Avisos y estados pendientes
private val LuumAzulMarino = Color(0xFF002B71)      // Identidad azul del logotipo

// Fondos y superficies
private val LuumFondo = Color(0xFF080E19)
private val LuumFondoGradienteA = Color(0xFF0B1E29)
private val LuumFondoGradienteB = Color(0xFF0B1624)
private val LuumSuperficie = Color(0xFF111D2B)
private val LuumSuperficieAlta = Color(0xFF182A3A)
private val LuumSuperficieNavegacion = Color(0xEE0E1926)

// Textos y estados
private val LuumTexto = Color(0xFFE7F0FB)
private val LuumGris = Color(0xFFA5B3C7)
private val LuumExito = Color(0xFF4ADE80)
private val LuumError = Color(0xFFFF8A80)

// Efectos de transparencia
private val LuumBordeCristal = Color(0x21FFFFFF)
private val LuumBordeTurquesa = Color(0x5514B8A6)
private val LuumContorno = Color(0x4D5EEAD4)

// Alias usados en toda la interfaz
private val Azul = LuumTurquesaClaro
private val AzulClaro = LuumAzulMarino
private val Fondo = LuumFondo
private val Superficie = LuumSuperficie
private val Texto = LuumTexto
private val Gris = LuumGris
private val BordeGlass = LuumBordeCristal

// Datos de contacto del desarrollador de LuumEdu.
private const val NOMBRE_DESARROLLADOR = "Josue Eduardo Cancino Balam"
private const val CORREO_DESARROLLADOR = "cancinojosue133@gmail.com"

// =====================================================
// 2. CONFIGURACIÓN GEOGRÁFICA
// =====================================================

private const val MAPA_ESTILO = "https://tiles.openfreemap.org/styles/dark"
private val CANCUN_REFERENCIA = LatLng(21.1619, -86.8515)
private const val RADIO_OPINION_METROS = 150f
private const val PRECISION_MAXIMA_METROS = 30f

// =====================================================
// 3. MODELOS
// =====================================================

private data class Pestana(val nombre: String, val simbolo: String)

data class Observacion(
    val id: String,
    val titulo: String,
    val categoria: String,
    val ubicacion: String,
    val descripcion: String,
    val estado: String,
    val turno: String,
    val fechaMillis: Long,
    val latitud: Double?,
    val longitud: Double?,
    val precisionMetros: Double?,
    val autorUid: String
)

data class NotificacionAula(
    val id: String,
    val titulo: String,
    val mensaje: String,
    val reporteId: String,
    val estadoReporte: String,
    val fechaMillis: Long,
    val leida: Boolean,
    val tipo: String = "report",
    val encuestaId: String = ""
)

data class Institucion(
    val id: String,
    val nombre: String,
    val tipo: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val estado: String = "active"
)

data class PerfilUsuario(
    val uid: String,
    val nombre: String,
    val correo: String,
    val rol: String,
    val estado: String,
    val institucionId: String?,
    val rolSolicitado: String
)


data class PreguntaEncuesta(
    val id: String,
    val texto: String,
    val tipo: String,
    val opciones: List<String>
)

data class Encuesta(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val categoria: String,
    val institucionId: String,
    val creadorUid: String,
    val estado: String,
    val cantidadRespuestas: Int,
    val fechaMillis: Long,
    val fechaPublicacionMillis: Long,
    val preguntas: List<PreguntaEncuesta>,
    val versionEstadisticas: Long
)

private class BorradorPregunta(
    val id: String = java.util.UUID.randomUUID().toString(),
    textoInicial: String = "",
    tipoInicial: String = "single_choice",
    opcionesIniciales: String = "Sí,No,No estoy seguro"
) {
    var texto by mutableStateOf(textoInicial)
    var tipo by mutableStateOf(tipoInicial)
    var opcionesTexto by mutableStateOf(opcionesIniciales)
}

private fun DocumentSnapshot.aInstitucion(): Institucion? {
    val nombre = getString("name") ?: getString("nombre") ?: return null
    val tipo = getString("type") ?: getString("tipo") ?: "Institución educativa"
    val direccion = getString("address") ?: getString("direccion") ?: ""
    val latitud = getDouble("latitude") ?: getDouble("latitud") ?: return null
    val longitud = getDouble("longitude") ?: getDouble("longitud") ?: return null

    if (!latitud.isFinite() || !longitud.isFinite()) return null
    if (latitud !in -90.0..90.0 || longitud !in -180.0..180.0) return null

    return Institucion(
        id = id,
        nombre = nombre,
        tipo = tipo,
        direccion = direccion,
        latitud = latitud,
        longitud = longitud,
        estado = getString("status") ?: "active"
    )
}

private fun DocumentSnapshot.aPerfil(): PerfilUsuario? {
    val uidDoc = getString("uid") ?: id
    return PerfilUsuario(
        uid = uidDoc,
        nombre = getString("displayName") ?: "Usuario LuumEdu",
        correo = getString("email") ?: "",
        rol = getString("role") ?: "pending",
        estado = getString("status") ?: "pending",
        institucionId = getString("institutionId"),
        rolSolicitado = getString("requestedRole") ?: "student"
    )
}

private fun DocumentSnapshot.aObservacion(): Observacion? {
    val titulo = getString("title") ?: return null
    val categoria = getString("category") ?: "General"
    val ubicacion = getString("locationLabel") ?: "Sin referencia de espacio"
    val descripcion = getString("description") ?: ""
    val fecha = getTimestamp("createdAt")?.toDate()?.time ?: 0L

    return Observacion(
        id = id,
        titulo = titulo,
        categoria = categoria,
        ubicacion = ubicacion,
        descripcion = descripcion,
        estado = getString("status") ?: "new",
        turno = getString("turn") ?: "Sin turno",
        fechaMillis = fecha,
        latitud = getDouble("latitude")?.takeIf { it.isFinite() && it in -90.0..90.0 },
        longitud = getDouble("longitude")?.takeIf { it.isFinite() && it in -180.0..180.0 },
        precisionMetros = getDouble("gpsAccuracyMeters"),
        autorUid = getString("authorUid") ?: ""
    )
}

private fun DocumentSnapshot.aNotificacion(): NotificacionAula? {
    val titulo = getString("title") ?: return null
    val mensaje = getString("body") ?: ""
    return NotificacionAula(
        id = id,
        titulo = titulo,
        mensaje = mensaje,
        reporteId = getString("observationId") ?: "",
        estadoReporte = getString("status") ?: "",
        fechaMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        leida = getBoolean("read") ?: false,
        tipo = getString("type") ?: "report",
        encuestaId = getString("surveyId") ?: ""
    )
}


private fun DocumentSnapshot.aEncuesta(): Encuesta? {
    val titulo = getString("title") ?: return null
    val preguntasRaw = get("questions") as? List<*> ?: emptyList<Any>()
    val preguntas = preguntasRaw.mapIndexedNotNull { index, elemento ->
        val mapa = elemento as? Map<*, *> ?: return@mapIndexedNotNull null
        val texto = mapa["text"] as? String ?: return@mapIndexedNotNull null
        val tipo = mapa["type"] as? String ?: "single_choice"
        val opciones = (mapa["options"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
        PreguntaEncuesta(
            id = mapa["id"] as? String ?: "pregunta_$index",
            texto = texto,
            tipo = tipo,
            opciones = opciones
        )
    }
    if (preguntas.isEmpty()) return null

    return Encuesta(
        id = id,
        titulo = titulo,
        descripcion = getString("description") ?: "",
        categoria = getString("category") ?: "General",
        institucionId = getString("institutionId") ?: "",
        creadorUid = getString("createdByUid") ?: "",
        estado = getString("status") ?: "published",
        cantidadRespuestas = (getLong("responseCount") ?: 0L).toInt(),
        fechaMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        fechaPublicacionMillis = getTimestamp("publishedAt")?.toDate()?.time ?: 0L,
        preguntas = preguntas,
        versionEstadisticas = getLong("statisticsVersion") ?: 0L
    )
}

private data class EstadisticaPregunta(
    val idPregunta: String,
    val indice: Int,
    val tipo: String,
    val total: Long,
    val conteosOpciones: Map<String, Long>
)

private fun DocumentSnapshot.aEstadisticaPregunta(): EstadisticaPregunta? {
    val idPregunta = getString("questionId") ?: return null
    val indice = getLong("questionIndex")?.toInt() ?: return null
    val tipo = getString("type") ?: return null
    val conteos = (get("optionCounts") as? Map<*, *>)
        ?.mapNotNull { (clave, valor) ->
            val nombre = clave as? String ?: return@mapNotNull null
            val cantidad = (valor as? Number)?.toLong() ?: return@mapNotNull null
            nombre to cantidad
        }
        ?.toMap()
        ?: emptyMap()
    return EstadisticaPregunta(
        idPregunta = idPregunta,
        indice = indice,
        tipo = tipo,
        total = getLong("totalCount") ?: 0L,
        conteosOpciones = conteos
    )
}

private fun etiquetaEstadoEncuesta(estado: String): String = when (estado) {
    "published" -> "Publicada"
    "closed" -> "Cerrada"
    "draft" -> "Borrador"
    else -> "No disponible"
}

private fun etiquetaEstadoReporte(estado: String): String = when (estado) {
    "new" -> "Nuevo"
    "in_review" -> "En revisión"
    "in_progress" -> "En atención"
    "resolved" -> "Resuelto"
    "verified" -> "Mejora verificada"
    "rejected" -> "No procedente"
    else -> "Estado pendiente"
}

private fun etiquetaTurno(turno: String): String = when (turno.lowercase(Locale.ROOT)) {
    "morning", "matutino" -> "Matutino"
    "afternoon", "vespertino" -> "Vespertino"
    "night", "nocturno" -> "Nocturno"
    else -> "Sin turno"
}

private fun formatoFechaReporte(fechaMillis: Long): String {
    if (fechaMillis <= 0L) return "Fecha pendiente"
    return SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(Date(fechaMillis))
}

private fun etiquetaRol(rol: String): String = when (rol) {
    "director" -> "Director"
    "student" -> "Estudiante"
    "parent" -> "Padre de familia"
    "teacher" -> "Docente"
    else -> "Sin autorización"
}

// =====================================================
// 4. UBICACIÓN ACTUAL
// =====================================================

@SuppressLint("MissingPermission")
private fun obtenerUbicacionActual(
    context: Context,
    resultado: (Location?) -> Unit
) {
    val cliente = LocationServices.getFusedLocationProviderClient(context)
    val cancelacion = CancellationTokenSource()

    try {
        cliente.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancelacion.token
        )
            .addOnSuccessListener { ubicacion -> resultado(ubicacion) }
            .addOnFailureListener { resultado(null) }
    } catch (_: SecurityException) {
        resultado(null)
    }
}

private fun distanciaMetros(
    latitud1: Double,
    longitud1: Double,
    latitud2: Double,
    longitud2: Double
): Float {
    val resultados = FloatArray(1)
    Location.distanceBetween(
        latitud1,
        longitud1,
        latitud2,
        longitud2,
        resultados
    )
    return resultados[0]
}

// =====================================================
// 5. ACTIVIDAD PRINCIPAL
// =====================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Protección de Firebase AI Logic
        AppCheckInitializer.install(this)

        MapLibre.getInstance(applicationContext)
        setContent { Aula360App() }
    }
}

// =====================================================
// 5.1 INTRO ANIMADA - LUUMEDU (estilo Disney+)
// =====================================================

@Composable
private fun IntroLogoLuumEdu(onFinished: () -> Unit) {
    val alfaLogo = remember { Animatable(0f) }
    val escala = remember { Animatable(0.85f) }
    val brillo = remember { Animatable(-0.5f) }
    val alfaPantalla = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1) Aparece el logo con un leve acercamiento
        coroutineScope {
            launch { alfaLogo.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            launch { escala.animateTo(1f, tween(1800, easing = FastOutSlowInEasing)) }
            // 2) Un destello de luz cruza el logo
            launch {
                delay(700)
                brillo.animateTo(1.5f, tween(1100, easing = FastOutSlowInEasing))
            }
        }
        // 3) Pausa y desvanecimiento
        delay(500)
        alfaPantalla.animateTo(0f, tween(550))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alfaPantalla.value)
            .background(Color(0xFFFAFCFC)) // mismo fondo del logo
            .pointerInput(Unit) { },        // bloquea toques mientras se muestra
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_luum),
            contentDescription = "LuumEdu - Aprender desde nuestro entorno",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .graphicsLayer {
                    alpha = alfaLogo.value
                    scaleX = escala.value
                    scaleY = escala.value
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    // Destello diagonal que solo se pinta sobre el logo
                    val ancho = size.width
                    val inicio = brillo.value * ancho
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.75f),
                                Color.Transparent
                            ),
                            start = Offset(inicio - ancho * 0.25f, 0f),
                            end = Offset(inicio + ancho * 0.25f, size.height * 0.4f)
                        ),
                        blendMode = BlendMode.SrcAtop
                    )
                }
        )
    }
}

// =====================================================
// 6. APLICACIÓN Y CONEXIÓN CON FIREBASE
// =====================================================

@Composable
fun Aula360App() {
    val context = LocalContext.current.applicationContext
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var usuarioFirebase by remember { mutableStateOf(auth.currentUser) }
    var perfil by remember { mutableStateOf<PerfilUsuario?>(null) }
    var perfilCargando by remember { mutableStateOf(auth.currentUser != null) }
    var errorPerfil by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    var mensajeAuth by remember { mutableStateOf("") }
    var mostrarIntro by rememberSaveable {
        mutableStateOf(true)
    }

    var pestanaActual by rememberSaveable { mutableStateOf("Inicio") }
    val observaciones = remember { mutableStateListOf<Observacion>() }
    val instituciones = remember { mutableStateListOf<Institucion>() }
    val notificaciones = remember { mutableStateListOf<NotificacionAula>() }
    var institucionUsuario by remember { mutableStateOf<Institucion?>(null) }
    var errorNotificaciones by remember { mutableStateOf("") }
    val encuestas = remember { mutableStateListOf<Encuesta>() }
    var cargandoEncuestas by remember { mutableStateOf(false) }
    var errorEncuestas by remember { mutableStateOf("") }
    val encuestasLeidasIds = remember { mutableStateListOf<String>() }
    var errorLecturasEncuestas by remember { mutableStateOf("") }

    // Mantener sincronizado el estado de Firebase Authentication.
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            usuarioFirebase = firebaseAuth.currentUser
            perfil = null
            institucionUsuario = null
            errorPerfil = ""
            perfilCargando = firebaseAuth.currentUser != null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Cargar el perfil real del usuario; el rol ya no se elige desde la app.
    LaunchedEffect(usuarioFirebase?.uid) {
        val usuario = usuarioFirebase
        if (usuario == null) {
            perfil = null
            perfilCargando = false
        } else {
            perfilCargando = true
            db.collection("users").document(usuario.uid).get()
                .addOnSuccessListener { documento ->
                    val perfilLeido = documento.aPerfil()
                    if (perfilLeido == null) {
                        errorPerfil = "La cuenta existe, pero todavía no encontramos su perfil en Firestore. Contacta al administrador."
                    } else {
                        perfil = perfilLeido
                        errorPerfil = ""
                    }
                    perfilCargando = false
                }
                .addOnFailureListener { error ->
                    errorPerfil = "No se pudo consultar tu perfil: ${error.localizedMessage ?: "Error de conexión"}"
                    perfilCargando = false
                }
        }
    }

    // Directorio de instituciones activas. El documento campus_demo_01 se conserva
    // como institución de prueba mientras permanezca con status = active.
    DisposableEffect(db) {
        val registro: ListenerRegistration = db.collection("institutions")
            .whereEqualTo("status", "active")
            .addSnapshotListener { instantanea, error ->
                if (error == null && instantanea != null) {
                    instituciones.clear()
                    instantanea.documents.mapNotNull { it.aInstitucion() }
                        .forEach { instituciones.add(it) }
                }
            }
        onDispose { registro.remove() }
    }

    // Obtener también la institución vinculada al perfil.
    LaunchedEffect(perfil?.institucionId) {
        val id = perfil?.institucionId
        if (id.isNullOrBlank()) {
            institucionUsuario = null
        } else {
            db.collection("institutions").document(id).get()
                .addOnSuccessListener { documento ->
                    institucionUsuario = documento.aInstitucion()
                }
                .addOnFailureListener { institucionUsuario = null }
        }
    }

    var errorReportes by remember { mutableStateOf("") }
    var cargandoReportes by remember { mutableStateOf(false) }

    // Los reportes se mantienen sincronizados con Firestore.
    // Estudiantes consultan únicamente los propios; docentes y directores
    // consultan los reportes de su institución.
    DisposableEffect(db, perfil?.uid, perfil?.institucionId, perfil?.rol, perfil?.estado) {
        observaciones.clear()
        errorReportes = ""
        val usuario = perfil
        val institutionId = usuario?.institucionId

        if (usuario == null || usuario.estado != "approved" || institutionId.isNullOrBlank()) {
            cargandoReportes = false
            onDispose { }
        } else {
            cargandoReportes = true
            val consulta = when (usuario.rol) {
                "student" -> db.collection("observations")
                    .whereEqualTo("authorUid", usuario.uid)
                    .limit(100)
                "teacher", "director" -> db.collection("observations")
                    .whereEqualTo("institutionId", institutionId)
                    .limit(100)
                else -> null
            }

            if (consulta == null) {
                cargandoReportes = false
                onDispose { }
            } else {
                val registro = consulta.addSnapshotListener { instantanea, error ->
                    if (error != null) {
                        errorReportes = error.localizedMessage ?: "No se pudieron consultar los reportes."
                        cargandoReportes = false
                    } else if (instantanea != null) {
                        val nuevos = instantanea.documents
                            .mapNotNull { it.aObservacion() }
                            .sortedByDescending { it.fechaMillis }
                        observaciones.clear()
                        observaciones.addAll(nuevos)
                        errorReportes = ""
                        cargandoReportes = false
                    }
                }
                onDispose { registro.remove() }
            }
        }
    }

    // Bandeja privada de notificaciones del usuario. Se sincroniza en tiempo real.
    DisposableEffect(db, perfil?.uid, perfil?.estado) {
        notificaciones.clear()
        errorNotificaciones = ""
        val usuario = perfil
        if (usuario == null || usuario.estado != "approved") {
            onDispose { }
        } else {
            val registro = db.collection("users")
                .document(usuario.uid)
                .collection("notifications")
                .limit(50)
                .addSnapshotListener { instantanea, error ->
                    if (error != null) {
                        errorNotificaciones = error.localizedMessage ?: "No se pudieron cargar las notificaciones."
                    } else if (instantanea != null) {
                        val nuevas = instantanea.documents
                            .mapNotNull { it.aNotificacion() }
                            .sortedByDescending { it.fechaMillis }
                        notificaciones.clear()
                        notificaciones.addAll(nuevas)
                        errorNotificaciones = ""
                    }
                }
            onDispose { registro.remove() }
        }
    }


    // Los participantes mantienen un recibo privado por encuesta para recordar
    // cuáles avisos ya consultaron. No hace falta que el director enumere usuarios.
    DisposableEffect(db, perfil?.uid, perfil?.institucionId, perfil?.rol, perfil?.estado) {
        encuestasLeidasIds.clear()
        errorLecturasEncuestas = ""
        val usuario = perfil
        if (
            usuario == null ||
            usuario.estado != "approved" ||
            usuario.institucionId.isNullOrBlank() ||
            usuario.rol !in setOf("student", "teacher", "parent")
        ) {
            onDispose { }
        } else {
            val registro = db.collection("users")
                .document(usuario.uid)
                .collection("surveyReads")
                .addSnapshotListener { instantanea, error ->
                    if (error != null) {
                        errorLecturasEncuestas = error.localizedMessage ?: "No se pudo actualizar el estado de los avisos de encuestas."
                    } else if (instantanea != null) {
                        encuestasLeidasIds.clear()
                        encuestasLeidasIds.addAll(instantanea.documents.map { it.id })
                        errorLecturasEncuestas = ""
                    }
                }
            onDispose { registro.remove() }
        }
    }

    // Encuestas de la institución: el director ve también las cerradas;
    // los participantes consultan las publicadas y las cerradas como historial.
    DisposableEffect(db, perfil?.uid, perfil?.institucionId, perfil?.rol, perfil?.estado, institucionUsuario?.estado) {
        encuestas.clear()
        errorEncuestas = ""
        val usuario = perfil
        val institutionId = usuario?.institucionId
        if (usuario == null || usuario.estado != "approved" || institutionId.isNullOrBlank() || institucionUsuario?.estado != "active") {
            cargandoEncuestas = false
            onDispose { }
        } else {
            cargandoEncuestas = true
            val consultaBase = db.collection("surveys")
                .whereEqualTo("institutionId", institutionId)
            val consulta = if (usuario.rol == "director") {
                consultaBase
            } else {
                // Firestore Rules no filtra documentos: el query del participante
                // debe limitarse a encuestas publicadas o cerradas.
                consultaBase.whereIn("status", listOf("published", "closed"))
            }
            val registro = consulta
                .limit(50)
                .addSnapshotListener { instantanea, error ->
                    if (error != null) {
                        errorEncuestas = error.localizedMessage ?: "No se pudieron consultar las encuestas."
                        cargandoEncuestas = false
                    } else if (instantanea != null) {
                        val nuevas = instantanea.documents
                            .mapNotNull { it.aEncuesta() }
                            .sortedByDescending { it.fechaMillis }
                        encuestas.clear()
                        encuestas.addAll(nuevas)
                        errorEncuestas = ""
                        cargandoEncuestas = false
                    }
                }
            onDispose { registro.remove() }
        }
    }

    // Una encuesta publicada funciona como aviso institucional. El listener
    // de encuestas ya sincroniza en tiempo real; el recibo privado por usuario
    // permite mantener el aviso como leído/no leído sin exponer la lista de alumnos.
    val notificacionesEncuestas = if (
        perfil?.estado == "approved" &&
        (perfil?.rol == "student" || perfil?.rol == "teacher" || perfil?.rol == "parent")
    ) {
        encuestas
            .filter { (it.estado == "published" || it.estado == "closed") && it.fechaPublicacionMillis > 0L }
            .map { encuesta ->
                NotificacionAula(
                    id = "encuesta_${encuesta.id}",
                    titulo = "Nueva encuesta de tu institución",
                    mensaje = "El director publicó «${encuesta.titulo}». Entra a Encuestas y censos para consultar y participar.",
                    reporteId = "",
                    estadoReporte = encuesta.estado,
                    fechaMillis = encuesta.fechaPublicacionMillis,
                    leida = encuestasLeidasIds.contains(encuesta.id),
                    tipo = "survey",
                    encuestaId = encuesta.id
                )
            }
    } else {
        emptyList()
    }
    val notificacionesCompletas = (notificaciones.toList() + notificacionesEncuestas)
        .sortedByDescending { it.fechaMillis }

    val iniciarSesion: (String, String) -> Unit = { correo, clave ->
        ocupado = true
        mensajeAuth = ""
        auth.signInWithEmailAndPassword(correo.trim(), clave)
            .addOnCompleteListener { tarea ->
                ocupado = false
                if (!tarea.isSuccessful) {
                    mensajeAuth = tarea.exception?.localizedMessage ?: "No se pudo iniciar sesión."
                }
            }
    }

    val registrarCuenta: (String, String, String, String, String?, Location?) -> Unit =
        { nombre, correo, clave, rolSolicitado, institucionSolicitada, ubicacionRegistro ->
            ocupado = true
            mensajeAuth = ""

            val accesoAutomatico = rolSolicitado == "student" || rolSolicitado == "teacher"
            val institucionSeleccionada = instituciones.firstOrNull { it.id == institucionSolicitada }

            if (accesoAutomatico && (ubicacionRegistro == null || institucionSeleccionada == null)) {
                ocupado = false
                mensajeAuth = "No pudimos comprobar tu ubicación o la institución seleccionada. Inténtalo de nuevo."
            } else {
                val distanciaRegistro = if (accesoAutomatico && ubicacionRegistro != null && institucionSeleccionada != null) {
                    distanciaMetros(
                        ubicacionRegistro.latitude,
                        ubicacionRegistro.longitude,
                        institucionSeleccionada.latitud,
                        institucionSeleccionada.longitud
                    )
                } else {
                    0f
                }

                auth.createUserWithEmailAndPassword(correo.trim(), clave)
                    .addOnCompleteListener { tarea ->
                        if (!tarea.isSuccessful) {
                            ocupado = false
                            mensajeAuth = tarea.exception?.localizedMessage ?: "No se pudo crear la cuenta."
                            return@addOnCompleteListener
                        }

                        val nuevoUsuario: FirebaseUser? = tarea.result?.user
                        if (nuevoUsuario == null) {
                            ocupado = false
                            mensajeAuth = "Firebase no devolvió los datos de la cuenta creada."
                        } else {
                            val rolFinal = if (accesoAutomatico) rolSolicitado else "pending"
                            val estadoFinal = if (accesoAutomatico) "approved" else "pending"
                            val institucionFinal = if (accesoAutomatico) institucionSeleccionada?.id else null

                            val datos = hashMapOf<String, Any?>(
                                "uid" to nuevoUsuario.uid,
                                "displayName" to nombre.trim(),
                                "email" to correo.trim(),
                                "requestedRole" to rolSolicitado,
                                "requestedInstitutionId" to institucionSolicitada,
                                "role" to rolFinal,
                                "status" to estadoFinal,
                                "institutionId" to institucionFinal,
                                "createdAt" to FieldValue.serverTimestamp()
                            )

                            if (accesoAutomatico && ubicacionRegistro != null) {
                                datos["enrollmentLatitude"] = ubicacionRegistro.latitude
                                datos["enrollmentLongitude"] = ubicacionRegistro.longitude
                                datos["enrollmentAccuracyMeters"] = ubicacionRegistro.accuracy.toDouble()
                                datos["enrollmentDistanceMeters"] = distanciaRegistro.toDouble()
                                datos["enrollmentVerifiedAt"] = FieldValue.serverTimestamp()
                                datos["enrollmentVerificationMethod"] = "android_fused_location"
                            }

                            db.collection("users").document(nuevoUsuario.uid)
                                .set(datos)
                                .addOnSuccessListener {
                                    perfil = PerfilUsuario(
                                        uid = nuevoUsuario.uid,
                                        nombre = nombre.trim(),
                                        correo = correo.trim(),
                                        rol = rolFinal,
                                        estado = estadoFinal,
                                        institucionId = institucionFinal,
                                        rolSolicitado = rolSolicitado
                                    )
                                    perfilCargando = false
                                    ocupado = false
                                    mensajeAuth = ""
                                }
                                .addOnFailureListener { error ->
                                    // Evita dejar una cuenta Auth huérfana si Firestore rechaza el perfil.
                                    nuevoUsuario.delete().addOnCompleteListener {
                                        auth.signOut()
                                        ocupado = false
                                        mensajeAuth = "No se pudo guardar el perfil en Firestore; la cuenta de acceso se canceló para evitar un registro incompleto. Revisa las reglas publicadas. Detalle: ${error.localizedMessage ?: "sin detalle"}"
                                    }
                                }
                        }
                    }
            }
        }

    val cerrarSesion = {
        auth.signOut()
        pestanaActual = "Inicio"
        observaciones.clear()
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LuumTurquesa,
            onPrimary = Fondo,
            primaryContainer = AzulClaro,
            onPrimaryContainer = Texto,
            secondary = LuumVerde,
            onSecondary = Fondo,
            background = Fondo,
            onBackground = Texto,
            surface = Superficie,
            onSurface = Texto,
            surfaceVariant = LuumSuperficieAlta,
            onSurfaceVariant = Gris,
            outline = LuumContorno,
            error = LuumError
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(LuumFondoGradienteA, Fondo, LuumFondoGradienteB, Fondo)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .offset(x = 225.dp, y = 80.dp)
                    .size(190.dp)
                    .background(Color(0x1C019485), CircleShape)
            )
            Box(
                modifier = Modifier
                    .offset(x = (-100).dp, y = 430.dp)
                    .size(230.dp)
                    .background(Color(0x1202A215), CircleShape)
            )

            when {
                usuarioFirebase == null -> {
                    AccesoScreen(
                        instituciones = instituciones,
                        ocupado = ocupado,
                        mensaje = mensajeAuth,
                        iniciarSesion = iniciarSesion,
                        registrarCuenta = registrarCuenta
                    )

                }

                perfilCargando -> {
                    EstadoCargaScreen("Consultando tu cuenta segura...")
                }

                perfil == null -> {
                    EstadoCuentaScreen(
                        mensaje = errorPerfil.ifBlank { "No encontramos tu perfil de LuumEdu." },
                        cerrarSesion = cerrarSesion
                    )
                }

                perfil!!.estado != "approved" || perfil!!.rol == "pending" -> {
                    SolicitudPendienteScreen(
                        perfil = perfil!!,
                        cerrarSesion = cerrarSesion
                    )
                }

                else -> {
                    val usuario = perfil!!
                    // El director administra reportes; estudiantes y docentes los publican.
                    val pestanaReportes = if (usuario.rol == "director") "Reportes" else "Reportar"
                    val pestanas = listOf(
                        Pestana("Inicio", "⌂"),
                        Pestana("Escuela", "◎"),
                        Pestana(pestanaReportes, "+"),
                        Pestana("Aprender", "▤"),
                        Pestana("Perfil", "◉")
                    )

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        bottomBar = {
                            NavigationBar(
                                containerColor = LuumSuperficieNavegacion,
                                tonalElevation = 0.dp
                            ) {
                                pestanas.forEach { pestana ->
                                    NavigationBarItem(
                                        selected = pestanaActual == pestana.nombre,
                                        onClick = { pestanaActual = pestana.nombre },
                                        icon = { Text(pestana.simbolo, fontSize = 22.sp) },
                                        label = { Text(pestana.nombre, fontSize = 10.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Azul,
                                            selectedTextColor = Azul,
                                            indicatorColor = AzulClaro,
                                            unselectedIconColor = Gris,
                                            unselectedTextColor = Gris
                                        )
                                    )
                                }
                            }
                        }
                    ) { espacio ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(espacio)
                        ) {
                            Encabezado(
                                rol = etiquetaRol(usuario.rol),
                                notificacionesNoLeidas = notificacionesCompletas.count { !it.leida },
                                abrirNotificaciones = { pestanaActual = "Notificaciones" }
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 18.dp, vertical = 17.dp)
                            ) {
                                when (pestanaActual) {
                                    "Inicio" -> InicioScreen(
                                        rol = etiquetaRol(usuario.rol),
                                        observaciones = observaciones,
                                        instituciones = instituciones,
                                        institucionDestacada = institucionUsuario,
                                        navegar = { pestanaActual = it }
                                    )

                                    "Escuela" -> EscuelaScreen(
                                        perfil = usuario,
                                        institucionPropia = institucionUsuario,
                                        instituciones = instituciones,
                                        registrar = { nombre, tipo, direccion, ubicacion, resultado ->
                                            val idUsuario = usuarioFirebase?.uid
                                            if (idUsuario == null) {
                                                resultado(false, "La sesión expiró. Inicia sesión otra vez.")
                                            } else {
                                                val nuevaReferencia = db.collection("institutions").document()
                                                val referenciaUsuario = db.collection("users").document(idUsuario)
                                                val lote = db.batch()
                                                lote.set(
                                                    nuevaReferencia,
                                                    mapOf(
                                                        "name" to nombre.trim(),
                                                        "type" to tipo,
                                                        "address" to direccion.trim(),
                                                        "latitude" to ubicacion.latitude,
                                                        "longitude" to ubicacion.longitude,
                                                        "status" to "pending",
                                                        "directorUid" to idUsuario,
                                                        "createdAt" to FieldValue.serverTimestamp()
                                                    )
                                                )
                                                lote.update(
                                                    referenciaUsuario,
                                                    mapOf(
                                                        "institutionId" to nuevaReferencia.id,
                                                        "updatedAt" to FieldValue.serverTimestamp()
                                                    )
                                                )
                                                lote.commit()
                                                    .addOnSuccessListener {
                                                        perfil = usuario.copy(institucionId = nuevaReferencia.id)
                                                        institucionUsuario = Institucion(
                                                            id = nuevaReferencia.id,
                                                            nombre = nombre.trim(),
                                                            tipo = tipo,
                                                            direccion = direccion.trim(),
                                                            latitud = ubicacion.latitude,
                                                            longitud = ubicacion.longitude,
                                                            estado = "pending"
                                                        )
                                                        resultado(true, "Solicitud enviada. La institución quedó pendiente de revisión.")
                                                    }
                                                    .addOnFailureListener { error ->
                                                        resultado(false, "No se pudo enviar la institución. Revisa las reglas de Firestore. ${error.localizedMessage ?: ""}")
                                                    }
                                            }
                                        }
                                    )

                                    "Reportar", "Reportes" -> ReportarScreen(
                                        perfil = usuario,
                                        institucion = institucionUsuario,
                                        observaciones = observaciones,
                                        db = db,
                                        cargandoReportes = cargandoReportes,
                                        errorReportes = errorReportes
                                    )

                                    "Aprender" -> AprenderIAContent(
                                        observaciones = observaciones.toList()
                                    )

                                    "Encuestas" -> EncuestasScreen(
                                        perfil = usuario,
                                        institucion = institucionUsuario,
                                        encuestas = encuestas,
                                        db = db,
                                        cargando = cargandoEncuestas,
                                        error = errorEncuestas
                                    )

                                    "Notificaciones" -> NotificacionesScreen(
                                        notificaciones = notificacionesCompletas,
                                        error = listOf(errorNotificaciones, errorLecturasEncuestas)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" · "),
                                        marcarComoLeida = { notificacion ->
                                            if (notificacion.tipo == "survey" && notificacion.encuestaId.isNotBlank()) {
                                                db.collection("users")
                                                    .document(usuario.uid)
                                                    .collection("surveyReads")
                                                    .document(notificacion.encuestaId)
                                                    .set(
                                                        mapOf(
                                                            "surveyId" to notificacion.encuestaId,
                                                            "institutionId" to (usuario.institucionId ?: ""),
                                                            "readAt" to FieldValue.serverTimestamp()
                                                        )
                                                    )
                                            } else {
                                                db.collection("users")
                                                    .document(usuario.uid)
                                                    .collection("notifications")
                                                    .document(notificacion.id)
                                                    .update("read", true)
                                            }
                                        },
                                        marcarTodasComoLeidas = {
                                            val pendientes = notificacionesCompletas.filter { !it.leida }
                                            if (pendientes.isNotEmpty()) {
                                                val lote = db.batch()
                                                pendientes.forEach { notificacion ->
                                                    if (notificacion.tipo == "survey" && notificacion.encuestaId.isNotBlank()) {
                                                        val referencia = db.collection("users")
                                                            .document(usuario.uid)
                                                            .collection("surveyReads")
                                                            .document(notificacion.encuestaId)
                                                        lote.set(
                                                            referencia,
                                                            mapOf(
                                                                "surveyId" to notificacion.encuestaId,
                                                                "institutionId" to (usuario.institucionId ?: ""),
                                                                "readAt" to FieldValue.serverTimestamp()
                                                            )
                                                        )
                                                    } else {
                                                        val referencia = db.collection("users")
                                                            .document(usuario.uid)
                                                            .collection("notifications")
                                                            .document(notificacion.id)
                                                        lote.update(referencia, "read", true)
                                                    }
                                                }
                                                lote.commit()
                                            }
                                        },
                                        abrirEncuestas = { pestanaActual = "Encuestas" }
                                    )

                                    "Perfil" -> PerfilScreen(
                                        perfil = usuario,
                                        institucion = institucionUsuario,
                                        cerrarSesion = cerrarSesion
                                    )
                                }

                                if (mensajeAuth.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(mensajeAuth, color = LuumAmbar, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            if (mostrarIntro) {
                IntroLogoLuumEdu(
                    onFinished = {
                        mostrarIntro = false
                    }
                )
            }
        }
    }
}

// =====================================================
// 7. ACCESO, REGISTRO Y SOLICITUD DE ROL
// =====================================================

@Composable
private fun AccesoScreen(
    instituciones: List<Institucion>,
    ocupado: Boolean,
    mensaje: String,
    iniciarSesion: (String, String) -> Unit,
    registrarCuenta: (String, String, String, String, String?, Location?) -> Unit
) {
    val context = LocalContext.current
    var modoRegistro by rememberSaveable { mutableStateOf(false) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var rolSolicitado by rememberSaveable { mutableStateOf("student") }
    var institucionId by rememberSaveable { mutableStateOf("") }
    var menuRoles by remember { mutableStateOf(false) }
    var menuInstituciones by remember { mutableStateOf(false) }
    var obteniendoUbicacion by remember { mutableStateOf(false) }
    var mensajeLocal by remember { mutableStateOf("") }

    val institucionSeleccionada = instituciones.firstOrNull { it.id == institucionId }

    val solicitarPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        mensajeLocal = if (concedido) {
            "Permiso concedido. Pulsa nuevamente «Crear cuenta» para comprobar tu ubicación."
        } else {
            "Para crear una cuenta de estudiante o docente debes permitir la ubicación mientras usas la aplicación."
        }
    }

    fun procesarRegistro() {
        if (rolSolicitado == "director" || rolSolicitado == "parent") {
            registrarCuenta(
                nombre.trim(), correo.trim(), clave, rolSolicitado,
                if (rolSolicitado == "director") null else institucionId,
                null
            )
            return
        }

        val escuela = institucionSeleccionada
        if (escuela == null) {
            mensajeLocal = "Selecciona una institución activa antes de continuar."
            return
        }

        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            solicitarPermisos.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            return
        }

        obteniendoUbicacion = true
        mensajeLocal = "Comprobando que estés dentro del plantel..."
        obtenerUbicacionActual(context) { ubicacion ->
            obteniendoUbicacion = false
            when {
                ubicacion == null -> {
                    mensajeLocal = "No pudimos obtener tu ubicación actual. Activa el GPS e inténtalo nuevamente."
                }
                ubicacion.accuracy > PRECISION_MAXIMA_METROS -> {
                    mensajeLocal = "La ubicación no es suficientemente precisa (±${ubicacion.accuracy.toInt()} m). Necesitamos 30 m o menos."
                }
                else -> {
                    val distancia = distanciaMetros(
                        ubicacion.latitude,
                        ubicacion.longitude,
                        escuela.latitud,
                        escuela.longitud
                    )
                    if (distancia <= RADIO_OPINION_METROS) {
                        mensajeLocal = ""
                        registrarCuenta(
                            nombre.trim(), correo.trim(), clave, rolSolicitado,
                            escuela.id, ubicacion
                        )
                    } else {
                        mensajeLocal = "No estás dentro del área de registro de ${escuela.nombre}. Estás aproximadamente a ${distancia.toInt()} m; el límite es ${RADIO_OPINION_METROS.toInt()} m."
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LuumEdu", color = Azul, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
            Text("GEOSPATIAL EDUCATION INTELLIGENCE", color = Gris, fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(25.dp))

            GlassPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        if (modoRegistro) "CREAR CUENTA" else "BIENVENIDO A LUUMEDU",
                        color = Azul,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(17.dp))

                    if (modoRegistro) {
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it; mensajeLocal = "" },
                            label = { Text("Nombre completo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(11.dp))
                    }

                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it; mensajeLocal = "" },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(11.dp))
                    OutlinedTextField(
                        value = clave,
                        onValueChange = { clave = it; mensajeLocal = "" },
                        label = { Text("Contraseña (mínimo 6 caracteres)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (modoRegistro) {
                        Spacer(Modifier.height(15.dp))
                        Text("Tipo de cuenta", color = Gris, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Box {
                            OutlinedButton(
                                onClick = { menuRoles = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(etiquetaRol(rolSolicitado), modifier = Modifier.weight(1f))
                                Text("⌄")
                            }
                            DropdownMenu(
                                expanded = menuRoles,
                                onDismissRequest = { menuRoles = false }
                            ) {
                                listOf(
                                    "director" to "Director",
                                    "student" to "Estudiante",
                                    "teacher" to "Docente",
                                    "parent" to "Padre de familia"
                                ).forEach { (valor, texto) ->
                                    DropdownMenuItem(
                                        text = { Text(texto) },
                                        onClick = {
                                            rolSolicitado = valor
                                            institucionId = ""
                                            mensajeLocal = ""
                                            menuRoles = false
                                        }
                                    )
                                }
                            }
                        }

                        if (rolSolicitado != "director") {
                            Spacer(Modifier.height(11.dp))
                            Text("Institución educativa", color = Gris, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            Box {
                                OutlinedButton(
                                    onClick = { menuInstituciones = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        institucionSeleccionada?.nombre ?: "Seleccionar institución",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("⌄")
                                }
                                DropdownMenu(
                                    expanded = menuInstituciones,
                                    onDismissRequest = { menuInstituciones = false }
                                ) {
                                    instituciones.forEach { institucion ->
                                        DropdownMenuItem(
                                            text = { Text(institucion.nombre) },
                                            onClick = {
                                                institucionId = institucion.id
                                                mensajeLocal = ""
                                                menuInstituciones = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (instituciones.isEmpty()) {
                                Spacer(Modifier.height(7.dp))
                                Text(
                                    "Todavía no hay escuelas activas. Un director debe registrar una y el administrador aprobarla antes de que puedas crear tu cuenta.",
                                    color = Gris,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(11.dp))
                        val explicacion = when (rolSolicitado) {
                            "director" -> "Las cuentas de director requieren autorización del administrador. Después podrán registrar su institución."
                            "student" -> "Tu cuenta se activará automáticamente si estás dentro de los 150 m del plantel seleccionado y el GPS tiene precisión de 30 m o mejor."
                            "teacher" -> "Tu cuenta se activará automáticamente si estás dentro de los 150 m del plantel seleccionado y el GPS tiene precisión de 30 m o mejor."
                            else -> "Las cuentas de padres de familia quedan pendientes de revisión administrativa."
                        }
                        Text(explicacion, color = Gris, fontSize = 11.sp, lineHeight = 16.sp)
                    }

                    val mensajeVisible = mensajeLocal.ifBlank { mensaje }
                    if (mensajeVisible.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            mensajeVisible,
                            color = if (mensajeVisible.startsWith("Ubicación verificada")) LuumExito else LuumError,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (modoRegistro) {
                                procesarRegistro()
                            } else {
                                iniciarSesion(correo.trim(), clave)
                            }
                        },
                        enabled = !ocupado && !obteniendoUbicacion && correo.contains("@") && clave.length >= 6 &&
                                (!modoRegistro || (nombre.isNotBlank() && (rolSolicitado == "director" || institucionId.isNotBlank()))),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (ocupado || obteniendoUbicacion) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(if (obteniendoUbicacion) "Comprobando GPS..." else "Creando cuenta...")
                        } else {
                            Text(
                                when {
                                    !modoRegistro -> "Iniciar sesión"
                                    rolSolicitado == "student" || rolSolicitado == "teacher" -> "Comprobar ubicación y crear cuenta"
                                    else -> "Enviar solicitud"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(9.dp))
                    TextButton(
                        onClick = {
                            modoRegistro = !modoRegistro
                            mensajeLocal = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (modoRegistro) "Ya tengo una cuenta · Iniciar sesión" else "¿No tienes cuenta? Crear cuenta",
                            color = Azul,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Directores: autorización administrativa. Estudiantes y docentes: alta automática al validar su ubicación en el plantel. Padres de familia: revisión administrativa.",
                color = Gris,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun EstadoCargaScreen(texto: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Azul)
            Spacer(Modifier.height(14.dp))
            Text(texto, color = Gris, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EstadoCuentaScreen(mensaje: String, cerrarSesion: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        GlassPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
            Column(Modifier.padding(20.dp)) {
                Text("PERFIL NO DISPONIBLE", color = Azul, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(mensaje, color = Gris, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                Button(onClick = cerrarSesion, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
            }
        }
    }
}

@Composable
private fun SolicitudPendienteScreen(perfil: PerfilUsuario, cerrarSesion: () -> Unit) {
    val rechazado = perfil.estado == "rejected"
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        GlassPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
            Column(Modifier.padding(22.dp)) {
                Text(if (rechazado) "SOLICITUD NO APROBADA" else "SOLICITUD EN REVISIÓN", color = Azul, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(13.dp))
                Text("Hola, ${perfil.nombre}", color = Texto, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text(
                    if (rechazado) "Tu solicitud fue rechazada. Contacta al administrador de LuumEdu si consideras que se trata de un error."
                    else if (perfil.rolSolicitado == "director") "Tu solicitud para tener una cuenta de director está pendiente de autorización. Cuando sea aprobada podrás registrar tu institución."
                    else "Tu solicitud de cuenta está pendiente de revisión administrativa.",
                    color = Gris,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(15.dp))
                Text("Perfil solicitado: ${etiquetaRol(perfil.rolSolicitado)}", color = Texto, fontSize = 12.sp)
                Spacer(Modifier.height(20.dp))
                Button(onClick = cerrarSesion, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
            }
        }
    }
}

// =====================================================
// 8. MAPA GEOGRÁFICO, MARCADORES Y GPS
// =====================================================

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
@Composable
fun MapaGeografico(
    modifier: Modifier = Modifier,
    instituciones: List<Institucion>,
    observaciones: List<Observacion>,
    institucionDestacada: Institucion?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var tienePermisoUbicacion by remember(context) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val solicitarPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        tienePermisoUbicacion = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!tienePermisoUbicacion) {
            solicitarPermisos.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    var mapaActual by remember { mutableStateOf<MapLibreMap?>(null) }
    var estiloActual by remember { mutableStateOf<Style?>(null) }
    var estiloListo by remember { mutableStateOf(false) }
    var reporteSeleccionado by remember { mutableStateOf<Observacion?>(null) }

    var filtroCategoria by rememberSaveable { mutableStateOf("Todas") }
    var filtroEstado by remember { mutableStateOf("Activos") }
    var filtroPeriodo by rememberSaveable { mutableStateOf("Todo") }
    var menuCategoria by remember { mutableStateOf(false) }
    var menuEstado by remember { mutableStateOf(false) }
    var menuPeriodo by remember { mutableStateOf(false) }

    val categoriasFiltro = listOf(
        "Todas",
        "Entorno geográfico",
        "Infraestructura",
        "Servicios escolares",
        "Experiencia educativa",
        "Condiciones ambientales",
        "Seguridad y accesibilidad",
        "Otro"
    )
    val estadosFiltro = listOf(
        "Activos" to "Activos",
        "Todos" to "Todos (historial)",
        "new" to "Nuevo",
        "in_review" to "En revisión",
        "in_progress" to "En atención",
        "resolved" to "Resuelto",
        "verified" to "Mejora verificada",
        "rejected" to "No procedente"
    )
    val periodosFiltro = listOf("Todo", "Últimos 7 días", "Últimos 30 días", "Últimos 90 días")

    // Convertimos las listas observables a instantáneas para que los cambios
    // recibidos en tiempo real desde Firestore actualicen los marcadores.
    val observacionesActuales = observaciones.toList()
    val institucionesActuales = instituciones.toList()

    val ahora = System.currentTimeMillis()
    val dias = when (filtroPeriodo) {
        "Últimos 7 días" -> 7L
        "Últimos 30 días" -> 30L
        "Últimos 90 días" -> 90L
        else -> null
    }
    val observacionesFiltradas = observacionesActuales.filter { reporte ->
        val tieneCoordenadas = reporte.latitud != null && reporte.longitud != null
        val coincideCategoria = filtroCategoria == "Todas" || reporte.categoria == filtroCategoria
        val coincideEstado = when (filtroEstado) {
            "Activos" -> reporte.estado in setOf("new", "in_review", "in_progress")
            "Todos" -> true
            else -> reporte.estado == filtroEstado
        }
        val fechaDentroDelPeriodo = dias == null || (
                reporte.fechaMillis > 0L &&
                        reporte.fechaMillis >= ahora - dias * 24L * 60L * 60L * 1000L &&
                        reporte.fechaMillis <= ahora
                )
        tieneCoordenadas && coincideCategoria && coincideEstado && fechaDentroDelPeriodo
    }
    val clavesInstituciones = institucionesActuales.map { "${it.id}:${it.latitud}:${it.longitud}" }
    val clavesReportes = observacionesFiltradas.map {
        "${it.id}:${it.latitud}:${it.longitud}:${it.estado}:${it.categoria}:${it.titulo}:${it.ubicacion}:${it.turno}:${it.fechaMillis}:${it.descripcion.hashCode()}"
    }

    val mapView = remember(context) { MapView(context).apply { onCreate(null) } }
    val marcadores = remember(mapView) { mutableListOf<Marker>() }
    val reportesPorMarcador = remember(mapView) { mutableMapOf<Long, Observacion>() }
    val iconosMarcadores = remember(mapView) { mutableMapOf<Int, Icon>() }

    DisposableEffect(mapView, lifecycleOwner) {
        var iniciado = false
        var reanudado = false
        var destruido = false

        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_START -> if (!iniciado && !destruido) {
                    mapView.onStart()
                    iniciado = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!iniciado && !destruido) {
                        mapView.onStart()
                        iniciado = true
                    }
                    if (!reanudado && !destruido) {
                        mapView.onResume()
                        reanudado = true
                    }
                }
                Lifecycle.Event.ON_PAUSE -> if (reanudado && !destruido) {
                    mapView.onPause()
                    reanudado = false
                }
                Lifecycle.Event.ON_STOP -> if (iniciado && !destruido) {
                    if (reanudado) {
                        mapView.onPause()
                        reanudado = false
                    }
                    mapView.onStop()
                    iniciado = false
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        when {
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> {
                mapView.onStart()
                iniciado = true
                mapView.onResume()
                reanudado = true
            }
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) -> {
                mapView.onStart()
                iniciado = true
            }
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!destruido) {
                if (reanudado) mapView.onPause()
                if (iniciado) mapView.onStop()
                mapView.onDestroy()
                destruido = true
            }
        }
    }

    LaunchedEffect(mapaActual, estiloListo, institucionDestacada?.id) {
        val mapa = mapaActual ?: return@LaunchedEffect
        if (!estiloListo) return@LaunchedEffect
        val escuela = institucionDestacada
        if (escuela != null && escuela.latitud.isFinite() && escuela.longitud.isFinite()) {
            mapa.cameraPosition = CameraPosition.Builder()
                .target(LatLng(escuela.latitud, escuela.longitud))
                .zoom(16.0)
                .build()
        }
    }

    LaunchedEffect(clavesReportes) {
        val seleccionadoId = reporteSeleccionado?.id
        if (seleccionadoId != null) {
            reporteSeleccionado = observacionesFiltradas.firstOrNull { it.id == seleccionadoId }
        }
    }

    LaunchedEffect(mapaActual, estiloActual, estiloListo, clavesInstituciones, clavesReportes) {
        val mapa = mapaActual ?: return@LaunchedEffect
        if (!estiloListo || estiloActual == null) return@LaunchedEffect

        marcadores.forEach { mapa.removeMarker(it) }
        marcadores.clear()
        reportesPorMarcador.clear()

        val colorEscuela = AndroidColor.rgb(94, 234, 212)
        val iconoEscuela = iconosMarcadores.getOrPut(colorEscuela) {
            crearIconoPunto(context, colorEscuela)
        }
        institucionesActuales.forEach { institucion ->
            val marcador = mapa.addMarker(
                MarkerOptions()
                    .position(LatLng(institucion.latitud, institucion.longitud))
                    .title(institucion.nombre)
                    .snippet("${institucion.tipo} · ${institucion.direccion}")
                    .icon(iconoEscuela)
            )
            marcadores.add(marcador)
        }

        observacionesFiltradas.forEach { reporte ->
            val latitud = reporte.latitud ?: return@forEach
            val longitud = reporte.longitud ?: return@forEach
            val color = colorCategoriaReporte(reporte.categoria)
            val icono = iconosMarcadores.getOrPut(color) {
                crearIconoPunto(context, color)
            }
            val marcador = mapa.addMarker(
                MarkerOptions()
                    .position(LatLng(latitud, longitud))
                    .title(reporte.titulo)
                    .snippet("${reporte.categoria} · ${etiquetaEstadoReporte(reporte.estado)} · ${etiquetaTurno(reporte.turno)}")
                    .icon(icono)
            )
            marcadores.add(marcador)
            reportesPorMarcador[marcador.id] = reporte
        }

        mapa.setOnMarkerClickListener { marcador ->
            val reporte = reportesPorMarcador[marcador.id]
            if (reporte != null) {
                reporteSeleccionado = reporte
                true
            } else {
                false
            }
        }
    }

    LaunchedEffect(mapaActual, estiloActual, tienePermisoUbicacion) {
        val mapa = mapaActual ?: return@LaunchedEffect
        val estilo = estiloActual ?: return@LaunchedEffect
        if (!tienePermisoUbicacion) return@LaunchedEffect

        try {
            val ubicacion = mapa.locationComponent
            if (!ubicacion.isLocationComponentActivated) {
                val opciones = LocationComponentOptions.builder(context)
                    .pulseEnabled(true)
                    .pulseColor(AndroidColor.rgb(20, 184, 166))
                    .foregroundTintColor(AndroidColor.WHITE)
                    .trackingGesturesManagement(true)
                    .build()
                val activacion = LocationComponentActivationOptions.builder(context, estilo)
                    .locationComponentOptions(opciones)
                    .useDefaultLocationEngine(true)
                    .build()
                ubicacion.activateLocationComponent(activacion)
            }
            ubicacion.isLocationComponentEnabled = true
            ubicacion.cameraMode = CameraMode.TRACKING
            ubicacion.zoomWhileTracking(16.0)
        } catch (_: SecurityException) {
            tienePermisoUbicacion = false
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { mapa ->
                        mapaActual = mapa
                        mapa.setStyle(MAPA_ESTILO) { estilo ->
                            estiloActual = estilo
                            estiloListo = true
                            val escuela = institucionDestacada
                            mapa.cameraPosition = CameraPosition.Builder()
                                .target(
                                    if (escuela != null) LatLng(escuela.latitud, escuela.longitud)
                                    else CANCUN_REFERENCIA
                                )
                                .zoom(if (escuela != null) 16.0 else 14.0)
                                .build()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        GlassPanel(
            modifier = Modifier.align(Alignment.TopCenter).padding(8.dp).fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            accent = true
        ) {
            Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CAPAS DEL MAPA", color = LuumTurquesaClaro, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                    Text("${instituciones.size} escuelas · ${observacionesFiltradas.size} reportes", color = Color.White, fontSize = 9.sp)
                }
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { menuCategoria = true },
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)
                        ) { Text(if (filtroCategoria == "Todas") "Categoría" else filtroCategoria.take(10), fontSize = 9.sp, maxLines = 1) }
                        DropdownMenu(expanded = menuCategoria, onDismissRequest = { menuCategoria = false }) {
                            categoriasFiltro.forEach { opcion ->
                                DropdownMenuItem(text = { Text(opcion) }, onClick = { filtroCategoria = opcion; menuCategoria = false; reporteSeleccionado = null })
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { menuEstado = true },
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)
                        ) { Text(if (filtroEstado == "Activos") "Activos" else if (filtroEstado == "Todos") "Historial" else etiquetaEstadoReporte(filtroEstado), fontSize = 9.sp, maxLines = 1) }
                        DropdownMenu(expanded = menuEstado, onDismissRequest = { menuEstado = false }) {
                            estadosFiltro.forEach { (codigo, etiqueta) ->
                                DropdownMenuItem(text = { Text(etiqueta) }, onClick = { filtroEstado = codigo; menuEstado = false; reporteSeleccionado = null })
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { menuPeriodo = true },
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)
                        ) { Text(if (filtroPeriodo == "Todo") "Periodo" else filtroPeriodo.replace("Últimos ", ""), fontSize = 9.sp, maxLines = 1) }
                        DropdownMenu(expanded = menuPeriodo, onDismissRequest = { menuPeriodo = false }) {
                            periodosFiltro.forEach { opcion ->
                                DropdownMenuItem(text = { Text(opcion) }, onClick = { filtroPeriodo = opcion; menuPeriodo = false; reporteSeleccionado = null })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).background(LuumTurquesaClaro, CircleShape))
                        Text("Escuelas", color = Gris, fontSize = 9.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).background(LuumAmbar, CircleShape))
                        Text("Infraestructura", color = Gris, fontSize = 9.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).background(LuumVerde, CircleShape))
                        Text("Ambiente", color = Gris, fontSize = 9.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).background(Color(0xFFB59CFF), CircleShape))
                        Text("Otros", color = Gris, fontSize = 9.sp)
                    }
                }
            }
        }

        reporteSeleccionado?.let { reporte ->
            GlassPanel(
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 8.dp, end = 8.dp, bottom = 56.dp).fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                accent = true
            ) {
                Column(Modifier.padding(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(reporte.titulo, modifier = Modifier.weight(1f), color = Texto, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        TextButton(onClick = { reporteSeleccionado = null }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                            Text("Cerrar", fontSize = 10.sp)
                        }
                    }
                    Text("${reporte.categoria} · ${reporte.ubicacion}", color = Azul, fontSize = 10.sp, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Text(reporte.descripcion, color = Gris, fontSize = 10.sp, maxLines = 2, lineHeight = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${etiquetaEstadoReporte(reporte.estado)} · ${etiquetaTurno(reporte.turno)} · ${formatoFechaReporte(reporte.fechaMillis)}", color = LuumTurquesaClaro, fontSize = 9.sp)
                }
            }
        }

        GlassPanel(
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            shape = CircleShape,
            accent = true,
            onClick = {
                if (!tienePermisoUbicacion) {
                    solicitarPermisos.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                } else {
                    val mapa = mapaActual
                    if (mapa != null) {
                        val ubicacion = mapa.locationComponent
                        if (ubicacion.isLocationComponentActivated && ubicacion.isLocationComponentEnabled) {
                            val ultima = ubicacion.lastKnownLocation
                            if (ultima != null) {
                                mapa.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(ultima.latitude, ultima.longitude))
                                    .zoom(16.0)
                                    .build()
                            }
                            ubicacion.cameraMode = CameraMode.TRACKING
                        }
                    }
                }
            }
        ) {
            Text("◎", modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }

        GlassPanel(
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            shape = RoundedCornerShape(7.dp)
        ) {
            Text("© OpenFreeMap · © OpenMapTiles · © OpenStreetMap", modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp), fontSize = 7.sp, color = Color.White)
        }

        if (!tienePermisoUbicacion) {
            GlassPanel(
                modifier = Modifier.align(Alignment.Center).padding(12.dp),
                accent = true,
                onClick = {
                    solicitarPermisos.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                }
            ) {
                Text("Activar ubicación GPS", modifier = Modifier.padding(12.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

private fun colorCategoriaReporte(categoria: String): Int = when (categoria) {
    "Infraestructura" -> AndroidColor.rgb(255, 183, 77)
    "Condiciones ambientales" -> AndroidColor.rgb(34, 197, 94)
    "Servicios escolares" -> AndroidColor.rgb(181, 156, 255)
    "Experiencia educativa" -> AndroidColor.rgb(94, 172, 255)
    "Entorno geográfico" -> AndroidColor.rgb(255, 126, 126)
    "Seguridad y accesibilidad" -> AndroidColor.rgb(255, 94, 112)
    else -> AndroidColor.rgb(190, 203, 220)
}

private fun crearIconoPunto(context: Context, color: Int): Icon {
    val density = context.resources.displayMetrics.density
    val diametro = (22f * density).toInt().coerceAtLeast(12)
    val bitmap = Bitmap.createBitmap(diametro, diametro, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centro = diametro / 2f
    val radio = diametro * 0.39f
    val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centro, centro, radio + 2.2f * density, borde)
    val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centro, centro, radio, relleno)
    return IconFactory.getInstance(context).fromBitmap(bitmap)
}

// =====================================================
// 9. COMPONENTES VISUALES
// =====================================================

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val brush = if (accent) {
        Brush.linearGradient(listOf(LuumTurquesaLogo.copy(alpha = 0.50f), LuumSuperficieAlta.copy(alpha = 0.85f), LuumSuperficie.copy(alpha = 0.90f)))
    } else {
        Brush.linearGradient(listOf(LuumSuperficieAlta.copy(alpha = 0.88f), LuumSuperficie.copy(alpha = 0.80f), LuumSuperficie.copy(alpha = 0.82f)))
    }

    val forma = modifier
        .clip(shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .background(brush)
        .border(1.dp, if (accent) LuumBordeTurquesa else BordeGlass, shape)

    Box(modifier = forma, content = content)
}

@Composable
fun Encabezado(
    rol: String,
    notificacionesNoLeidas: Int,
    abrirNotificaciones: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("LuumEdu", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Azul)
                Text("GEOSPATIAL EDUCATION INTELLIGENCE", fontSize = 9.sp, color = Gris, letterSpacing = 1.sp)
            }
            GlassPanel(
                shape = RoundedCornerShape(13.dp),
                accent = notificacionesNoLeidas > 0,
                onClick = abrirNotificaciones
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("Avisos", color = Texto, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    if (notificacionesNoLeidas > 0) {
                        Text(
                            notificacionesNoLeidas.coerceAtMost(99).toString(),
                            color = Fondo,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Azul, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            GlassPanel(shape = CircleShape, accent = true) {
                Text(rol.take(1).uppercase(), modifier = Modifier.padding(12.dp), color = Texto, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TituloSeccion(titulo: String, descripcion: String) {
    Text(titulo, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Texto)
    Spacer(Modifier.height(6.dp))
    Text(descripcion, fontSize = 14.sp, color = Gris)
    Spacer(Modifier.height(20.dp))
}

@Composable
fun TarjetaAccion(
    titulo: String,
    descripcion: String,
    accion: String,
    alPulsar: (() -> Unit)? = null
) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), onClick = alPulsar) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(titulo, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Texto)
            Spacer(Modifier.height(7.dp))
            Text(descripcion, fontSize = 13.sp, color = Gris, lineHeight = 19.sp)
            Spacer(Modifier.height(13.dp))
            Text(accion, color = Azul, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
    Spacer(Modifier.height(13.dp))
}

// =====================================================
// 10. INICIO
// =====================================================

@Composable
fun InicioScreen(
    rol: String,
    observaciones: List<Observacion>,
    instituciones: List<Institucion>,
    institucionDestacada: Institucion?,
    navegar: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text("OBSERVATORIO ESCOLAR", color = LuumVerde, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Text("El entorno\ntambién educa.", fontSize = 31.sp, fontWeight = FontWeight.Bold, color = Texto, lineHeight = 37.sp)
        Spacer(Modifier.height(9.dp))
        Text(
            "Explora las condiciones geográficas, ambientales y sociales que rodean a las instituciones educativas.",
            fontSize = 14.sp,
            color = Gris,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(25.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ENTORNO GEOGRÁFICO", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Azul)
            Text("MAPA INTELIGENTE", fontSize = 9.sp, color = LuumVerde, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(11.dp))
        val formaMapa = RoundedCornerShape(22.dp)
        Box(
            modifier = Modifier.fillMaxWidth().height(370.dp).clip(formaMapa).border(1.dp, LuumBordeTurquesa, formaMapa)
        ) {
            MapaGeografico(
                modifier = Modifier.fillMaxSize(),
                instituciones = instituciones,
                observaciones = observaciones,
                institucionDestacada = institucionDestacada
            )
        }
        Spacer(Modifier.height(9.dp))
        Text(
            "Los marcadores de reportes respetan los permisos de tu cuenta: estudiantes ven sus observaciones y docentes o directores ven las de su institución.",
            color = Gris,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(26.dp))
        Text("ACTIVIDAD DE LUUMEDU", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Gris)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResumenCard("Reportes", observaciones.size.toString(), Modifier.weight(1f))
            ResumenCard("Instituciones", instituciones.size.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        Text("OBSERVAR Y PARTICIPAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Gris)
        Spacer(Modifier.height(12.dp))
        if (rol == "Director") {
            TarjetaAccion(
                "Seguimiento de reportes",
                "Consulta las observaciones de tu institución y actualiza su estado de atención.",
                "Gestionar reportes →"
            ) { navegar("Reportes") }
        } else {
            TarjetaAccion(
                "Documentar una observación",
                "Registra una situación del entorno escolar y aporta evidencia.",
                "Crear observación →"
            ) { navegar("Reportar") }
        }
        TarjetaAccion("Consultar instituciones", "Explora preparatorias y universidades participantes.", "Explorar directorio →") { navegar("Escuela") }
        TarjetaAccion("Investigar y aprender", "Convierte las situaciones de tu entorno en experiencias educativas.", "Ver actividades →") { navegar("Aprender") }
        TarjetaAccion("Encuestas y censos escolares", "Participa en evaluaciones sobre infraestructura, servicios, educación y medio ambiente.", "Consultar encuestas →") { navegar("Encuestas") }
        Spacer(Modifier.height(10.dp))
        Text("Sesión: $rol", color = Gris, fontSize = 11.sp)
    }
}

@Composable
fun ResumenCard(titulo: String, cantidad: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(17.dp)) {
            Text(cantidad, fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Azul)
            Spacer(Modifier.height(5.dp))
            Text(titulo, color = Gris, fontSize = 12.sp)
        }
    }
}

// =====================================================
// 11. DIRECTORIO Y REGISTRO DE INSTITUCIONES CON GPS
// =====================================================

@Composable
fun EscuelaScreen(
    perfil: PerfilUsuario,
    institucionPropia: Institucion?,
    instituciones: List<Institucion>,
    registrar: (String, String, String, Location, (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var nombre by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf("Preparatoria") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var ubicacionCapturada by remember { mutableStateOf<Location?>(null) }
    var obteniendoUbicacion by remember { mutableStateOf(false) }
    var mensaje by rememberSaveable { mutableStateOf("") }

    val permisosUbicacion = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    val tienePermiso = {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val solicitarPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true || permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) {
            mensaje = "Permiso concedido. Pulsa nuevamente para obtener la ubicación del campus."
        } else {
            mensaje = "Necesitamos permiso de ubicación para registrar la escuela."
        }
    }

    fun capturarUbicacion() {
        if (!tienePermiso()) {
            solicitarPermiso.launch(permisosUbicacion)
            return
        }
        obteniendoUbicacion = true
        mensaje = "Obteniendo coordenadas GPS..."
        obtenerUbicacionActual(context) { ubicacion ->
            obteniendoUbicacion = false
            if (ubicacion == null) {
                ubicacionCapturada = null
                mensaje = "No se pudo obtener la ubicación. Activa el GPS e inténtalo otra vez."
            } else if (ubicacion.accuracy > PRECISION_MAXIMA_METROS) {
                ubicacionCapturada = null
                mensaje = "La precisión actual es de ±${ubicacion.accuracy.toInt()} m. Necesitamos 30 m o menos. Inténtalo al aire libre."
            } else {
                ubicacionCapturada = ubicacion
                mensaje = "Ubicación capturada con precisión de ±${ubicacion.accuracy.toInt()} m."
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        TituloSeccion("Directorio educativo", "Preparatorias y universidades con ubicación geográfica.")

        if (perfil.rol == "director" && perfil.institucionId == null) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
                Column(Modifier.padding(17.dp)) {
                    Text("REGISTRAR INSTITUCIÓN", color = Azul, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(15.dp))
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it; mensaje = "" }, label = { Text("Nombre de la institución") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    Text("Nivel educativo", color = Gris, fontSize = 13.sp)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = tipo == "Preparatoria", onClick = { tipo = "Preparatoria" }, label = { Text("Preparatoria") })
                        FilterChip(selected = tipo == "Universidad", onClick = { tipo = "Universidad" }, label = { Text("Universidad") })
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = direccion,
                        onValueChange = { direccion = it; mensaje = "" },
                        label = { Text("Dirección o referencia") },
                        placeholder = { Text("Ciudad, estado y país") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("UBICACIÓN AUTOMÁTICA DEL CAMPUS", color = Azul, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        ubicacionCapturada?.let { "GPS capturado · ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)} · precisión ±${it.accuracy.toInt()} m" }
                            ?: "Todavía no has capturado la ubicación del campus.",
                        color = Gris,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { capturarUbicacion() }, modifier = Modifier.fillMaxWidth(), enabled = !obteniendoUbicacion) {
                        if (obteniendoUbicacion) {
                            CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (obteniendoUbicacion) "Obteniendo GPS..." else "Detectar ubicación del campus")
                    }
                    Spacer(Modifier.height(15.dp))
                    Button(
                        onClick = {
                            val ubicacion = ubicacionCapturada
                            if (ubicacion != null) {
                                registrar(nombre.trim(), tipo, direccion.trim(), ubicacion) { correcto, texto ->
                                    mensaje = texto
                                    if (correcto) {
                                        nombre = ""
                                        direccion = ""
                                        ubicacionCapturada = null
                                    }
                                }
                            }
                        },
                        enabled = nombre.isNotBlank() && direccion.isNotBlank() && ubicacionCapturada != null && !obteniendoUbicacion,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Enviar institución para aprobación", fontWeight = FontWeight.Bold) }

                    if (mensaje.isNotBlank()) {
                        Spacer(Modifier.height(11.dp))
                        Text(mensaje, color = if (mensaje.startsWith("Solicitud enviada") || mensaje.startsWith("Ubicación capturada")) LuumExito else LuumError, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    Spacer(Modifier.height(9.dp))
                    Text("La ubicación se obtiene desde el teléfono. No se permite escribir latitud ni longitud manualmente.", color = Gris, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
        } else if (perfil.rol == "director" && perfil.institucionId != null) {
            val estado = institucionPropia?.estado ?: "pending"
            TarjetaAccion(
                "Institución vinculada",
                institucionPropia?.let { "${it.nombre}\n${it.direccion}" } ?: "Tu solicitud de institución fue enviada.",
                if (estado == "active") "Institución aprobada" else "Pendiente de revisión administrativa"
            )
        } else {
            TarjetaAccion(
                "Registro de instituciones",
                "El registro de escuelas está reservado para directores autorizados por LuumEdu.",
                "Tu perfil actual: ${etiquetaRol(perfil.rol)}"
            )
        }

        Spacer(Modifier.height(25.dp))
        Text("INSTITUCIONES ACTIVAS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Gris)
        Spacer(Modifier.height(13.dp))
        if (instituciones.isEmpty()) {
            Text("Todavía no hay instituciones aprobadas para mostrar.", color = Gris, fontSize = 13.sp)
        } else {
            instituciones.forEach { institucion ->
                TarjetaAccion(
                    titulo = institucion.nombre,
                    descripcion = "${institucion.tipo}\n${institucion.direccion}",
                    accion = "Ubicación disponible en el mapa"
                )
            }
        }
    }
}

// =====================================================
// BANDEJA DE NOTIFICACIONES PRIVADAS
// =====================================================

@Composable
fun NotificacionesScreen(
    notificaciones: List<NotificacionAula>,
    error: String,
    marcarComoLeida: (NotificacionAula) -> Unit,
    marcarTodasComoLeidas: () -> Unit,
    abrirEncuestas: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        TituloSeccion(
            "Notificaciones",
            "Novedades sobre tus reportes y encuestas de tu institución."
        )

        val pendientes = notificaciones.count { !it.leida }
        if (pendientes > 0) {
            OutlinedButton(
                onClick = marcarTodasComoLeidas,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Marcar todas como leídas ($pendientes)")
            }
            Spacer(Modifier.height(12.dp))
        }

        if (error.isNotBlank()) {
            Text("No se pudieron cargar las notificaciones: $error", color = LuumAmbar, fontSize = 12.sp)
        } else if (notificaciones.isEmpty()) {
            GlassPanel(Modifier.fillMaxWidth()) {
                Text(
                    "Todavía no tienes notificaciones. Te avisaremos cuando haya novedades sobre tus reportes o se publique una encuesta para tu institución.",
                    modifier = Modifier.padding(17.dp),
                    color = Gris,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        } else {
            notificaciones.forEach { aviso ->
                GlassPanel(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp),
                    accent = !aviso.leida
                ) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                aviso.titulo,
                                modifier = Modifier.weight(1f),
                                color = Texto,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (!aviso.leida) {
                                Text("NUEVA", color = Azul, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(aviso.mensaje, color = Gris, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        val estadoAviso = if (aviso.tipo == "survey") {
                            if (aviso.estadoReporte == "closed") "Encuesta cerrada" else "Encuesta disponible"
                        } else {
                            etiquetaEstadoReporte(aviso.estadoReporte)
                        }
                        Text(
                            "$estadoAviso · ${formatoFechaReporte(aviso.fechaMillis)}",
                            color = LuumTurquesaClaro,
                            fontSize = 10.sp
                        )
                        if (aviso.tipo == "survey") {
                            TextButton(
                                onClick = {
                                    if (!aviso.leida) marcarComoLeida(aviso)
                                    abrirEncuestas()
                                },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 3.dp)
                            ) {
                                Text("Ver encuestas")
                            }
                        } else if (!aviso.leida) {
                            TextButton(
                                onClick = { marcarComoLeida(aviso) },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 3.dp)
                            ) {
                                Text("Marcar como leída")
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================
// 12. REPORTAR - CONTROL DE PRESENCIA PARA ESTUDIANTES
// =====================================================

@Composable
fun ReportarScreen(
    perfil: PerfilUsuario,
    institucion: Institucion?,
    observaciones: List<Observacion>,
    db: FirebaseFirestore,
    cargandoReportes: Boolean,
    errorReportes: String
) {
    val context = LocalContext.current
    var titulo by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf("Infraestructura") }
    var ubicacion by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var mensaje by rememberSaveable { mutableStateOf("") }
    var validandoUbicacion by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var menuCategorias by remember { mutableStateOf(false) }
    var reporteConMenu by remember { mutableStateOf<String?>(null) }

    val categorias = listOf(
        "Entorno geográfico",
        "Infraestructura",
        "Servicios escolares",
        "Experiencia educativa",
        "Condiciones ambientales",
        "Seguridad y accesibilidad",
        "Otro"
    )

    val rolPuedeReportar = perfil.rol == "student" || perfil.rol == "teacher"
    val requierePresenciaEnCampus = perfil.rol == "student"
    val permisosUbicacion = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val tienePermisoUbicacion = {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val solicitarPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        mensaje = if (concedido) {
            "Permiso concedido. Pulsa nuevamente para verificar y enviar el reporte."
        } else {
            "Para registrar un reporte georreferenciado debes permitir el acceso a la ubicación."
        }
    }

    fun enviarReporteConUbicacion(actual: Location) {
        val escuela = institucion
        if (escuela == null || escuela.estado != "active") {
            validandoUbicacion = false
            mensaje = "Tu cuenta no tiene una institución activa vinculada. Contacta al administrador."
            return
        }

        if (actual.accuracy > PRECISION_MAXIMA_METROS) {
            validandoUbicacion = false
            mensaje = "La precisión actual es de ±${actual.accuracy.toInt()} m. Necesitamos 30 m o mejor. Inténtalo de nuevo en un lugar despejado."
            return
        }

        val distancia = distanciaMetros(
            actual.latitude,
            actual.longitude,
            escuela.latitud,
            escuela.longitud
        )

        if (requierePresenciaEnCampus && distancia > RADIO_OPINION_METROS) {
            validandoUbicacion = false
            mensaje = "No puedes publicar desde esta ubicación. Estás aproximadamente a ${distancia.toInt()} m del plantel; el límite es ${RADIO_OPINION_METROS.toInt()} m."
            return
        }

        // Se conserva el identificador interno que ya aceptan las reglas de Firestore.
        // La interfaz lo presenta traducido al español mediante etiquetaTurno().
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val turno = when (hora) {
            in 6..11 -> "morning"
            in 12..18 -> "afternoon"
            else -> "night"
        }

        val datos = hashMapOf<String, Any>(
            "title" to titulo.trim(),
            "category" to categoria,
            "locationLabel" to ubicacion.trim(),
            "description" to descripcion.trim(),
            "institutionId" to escuela.id,
            "authorUid" to perfil.uid,
            "authorRole" to perfil.rol,
            "status" to "new",
            "turn" to turno,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "latitude" to actual.latitude,
            "longitude" to actual.longitude,
            "gpsAccuracyMeters" to actual.accuracy.toDouble(),
            "distanceToCampusMeters" to distancia.toDouble(),
            "gpsVerificationMethod" to "android_fused_location"
        )

        guardando = true
        mensaje = "Guardando el reporte en LuumEdu..."
        db.collection("observations")
            .add(datos)
            .addOnSuccessListener {
                titulo = ""
                categoria = "Infraestructura"
                ubicacion = ""
                descripcion = ""
                mensaje = "Reporte guardado correctamente. Ya está sincronizado con tu institución."
                guardando = false
                validandoUbicacion = false
            }
            .addOnFailureListener { error ->
                mensaje = "No se pudo guardar el reporte: ${error.localizedMessage ?: "Error de conexión o permisos"}"
                guardando = false
                validandoUbicacion = false
            }
    }

    fun guardarReporte() {
        if (!rolPuedeReportar) {
            mensaje = "Por ahora, los reportes pueden ser publicados por estudiantes y docentes aprobados."
            return
        }
        if (titulo.isBlank() || descripcion.trim().length < 10) {
            mensaje = "Escribe un título y una descripción de al menos 10 caracteres."
            return
        }
        if (ubicacion.trim().isBlank()) {
            mensaje = "Indica el espacio donde observaste la situación (salón, patio, baños, etc.)."
            return
        }
        if (institucion == null || institucion.estado != "active") {
            mensaje = "Tu cuenta no tiene una institución activa vinculada."
            return
        }
        if (!tienePermisoUbicacion()) {
            solicitarPermiso.launch(permisosUbicacion)
            return
        }

        validandoUbicacion = true
        mensaje = "Obteniendo ubicación GPS y verificando el reporte..."
        obtenerUbicacionActual(context) { actual ->
            if (actual == null) {
                validandoUbicacion = false
                mensaje = "No se pudo obtener tu ubicación. Activa el GPS e inténtalo nuevamente."
            } else {
                enviarReporteConUbicacion(actual)
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        TituloSeccion(
            if (perfil.rol == "director") "Reportes de la institución" else "Nueva observación",
            if (perfil.rol == "director") {
                "Consulta las observaciones recibidas y da seguimiento a las acciones de mejora de tu escuela."
            } else {
                "Registra una situación real del entorno educativo. Tu reporte quedará asociado a tu institución."
            }
        )

        if (!rolPuedeReportar) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
                Column(Modifier.padding(15.dp)) {
                    Text(
                        if (perfil.rol == "director") "GESTIÓN DE REPORTES" else "PUBLICACIÓN DE REPORTES",
                        color = Azul,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        if (perfil.rol == "director") {
                            "Aquí puedes revisar los reportes de tu institución, actualizar su seguimiento y comunicar la resolución a quien lo registró."
                        } else {
                            "La publicación de observaciones está disponible para estudiantes y docentes autorizados."
                        },
                        color = Gris,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            GlassPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
                Column(Modifier.padding(16.dp)) {
                    Text("NUEVO REPORTE", color = Azul, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Institución: ${institucion?.nombre ?: "Cargando institución..."}",
                        color = Gris,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(15.dp))

                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it; mensaje = "" },
                        label = { Text("Título del reporte") },
                        placeholder = { Text("Ej. Temperatura elevada en el salón") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !guardando && !validandoUbicacion
                    )
                    Spacer(Modifier.height(12.dp))

                    Text("Categoría", color = Gris, fontSize = 12.sp)
                    Spacer(Modifier.height(5.dp))
                    Box {
                        OutlinedButton(
                            onClick = { menuCategorias = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !guardando && !validandoUbicacion
                        ) {
                            Text(categoria, modifier = Modifier.weight(1f))
                            Text("▾")
                        }
                        DropdownMenu(
                            expanded = menuCategorias,
                            onDismissRequest = { menuCategorias = false }
                        ) {
                            categorias.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = {
                                        categoria = opcion
                                        menuCategorias = false
                                        mensaje = ""
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ubicacion,
                        onValueChange = { ubicacion = it; mensaje = "" },
                        label = { Text("Espacio donde ocurrió") },
                        placeholder = { Text("Ej. Laboratorio, patio, salón 4...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !guardando && !validandoUbicacion
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it; mensaje = "" },
                        label = { Text("Describe lo que observaste") },
                        placeholder = { Text("Explica qué ocurrió, cuándo lo notaste y qué podría mejorar...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        enabled = !guardando && !validandoUbicacion
                    )
                    Spacer(Modifier.height(13.dp))

                    Text(
                        if (requierePresenciaEnCampus) {
                            "VALIDACIÓN DE PRESENCIA: para publicar, el GPS debe ubicarte dentro de ${RADIO_OPINION_METROS.toInt()} m de tu escuela, con precisión de 30 m o mejor."
                        } else {
                            "UBICACIÓN DEL REPORTE: se guardará la posición GPS desde donde envíes la observación, con precisión de 30 m o mejor."
                        },
                        color = Gris,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(15.dp))

                    Button(
                        onClick = { guardarReporte() },
                        enabled = !guardando && !validandoUbicacion && titulo.isNotBlank() && ubicacion.isNotBlank() && descripcion.trim().length >= 10,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        if (guardando || validandoUbicacion) {
                            CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            when {
                                validandoUbicacion -> "Verificando ubicación..."
                                guardando -> "Guardando reporte..."
                                else -> "Verificar ubicación y publicar"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (mensaje.isNotBlank()) {
                        Spacer(Modifier.height(11.dp))
                        Text(
                            mensaje,
                            color = if (mensaje.startsWith("Reporte guardado")) LuumExito else LuumError,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(25.dp))
        Text(
            if (perfil.rol == "student") "Mis reportes" else "Reportes de la institución",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Texto
        )
        Spacer(Modifier.height(10.dp))

        if (errorReportes.isNotBlank()) {
            Text("No se pudieron cargar los reportes: $errorReportes", color = LuumAmbar, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
        }

        if (cargandoReportes) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(9.dp))
                Text("Sincronizando reportes...", color = Gris, fontSize = 12.sp)
            }
        } else if (observaciones.isEmpty()) {
            Text("Todavía no hay reportes registrados.", color = Gris, fontSize = 13.sp)
        } else {
            observaciones.forEach { reporte ->
                val fecha = formatoFechaReporte(reporte.fechaMillis)
                Column(Modifier.fillMaxWidth()) {
                    TarjetaAccion(
                        titulo = reporte.titulo,
                        descripcion = "${reporte.categoria} · ${reporte.ubicacion}\n${reporte.descripcion}\nTurno: ${etiquetaTurno(reporte.turno)} · $fecha",
                        accion = etiquetaEstadoReporte(reporte.estado)
                    )
                    if (perfil.rol == "director") {
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { reporteConMenu = if (reporteConMenu == reporte.id) null else reporte.id },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Actualizar seguimiento")
                            }
                            DropdownMenu(
                                expanded = reporteConMenu == reporte.id,
                                onDismissRequest = { reporteConMenu = null }
                            ) {
                                listOf(
                                    "in_review" to "En revisión",
                                    "in_progress" to "En atención",
                                    "resolved" to "Resuelto",
                                    "verified" to "Mejora verificada",
                                    "rejected" to "No procedente"
                                ).forEach { (codigo, etiqueta) ->
                                    DropdownMenuItem(
                                        text = { Text(etiqueta) },
                                        onClick = {
                                            reporteConMenu = null
                                            val referenciaReporte = db.collection("observations").document(reporte.id)
                                            val lote = db.batch()
                                            val actualizacion = mutableMapOf<String, Any>(
                                                "status" to codigo,
                                                "updatedAt" to FieldValue.serverTimestamp()
                                            )
                                            if (codigo == "in_review") {
                                                actualizacion["reviewedAt"] = FieldValue.serverTimestamp()
                                            }
                                            if (codigo == "resolved" || codigo == "verified") {
                                                actualizacion["resolvedAt"] = FieldValue.serverTimestamp()
                                            }
                                            lote.update(referenciaReporte, actualizacion)

                                            // Los cambios finales generan un aviso privado para quien creó el reporte.
                                            if (codigo in setOf("resolved", "verified", "rejected") && reporte.autorUid.isNotBlank()) {
                                                val referenciaNotificacion = db.collection("users")
                                                    .document(reporte.autorUid)
                                                    .collection("notifications")
                                                    .document("${reporte.id}_$codigo")
                                                val tituloAviso = when (codigo) {
                                                    "resolved" -> "Tu reporte fue marcado como resuelto"
                                                    "verified" -> "La mejora de tu reporte fue verificada"
                                                    else -> "Tu reporte fue marcado como no procedente"
                                                }
                                                val cuerpoAviso = when (codigo) {
                                                    "resolved" -> "El director actualizó el reporte ‘${reporte.titulo}’ y lo marcó como resuelto."
                                                    "verified" -> "El director confirmó la mejora relacionada con ‘${reporte.titulo}’."
                                                    else -> "El director revisó ‘${reporte.titulo}’ y lo marcó como no procedente."
                                                }
                                                lote.set(
                                                    referenciaNotificacion,
                                                    mapOf(
                                                        "targetUid" to reporte.autorUid,
                                                        "createdByUid" to perfil.uid,
                                                        "institutionId" to (perfil.institucionId ?: ""),
                                                        "observationId" to reporte.id,
                                                        "observationTitle" to reporte.titulo,
                                                        "status" to codigo,
                                                        "title" to tituloAviso,
                                                        "body" to cuerpoAviso,
                                                        "read" to false,
                                                        "createdAt" to FieldValue.serverTimestamp()
                                                    )
                                                )
                                            }

                                            lote.commit()
                                                .addOnSuccessListener {
                                                    mensaje = if (codigo in setOf("resolved", "verified", "rejected") && reporte.autorUid.isNotBlank()) {
                                                        "Seguimiento actualizado y aviso guardado en la bandeja del autor del reporte."
                                                    } else {
                                                        "Seguimiento actualizado correctamente."
                                                    }
                                                }
                                                .addOnFailureListener { error ->
                                                    mensaje = "No se pudo actualizar el reporte: ${error.localizedMessage ?: "Error de permisos"}"
                                                }
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Privacidad: los estudiantes consultan sus propios reportes. Los docentes y directores aprobados pueden consultar los reportes de su institución. Las opiniones no se publican abiertamente en esta etapa.",
            color = Gris,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "Las fotografías se incorporarán en una siguiente etapa de almacenamiento de evidencias. Los reportes actuales guardan texto, fecha, turno y ubicación GPS en Firestore.",
            color = Gris,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
    }
}

// =====================================================
// 13. ENCUESTAS Y CENSOS ESCOLARES
// =====================================================

@Composable
private fun EncuestasScreen(
    perfil: PerfilUsuario,
    institucion: Institucion?,
    encuestas: List<Encuesta>,
    db: FirebaseFirestore,
    cargando: Boolean,
    error: String
) {
    var mostrarCrear by rememberSaveable { mutableStateOf(false) }
    var encuestaSeleccionada by remember { mutableStateOf<Encuesta?>(null) }
    var encuestaResultados by remember { mutableStateOf<Encuesta?>(null) }
    var mensaje by remember { mutableStateOf("") }
    var guardandoEncuesta by remember { mutableStateOf(false) }
    val encuestasRespondidasEnSesion = remember(perfil.uid) { mutableStateListOf<String>() }

    val esDirector = perfil.rol == "director"
    val puedeResponder = perfil.rol in setOf("student", "teacher", "parent")
    val encuestasVisibles = if (esDirector) {
        encuestas
    } else {
        encuestas.filter { it.estado == "published" || it.estado == "closed" }
    }

    Column(Modifier.fillMaxWidth()) {
        TituloSeccion(
            "Encuestas y censos",
            "Conoce la percepción de la comunidad y ayuda a identificar oportunidades de mejora."
        )

        GlassPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
            Column(Modifier.padding(15.dp)) {
                Text("EVALUACIÓN INSTITUCIONAL", color = Azul, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text("Institución: ${institucion?.nombre ?: "Sin institución vinculada"}", color = Texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    if (esDirector) {
                        "Publica cuestionarios para estudiantes, docentes y padres de familia. Podrás consultar cuántas personas participaron."
                    } else {
                        "Responde las encuestas de tu institución. Cada cuenta puede enviar una respuesta por encuesta."
                    },
                    color = Gris,
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
            }
        }

        if (esDirector) {
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { mostrarCrear = true },
                enabled = institucion?.estado == "active" && !guardandoEncuesta,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Crear nueva encuesta", fontWeight = FontWeight.Bold)
            }
            if (institucion?.estado != "active") {
                Spacer(Modifier.height(7.dp))
                Text("Tu institución debe estar activa para publicar encuestas.", color = LuumAmbar, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            if (esDirector) "ENCUESTAS DE MI INSTITUCIÓN" else "ENCUESTAS DISPONIBLES",
            color = Gris,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(10.dp))

        if (error.isNotBlank()) {
            Text("No se pudieron cargar las encuestas: $error", color = LuumAmbar, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(8.dp))
        }

        if (cargando) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(9.dp))
                Text("Consultando encuestas...", color = Gris, fontSize = 12.sp)
            }
        } else if (encuestasVisibles.isEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (esDirector) "Todavía no has publicado encuestas. Crea la primera para comenzar a recopilar información." else "Todavía no hay encuestas publicadas para tu institución.",
                    modifier = Modifier.padding(16.dp),
                    color = Gris,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        } else {
            encuestasVisibles.forEach { encuesta ->
                TarjetaEncuesta(
                    encuesta = encuesta,
                    perfil = perfil,
                    db = db,
                    puedeResponder = puedeResponder,
                    esDirector = esDirector,
                    respondidaEnSesion = encuestasRespondidasEnSesion.contains(encuesta.id),
                    alResponder = {
                        db.collection("users")
                            .document(perfil.uid)
                            .collection("surveyReads")
                            .document(encuesta.id)
                            .set(
                                mapOf(
                                    "surveyId" to encuesta.id,
                                    "institutionId" to (perfil.institucionId ?: ""),
                                    "readAt" to FieldValue.serverTimestamp()
                                )
                            )
                        encuestaSeleccionada = encuesta
                    },
                    alVerResultados = { encuestaResultados = encuesta },
                    alCerrar = {
                        db.collection("surveys").document(encuesta.id)
                            .update(
                                mapOf(
                                    "status" to "closed",
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            .addOnSuccessListener { mensaje = "La encuesta se cerró correctamente." }
                            .addOnFailureListener { ex -> mensaje = "No se pudo cerrar la encuesta: ${ex.localizedMessage ?: "Error de permisos"}" }
                    }
                )
            }
        }

        if (mensaje.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(mensaje, color = if (mensaje.startsWith("Encuesta publicada") || mensaje.startsWith("La encuesta se cerró")) LuumExito else LuumError, fontSize = 11.sp, lineHeight = 16.sp)
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Privacidad: las respuestas individuales no son visibles para otros participantes ni para el director. Los resultados se muestran agrupados a partir de 5 participaciones; las respuestas abiertas no se reproducen en el panel.",
            color = Gris,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
    }

    if (mostrarCrear) {
        CrearEncuestaDialog(
            publicando = guardandoEncuesta,
            onDismiss = { if (!guardandoEncuesta) mostrarCrear = false },
            onPublicar = { titulo, descripcion, categoria, preguntas, terminar ->
                val institutionId = perfil.institucionId
                if (institutionId.isNullOrBlank() || institucion?.estado != "active") {
                    terminar(false, "No se encontró una institución activa vinculada al director.")
                } else {
                    guardandoEncuesta = true
                    val referenciaEncuesta = db.collection("surveys").document()
                    val datos = hashMapOf<String, Any>(
                        "title" to titulo,
                        "description" to descripcion,
                        "category" to categoria,
                        "institutionId" to institutionId,
                        "createdByUid" to perfil.uid,
                        // La encuesta permanece como borrador mientras se preparan
                        // sus documentos estadísticos. Los estudiantes no la verán.
                        "status" to "draft",
                        "questions" to preguntas,
                        "responseCount" to 0L,
                        "statisticsVersion" to 1L,
                        "lastResponseBatchToken" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    // Primero se crea solo el documento principal. Así evitamos
                    // exceder el límite de llamadas de Rules de un lote grande.
                    referenciaEncuesta.set(datos)
                        .addOnSuccessListener {
                            // Cada agregado se crea en una petición independiente.
                            // Las reglas comprueban que el borrador pertenezca al director.
                            val tareasAgregados = preguntas.mapIndexed { indice, pregunta ->
                                val questionId = pregunta["id"] as? String ?: "pregunta_${indice + 1}"
                                val type = pregunta["type"] as? String ?: "single_choice"
                                val options = pregunta["options"] as? List<*> ?: emptyList<Any>()
                                val optionCounts = if (type == "open_text") {
                                    emptyMap<String, Any>()
                                } else {
                                    options.indices.associate { opcion -> "o$opcion" to 0L }
                                }
                                val referenciaEstadistica = referenciaEncuesta
                                    .collection("aggregates")
                                    .document(questionId)

                                referenciaEstadistica.set(
                                    mapOf(
                                        "questionId" to questionId,
                                        "questionIndex" to indice.toLong(),
                                        "institutionId" to institutionId,
                                        "type" to type,
                                        "totalCount" to 0L,
                                        "optionCounts" to optionCounts,
                                        "lastBucketKey" to "",
                                        "lastBatchToken" to "",
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                            }

                            Tasks.whenAllComplete(tareasAgregados)
                                .addOnCompleteListener {
                                    val falloAgregado = tareasAgregados.firstOrNull { !it.isSuccessful }?.exception

                                    if (falloAgregado != null) {
                                        // Si algún agregado falla, no publicamos la encuesta.
                                        // Se intenta retirar lo que alcanzó a crearse y el borrador.
                                        referenciaEncuesta.collection("aggregates").get()
                                            .addOnSuccessListener { snapshotAgregados ->
                                                val tareasBorrado = snapshotAgregados.documents.map { documento ->
                                                    documento.reference.delete()
                                                }
                                                Tasks.whenAllComplete(tareasBorrado)
                                                    .addOnCompleteListener {
                                                        referenciaEncuesta.delete()
                                                            .addOnCompleteListener {
                                                                guardandoEncuesta = false
                                                                val detalle = "No se pudieron preparar los indicadores de la encuesta. No se publicó. Detalle: ${falloAgregado.localizedMessage ?: "Permiso denegado"}"
                                                                mensaje = detalle
                                                                terminar(false, detalle)
                                                            }
                                                    }
                                            }
                                            .addOnFailureListener { errorLimpieza ->
                                                guardandoEncuesta = false
                                                val detalle = "La encuesta quedó como borrador y no es visible para los participantes. No se pudieron completar los indicadores: ${falloAgregado.localizedMessage ?: "Permiso denegado"}. Limpieza: ${errorLimpieza.localizedMessage ?: "no disponible"}"
                                                mensaje = detalle
                                                terminar(false, detalle)
                                            }
                                    } else {
                                        // Solo se publica cuando todos los agregados existen.
                                        referenciaEncuesta.update(
                                            mapOf(
                                                "status" to "published",
                                                "publishedAt" to FieldValue.serverTimestamp(),
                                                "updatedAt" to FieldValue.serverTimestamp()
                                            )
                                        )
                                            .addOnSuccessListener {
                                                guardandoEncuesta = false
                                                mensaje = "Encuesta publicada correctamente. Los resultados estadísticos se actualizarán con cada respuesta."
                                                terminar(true, "Encuesta publicada correctamente.")
                                            }
                                            .addOnFailureListener { error ->
                                                guardandoEncuesta = false
                                                val detalle = "Los indicadores se guardaron, pero no se pudo publicar la encuesta. Detalle: ${error.localizedMessage ?: "Permiso denegado"}"
                                                mensaje = detalle
                                                terminar(false, detalle)
                                            }
                                    }
                                }
                        }
                        .addOnFailureListener { ex ->
                            guardandoEncuesta = false
                            val detalle = "No se pudo crear el borrador de la encuesta: ${ex.localizedMessage ?: "Revisa las reglas de Firestore"}"
                            mensaje = detalle
                            terminar(false, detalle)
                        }
                }
            }
        )
    }

    encuestaSeleccionada?.let { encuesta ->
        ResponderEncuestaDialog(
            encuesta = encuesta,
            perfil = perfil,
            onDismiss = { encuestaSeleccionada = null },
            onEnviar = { respuestas, terminar ->
                val institutionId = perfil.institucionId
                if (institutionId.isNullOrBlank()) {
                    terminar(false, "Tu cuenta no tiene una institución vinculada.")
                } else {
                    val respuestaInvalida = encuesta.preguntas.firstOrNull { pregunta ->
                        when (pregunta.tipo) {
                            "single_choice" -> pregunta.opciones.indexOf(respuestas[pregunta.id].orEmpty()) !in pregunta.opciones.indices
                            "scale_1_5" -> (respuestas[pregunta.id]?.toIntOrNull()?.minus(1) ?: -1) !in pregunta.opciones.indices
                            else -> false
                        }
                    }
                    if (respuestaInvalida != null) {
                        terminar(false, "La respuesta de '${respuestaInvalida.texto}' no coincide con las opciones de la encuesta.")
                    } else {
                        val referenciaEncuesta = db.collection("surveys").document(encuesta.id)
                        val referenciaRespuesta = referenciaEncuesta.collection("responses").document(perfil.uid)
                        val lote = db.batch()
                        val marcadorLote = java.util.UUID.randomUUID().toString()
                        lote.set(
                            referenciaRespuesta,
                            mapOf(
                                "institutionId" to institutionId,
                                "respondentRole" to perfil.rol,
                                "answers" to respuestas,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                        lote.update(
                            referenciaEncuesta,
                            mapOf(
                                "responseCount" to FieldValue.increment(1),
                                "lastResponseBatchToken" to marcadorLote,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )

                        if (encuesta.versionEstadisticas >= 1L) {
                            encuesta.preguntas.forEach { pregunta ->
                                val respuesta = respuestas[pregunta.id].orEmpty()
                                val referenciaAgregado = referenciaEncuesta.collection("aggregates")
                                    .document(pregunta.id)
                                val bucketIndex = when (pregunta.tipo) {
                                    "single_choice" -> pregunta.opciones.indexOf(respuesta)
                                    "scale_1_5" -> respuesta.toIntOrNull()?.minus(1) ?: -1
                                    else -> -1
                                }
                                val bucketKey = if (bucketIndex >= 0) "o$bucketIndex" else ""
                                if (pregunta.tipo == "open_text") {
                                    lote.update(
                                        referenciaAgregado,
                                        mapOf(
                                            "totalCount" to FieldValue.increment(1),
                                            "lastBucketKey" to "",
                                            "lastBatchToken" to marcadorLote,
                                            "updatedAt" to FieldValue.serverTimestamp()
                                        )
                                    )
                                } else {
                                    lote.update(
                                        referenciaAgregado,
                                        "totalCount", FieldValue.increment(1),
                                        "lastBucketKey", bucketKey,
                                        "lastBatchToken", marcadorLote,
                                        "updatedAt", FieldValue.serverTimestamp(),
                                        "optionCounts.o$bucketIndex", FieldValue.increment(1)
                                    )
                                }
                            }
                        }

                        lote.commit()
                            .addOnSuccessListener {
                                if (!encuestasRespondidasEnSesion.contains(encuesta.id)) {
                                    encuestasRespondidasEnSesion.add(encuesta.id)
                                }
                                terminar(true, "Tu respuesta se registró correctamente.")
                            }
                            .addOnFailureListener { ex ->
                                val detalle = ex.localizedMessage ?: "Error de conexión o permisos"
                                terminar(false, "No se pudo registrar tu respuesta. Es posible que ya hayas contestado esta encuesta. $detalle")
                            }
                    }
                }
            }
        )
    }

    encuestaResultados?.let { encuesta ->
        ResultadosEncuestaDialog(
            encuesta = encuesta,
            db = db,
            onDismiss = { encuestaResultados = null }
        )
    }
}

@Composable
private fun TarjetaEncuesta(
    encuesta: Encuesta,
    perfil: PerfilUsuario,
    db: FirebaseFirestore,
    puedeResponder: Boolean,
    esDirector: Boolean,
    respondidaEnSesion: Boolean,
    alResponder: () -> Unit,
    alVerResultados: () -> Unit,
    alCerrar: () -> Unit
) {
    var yaRespondio by remember(encuesta.id, perfil.uid) { mutableStateOf(false) }
    var comprobandoRespuesta by remember(encuesta.id, perfil.uid) { mutableStateOf(puedeResponder && encuesta.estado == "published") }
    var errorRespuesta by remember(encuesta.id, perfil.uid) { mutableStateOf("") }

    LaunchedEffect(encuesta.id, perfil.uid, encuesta.estado, puedeResponder) {
        if (puedeResponder && encuesta.estado == "published") {
            comprobandoRespuesta = true
            db.collection("surveys").document(encuesta.id)
                .collection("responses").document(perfil.uid)
                .get()
                .addOnSuccessListener { documento ->
                    yaRespondio = documento.exists()
                    comprobandoRespuesta = false
                }
                .addOnFailureListener { ex ->
                    errorRespuesta = ex.localizedMessage ?: "No se pudo comprobar tu respuesta."
                    comprobandoRespuesta = false
                }
        } else {
            comprobandoRespuesta = false
        }
    }

    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        accent = encuesta.estado == "published"
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(encuesta.titulo, modifier = Modifier.weight(1f), color = Texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    etiquetaEstadoEncuesta(encuesta.estado),
                    color = if (encuesta.estado == "published") LuumExito else Gris,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(encuesta.descripcion, color = Gris, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(8.dp))
            Text("${encuesta.categoria} · ${encuesta.preguntas.size} preguntas", color = Azul, fontSize = 10.sp)
            Spacer(Modifier.height(5.dp))
            Text(
                if (esDirector) "Participaciones recibidas: ${encuesta.cantidadRespuestas}" else "Fecha de publicación: ${formatoFechaReporte(encuesta.fechaMillis)}",
                color = Gris,
                fontSize = 10.sp
            )
            if (puedeResponder) {
                Spacer(Modifier.height(10.dp))
                when {
                    encuesta.estado != "published" -> Text("Esta encuesta está cerrada.", color = Gris, fontSize = 11.sp)
                    comprobandoRespuesta -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(7.dp))
                        Text("Comprobando participación...", color = Gris, fontSize = 10.sp)
                    }
                    yaRespondio || respondidaEnSesion -> Text("Ya enviaste tu respuesta. Gracias por participar.", color = LuumExito, fontSize = 11.sp)
                    else -> Button(onClick = alResponder, modifier = Modifier.fillMaxWidth()) { Text("Responder encuesta") }
                }
                if (errorRespuesta.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(errorRespuesta, color = LuumAmbar, fontSize = 10.sp)
                }
            }
            if (esDirector) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = alVerResultados,
                    enabled = encuesta.versionEstadisticas >= 1L && encuesta.cantidadRespuestas >= 5,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver resultados estadísticos")
                }
                if (encuesta.versionEstadisticas < 1L) {
                    Text(
                        "Esta encuesta es anterior al módulo estadístico. Las nuevas encuestas recopilarán resultados agrupados.",
                        color = Gris, fontSize = 10.sp, lineHeight = 15.sp
                    )
                } else if (encuesta.cantidadRespuestas < 5) {
                    Text(
                        "Los resultados se mostrarán cuando haya al menos 5 participaciones.",
                        color = Gris, fontSize = 10.sp
                    )
                }
                if (encuesta.estado == "published") {
                    OutlinedButton(onClick = alCerrar, modifier = Modifier.fillMaxWidth()) {
                        Text("Cerrar encuesta")
                    }
                }
            }
        }
    }
}

@Composable
private fun CrearEncuestaDialog(
    publicando: Boolean,
    onDismiss: () -> Unit,
    onPublicar: (String, String, String, List<Map<String, Any>>, (Boolean, String) -> Unit) -> Unit
) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf("Infraestructura") }
    var categoriaMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val preguntas = remember { mutableStateListOf(BorradorPregunta()) }
    val categorias = listOf(
        "Entorno geográfico",
        "Infraestructura",
        "Servicios escolares",
        "Experiencia educativa",
        "Condiciones ambientales",
        "Seguridad y accesibilidad",
        "General"
    )

    val preguntasValidas = preguntas.isNotEmpty() && preguntas.all { pregunta ->
        pregunta.texto.trim().length >= 5 &&
                (pregunta.tipo != "single_choice" || pregunta.opcionesTexto.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct().size in 2..10)
    }
    val puedePublicar = titulo.trim().length >= 4 && descripcion.trim().length >= 10 && preguntasValidas && !publicando

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear encuesta") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("La encuesta se publicará para los miembros aprobados de tu institución.", color = Gris, fontSize = 11.sp)
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it; error = "" },
                    label = { Text("Título de la encuesta") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it; error = "" },
                    label = { Text("Objetivo o descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                Box {
                    OutlinedButton(onClick = { categoriaMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(categoria, modifier = Modifier.weight(1f))
                        Text("▾")
                    }
                    DropdownMenu(expanded = categoriaMenu, onDismissRequest = { categoriaMenu = false }) {
                        categorias.forEach { opcion ->
                            DropdownMenuItem(text = { Text(opcion) }, onClick = { categoria = opcion; categoriaMenu = false })
                        }
                    }
                }
                Divider()
                Text("PREGUNTAS", color = Azul, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                preguntas.forEachIndexed { indice, pregunta ->
                    GlassPanel(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pregunta ${indice + 1}", color = Texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 12.sp)
                                if (preguntas.size > 1) {
                                    TextButton(onClick = { preguntas.remove(pregunta) }) { Text("Eliminar", color = LuumAmbar, fontSize = 10.sp) }
                                }
                            }
                            OutlinedTextField(
                                value = pregunta.texto,
                                onValueChange = { pregunta.texto = it; error = "" },
                                label = { Text("Escribe la pregunta") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Tipo de respuesta", color = Gris, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = pregunta.tipo == "single_choice",
                                    onClick = { pregunta.tipo = "single_choice" },
                                    label = { Text("Opciones", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = pregunta.tipo == "scale_1_5",
                                    onClick = { pregunta.tipo = "scale_1_5" },
                                    label = { Text("Escala 1–5", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = pregunta.tipo == "open_text",
                                    onClick = { pregunta.tipo = "open_text" },
                                    label = { Text("Abierta", fontSize = 10.sp) }
                                )
                            }
                            if (pregunta.tipo == "single_choice") {
                                OutlinedTextField(
                                    value = pregunta.opcionesTexto,
                                    onValueChange = { pregunta.opcionesTexto = it; error = "" },
                                    label = { Text("Opciones separadas por coma") },
                                    placeholder = { Text("Sí, No, No estoy seguro") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 1,
                                    maxLines = 2
                                )
                            } else if (pregunta.tipo == "scale_1_5") {
                                Text("La persona responderá con una escala del 1 (muy bajo) al 5 (muy alto).", color = Gris, fontSize = 10.sp)
                            } else {
                                Text("La respuesta será de texto libre. Se recomienda no escribir nombres ni datos personales.", color = Gris, fontSize = 10.sp)
                            }
                        }
                    }
                }
                TextButton(
                    onClick = {
                        if (preguntas.size < 10) preguntas.add(BorradorPregunta())
                    },
                    enabled = preguntas.size < 10
                ) {
                    Text("+ Agregar pregunta")
                }
                if (error.isNotBlank()) {
                    Text(error, color = LuumAmbar, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!puedePublicar) {
                        error = "Completa el título, la descripción y todas las preguntas. Las preguntas de opciones necesitan entre 2 y 10 opciones distintas."
                    } else {
                        val preguntasFirestore = preguntas.mapIndexed { indice, pregunta ->
                            val opciones = when (pregunta.tipo) {
                                "single_choice" -> pregunta.opcionesTexto.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
                                "scale_1_5" -> listOf("1", "2", "3", "4", "5")
                                else -> emptyList()
                            }
                            mapOf<String, Any>(
                                "id" to "pregunta_${indice + 1}",
                                "text" to pregunta.texto.trim(),
                                "type" to pregunta.tipo,
                                "options" to opciones,
                                "required" to true
                            )
                        }
                        onPublicar(titulo.trim(), descripcion.trim(), categoria, preguntasFirestore) { correcto, texto ->
                            if (correcto) {
                                error = ""
                                onDismiss()
                            } else {
                                error = texto
                            }
                        }
                    }
                },
                enabled = puedePublicar
            ) {
                if (publicando) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Publicar encuesta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !publicando) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ResultadosEncuestaDialog(
    encuesta: Encuesta,
    db: FirebaseFirestore,
    onDismiss: () -> Unit
) {
    var estadisticas by remember(encuesta.id) { mutableStateOf<List<EstadisticaPregunta>>(emptyList()) }
    var cargando by remember(encuesta.id) { mutableStateOf(true) }
    var error by remember(encuesta.id) { mutableStateOf("") }

    DisposableEffect(db, encuesta.id, encuesta.versionEstadisticas, encuesta.cantidadRespuestas) {
        if (encuesta.versionEstadisticas < 1L || encuesta.cantidadRespuestas < 5) {
            cargando = false
            onDispose { }
        } else {
            val registro = db.collection("surveys").document(encuesta.id)
                .collection("aggregates")
                .addSnapshotListener { instantanea, fallo ->
                    if (fallo != null) {
                        error = fallo.localizedMessage ?: "No se pudieron consultar los resultados."
                        cargando = false
                    } else if (instantanea != null) {
                        estadisticas = instantanea.documents
                            .mapNotNull { it.aEstadisticaPregunta() }
                            .sortedBy { it.indice }
                        error = ""
                        cargando = false
                    }
                }
            onDispose { registro.remove() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resultados de la encuesta") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(encuesta.titulo, color = Texto, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${encuesta.cantidadRespuestas} participaciones registradas", color = Azul, fontSize = 11.sp)
                if (encuesta.versionEstadisticas < 1L) {
                    Text(
                        "Esta encuesta se publicó antes de habilitar las estadísticas agrupadas. Sus respuestas anteriores no se pueden reconstruir sin consultar los registros individuales.",
                        color = Gris, fontSize = 12.sp, lineHeight = 18.sp
                    )
                } else if (encuesta.cantidadRespuestas < 5) {
                    Text(
                        "Por privacidad, las distribuciones se mostrarán cuando haya al menos 5 participaciones.",
                        color = Gris, fontSize = 12.sp, lineHeight = 18.sp
                    )
                } else if (cargando) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Calculando resultados agrupados...", color = Gris, fontSize = 11.sp)
                    }
                } else if (error.isNotBlank()) {
                    Text("No se pudieron cargar las estadísticas: $error", color = LuumAmbar, fontSize = 11.sp)
                } else if (estadisticas.isEmpty()) {
                    Text(
                        "Todavía no hay resultados estadísticos disponibles. Las respuestas se irán agrupando conforme participen los usuarios.",
                        color = Gris, fontSize = 12.sp, lineHeight = 18.sp
                    )
                } else {
                    encuesta.preguntas.forEachIndexed { indice, pregunta ->
                        val agregado = estadisticas.firstOrNull { it.idPregunta == pregunta.id }
                        if (agregado != null) {
                            GlassPanel(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text("${indice + 1}. ${pregunta.texto}", color = Texto, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text("${agregado.total} respuestas", color = Azul, fontSize = 10.sp)

                                    when (pregunta.tipo) {
                                        "single_choice", "scale_1_5" -> {
                                            val opciones = if (pregunta.tipo == "scale_1_5") listOf("1", "2", "3", "4", "5") else pregunta.opciones
                                            val totalOpciones = opciones.indices.sumOf { agregado.conteosOpciones["o$it"] ?: 0L }
                                            opciones.forEachIndexed { opcionIndex, opcion ->
                                                val cantidad = agregado.conteosOpciones["o$opcionIndex"] ?: 0L
                                                val porcentaje = if (totalOpciones > 0L) cantidad.toFloat() / totalOpciones.toFloat() else 0f
                                                Spacer(Modifier.height(8.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        if (pregunta.tipo == "scale_1_5") "$opcion · ${when (opcion) { "1" -> "Muy bajo"; "5" -> "Muy alto"; else -> "" }}" else opcion,
                                                        modifier = Modifier.weight(1f),
                                                        color = Gris,
                                                        fontSize = 10.sp
                                                    )
                                                    Text(
                                                        when {
                                                            cantidad == 0L -> "0"
                                                            cantidad < 3L -> "Menos de 3"
                                                            else -> "$cantidad (${(porcentaje * 100).toInt()}%)"
                                                        },
                                                        color = Texto,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                BarraResultado(porcentaje = if (cantidad < 3L) 0f else porcentaje)
                                            }
                                            if (pregunta.tipo == "scale_1_5" && totalOpciones > 0L) {
                                                val suma = (1..5).sumOf { valor -> valor.toLong() * (agregado.conteosOpciones["o${valor - 1}"] ?: 0L) }
                                                val promedio = suma.toDouble() / totalOpciones.toDouble()
                                                Spacer(Modifier.height(9.dp))
                                                Text("Promedio: ${String.format(Locale.getDefault(), "%.1f", promedio)} de 5", color = Azul, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        "open_text" -> {
                                            Text(
                                                "Se recibieron ${agregado.total} respuestas abiertas. Por privacidad, el panel no muestra el texto individual.",
                                                color = Gris, fontSize = 11.sp, lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "Los conteos de opciones inferiores a 3 se agrupan como ‘Menos de 3’ para reducir el riesgo de identificar participantes.",
                        color = Gris, fontSize = 10.sp, lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun BarraResultado(porcentaje: Float) {
    Box(
        Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)
            .background(LuumSuperficieAlta)
    ) {
        Box(
            Modifier.fillMaxWidth(porcentaje.coerceIn(0f, 1f)).height(7.dp)
                .clip(CircleShape).background(Azul)
        )
    }
}

@Composable
private fun ResponderEncuestaDialog(
    encuesta: Encuesta,
    perfil: PerfilUsuario,
    onDismiss: () -> Unit,
    onEnviar: (Map<String, String>, (Boolean, String) -> Unit) -> Unit
) {
    val respuestas = remember(encuesta.id, perfil.uid) { mutableStateMapOf<String, String>() }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        title = { Text(encuesta.titulo) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(encuesta.descripcion, color = Gris, fontSize = 12.sp)
                encuesta.preguntas.forEachIndexed { indice, pregunta ->
                    Column(Modifier.fillMaxWidth()) {
                        Text("${indice + 1}. ${pregunta.texto}", color = Texto, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        when (pregunta.tipo) {
                            "single_choice" -> {
                                pregunta.opciones.forEach { opcion ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).clickable { respuestas[pregunta.id] = opcion }.padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = respuestas[pregunta.id] == opcion,
                                            onClick = { respuestas[pregunta.id] = opcion }
                                        )
                                        Text(opcion, color = Gris, fontSize = 12.sp)
                                    }
                                }
                            }
                            "scale_1_5" -> {
                                Text("1 = Muy bajo · 5 = Muy alto", color = Gris, fontSize = 10.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    (1..5).forEach { valor ->
                                        FilterChip(
                                            selected = respuestas[pregunta.id] == valor.toString(),
                                            onClick = { respuestas[pregunta.id] = valor.toString() },
                                            label = { Text(valor.toString()) }
                                        )
                                    }
                                }
                            }
                            "open_text" -> {
                                OutlinedTextField(
                                    value = respuestas[pregunta.id] ?: "",
                                    onValueChange = { respuestas[pregunta.id] = it.take(1000) },
                                    label = { Text("Tu respuesta") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 4
                                )
                            }
                        }
                    }
                }
                Text(
                    "Tu respuesta se guarda de forma privada y esta cuenta solo puede responder una vez. Evita escribir nombres o datos personales en preguntas abiertas.",
                    color = Gris,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                if (error.isNotBlank()) Text(error, color = LuumAmbar, fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val faltantes = encuesta.preguntas.filter { pregunta ->
                        val respuesta = respuestas[pregunta.id]?.trim().orEmpty()
                        respuesta.isBlank()
                    }
                    if (faltantes.isNotEmpty()) {
                        error = "Contesta todas las preguntas antes de enviar."
                    } else {
                        enviando = true
                        error = ""
                        onEnviar(respuestas.toMap()) { correcto, texto ->
                            enviando = false
                            if (correcto) onDismiss() else error = texto
                        }
                    }
                },
                enabled = !enviando
            ) {
                if (enviando) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Enviar respuestas")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !enviando) { Text("Cancelar") }
        }
    )
}

// =====================================================
// 14. APRENDER
// =====================================================

@Composable
fun AprenderScreen() {
    TituloSeccion("Aprender y mejorar", "Actividades educativas basadas en situaciones reales.")
    TarjetaAccion("Temperatura en las aulas", "Investiga cómo influyen el sol, la ventilación y los materiales en la temperatura.", "Reto práctico")
    TarjetaAccion("Residuos escolares", "Aprende a clasificar residuos y propone alternativas de reutilización.", "Reto ambiental")
    TarjetaAccion("Consumo de agua", "Investiga el uso del agua y plantea estrategias para reducir desperdicios.", "Investigación")
    Text("Las actividades generadas por inteligencia artificial se incorporarán en una siguiente etapa.", color = Gris, fontSize = 12.sp, lineHeight = 19.sp)
}

// =====================================================
// 14. PERFIL - SIN CAMBIO MANUAL DE ROL
// =====================================================

@Composable
fun PerfilScreen(
    perfil: PerfilUsuario,
    institucion: Institucion?,
    cerrarSesion: () -> Unit
) {
    TituloSeccion("Mi perfil", "Información de tu cuenta de LuumEdu.")
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(perfil.nombre, color = Texto, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(perfil.correo, color = Gris, fontSize = 12.sp)
            Spacer(Modifier.height(15.dp))
            Text("PERFIL AUTORIZADO", color = Azul, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(etiquetaRol(perfil.rol), color = Texto, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text("INSTITUCIÓN", color = Azul, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(institucion?.nombre ?: "Sin institución vinculada", color = Texto, fontSize = 13.sp)
            if (institucion != null && institucion.estado != "active") {
                Spacer(Modifier.height(7.dp))
                Text("La institución está pendiente de aprobación.", color = LuumAmbar, fontSize = 11.sp)
            }
        }
    }
    if (perfil.rol == "director" && perfil.estado == "approved") {
        Spacer(Modifier.height(22.dp))
        ContactoDesarrolladorScreen()
        Spacer(Modifier.height(19.dp))
    } else {
        Spacer(Modifier.height(19.dp))
    }
    TarjetaAccion(
        "Seguridad de la cuenta",
        "Tu rol y tus permisos provienen del perfil aprobado en Firebase. No puedes cambiarlos desde esta pantalla.",
        "Autorización institucional"
    )
    Button(onClick = cerrarSesion, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text("Cerrar sesión", fontWeight = FontWeight.Bold)
    }
}

// =====================================================
// CONTACTO CON EL DESARROLLADOR - SOLO DIRECTORES
// =====================================================

@Composable
private fun ContactoDesarrolladorScreen() {
    val context = LocalContext.current
    var mensajeError by rememberSaveable { mutableStateOf("") }

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "CONTACTO CON EL DESARROLLADOR",
                color = Azul,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(15.dp))

            // Fotografía del desarrollador ubicada en:
            // app/src/main/res/drawable/foto_desarrollador.jpg
            // También puede utilizarse PNG o WEBP, conservando el mismo nombre
            // del recurso: foto_desarrollador.
            Image(
                painter = painterResource(id = R.drawable.foto_desarrollador),
                contentDescription = "Fotografía de $NOMBRE_DESARROLLADOR",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .border(1.dp, LuumContorno, CircleShape)
            )

            Spacer(Modifier.height(13.dp))
            Text(
                NOMBRE_DESARROLLADOR,
                color = Texto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Desarrollador de LuumEdu",
                color = Gris,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                CORREO_DESARROLLADOR,
                color = Azul,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val saludo = when (hora) {
                        in 5..11 -> "Buenos días,"
                        in 12..18 -> "Buenas tardes,"
                        else -> "Buenas noches,"
                    }
                    val intentCorreo = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$CORREO_DESARROLLADOR")
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "Contacto con el desarrollador de LuumEdu"
                        )
                        putExtra(Intent.EXTRA_TEXT, "$saludo\n\n")
                    }

                    try {
                        context.startActivity(
                            Intent.createChooser(
                                intentCorreo,
                                "Enviar correo al desarrollador"
                            )
                        )
                        mensajeError = ""
                    } catch (_: Exception) {
                        mensajeError = "No se encontró una aplicación de correo configurada en este dispositivo."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Redactar correo", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(9.dp))
            Text(
                "Se abrirá tu aplicación de correo con el destinatario, el asunto y el saludo según la hora local. Tú completas el mensaje y lo envías manualmente.",
                color = Gris,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (mensajeError.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    mensajeError,
                    color = LuumAmbar,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
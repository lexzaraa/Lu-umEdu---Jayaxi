package mx.luumedu.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.location.Location
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
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

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import androidx.compose.ui.text.withStyle
import mx.luumedu.app.ui.theme.*

// =====================================================
// 1. IDENTIDAD VISUAL - DARK GLASS
// =====================================================

// Alias locales: los valores reales viven en ui/theme/Color.kt
private val Acento = LuumTurquesaClaro            // texto de acento / títulos
private val AcentoContenedor = LuumTurquesaProfundo // indicador de navegación
private val Fondo = LuumFondo
private val Superficie = LuumSuperficie
private val Texto = LuumTexto
private val Gris = LuumGris
private val BordeGlass = LuumBordeCristal

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
    val titulo: String,
    val categoria: String,
    val ubicacion: String,
    val descripcion: String,
    val creadoEn: Long = 0L
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
    val rolSolicitado: String,
    val institucionSolicitadaId: String? = null
)

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
    // Si el documento no existe, no hay perfil (antes se devolvía uno "pendiente" falso).
    if (!exists()) return null
    val uidDoc = getString("uid") ?: id
    return PerfilUsuario(
        uid = uidDoc,
        nombre = getString("displayName") ?: "Usuario LUUM EDU",
        correo = getString("email") ?: "",
        rol = getString("role") ?: "pending",
        estado = getString("status") ?: "pending",
        institucionId = getString("institutionId"),
        rolSolicitado = getString("requestedRole") ?: "student",
        institucionSolicitadaId = getString("requestedInstitutionId")
    )
}

private fun DocumentSnapshot.aObservacion(): Observacion? {
    val titulo = getString("title") ?: return null
    return Observacion(
        titulo = titulo,
        categoria = getString("category") ?: "",
        ubicacion = getString("place") ?: "",
        descripcion = getString("description") ?: "",
        // Mientras el servidor confirma la hora, el reporte aparece como el más reciente.
        creadoEn = getTimestamp("createdAt")?.toDate()?.time ?: Long.MAX_VALUE
    )
}

private fun etiquetaRol(rol: String): String = when (rol) {
    "director" -> "Director"
    "admin" -> "Administrador"
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
        MapLibre.getInstance(applicationContext)
        setContent {
            // La intro se muestra una sola vez por arranque (no se repite al girar la pantalla).
            var mostrarIntro by rememberSaveable { mutableStateOf(true) }
            Box(Modifier.fillMaxSize()) {
                LuumEduApp()
                if (mostrarIntro) {
                    IntroLuumEdu { mostrarIntro = false }
                }
            }
        }
    }
}

// =====================================================
// 6. APLICACIÓN Y CONEXIÓN CON FIREBASE
// =====================================================

@Composable
fun LuumEduApp() {
    val context = LocalContext.current.applicationContext
    val auth = remember { FirebaseAuth.getInstance().also { it.setLanguageCode("es") } }
    val db = remember { FirebaseFirestore.getInstance() }

    var usuarioFirebase by remember { mutableStateOf(auth.currentUser) }
    var perfil by remember { mutableStateOf<PerfilUsuario?>(null) }
    var perfilCargando by remember { mutableStateOf(auth.currentUser != null) }
    var errorPerfil by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    var mensajeAuth by remember { mutableStateOf("") }
    var registrandoCuenta by remember { mutableStateOf(false) }

    var pestanaActual by rememberSaveable { mutableStateOf("Inicio") }
    val observaciones = remember { mutableStateListOf<Observacion>() }
    val instituciones = remember { mutableStateListOf<Institucion>() }
    var institucionUsuario by remember { mutableStateOf<Institucion?>(null) }

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

    // Perfil en tiempo real: si el administrador aprueba la cuenta, la app se actualiza sola.
    DisposableEffect(usuarioFirebase?.uid) {
        val uidActual = usuarioFirebase?.uid
        var registro: ListenerRegistration? = null
        if (uidActual == null) {
            perfil = null
            perfilCargando = false
        } else {
            perfilCargando = true
            registro = db.collection("users").document(uidActual)
                .addSnapshotListener { documento, error ->
                    if (error != null) {
                        errorPerfil = "No se pudo consultar tu perfil: ${error.localizedMessage ?: "Error de conexión"}"
                        perfilCargando = false
                    } else if (documento != null) {
                        val perfilLeido = documento.aPerfil()
                        if (perfilLeido != null) {
                            perfil = perfilLeido
                            errorPerfil = ""
                            perfilCargando = false
                        } else if (!registrandoCuenta && !documento.metadata.isFromCache) {
                            perfil = null
                            errorPerfil = "La cuenta existe, pero todavía no encontramos su perfil. Contacta al administrador."
                            perfilCargando = false
                        }
                    }
                }
        }
        onDispose { registro?.remove() }
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

    // Institución vinculada al perfil, en tiempo real.
    DisposableEffect(perfil?.institucionId) {
        val id = perfil?.institucionId
        var registro: ListenerRegistration? = null
        if (id.isNullOrBlank()) {
            institucionUsuario = null
        } else {
            registro = db.collection("institutions").document(id)
                .addSnapshotListener { documento, error ->
                    institucionUsuario = if (error == null && documento != null) documento.aInstitucion() else null
                }
        }
        onDispose { registro?.remove() }
    }

    // Reportes guardados de la persona (se conservan al cerrar la app).
    val uidReportes = if (perfil?.estado == "approved") perfil?.uid else null
    DisposableEffect(uidReportes) {
        var registro: ListenerRegistration? = null
        if (uidReportes == null) {
            observaciones.clear()
        } else {
            registro = db.collection("reports")
                .whereEqualTo("authorUid", uidReportes)
                .addSnapshotListener { instantanea, error ->
                    if (error == null && instantanea != null) {
                        val lista = instantanea.documents
                            .mapNotNull { it.aObservacion() }
                            .sortedBy { it.creadoEn }
                        observaciones.clear()
                        observaciones.addAll(lista)
                    }
                }
        }
        onDispose { registro?.remove() }
    }

    val guardarObservacion: (Observacion, (Boolean) -> Unit) -> Unit = { observacion, resultado ->
        val usuarioActual = perfil
        if (usuarioActual == null) {
            resultado(false)
        } else {
            val datos = hashMapOf<String, Any?>(
                "title" to observacion.titulo,
                "category" to observacion.categoria,
                "place" to observacion.ubicacion,
                "description" to observacion.descripcion,
                "authorUid" to usuarioActual.uid,
                "authorName" to usuarioActual.nombre,
                "institutionId" to usuarioActual.institucionId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("reports").add(datos)
                .addOnSuccessListener { resultado(true) }
                .addOnFailureListener { resultado(false) }
        }
    }

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

    val registrarCuenta: (String, String, String, String, String?) -> Unit =
        { nombre, correo, clave, rolSolicitado, institucionSolicitada ->
            ocupado = true
            registrandoCuenta = true
            mensajeAuth = ""
            auth.createUserWithEmailAndPassword(correo.trim(), clave)
                .addOnCompleteListener { tarea ->
                    if (!tarea.isSuccessful) {
                        ocupado = false
                        registrandoCuenta = false
                        mensajeAuth = tarea.exception?.localizedMessage ?: "No se pudo crear la cuenta."
                        return@addOnCompleteListener
                    }

                    val nuevoUsuario: FirebaseUser? = tarea.result?.user
                    if (nuevoUsuario == null) {
                        ocupado = false
                        registrandoCuenta = false
                        mensajeAuth = "Firebase no devolvió los datos de la cuenta creada."
                    } else {
                        val datos = hashMapOf<String, Any?>(
                            "uid" to nuevoUsuario.uid,
                            "displayName" to nombre.trim(),
                            "email" to correo.trim(),
                            "requestedRole" to rolSolicitado,
                            "requestedInstitutionId" to institucionSolicitada,
                            "role" to "pending",
                            "status" to "pending",
                            "institutionId" to null,
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        db.collection("users").document(nuevoUsuario.uid)
                            .set(datos)
                            .addOnSuccessListener {
                                registrandoCuenta = false
                                ocupado = false
                                mensajeAuth = ""
                            }
                            .addOnFailureListener { error ->
                                registrandoCuenta = false
                                ocupado = false
                                perfilCargando = false
                                errorPerfil = "La cuenta de acceso se creó, pero no se pudo guardar el perfil. Contacta al administrador. Detalle: ${error.localizedMessage ?: "sin detalle"}"
                            }
                    }
                }
        }

    val recuperarClave: (String) -> Unit = { correo ->
        if (correo.isBlank() || !correo.contains("@")) {
            mensajeAuth = "Escribe tu correo electrónico para enviarte el enlace de recuperación."
        } else {
            ocupado = true
            auth.sendPasswordResetEmail(correo.trim())
                .addOnCompleteListener { tarea ->
                    ocupado = false
                    mensajeAuth = if (tarea.isSuccessful) {
                        "Te enviamos un correo para restablecer tu contraseña. Revisa también la carpeta de spam."
                    } else {
                        tarea.exception?.localizedMessage ?: "No se pudo enviar el correo de recuperación."
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
        colorScheme = LuumDarkColorScheme
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
                    .background(LuumTurquesa.copy(alpha = 0.10f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .offset(x = (-100).dp, y = 430.dp)
                    .size(230.dp)
                    .background(LuumAzulProfundo.copy(alpha = 0.13f), CircleShape)
            )

            when {
                usuarioFirebase == null -> {
                    AccesoScreen(
                        instituciones = instituciones,
                        ocupado = ocupado,
                        mensaje = mensajeAuth,
                        iniciarSesion = iniciarSesion,
                        registrarCuenta = registrarCuenta,
                        recuperarClave = recuperarClave
                    )
                }

                perfilCargando -> {
                    EstadoCargaScreen("Consultando tu cuenta segura...")
                }

                perfil == null -> {
                    EstadoCuentaScreen(
                        mensaje = errorPerfil.ifBlank { "No encontramos tu perfil de LUUM EDU." },
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
                    val pestanas = buildList {
                        add(Pestana("Inicio", "⌂"))
                        add(Pestana("Escuela", "◎"))
                        add(Pestana("Reportar", "+"))
                        add(Pestana("Aprender", "▤"))
                        if (usuario.rol == "admin") add(Pestana("Admin", "★"))
                        add(Pestana("Perfil", "◉"))
                    }

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
                                            selectedIconColor = Acento,
                                            selectedTextColor = Acento,
                                            indicatorColor = AcentoContenedor,
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
                            Encabezado(etiquetaRol(usuario.rol))
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 18.dp, vertical = 17.dp)
                            ) {
                                when (pestanaActual) {
                                    "Inicio" -> InicioScreen(
                                        rol = etiquetaRol(usuario.rol),
                                        observaciones = observaciones.size,
                                        instituciones = instituciones,
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

                                    "Reportar" -> ReportarScreen(
                                        perfil = usuario,
                                        institucion = institucionUsuario,
                                        observaciones = observaciones,
                                        guardar = guardarObservacion
                                    )

                                    "Aprender" -> AprenderScreen()

                                    "Admin" -> AdminScreen(db = db, instituciones = instituciones)

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
    registrarCuenta: (String, String, String, String, String?) -> Unit,
    recuperarClave: (String) -> Unit
) {
    var modoRegistro by rememberSaveable { mutableStateOf(false) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var rolSolicitado by rememberSaveable { mutableStateOf("student") }
    var institucionId by rememberSaveable { mutableStateOf("") }
    var menuRoles by remember { mutableStateOf(false) }
    var menuInstituciones by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MarcaLuumEdu(fontSize = 31.sp)
            Text("GEOSPATIAL EDUCATION INTELLIGENCE", color = Gris, fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(25.dp))

            GlassPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        if (modoRegistro) "SOLICITAR ACCESO" else "BIENVENIDO A LUUM EDU",
                        color = Acento,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(17.dp))

                    if (modoRegistro) {
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre completo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(11.dp))
                    }

                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(11.dp))
                    OutlinedTextField(
                        value = clave,
                        onValueChange = { clave = it },
                        label = { Text("Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (modoRegistro) {
                        Spacer(Modifier.height(15.dp))
                        Text("Solicitar perfil", color = Gris, fontSize = 12.sp)
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
                                    "parent" to "Padre de familia",
                                    "teacher" to "Docente"
                                ).forEach { (valor, texto) ->
                                    DropdownMenuItem(
                                        text = { Text(texto) },
                                        onClick = {
                                            rolSolicitado = valor
                                            institucionId = ""
                                            menuRoles = false
                                        }
                                    )
                                }
                            }
                        }

                        if (rolSolicitado != "director") {
                            Spacer(Modifier.height(11.dp))
                            Text("Institución a la que perteneces", color = Gris, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            Box {
                                OutlinedButton(
                                    onClick = { menuInstituciones = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val seleccionada = instituciones.firstOrNull { it.id == institucionId }
                                    Text(
                                        seleccionada?.nombre ?: "Seleccionar institución",
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
                                                menuInstituciones = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (instituciones.isEmpty()) {
                                Spacer(Modifier.height(7.dp))
                                Text(
                                    "Todavía no hay escuelas activas. Un director debe registrar la suya y el administrador aprobarla antes de que los demás perfiles puedan registrarse.",
                                    color = LuumAmbar,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        } else {
                            Spacer(Modifier.height(9.dp))
                            Text(
                                "La solicitud de director no necesita seleccionar una escuela. Primero debe ser autorizada por el administrador de LUUM EDU.",
                                color = Gris,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    if (mensaje.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(mensaje, color = LuumAmbar, fontSize = 12.sp, lineHeight = 17.sp)
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (modoRegistro) {
                                registrarCuenta(
                                    nombre.trim(),
                                    correo.trim(),
                                    clave,
                                    rolSolicitado,
                                    if (rolSolicitado == "director") null else institucionId
                                )
                            } else {
                                iniciarSesion(correo.trim(), clave)
                            }
                        },
                        enabled = !ocupado && correo.contains("@") && clave.length >= 6 &&
                                (!modoRegistro || (nombre.isNotBlank() && (rolSolicitado == "director" || institucionId.isNotBlank()))),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (ocupado) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (modoRegistro) "Enviar solicitud" else "Iniciar sesión", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!modoRegistro) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = { recuperarClave(correo.trim()) },
                            enabled = !ocupado,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("¿Olvidaste tu contraseña?", color = Gris, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(9.dp))
                    TextButton(
                        onClick = {
                            modoRegistro = !modoRegistro
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (modoRegistro) "Ya tengo una cuenta · Iniciar sesión" else "¿No tienes cuenta? Solicitar acceso",
                            color = Acento,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "El rol solicitado no concede permisos automáticamente. Todas las cuentas necesitan aprobación.",
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
            CircularProgressIndicator(color = Acento)
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
                Text("PERFIL NO DISPONIBLE", color = Acento, fontWeight = FontWeight.Bold)
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
                Text(if (rechazado) "SOLICITUD NO APROBADA" else "SOLICITUD EN REVISIÓN", color = if (rechazado) LuumError else LuumAmbar, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(13.dp))
                Text("Hola, ${perfil.nombre}", color = Texto, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text(
                    if (rechazado) "Tu solicitud fue rechazada. Contacta al administrador de LUUM EDU si consideras que se trata de un error."
                    else "Tu cuenta fue creada correctamente. Un administrador debe revisar y aprobar el perfil antes de que puedas utilizar las funciones de LUUM EDU.",
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
    instituciones: List<Institucion>
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
    val mapView = remember(context) { MapView(context).apply { onCreate(null) } }
    val marcadores = remember(mapView) { mutableListOf<Marker>() }
    val institucionesActualizadas = instituciones.toList()

    DisposableEffect(mapView, lifecycleOwner) {
        var iniciado = false
        var reanudado = false
        var destruido = false

        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_START -> if (!iniciado && !destruido) {
                    mapView.onStart(); iniciado = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!iniciado && !destruido) { mapView.onStart(); iniciado = true }
                    if (!reanudado && !destruido) { mapView.onResume(); reanudado = true }
                }
                Lifecycle.Event.ON_PAUSE -> if (reanudado && !destruido) {
                    mapView.onPause(); reanudado = false
                }
                Lifecycle.Event.ON_STOP -> if (iniciado && !destruido) {
                    if (reanudado) { mapView.onPause(); reanudado = false }
                    mapView.onStop(); iniciado = false
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        when {
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> {
                mapView.onStart(); iniciado = true
                mapView.onResume(); reanudado = true
            }
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) -> {
                mapView.onStart(); iniciado = true
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

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { mapa ->
                        mapaActual = mapa
                        mapa.setStyle(MAPA_ESTILO) { estilo ->
                            estiloActual = estilo
                            estiloListo = true
                            mapa.cameraPosition = CameraPosition.Builder()
                                .target(CANCUN_REFERENCIA)
                                .zoom(14.0)
                                .build()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(mapaActual, estiloActual, estiloListo, institucionesActualizadas) {
            val mapa = mapaActual ?: return@LaunchedEffect
            if (!estiloListo || estiloActual == null) return@LaunchedEffect

            marcadores.forEach { mapa.removeMarker(it) }
            marcadores.clear()

            institucionesActualizadas.forEach { institucion ->
                val marcador = mapa.addMarker(
                    MarkerOptions()
                        .position(LatLng(institucion.latitud, institucion.longitud))
                        .title(institucion.nombre)
                        .snippet("${institucion.tipo} • ${institucion.direccion}")
                )
                marcadores.add(marcador)
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

        if (!tienePermisoUbicacion) {
            GlassPanel(
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
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

// =====================================================
// 9. COMPONENTES VISUALES
// =====================================================

/** Wordmark como en el logo: "LUUM" en blanco azulado y "EDU" en verde. */
@Composable
private fun MarcaLuumEdu(fontSize: androidx.compose.ui.unit.TextUnit) {
    Text(
        text = androidx.compose.ui.text.buildAnnotatedString {
            withStyle(androidx.compose.ui.text.SpanStyle(color = Texto)) { append("LUUM ") }
            withStyle(androidx.compose.ui.text.SpanStyle(color = LuumVerde)) { append("EDU") }
        },
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val brush = if (accent) {
        Brush.linearGradient(listOf(Color(0xC5126760), Color(0xB80E4547), Color(0xCC0E2230)))
    } else {
        Brush.linearGradient(listOf(Color(0xD91A2E3C), Color(0xBC122232), Color(0xC90E1B29)))
    }

    val forma = modifier
        .clip(shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .background(brush)
        .border(1.dp, if (accent) LuumBordeTurquesa else BordeGlass, shape)

    Box(modifier = forma, content = content)
}

@Composable
fun Encabezado(rol: String) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                MarcaLuumEdu(fontSize = 22.sp)
                Text("GEOSPATIAL EDUCATION INTELLIGENCE", fontSize = 9.sp, color = Gris, letterSpacing = 1.sp)
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
            Text(accion, color = Acento, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
    observaciones: Int,
    instituciones: List<Institucion>,
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
            Text("ENTORNO GEOGRÁFICO", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Acento)
            Text("MAPA INTERACTIVO", fontSize = 9.sp, color = LuumVerde, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(11.dp))
        val formaMapa = RoundedCornerShape(22.dp)
        Box(
            modifier = Modifier.fillMaxWidth().height(290.dp).clip(formaMapa).border(1.dp, LuumBordeTurquesa, formaMapa)
        ) {
            MapaGeografico(modifier = Modifier.fillMaxSize(), instituciones = instituciones)
            GlassPanel(modifier = Modifier.align(Alignment.TopStart).padding(12.dp), shape = RoundedCornerShape(9.dp), accent = true) {
                Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
                    Text("INSTITUCIONES ACTIVAS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LuumTurquesaClaro)
                    Text("${instituciones.size} instituciones", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            GlassPanel(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp), shape = RoundedCornerShape(7.dp)) {
                Text("© OpenFreeMap · © OpenMapTiles · © OpenStreetMap", modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), fontSize = 8.sp, color = Color.White)
            }
        }
        Spacer(Modifier.height(9.dp))
        Text("Solo las instituciones aprobadas aparecen en el directorio público.", color = Gris, fontSize = 11.sp, lineHeight = 16.sp)
        Spacer(Modifier.height(26.dp))
        Text("ACTIVIDAD DE LUUM EDU", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Gris)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResumenCard("Reportes", observaciones.toString(), Modifier.weight(1f))
            ResumenCard("Instituciones", instituciones.size.toString(), Modifier.weight(1f), colorCifra = LuumVerde)
        }
        Spacer(Modifier.height(28.dp))
        Text("OBSERVAR Y PARTICIPAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Gris)
        Spacer(Modifier.height(12.dp))
        TarjetaAccion("Documentar una observación", "Registra una situación del entorno escolar y aporta evidencia.", "Crear observación →") { navegar("Reportar") }
        TarjetaAccion("Consultar instituciones", "Explora preparatorias y universidades participantes.", "Explorar directorio →") { navegar("Escuela") }
        TarjetaAccion("Investigar y aprender", "Convierte las situaciones de tu entorno en experiencias educativas.", "Ver actividades →") { navegar("Aprender") }
        Spacer(Modifier.height(10.dp))
        Text("Sesión: $rol", color = Gris, fontSize = 11.sp)
    }
}

@Composable
fun ResumenCard(titulo: String, cantidad: String, modifier: Modifier = Modifier, colorCifra: Color = Acento) {
    GlassPanel(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(17.dp)) {
            Text(cantidad, fontSize = 29.sp, fontWeight = FontWeight.Bold, color = colorCifra)
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
                    Text("REGISTRAR INSTITUCIÓN", color = Acento, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
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
                    Text("UBICACIÓN AUTOMÁTICA DEL CAMPUS", color = Acento, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                        Text(mensaje, color = if (mensaje.startsWith("Solicitud enviada") || mensaje.startsWith("Ubicación capturada")) LuumExito else LuumAmbar, fontSize = 12.sp, lineHeight = 17.sp)
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
                when (estado) {
                    "active" -> "Institución aprobada"
                    "rejected" -> "Solicitud rechazada. Contacta al administrador."
                    else -> "Pendiente de revisión administrativa"
                }
            )
        } else {
            TarjetaAccion(
                "Registro de instituciones",
                "El registro de escuelas está reservado para directores autorizados por LUUM EDU.",
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
// 12. REPORTAR - CONTROL DE PRESENCIA PARA ESTUDIANTES
// =====================================================

@Suppress("DEPRECATION")
@Composable
fun ReportarScreen(
    perfil: PerfilUsuario,
    institucion: Institucion?,
    observaciones: List<Observacion>,
    guardar: (Observacion, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var titulo by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf("Infraestructura") }
    var ubicacion by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var mensaje by rememberSaveable { mutableStateOf("") }
    var validandoUbicacion by remember { mutableStateOf(false) }
    var guardandoReporte by remember { mutableStateOf(false) }

    val requierePresencia = perfil.rol == "student"
    val permisosUbicacion = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    val tienePermiso = {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val solicitarPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true || permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        mensaje = if (concedido) "Permiso concedido. Pulsa de nuevo para enviar tu opinión." else "Para opinar, debes permitir el acceso a la ubicación."
    }

    fun guardarReporte() {
        if (titulo.isBlank() || categoria.isBlank() || descripcion.isBlank()) {
            mensaje = "Completa el título, la categoría y la descripción."
            return
        }

        val completarGuardado = {
            guardandoReporte = true
            mensaje = "Guardando observación..."
            guardar(
                Observacion(
                    titulo = titulo.trim(),
                    categoria = categoria.trim(),
                    ubicacion = ubicacion.trim(),
                    descripcion = descripcion.trim()
                )
            ) { exito ->
                guardandoReporte = false
                if (exito) {
                    titulo = ""
                    ubicacion = ""
                    descripcion = ""
                    mensaje = "Observación guardada correctamente."
                } else {
                    mensaje = "No se pudo guardar la observación. Revisa tu conexión e inténtalo de nuevo."
                }
            }
        }

        if (!requierePresencia) {
            completarGuardado()
            return
        }

        if (institucion == null || institucion.estado != "active") {
            mensaje = "Tu cuenta no tiene una institución activa vinculada. Contacta al administrador."
            return
        }

        if (!tienePermiso()) {
            solicitarPermiso.launch(permisosUbicacion)
            return
        }

        validandoUbicacion = true
        mensaje = "Verificando que te encuentres dentro de tu escuela..."
        obtenerUbicacionActual(context) { actual ->
            validandoUbicacion = false
            when {
                actual == null -> {
                    mensaje = "No se pudo obtener tu ubicación actual. Activa el GPS e inténtalo otra vez."
                }
                actual.isFromMockProvider -> {
                    mensaje = "Se detectó una ubicación simulada. Desactívala para poder publicar."
                }
                actual.accuracy > PRECISION_MAXIMA_METROS -> {
                    mensaje = "La ubicación no es suficientemente precisa (±${actual.accuracy.toInt()} m). Necesitamos 30 m o menos."
                }
                else -> {
                    val distancia = distanciaMetros(
                        actual.latitude,
                        actual.longitude,
                        institucion.latitud,
                        institucion.longitud
                    )
                    if (distancia <= RADIO_OPINION_METROS) {
                        completarGuardado()
                    } else {
                        mensaje = "No puedes publicar desde esta ubicación. Estás aproximadamente a ${distancia.toInt()} m del centro registrado de tu escuela; el límite es ${RADIO_OPINION_METROS.toInt()} m."
                    }
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        TituloSeccion("Nueva observación", "Documenta una situación de tu entorno educativo.")

        if (requierePresencia) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
                Column(Modifier.padding(14.dp)) {
                    Text("VALIDACIÓN DE PRESENCIA", color = Acento, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Para publicar, tu GPS debe ubicarte dentro de ${RADIO_OPINION_METROS.toInt()} m de tu escuela, con precisión de 30 m o mejor.",
                        color = Gris,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título del reporte") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(13.dp))
        OutlinedTextField(value = categoria, onValueChange = { categoria = it }, label = { Text("Categoría") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(13.dp))
        OutlinedTextField(value = ubicacion, onValueChange = { ubicacion = it }, label = { Text("Ubicación o espacio") }, placeholder = { Text("Ej. Laboratorio, patio, salón...") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(13.dp))
        OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Describe lo que observaste") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { guardarReporte() },
            enabled = !validandoUbicacion && !guardandoReporte && titulo.isNotBlank() && categoria.isNotBlank() && descripcion.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (validandoUbicacion) {
                CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (validandoUbicacion) "Verificando ubicación..." else "Guardar observación", fontWeight = FontWeight.Bold)
        }

        if (mensaje.isNotBlank()) {
            Spacer(Modifier.height(11.dp))
            Text(mensaje, color = if (mensaje.startsWith("Observación guardada")) LuumExito else LuumAmbar, fontSize = 12.sp, lineHeight = 17.sp)
        }

        Spacer(Modifier.height(25.dp))
        Text("Mis reportes", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Texto)
        Spacer(Modifier.height(12.dp))
        if (observaciones.isEmpty()) {
            Text("Todavía no has registrado observaciones.", color = Gris, fontSize = 13.sp)
        } else {
            observaciones.reversed().forEach { reporte ->
                TarjetaAccion(
                    reporte.titulo,
                    "${reporte.categoria} • ${reporte.ubicacion}\n${reporte.descripcion}",
                    "Guardado"
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            "Tus reportes se guardan en tu cuenta. La verificación GPS se realiza en el teléfono y es una medida de apoyo; aún no es una prueba validada por un servidor.",
            color = Gris,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
    }
}

// =====================================================
// 13. APRENDER
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
    TituloSeccion("Mi perfil", "Información de tu cuenta de LUUM EDU.")
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(perfil.nombre, color = Texto, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(perfil.correo, color = Gris, fontSize = 12.sp)
            Spacer(Modifier.height(15.dp))
            Text("PERFIL AUTORIZADO", color = Acento, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(etiquetaRol(perfil.rol), color = Texto, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text("INSTITUCIÓN", color = Acento, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(institucion?.nombre ?: "Sin institución vinculada", color = Texto, fontSize = 13.sp)
            if (institucion != null && institucion.estado != "active") {
                Spacer(Modifier.height(7.dp))
                Text("La institución está pendiente de aprobación.", color = LuumAmbar, fontSize = 11.sp)
            }
        }
    }
    Spacer(Modifier.height(19.dp))
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
// 15. ADMINISTRACIÓN - APROBAR USUARIOS E INSTITUCIONES
// =====================================================

@Composable
private fun AdminScreen(
    db: FirebaseFirestore,
    instituciones: List<Institucion>
) {
    val usuariosPendientes = remember { mutableStateListOf<PerfilUsuario>() }
    val institucionesPendientes = remember { mutableStateListOf<Institucion>() }
    var mensaje by remember { mutableStateOf("") }
    var errorCarga by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf("") }

    DisposableEffect(db) {
        val registroUsuarios = db.collection("users")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { instantanea, error ->
                if (error != null) {
                    errorCarga = "No se pudieron cargar las solicitudes: ${error.localizedMessage ?: "Error de conexión"}"
                } else if (instantanea != null) {
                    errorCarga = ""
                    usuariosPendientes.clear()
                    instantanea.documents.mapNotNull { it.aPerfil() }.forEach { usuariosPendientes.add(it) }
                }
            }
        val registroInstituciones = db.collection("institutions")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { instantanea, error ->
                if (error != null) {
                    errorCarga = "No se pudieron cargar las instituciones: ${error.localizedMessage ?: "Error de conexión"}"
                } else if (instantanea != null) {
                    institucionesPendientes.clear()
                    instantanea.documents.mapNotNull { it.aInstitucion() }.forEach { institucionesPendientes.add(it) }
                }
            }
        onDispose {
            registroUsuarios.remove()
            registroInstituciones.remove()
        }
    }

    fun aplicar(coleccion: String, id: String, cambios: HashMap<String, Any>, textoExito: String) {
        procesando = id
        cambios["updatedAt"] = FieldValue.serverTimestamp()
        db.collection(coleccion).document(id).update(cambios)
            .addOnSuccessListener {
                procesando = ""
                mensaje = textoExito
            }
            .addOnFailureListener { error ->
                procesando = ""
                mensaje = "No se pudo completar la acción: ${error.localizedMessage ?: "sin detalle"}"
            }
    }

    fun aprobarUsuario(persona: PerfilUsuario) {
        val cambios = hashMapOf<String, Any>(
            "role" to persona.rolSolicitado,
            "status" to "approved"
        )
        if (persona.rolSolicitado != "director") {
            val institucionElegida = persona.institucionSolicitadaId
            if (institucionElegida.isNullOrBlank()) {
                mensaje = "${persona.nombre} no eligió una institución al registrarse. No se puede aprobar sin una escuela."
                return
            }
            cambios["institutionId"] = institucionElegida
        }
        aplicar("users", persona.uid, cambios, "Se aprobó a ${persona.nombre} como ${etiquetaRol(persona.rolSolicitado)}.")
    }

    fun rechazarUsuario(persona: PerfilUsuario) {
        aplicar("users", persona.uid, hashMapOf<String, Any>("status" to "rejected"), "Se rechazó la solicitud de ${persona.nombre}.")
    }

    fun cambiarInstitucion(institucion: Institucion, estado: String, texto: String) {
        aplicar("institutions", institucion.id, hashMapOf<String, Any>("status" to estado), texto)
    }

    Column(Modifier.fillMaxWidth()) {
        TituloSeccion("Administración", "Aprueba cuentas e instituciones enviadas a LUUM EDU.")

        if (errorCarga.isNotBlank()) {
            Text(errorCarga, color = LuumAmbar, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(12.dp))
        }
        if (mensaje.isNotBlank()) {
            Text(mensaje, color = LuumExito, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(12.dp))
        }

        Text("CUENTAS PENDIENTES (${usuariosPendientes.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Gris)
        Spacer(Modifier.height(13.dp))
        if (usuariosPendientes.isEmpty()) {
            Text("No hay cuentas por aprobar.", color = Gris, fontSize = 13.sp)
        } else {
            usuariosPendientes.forEach { persona ->
                val nombreInstitucion = instituciones.firstOrNull { it.id == persona.institucionSolicitadaId }?.nombre
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(persona.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Texto)
                        Spacer(Modifier.height(4.dp))
                        Text(persona.correo, fontSize = 12.sp, color = Gris)
                        Spacer(Modifier.height(8.dp))
                        Text("Perfil solicitado: ${etiquetaRol(persona.rolSolicitado)}", fontSize = 13.sp, color = Texto)
                        if (persona.rolSolicitado != "director") {
                            Text("Institución: ${nombreInstitucion ?: "No disponible"}", fontSize = 13.sp, color = Gris)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { aprobarUsuario(persona) },
                                enabled = procesando.isEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Aprobar", fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = { rechazarUsuario(persona) },
                                enabled = procesando.isEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Rechazar") }
                        }
                    }
                }
                Spacer(Modifier.height(13.dp))
            }
        }

        Spacer(Modifier.height(19.dp))
        Text("INSTITUCIONES PENDIENTES (${institucionesPendientes.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Gris)
        Spacer(Modifier.height(13.dp))
        if (institucionesPendientes.isEmpty()) {
            Text("No hay instituciones por aprobar.", color = Gris, fontSize = 13.sp)
        } else {
            institucionesPendientes.forEach { institucion ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(institucion.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Texto)
                        Spacer(Modifier.height(4.dp))
                        Text(institucion.tipo, fontSize = 12.sp, color = Gris)
                        Text(institucion.direccion, fontSize = 12.sp, color = Gris)
                        Text("Coordenadas: ${institucion.latitud}, ${institucion.longitud}", fontSize = 12.sp, color = Gris)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { cambiarInstitucion(institucion, "active", "Se activó ${institucion.nombre}.") },
                                enabled = procesando.isEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Activar", fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = { cambiarInstitucion(institucion, "rejected", "Se rechazó ${institucion.nombre}.") },
                                enabled = procesando.isEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Rechazar") }
                        }
                    }
                }
                Spacer(Modifier.height(13.dp))
            }
        }
    }
}

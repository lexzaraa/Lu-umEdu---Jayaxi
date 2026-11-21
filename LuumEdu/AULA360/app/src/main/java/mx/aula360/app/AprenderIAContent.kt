package mx.aula360.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Pantalla inicial para probar la generación educativa con Gemini. */
@Composable
fun AprenderIAContent(observaciones: List<Observacion>) {
    val categorias = listOf(
        "Entorno geográfico",
        "Infraestructura",
        "Servicios escolares",
        "Experiencia educativa",
        "Condiciones ambientales",
        "Seguridad y accesibilidad",
        "Otro"
    )

    var categoria by rememberSaveable { mutableStateOf("Infraestructura") }
    var titulo by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var menuCategoria by remember { mutableStateOf(false) }
    var menuReportes by remember { mutableStateOf(false) }
    var consentimiento by rememberSaveable { mutableStateOf(false) }
    var generando by remember { mutableStateOf(false) }
    var resultado by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val colores = MaterialTheme.colorScheme

    Column(
        // MainActivity ya proporciona el desplazamiento vertical de la pantalla.
        // Evitamos anidar otro verticalScroll para prevenir una excepción de medición.
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Aprender y mejorar", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = colores.onBackground)
        Text(
            "Transforma una observación del entorno escolar en una explicación y una actividad educativa.",
            fontSize = 13.sp,
            color = colores.onSurfaceVariant
        )

        if (observaciones.isNotEmpty()) {
            Column {
                Text("UTILIZAR UN REPORTE EXISTENTE", color = colores.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                androidx.compose.foundation.layout.Box {
                    OutlinedButton(
                        onClick = { menuReportes = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !generando
                    ) {
                        Text(
                            if (titulo.isBlank()) "Seleccionar uno de mis reportes disponibles"
                            else "Cambiar reporte: ${titulo.take(38)}",
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text("▾")
                    }
                    DropdownMenu(
                        expanded = menuReportes,
                        onDismissRequest = { menuReportes = false }
                    ) {
                        observaciones.forEach { reporte ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(reporte.titulo, fontWeight = FontWeight.SemiBold)
                                        Text(reporte.categoria, fontSize = 11.sp, color = colores.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    categoria = reporte.categoria.takeIf { it in categorias } ?: "Otro"
                                    titulo = reporte.titulo
                                    descripcion = reporte.descripcion
                                    resultado = ""
                                    error = ""
                                    menuReportes = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colores.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("CONTEXTO DE APRENDIZAJE", color = colores.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                androidx.compose.foundation.layout.Box {
                    OutlinedButton(
                        onClick = { menuCategoria = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !generando
                    ) {
                        Text(categoria, modifier = Modifier.weight(1f))
                        Text("▾")
                    }
                    DropdownMenu(
                        expanded = menuCategoria,
                        onDismissRequest = { menuCategoria = false }
                    ) {
                        categorias.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = {
                                    categoria = opcion
                                    menuCategoria = false
                                    resultado = ""
                                    error = ""
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it.take(180); resultado = ""; error = "" },
                    label = { Text("Título de la observación") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !generando
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it.take(2500); resultado = ""; error = "" },
                    label = { Text("Descripción de la situación") },
                    placeholder = { Text("Describe qué se observa, sin incluir datos personales...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 7,
                    enabled = !generando
                )

                Text(
                    "No se envían el nombre del estudiante, UID, correo, coordenadas GPS ni el nombre de la institución. El texto del reporte sí se envía a Gemini después de una limpieza básica.",
                    color = colores.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = consentimiento,
                        onCheckedChange = { consentimiento = it },
                        enabled = !generando
                    )
                    Text(
                        "Confirmo que retiré nombres y cualquier dato personal o identificable. Entiendo que el nivel gratuito de Gemini puede utilizar solicitudes y respuestas para mejorar productos de Google.",
                        color = colores.onSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }

                Button(
                    onClick = {
                        if (!consentimiento) {
                            error = "Confirma primero que el texto no contiene datos personales."
                        } else {
                            generando = true
                            error = ""
                            resultado = ""
                            scope.launch {
                                try {
                                    resultado = LuumAiService.generarActividad(
                                        categoria = categoria,
                                        tituloReporte = titulo,
                                        descripcionReporte = descripcion
                                    )
                                } catch (ex: Exception) {
                                    error = ex.localizedMessage
                                        ?: "No se pudo conectar con Gemini. Revisa Internet y la configuración de App Check."
                                } finally {
                                    generando = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !generando && titulo.isNotBlank() && descripcion.trim().length >= 10
                ) {
                    if (generando) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 9.dp).size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Generando borrador...")
                    } else {
                        Text("Generar explicación y actividad", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colores.errorContainer)
            ) {
                Text(
                    error,
                    modifier = Modifier.padding(14.dp),
                    color = colores.onErrorContainer,
                    fontSize = 12.sp
                )
            }
        }

        if (resultado.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colores.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("BORRADOR EDUCATIVO GENERADO", color = colores.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(resultado, color = colores.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp)
                    Text(
                        "Este contenido todavía no se guarda ni se publica. En la siguiente etapa se incorporará la revisión de docentes o directores antes de que una actividad pueda llegar a los estudiantes.",
                        color = colores.onSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Text(
            "La IA ofrece hipótesis y propuestas educativas; no realiza diagnósticos técnicos ni sustituye la revisión del personal responsable del plantel.",
            color = colores.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
    }
}

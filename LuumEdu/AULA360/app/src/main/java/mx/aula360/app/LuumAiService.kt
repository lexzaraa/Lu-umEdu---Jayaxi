package mx.aula360.app

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

/**
 * Servicio de IA de LUUM.
 * Utiliza Firebase AI Logic como intermediario; no requiere incluir una API key
 * de Gemini dentro de la aplicación.
 */
object LuumAiService {

    const val MODEL_NAME = "gemini-3.5-flash-lite"

    private val model by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(MODEL_NAME)
    }

    /**
     * Genera una explicación y una actividad educativa a partir de un reporte.
     * No envía UID, correo, ubicación GPS ni nombre de institución.
     */
    suspend fun generarActividad(
        categoria: String,
        tituloReporte: String,
        descripcionReporte: String
    ): String {
        val categoriaSegura = limpiarTexto(categoria, 120)
        val tituloSeguro = limpiarTexto(tituloReporte, 180)
        val descripcionSegura = limpiarTexto(descripcionReporte, 2200)

        require(categoriaSegura.isNotBlank()) { "Selecciona una categoría." }
        require(tituloSeguro.isNotBlank()) { "El reporte necesita un título." }
        require(descripcionSegura.length >= 10) {
            "La descripción debe contener al menos 10 caracteres después de retirar datos personales."
        }

        val prompt = """
            Eres el asistente educativo de LUUM, una plataforma para preparatorias y universidades.
            Tu tarea es convertir una observación del entorno escolar en una oportunidad de aprendizaje.
            Responde exclusivamente en español, con lenguaje claro, respetuoso y adecuado para estudiantes de educación media superior y superior.

            REGLAS IMPORTANTES:
            - No diagnostiques ni declares que un edificio, alimento, persona, instalación o servicio es seguro o inseguro.
            - No presentes causas posibles como hechos comprobados. Distingue observaciones, hipótesis y datos que sería necesario verificar.
            - No atribuyas culpas ni identifiques personas.
            - No solicites nombres, teléfonos, correos, matrículas ni otros datos personales.
            - Si el reporte describe un riesgo inmediato para la integridad de alguien, recomienda avisar al personal responsable del plantel y no realizar experimentos ni acercamientos al riesgo.
            - Diseña una actividad educativa que pueda realizarse sin exponerse a peligros, sin dañar instalaciones y sin interferir con las clases.
            - No inventes mediciones, normas, resultados ni fuentes.

            ENTREGA CON ESTA ESTRUCTURA:
            1. ¿Qué se observa? Resume el problema sin exagerarlo.
            2. ¿Qué podría estar influyendo? Da hasta tres factores posibles y aclara que deben verificarse.
            3. ¿Qué podemos investigar? Propón preguntas de investigación apropiadas.
            4. Actividad educativa: indica el tipo (informativa, práctica, recreativa o de investigación), objetivo y duración aproximada.
            5. Materiales: usa materiales comunes y de bajo costo; aclara cuando no se necesiten.
            6. Desarrollo: presenta pasos numerados, seguros y realizables.
            7. Reflexión: incluye dos preguntas para discutir.
            8. Evaluación: propone una forma sencilla de comprobar el aprendizaje.

            CONTEXTO ANONIMIZADO DEL REPORTE:
            Categoría: $categoriaSegura
            Título: $tituloSeguro
            Descripción: $descripcionSegura

            No incluyas datos distintos de los proporcionados como si fueran hechos. La respuesta es un BORRADOR EDUCATIVO para revisión humana, no un diagnóstico ni una instrucción institucional.
        """.trimIndent()

        val response = model.generateContent(prompt)
        return response.text
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini no devolvió contenido. Inténtalo nuevamente.")
    }

    /**
     * Redacción básica preventiva de correos, teléfonos y números largos.
     * No puede identificar todos los nombres o datos personales: la interfaz
     * también solicita al usuario que retire esa información antes de enviar.
     */
    private fun limpiarTexto(valor: String, maxLength: Int): String {
        var limpio = valor
            .replace(Regex("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b"), "[correo omitido]")
            .replace(Regex("(?<!\\d)(?:\\+?\\d[\\d\\s().-]{7,}\\d)(?!\\d)"), "[teléfono omitido]")
            .replace(Regex("\\b\\d{8,}\\b"), "[identificador omitido]")
            .trim()

        if (limpio.length > maxLength) {
            limpio = limpio.take(maxLength).trim()
        }
        return limpio
    }
}

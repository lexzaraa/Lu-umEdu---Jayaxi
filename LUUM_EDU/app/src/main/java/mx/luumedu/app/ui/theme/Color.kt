package mx.luumedu.app.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================
// PALETA DE MARCA LUUM EDU
// Fuente: intro animada (IntroLuumEdu) + logo (pirámide, "Luum" y "Edu").
// Este archivo es la ÚNICA fuente de colores de la app.
// =====================================================

// ---- Turquesas (pirámide del logo e intro) ----
val LuumTurquesa = Color(0xFF14B8A6)          // Acción principal (botones, foco, selección). Intro.
val LuumTurquesaClaro = Color(0xFF5EEAD4)     // Acentos de texto y títulos sobre fondo oscuro. Intro.
val LuumTurquesaLogo = Color(0xFF019485)      // Tono exacto de la pirámide del logo.
val LuumTurquesaProfundo = Color(0xFF0E4B4A)  // Contenedores / indicador de navegación.

// ---- Verdes (la palabra "Edu" del logo e intro) ----
val LuumVerde = Color(0xFF22C55E)             // Acento secundario. Intro.
val LuumVerdeLogo = Color(0xFF02A215)         // Tono exacto de "Edu" en el logo.
val LuumVerdeProfundo = Color(0xFF0F4A2E)     // Contenedor secundario.

// ---- Complementarios armónicos ----
val LuumAmbar = Color(0xFFFFB74D)             // Dorado cálido (sol / piedra maya): avisos y estados pendientes.
val LuumAzulProfundo = Color(0xFF3C62C9)      // Azul de profundidad usado en el brillo de la intro.
val LuumAzulMarino = Color(0xFF002B71)        // Azul marino de "Luum" y el libro en el ícono de la app.

// ---- Neutros (azul marino del fondo del logo + blanco del libro) ----
val LuumFondo = Color(0xFF080E19)             // Fondo del logo e intro.
val LuumFondoGradienteA = Color(0xFF0B1E29)   // Gradiente de fondo (marino con matiz turquesa)
val LuumFondoGradienteB = Color(0xFF0B1624)
val LuumSuperficie = Color(0xFF111D2B)
val LuumSuperficieAlta = Color(0xFF182A3A)
val LuumSuperficieNavegacion = Color(0xEE0E1926)
val LuumTexto = Color(0xFFE7F0FB)             // Blanco azulado del libro y de "Luum" en el logo.
val LuumGris = Color(0xFFA5B3C7)              // Texto secundario.

// ---- Estados ----
val LuumExito = Color(0xFF4ADE80)
val LuumError = Color(0xFFFF8A80)

// ---- Transparencias reutilizables (efecto cristal) ----
val LuumBordeCristal = Color(0x21FFFFFF)
val LuumBordeTurquesa = Color(0x5514B8A6)
val LuumContorno = Color(0x4D5EEAD4)          // Contorno de campos y botones outlined

// ---- Esquema claro (para LuumEduTheme; la app usa el oscuro) ----
val LuumLuzFondo = Color(0xFFF3F8F9)
val LuumLuzTexto = Color(0xFF0B1620)
val LuumLuzSuperficie = Color(0xFFFFFFFF)

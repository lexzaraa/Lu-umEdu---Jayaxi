package mx.luumedu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Esquema oscuro oficial de LUUM EDU (el que usa la app). */
val LuumDarkColorScheme = darkColorScheme(
    primary = LuumTurquesa,
    onPrimary = LuumFondo,
    primaryContainer = LuumTurquesaProfundo,
    onPrimaryContainer = LuumTurquesaClaro,
    secondary = LuumVerde,
    onSecondary = LuumFondo,
    secondaryContainer = LuumVerdeProfundo,
    onSecondaryContainer = LuumTexto,
    tertiary = LuumAmbar,
    onTertiary = LuumFondo,
    background = LuumFondo,
    onBackground = LuumTexto,
    surface = LuumSuperficie,
    onSurface = LuumTexto,
    surfaceVariant = LuumSuperficieAlta,
    onSurfaceVariant = LuumGris,
    surfaceTint = LuumTurquesa,
    // Menús desplegables, chips y diálogos (evita grises/morados por defecto)
    surfaceContainerLowest = LuumFondo,
    surfaceContainerLow = LuumSuperficie,
    surfaceContainer = LuumSuperficie,
    surfaceContainerHigh = LuumSuperficieAlta,
    surfaceContainerHighest = LuumSuperficieAlta,
    outline = LuumContorno,
    outlineVariant = LuumBordeCristal,
    error = LuumError,
    onError = LuumFondo
)

private val LuumLightColorScheme = lightColorScheme(
    primary = LuumTurquesaLogo,
    onPrimary = LuumLuzSuperficie,
    primaryContainer = LuumTurquesaClaro,
    onPrimaryContainer = LuumFondo,
    secondary = LuumVerdeLogo,
    onSecondary = LuumLuzSuperficie,
    secondaryContainer = LuumAzulMarino,
    onSecondaryContainer = LuumLuzSuperficie,
    tertiary = LuumAmbar,
    background = LuumLuzFondo,
    onBackground = LuumLuzTexto,
    surface = LuumLuzSuperficie,
    onSurface = LuumLuzTexto
)

@Composable
fun LuumEduTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Se desactiva el color dinámico de Android 12+ para conservar siempre la identidad de marca.
    MaterialTheme(
        colorScheme = if (darkTheme) LuumDarkColorScheme else LuumLightColorScheme,
        typography = Typography,
        content = content
    )
}

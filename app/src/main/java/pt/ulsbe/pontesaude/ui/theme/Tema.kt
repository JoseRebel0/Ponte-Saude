package pt.ulsbe.pontesaude.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * Esquema de cores claro baseado nas diretrizes do Material 3 e tons da ULSBE.
 */
private val EsquemaCores = lightColorScheme(
    primary = MarcaMedio,
    onPrimary = Color.White,
    primaryContainer = Marca,
    onPrimaryContainer = PrimeiroPlano,
    secondary = MarcaClaro,
    background = Tinta,
    onBackground = PrimeiroPlano,
    surface = Superficie,
    onSurface = PrimeiroPlano,
    surfaceVariant = Superficie2,
    onSurfaceVariant = Atenuado,
    error = Color(0xFFDC2626),
)

/**
 * Tema principal da aplicação com suporte a modo de acessibilidade (texto ampliado).
 */
@Composable
fun TemaPonteSaude(textoGrande: Boolean, content: @Composable () -> Unit) {
    val escala = if (textoGrande) 1.18f else 1f
    val base = TipografiaPadrao
    val tipografia = if (textoGrande) {
        Typography(
            displayLarge = base.displayLarge.copy(fontSize = (base.displayLarge.fontSize.value * escala).sp),
            displayMedium = base.displayMedium.copy(fontSize = (base.displayMedium.fontSize.value * escala).sp),
            displaySmall = base.displaySmall.copy(fontSize = (base.displaySmall.fontSize.value * escala).sp),
            headlineLarge = base.headlineLarge.copy(fontSize = (base.headlineLarge.fontSize.value * escala).sp),
            headlineMedium = base.headlineMedium.copy(fontSize = (base.headlineMedium.fontSize.value * escala).sp),
            headlineSmall = base.headlineSmall.copy(fontSize = (base.headlineSmall.fontSize.value * escala).sp),
            titleLarge = base.titleLarge.copy(fontSize = (base.titleLarge.fontSize.value * escala).sp),
            titleMedium = base.titleMedium.copy(fontSize = (base.titleMedium.fontSize.value * escala).sp),
            titleSmall = base.titleSmall.copy(fontSize = (base.titleSmall.fontSize.value * escala).sp),
            bodyLarge = base.bodyLarge.copy(fontSize = (base.bodyLarge.fontSize.value * escala).sp),
            bodyMedium = base.bodyMedium.copy(fontSize = (base.bodyMedium.fontSize.value * escala).sp),
            bodySmall = base.bodySmall.copy(fontSize = (base.bodySmall.fontSize.value * escala).sp),
            labelLarge = base.labelLarge.copy(fontSize = (base.labelLarge.fontSize.value * escala).sp),
            labelMedium = base.labelMedium.copy(fontSize = (base.labelMedium.fontSize.value * escala).sp),
            labelSmall = base.labelSmall.copy(fontSize = (base.labelSmall.fontSize.value * escala).sp),
        )
    } else {
        base
    }
    MaterialTheme(colorScheme = EsquemaCores, typography = tipografia, content = content)
}

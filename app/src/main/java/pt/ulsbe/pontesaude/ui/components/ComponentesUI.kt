package pt.ulsbe.pontesaude.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleanHands
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Elderly
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sick
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ulsbe.pontesaude.model.SeparadorApp
import pt.ulsbe.pontesaude.ui.theme.Marca
import pt.ulsbe.pontesaude.ui.theme.MarcaClaro
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio
import pt.ulsbe.pontesaude.ui.theme.MarcaProfundo
import pt.ulsbe.pontesaude.ui.theme.MarcaTopo
import pt.ulsbe.pontesaude.ui.theme.Superficie
import pt.ulsbe.pontesaude.ui.theme.TemaPonteSaude

/**
 * Cabeçalho padrão dos ecrãs com título, subtítulo, botão de voltar opcional e avatar do perfil.
 */
@Composable
fun CabecalhoEcra(
    titulo: String,
    subtitulo: String = "Ponte Saúde",
    iniciais: String,
    aoVoltar: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(MarcaMedio, MarcaTopo)))
            .padding(start = 20.dp, end = 20.dp, top = 42.dp, bottom = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (aoVoltar != null) {
                IconButton(
                    onClick = aoVoltar,
                    modifier = Modifier.background(MarcaProfundo, CircleShape),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", tint = Color.White)
                }
                Spacer(Modifier.size(10.dp))
            }
            Column {
                Text(subtitulo, color = Color.White.copy(alpha = 0.72f))
                Text(
                    titulo,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Cartão base arredondado para apresentar informações e blocos funcionais da aplicação.
 */
@Composable
fun CartaoApp(
    modifier: Modifier = Modifier,
    conteudo: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Superficie),
    ) {
        Box(Modifier.padding(18.dp)) { conteudo() }
    }
}

/**
 * Botão acessível para reprodução de áudio/voz de um texto ou instrução.
 */
@Composable
fun BotaoOuvir(
    rotulo: String = "Ouvir",
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = aoClicar,
        modifier = modifier.background(MarcaMedio, CircleShape).height(48.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White)
        Spacer(Modifier.size(8.dp))
        Text(rotulo, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Linha de definição com ícone, título descritivo e interruptor (Switch).
 */
@Composable
fun LinhaDefinicao(
    icone: ImageVector,
    titulo: String,
    subtitulo: String,
    selecionado: Boolean,
    aoMudarSelecao: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(48.dp).background(Marca, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icone, null, tint = MarcaClaro)
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.SemiBold)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = selecionado, onCheckedChange = aoMudarSelecao)
    }
}

fun iconeSeparador(separador: SeparadorApp): ImageVector = when (separador) {
    SeparadorApp.INICIO -> Icons.Rounded.Home
    SeparadorApp.SAUDE -> Icons.Rounded.Favorite
    SeparadorApp.CONSULTAS -> Icons.Rounded.LocalHospital
    SeparadorApp.MEDICACAO -> Icons.Rounded.Medication
    SeparadorApp.NOTAS -> Icons.Rounded.Description
    SeparadorApp.DEFINICOES -> Icons.Rounded.Settings
}

fun iconeModulo(nome: String): ImageVector = when (nome) {
    "badge" -> Icons.Rounded.Badge
    "message" -> Icons.AutoMirrored.Rounded.Message
    "heart" -> Icons.Rounded.Favorite
    "hand" -> Icons.Rounded.CleanHands
    "checklist" -> Icons.AutoMirrored.Rounded.ListAlt
    "pill" -> Icons.Rounded.Medication
    "warning" -> Icons.Rounded.Warning
    "qr" -> Icons.Rounded.QrCodeScanner
    "steps" -> Icons.Rounded.Elderly
    "child" -> Icons.Rounded.ChildCare
    "shield" -> Icons.Rounded.Shield
    "vaccine" -> Icons.Rounded.Vaccines
    "camera" -> Icons.Rounded.CameraAlt
    "phone" -> Icons.Rounded.Phone
    "water" -> Icons.Rounded.WaterDrop
    "bed" -> Icons.Rounded.Bed
    "light" -> Icons.Rounded.Lightbulb
    "add" -> Icons.Rounded.Add
    "accessibility" -> Icons.Rounded.AccessibilityNew
    "text" -> Icons.Rounded.TextFields
    "users" -> Icons.Rounded.Wc
    "health" -> Icons.Rounded.HealthAndSafety
    "sick" -> Icons.Rounded.Sick
    else -> Icons.Rounded.CheckCircle
}

// ==========================================
// PREVIEWS COM COMPOSABLE NO ANDROID STUDIO
// ==========================================

@Preview(name = "Cabeçalho de Ecrã", showBackground = true)
@Composable
fun PreVisualizacaoCabecalhoEcra() {
    TemaPonteSaude(textoGrande = false) {
        CabecalhoEcra(titulo = "Consultas", subtitulo = "Ponte Saúde", iniciais = "JO", aoVoltar = {})
    }
}

@Preview(name = "Cartão da Aplicação", showBackground = true)
@Composable
fun PreVisualizacaoCartaoApp() {
    TemaPonteSaude(textoGrande = false) {
        CartaoApp {
            Text("Exemplo de conteúdo do cartão da aplicação.")
        }
    }
}

@Preview(name = "Botão de Ouvir", showBackground = true)
@Composable
fun PreVisualizacaoBotaoOuvir() {
    TemaPonteSaude(textoGrande = false) {
        BotaoOuvir(rotulo = "Ouvir", aoClicar = {})
    }
}

@Preview(name = "Linha de Definição", showBackground = true)
@Composable
fun PreVisualizacaoLinhaDefinicao() {
    TemaPonteSaude(textoGrande = false) {
        LinhaDefinicao(
            icone = Icons.Rounded.TextFields,
            titulo = "Texto maior",
            subtitulo = "Aumenta o texto em toda a aplicação",
            selecionado = true,
            aoMudarSelecao = {},
        )
    }
}

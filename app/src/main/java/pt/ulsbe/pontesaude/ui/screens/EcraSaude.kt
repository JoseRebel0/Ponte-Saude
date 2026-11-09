package pt.ulsbe.pontesaude.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleanHands
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.RepositorioApp
import pt.ulsbe.pontesaude.data.calcularNivelSeguranca
import pt.ulsbe.pontesaude.model.CategoriaChecklist
import pt.ulsbe.pontesaude.model.ItemChecklist
import pt.ulsbe.pontesaude.model.ModuloSaude
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.RecompensaSeguranca
import pt.ulsbe.pontesaude.model.SeparadorApp
import pt.ulsbe.pontesaude.model.TipoModulo
import pt.ulsbe.pontesaude.model.VoucherResgatado
import pt.ulsbe.pontesaude.ui.ServicosDispositivo
import pt.ulsbe.pontesaude.ui.components.BotaoOuvir
import pt.ulsbe.pontesaude.ui.components.CabecalhoEcra
import pt.ulsbe.pontesaude.ui.components.CartaoApp
import pt.ulsbe.pontesaude.ui.components.ChecklistInterativa
import pt.ulsbe.pontesaude.ui.components.iconeModulo
import pt.ulsbe.pontesaude.ui.theme.Atenuado
import pt.ulsbe.pontesaude.ui.theme.Marca
import pt.ulsbe.pontesaude.ui.theme.MarcaClaro
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio
import pt.ulsbe.pontesaude.ui.theme.Superficie2
import pt.ulsbe.pontesaude.ui.theme.TemaPonteSaude

fun normalizarTexto(entrada: String): String {
    val normalizado = java.text.Normalizer.normalize(entrada, java.text.Normalizer.Form.NFD)
    return normalizado.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").lowercase().trim()
}

private val entradasGlossario = listOf(
    listOf("hipertensao", "hipertensão", "pressao alta", "tensao alta") to
        "A pressão do sangue nas artérias está mais alta do que devia. Tomar a medicação todos os dias ajuda a proteger o coração.",
    listOf("jejum") to
        "Não comer nem beber nada (só água) durante o número de horas indicado antes do exame.",
    listOf("anticoagulante", "varfarina") to
        "Medicamento que torna o sangue menos espesso, para evitar coágulos. Atenção a chás como o de erva-de-são-joão.",
    listOf("alta hospitalar", "alta") to
        "O médico considera que já pode ir para casa. Siga as indicações escritas e marque a consulta de seguimento.",
    listOf("diabetes", "glicemia", "acucar", "açúcar") to
        "Açúcar mais alto no sangue do que devia. Alimentação, exercício e medicação ajudam a manter controlado.",
)

fun pesquisarGlossario(consulta: String): String {
    val limpa = normalizarTexto(consulta)
    if (limpa.isBlank()) return "Escreva ou diga um termo para explicar."
    for ((chaves, explicacao) in entradasGlossario) {
        if (chaves.any { limpa.contains(normalizarTexto(it)) }) {
            return explicacao
        }
    }
    return "Em linguagem simples: peça ao seu médico para explicar este termo com calma, ou grave a dúvida nos Apontamentos para a próxima consulta."
}

/**
 * Ecrã de Saúde e Literacia com tradutor médico em linguagem simples, módulos educativos e Pull-to-Refresh suave.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcraSaude(
    perfil: Perfil,
    idModuloAberto: String?,
    concluidos: List<String>,
    repositorio: RepositorioApp? = null,
    plataforma: ServicosDispositivo,
    aoAbrirModulo: (String?) -> Unit,
    aoAlternarModulo: (String) -> Unit,
    aoNavegar: (SeparadorApp) -> Unit,
) {
    val modulo = repositorio?.modulosSaude?.find { it.id == idModuloAberto }
    if (modulo != null) {
        EcraModulo(
            modulo = modulo,
            perfil = perfil,
            concluidos = concluidos,
            repositorio = repositorio,
            plataforma = plataforma,
            aoVoltar = { aoAbrirModulo(null) },
            aoAlternar = aoAlternarModulo,
            aoNavegar = aoNavegar,
            aoAbrir = aoAbrirModulo,
        )
        return
    }
    var aAtualizar by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()
    var mostrarDialogoResgate by remember { mutableStateOf(false) }
    var mostrarDialogoVouchers by remember { mutableStateOf(false) }
    var voucherRecemResgatado by remember { mutableStateOf<VoucherResgatado?>(null) }
    var mensagemNotificacao by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        val tituloEcra = if (repositorio?.modoCuidador == true) "Saúde · ${perfil.nome}" else "Saúde"
        val subtituloEcra = if (repositorio?.modoCuidador == true) "Conta de ${perfil.nome} (Modo Cuidador)" else "Literacia e apoio"
        CabecalhoEcra(tituloEcra, subtituloEcra, perfil.iniciais)
        val estadoLista = rememberLazyListState()
        PullToRefreshBox(
            isRefreshing = aAtualizar,
            onRefresh = {
                aAtualizar = true
                escopo.launch {
                    delay(800) // Transição suave de carregamento
                    repositorio?.registarAtualizacao()
                    aAtualizar = false
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                state = estadoLista,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
            if (repositorio?.modoCuidador == true) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFF10B981), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(perfil.iniciais, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(perfil.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFDCFCE7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text("Modo Cuidador", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                    }
                                }
                                Text(
                                    "Nº SNS: ${perfil.numeroUtente} · ${perfil.idadeFormatada}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Atenuado,
                                )
                                if (perfil.alergias.isNotBlank() && !perfil.alergias.contains("Sem", ignoreCase = true)) {
                                    Text(
                                        "⚠️ Alergias: ${perfil.alergias}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // PROGRAMA DE LITERACIA & SEGURANÇA (HUD POLIDO E EQUILIBRADO)
            item {
                val pontos = repositorio?.pontosSeguranca ?: 50
                val infoNivel = calcularNivelSeguranca(pontos)
                val vouchers = repositorio?.vouchersResgatados.orEmpty()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // CABEÇALHO DO PROGRAMA COM PESO CORRETO PARA NÃO COMPRIMIR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFFFEF3C7), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Programa de Literacia & Segurança",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "ULSBE · Participação Ativa do Utente",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Atenuado,
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // BADGE DO NÍVEL
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = "★ Nível ${infoNivel.nivel}",
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        // PAINEL DE SALDO E PROGRESSÃO (SEM SOBREPOSIÇÃO)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Superficie2)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Saldo Acumulado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Atenuado,
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "$pontos",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MarcaMedio,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "pontos",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MarcaMedio,
                                            modifier = Modifier.padding(bottom = 2.dp),
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Classificação",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Atenuado,
                                    )
                                    Text(
                                        text = infoNivel.titulo,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }

                            // BARRA DE PROGRESSO
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "Progresso para o próximo nível",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Atenuado,
                                    )
                                    Text(
                                        text = "$pontos / ${infoNivel.pontosProximoNivel} pts",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Atenuado,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { infoNivel.progresso },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFFF59E0B),
                                    trackColor = Color(0xFFE2E8F0),
                                )
                            }
                        }

                        // FRASE CONCISA INFORMATIVA
                        Text(
                            text = "💡 Ouça os módulos clínicos e veja os vídeos do SNS para ganhar pontos e resgatar recompensas na sua USF.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Atenuado,
                            lineHeight = 18.sp,
                        )

                        // BOTÕES DE RESGATE E VOUCHERS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = { mostrarDialogoResgate = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                            ) {
                                Icon(Icons.Rounded.CardGiftcard, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Resgatar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            FilledTonalButton(
                                onClick = { mostrarDialogoVouchers = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.ConfirmationNumber, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Vouchers (${vouchers.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // LISTA DE MÓDULOS DE SEGURANÇA COM CARTÕES BEM ESTRUTURADOS
            item {
                Column {
                    Text("Módulos de Segurança do Doente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("Toque para abrir, ouvir e acumular pontos de segurança", color = Atenuado, style = MaterialTheme.typography.bodySmall)
                }
            }
            val listaModulos = repositorio?.modulosSaude ?: emptyList()
            items(listaModulos, key = { it.id }) { item ->
                val concluido = item.id in concluidos
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { aoAbrirModulo(item.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(if (concluido) Color(0xFFDCFCE7) else MarcaMedio.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                iconeModulo(item.icone),
                                contentDescription = null,
                                tint = if (concluido) Color(0xFF15803D) else MarcaMedio,
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = item.titulo,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = item.subtitulo,
                                style = MaterialTheme.typography.bodySmall,
                                color = Atenuado,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (concluido) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = if (concluido) Color(0xFF15803D) else Color(0xFFD97706),
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = if (concluido) "50 pts Concluído ✓" else "+50 pts",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (concluido) Color(0xFF15803D) else Color(0xFFB45309),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Atenuado, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // LISTA DE VÍDEOS OFICIAIS DO SNS COM DESIGN ESPAÇOSO
            item {
                Column {
                    Text("Biblioteca de Saúde & Vídeos Oficiais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("Vídeos educativos do SNS no YouTube · Assista e ganhe +30 pontos de literacia", color = Atenuado, style = MaterialTheme.typography.bodySmall)
                }
            }
            val listaVideos = repositorio?.videosBiblioteca ?: emptyList()
            items(listaVideos, key = { it.id }) { video ->
                val jaVisto = repositorio?.videosAssistidos?.contains(video.id) == true
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            plataforma.abrirLigacao(video.urlYoutube)
                            val pts = repositorio?.premiarVerVideo(video.id) ?: 0
                            if (pts > 0) {
                                mensagemNotificacao = "⭐ Parabéns! Ganhou +$pts Pontos por assistir ao vídeo oficial «${video.titulo}»!"
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(if (jaVisto) Color(0xFFDCFCE7) else Color(0xFFEFF6FF), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Ver vídeo",
                                tint = if (jaVisto) Color(0xFF15803D) else MarcaMedio,
                                modifier = Modifier.size(26.dp),
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = video.titulo,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = video.descricao,
                                style = MaterialTheme.typography.bodySmall,
                                color = Atenuado,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Superficie2)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "YouTube · ${video.duracao}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (jaVisto) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = if (jaVisto) Color(0xFF15803D) else Color(0xFFD97706),
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = if (jaVisto) "30 pts Visto ✓" else "+30 pts",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (jaVisto) Color(0xFF15803D) else Color(0xFFB45309),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Atenuado, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // BOTÕES DE CONTACTO DE EMERGÊNCIA
            item { BotaoContacto("Ligar SNS 24 · 808 24 24 24", MarcaMedio) { plataforma.marcarNumero("808242424") } }
            item { BotaoContacto("Emergência 112", Color(0xFFDC2626)) { plataforma.marcarNumero("112") } }
        }
    }
}

    if (mostrarDialogoResgate) {
        DialogoResgateRecompensas(
            pontosAtuais = repositorio?.pontosSeguranca ?: 50,
            recompensas = repositorio?.catalogoRecompensas ?: emptyList(),
            aoResgatar = { recompensa ->
                mostrarDialogoResgate = false
                val voucher = repositorio?.resgatarRecompensa(recompensa)
                if (voucher != null) {
                    voucherRecemResgatado = voucher
                }
            },
            aoFechar = { mostrarDialogoResgate = false },
        )
    }

    if (mostrarDialogoVouchers) {
        DialogoMeusVouchers(
            vouchers = repositorio?.vouchersResgatados.orEmpty(),
            aoFechar = { mostrarDialogoVouchers = false },
        )
    }

    voucherRecemResgatado?.let { voucher ->
        DialogoSucessoVoucher(
            voucher = voucher,
            aoFechar = { voucherRecemResgatado = null },
        )
    }

    mensagemNotificacao?.let { msg ->
        AlertDialog(
            onDismissRequest = { mensagemNotificacao = null },
            icon = { Icon(Icons.Rounded.NotificationsActive, null, tint = MarcaMedio) },
            title = { Text("Notificações de Segurança") },
            text = { Text(msg) },
            confirmButton = { Button(onClick = { mensagemNotificacao = null }) { Text("Entendido") } },
        )
    }
}

@Composable
private fun BotaoContacto(rotulo: String, cor: Color, aoClicar: () -> Unit) {
    Button(aoClicar, Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = cor)) {
        Icon(Icons.Rounded.Phone, null)
        Spacer(Modifier.size(8.dp))
        Text(rotulo)
    }
}

@Composable
fun EcraModulo(
    modulo: ModuloSaude,
    perfil: Perfil,
    concluidos: List<String>,
    repositorio: RepositorioApp? = null,
    plataforma: ServicosDispositivo,
    aoVoltar: () -> Unit,
    aoAlternar: (String) -> Unit,
    aoNavegar: (SeparadorApp) -> Unit,
    aoAbrir: (String?) -> Unit,
) {
    var mensagemPontos by remember { mutableStateOf<String?>(null) }
    val estadoModulo = rememberLazyListState()
    Column {
        CabecalhoEcra(modulo.titulo, "Módulo", perfil.iniciais, aoVoltar)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            state = estadoModulo,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (modulo.tipo != TipoModulo.CRIANCAS) {
                item {
                    CartaoApp {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            modulo.conteudos.forEach {
                                Row {
                                    Icon(Icons.Rounded.Check, null, tint = MarcaMedio)
                                    Spacer(Modifier.size(9.dp))
                                    Text(it, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
            item {
                BotaoOuvir("Ouvir este módulo (+25 pts)", {
                    plataforma.falar(modulo.fala)
                    val pts = repositorio?.premiarOuvirModulo(modulo.id) ?: 0
                    if (pts > 0) {
                        mensagemPontos = "🎉 Parabéns! Ganhou +$pts Pontos de Segurança por ouvir este módulo!"
                    }
                }, Modifier.fillMaxWidth())
            }
            when (modulo.tipo) {
                TipoModulo.LAVAGEM_MAOS -> item { LavagemMaos(plataforma) }
                TipoModulo.CIRURGIA -> item { ModuloCirurgia() }
                TipoModulo.QUESTIONARIO -> item { ModuloQuestionario(plataforma) }
                TipoModulo.LEITURA -> item { ModuloLeitura(plataforma) }
                TipoModulo.QUEDAS -> item {
                    CartaoChecklist(
                        titulo = "A minha casa é segura?",
                        entradas = listOf(
                            "Boa iluminação em todas as divisões",
                            "Calçado fechado e antiderrapante",
                            "Tapetes presos ao chão",
                            "Levantar-me devagar, sem pressa",
                            "Perguntar quais medicamentos aumentam o risco",
                            "Pedir ajuda quando preciso",
                        ),
                        repositorio = repositorio,
                        plataforma = plataforma,
                    )
                }
                TipoModulo.CRIANCAS -> item { ModuloCriancas(plataforma) }
                TipoModulo.CONTACTOS -> item { ModuloContactos(plataforma) }
                TipoModulo.RESUMO -> item { ModuloResumo(concluidos, repositorio?.modulosSaude ?: emptyList(), aoAbrir) }
                TipoModulo.PERGUNTAS -> item { Button({ aoNavegar(SeparadorApp.NOTAS) }, Modifier.fillMaxWidth()) { Text("Preparar perguntas para o médico") } }
                TipoModulo.MEDICAMENTOS -> item { Button({ aoNavegar(SeparadorApp.MEDICACAO) }, Modifier.fillMaxWidth()) { Text("Abrir a minha lista") } }
                TipoModulo.INFO -> Unit
            }
            item {
                val feito = modulo.id in concluidos
                Button(
                    onClick = {
                        val pts = repositorio?.alternarModuloComPontos(modulo.id) ?: run {
                            aoAlternar(modulo.id)
                            0
                        }
                        if (pts > 0) {
                            mensagemPontos = "⭐ Módulo concluído! Ganhou +$pts Pontos de Segurança!"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (feito) Marca else Superficie2, contentColor = if (feito) MarcaClaro else MaterialTheme.colorScheme.onSurface),
                ) {
                    Icon(Icons.Rounded.Star, null, tint = if (feito) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.size(8.dp))
                    Text(if (feito) "Módulo concluído (50 pts)" else "Marcar como visto (+50 pts)")
                }
            }
        }
    }

    if (mensagemPontos != null) {
        AlertDialog(
            onDismissRequest = { mensagemPontos = null },
            icon = { Icon(Icons.Rounded.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(36.dp)) },
            title = { Text("Recompensa de Segurança!", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(mensagemPontos.orEmpty())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Saldo atual: ${repositorio?.pontosSeguranca ?: 50} pontos",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MarcaMedio,
                    )
                }
            },
            confirmButton = {
                Button(onClick = { mensagemPontos = null }) {
                    Text("Continuar")
                }
            },
        )
    }
}

@Composable
private fun CartaoChecklist(
    titulo: String,
    entradas: List<String>,
    subtitulo: String? = "Checklist de Segurança do Utente",
    repositorio: RepositorioApp? = null,
    plataforma: ServicosDispositivo? = null,
) {
    val itens = remember(titulo, entradas) {
        entradas.mapIndexed { idx, entrada ->
            val cat = when {
                entrada.contains("medicamento", ignoreCase = true) -> CategoriaChecklist.MEDICACAO
                entrada.contains("jejum", ignoreCase = true) -> CategoriaChecklist.JEJUM
                entrada.contains("chão", ignoreCase = true) || entrada.contains("calçado", ignoreCase = true) || entrada.contains("limpa", ignoreCase = true) -> CategoriaChecklist.SEGURANCA
                else -> CategoriaChecklist.GERAL
            }
            ItemChecklist("chk_${kotlin.math.abs(titulo.hashCode())}_$idx", entrada, cat)
        }
    }
    ChecklistInterativa(
        titulo = titulo,
        subtitulo = subtitulo,
        itensPredefinidos = itens,
        chavePersistencia = "saude_${kotlin.math.abs(titulo.hashCode())}",
        repositorio = repositorio,
        plataforma = plataforma,
        permiteAdicionar = true,
    )
}

private val passosLavagem = listOf(
    "Molhe as mãos com água",
    "Ensaboe as palmas das mãos",
    "Esfregue entre os dedos",
    "Lave as costas das mãos",
    "Lave os polegares e as unhas",
    "Enxague e seque bem",
)

@Composable
private fun LavagemMaos(plataforma: ServicosDispositivo) {
    var passo by remember { mutableStateOf(0) }
    var aReproduzir by remember { mutableStateOf(false) }
    LaunchedEffect(aReproduzir, passo) {
        if (aReproduzir) {
            plataforma.falar(passosLavagem[passo])
            delay(3500)
            if (passo < passosLavagem.lastIndex) passo++ else aReproduzir = false
        }
    }
    CartaoApp {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(150.dp).background(Marca, CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (passo == 0 || passo == passosLavagem.lastIndex) Icons.Rounded.WaterDrop else Icons.Rounded.CleanHands, null, tint = MarcaClaro, modifier = Modifier.size(76.dp))
            }
            Text("Passo ${passo + 1} de ${passosLavagem.size}", color = Atenuado, modifier = Modifier.padding(top = 12.dp))
            Text(passosLavagem[passo], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (!aReproduzir && passo == passosLavagem.lastIndex) passo = 0
                    aReproduzir = !aReproduzir
                    if (!aReproduzir) plataforma.parar()
                }) {
                    Icon(if (aReproduzir) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (aReproduzir) "Pausa" else "Ver vídeo")
                }
                TextButton(onClick = {
                    passo = (passo + 1) % passosLavagem.size
                    plataforma.falar(passosLavagem[passo])
                }) { Text("Seguinte") }
            }
        }
    }
}

@Composable
private fun ModuloCirurgia() {
    var rascunho by remember { mutableStateOf("") }
    var mensagens by remember { mutableStateOf(emptyList<String>()) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CartaoChecklist("Antes da cirurgia", listOf("Jejum conforme indicado", "Perguntar que medicamentos suspender", "Confirmar o local da operação", "Perguntar antes de assinar o consentimento"))
        CartaoChecklist("Depois, em casa", listOf("Manter a ferida limpa e seca", "Tomar a medicação indicada", "Febre, pus ou sangramento? Ligar ao SNS 24"))
        CartaoApp {
            Column {
                Text("Chat com o profissional de serviço", fontWeight = FontWeight.SemiBold)
                Text("Para dúvidas. Em urgência, ligue 112.", color = Atenuado, style = MaterialTheme.typography.bodySmall)
                mensagens.forEachIndexed { indice, mensagem ->
                    Text(mensagem, Modifier.padding(top = 7.dp).background(if (indice % 2 == 0) MarcaMedio else Marca, RoundedCornerShape(14.dp)).padding(10.dp), color = if (indice % 2 == 0) Color.White else MaterialTheme.colorScheme.onSurface)
                }
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(rascunho, { rascunho = it }, Modifier.weight(1f), placeholder = { Text("Escreva a sua dúvida…") })
                    FilledIconButton(onClick = {
                        if (rascunho.isNotBlank()) {
                            mensagens = mensagens + rascunho.trim() + "Recebemos a sua dúvida. Um profissional responde assim que possível."
                            rascunho = ""
                        }
                    }) { Icon(Icons.AutoMirrored.Rounded.Send, "Enviar") }
                }
            }
        }
    }
}

private data class ItemQuiz(val pergunta: String, val resposta: Boolean, val explicacao: String)
private val itensQuiz = listOf(
    ItemQuiz("Posso tomar dois medicamentos com o mesmo princípio ativo?", false, "Não. Pode estar a tomar a dose a dobrar, mesmo com caixas diferentes."),
    ItemQuiz("Se suspeito de um erro, devo esperar para ver o que acontece.", false, "Não. Diga logo a um profissional ou ligue ao SNS 24."),
    ItemQuiz("Os genéricos podem ter nomes diferentes do medicamento original.", true, "Sim. Confirme sempre o princípio ativo escrito na caixa."),
)

@Composable
private fun ModuloQuestionario(plataforma: ServicosDispositivo) {
    val respostas = remember { mutableStateMapOf<Int, Boolean>() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        itensQuiz.forEachIndexed { indice, item ->
            CartaoApp {
                Column {
                    Text(item.pergunta, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(true to "Verdade", false to "Falso").forEach { (valor, rotulo) ->
                            val selecionado = respostas[indice] == valor
                            val correto = valor == item.resposta
                            Button(
                                onClick = { respostas[indice] = valor; plataforma.falar(item.explicacao) },
                                enabled = indice !in respostas,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selecionado) {
                                        if (correto) MarcaMedio else Color(0xFFDC2626)
                                    } else Superficie2,
                                    contentColor = if (selecionado) Color.White else MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = if (selecionado) {
                                        if (correto) MarcaMedio else Color(0xFFDC2626)
                                    } else Superficie2.copy(alpha = 0.5f),
                                    disabledContentColor = if (selecionado) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                ),
                            ) { Text(rotulo) }
                        }
                    }
                    if (indice in respostas) Text(item.explicacao, Modifier.padding(top = 10.dp).background(Marca, RoundedCornerShape(14.dp)).padding(10.dp))
                }
            }
        }
        if (respostas.size == itensQuiz.size) {
            val score = itensQuiz.withIndex().count { (indice, item) -> respostas[indice] == item.resposta }
            CartaoApp { Text("$score de ${itensQuiz.size} certas", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ModuloLeitura(plataforma: ServicosDispositivo) {
    var resultado by remember { mutableStateOf(false) }
    val texto = "Ramipril 5 miligramas. Tomar um comprimido de manhã, com água. Guardar abaixo de 25 graus. Devolver as sobras à farmácia."
    CartaoApp {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.QrCodeScanner, null, tint = MarcaClaro, modifier = Modifier.size(96.dp))
            Button(onClick = { resultado = true; plataforma.falar(texto) }, Modifier.fillMaxWidth()) { Text("Ler QR / código de barras (demonstração)") }
            if (resultado) {
                Column(Modifier.padding(top = 12.dp).background(Marca, RoundedCornerShape(16.dp)).padding(12.dp)) {
                    Text("Ramipril 5 mg", fontWeight = FontWeight.SemiBold)
                    Text(texto.substringAfter(". "))
                    BotaoOuvir("Ouvir outra vez", { plataforma.falar(texto) }, Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ModuloCriancas(plataforma: ServicosDispositivo) {
    val aprendeu = remember { mutableStateMapOf<Int, Boolean>() }
    val entradas = listOf(
        Triple("Lavar as mãos", "Vamos lavar as mãos com sabão. Esfrega, esfrega, esfrega!", Icons.Rounded.WaterDrop),
        Triple("Não mexer no cateter", "O tubinho no teu braço ajuda-te a ficar melhor. Não lhe mexas.", Icons.Rounded.Medication),
        Triple("Grades da cama", "As grades protegem-te. Não passes por cima delas.", Icons.Rounded.DarkMode),
        Triple("A minha pulseira", "A pulseira tem o teu nome. Assim, todos sabem quem tu és.", Icons.Rounded.ChildCare),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CartaoApp {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(90.dp).background(MarcaMedio, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ChildCare, null, tint = Color.White, modifier = Modifier.size(54.dp))
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("Eu sou o Tico!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    BotaoOuvir("Ouvir o Tico", { plataforma.falar("Olá! Eu sou o Tico. Toca nos desenhos e eu explico.") })
                }
            }
        }
        entradas.chunked(2).forEachIndexed { indiceLinha, linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                linha.forEachIndexed { indiceColuna, entrada ->
                    val indice = indiceLinha * 2 + indiceColuna
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(Marca)
                            .clickable { plataforma.falar(entrada.second); aprendeu[indice] = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(entrada.third, entrada.first, tint = MarcaClaro, modifier = Modifier.size(60.dp))
                        if (aprendeu[indice] == true) {
                            Icon(Icons.Rounded.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuloContactos(plataforma: ServicosDispositivo) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BotaoContacto("SNS 24 · 808 24 24 24", MarcaMedio) { plataforma.marcarNumero("808242424") }
        BotaoContacto("Emergência · 112", Color(0xFFDC2626)) { plataforma.marcarNumero("112") }
        CartaoApp { Text("Gabinete do Cidadão · contacto a definir pela ULSBE") }
    }
}

@Composable
private fun ModuloResumo(
    concluidos: List<String>,
    modulos: List<ModuloSaude>,
    aoAbrir: (String?) -> Unit,
) {
    val respostas = remember { mutableStateMapOf<Int, Boolean>() }
    val perguntas = listOf("Sei porque uso a pulseira.", "Sei dizer o nome dos meus medicamentos.", "Digo quando não percebo.", "Sei a quem ligar se tiver dúvidas.")
    val restantes = modulos.filter { it.tipo != TipoModulo.RESUMO && it.id !in concluidos }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CartaoApp {
            Column {
                Text("Questionário final", fontWeight = FontWeight.SemiBold)
                perguntas.forEachIndexed { indice, pergunta ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).background(Superficie2, RoundedCornerShape(15.dp))
                            .clickable { respostas[indice] = respostas[indice] != true }.padding(11.dp),
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = if (respostas[indice] == true) MarcaMedio else Atenuado)
                        Spacer(Modifier.size(8.dp))
                        Text(pergunta)
                    }
                }
            }
        }
        CartaoApp {
            Column {
                Text(if (restantes.isEmpty()) "Viu todos os módulos!" else "Ainda por ver", fontWeight = FontWeight.SemiBold)
                restantes.forEach { modulo ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 7.dp).clickable { aoAbrir(modulo.id) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(iconeModulo(modulo.icone), null, tint = MarcaClaro)
                        Spacer(Modifier.size(8.dp))
                        Text(modulo.titulo, Modifier.weight(1f))
                        Icon(Icons.Rounded.ChevronRight, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoResgateRecompensas(
    pontosAtuais: Int,
    recompensas: List<RecompensaSeguranca>,
    aoResgatar: (RecompensaSeguranca) -> Unit,
    aoFechar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoFechar,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Catálogo de Recompensas", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("Segurança do Doente · ULSBE", color = Atenuado, style = MaterialTheme.typography.bodySmall)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        "$pontosAtuais pts",
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(recompensas) { recompensa ->
                    val suficiente = pontosAtuais >= recompensa.pontos
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = recompensa.titulo,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (suficiente) Color(0xFFDCFCE7) else Color(0xFFF1F5F9))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = "${recompensa.pontos} pts",
                                    fontWeight = FontWeight.Bold,
                                    color = if (suficiente) Color(0xFF15803D) else Atenuado,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }

                        Text(
                            text = recompensa.descricao,
                            color = Atenuado,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.LocationOn, null, tint = MarcaMedio, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = recompensa.localLevantamento,
                                color = Atenuado,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        Button(
                            onClick = { aoResgatar(recompensa) },
                            enabled = suficiente,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                        ) {
                            Text(
                                text = if (suficiente) "Resgatar por ${recompensa.pontos} pts" else "Faltam ${recompensa.pontos - pontosAtuais} pts",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = aoFechar) {
                Text("Fechar")
            }
        },
    )
}

@Composable
private fun DialogoMeusVouchers(
    vouchers: List<VoucherResgatado>,
    aoFechar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoFechar,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Os Meus Vouchers", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFF6FF))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${vouchers.size} ativos",
                        color = MarcaMedio,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        text = {
            if (vouchers.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.ConfirmationNumber, null, tint = Atenuado, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Ainda não tem vouchers resgatados.", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ganhe pontos ouvindo e concluindo os módulos de segurança para resgatar recompensas!",
                        color = Atenuado,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(vouchers) { voucher ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, Color(0xFF0F3D9E).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Título da recompensa (sempre com largura total e quebra limpa)
                            Text(
                                text = voucher.titulo,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                            )

                            // Data de resgate e pontos usados
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Resgatado a ${voucher.dataResgate}",
                                    color = Atenuado,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Text(
                                    text = "${voucher.pontosUsados} pts usados",
                                    color = Color(0xFFD97706),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            // Caixa do Código do Voucher (com formato de bilhete espaçoso)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F3D9E).copy(alpha = 0.08f))
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "CÓDIGO DE LEVANTAMENTO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Atenuado,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = voucher.codigo,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF0F3D9E),
                                        letterSpacing = 3.sp,
                                    )
                                }
                            }

                            // Local de levantamento
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(Icons.Rounded.LocationOn, null, tint = MarcaMedio, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = voucher.localLevantamento,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            Text(
                                text = "Apresente este código no balcão de atendimento.",
                                color = Atenuado,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = aoFechar) {
                Text("Fechar")
            }
        },
    )
}

@Composable
private fun DialogoSucessoVoucher(
    voucher: VoucherResgatado,
    aoFechar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoFechar,
        modifier = Modifier.fillMaxWidth(0.95f),
        icon = {
            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(42.dp))
        },
        title = {
            Text("Recompensa Resgatada!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Parabéns pelo seu compromisso com a segurança do doente!", textAlign = TextAlign.Center)
                Text(voucher.titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFF0F3D9E))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F3D9E).copy(alpha = 0.08f))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "CÓDIGO DE LEVANTAMENTO",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Atenuado,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            voucher.codigo,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF0F3D9E),
                            letterSpacing = 3.sp,
                        )
                    }
                }
                Text(
                    "Local de levantamento:\n${voucher.localLevantamento}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Atenuado,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Button(onClick = aoFechar, modifier = Modifier.fillMaxWidth()) {
                Text("Concluir")
            }
        },
    )
}

// ==========================================
// PREVIEWS COM COMPOSABLE NO ANDROID STUDIO
// ==========================================

@Preview(name = "Ecrã Saúde", showBackground = true)
@Composable
fun PreVisualizacaoEcraSaude() {
    val perfil = Perfil(
        id = "1",
        nome = "Utente Demonstração",
        nomeCurto = "Utente",
        relacao = "Utente",
        iniciais = "UD",
        numeroUtente = "189 423 761",
        tipoSanguineo = "A+",
        alergias = "Sem alergias",
    )
    TemaPonteSaude(textoGrande = false) {
        EcraSaude(
            perfil = perfil,
            idModuloAberto = null,
            concluidos = emptyList(),
            plataforma = ServicosDispositivo(null),
            aoAbrirModulo = {},
            aoAlternarModulo = {},
            aoNavegar = {},
        )
    }
}

@Preview(name = "Ecrã Módulo Detalhe", showBackground = true)
@Composable
fun PreVisualizacaoEcraModulo() {
    val perfil = Perfil(
        id = "1",
        nome = "Utente Demonstração",
        nomeCurto = "Utente",
        relacao = "Utente",
        iniciais = "UD",
        numeroUtente = "189 423 761",
        tipoSanguineo = "A+",
        alergias = "Sem alergias",
    )
    val modulo = ModuloSaude(
        id = "identificacao",
        titulo = "Identificação inequívoca",
        subtitulo = "Pulseira e nome certo",
        icone = "badge",
        tipo = TipoModulo.INFO,
        conteudos = listOf("A pulseira serve para garantir a sua segurança."),
        fala = "A pulseira serve para garantir a sua segurança.",
    )
    TemaPonteSaude(textoGrande = false) {
        EcraModulo(
            modulo = modulo,
            perfil = perfil,
            concluidos = listOf(modulo.id),
            plataforma = ServicosDispositivo(null),
            aoVoltar = {},
            aoAlternar = {},
            aoNavegar = {},
            aoAbrir = {},
        )
    }
}

@file:Suppress("SpellCheckingInspection")

package pt.ulsbe.pontesaude.ui.screens.medicacao

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.util.ProcessadorImagem
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.util.ServicosDispositivo
import pt.ulsbe.pontesaude.ui.components.BarraScrollLateralSuave
import pt.ulsbe.pontesaude.ui.components.BotaoOuvir
import pt.ulsbe.pontesaude.ui.components.CabecalhoEcra
import pt.ulsbe.pontesaude.ui.theme.Atenuado
import pt.ulsbe.pontesaude.ui.theme.MarcaClaro
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio
import pt.ulsbe.pontesaude.ui.theme.MarcaProfundo
import pt.ulsbe.pontesaude.ui.theme.Superficie2
import pt.ulsbe.pontesaude.ui.theme.TemaPonteSaude

fun formatarHoraParaFala(hora: String): String {
    val partes = hora.split(":")
    if (partes.size == 2) {
        val h = partes[0].toIntOrNull()
        val m = partes[1].toIntOrNull()
        if (h != null && m != null) {
            return when (m) {
                0 -> "às $h horas"
                30 -> "às $h e meia"
                else -> "às $h horas e $m minutos"
            }
        }
    }
    return "às $hora"
}

/**
 * Ecrã de Medicação moderno, clínico e acessível.
 * Inclui:
 * - Gestão de prescrições e suporte a múltiplos horários por toma (ex: 08:00, 14:00, 20:00).
 * - Edição completa de medicamentos existentes.
 * - Alarmes nativos sincronizados no AlarmManager.
 * - Deteção de duplicações terapêuticas de princípio ativo.
 * - Registo de suplementos naturais e ervanária sem espaços vazios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcraMedicacao(
    perfil: Perfil,
    viewModel: MedicacaoViewModel? = null,
    aoSincronizar: (suspend () -> Unit)? = null,
    plataforma: ServicosDispositivo,
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val contexto = LocalContext.current
    val medicamentos = if (uiState != null && uiState.perfil?.id == perfil.id) {
        uiState.medicamentos
    } else {
        perfil.medicamentos
    }
    var aAtualizarLocal by remember { mutableStateOf(false) }
    val aAtualizar = uiState?.emCarregamento ?: aAtualizarLocal
    val escopo = rememberCoroutineScope()

    var mostrarDialogoFormulario by remember { mutableStateOf(false) }
    var medicamentoEmEdicao by remember { mutableStateOf<Medicamento?>(null) }
    var pendenteApagar by remember { mutableStateOf<Medicamento?>(null) }
    var mensagemAlarme by remember { mutableStateOf<String?>(null) }
    var medicamentoParaFoto by remember { mutableStateOf<Medicamento?>(null) }
    var medicamentoParaVerFoto by remember { mutableStateOf<Medicamento?>(null) }
    var confirmarRemoverFotoMed by remember { mutableStateOf<Medicamento?>(null) }
    var filtroHorario by remember { mutableStateOf("Todos") } // "Todos", "Ativos", "Manhã", "Tarde", "Noite"
    var rascunhoProduto by remember { mutableStateOf("") }
    var pdfGeradoUri by remember { mutableStateOf<Uri?>(null) }
    var pdfNomeFicheiro by remember { mutableStateOf("") }
    var mostrarDialogoPdf by remember { mutableStateOf(false) }
    var medParaExplicarIa by remember { mutableStateOf<Medicamento?>(null) }
    var textoExplicacaoIa by remember { mutableStateOf<String?>(null) }
    var aCarregarExplicacaoIa by remember { mutableStateOf(false) }
    var erroExplicacaoIa by remember { mutableStateOf<String?>(null) }
    var avisoSemInternetMedicacao by remember { mutableStateOf(false) }

    LaunchedEffect(perfil.id) {
        viewModel?.recarregar()
        mostrarDialogoFormulario = false
        medicamentoEmEdicao = null
        pendenteApagar = null
        medicamentoParaFoto = null
        medicamentoParaVerFoto = null
        confirmarRemoverFotoMed = null
        medParaExplicarIa = null
        textoExplicacaoIa = null
        erroExplicacaoIa = null
    }

    val pedirExplicacaoIa: (Medicamento) -> Unit = { med ->
        if (!plataforma.temConexaoInternet()) {
            avisoSemInternetMedicacao = true
        } else {
            medParaExplicarIa = med
            viewModel?.pedirExplicacaoIa(med.nome, med.principioAtivo, med.finalidade)
        }
    }

    val duplicados = medicamentos
        .filter { medicamento ->
            medicamento.principioAtivo.isNotBlank() &&
                medicamentos.count { it.principioAtivo.trim().equals(medicamento.principioAtivo.trim(), ignoreCase = true) } > 1
        }
        .map { it.principioAtivo.trim().lowercase() }
        .distinct()

    val agendaAtiva = uiState?.agendaEAlarmesAtivos ?: true

    // Sincronização automática com os alarmes programados do telemóvel para todos os horários
    LaunchedEffect(agendaAtiva, medicamentos) {
        if (agendaAtiva) {
            medicamentos.filter { it.ativo }.forEach { med ->
                med.listaHorarios.forEachIndexed { idx, horaStr ->
                    val partes = horaStr.split(":")
                    val h = partes.getOrNull(0)?.toIntOrNull() ?: 8
                    val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
                    plataforma.agendarAlarmeMedicacao(med.id * 100L + idx, med.nome, med.finalidade, h, m)
                }
            }
        } else {
            medicamentos.forEach { med ->
                for (idx in 0..10) {
                    plataforma.cancelarAlarmeMedicacao(med.id * 100L + idx)
                }
                plataforma.cancelarAlarmeMedicacao(med.id)
            }
        }
    }

    val medicamentosFiltrados = remember(medicamentos, filtroHorario) {
        when (filtroHorario) {
            "Manhã" -> medicamentos.filter { med ->
                med.listaHorarios.any { hStr ->
                    val h = hStr.split(":").firstOrNull()?.toIntOrNull() ?: 0
                    h in 5..11
                }
            }
            "Tarde" -> medicamentos.filter { med ->
                med.listaHorarios.any { hStr ->
                    val h = hStr.split(":").firstOrNull()?.toIntOrNull() ?: 0
                    h in 12..18
                }
            }
            "Noite" -> medicamentos.filter { med ->
                med.listaHorarios.any { hStr ->
                    val h = hStr.split(":").firstOrNull()?.toIntOrNull() ?: 0
                    h !in 5..18
                }
            }
            "Ativos" -> medicamentos.filter { it.ativo }
            else -> medicamentos
        }
    }

    val totalAtivos = medicamentos.count { it.ativo }
    val proximaToma = remember(medicamentos) {
        medicamentos.filter { it.ativo }.minByOrNull { it.listaHorarios.firstOrNull() ?: it.hora }
    }
    val estadoLista = rememberLazyListState()

    val modoCuidadorAtivo = uiState?.modoCuidador ?: false
    val tituloEcra = if (modoCuidadorAtivo) "Medicação · ${perfil.nome}" else "Medicação"
    val subtituloEcra = if (modoCuidadorAtivo) "Conta de ${perfil.nome} (Modo Cuidador)" else "Gestor visual e alarmes"

    Column(Modifier.fillMaxSize()) {
        CabecalhoEcra(tituloEcra, subtituloEcra, perfil.iniciais)

        PullToRefreshBox(
            isRefreshing = aAtualizar,
            onRefresh = {
                if (viewModel != null) {
                    viewModel.dispararAtualizacao(aoSincronizar)
                } else {
                    aAtualizarLocal = true
                    escopo.launch {
                        aoSincronizar?.invoke()
                        delay(500.milliseconds)
                        aAtualizarLocal = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = estadoLista,
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. FICHA DA CONTA CLÍNICA EM MODO CUIDADOR
            if (modoCuidadorAtivo) {
                item {
                    Box(Modifier.padding(horizontal = 20.dp)) {
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
                                    Text(
                                        text = perfil.iniciais,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = perfil.nome,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFDCFCE7))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                "Modo Cuidador",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D),
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Nº SNS: ${perfil.numeroUtente} · ${perfil.idadeFormatada}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Atenuado,
                                    )
                                    if (perfil.alergias.isNotBlank() && !perfil.alergias.contains("Sem", ignoreCase = true)) {
                                        Text(
                                            text = "⚠️ Alergias: ${perfil.alergias}",
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
            }

            // 2. DASHBOARD HUD POLIDO (SEM TEXTO AMONTOADO NEM CORTE)
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            // LINHA SUPERIOR: Título do painel + Toggle de Alarmes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFEFF6FF), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Rounded.Medication,
                                            contentDescription = null,
                                            tint = MarcaMedio,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = "Gestão de Prescrições",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "Sincronizado com relógio nativo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Atenuado,
                                        )
                                    }
                                }

                                Spacer(Modifier.width(16.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (agendaAtiva) Color(0xFFDCFCE7) else Color(0xFFF1F5F9))
                                        .clickable {
                                            viewModel?.atualizarAgendaEAlarmes(!agendaAtiva)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Alarm,
                                            contentDescription = null,
                                            tint = if (agendaAtiva) Color(0xFF15803D) else Atenuado,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (agendaAtiva) "Alarmes Ativos" else "Alarmes Pausa",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (agendaAtiva) Color(0xFF15803D) else Atenuado,
                                        )
                                    }
                                }
                            }

                            // MÉTRICAS EQUILIBRADAS EM 2 COLUNAS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Superficie2)
                                        .padding(12.dp),
                                ) {
                                    Column {
                                        Text(
                                            text = "Medicamentos Ativos",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Atenuado,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "$totalAtivos de ${medicamentos.size}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MarcaMedio,
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Superficie2)
                                        .padding(12.dp),
                                ) {
                                    Column {
                                        Text(
                                            text = "Próxima Toma",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Atenuado,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (proximaToma != null) {
                                                "${proximaToma.listaHorarios.firstOrNull() ?: proximaToma.hora} · ${proximaToma.nome.take(12)}"
                                            } else {
                                                "Sem tomas"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (proximaToma != null) Color(0xFF0F766E) else Atenuado,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }

                            // FILTROS HORÁRIOS
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf("Todos", "Ativos", "Manhã", "Tarde", "Noite").forEach { filtro ->
                                    val selecionado = filtroHorario == filtro
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selecionado) MarcaMedio else Superficie2)
                                            .clickable { filtroHorario = filtro }
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                    ) {
                                        Text(
                                            text = filtro,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selecionado) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. AVISO DE DUPLICAÇÃO DE PRINCÍPIO ATIVO
            items(duplicados) { duplicado ->
                Box(Modifier.padding(horizontal = 20.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Warning, null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Possível duplicação terapêutica",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF92400E),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Identificados medicamentos com o mesmo princípio ativo «$duplicado». Pode estar a tomar dose dobrada. Consulte o seu médico.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB45309),
                                )
                            }
                        }
                    }
                }
            }

            // 4. LISTA DE MEDICAMENTOS COM DESIGN CLÍNICO, MÚLTIPLOS HORÁRIOS E BOTÃO DE EDITAR
            if (medicamentosFiltrados.isEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Nenhum medicamento encontrado para o filtro «$filtroHorario».",
                                    color = Atenuado,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            } else {
                items(medicamentosFiltrados, key = { it.id }) { medicamento ->
                    val bitmap = remember(medicamento.fotografia) {
                        ProcessadorImagem.descodificarBitmap(contexto, medicamento.id, medicamento.fotografia)
                    }
                    val corPill = Color(medicamento.cor)

                    Box(Modifier.padding(horizontal = 20.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (medicamento.ativo) MaterialTheme.colorScheme.surface else Color(0xFFF8FAFC),
                            ),
                            border = BorderStroke(
                                1.2.dp,
                                if (medicamento.ativo) Color(0xFFCBD5E1) else Color(0xFFE2E8F0),
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (medicamento.ativo) 2.dp else 0.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // LINHA SUPERIOR: Imagem / Ícone + Nome / Substância + Switch Ativo
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // FOTOGRAFIA OU ÍCONE DA EMBALAGEM
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (bitmap != null) Color(0xFFF1F5F9) else corPill.copy(alpha = 0.12f))
                                            .border(1.5.dp, if (bitmap != null) Color(0xFFCBD5E1) else corPill.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                            .clickable {
                                                if (bitmap != null) {
                                                    medicamentoParaVerFoto = medicamento
                                                } else {
                                                    medicamentoParaFoto = medicamento
                                                }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Embalagem de ${medicamento.nome}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(3.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.55f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Search,
                                                    contentDescription = "Ver foto",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp),
                                                )
                                            }
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Medication,
                                                    contentDescription = null,
                                                    tint = corPill,
                                                    modifier = Modifier.size(32.dp),
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Icon(
                                                    Icons.Rounded.AddAPhoto,
                                                    contentDescription = "Adicionar foto",
                                                    tint = corPill.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(15.dp),
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = medicamento.nome,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (medicamento.principioAtivo.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Substância: ${medicamento.principioAtivo}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Atenuado,
                                            )
                                        }
                                        if (medicamento.laboratorio.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Laboratório: ${medicamento.laboratorio}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MarcaMedio,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // SWITCH DE ATIVO / PAUSA
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Switch(
                                            checked = medicamento.ativo && agendaAtiva,
                                            onCheckedChange = { ativo ->
                                                viewModel?.alternarAtivo(medicamento.id, ativo)
                                                // Cancelar alarmes antigos
                                                for (idx in 0..10) {
                                                    plataforma.cancelarAlarmeMedicacao(medicamento.id * 100L + idx)
                                                }
                                                plataforma.cancelarAlarmeMedicacao(medicamento.id)

                                                if (ativo) {
                                                    if (!agendaAtiva) {
                                                        viewModel.atualizarAgendaEAlarmes(true)
                                                    }
                                                    medicamento.listaHorarios.forEachIndexed { idx, hStr ->
                                                        val partes = hStr.split(":")
                                                        val h = partes.getOrNull(0)?.toIntOrNull() ?: 8
                                                        val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
                                                        plataforma.agendarAlarmeMedicacao(medicamento.id * 100L + idx, medicamento.nome, medicamento.finalidade, h, m)
                                                    }
                                                    plataforma.sincronizarAlarmesNoRelogio(medicamento)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF10B981),
                                            ),
                                        )
                                        Text(
                                            text = if (medicamento.ativo) "Ativo" else "Pausa",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (medicamento.ativo) Color(0xFF10B981) else Atenuado,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }

                                // LINHA INTERMÉDIA: Indicação clínica + Frequência + Badge Foto + Chips de Horários
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFEFF6FF))
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                        ) {
                                            Text(
                                                text = medicamento.finalidade,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF1D4ED8),
                                                fontWeight = FontWeight.Bold,
                                                softWrap = true,
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                        ) {
                                            Text(
                                                text = medicamento.frequenciaFormatada,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF475569),
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }

                                    // Horários de toma individuais com badge e ícone
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        medicamento.listaHorarios.forEach { horaStr ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (medicamento.ativo) Color(0xFFECFDF5) else Color(0xFFF1F5F9))
                                                    .border(
                                                        1.dp,
                                                        if (medicamento.ativo) Color(0xFFA7F3D0) else Color(0xFFE2E8F0),
                                                        RoundedCornerShape(8.dp),
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Rounded.Alarm,
                                                        contentDescription = null,
                                                        tint = if (medicamento.ativo) Color(0xFF059669) else Atenuado,
                                                        modifier = Modifier.size(13.dp),
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = horaStr,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (medicamento.ativo) Color(0xFF065F46) else Atenuado,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }


                                // BOTÃO EXPLICAR COM IA
                                Button(
                                    onClick = { pedirExplicacaoIa(medicamento) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MarcaMedio.copy(alpha = 0.12f),
                                        contentColor = MarcaProfundo,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MarcaMedio,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Para que serve este medicamento? (IA)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                // LINHA INFERIOR: Ouvir + Editar + Eliminar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    BotaoOuvir(
                                        aoClicar = {
                                            val textoFala = if (medicamento.listaHorarios.size > 1) {
                                                "Tome ${medicamento.nome} ${medicamento.frequenciaFormatada}, ${medicamento.listaHorarios.joinToString(" e ") { formatarHoraParaFala(it) }}. Para ${medicamento.finalidade}."
                                            } else {
                                                "Tome ${medicamento.nome} ${formatarHoraParaFala(medicamento.hora)}. Para ${medicamento.finalidade}."
                                            }
                                            plataforma.falar(textoFala)
                                        },
                                        modifier = Modifier.weight(1f),
                                    )

                                    // BOTÃO EDITAR
                                    FilledTonalButton(
                                        onClick = {
                                            medicamentoEmEdicao = medicamento
                                            mostrarDialogoFormulario = true
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFFF1F5F9),
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Editar", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }

                                    // BOTÃO ELIMINAR
                                    IconButton(
                                        onClick = { pendenteApagar = medicamento },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFEF2F2)),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Apagar ${medicamento.nome}",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. BOTÕES PRINCIPAIS: ADICIONAR MEDICAMENTO + PARTILHAR LISTA
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                medicamentoEmEdicao = null
                                mostrarDialogoFormulario = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio, contentColor = Color.White),
                        ) {
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Adicionar Medicamento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }

                        FilledIconButton(
                            onClick = {
                                val textoPartilha = buildString {
                                    appendLine("📋 Lista de Medicação · Ponte Saúde ULSBE")
                                    appendLine("Utente: ${perfil.nome} (Nº SNS: ${perfil.numeroUtente})")
                                    appendLine("Alergias: ${perfil.alergias}")
                                    appendLine("\nMedicamentos Ativos:")
                                    medicamentos.filter { it.ativo }.forEach { m ->
                                        appendLine("• ${m.nome} — ${m.frequenciaFormatada} (${m.listaHorarios.joinToString(", ")}) [${m.finalidade} · Princípio: ${m.principioAtivo}]")
                                    }
                                    val prods = uiState?.produtos ?: perfil.outrosProdutos
                                    if (prods.isNotEmpty()) {
                                        appendLine("\nSuplementos e Ervanária:")
                                        prods.forEach { appendLine("• $it") }
                                    }
                                }
                                plataforma.partilhar("Lista de Medicação · ${perfil.nome}", textoPartilha)
                            },
                            modifier = Modifier.size(50.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Share, "Partilhar lista com médico ou farmácia")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val (uri, nome) = plataforma.gerarEGuardarPdfMedicacao(perfil, medicamentos, uiState?.produtos ?: perfil.outrosProdutos)
                            if (uri != null) {
                                pdfGeradoUri = uri
                                pdfNomeFicheiro = nome
                                mostrarDialogoPdf = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MarcaMedio),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Baixar Guia em PDF", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // 6. OUTROS PRODUTOS & SUPLEMENTOS NATURAIS (LAYOUT LIMPO SEM ESPAÇOS MORTOS)
            item {
                val produtos = uiState?.produtos ?: perfil.outrosProdutos
                Box(Modifier.padding(horizontal = 20.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFECFDF5)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Outros Produtos & Suplementos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Chás, vitaminas e ervanária (informe a equipa de saúde)",
                                        color = Atenuado,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                if (produtos.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Superficie2)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = "${produtos.size} registado${if (produtos.size > 1) "s" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MarcaMedio,
                                        )
                                    }
                                }
                            }

                            // ALERTA DE INTERAÇÕES MEDICAMENTOSAS
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFFBEB))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "⚠️ Chás e ervanária podem alterar a ação de anticoagulantes e medicamentos para a tensão.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF92400E),
                                    lineHeight = 16.sp,
                                )
                            }

                            // LISTA DE PRODUTOS SE EXISTIREM
                            if (produtos.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    produtos.forEach { produto ->
                                        val eCha = produto.contains("chá", ignoreCase = true) || produto.contains("infusão", ignoreCase = true)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Superficie2)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(if (eCha) Color(0xFFD1FAE5) else Color(0xFFE0E7FF)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Medication,
                                                    null,
                                                    tint = if (eCha) Color(0xFF059669) else Color(0xFF4338CA),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(produto, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    if (eCha) "Infusão / Ervanária natural" else "Vitamina / Suplemento alimentar",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Atenuado,
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel?.apagarProduto(produto) },
                                                modifier = Modifier.size(30.dp),
                                            ) {
                                                Icon(Icons.Rounded.Close, "Remover $produto", tint = Atenuado, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // SUGESTÕES RÁPIDAS COM SCROLL HORIZONTAL
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (produtos.isEmpty()) "Adicionar rapidamente se consome:" else "Sugestões frequentes:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Atenuado,
                                    fontWeight = FontWeight.SemiBold,
                                    )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    listOf("Chá de camomila", "Vitamina D", "Magnésio", "Ómega 3", "Chá de cidreira", "Ginkgo Biloba").forEach { sugestao ->
                                        val jaExiste = produtos.any { it.equals(sugestao, ignoreCase = true) }
                                        if (!jaExiste) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Superficie2)
                                                    .clickable { viewModel?.adicionarProduto(sugestao) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                            ) {
                                                Text("+ $sugestao", style = MaterialTheme.typography.labelSmall, color = MarcaMedio, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // CAMPO DE ADIÇÃO MANUAL
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = rascunhoProduto,
                                    onValueChange = { rascunhoProduto = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Outro chá ou suplemento…") },
                                    singleLine = true,
                                )
                                Button(
                                    onClick = {
                                        if (rascunhoProduto.isNotBlank()) {
                                            viewModel?.adicionarProduto(rascunhoProduto.trim())
                                            rascunhoProduto = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                                ) {
                                    Text("Adicionar")
                                }
                            }
                        }
                    }
                }
            }

            // NOTA INFORMATIVA FINAL
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.HealthAndSafety, null, tint = MarcaClaro, modifier = Modifier.size(18.dp))
                        Text(
                            text = "A lista permanece guardada em segurança no telemóvel. Mostre-a nas consultas e no hospital.",
                            color = Atenuado,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }

        BarraScrollLateralSuave(
            estadoLista = estadoLista,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp, top = 12.dp, bottom = 16.dp),
        )
    }
}

    // DIÁLOGO UNIFICADO PARA ADICIONAR OU EDITAR MEDICAMENTO COM SUPORTE A MÚLTIPLOS HORÁRIOS
    if (mostrarDialogoFormulario) {
        DialogoFormularioMedicamento(
            medicamentoExistente = medicamentoEmEdicao,
            aoDispensar = {
                mostrarDialogoFormulario = false
                medicamentoEmEdicao = null
            },
            aoGuardar = { medSalvo ->
                if (medicamentoEmEdicao == null) {
                    // Adicionar novo
                    viewModel?.adicionarMedicamento(medSalvo)
                    if (agendaAtiva) {
                        medSalvo.listaHorarios.forEachIndexed { idx, horaStr ->
                            val partes = horaStr.split(":")
                            val h = partes.getOrNull(0)?.toIntOrNull() ?: 8
                            val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
                            plataforma.agendarAlarmeMedicacao(medSalvo.id * 100L + idx, medSalvo.nome, medSalvo.finalidade, h, m)
                        }
                        plataforma.sincronizarAlarmesNoRelogio(medSalvo)
                        mensagemAlarme = "Medicamento guardado com ${medSalvo.listaHorarios.size} alarme(s) programado(s)!"
                    }
                } else {
                    // Atualizar existente
                    viewModel?.atualizarMedicamento(medSalvo)
                    for (idx in 0..10) {
                        plataforma.cancelarAlarmeMedicacao(medSalvo.id * 100L + idx)
                    }
                    plataforma.cancelarAlarmeMedicacao(medSalvo.id)
                    if (medSalvo.ativo && agendaAtiva) {
                        medSalvo.listaHorarios.forEachIndexed { idx, horaStr ->
                            val partes = horaStr.split(":")
                            val h = partes.getOrNull(0)?.toIntOrNull() ?: 8
                            val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
                            plataforma.agendarAlarmeMedicacao(medSalvo.id * 100L + idx, medSalvo.nome, medSalvo.finalidade, h, m)
                        }
                        plataforma.sincronizarAlarmesNoRelogio(medSalvo)
                        mensagemAlarme = "Medicamento «${medSalvo.nome}» atualizado com ${medSalvo.listaHorarios.size} alarme(s) sincronizado(s)!"
                    } else {
                        mensagemAlarme = "Medicamento «${medSalvo.nome}» atualizado com sucesso!"
                    }
                }
                mostrarDialogoFormulario = false
                medicamentoEmEdicao = null
            },
        )
    }

    // DIÁLOGO CONFIRMAR ELIMINAÇÃO
    pendenteApagar?.let { medicamento ->
        AlertDialog(
            onDismissRequest = { pendenteApagar = null },
            icon = { Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Apagar medicamento?") },
            text = { Text("Tem a certeza de que pretende apagar «${medicamento.nome}» da lista de prescrições?") },
            dismissButton = { TextButton(onClick = { pendenteApagar = null }) { Text("Cancelar") } },
            confirmButton = {
                Button(
                    onClick = {
                        for (idx in 0..10) {
                            plataforma.cancelarAlarmeMedicacao(medicamento.id * 100L + idx)
                        }
                        plataforma.cancelarAlarmeMedicacao(medicamento.id)
                        viewModel?.apagarMedicamento(medicamento.id)
                        pendenteApagar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Apagar") }
            },
        )
    }

    // DIÁLOGO INFORMATIVO DE ALARME
    mensagemAlarme?.let { msg ->
        AlertDialog(
            onDismissRequest = { mensagemAlarme = null },
            icon = { Icon(Icons.Rounded.Alarm, null, tint = MarcaMedio) },
            title = { Text("Alarme & Notificação") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { mensagemAlarme = null }) {
                    Text("Entendido")
                }
            },
        )
    }

    // DIÁLOGO DE VISUALIZAÇÃO AMPLIADA DA FOTOGRAFIA DA EMBALAGEM
    medicamentoParaVerFoto?.let { med ->
        DialogoVisualizadorFotoMedicamento(
            medicamento = med,
            aoDispensar = { medicamentoParaVerFoto = null },
            aoPedirAlterarFoto = {
                medicamentoParaVerFoto = null
                medicamentoParaFoto = med
            },
            aoPedirRemoverFoto = {
                confirmarRemoverFotoMed = med
            },
        )
    }

    // DIÁLOGO DE SELEÇÃO DE FOTOGRAFIA (CÂMARA OU GALERIA/FICHEIROS)
    medicamentoParaFoto?.let { med ->
        DialogoSelecionarFotoMedicamento(
            nomeMedicamento = med.nome,
            idMedicamento = med.id,
            aoDispensar = { medicamentoParaFoto = null },
            aoFotoProcessada = { base64 ->
                viewModel?.atualizarFoto(med.id, base64)
                medicamentoParaFoto = null
                Toast.makeText(contexto, "Fotografia guardada com sucesso!", Toast.LENGTH_SHORT).show()
            },
        )
    }

    // DIÁLOGO DE CONFIRMAÇÃO PARA REMOVER FOTOGRAFIA
    confirmarRemoverFotoMed?.let { med ->
        AlertDialog(
            onDismissRequest = { confirmarRemoverFotoMed = null },
            icon = { Icon(Icons.Rounded.Warning, null, tint = Color(0xFFDC2626)) },
            title = { Text("Remover Fotografia?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Tem a certeza de que deseja remover a fotografia da caixa de ${med.nome}? O medicamento continuará registado na sua lista.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel?.atualizarFoto(med.id, null)
                        ProcessadorImagem.apagarFotoLocal(contexto, med.id)
                        confirmarRemoverFotoMed = null
                        medicamentoParaVerFoto = null
                        Toast.makeText(contexto, "Fotografia removida.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                ) {
                    Text("Sim, Remover", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRemoverFotoMed = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    // DIÁLOGO CONFIRMAÇÃO DE DOWNLOAD DO GUIA EM PDF
    if (mostrarDialogoPdf && pdfGeradoUri != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPdf = false },
            icon = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, tint = MarcaMedio) },
            title = { Text("Guia em PDF Descarregado", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("O documento foi gerado e guardado com sucesso na pasta de Transferências / Downloads do seu telemóvel:")
                    Text(pdfNomeFicheiro, fontWeight = FontWeight.Bold, color = MarcaMedio)
                    Text(
                        "Pode agora abrir o PDF no seu telemóvel para consulta ou partilhar com o seu médico assistente ou farmácia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    pdfGeradoUri?.let { plataforma.abrirPdf(it) }
                    mostrarDialogoPdf = false
                }) {
                    Text("Abrir PDF")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        pdfGeradoUri?.let { plataforma.partilharPdf(it, "Guia de Medicação · ${perfil.nome}") }
                        mostrarDialogoPdf = false
                    }) {
                        Text("Partilhar")
                    }
                    TextButton(onClick = { mostrarDialogoPdf = false }) {
                        Text("Fechar")
                    }
                }
            },
        )
    }

    // DIÁLOGO SEM INTERNET PARA IA
    if (avisoSemInternetMedicacao) {
        AlertDialog(
            onDismissRequest = { avisoSemInternetMedicacao = false },
            icon = { Icon(Icons.Rounded.WifiOff, null, tint = Color(0xFFDC2626), modifier = Modifier.size(36.dp)) },
            title = { Text("Sem Ligação à Internet", fontWeight = FontWeight.Bold) },
            text = {
                Text("O assistente inteligente necessita de ligação à internet para analisar o medicamento. Por favor, ligue o Wi-Fi ou os dados móveis no seu telemóvel.")
            },
            confirmButton = {
                Button(
                    onClick = { avisoSemInternetMedicacao = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                ) {
                    Text("Compreendi")
                }
            },
        )
    }

    // DIÁLOGO DE EXPLICAÇÃO DO MEDICAMENTO COM IA
    if (medParaExplicarIa != null) {
        val med = medParaExplicarIa!!
        AlertDialog(
            onDismissRequest = {
                medParaExplicarIa = null
                textoExplicacaoIa = null
                erroExplicacaoIa = null
            },
            icon = {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MarcaMedio,
                    modifier = Modifier.size(36.dp),
                )
            },
            title = {
                Text(
                    text = med.nome,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val carregandoIa = uiState?.aCarregarIa ?: aCarregarExplicacaoIa
                    val textoIa = uiState?.explicacaoIa ?: textoExplicacaoIa
                    val erroIa = uiState?.mensagemErro ?: erroExplicacaoIa
                    if (carregandoIa) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MarcaMedio,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "A analisar indicações clínicas com IA…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Atenuado,
                            )
                        }
                    } else if (textoIa != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Superficie2)
                                .padding(14.dp),
                        ) {
                            Text(
                                text = textoIa,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                            )
                        }

                        // Botão Ouvir (Apenas se clicar!)
                        BotaoOuvir(
                            aoClicar = { plataforma.falar(textoIa) },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            text = "Aviso: Informação gerada por Inteligência Artificial. Siga sempre as indicações da receita e confirme com o seu médico ou farmacêutico.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Atenuado,
                            lineHeight = 15.sp,
                        )
                    } else if (erroIa != null) {
                        Text(
                            text = erroIa,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        medParaExplicarIa = null
                        textoExplicacaoIa = null
                        erroExplicacaoIa = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                ) {
                    Text("Fechar")
                }
            },
        )
    }
}

/**
 * Diálogo profissional para selecionar ou fotografar a caixa da embalagem do medicamento.
 * Permite capturar com a câmara ou carregar um ficheiro de imagem transferido para o telemóvel.
 * Otimiza e guarda a imagem localmente (offline) e prepara para sincronização com o Firestore no campo "fotografia".
 */
@Composable
fun DialogoSelecionarFotoMedicamento(
    nomeMedicamento: String,
    idMedicamento: Long? = null,
    aoDispensar: () -> Unit,
    aoFotoProcessada: (String) -> Unit,
) {
    val contexto = LocalContext.current
    var uriTempCamera by remember { mutableStateOf<Uri?>(null) }
    var aProcessarImagem by remember { mutableStateOf(false) }

    val launcherCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { sucesso ->
        if (sucesso && uriTempCamera != null) {
            aProcessarImagem = true
            val base64 = ProcessadorImagem.processarUriParaBase64(
                context = contexto,
                uri = uriTempCamera!!,
                idMedicamento = idMedicamento,
            )
            aProcessarImagem = false
            if (base64 != null) {
                aoFotoProcessada(base64)
            } else {
                Toast.makeText(contexto, "Não foi possível processar a fotografia da câmara.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launcherPermissaoCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { concedida ->
        if (concedida) {
            val (uri, _) = ProcessadorImagem.criarUriFotoTemporaria(contexto)
            uriTempCamera = uri
            launcherCamera.launch(uri)
        } else {
            Toast.makeText(contexto, "Permissão da câmara necessária para fotografar a caixa.", Toast.LENGTH_LONG).show()
        }
    }

    val launcherGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            aProcessarImagem = true
            val base64 = ProcessadorImagem.processarUriParaBase64(
                context = contexto,
                uri = uri,
                idMedicamento = idMedicamento,
            )
            aProcessarImagem = false
            if (base64 != null) {
                aoFotoProcessada(base64)
            } else {
                Toast.makeText(contexto, "Não foi possível carregar a imagem do telemóvel.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun iniciarCamera() {
        val temPermissao = ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        if (temPermissao) {
            val (uri, _) = ProcessadorImagem.criarUriFotoTemporaria(contexto)
            uriTempCamera = uri
            launcherCamera.launch(uri)
        } else {
            launcherPermissaoCamera.launch(Manifest.permission.CAMERA)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!aProcessarImagem) aoDispensar() },
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MarcaMedio.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = MarcaMedio, modifier = Modifier.size(24.dp))
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Foto da Caixa / Embalagem",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (nomeMedicamento.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = nomeMedicamento,
                        style = MaterialTheme.typography.bodySmall,
                        color = Atenuado,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (aProcessarImagem) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MarcaMedio)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "A otimizar fotografia da embalagem...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Como pretende adicionar a fotografia da caixa deste medicamento?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // OPÇÃO 1: CÂMARA (Tirar Foto)
                    Card(
                        onClick = { iniciarCamera() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MarcaMedio),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tirar fotografia agora",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A),
                                )
                                Text(
                                    text = "Fotografe a frente da embalagem ou caixa",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF3B82F6),
                                )
                            }
                        }
                    }

                    // OPÇÃO 2: GALERIA / FICHEIROS
                    Card(
                        onClick = { launcherGaleria.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF059669)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Escolher do telemóvel",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                )
                                Text(
                                    text = "Foto da galeria ou ficheiro transferido",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF10B981),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = aoDispensar, enabled = !aProcessarImagem) {
                Text("Cancelar")
            }
        },
    )
}

/**
 * Diálogo de visualização detalhada da fotografia da embalagem.
 * Apresenta a fotografia em alta definição com indicação de sincronização (Cloud / Offline),
 * botão para alterar a foto e botão para remover.
 */
@Composable
fun DialogoVisualizadorFotoMedicamento(
    medicamento: Medicamento,
    aoDispensar: () -> Unit,
    aoPedirAlterarFoto: () -> Unit,
    aoPedirRemoverFoto: () -> Unit,
) {
    val contexto = LocalContext.current
    val bitmap = remember(medicamento.fotografia) {
        ProcessadorImagem.descodificarBitmap(contexto, medicamento.id, medicamento.fotografia)
    }

    AlertDialog(
        onDismissRequest = aoDispensar,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(medicamento.cor).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Medication,
                        contentDescription = null,
                        tint = Color(medicamento.cor),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicamento.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (medicamento.laboratorio.isNotBlank()) "Laboratório: ${medicamento.laboratorio}" else "Foto da Caixa / Embalagem",
                        style = MaterialTheme.typography.labelSmall,
                        color = Atenuado,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // IMAGEM EM DESTAQUE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Embalagem de ${medicamento.nome}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Image,
                                contentDescription = null,
                                tint = Atenuado,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                "Imagem não disponível",
                                style = MaterialTheme.typography.bodySmall,
                                color = Atenuado,
                            )
                        }
                    }
                }

                // BOTÕES DE AÇÃO: ALTERAR E REMOVER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = aoPedirAlterarFoto,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MarcaMedio),
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Alterar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = aoPedirRemoverFoto,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remover", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = aoDispensar,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
            ) {
                Text("Fechar")
            }
        },
    )
}

/**
 * Diálogo de criação e edição de medicamentos com suporte completo a múltiplos horários,
 * presets rápidos e fotografia da embalagem.
 */
@Composable
fun DialogoFormularioMedicamento(
    medicamentoExistente: Medicamento?,
    aoDispensar: () -> Unit,
    aoGuardar: (Medicamento) -> Unit,
) {
    val contexto = LocalContext.current
    var nome by remember { mutableStateOf(medicamentoExistente?.nome ?: "") }
    var finalidade by remember { mutableStateOf(medicamentoExistente?.finalidade ?: "") }
    var principioAtivo by remember { mutableStateOf(medicamentoExistente?.principioAtivo ?: "") }
    var laboratorio by remember { mutableStateOf(medicamentoExistente?.laboratorio ?: "") }
    var listaHoras by remember {
        mutableStateOf(medicamentoExistente?.listaHorarios ?: listOf("08:00"))
    }
    var horaCustom by remember { mutableStateOf("") }
    var erroHora by remember { mutableStateOf<String?>(null) }
    var fotografia by remember { mutableStateOf(medicamentoExistente?.fotografia) }
    var mostrarSeletorFotoForm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = aoDispensar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (medicamentoExistente == null) Icons.Rounded.Add else Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = MarcaMedio,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (medicamentoExistente == null) "Adicionar Medicamento" else "Editar Medicamento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // FOTOGRAFIA E SCANNER
                item {
                    val bmp = remember(fotografia) {
                        ProcessadorImagem.descodificarBitmap(contexto, medicamentoExistente?.id, fotografia)
                    }

                    if (bmp != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Embalagem",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Foto da Caixa ✓",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MarcaMedio,
                                    )
                                }
                                TextButton(onClick = { mostrarSeletorFotoForm = true }) {
                                    Text("Alterar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { fotografia = null },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Remover",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { mostrarSeletorFotoForm = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEFF6FF),
                                    contentColor = MarcaMedio,
                                ),
                            ) {
                                Icon(Icons.Rounded.CameraAlt, "Fotografar", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Foto da Caixa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    nome = "Metformina 850 mg"
                                    finalidade = "Diabetes"
                                    principioAtivo = "metformina"
                                    laboratorio = "Generis"
                                    listaHoras = listOf("08:00", "20:00")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Superficie2, contentColor = MaterialTheme.colorScheme.onSurface),
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ler Código", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // NOME COMERCIAL
                item {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nome Comercial e Dosagem") },
                        placeholder = { Text("Ex: Ben-u-ron 1000 mg") },
                        singleLine = true,
                    )
                }

                // FINALIDADE
                item {
                    OutlinedTextField(
                        value = finalidade,
                        onValueChange = { finalidade = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Para que serve (Indicação)") },
                        placeholder = { Text("Ex: Dores / Febre ou Tensão arterial") },
                        singleLine = true,
                    )
                }

                // PRINCÍPIO ATIVO
                item {
                    OutlinedTextField(
                        value = principioAtivo,
                        onValueChange = { principioAtivo = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Princípio Ativo (Substância)") },
                        placeholder = { Text("Ex: paracetamol") },
                        singleLine = true,
                    )
                }

                // LABORATÓRIO FARMACÊUTICO
                item {
                    OutlinedTextField(
                        value = laboratorio,
                        onValueChange = { laboratorio = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Laboratório Farmacêutico") },
                        placeholder = { Text("Ex: Bial, Generis, Labesfal, Cinfa...") },
                        singleLine = true,
                    )
                }

                // GESTÃO DE HORÁRIOS DA TOMA (MÚLTIPLOS HORÁRIOS)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Superficie2)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Horários da Toma",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "${listaHoras.size} toma${if (listaHoras.size > 1) "s/dia" else "/dia"}",
                                color = MarcaMedio,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        // CHIPS DOS HORÁRIOS SELECIONADOS COM BOTÃO DE REMOVER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listaHoras.forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFDCFCE7))
                                        .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "⏰ $h",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF166534),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Remover $h",
                                            tint = Color(0xFF166534),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    if (listaHoras.size > 1) {
                                                        listaHoras = listaHoras.filterNot { it == h }
                                                    }
                                                },
                                        )
                                    }
                                }
                            }
                        }

                        // PRESETS RÁPIDOS PARA ADICIONAR HORAS
                        Text(
                            text = "Toque para adicionar mais horários:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Atenuado,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf("08:00 (P.Almoço)", "13:00 (Almoço)", "20:00 (Jantar)", "22:00 (Deitar)").forEach { preset ->
                                val horaValor = preset.substring(0, 5)
                                val jaExiste = horaValor in listaHoras
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (jaExiste) Color(0xFFE2E8F0) else Color.White)
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                        .clickable(enabled = !jaExiste) {
                                            listaHoras = (listaHoras + horaValor).distinct().sorted()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = if (jaExiste) "✓ $horaValor" else "+ $preset",
                                        fontSize = 11.sp,
                                        fontWeight = if (jaExiste) FontWeight.Normal else FontWeight.Bold,
                                        color = if (jaExiste) Atenuado else MarcaMedio,
                                    )
                                }
                            }
                        }

                        // ADICIONAR OUTRO HORÁRIO MANUALMENTE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = horaCustom,
                                onValueChange = {
                                    horaCustom = it
                                    erroHora = null
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Outro (ex: 16:30)") },
                                singleLine = true,
                            )
                            Button(
                                onClick = {
                                    val regexHora = Regex("""^([01]?[0-9]|2[0-3]):[0-5][0-9]$""")
                                    val limpa = horaCustom.trim()
                                    if (regexHora.matches(limpa)) {
                                        val partes = limpa.split(":")
                                        val formatada = String.format("%02d:%02d", partes[0].toInt(), partes[1].toInt())
                                        listaHoras = (listaHoras + formatada).distinct().sorted()
                                        horaCustom = ""
                                        erroHora = null
                                    } else {
                                        erroHora = "Formato inválido (HH:MM)"
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                            ) {
                                Text("+")
                            }
                        }
                        erroHora?.let {
                            Text(it, color = Color(0xFFDC2626), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nome.isNotBlank()) {
                        val horaFinal = if (listaHoras.isNotEmpty()) listaHoras.sorted().joinToString(", ") else "08:00"
                        val medSalvo = medicamentoExistente?.copy(
                            nome = nome.trim(),
                            finalidade = finalidade.trim().ifBlank { "Prescrição habitual" },
                            principioAtivo = principioAtivo.trim().lowercase(),
                            hora = horaFinal,
                            fotografia = fotografia,
                            laboratorio = laboratorio.trim(),
                        ) ?: Medicamento(
                            id = System.currentTimeMillis(),
                            nome = nome.trim(),
                            finalidade = finalidade.trim().ifBlank { "Prescrição habitual" },
                            principioAtivo = principioAtivo.trim().lowercase(),
                            hora = horaFinal,
                            cor = 0xFF1D5BD8,
                            ativo = true,
                            fotografia = fotografia,
                            laboratorio = laboratorio.trim(),
                        )
                        aoGuardar(medSalvo)
                    }
                },
                enabled = nome.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
            ) {
                Text(if (medicamentoExistente == null) "Adicionar" else "Guardar Alterações", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = aoDispensar) {
                Text("Cancelar")
            }
        },
    )

    if (mostrarSeletorFotoForm) {
        DialogoSelecionarFotoMedicamento(
            nomeMedicamento = nome.ifBlank { "Medicamento" },
            idMedicamento = medicamentoExistente?.id,
            aoDispensar = { mostrarSeletorFotoForm = false },
            aoFotoProcessada = { base64 ->
                fotografia = base64
                mostrarSeletorFotoForm = false
            },
        )
    }
}

@Preview(name = "Ecrã Medicação", showBackground = true)
@Composable
fun PreVisualizacaoEcraMedicacao() {
    val perfil = Perfil(
        id = "1",
        nome = "Utente Demonstração",
        nomeCurto = "Utente",
        relacao = "Utente",
        iniciais = "UD",
        numeroUtente = "189 423 761",
        tipoSanguineo = "A+",
        alergias = "Sem alergias",
        medicamentos = listOf(
            Medicamento(
                id = 1L,
                nome = "Ben-u-ron 1000mg",
                finalidade = "Alívio da dor e febre",
                principioAtivo = "Paracetamol",
                hora = "08:00, 20:00",
                cor = 0xFF2563EB,
                ativo = true,
                laboratorio = "Bene Farmacêutica",
            ),
            Medicamento(
                id = 2L,
                nome = "Sinvastatina 20mg",
                finalidade = "Controlo do colesterol",
                principioAtivo = "Sinvastatina",
                hora = "21:00",
                cor = 0xFF10B981,
                ativo = true,
                laboratorio = "Generis",
            ),
        ),
        outrosProdutos = listOf("Magnésio Supremo", "Chá de Camomila"),
    )
    TemaPonteSaude(textoGrande = false) {
        EcraMedicacao(
            perfil = perfil,
            plataforma = ServicosDispositivo(null),
        )
    }
}

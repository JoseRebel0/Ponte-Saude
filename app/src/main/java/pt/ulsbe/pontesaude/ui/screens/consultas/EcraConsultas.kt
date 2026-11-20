package pt.ulsbe.pontesaude.ui.screens.consultas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.repository.AppContainer
import pt.ulsbe.pontesaude.data.repository.ConfiguracaoRepository
import pt.ulsbe.pontesaude.data.repository.ConsultaRepository
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.model.CategoriaChecklist
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.ItemChecklist
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.util.ServicosDispositivo
import pt.ulsbe.pontesaude.ui.components.BotaoOuvir
import pt.ulsbe.pontesaude.ui.components.CabecalhoEcra
import pt.ulsbe.pontesaude.ui.components.CartaoApp
import pt.ulsbe.pontesaude.ui.components.ChecklistInterativa
import pt.ulsbe.pontesaude.ui.theme.Atenuado
import pt.ulsbe.pontesaude.ui.theme.Marca
import pt.ulsbe.pontesaude.ui.theme.MarcaClaro
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio
import pt.ulsbe.pontesaude.ui.theme.Superficie2
import pt.ulsbe.pontesaude.ui.theme.TemaPonteSaude

/**
 * Ecrã de Consultas e Exames com checklist interativa, preparação para o utente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcraConsultas(
    perfil: Perfil,
    viewModel: ConsultasViewModel? = null,
    consultaRepository: ConsultaRepository? = null,
    perfilRepository: PerfilRepository? = null,
    configuracaoRepository: ConfiguracaoRepository? = null,
    aoSincronizar: (suspend () -> Unit)? = null,
    plataforma: ServicosDispositivo,
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val perfilAtivo = if (uiState != null && uiState.perfil?.id == perfil.id) uiState.perfil ?: perfil else perfil
    val consultas = if (uiState != null && uiState.perfil?.id == perfil.id) {
        uiState.consultas
    } else {
        consultaRepository?.consultas(perfil) ?: perfil.consultas
    }
    LaunchedEffect(perfil.id) {
        viewModel?.recarregar()
    }
    val modoCuidador = uiState?.modoCuidador ?: (perfilRepository?.modoCuidador == true)
    val agendaAtiva = uiState?.agendaEAlarmesAtivos ?: configuracaoRepository?.agendaEAlarmesAtivos ?: true

    val estadoLista = rememberLazyListState()
    var aAtualizarLocal by remember { mutableStateOf(false) }
    val aAtualizar = uiState?.emCarregamento ?: aAtualizarLocal
    val escopo = rememberCoroutineScope()

    var idSelecionado by remember(perfilAtivo.id, consultas.size) {
        mutableStateOf(uiState?.consultaSelecionada?.id ?: consultas.firstOrNull()?.id ?: 0)
    }
    var mostrarDialogoNovaConsulta by remember { mutableStateOf(false) }
    val verificacoes = remember(perfilAtivo.id) { mutableStateMapOf<String, Boolean>() }
    var mensagem by remember(idSelecionado) { mutableStateOf("") }
    val selecionada = consultas.firstOrNull { it.id == idSelecionado } ?: consultas.firstOrNull()

    // Sincronização automática com o calendário nativo do telemóvel por defeito
    LaunchedEffect(consultas, agendaAtiva) {
        if (viewModel != null) {
            viewModel.sincronizarComCalendario(plataforma)
        } else {
            if (agendaAtiva) {
                plataforma.sincronizarConsultasNoCalendario(consultas)
            } else {
                plataforma.removerConsultasDoCalendario()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        val tituloEcra = if (modoCuidador) "Consultas · ${perfilAtivo.nome}" else "Consultas"
        val subtituloEcra = if (modoCuidador) "Conta de ${perfilAtivo.nome} (Modo Cuidador)" else "Agenda e exames"
        CabecalhoEcra(tituloEcra, subtituloEcra, perfilAtivo.iniciais)
        PullToRefreshBox(
            isRefreshing = aAtualizar,
            onRefresh = {
                if (viewModel != null) {
                    viewModel.dispararAtualizacao(aoSincronizar)
                } else {
                    aAtualizarLocal = true
                    escopo.launch {
                        aoSincronizar?.invoke()
                        delay(500)
                        aAtualizarLocal = false
                    }
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                state = estadoLista,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // CARTÃO DE MODO CUIDADOR
                if (perfilRepository?.modoCuidador == true) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.SupervisorAccount,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Consultas e Exames de ${perfil.nome}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Nº SNS: ${perfil.numeroUtente} · Dados sincronizados com o cuidador",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Atenuado,
                                    )
                                }
                            }
                        }
                    }
                }

                // BOTÃO PARA AGENDAR NOVA CONSULTA / EXAME
                item {
                    Button(
                        onClick = { mostrarDialogoNovaConsulta = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Adicionar Nova Consulta / Exame", fontWeight = FontWeight.Bold)
                    }
                }

                if (consultas.isEmpty()) {
                    item {
                        CartaoApp {
                            Text("Não tem consultas ou exames agendados.", color = Atenuado)
                        }
                    }
                } else {
                    items(consultas, key = { it.id }) { consulta ->
                        val ativo = consulta.id == idSelecionado
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (ativo) Marca else MaterialTheme.colorScheme.surface)
                                .clickable { idSelecionado = consulta.id }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                Modifier.size(64.dp).background(Color.White, RoundedCornerShape(16.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(consulta.dia, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(consulta.mes.uppercase(), color = MarcaClaro, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.size(14.dp))
                            Column {
                                Text(consulta.tipo.uppercase(), style = MaterialTheme.typography.labelSmall, color = Atenuado)
                                Text(consulta.titulo, fontWeight = FontWeight.SemiBold)
                                Text(consulta.localizacao, style = MaterialTheme.typography.bodySmall, color = Atenuado)
                            }
                        }
                    }
                }

                if (selecionada != null) {
                    val consultaAtiva = selecionada
                    item {
                        val itensChecklist = remember(consultaAtiva.id) {
                            gerarChecklistParaConsulta(consultaAtiva)
                        }
                        ChecklistInterativa(
                            titulo = "Checklist · ${consultaAtiva.titulo}",
                            subtitulo = "Preparação específica para ${consultaAtiva.tipo}",
                            itensPredefinidos = itensChecklist,
                            chavePersistencia = "consulta_${consultaAtiva.id}",
                            consultaRepository = consultaRepository,
                            plataforma = plataforma,
                            permiteAdicionar = false,
                        )
                    }

                    if (agendaAtiva) {
                        item {
                            CartaoApp {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFECFDF5), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Rounded.Check, null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Gravado na agenda do telemóvel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF065F46),
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            plataforma.adicionarConsultaAoCalendario(
                                                consultaAtiva.titulo,
                                                "Consulta no Serviço Nacional de Saúde (ULSBE). Recomendações: Levar Cartão de Cidadão, medicação e exames.",
                                                consultaAtiva.localizacao,
                                                consultaAtiva.dia,
                                                consultaAtiva.mes,
                                            )
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF065F46)),
                                    ) {
                                        Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Ver na Agenda", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        CartaoApp {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column {
                                    Text(
                                        text = "Não vai conseguir comparecer?",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Avise o hospital para libertar a vaga para outro utente ou justificar a sua ausência:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Atenuado,
                                    )
                                }

                                // 1. DESMARCAR COM ANTECEDÊNCIA (Informativo / Visual)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFEF2F2))
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(Color(0xFFFEE2E2), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.EventBusy, null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Desmarcar Consulta com Antecedência",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF991B1B),
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Liberta a vaga para outro utente; a ULSBE propõe nova marcação",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFB91C1C),
                                        )
                                    }
                                }

                                // 2. JUSTIFICAR FALTA (Informativo / Visual)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(Color(0xFFDBEAFE), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Description, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Justificar Falta à Consulta",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E40AF),
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Prazo legal de 5 dias úteis com atestado ou declaração médica",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF1D4ED8),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoNovaConsulta) {
        var novoTitulo by remember { mutableStateOf("") }
        var novoTipo by remember { mutableStateOf("Consulta de Especialidade") }
        var novoLocal by remember { mutableStateOf("Hospital de Santa Maria Maior - Barcelos") }
        var novoDia by remember { mutableStateOf("15") }
        var novoMes by remember { mutableStateOf("Nov") }
        var precisaJejum by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { mostrarDialogoNovaConsulta = false },
            title = {
                Text(
                    text = if (perfilRepository?.modoCuidador == true) "Agendar para ${perfil.nomeCurto}" else "Agendar Consulta ou Exame",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = novoTitulo,
                        onValueChange = { novoTitulo = it },
                        label = { Text("Título / Especialidade") },
                        placeholder = { Text("Ex: Cardiologia, Análises Clínicas") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = novoTipo,
                        onValueChange = { novoTipo = it },
                        label = { Text("Tipo de Ato Clínico") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = novoLocal,
                        onValueChange = { novoLocal = it },
                        label = { Text("Localização") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = novoDia,
                            onValueChange = { novoDia = it },
                            label = { Text("Dia") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = novoMes,
                            onValueChange = { novoMes = it },
                            label = { Text("Mês") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Requer Jejum prévio?", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = precisaJejum,
                            onCheckedChange = { precisaJejum = it },
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val novoId = (consultas.maxOfOrNull { it.id } ?: 0) + 1
                        val nova = Consulta(
                            id = novoId,
                            dia = novoDia.trim().ifBlank { "15" },
                            mes = novoMes.trim().ifBlank { "Nov" },
                            titulo = novoTitulo.trim().ifBlank { "Consulta Médica" },
                            localizacao = novoLocal.trim().ifBlank { "Hospital de Santa Maria Maior - Barcelos" },
                            tipo = novoTipo.trim().ifBlank { "Consulta Externa" },
                            jejum = precisaJejum,
                        )
                        if (viewModel != null) {
                            viewModel.agendarConsulta(nova)
                        } else {
                            consultaRepository?.adicionarConsulta(perfilAtivo, nova)
                        }
                        idSelecionado = novoId
                        mostrarDialogoNovaConsulta = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoNovaConsulta = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

/**
 * Gera automaticamente a checklist clínica de preparação adaptada ao tipo específico de exame ou consulta.
 */
private fun gerarChecklistParaConsulta(consulta: Consulta): List<ItemChecklist> {
    val tituloLower = consulta.titulo.lowercase()
    val idBase = "c_${consulta.id}"

    return when {
        tituloLower.contains("análise") || tituloLower.contains("sangue") || tituloLower.contains("glicemia") -> {
            listOf(
                ItemChecklist("${idBase}_jejum", "Jejum rigoroso de 8 a 12 horas (apenas beber água)", CategoriaChecklist.JEJUM, obrigatorio = true),
                ItemChecklist("${idBase}_cc", "Levar Cartão de Cidadão e requisição/guia do SNS", CategoriaChecklist.DOCUMENTOS, obrigatorio = true),
                ItemChecklist("${idBase}_meds", "Confirmar se deve tomar a medicação matinal antes ou após a colheita", CategoriaChecklist.MEDICACAO, obrigatorio = true),
                ItemChecklist("${idBase}_repouso", "Evitar esforços físicos intensos e consumo de álcool nas 24h anteriores", CategoriaChecklist.GERAL),
                ItemChecklist("${idBase}_antecedencia", "Chegar 10 a 15 minutos antes para retirar a senha de atendimento", CategoriaChecklist.GERAL),
            )
        }
        tituloLower.contains("cardio") -> {
            listOf(
                ItemChecklist("${idBase}_tensao", "Registo das tensões arteriais da última semana (caderno de valores)", CategoriaChecklist.DOCUMENTOS),
                ItemChecklist("${idBase}_meds", "Lista completa e caixas dos medicamentos cardíacos em toma", CategoriaChecklist.MEDICACAO, obrigatorio = true),
                ItemChecklist("${idBase}_exames_previos", "Levar exames anteriores (ECG, Holter ou Ecocardiograma)", CategoriaChecklist.DOCUMENTOS),
                ItemChecklist("${idBase}_cc", "Levar Cartão de Cidadão e número de utente SNS", CategoriaChecklist.DOCUMENTOS, obrigatorio = true),
                ItemChecklist("${idBase}_sintomas", "Apontar episódios de palpitações, falta de ar ou cansaço aos esforços", CategoriaChecklist.GERAL),
            )
        }
        tituloLower.contains("ecocardio") -> {
            listOf(
                ItemChecklist("${idBase}_creme", "Não aplicar cremes, pomadas ou loções no peito no dia do exame", CategoriaChecklist.SEGURANCA, obrigatorio = true),
                ItemChecklist("${idBase}_vestuario", "Vestir roupa prática de duas peças (fácil de despir da cintura para cima)", CategoriaChecklist.GERAL),
                ItemChecklist("${idBase}_exames_ant", "Levar relatórios de ecocardiogramas prévios para comparação clínica", CategoriaChecklist.DOCUMENTOS),
                ItemChecklist("${idBase}_cc", "Levar Cartão de Cidadão", CategoriaChecklist.DOCUMENTOS, obrigatorio = true),
                ItemChecklist("${idBase}_meds", "Tomar a medicação habitual normalmente à hora habitual", CategoriaChecklist.MEDICACAO),
            )
        }
        tituloLower.contains("retino") || tituloLower.contains("oftalmo") -> {
            listOf(
                ItemChecklist("${idBase}_oculos_sol", "Levar óculos escuros (pode haver dilatação da pupila que causa fotofobia)", CategoriaChecklist.SEGURANCA, obrigatorio = true),
                ItemChecklist("${idBase}_conduzir", "Não conduzir logo após o exame; vir acompanhado se possível", CategoriaChecklist.SEGURANCA, obrigatorio = true),
                ItemChecklist("${idBase}_oculos_habituais", "Levar os seus óculos graduados habituais", CategoriaChecklist.DOCUMENTOS),
                ItemChecklist("${idBase}_cc", "Levar Cartão de Cidadão e número SNS", CategoriaChecklist.DOCUMENTOS, obrigatorio = true),
            )
        }
        tituloLower.contains("ortop") -> {
            listOf(
                ItemChecklist("${idBase}_raiox", "Levar radiografias (raio-X), ressonâncias ou relatórios prévios", CategoriaChecklist.DOCUMENTOS, obrigatorio = true),
                ItemChecklist("${idBase}_roupa", "Vestir roupa folgada que permita examinar a articulação", CategoriaChecklist.GERAL),
                ItemChecklist("${idBase}_meds", "Lista de analgésicos ou anti-inflamatórios em curso", CategoriaChecklist.MEDICACAO),
                ItemChecklist("${idBase}_cc", "Levar Cartão de Cidadão", CategoriaChecklist.DOCUMENTOS, obrigatorio = true),
                ItemChecklist("${idBase}_dor", "Anotar as alturas do dia ou posições em que a dor é mais forte", CategoriaChecklist.GERAL),
            )
        }
        tituloLower.contains("endocrino") || tituloLower.contains("diabet") -> {
            listOf(
                ItemChecklist("${idBase}_glicemias", "Caderno ou aparelho com o registo das glicemias capilares", CategoriaChecklist.DOCUMENTOS, obrigatorio = true),
                ItemChecklist("${idBase}_insulina", "Lista e doses atuais de insulina e antidiabéticos orais", CategoriaChecklist.MEDICACAO, obrigatorio = true),
                ItemChecklist("${idBase}_hba1c", "Levar as últimas análises laboratoriais (HbA1c / perfil lipídico)", CategoriaChecklist.DOCUMENTOS),
                ItemChecklist("${idBase}_cc", "Levar Cartão de Cidadão", CategoriaChecklist.DOCUMENTOS, obrigatorio = true),
                ItemChecklist("${idBase}_duvidas", "Apontar dúvidas sobre a dieta e episódios de quebra de açúcar", CategoriaChecklist.GERAL),
            )
        }
        else -> {
            val lista = mutableListOf<ItemChecklist>()
            if (consulta.jejum) {
                lista.add(ItemChecklist("${idBase}_jejum", "Jejum rigoroso de 8 horas antes do procedimento", CategoriaChecklist.JEJUM, obrigatorio = true))
            }
            lista.add(ItemChecklist("${idBase}_cc", "Levar Cartão de Cidadão e número de utente SNS", CategoriaChecklist.DOCUMENTOS, obrigatorio = true))
            lista.add(ItemChecklist("${idBase}_meds", "Levar lista ou caixas dos medicamentos que toma habitualmente", CategoriaChecklist.MEDICACAO, obrigatorio = true))
            lista.add(ItemChecklist("${idBase}_exames", "Levar exames e relatórios médicos anteriores relacionados", CategoriaChecklist.DOCUMENTOS))
            lista.add(ItemChecklist("${idBase}_duvidas", "Apontar principais sintomas e dúvidas para o profissional", CategoriaChecklist.GERAL))
            lista
        }
    }
}

// ==========================================
// PREVIEWS COM COMPOSABLE NO ANDROID STUDIO
// ==========================================

@Preview(name = "Ecrã Consultas", showBackground = true)
@Composable
fun PreVisualizacaoEcraConsultas() {
    val perfil = Perfil(
        id = "utente_1",
        nome = "Maria Inácia Silva",
        nomeCurto = "Maria",
        iniciais = "MS",
        consultas = listOf(
            Consulta(
                id = 1,
                dia = "14",
                mes = "Nov",
                titulo = "Consulta de Cardiologia",
                localizacao = "Hospital de Santa Maria Maior - Barcelos",
                tipo = "Consulta Externa",
                jejum = false,
            ),
            Consulta(
                id = 2,
                dia = "28",
                mes = "Nov",
                titulo = "Análises Clínicas de Rotina",
                localizacao = "Centro de Saúde de Barcelos",
                tipo = "Exames",
                jejum = true,
            ),
        ),
    )
    TemaPonteSaude(textoGrande = false) {
        EcraConsultas(
            perfil = perfil,
            plataforma = ServicosDispositivo(null),
        )
    }
}

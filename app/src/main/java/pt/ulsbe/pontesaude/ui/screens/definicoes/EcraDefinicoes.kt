package pt.ulsbe.pontesaude.ui.screens.definicoes

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ulsbe.pontesaude.data.repository.AppContainer
import pt.ulsbe.pontesaude.data.repository.ConfiguracaoRepository
import pt.ulsbe.pontesaude.data.repository.MedicacaoRepository
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.util.ServicosDispositivo
import pt.ulsbe.pontesaude.ui.components.CabecalhoEcra
import pt.ulsbe.pontesaude.ui.components.CartaoApp
import pt.ulsbe.pontesaude.ui.components.LinhaDefinicao
import pt.ulsbe.pontesaude.ui.components.SecaoModoCuidador
import pt.ulsbe.pontesaude.ui.theme.Atenuado
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio
import pt.ulsbe.pontesaude.ui.theme.MarcaProfundo
import pt.ulsbe.pontesaude.ui.theme.Superficie2
import pt.ulsbe.pontesaude.ui.theme.TemaPonteSaude

/**
 * Ecrã de Definições e Acessibilidade (Texto Grande, Modo Cuidador, Redução de Movimento, Notificações e Agenda/Alarmes).
 */
@Composable
fun EcraDefinicoes(
    perfil: Perfil,
    configuracaoRepository: ConfiguracaoRepository? = null,
    perfilRepository: PerfilRepository? = null,
    medicacaoRepository: MedicacaoRepository? = null,
    plataforma: ServicosDispositivo? = null,
) {
    var mensagemTeste by remember { mutableStateOf<String?>(null) }

    val lancadorPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) {
            val enviou = plataforma?.enviarNotificacaoSeguranca(
                "🔔 Teste de Notificação · Ponte Saúde",
                "As notificações estão ativas e a funcionar perfeitamente no seu telemóvel!",
            ) ?: false
            mensagemTeste = if (enviou) {
                "Notificação de teste enviada com sucesso! Verifique a barra de notificações do telemóvel."
            } else {
                "Notificação autorizada! Experimente premir novamente para enviar o teste."
            }
        } else {
            mensagemTeste = "A permissão de notificações foi recusada no telemóvel. Por favor, ative as notificações da aplicação nas Definições do Android."
        }
    }

    val estadoLista = rememberLazyListState()

    Column(Modifier.fillMaxSize()) {
        CabecalhoEcra("Definições", "Acessibilidade e preferências", perfil.iniciais)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            state = estadoLista,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                CartaoApp {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        LinhaDefinicao(
                            Icons.Rounded.TextFields,
                            "Texto maior",
                            "Aumenta o texto em toda a aplicação",
                            configuracaoRepository?.textoGrande ?: false,
                            aoMudarSelecao = { configuracaoRepository?.atualizarTextoGrande(it) },
                        )
                        HorizontalDivider(color = Superficie2)
                        if (perfilRepository != null) {
                            SecaoModoCuidador(repositorio = perfilRepository, plataforma = plataforma)
                            HorizontalDivider(color = Superficie2)
                        }
                        LinhaDefinicao(
                            Icons.Rounded.AccessibilityNew,
                            "Reduzir animações",
                            "Diminui movimentos e transições na aplicação",
                            configuracaoRepository?.reduzirMovimento ?: false,
                            aoMudarSelecao = { configuracaoRepository?.atualizarReduzirMovimento(it) },
                        )
                        HorizontalDivider(color = Superficie2)
                        LinhaDefinicao(
                            Icons.Rounded.NotificationsActive,
                            "Notificações da aplicação",
                            "Avisos automáticos de medicação, consultas e clube de segurança",
                            configuracaoRepository?.notificacoesAtivas ?: true,
                            aoMudarSelecao = { ativas: Boolean ->
                                configuracaoRepository?.atualizarNotificacoesAtivas(ativas)
                                if (!ativas) {
                                    plataforma?.cancelarTodasNotificacoes()
                                }
                            },
                        )
                        if (configuracaoRepository?.notificacoesAtivas != false && plataforma != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Superficie2)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.NotificationsActive,
                                        contentDescription = null,
                                        tint = MarcaMedio,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Testes Individuais de Notificação",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Text(
                                    "Experimente o aviso de cada funcionalidade no seu telemóvel:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Atenuado,
                                )

                                fun executarTesteNotificacao(acao: () -> Unit) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !plataforma.temPermissaoNotificacoes()) {
                                        lancadorPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        acao()
                                    }
                                }

                                // 1. Testar Medicação
                                BotaoItemTeste(
                                    icone = Icons.Rounded.Medication,
                                    corIcone = Color(0xFF1D5BD8),
                                    titulo = "Testar Medicação",
                                    descricao = "Aviso com hora e dosagem do comprimido",
                                    aoClicar = {
                                        executarTesteNotificacao {
                                            val enviou = plataforma.enviarNotificacaoMedicacao(
                                                id = 9001,
                                                nome = "Metformina 850 mg",
                                                dose = "1 comprimido ao almoço",
                                                hora = "12:00",
                                            )
                                            mensagemTeste = if (enviou) {
                                                "💊 Notificação de teste da Medicação enviada! Verifique a barra de notificações do telemóvel."
                                            } else {
                                                "Não foi possível enviar. Verifique as notificações nas Definições do telemóvel."
                                            }
                                        }
                                    },
                                )

                                // 2. Testar Módulos / Clube de Segurança
                                BotaoItemTeste(
                                    icone = Icons.Rounded.Star,
                                    corIcone = Color(0xFFD97706),
                                    titulo = "Testar Módulos & Pontos",
                                    descricao = "Lembrete do Clube de Segurança para ganhar prémios",
                                    aoClicar = {
                                        executarTesteNotificacao {
                                            val enviou = plataforma.enviarNotificacaoSeguranca(
                                                "🎁 Clube de Segurança ULSBE",
                                                "Tem módulos de segurança por concluir! Ganhe pontos para resgatar caixas de comprimidos e outros prémios.",
                                            )
                                            mensagemTeste = if (enviou) {
                                                "⭐ Notificação de teste dos Módulos enviada! Verifique a barra de notificações do telemóvel."
                                            } else {
                                                "Não foi possível enviar. Verifique as notificações nas Definições do telemóvel."
                                            }
                                        }
                                    },
                                )

                                // 3. Testar Não se esquecer de fazer a Checklist (Consultas/Exames)
                                BotaoItemTeste(
                                    icone = Icons.Rounded.CheckCircle,
                                    corIcone = Color(0xFF059669),
                                    titulo = "Testar Lembrete de Checklist",
                                    descricao = "Aviso para não esquecer jejum, exames e documentos",
                                    aoClicar = {
                                        executarTesteNotificacao {
                                            val enviou = plataforma.enviarNotificacaoChecklist(
                                                "Consulta de Cardiologia",
                                                "Jejum de 8 horas, Cartão de Cidadão e lista de medicação",
                                            )
                                            mensagemTeste = if (enviou) {
                                                "📋 Notificação de teste da Checklist enviada! Verifique a barra de notificações do telemóvel."
                                            } else {
                                                "Não foi possível enviar. Verifique as notificações nas Definições do telemóvel."
                                            }
                                        }
                                    },
                                )
                            }
                        }
                        HorizontalDivider(color = Superficie2)
                        LinhaDefinicao(
                            Icons.Rounded.CalendarMonth,
                            "Agenda e Alarmes do Telemóvel",
                            "Sincroniza automaticamente consultas na agenda e medicação no despertador",
                            configuracaoRepository?.agendaEAlarmesAtivos ?: true,
                            aoMudarSelecao = { ativos: Boolean ->
                                configuracaoRepository?.atualizarAgendaEAlarmesAtivos(ativos)
                                if (!ativos) {
                                    val meds = medicacaoRepository?.medicamentos(perfil).orEmpty()
                                    plataforma?.cancelarTodosOsAlarmes(meds)
                                    plataforma?.removerConsultasDoCalendario()
                                } else {
                                    val meds = medicacaoRepository?.medicamentos(perfil).orEmpty()
                                    val medsAtivos = meds.filter { it.ativo }
                                    val paraAtivar = if (medsAtivos.isEmpty()) meds else medsAtivos
                                    paraAtivar.forEachIndexed { idx, med ->
                                        if (medsAtivos.isEmpty()) {
                                            medicacaoRepository?.definirMedicamentoAtivo(perfil, med.id, true)
                                        }
                                        val partes = med.hora.split(":")
                                        val h = partes.getOrNull(0)?.toIntOrNull() ?: 8
                                        val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
                                        plataforma?.agendarAlarmeMedicacao(med.id, med.nome, med.finalidade, h, m)
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            plataforma?.definirAlarmeRelogio(h, m, "Tomar ${med.nome}")
                                        }, (idx * 400).toLong())
                                    }
                                    plataforma?.sincronizarConsultasNoCalendario(perfil.consultas)
                                }
                            },
                        )
                        if (configuracaoRepository?.agendaEAlarmesAtivos == false) {
                            Text(
                                "Ao desativar, não serão associados alarmes nem eventos no calendário do telemóvel (consultas, medicação e saúde).",
                                color = Atenuado,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        if (plataforma != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(
                                    onClick = { plataforma.abrirRelogioAlarmes() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MarcaMedio),
                                ) {
                                    Icon(Icons.Rounded.Alarm, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Abrir Despertador do telemóvel", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
            item {
                CartaoApp {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MarcaProfundo, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VpnKey,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.size(10.dp))
                                Column {
                                    Text("Chave Móvel Digital", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Sessão oficial ativa (Autenticação.gov)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF16A34A),
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Superficie2)

                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text("Utente", color = Atenuado, style = MaterialTheme.typography.bodySmall)
                                Text(perfil.nome, fontWeight = FontWeight.SemiBold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Nº de Utente SNS", color = Atenuado, style = MaterialTheme.typography.bodySmall)
                                Text(perfil.numeroUtente, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text("NIF", color = Atenuado, style = MaterialTheme.typography.bodySmall)
                                Text(perfil.nif, fontWeight = FontWeight.SemiBold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Cartão de Cidadão", color = Atenuado, style = MaterialTheme.typography.bodySmall)
                                Text(perfil.cartaoCidadao, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Button(
                            onClick = { perfilRepository?.terminarSessaoCMD() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFEE2E2),
                                contentColor = Color(0xFFDC2626),
                            ),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Terminar Sessão CMD", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                CartaoApp {
                    Column {
                        Text("Dados guardados neste dispositivo", fontWeight = FontWeight.SemiBold)
                        Text(
                            "As suas preferências ficam apenas neste telemóvel e podem ser alteradas a qualquer momento.",
                            color = Atenuado,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    mensagemTeste?.let { msg ->
        AlertDialog(
            onDismissRequest = { mensagemTeste = null },
            icon = { Icon(Icons.Rounded.NotificationsActive, null, tint = MarcaMedio) },
            title = { Text("Notificação de Teste") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { mensagemTeste = null }) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun BotaoItemTeste(
    icone: ImageVector,
    corIcone: Color,
    titulo: String,
    descricao: String,
    aoClicar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = aoClicar)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(corIcone.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icone, contentDescription = null, tint = corIcone, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(descricao, color = Atenuado, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MarcaMedio.copy(alpha = 0.1f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                "Testar",
                color = MarcaMedio,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ==========================================
// PREVIEWS COM COMPOSABLE NO ANDROID STUDIO
// ==========================================

@Preview(name = "Ecrã Definições", showBackground = true)
@Composable
fun PreVisualizacaoEcraDefinicoes() {
    val perfil = Perfil(
        id = "utente_1",
        nome = "Maria Inácia Silva",
        nomeCurto = "Maria",
        iniciais = "MS",
    )
    TemaPonteSaude(textoGrande = false) {
        EcraDefinicoes(
            perfil = perfil,
        )
    }
}

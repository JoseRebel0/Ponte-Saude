@file:Suppress("SpellCheckingInspection")

package pt.ulsbe.pontesaude.ui.screens.inicio

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.SeparadorApp
import pt.ulsbe.pontesaude.util.ServicosDispositivo
import pt.ulsbe.pontesaude.ui.components.BannerModoCuidadorInicio
import pt.ulsbe.pontesaude.ui.components.BarraScrollLateralSuave
import pt.ulsbe.pontesaude.ui.components.CartaoApp
import pt.ulsbe.pontesaude.ui.theme.Atenuado
import pt.ulsbe.pontesaude.ui.theme.Marca
import pt.ulsbe.pontesaude.ui.theme.MarcaClaro
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio
import pt.ulsbe.pontesaude.ui.theme.TemaPonteSaude

private data class Atalho(val rotulo: String, val icone: ImageVector, val acao: () -> Unit)

/**
 * Ecrã Principal da aplicação com o Cartão de Emergência, atalhos rápidos e próxima consulta.
 * Inclui o Número Nacional de Saúde (SNS) no cartão, a aba com o registo da última atualização.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcraInicio(
    perfil: Perfil,
    viewModel: InicioViewModel? = null,
    perfilRepository: PerfilRepository? = null,
    aoSincronizar: (suspend () -> Unit)? = null,
    plataforma: ServicosDispositivo,
    navegarPara: (SeparadorApp) -> Unit,
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val temInternet = plataforma.temConexaoInternet()
    val perfilAtivo = if (uiState != null && uiState.perfil?.id == perfil.id) uiState.perfil else perfil
    val proxima = if (uiState != null && uiState.perfil?.id == perfil.id) {
        uiState.proximaConsulta ?: perfilAtivo.consultas.firstOrNull()
    } else {
        perfil.consultas.firstOrNull()
    }
    var aAtualizarLocal by remember { mutableStateOf(false) }
    val aAtualizar = uiState?.emCarregamento ?: aAtualizarLocal
    val textoAtualizacao = uiState?.ultimaAtualizacaoFormatada ?: "Hoje às 10:00"

    val escopo = rememberCoroutineScope()

    LaunchedEffect(perfil.id) {
        viewModel?.atualizarEstado()
    }


    val atalhos = listOf(
        Atalho("Linha SNS 24", Icons.Rounded.HeadsetMic) {
            viewModel?.ligarSaude24(plataforma) ?: plataforma.marcarNumero("808242424")
        },
        Atalho("Receitas", Icons.AutoMirrored.Rounded.ReceiptLong) { navegarPara(SeparadorApp.MEDICACAO) },
        Atalho("Consultas/Exames", Icons.Rounded.CalendarMonth) { navegarPara(SeparadorApp.CONSULTAS) },
        Atalho("Segurança do Utente", Icons.Rounded.Favorite) { navegarPara(SeparadorApp.SAUDE) },
        Atalho("Apontamentos", Icons.Rounded.Description) { navegarPara(SeparadorApp.NOTAS) },
        Atalho("Definições", Icons.Rounded.Settings) { navegarPara(SeparadorApp.DEFINICOES) },
    )

    val estadoLista = rememberLazyListState()

    val dispararAtualizacao: () -> Unit = {
        if (viewModel != null) {
            viewModel.dispararAtualizacao(aoSincronizar)
        } else {
            aAtualizarLocal = true
            escopo.launch {
                delay(600.milliseconds)
                aoSincronizar?.invoke()
                aAtualizarLocal = false
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = aAtualizar,
        onRefresh = dispararAtualizacao,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = estadoLista,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Column(
                    Modifier
                        .background(Brush.verticalGradient(listOf(MarcaMedio, Color(0xFF0F3D9E))))
                        .padding(start = 20.dp, end = 20.dp, top = 54.dp, bottom = 28.dp),
                ) {
                    val modoCuidadorAtivo = uiState?.modoCuidadorAtivo ?: perfilRepository?.modoCuidador ?: false
                    if (modoCuidadorAtivo) {
                        BannerModoCuidadorInicio(
                            viewModel = viewModel,
                            repositorio = perfilRepository,
                            plataforma = plataforma,
                            aoGerirCuidador = { navegarPara(SeparadorApp.DEFINICOES) },
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF2F6FE8), Color(0xFF5E97F5))))
                            .padding(20.dp),
                    ) {
                        Text("CARTÃO DE EMERGÊNCIA", color = Color.White.copy(alpha = .76f), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(12.dp))
                        Text(perfilAtivo.nome, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        CampoEmergencia("Nº Nacional de Saúde (SNS)", perfilAtivo.numeroUtente)
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth()) {
                            CampoEmergencia("Tipo sanguíneo", perfilAtivo.tipoSanguineo, Modifier.weight(1f))
                            CampoEmergencia("Alergias", perfilAtivo.alergias, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (temInternet) Icons.Rounded.Refresh else Icons.Rounded.WifiOff,
                                    null,
                                    tint = Color.White.copy(alpha = .8f),
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    if (temInternet) "Sincronizado online" else "Disponível sem internet",
                                    color = Color.White.copy(alpha = .8f),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { dispararAtualizacao() }
                                    .padding(vertical = 2.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    null,
                                    tint = Color.White.copy(alpha = .85f),
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    if (aAtualizar) "A carregar…" else textoAtualizacao,
                                    color = Color.White.copy(alpha = .9f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    atalhos.chunked(3).forEach { linha ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            linha.forEach { atalho ->
                                Column(
                                    Modifier.weight(1f).clickable(onClick = atalho.acao),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(Modifier.size(76.dp).background(Marca, CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(atalho.icone, null, tint = MarcaClaro, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(Modifier.height(7.dp))
                                    Text(
                                        text = atalho.rotulo,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                    if (proxima != null) {
                        CartaoApp(Modifier.clickable { navegarPara(SeparadorApp.CONSULTAS) }) {
                            Column {
                                Text("PRÓXIMA CONSULTA", style = MaterialTheme.typography.labelSmall, color = Atenuado)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${proxima.titulo} · ULSBE", fontWeight = FontWeight.SemiBold)
                                        Text("${proxima.dia} ${proxima.mes} · ${proxima.localizacao.substringAfterLast("· ")}", color = Atenuado)
                                    }
                                    Icon(Icons.Rounded.ChevronRight, null, tint = MarcaClaro)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BotaoTelefone("SNS 24", MarcaMedio, Modifier.weight(1f)) {
                            viewModel?.ligarSaude24(plataforma) ?: plataforma.marcarNumero("808242424")
                        }
                        BotaoTelefone("112", Color(0xFFDC2626), Modifier.weight(1f)) {
                            viewModel?.ligarSOS(plataforma) ?: plataforma.marcarNumero("112")
                        }
                    }
                }
            }
        }

        BarraScrollLateralSuave(
            estadoLista = estadoLista,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp, top = 54.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun CampoEmergencia(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(rotulo, color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.bodySmall)
        Text(valor, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BotaoTelefone(rotulo: String, cor: Color, modifier: Modifier, aoClicar: () -> Unit) {
    Button(onClick = aoClicar, modifier = modifier.height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = cor)) {
        Icon(Icons.Rounded.Phone, null)
        Spacer(Modifier.size(8.dp))
        Text(rotulo, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// PREVIEWS COM COMPOSABLE NO ANDROID STUDIO
// ==========================================

@Preview(name = "Ecrã Início", showBackground = true)
@Composable
fun PreVisualizacaoEcraInicio() {
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
        EcraInicio(perfil = perfil, plataforma = ServicosDispositivo(null), navegarPara = {})
    }
}

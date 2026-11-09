package pt.ulsbe.pontesaude.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.DadosIniciais
import pt.ulsbe.pontesaude.data.RepositorioApp
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.ui.ServicosDispositivo
import pt.ulsbe.pontesaude.ui.components.BotaoOuvir
import pt.ulsbe.pontesaude.ui.components.CabecalhoEcra
import pt.ulsbe.pontesaude.ui.components.CartaoApp
import pt.ulsbe.pontesaude.ui.theme.Atenuado
import pt.ulsbe.pontesaude.ui.theme.Marca
import pt.ulsbe.pontesaude.ui.theme.MarcaClaro
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio
import pt.ulsbe.pontesaude.ui.theme.Superficie2
import pt.ulsbe.pontesaude.ui.theme.TemaPonteSaude

/**
 * Ecrã de Notas e Apontamentos com o Tradutor Médico e Assistente IA no topo,
 * seguido do bloco de registo e gestão de notas clínicas do utente.
 * As perguntas simples foram removidas e a HUD está preparada para futura API de IA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcraApontamentos(perfil: Perfil, repositorio: RepositorioApp, plataforma: ServicosDispositivo) {
    var consultaTradutor by remember { mutableStateOf("") }
    var explicacaoTradutor by remember { mutableStateOf<String?>(null) }
    var rascunhoNota by remember(perfil.id) { mutableStateOf("") }
    var avisoNota by remember { mutableStateOf("") }
    var aAtualizar by remember { mutableStateOf(false) }
    val estadoLista = rememberLazyListState()
    val escopo = rememberCoroutineScope()

    // Ditado por voz para o Tradutor Médico
    val lancadorVozTradutor = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val falado = resultado.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (falado.isNotBlank()) {
                consultaTradutor = falado
                val res = pesquisarGlossario(falado)
                explicacaoTradutor = res
                plataforma.falar(res)
            }
        }
    }

    // Ditado por voz para as Notas
    val lancadorVozNota = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            rascunhoNota = resultado.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        }
    }

    fun adicionarNotaAoRegisto(texto: String) {
        if (texto.isNotBlank()) {
            repositorio.adicionarNota(perfil, Nota(texto.trim(), if (repositorio.modoCuidador) "Cuidador" else "Eu"))
        }
    }

    Column(Modifier.fillMaxSize()) {
        CabecalhoEcra("Notas", "Tradutor IA & Apontamentos", perfil.iniciais)
        PullToRefreshBox(
            isRefreshing = aAtualizar,
            onRefresh = {
                aAtualizar = true
                escopo.launch {
                    delay(800) // Transição suave de carregamento
                    repositorio.registarAtualizacao()
                    aAtualizar = false
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                state = estadoLista,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. TRADUTOR MÉDICO & ASSISTENTE IA NO TOPO
                item {
                    CartaoApp {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Marca),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.SmartToy,
                                        contentDescription = null,
                                        tint = MarcaClaro,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Tradutor Médico & Assistente IA",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Linguagem simples para termos clínicos e dúvidas dos utentes",
                                        color = Atenuado,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Escreva ou dite o termo médico que ouviu ou a sua questão. O assistente explica em linguagem clara e descomplicada (preparado para ligação futura a API de IA).",
                                color = Atenuado,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp,
                            )

                            Spacer(Modifier.height(14.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = consultaTradutor,
                                    onValueChange = { consultaTradutor = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Ex: jejum, hipertensão, anticoagulante…") },
                                    singleLine = true,
                                )
                                FilledIconButton(
                                    onClick = {
                                        runCatching {
                                            lancadorVozTradutor.launch(
                                                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-PT")
                                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga o termo médico ou dúvida")
                                                },
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(52.dp),
                                ) { Icon(Icons.Rounded.Mic, "Ditar termo por voz") }
                            }

                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val res = pesquisarGlossario(consultaTradutor)
                                    explicacaoTradutor = res
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                            ) {
                                Text("Explicar de forma simples", fontWeight = FontWeight.SemiBold)
                            }

                            explicacaoTradutor?.let { texto ->
                                Spacer(Modifier.height(14.dp))
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Marca)
                                        .padding(16.dp),
                                ) {
                                    Text(texto, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        BotaoOuvir(
                                            aoClicar = { plataforma.falar(texto) },
                                            modifier = Modifier.weight(1f),
                                        )
                                        FilledTonalButton(
                                            onClick = {
                                                adicionarNotaAoRegisto("Dúvida: \"$consultaTradutor\" — $texto")
                                            },
                                        ) {
                                            Icon(Icons.Rounded.AddComment, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Guardar nas Notas", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. BLOCO DE ADICIONAR APONTAMENTO DO UTENTE
                item {
                    CartaoApp {
                        Column {
                            Text(
                                "Os Meus Apontamentos para a Consulta",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Registe dúvidas, sintomas ou alterações para falar com o seu médico",
                                color = Atenuado,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = rascunhoNota,
                                onValueChange = { rascunhoNota = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                placeholder = { Text("Escreva a sua dúvida ou sintoma…") },
                            )
                            if (avisoNota.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(avisoNota, color = Atenuado, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledIconButton(
                                    onClick = {
                                        runCatching {
                                            lancadorVozNota.launch(
                                                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-PT")
                                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga a sua dúvida para o médico")
                                                },
                                            )
                                        }.onFailure { avisoNota = "Este telemóvel não suporta ditado. Escreva a dúvida." }
                                    },
                                    modifier = Modifier.size(52.dp),
                                ) { Icon(Icons.Rounded.Mic, "Gravar dúvida por voz") }

                                Button(
                                    onClick = {
                                        adicionarNotaAoRegisto(rascunhoNota)
                                        rascunhoNota = ""
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                ) { Text("Guardar Nota", fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }

                // 3. LISTA DE APONTAMENTOS REGISTADOS
                val notas = repositorio.notas(perfil)
                if (notas.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Ainda não tem notas guardadas. Use o campo acima para registar as suas dúvidas para a próxima consulta.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Atenuado,
                            )
                        }
                    }
                } else {
                    itemsIndexed(notas) { indice, nota ->
                        CartaoApp {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(nota.texto, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "${nota.autor} · registo pessoal do utente",
                                        color = Atenuado,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                IconButton(
                                    onClick = { repositorio.apagarNota(perfil, indice) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(Icons.Rounded.Delete, "Apagar nota", tint = Atenuado, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Aliases para compatibilidade
@Composable
fun EcraNotas(perfil: Perfil, repositorio: RepositorioApp, plataforma: ServicosDispositivo) =
    EcraApontamentos(perfil, repositorio, plataforma)

// ==========================================
// PREVIEWS COM COMPOSABLE NO ANDROID STUDIO
// ==========================================

@Preview(name = "Ecrã Apontamentos", showBackground = true)
@Composable
fun PreVisualizacaoEcraApontamentos() {
    val perfil = DadosIniciais.perfis.first()
    val contexto = LocalContext.current
    TemaPonteSaude(textoGrande = false) {
        EcraApontamentos(perfil = perfil, repositorio = remember { RepositorioApp(contexto) }, plataforma = ServicosDispositivo(null))
    }
}

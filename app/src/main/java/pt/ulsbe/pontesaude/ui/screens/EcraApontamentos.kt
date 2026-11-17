package pt.ulsbe.pontesaude.ui.screens

import android.app.Activity
import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.SupervisorAccount
import androidx.compose.material.icons.rounded.VpnKey
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

data class ItemHistoricoIa(
    val pergunta: String,
    val resposta: String,
    val dataHora: String,
)

private fun carregarHistoricoIa(context: Context, idPerfil: String): List<ItemHistoricoIa> {
    val prefs = context.getSharedPreferences("ponte_saude", Context.MODE_PRIVATE)
    val jsonString = prefs.getString("historico_ia_$idPerfil", null) ?: return emptyList()
    return runCatching {
        val array = org.json.JSONArray(jsonString)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            ItemHistoricoIa(
                pergunta = obj.getString("p"),
                resposta = obj.getString("r"),
                dataHora = obj.getString("d"),
            )
        }
    }.getOrDefault(emptyList())
}

private fun guardarHistoricoIa(context: Context, idPerfil: String, lista: List<ItemHistoricoIa>) {
    val prefs = context.getSharedPreferences("ponte_saude", Context.MODE_PRIVATE)
    val array = org.json.JSONArray()
    lista.take(20).forEach { item ->
        val obj = org.json.JSONObject().apply {
            put("p", item.pergunta)
            put("r", item.resposta)
            put("d", item.dataHora)
        }
        array.put(obj)
    }
    prefs.edit().putString("historico_ia_$idPerfil", array.toString()).apply()
}

/**
 * Ecrã de Notas e Apontamentos com o Tradutor Médico e Assistente IA no topo,
 * seguido do bloco de registo e gestão de notas clínicas do utente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcraApontamentos(perfil: Perfil, repositorio: RepositorioApp, plataforma: ServicosDispositivo) {
    val contexto = LocalContext.current
    var consultaTradutor by remember { mutableStateOf("") }
    var explicacaoTradutor by remember { mutableStateOf<String?>(null) }
    var aProcessarIa by remember { mutableStateOf(false) }
    var avisoSemInternet by remember { mutableStateOf(false) }
    var mensagemErroIa by remember { mutableStateOf<String?>(null) }
    var historicoIa by remember(perfil.id) { mutableStateOf(carregarHistoricoIa(contexto, perfil.id)) }
    var mostrarHistoricoIa by remember { mutableStateOf(false) }
    var rascunhoNota by remember(perfil.id) { mutableStateOf("") }
    var avisoNota by remember { mutableStateOf("") }
    var aAtualizar by remember { mutableStateOf(false) }
    val estadoLista = rememberLazyListState()
    val escopo = rememberCoroutineScope()

    fun traduzirConsulta(termo: String) {
        if (termo.isBlank()) return
        if (!plataforma.temConexaoInternet()) {
            avisoSemInternet = true
            return
        }
        avisoSemInternet = false
        mensagemErroIa = null
        aProcessarIa = true
        escopo.launch {
            val resultado = repositorio.traduzirTermoComIa(termo)
            resultado.onSuccess { explicacao ->
                explicacaoTradutor = explicacao
                val dataAtual = java.text.SimpleDateFormat("dd/MM · HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                val novoItem = ItemHistoricoIa(pergunta = termo.trim(), resposta = explicacao.trim(), dataHora = dataAtual)
                val listaAtualizada = listOf(novoItem) + historicoIa.filterNot { it.pergunta.equals(termo.trim(), ignoreCase = true) }
                historicoIa = listaAtualizada
                guardarHistoricoIa(contexto, perfil.id, listaAtualizada)
            }.onFailure { erro ->
                android.util.Log.e("EcraApontamentos", "Falha na IA: ${erro.message}", erro)
                mensagemErroIa = erro.localizedMessage ?: erro.message.orEmpty()
            }
            aProcessarIa = false
        }
    }

    // Ditado por voz para o Tradutor Médico
    val lancadorVozTradutor = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val falado = resultado.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (falado.isNotBlank()) {
                consultaTradutor = falado
                traduzirConsulta(falado)
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
            val autorNota = if (repositorio.modoCuidador) "Cuidador de ${perfil.nomeCurto}" else "Eu"
            repositorio.adicionarNota(perfil, Nota(texto.trim(), autorNota))
        }
    }

    Column(Modifier.fillMaxSize()) {
        val tituloEcra = if (repositorio.modoCuidador) "Notas · ${perfil.nome}" else "Notas"
        val subtituloEcra = if (repositorio.modoCuidador) "Conta de ${perfil.nome} (Modo Cuidador)" else "Tradutor IA & Apontamentos"
        CabecalhoEcra(tituloEcra, subtituloEcra, perfil.iniciais)
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
                // CARTÃO DE MODO CUIDADOR
                if (repositorio.modoCuidador) {
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
                                        "Notas e Dúvidas de ${perfil.nome}",
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
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Marca),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MarcaClaro,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Tradutor Clínico & Assistente IA",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Apoio à literacia em saúde · ULS Barcelos / Esposende",
                                        color = Atenuado,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                IconButton(
                                    onClick = { mostrarHistoricoIa = true },
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.History,
                                        contentDescription = "Ver histórico de perguntas",
                                        tint = MarcaMedio,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // AVISO DE PRIVACIDADE E DADOS PESSOAIS (RGPD)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Superficie2)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MarcaMedio,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Privacidade e Proteção de Dados: O assistente não acede ao seu processo clínico nem guarda dados pessoais sensíveis (NIF, nº SNS ou morada). As respostas são puramente informativas.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Atenuado,
                                    lineHeight = 16.sp,
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Escreva ou dite o termo médico, exame ou receita para receber uma explicação clara e descomplicada.",
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
                                    shape = RoundedCornerShape(14.dp),
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

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { traduzirConsulta(consultaTradutor) },
                                enabled = !aProcessarIa && consultaTradutor.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                            ) {
                                if (aProcessarIa) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("A analisar termo clínico…", fontWeight = FontWeight.SemiBold)
                                } else {
                                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Explicar de forma simples", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (!plataforma.temConexaoInternet()) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                ) {
                                    Icon(Icons.Rounded.WifiOff, null, tint = Atenuado, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Ligue a internet para utilizar o Tradutor IA.",
                                        color = Atenuado,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }

                            explicacaoTradutor?.let { texto ->
                                Spacer(Modifier.height(16.dp))
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Marca)
                                        .padding(16.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Rounded.SmartToy,
                                            contentDescription = null,
                                            tint = MarcaClaro,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Explicação Clínica Descomplicada",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MarcaClaro,
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        texto,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 22.sp,
                                    )
                                    Spacer(Modifier.height(14.dp))
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
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        "Aviso: Informação gerada por Inteligência Artificial para apoio à literacia em saúde. Não substitui consulta médica.",
                                        color = Atenuado,
                                        style = MaterialTheme.typography.labelSmall,
                                        lineHeight = 15.sp,
                                    )
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

    if (avisoSemInternet) {
        AlertDialog(
            onDismissRequest = { avisoSemInternet = false },
            icon = {
                Icon(
                    Icons.Rounded.WifiOff,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp),
                )
            },
            title = {
                Text(
                    "Sem Ligação à Internet",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    "O Tradutor com Inteligência Artificial necessita de ligação à internet para responder às suas dúvidas. Por favor, ligue o Wi-Fi ou os dados móveis no seu telemóvel.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = { avisoSemInternet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                ) {
                    Text("Compreendi")
                }
            },
        )
    }

    if (mensagemErroIa != null) {
        AlertDialog(
            onDismissRequest = { mensagemErroIa = null },
            icon = {
                Icon(
                    Icons.Rounded.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp),
                )
            },
            title = {
                Text(
                    "Resposta do Serviço de IA",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    mensagemErroIa ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = { mensagemErroIa = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                ) {
                    Text("Fechar")
                }
            },
        )
    }

    if (mostrarHistoricoIa) {
        AlertDialog(
            onDismissRequest = { mostrarHistoricoIa = false },
            icon = {
                Icon(
                    Icons.Rounded.History,
                    contentDescription = null,
                    tint = MarcaMedio,
                    modifier = Modifier.size(36.dp),
                )
            },
            title = {
                Text(
                    "Histórico de Perguntas",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Superficie2)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Atenuado,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Guardado apenas localmente no telemóvel.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Atenuado,
                        )
                    }

                    if (historicoIa.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Ainda não fez perguntas ao assistente.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Atenuado,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(historicoIa.size) { index ->
                                val item = historicoIa[index]
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Superficie2)
                                        .clickable {
                                            consultaTradutor = item.pergunta
                                            explicacaoTradutor = item.resposta
                                            mostrarHistoricoIa = false
                                        }
                                        .padding(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = item.pergunta,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = item.dataHora,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Atenuado,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = item.resposta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Atenuado,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Toque para abrir e reler",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MarcaMedio,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarHistoricoIa = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                ) {
                    Text("Fechar")
                }
            },
            dismissButton = if (historicoIa.isNotEmpty()) {
                {
                    TextButton(
                        onClick = {
                            historicoIa = emptyList()
                            guardarHistoricoIa(contexto, perfil.id, emptyList())
                        },
                    ) {
                        Text("Limpar", color = Color(0xFFDC2626))
                    }
                }
            } else null,
        )
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
    val perfil = Perfil(
        id = "utente_1",
        nome = "Maria Inácia Silva",
        nomeCurto = "Maria",
        iniciais = "MS",
        notas = listOf(Nota("Perguntar sobre a tensão matinal", "Eu")),
    )
    val contexto = LocalContext.current
    TemaPonteSaude(textoGrande = false) {
        EcraApontamentos(perfil = perfil, repositorio = remember { RepositorioApp(contexto) }, plataforma = ServicosDispositivo(null))
    }
}

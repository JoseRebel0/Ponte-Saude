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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SupervisorAccount
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.ui.screens.definicoes.DefinicoesViewModel
import pt.ulsbe.pontesaude.ui.screens.inicio.InicioViewModel
import pt.ulsbe.pontesaude.util.ServicosDispositivo
import pt.ulsbe.pontesaude.ui.theme.Atenuado
import pt.ulsbe.pontesaude.ui.theme.MarcaClaro
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio
import pt.ulsbe.pontesaude.ui.theme.MarcaProfundo
import pt.ulsbe.pontesaude.ui.theme.Superficie2

/**
 * Diálogo interativo e profissional para Adicionar ou Associar uma Nova Pessoa a Cuidar.
 * Permite:
 * 1. Escolher um perfil existente no sistema (Firestore / Room) e associá-lo.
 * 2. Criar e registar um novo perfil com código de autorização simulado por SMS.
 */
@Composable
fun DialogoAdicionarPessoaCuidada(
    viewModel: DefinicoesViewModel? = null,
    repositorio: PerfilRepository? = null,
    plataforma: ServicosDispositivo?,
    aoFechar: () -> Unit,
    aoPessoaAdicionada: (Perfil) -> Unit,
) {
    var abaSelecionada by remember { mutableIntStateOf(0) } // 0: Escolher Existente, 1: Novo Registo
    var passo by remember { mutableIntStateOf(1) } // 1: Formulário / Escolha, 2: Código SMS, 3: Sucesso

    var nome by remember { mutableStateOf("") }
    var telemovel by remember { mutableStateOf("") }
    var numeroUtente by remember { mutableStateOf("") }
    var erroMensagem by remember { mutableStateOf<String?>(null) }

    var codigoSMS by remember { mutableStateOf("") }
    var codigoSmsGerado by remember { mutableStateOf("749218") }
    var perfilCriado by remember { mutableStateOf<Perfil?>(null) }

    val todos = viewModel?.uiState?.value?.todosOsPerfis ?: repositorio?.todosOsPerfis.orEmpty()
    val cuidados = viewModel?.uiState?.value?.pessoasCuidadas ?: repositorio?.pessoasCuidadas.orEmpty()
    val sessaoId = viewModel?.uiState?.value?.perfilSessao?.id ?: repositorio?.utenteSessaoId.orEmpty()
    val perfisDisponiveis = remember(todos.size, cuidados.size) {
        val idsCuidados = cuidados.map { it.id }.toSet()
        todos.filter { it.id != sessaoId && it.id !in idsCuidados }
    }

    AlertDialog(
        onDismissRequest = aoFechar,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // CABEÇALHO DO DIÁLOGO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFEFF6FF), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.PersonAddAlt1,
                                contentDescription = null,
                                tint = MarcaMedio,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Associar Utente a Cuidar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (passo == 3) "Concluído com Sucesso" else "Selecione ou registe o perfil do utente",
                                style = MaterialTheme.typography.labelSmall,
                                color = Atenuado,
                            )
                        }
                    }
                    IconButton(onClick = aoFechar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = Atenuado)
                    }
                }

                HorizontalDivider(color = Superficie2)

                // ABAS DE ESCOLHA (Só se ainda não estiver no passo de sucesso)
                if (passo != 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Superficie2)
                            .padding(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (abaSelecionada == 0) Color.White else Color.Transparent)
                                .clickable {
                                    abaSelecionada = 0
                                    passo = 1
                                    erroMensagem = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Perfis Existentes",
                                fontSize = 12.sp,
                                fontWeight = if (abaSelecionada == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (abaSelecionada == 0) MarcaProfundo else Atenuado,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (abaSelecionada == 1) Color.White else Color.Transparent)
                                .clickable {
                                    abaSelecionada = 1
                                    passo = 1
                                    erroMensagem = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Novo Registo (SMS)",
                                fontSize = 12.sp,
                                fontWeight = if (abaSelecionada == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (abaSelecionada == 1) MarcaProfundo else Atenuado,
                            )
                        }
                    }
                }

                // CORPO CONSOANTE A ABA E O PASSO
                if (passo == 3) {
                    // PASSO 3: SUCESSO
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFECFDF5), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(40.dp),
                            )
                        }

                        Text(
                            text = "Associação Concluída!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                        )

                        Text(
                            text = "O perfil de ${perfilCriado?.nome ?: "Utente"} está agora associado ao seu Modo Cuidador e guardado na base de dados Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Atenuado,
                        )

                        Button(
                            onClick = {
                                perfilCriado?.let { aoPessoaAdicionada(it) }
                                aoFechar()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Aceder como Cuidador Agora")
                        }
                    }
                } else if (abaSelecionada == 0) {
                    // ABA 0: ESCOLHER PERFIL EXISTENTE
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Escolha um utente registado para passar a ser seu cuidador:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Atenuado,
                        )

                        if (perfisDisponiveis.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(14.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Nenhum outro utente disponível para associar.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF92400E),
                                    )
                                    Text(
                                        text = "Todos os perfis existentes já estão sob o seu cuidado ou pode registar um novo utente na aba seguinte.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB45309),
                                    )
                                    TextButton(
                                        onClick = { abaSelecionada = 1 },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                    ) {
                                        Text("Registar Novo Utente ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                    }
                                }
                            }
                        } else {
                            perfisDisponiveis.forEach { p ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Superficie2),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(MarcaClaro, CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(p.iniciais, color = MarcaProfundo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(p.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("Nº SNS: ${p.numeroUtente} · ID: ${p.id}", style = MaterialTheme.typography.labelSmall, color = Atenuado)
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (viewModel != null) {
                                                    viewModel.associarPerfilCuidado(p.id)
                                                } else {
                                                    repositorio?.associarPerfilCuidado(p.id)
                                                }
                                                perfilCriado = p
                                                passo = 3
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Icon(Icons.Rounded.PersonAddAlt1, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Cuidar", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ABA 1: REGISTO COM SMS
                    when (passo) {
                        1 -> {
                            Text(
                                text = "Insira os dados do utente que vai acompanhar. Será enviado um código de autorização por SMS para o telemóvel do mesmo:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Atenuado,
                            )

                            OutlinedTextField(
                                value = nome,
                                onValueChange = {
                                    nome = it
                                    erroMensagem = null
                                },
                                label = { Text("Nome Completo do Utente") },
                                placeholder = { Text("Ex: Maria da Conceição") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Rounded.Person, null, tint = MarcaMedio) },
                            )

                            OutlinedTextField(
                                value = telemovel,
                                onValueChange = {
                                    telemovel = it.filter { c -> c.isDigit() || c == ' ' || c == '+' }
                                    erroMensagem = null
                                },
                                label = { Text("Nº Telemóvel do Utente") },
                                placeholder = { Text("912 345 678") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, null, tint = MarcaMedio) },
                            )

                            OutlinedTextField(
                                value = numeroUtente,
                                onValueChange = { numeroUtente = it },
                                label = { Text("Nº Utente de Saúde SNS (Opcional)") },
                                placeholder = { Text("Ex: 189 423 761") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )

                            if (erroMensagem != null) {
                                Text(
                                    text = erroMensagem.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }

                            Button(
                                onClick = {
                                    if (nome.isBlank()) {
                                        erroMensagem = "Por favor, indique o nome da pessoa."
                                        return@Button
                                    }
                                    val digitos = telemovel.filter { it.isDigit() }
                                    if (digitos.length < 9) {
                                        erroMensagem = "Indique um número de telemóvel válido com pelo menos 9 dígitos."
                                        return@Button
                                    }

                                    val novosDigitos = (100000..999999).random().toString()
                                    codigoSmsGerado = novosDigitos

                                    plataforma?.enviarNotificacaoSeguranca(
                                        titulo = "📩 PonteSaúde SMS: Autorização de Cuidador",
                                        mensagem = "Código de autorização: $codigoSmsGerado. O cuidador pediu acesso para gerir os seus cuidados.",
                                    )

                                    passo = 2
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Message, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Enviar Pedido por SMS")
                            }
                        }

                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.Shield, null, tint = MarcaMedio, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "Foi enviado um SMS com o código de autorização de 6 dígitos para (+351) $telemovel.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1E3A8A),
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .padding(10.dp),
                                ) {
                                    Column {
                                        Text(
                                            text = "Mensagem recebida no telemóvel de $nome:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Atenuado,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "«PonteSaúde: Código de confirmação $codigoSmsGerado para autorizar o seu cuidador.»",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        TextButton(
                                            onClick = { codigoSMS = codigoSmsGerado },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                        ) {
                                            Text("Preencher código automaticamente ($codigoSmsGerado)", fontSize = 12.sp, color = MarcaMedio)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = codigoSMS,
                                    onValueChange = {
                                        if (it.length <= 6) codigoSMS = it.filter { c -> c.isDigit() }
                                        erroMensagem = null
                                    },
                                    label = { Text("Código SMS de 6 dígitos") },
                                    placeholder = { Text("Ex: $codigoSmsGerado") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                )

                                if (erroMensagem != null) {
                                    Text(
                                        text = erroMensagem.orEmpty(),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (codigoSMS.trim() != codigoSmsGerado) {
                                            erroMensagem = "Código inválido. Por favor, introduza o código de 6 dígitos recebido por SMS."
                                            return@Button
                                        }

                                        val novo = if (viewModel != null) {
                                            viewModel.adicionarPessoaCuidada(
                                                nome = nome,
                                                relacao = "Utente",
                                                numeroUtente = numeroUtente,
                                                nif = telemovel,
                                            )
                                        } else {
                                            repositorio?.adicionarPessoaCuidada(
                                                nome = nome,
                                                relacao = "Utente",
                                                telemovel = telemovel,
                                                numeroUtente = numeroUtente,
                                            ) ?: Perfil(
                                                id = "temp_${System.currentTimeMillis()}",
                                                nome = nome,
                                                nomeCurto = nome,
                                                relacao = "Utente",
                                                iniciais = nome.take(2).uppercase(),
                                                numeroUtente = numeroUtente,
                                            )
                                        }
                                        perfilCriado = novo
                                        passo = 3
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Icon(Icons.Rounded.Verified, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Validar e Concluir Vinculação")
                                }

                                TextButton(
                                    onClick = { passo = 1 },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Voltar e corrigir número")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

/**
 * Secção Completa do Modo Cuidador para o Ecrã de Definições:
 * 1. Toggle e Utente em Acompanhamento Ativo.
 * 2. Perfis que eu cuido (perfisCuidadosIds) com opção de desassociar.
 * 3. Cuidadores que cuidam de mim (cuidadoresIds).
 */
@Composable
fun SecaoModoCuidador(
    viewModel: DefinicoesViewModel? = null,
    repositorio: PerfilRepository? = null,
    plataforma: ServicosDispositivo? = null,
    modifier: Modifier = Modifier,
) {
    var mostrarDialogoAdicionar by remember { mutableStateOf(false) }
    var mostrarDialogoTrocar by remember { mutableStateOf(false) }
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val perfilAtivo = uiState?.perfil ?: repositorio?.perfilAtivo ?: Perfil(
        id = "0",
        nome = "Utilizador",
        nomeCurto = "Utilizador",
        relacao = "Utente",
        iniciais = "U",
    )
    val perfilSessao = uiState?.perfilSessao ?: repositorio?.perfilSessao ?: perfilAtivo
    val modoCuidador = uiState?.modoCuidador ?: repositorio?.modoCuidador ?: false
    val pessoasCuidadas = uiState?.pessoasCuidadas ?: repositorio?.pessoasCuidadas.orEmpty()
    val todosOsPerfis = uiState?.todosOsPerfis ?: repositorio?.todosOsPerfis.orEmpty()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // LINHA MESTRE COM TOGGLE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (modoCuidador) Color(0xFFDCFCE7) else Superficie2, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.SupervisorAccount,
                        contentDescription = null,
                        tint = if (modoCuidador) Color(0xFF15803D) else Atenuado,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Modo Cuidador",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Acompanhar a conta e prescrições de outro utente",
                        style = MaterialTheme.typography.bodySmall,
                        color = Atenuado,
                    )
                }
            }

            Switch(
                checked = modoCuidador,
                onCheckedChange = { ativo ->
                    if (viewModel != null) {
                        viewModel.atualizarModoCuidador(ativo)
                    } else {
                        repositorio?.atualizarModoCuidador(ativo)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981),
                ),
            )
        }

        // CONTEÚDO QUANDO MODO CUIDADOR ESTÁ ATIVO
        if (modoCuidador) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF10B981)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (pessoasCuidadas.isEmpty()) {
                        Text(
                            text = "Ainda não tem utentes associados",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                        )
                        Text(
                            text = "Para começar a utilizar o Modo Cuidador, associe os familiares ou utentes que pretende acompanhar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Atenuado,
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Utente em Acompanhamento",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                            )
                            if (pessoasCuidadas.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFDCFCE7))
                                        .clickable { mostrarDialogoTrocar = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "Trocar Utente ▾",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF10B981), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = perfilAtivo.iniciais,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = perfilAtivo.nome,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Nº SNS: ${perfilAtivo.numeroUtente} · ${perfilAtivo.idadeFormatada}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Atenuado,
                                )
                            }
                        }
                    }

                    // BOTÃO PARA ADICIONAR UTENTE
                    Button(
                        onClick = { mostrarDialogoAdicionar = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF15803D),
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                    ) {
                        Icon(Icons.Rounded.PersonAddAlt1, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (pessoasCuidadas.isEmpty()) "Associar Primeiro Utente" else "Associar Novo Utente",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // ==========================================
        // 1. PERFIS QUE EU CUIDO (perfisCuidadosIds)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Superficie2),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FamilyRestroom, null, tint = MarcaMedio, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Perfis que eu cuido",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "${pessoasCuidadas.size} utente(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Atenuado,
                    )
                }

                if (pessoasCuidadas.isEmpty()) {
                    Text(
                        text = "Não está a cuidar de nenhum perfil atualmente. Pode associar utentes existentes ou criar novos para gerir a sua medicação e consultas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Atenuado,
                    )
                    TextButton(
                        onClick = { mostrarDialogoAdicionar = true },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Associar utente a cuidar", fontSize = 12.sp, color = MarcaMedio, fontWeight = FontWeight.Bold)
                    }
                } else {
                    pessoasCuidadas.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(MarcaClaro, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(p.iniciais, color = MarcaProfundo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(p.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text("Nº SNS: ${p.numeroUtente} · ID: ${p.id}", style = MaterialTheme.typography.labelSmall, color = Atenuado)
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (viewModel != null) {
                                        viewModel.desassociarPerfilCuidado(p.id)
                                    } else {
                                        repositorio?.desassociarPerfilCuidado(p.id)
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Desassociar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. CUIDADORES QUE CUIDAM DE MIM (cuidadoresIds)
        // ==========================================
        val cuidadoresDeMim = todosOsPerfis.filter { it.id in perfilSessao.cuidadoresIds && it.id != perfilSessao.id }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Superficie2),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shield, null, tint = MarcaMedio, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Quem cuida de mim",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "${cuidadoresDeMim.size} cuidador(es)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Atenuado,
                    )
                }

                if (cuidadoresDeMim.isEmpty()) {
                    Text(
                        text = "Nenhum cuidador associado a esta conta. Quando outro utente o associar como cuidador, aparecerá aqui.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Atenuado,
                    )
                } else {
                    cuidadoresDeMim.forEach { cuidador ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFFDCFCE7), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(cuidador.iniciais, color = Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(cuidador.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("Cuidador Autorizado · Nº SNS: ${cuidador.numeroUtente}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF15803D))
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoAdicionar) {
        DialogoAdicionarPessoaCuidada(
            viewModel = viewModel,
            repositorio = repositorio,
            plataforma = plataforma,
            aoFechar = { mostrarDialogoAdicionar = false },
            aoPessoaAdicionada = {
                mostrarDialogoAdicionar = false
            },
        )
    }

    if (mostrarDialogoTrocar) {
        DialogoTrocarContaCuidador(
            viewModel = viewModel,
            repositorio = repositorio,
            aoFechar = { mostrarDialogoTrocar = false },
            aoGerir = { mostrarDialogoTrocar = false },
        )
    }
}

/**
 * Banner no Topo do Ecrã Inicial quando o Modo Cuidador está ativo.
 */
@Composable
fun BannerModoCuidadorInicio(
    viewModel: InicioViewModel? = null,
    repositorio: PerfilRepository? = null,
    plataforma: ServicosDispositivo? = null,
    aoGerirCuidador: () -> Unit,
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val modoAtivo = uiState?.modoCuidadorAtivo ?: repositorio?.modoCuidador ?: false
    val pessoas = uiState?.pessoasCuidadas ?: repositorio?.pessoasCuidadas ?: emptyList()
    val pessoaAtual = uiState?.perfil ?: repositorio?.perfilAtivo

    if (!modoAtivo || pessoas.isEmpty() || pessoaAtual == null) return

    var mostrarTrocaRapida by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF0F766E), Color(0xFF0D9488)),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = pessoaAtual.iniciais,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "CONTA EM ACOMPANHAMENTO",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = pessoaAtual.nome,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Nº SNS: ${pessoaAtual.numeroUtente} · ${pessoaAtual.idadeFormatada}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .clickable { mostrarTrocaRapida = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Trocar ▾",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (mostrarTrocaRapida) {
        DialogoTrocarContaCuidador(
            viewModelInicio = viewModel,
            repositorio = repositorio,
            aoFechar = { mostrarTrocaRapida = false },
            aoGerir = aoGerirCuidador,
        )
    }
}

/**
 * Diálogo rápido para alternar instantaneamente entre a conta pessoal e as contas de utentes cuidados.
 */
@Composable
fun DialogoTrocarContaCuidador(
    viewModel: DefinicoesViewModel? = null,
    viewModelInicio: InicioViewModel? = null,
    repositorio: PerfilRepository? = null,
    aoFechar: () -> Unit,
    aoGerir: () -> Unit,
) {
    val defState = viewModel?.uiState?.collectAsState()?.value
    val iniState = viewModelInicio?.uiState?.collectAsState()?.value

    val perfilPessoal = defState?.perfilSessao
        ?: iniState?.perfilSessao
        ?: repositorio?.perfilSessao
        ?: Perfil(
            id = "0",
            nome = "Utilizador",
            nomeCurto = "Utilizador",
            relacao = "Utente",
            iniciais = "U",
        )

    val modoCuidador = defState?.modoCuidador
        ?: iniState?.modoCuidadorAtivo
        ?: repositorio?.modoCuidador
        ?: false

    val pessoasCuidadas = defState?.pessoasCuidadas
        ?: iniState?.pessoasCuidadas
        ?: repositorio?.pessoasCuidadas
        ?: emptyList()

    val indicePerfilCuidado = defState?.indicePerfilCuidado
        ?: iniState?.indicePerfilCuidado
        ?: repositorio?.indicePerfilCuidado
        ?: 0

    AlertDialog(
        onDismissRequest = aoFechar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SwitchAccount, null, tint = MarcaMedio)
                Spacer(Modifier.width(8.dp))
                Text("Trocar de Conta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Selecione o perfil que deseja consultar e gerir no Ponte Saúde:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Atenuado,
                )

                // 1. CONTA PESSOAL DO UTILIZADOR
                val pessoalAtivo = !modoCuidador
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel?.atualizarModoCuidador(false)
                                ?: viewModelInicio?.atualizarModoCuidador(false)
                                ?: repositorio?.atualizarModoCuidador(false)
                            aoFechar()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (pessoalAtivo) Color(0xFFEFF6FF) else Superficie2,
                    ),
                    border = if (pessoalAtivo) androidx.compose.foundation.BorderStroke(1.5.dp, MarcaMedio) else null,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MarcaMedio, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(perfilPessoal.iniciais, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(perfilPessoal.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("A Minha Conta Pessoal · Nº SNS: ${perfilPessoal.numeroUtente}", style = MaterialTheme.typography.labelSmall, color = Atenuado)
                        }
                        if (pessoalAtivo) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = MarcaMedio, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                HorizontalDivider(color = Superficie2)

                Text(
                    text = "Contas de Utentes em Acompanhamento:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Atenuado,
                )

                // 2. LISTA DE CONTAS DE PESSOAS CUIDADAS
                if (pessoasCuidadas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Superficie2)
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "Ainda não tem utentes associados para acompanhar. Pode associar utentes nas Definições da aplicação.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Atenuado,
                        )
                    }
                } else {
                    pessoasCuidadas.forEachIndexed { idx, p ->
                        val atual = modoCuidador && indicePerfilCuidado == idx
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!modoCuidador) {
                                        viewModel?.atualizarModoCuidador(true)
                                            ?: viewModelInicio?.atualizarModoCuidador(true)
                                            ?: repositorio?.atualizarModoCuidador(true)
                                    }
                                    viewModel?.definirPerfilCuidado(idx)
                                        ?: viewModelInicio?.definirPerfilCuidado(idx)
                                        ?: repositorio?.definirPerfilCuidado(idx)
                                    aoFechar()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (atual) Color(0xFFECFDF5) else Superficie2,
                            ),
                            border = if (atual) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)) else null,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (atual) Color(0xFF10B981) else Color(0xFF94A3B8), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(p.iniciais, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.nome,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (atual) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Nº SNS: ${p.numeroUtente} · ${p.idadeFormatada}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Atenuado,
                                    )
                                }
                                if (atual) {
                                    Icon(Icons.Rounded.Verified, null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    aoFechar()
                    aoGerir()
                },
            ) {
                Text("Gerir Cuidados nas Definições")
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) {
                Text("Fechar")
            }
        },
    )
}

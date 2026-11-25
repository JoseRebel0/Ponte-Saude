@file:Suppress("SpellCheckingInspection")

package pt.ulsbe.pontesaude.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.R
import pt.ulsbe.pontesaude.data.repository.AppContainer
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.ui.theme.Atenuado
import pt.ulsbe.pontesaude.ui.theme.Marca
import pt.ulsbe.pontesaude.ui.theme.MarcaClaro
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio
import pt.ulsbe.pontesaude.ui.theme.MarcaProfundo
import pt.ulsbe.pontesaude.ui.theme.MarcaTopo
import pt.ulsbe.pontesaude.ui.theme.Superficie
import pt.ulsbe.pontesaude.ui.theme.Superficie2
import pt.ulsbe.pontesaude.ui.theme.TemaPonteSaude

private enum class ModoIdentificacao {
    TELEMOVEL, NIF
}

/**
 * Ecrã de Autenticação com a Chave Móvel Digital (CMD) para a aplicação Ponte Saúde.
 * Simula o fluxo oficial português de identificação do utente com 2º fator por SMS.
 */
@Composable
fun EcraLoginCMD(
    viewModel: LoginCMDViewModel? = null,
    perfilRepository: PerfilRepository? = null,
    sincronizador: AppContainer? = null,
    aoAutenticar: (String) -> Unit,
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val perfisDemonstracao = uiState?.perfisDisponiveis.takeUnless { it.isNullOrEmpty() } ?: perfilRepository?.todosOsPerfis.orEmpty()
    val teclado = LocalSoftwareKeyboardController.current
    val escopo = rememberCoroutineScope()

    var passo by remember { mutableIntStateOf(1) } // 1: Credenciais CMD, 2: Código SMS
    var modoId by remember { mutableStateOf(ModoIdentificacao.TELEMOVEL) }
    var telemovel by remember { mutableStateOf("912 345 678") }
    var nif by remember { mutableStateOf("214 875 920") }
    var pinCMD by remember { mutableStateOf("1234") }
    var mostrarPin by remember { mutableStateOf(false) }

    var codigoSMS by remember { mutableStateOf("") }
    var codigoSmsGerado by remember { mutableStateOf("582194") }

    var perfilEscolhidoId by remember { mutableStateOf("1") }
    var mensagemErro by remember { mutableStateOf<String?>(null) }
    var mostrarAjuda by remember { mutableStateOf(false) }

    var emProgresso by remember { mutableStateOf(false) }
    var textoProgresso by remember { mutableStateOf("A contactar Chave Móvel Digital...") }
    var aCriarPerfil by remember { mutableStateOf(false) }
    var aApagarPerfis by remember { mutableStateOf(false) }
    var mensagemSucesso by remember { mutableStateOf<String?>(null) }

    val estaACriar = uiState?.aCriarPerfil == true || aCriarPerfil
    val estaAApagar = uiState?.aApagarPerfis == true || aApagarPerfis
    val mensagemSucessoEfetiva = mensagemSucesso ?: uiState?.mensagemSucesso
    val mensagemErroEfetiva = mensagemErro ?: uiState?.mensagemErro

    // Sincronizar seleção se um perfil for apagado ou atualizado no Firestore em tempo real
    LaunchedEffect(perfisDemonstracao.size) {
        if (perfisDemonstracao.isNotEmpty() && perfisDemonstracao.none { it.id == perfilEscolhidoId }) {
            perfisDemonstracao.firstOrNull()?.let { primeiro ->
                perfilEscolhidoId = primeiro.id
                telemovel = primeiro.telemovel.replace("+351 ", "")
                nif = primeiro.nif
            }
        }
    }

    LaunchedEffect(uiState?.perfilSelecionadoId) {
        val id = uiState?.perfilSelecionadoId
        if (!id.isNullOrBlank()) {
            val perfilAlvo = perfisDemonstracao.firstOrNull { it.id == id }
            if (perfilAlvo != null) {
                perfilEscolhidoId = id
                telemovel = perfilAlvo.telemovel.replace("+351 ", "")
                nif = perfilAlvo.nif
            }
        }
    }

    LaunchedEffect(uiState?.mensagemSucesso) {
        val msg = uiState?.mensagemSucesso
        if (!msg.isNullOrBlank()) {
            mensagemSucesso = msg
        }
    }

    LaunchedEffect(uiState?.mensagemErro) {
        val msg = uiState?.mensagemErro
        if (!msg.isNullOrBlank()) {
            mensagemErro = msg
        }
    }

    // Procedimento de Login Simulado com Feedback de Carregamento e Firebase Auth Seguro
    fun executarAutenticacao(idPerfil: String) {
        teclado?.hide()
        mensagemErro = null
        emProgresso = true
        escopo.launch {
            textoProgresso = "A ligar a Autenticação.gov..."
            delay(300.milliseconds)
            val perfilAlvo = perfisDemonstracao.firstOrNull { it.id == idPerfil }
            val telPerfil = perfilAlvo?.telemovel?.trim().orEmpty()
            val ident: String = if (passo == 2 && modoId == ModoIdentificacao.TELEMOVEL) {
                telemovel
            } else if (passo == 2) {
                nif
            } else {
                telPerfil.ifEmpty { idPerfil }
            }
            textoProgresso = "A validar Chave Móvel Digital..."
            pt.ulsbe.pontesaude.data.remote.AutenticadorFirebaseCMD.autenticarComCMD(ident)
            delay(350.milliseconds)
            textoProgresso = "A carregar ficha de utente do SNS..."
            sincronizador?.sincronizarComFirestore()
            delay(300.milliseconds)
            emProgresso = false
            if (viewModel != null) {
                viewModel.selecionarPerfil(idPerfil)
                viewModel.concluirAutenticacao(aoAutenticar, ident)
            } else {
                aoAutenticar(idPerfil)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ==========================================
            // CABEÇALHO INSTITUCIONAL PREMIUM (ULSBE & PONTE SAÚDE)
            // ==========================================
            item {
                val paddingStatusBars = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val espacamentoTopo = if (paddingStatusBars > 0.dp) paddingStatusBars + 12.dp else 52.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF061B48),
                                    MarcaProfundo,
                                    MarcaTopo,
                                    MarcaMedio,
                                ),
                            ),
                        )
                        .padding(bottom = 28.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = espacamentoTopo),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Barra Superior Institucional
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Badge República Portuguesa
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .height(11.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                ) {
                                    Box(Modifier.width(5.dp).height(11.dp).background(Color(0xFF006633)))
                                    Box(Modifier.width(7.dp).height(11.dp).background(Color(0xFFD32F2F)))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "REPÚBLICA PORTUGUESA",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                )
                            }

                            // Tag SNS / ULSBE
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF4ADE80), CircleShape),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "ULSBE · SNS",
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.4.sp,
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Logótipo com Moldura Sofisticada e Sombra Suave
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_ponte_saude),
                                    contentDescription = "Logótipo Ponte Saúde",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Título Principal
                        Text(
                            text = "Ponte Saúde",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp,
                        )

                        Spacer(Modifier.height(4.dp))

                        // Nome Institucional Completo e Correto (Barcelos / Esposende)
                        Text(
                            text = "Unidade Local de Saúde de Barcelos / Esposende",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.95f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "Portal do Utente · Serviço Nacional de Saúde",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(14.dp))

                        // Badge de Canal Seguro Oficial
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Autenticação Oficial Certificada",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // CORPO DO FORMULÁRIO CHAVE MÓVEL DIGITAL
            // ==========================================
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-18).dp)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // CARTÃO PRINCIPAL DA CHAVE MÓVEL DIGITAL
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Superficie),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // Cabeçalho de Marca Governamental Portuguesa
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(MarcaMedio, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_cmd),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "CHAVE MÓVEL DIGITAL",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MarcaProfundo,
                                        )
                                        Text(
                                            text = "Autenticação.gov · República Portuguesa",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Atenuado,
                                        )
                                    }
                                }

                                // Indicador das cores de Portugal
                                Row(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                ) {
                                    Box(Modifier.width(8.dp).height(18.dp).background(Color(0xFF006633)))
                                    Box(Modifier.width(12.dp).height(18.dp).background(Color(0xFFD32F2F)))
                                }
                            }

                            HorizontalDivider(color = Superficie2)

                            // MENSAGEM DE ERRO (CASO EXISTA)
                            if (mensagemErro != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFEE2E2),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = mensagemErro ?: "",
                                        color = Color(0xFFB91C1C),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }

                            // ------------------------------------------
                            // PASSO 1: CREDENCIAIS (TELEMOVEL / NIF + PIN)
                            // ------------------------------------------
                            if (passo == 1) {
                                Text(
                                    text = "Identifique-se com a sua Chave Móvel Digital para aceder ao seu processo clínico na ULS de Barcelos / Esposende.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )

                                // Seletor do Tipo de Identificação
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Superficie2)
                                        .padding(4.dp),
                                ) {
                                    BotaoAlternadorModo(
                                        rotulo = "Telemóvel",
                                        selecionado = modoId == ModoIdentificacao.TELEMOVEL,
                                        modifier = Modifier.weight(1f),
                                        aoClicar = { modoId = ModoIdentificacao.TELEMOVEL },
                                    )
                                    BotaoAlternadorModo(
                                        rotulo = "NIF",
                                        selecionado = modoId == ModoIdentificacao.NIF,
                                        modifier = Modifier.weight(1f),
                                        aoClicar = { modoId = ModoIdentificacao.NIF },
                                    )
                                }

                                // Campo de Telemóvel ou NIF
                                if (modoId == ModoIdentificacao.TELEMOVEL) {
                                    OutlinedTextField(
                                        value = telemovel,
                                        onValueChange = { telemovel = it },
                                        label = { Text("Número de Telemóvel") },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Phone, contentDescription = null, tint = MarcaClaro)
                                        },
                                        prefix = { Text("+351 ", fontWeight = FontWeight.Bold, color = Atenuado) },
                                        placeholder = { Text("912 345 678") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = coresCamposTexto(),
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = nif,
                                        onValueChange = { nif = it },
                                        label = { Text("NIF (Número de Identificação Fiscal)") },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Badge, contentDescription = null, tint = MarcaClaro)
                                        },
                                        placeholder = { Text("214 875 920") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = coresCamposTexto(),
                                    )
                                }

                                // Campo do PIN da Chave Móvel Digital
                                OutlinedTextField(
                                    value = pinCMD,
                                    onValueChange = { if (it.length <= 8) pinCMD = it },
                                    label = { Text("PIN da Chave Móvel Digital") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = MarcaClaro)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { mostrarPin = !mostrarPin }) {
                                            Icon(
                                                imageVector = if (mostrarPin) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                contentDescription = if (mostrarPin) "Ocultar PIN" else "Mostrar PIN",
                                                tint = Atenuado,
                                            )
                                        }
                                    },
                                    placeholder = { Text("4 a 8 dígitos") },
                                    singleLine = true,
                                    visualTransformation = if (mostrarPin) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (validarPasso1(modoId, telemovel, nif, pinCMD) { mensagemErro = it }) {
                                            passo = 2
                                        }
                                    }),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = coresCamposTexto(),
                                )

                                // Botão de Avanço para o Código de Segurança (SMS)
                                Button(
                                    onClick = {
                                        if (validarPasso1(modoId, telemovel, nif, pinCMD) { mensagemErro = it }) {
                                            teclado?.hide()
                                            codigoSmsGerado = gerarCodigoSimulado()
                                            passo = 2
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "Pedir Código de Segurança por SMS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    )
                                }
                            }

                            // ------------------------------------------
                            // PASSO 2: CÓDIGO DE SEGURANÇA SMS (2FA)
                            // ------------------------------------------
                            if (passo == 2) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(
                                        text = "Validação do 2º Fator (SMS)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MarcaProfundo,
                                    )

                                    Text(
                                        text = "Foi enviado um código de segurança de 6 dígitos por SMS para (+351) $telemovel.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Atenuado,
                                    )

                                    // Notificação Simulada de SMS para Facilidade de Teste
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Marca.copy(alpha = 0.45f),
                                        border = BorderStroke(1.dp, MarcaClaro.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_sms_codigo),
                                                    contentDescription = null,
                                                    tint = MarcaClaro,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "SMS Simulado de Autenticacao.gov",
                                                    fontWeight = FontWeight.SemiBold,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MarcaProfundo,
                                                )
                                            }
                                            Text(
                                                text = "Código de segurança da Chave Móvel Digital: $codigoSmsGerado",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Button(
                                                onClick = { codigoSMS = codigoSmsGerado },
                                                colors = ButtonDefaults.buttonColors(containerColor = MarcaProfundo),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(36.dp),
                                            ) {
                                                Text("Preencher código ($codigoSmsGerado)", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    // Campo de Inserção do Código SMS
                                    OutlinedTextField(
                                        value = codigoSMS,
                                        onValueChange = { if (it.length <= 6) codigoSMS = it },
                                        label = { Text("Código de 6 dígitos") },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = MarcaClaro)
                                        },
                                        placeholder = { Text("000000") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            executarAutenticacao(perfilEscolhidoId)
                                        }),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = coresCamposTexto(),
                                    )

                                    // Botão de Confirmação e Entrada
                                    Button(
                                        onClick = {
                                            if (codigoSMS.isBlank()) {
                                                mensagemErro = "Por favor introduza o código de segurança ou clique em preencher."
                                            } else {
                                                executarAutenticacao(perfilEscolhidoId)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "Confirmar e Aceder à Aplicação",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                        )
                                    }

                                    // Botão de Retrocesso
                                    OutlinedButton(
                                        onClick = {
                                            passo = 1
                                            mensagemErro = null
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(16.dp),
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Alterar telemóvel ou PIN")
                                    }
                                }
                            }

                            // Secção Expansível Informativa sobre a Chave Móvel Digital
                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { mostrarAjuda = !mostrarAjuda }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.HelpOutline,
                                            contentDescription = null,
                                            tint = MarcaClaro,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "O que é a Chave Móvel Digital?",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MarcaMedio,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    Icon(
                                        imageVector = if (mostrarAjuda) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Atenuado,
                                    )
                                }

                                AnimatedVisibility(visible = mostrarAjuda) {
                                    Text(
                                        text = "A Chave Móvel Digital (CMD) é o método oficial do Estado Português para aceder com segurança a serviços públicos como o SNS, Segurança Social e Finanças. Ao autenticar-se, a aplicação Ponte Saúde recupera os seus dados clínicos, consultas e medicação de forma segura.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Atenuado,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // CARTÃO DE TESTE & DEMONSTRAÇÃO (MOCK SANDBOX)
                    // ==========================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Superficie2),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MarcaProfundo, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Science,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Ambiente de Testes / Demonstração",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MarcaProfundo,
                                        )
                                        Text(
                                            text = if (sincronizador?.estaASincronizarFirestore == true) "A sincronizar em tempo real..." else "Sincronizado em tempo real com o Firestore",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (sincronizador?.estaASincronizarFirestore == true) MarcaMedio else Atenuado,
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        escopo.launch {
                                            sincronizador?.sincronizarAutomatico(true)
                                            perfilRepository?.carregarPerfisLocais()
                                            viewModel?.recarregar()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    if (sincronizador?.estaASincronizarFirestore == true) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MarcaMedio,
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.Refresh,
                                            contentDescription = "Sincronizar com Firestore",
                                            tint = MarcaMedio,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Escolha um utente de demonstração para testar imediatamente o carregamento das informações:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            // Feedback visual destacado: Perfil a ser gerado
                            if (estaACriar) {
                                Surface(
                                    color = MarcaMedio.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, MarcaMedio.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MarcaMedio,
                                            strokeWidth = 2.5.dp,
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "A criar perfil clínico com IA...",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MarcaMedio,
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "A gerar dados médicos realistas e a guardar no Firebase. Aguarde um momento...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }

                            // Seletor de Perfis de Teste
                            if (perfisDemonstracao.isEmpty() && !estaACriar) {
                                Surface(
                                    color = Superficie,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "Ainda não existem perfis no Firestore. Clique no botão abaixo para gerar automaticamente um perfil de teste completo!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Atenuado,
                                        modifier = Modifier.padding(14.dp),
                                    )
                                }
                            } else {
                                perfisDemonstracao.forEach { perfil ->
                                    val selecionado = perfilEscolhidoId == perfil.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (selecionado) MarcaMedio.copy(alpha = 0.12f) else Superficie)
                                            .border(
                                                width = if (selecionado) 1.5.dp else 0.dp,
                                                color = if (selecionado) MarcaMedio else Color.Transparent,
                                                shape = RoundedCornerShape(14.dp),
                                            )
                                            .clickable {
                                                perfilEscolhidoId = perfil.id
                                                telemovel = perfil.telemovel.replace("+351 ", "")
                                                nif = perfil.nif
                                                pinCMD = "1234"
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(if (selecionado) MarcaMedio else MarcaProfundo, CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = perfil.iniciais,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = perfil.nome,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                                Text(
                                                    text = "Nº SNS: ${perfil.numeroUtente} · ${perfil.idadeFormatada} · NIF: ${perfil.nif}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Atenuado,
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (selecionado) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MarcaMedio,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                                Spacer(Modifier.width(6.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    escopo.launch {
                                                        if (viewModel != null) {
                                                            viewModel.apagarPerfil(perfil.id)
                                                        } else {
                                                            perfilRepository?.apagarPerfil(perfil.id)
                                                        }
                                                        val restante = perfisDemonstracao.firstOrNull { it.id != perfil.id }
                                                        perfilEscolhidoId = restante?.id ?: "1"
                                                        if (restante != null) {
                                                            telemovel = restante.telemovel.replace("+351 ", "")
                                                            nif = restante.nif
                                                        }
                                                        mensagemSucesso = "Perfil '${perfil.nome}' eliminado do Firebase Auth e Firestore."
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Apagar perfil",
                                                    tint = Color(0xFFDC2626).copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Botão para Criar Automaticamente Perfil Aleatório no Firebase (1 Clique)
                            Button(
                                onClick = {
                                    aCriarPerfil = true
                                    mensagemSucesso = null
                                    mensagemErro = null
                                    escopo.launch {
                                        try {
                                            val criado = if (viewModel != null) {
                                                viewModel.criarPerfilAleatorio()
                                            } else {
                                                perfilRepository?.criarPerfilAleatorio()
                                            }
                                            if (criado != null) {
                                                perfilEscolhidoId = criado.id
                                                telemovel = criado.telemovel.replace("+351 ", "")
                                                nif = criado.nif
                                                pinCMD = "1234"
                                                mensagemSucesso = "Perfil '${criado.nome}' gerado com sucesso e pronto a usar!"
                                            }
                                            perfilRepository?.carregarPerfisLocais()
                                            sincronizador?.perfil?.carregarPerfisLocais()
                                        } catch (e: Exception) {
                                            mensagemErro = "Erro ao criar perfil: ${e.localizedMessage}"
                                        } finally {
                                            aCriarPerfil = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
                                enabled = !estaACriar && !estaAApagar,
                            ) {
                                if (estaACriar) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("A gerar perfil clínico com IA...", fontWeight = FontWeight.SemiBold)
                                } else {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Gerar Perfil com IA no Firebase", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Botão Simples para Apagar Perfis Aleatórios (1 Clique, Sem Confirmação)
                            if (perfisDemonstracao.any { it.id !in setOf("1", "2") }) {
                                OutlinedButton(
                                    onClick = {
                                        aApagarPerfis = true
                                        mensagemSucesso = null
                                        mensagemErro = null
                                        escopo.launch {
                                            try {
                                                if (viewModel != null) {
                                                    viewModel.limparPerfisDemonstracao()
                                                } else {
                                                    perfilRepository?.apagarPerfisAleatorios()
                                                }
                                                perfilRepository?.carregarPerfisLocais()
                                                sincronizador?.perfil?.carregarPerfisLocais()
                                                val rest = perfisDemonstracao.firstOrNull { it.id in setOf("1", "2") } ?: perfisDemonstracao.firstOrNull()
                                                perfilEscolhidoId = rest?.id ?: "1"
                                                if (rest != null) {
                                                    telemovel = rest.telemovel.replace("+351 ", "")
                                                    nif = rest.nif
                                                }
                                                mensagemSucesso = "Perfis de teste eliminados com sucesso."
                                            } catch (e: Exception) {
                                                mensagemErro = "Erro ao apagar perfis: ${e.localizedMessage}"
                                            } finally {
                                                aApagarPerfis = false
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFDC2626),
                                    ),
                                    border = BorderStroke(1.2.dp, Color(0xFFFCA5A5)),
                                    enabled = !estaAApagar && !estaACriar,
                                ) {
                                    if (estaAApagar) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFFDC2626),
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("A apagar perfis...", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    } else {
                                        Icon(
                                            Icons.Rounded.DeleteSweep,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color(0xFFDC2626),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Apagar Perfis Aleatórios", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }

                            if (mensagemSucessoEfetiva != null && !estaACriar) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(mensagemSucessoEfetiva, color = Color(0xFF1B5E20), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            if (mensagemErroEfetiva != null && !estaACriar) {
                                Surface(
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(mensagemErroEfetiva, color = Color(0xFFB71C1C), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            // Botão de Entrada Rápida de Teste (1 Clique)
                            Button(
                                onClick = {
                                    executarAutenticacao(perfilEscolhidoId)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MarcaProfundo),
                                enabled = perfisDemonstracao.isNotEmpty(),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_cmd),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                val nomeAtual = perfisDemonstracao.find { it.id == perfilEscolhidoId }?.nomeCurto ?: perfisDemonstracao.firstOrNull()?.nomeCurto ?: "Utente"
                                Text("Entrar como $nomeAtual (Teste Rápido)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Nota de Rodapé Institucional
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Unidade Local de Saúde de Barcelos / Esposende, E.P.E. (ULSBE)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Atenuado,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Dados protegidos em conformidade com o RGPD e normas do SNS",
                            style = MaterialTheme.typography.bodySmall,
                            color = Atenuado.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // ==========================================
        // DIÁLOGO DE CARREGAMENTO / PROGRESSO
        // ==========================================
        if (emProgresso) {
            Dialog(onDismissRequest = {}) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(
                            color = MarcaMedio,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = textoProgresso,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = PrimeiroPlanoCompat,
                        )
                    }
                }
            }
        }
    }
}

// Cor de Primeiro Plano para compatibilidade
private val PrimeiroPlanoCompat = Color(0xFF0B1A3A)

@Composable
private fun BotaoAlternadorModo(
    rotulo: String,
    selecionado: Boolean,
    modifier: Modifier = Modifier,
    aoClicar: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selecionado) MarcaMedio else Color.Transparent)
            .clickable(onClick = aoClicar)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rotulo,
            color = if (selecionado) Color.White else Atenuado,
            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun coresCamposTexto() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MarcaMedio,
    unfocusedBorderColor = Superficie2,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
)

private fun validarPasso1(
    modo: ModoIdentificacao,
    telemovel: String,
    nif: String,
    pin: String,
    aoOcorrerErro: (String) -> Unit,
): Boolean {
    if (modo == ModoIdentificacao.TELEMOVEL && telemovel.trim().length < 9) {
        aoOcorrerErro("Introduza um número de telemóvel válido (ex: 912 345 678).")
        return false
    }
    if (modo == ModoIdentificacao.NIF && nif.trim().length < 9) {
        aoOcorrerErro("Introduza um NIF com 9 dígitos.")
        return false
    }
    if (pin.trim().length < 4) {
        aoOcorrerErro("O PIN da Chave Móvel Digital tem pelo menos 4 dígitos.")
        return false
    }
    return true
}

private fun gerarCodigoSimulado(): String {
    val codigos = listOf("582194", "391740", "824619", "472851", "619358")
    return codigos.random()
}

// ==========================================
// PREVIEWS COM COMPOSABLE NO ANDROID STUDIO
// ==========================================

@Preview(name = "Ecrã de Login CMD - Pré-visualização", showBackground = true)
@Composable
fun PreVisualizacaoEcraLoginCMD() {
    TemaPonteSaude(textoGrande = false) {
        EcraLoginCMD(
            perfilRepository = null,
            sincronizador = null,
            aoAutenticar = {},
        )
    }
}

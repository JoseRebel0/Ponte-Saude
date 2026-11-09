package pt.ulsbe.pontesaude.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import pt.ulsbe.pontesaude.data.room.BaseDadosApp
import pt.ulsbe.pontesaude.data.room.NotaEntity
import pt.ulsbe.pontesaude.data.room.deserializarListaString
import pt.ulsbe.pontesaude.data.room.deserializarVouchers
import pt.ulsbe.pontesaude.data.room.paraEntidade
import pt.ulsbe.pontesaude.data.room.paraModelo
import pt.ulsbe.pontesaude.data.room.serializarListaString
import pt.ulsbe.pontesaude.data.room.serializarVouchers
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.ModuloSaude
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.RecompensaSeguranca
import pt.ulsbe.pontesaude.model.VideoBibliotecaSaude
import pt.ulsbe.pontesaude.model.VoucherResgatado

/**
 * Repositório principal da aplicação com arquitetura Offline-First:
 * - Base de dados local Room Database para funcionamento 100% offline.
 * - Sincronização automática bidirecional com o Google Cloud Firestore quando existe ligação.
 * - Tratamento de primeira abertura sem Internet.
 */
class RepositorioApp(context: Context) {
    private val preferencias = context.getSharedPreferences("ponte_saude", Context.MODE_PRIVATE)
    val dbRoom: BaseDadosApp = BaseDadosApp.obterInstancia(context)
    val servicoFirestore = ServicoFirestore()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val listaMedicamentos = mutableStateMapOf<String, List<Medicamento>>()
    private val listaConsultas = mutableStateMapOf<String, List<Consulta>>()
    private val listaProdutos = mutableStateMapOf<String, List<String>>()
    private val listaNotas = mutableStateMapOf<String, List<Nota>>()

    var textoGrande by mutableStateOf(preferencias.getBoolean("big_text", false))
        private set
    var modoCuidador by mutableStateOf(preferencias.getBoolean("caregiver", false))
        private set
    var indicePerfilCuidado by mutableIntStateOf(preferencias.getInt("cared_profile", 0))
        private set
    var reduzirMovimento by mutableStateOf(preferencias.getBoolean("reduce_motion", false))
        private set
    var notificacoesAtivas by mutableStateOf(preferencias.getBoolean("notificacoes_ativas", true))
        private set
    var agendaEAlarmesAtivos by mutableStateOf(preferencias.getBoolean("agenda_alarmes_ativos", true))
        private set
    var autenticado by mutableStateOf(preferencias.getBoolean("cmd_autenticado", false))
        private set
    var utenteSessaoId by mutableStateOf(preferencias.getString("cmd_utente_id", "1") ?: "1")
        private set
    var timestampUltimaAtualizacao by mutableStateOf(preferencias.getLong("ts_ultima_atualizacao", System.currentTimeMillis()))
        private set
    var pontosSeguranca by mutableIntStateOf(preferencias.getInt("pontos_seguranca", 50))
        private set

    var erroPrimeiraConexao by mutableStateOf(false)
        private set
    var estaOnline by mutableStateOf(true)
        private set
    var estadoSincronizacaoFirestore by mutableStateOf(preferencias.getString("firestore_status", "Sincronização automática ativa") ?: "Sincronização automática ativa")
        private set
    var estaASincronizarFirestore by mutableStateOf(false)
        private set

    val modulosConcluidos = mutableStateListOf<String>()
    val modulosOuvidos = mutableStateListOf<String>()
    val videosAssistidos = mutableStateListOf<String>()
    val vouchersResgatados = mutableStateListOf<VoucherResgatado>()
    val pessoasCuidadas = mutableStateListOf<Perfil>()
    val todosOsPerfis = mutableStateListOf<Perfil>()

    val modulosSaude = mutableStateListOf<ModuloSaude>()
    val videosBiblioteca = mutableStateListOf<VideoBibliotecaSaude>()
    val catalogoRecompensas = mutableStateListOf<RecompensaSeguranca>()

    init {
        // Remover perfis associados antigos para começar do zero com controlo do utilizador
        if (!preferencias.getBoolean("perfis_associados_limpos_v3", false)) {
            preferencias.edit()
                .remove("cared_profiles_custom_json")
                .putBoolean("caregiver", false)
                .putInt("cared_profile", 0)
                .putBoolean("perfis_associados_limpos_v3", true)
                .apply()
            modoCuidador = false
            indicePerfilCuidado = 0
            scope.launch(Dispatchers.IO) {
                servicoFirestore.limparTodasAssociacoesFirestore()
            }
        }

        // 1. Carregar do Room Database e sincronizar com o Firestore automaticamente
        carregarTudoDoRoomESincronizar()

        // 2. Iniciar escuta em tempo real do Firestore para atualizar na hora
        iniciarEscutaTempoRealFirestore()
    }

    val perfilSessao: Perfil
        get() = todosOsPerfis.find { it.id == utenteSessaoId }
            ?: todosOsPerfis.firstOrNull()
            ?: Perfil(
                id = utenteSessaoId,
                nome = "Utente SNS",
                nomeCurto = "Utente",
                relacao = "Utente",
                iniciais = "UT",
                numeroUtente = "000 000 000",
                tipoSanguineo = "A+",
                alergias = "Sem alergias conhecidas",
            )

    val perfilAtivo: Perfil
        get() {
            val perfilBase = perfilSessao
            val perfilAlvo = if (!modoCuidador || pessoasCuidadas.isEmpty()) {
                perfilBase
            } else {
                pessoasCuidadas.getOrNull(indicePerfilCuidado.coerceIn(0, (pessoasCuidadas.size - 1).coerceAtLeast(0))) ?: perfilBase
            }
            return perfilAlvo.copy(
                medicamentos = medicamentos(perfilAlvo),
                consultas = consultas(perfilAlvo),
                notas = notas(perfilAlvo),
                outrosProdutos = produtos(perfilAlvo),
            )
        }

    fun atualizarListaPessoasCuidadas() {
        val meuPerfil = todosOsPerfis.find { it.id == utenteSessaoId }
        val idsCuidados = meuPerfil?.perfisCuidadosIds.orEmpty()
        pessoasCuidadas.clear()
        val encontradas = todosOsPerfis.filter { it.id in idsCuidados }
        pessoasCuidadas.addAll(encontradas)
        for (p in pessoasCuidadas) {
            if (!listaConsultas.containsKey(p.id) && p.consultas.isNotEmpty()) {
                listaConsultas[p.id] = p.consultas
            }
            if (!listaNotas.containsKey(p.id) && p.notas.isNotEmpty()) {
                listaNotas[p.id] = p.notas
            }
            if (!listaMedicamentos.containsKey(p.id) && p.medicamentos.isNotEmpty()) {
                listaMedicamentos[p.id] = p.medicamentos
            }
        }
        if (pessoasCuidadas.isEmpty()) {
            modoCuidador = false
            preferencias.edit().putBoolean("caregiver", false).apply()
        } else if (indicePerfilCuidado >= pessoasCuidadas.size) {
            definirPerfilCuidado(pessoasCuidadas.lastIndex)
        }
    }

    fun chavePerfil(chaveBase: String, idPerfil: String = perfilAtivo.id): String {
        return if (idPerfil == "1") chaveBase else "${chaveBase}_$idPerfil"
    }

    fun carregarDadosPerfil(idPerfil: String = perfilAtivo.id) {
        val chavePontos = chavePerfil("pontos_seguranca", idPerfil)
        val defaultPontos = when (idPerfil) {
            "2" -> 140
            "3" -> 110
            else -> 50
        }
        pontosSeguranca = preferencias.getInt(chavePontos, defaultPontos)

        val chaveModulos = chavePerfil("completed_modules", idPerfil)
        val defaultModulos = when (idPerfil) {
            "2" -> setOf("lavagem-maos", "medicacao-segura")
            "3" -> setOf("quedas", "sinais-alarme")
            else -> emptySet()
        }
        modulosConcluidos.clear()
        modulosConcluidos.addAll(preferencias.getStringSet(chaveModulos, defaultModulos).orEmpty())

        val chaveOuvidos = chavePerfil("listened_modules", idPerfil)
        modulosOuvidos.clear()
        modulosOuvidos.addAll(preferencias.getStringSet(chaveOuvidos, emptySet()).orEmpty())

        val chaveVideos = chavePerfil("watched_videos", idPerfil)
        videosAssistidos.clear()
        videosAssistidos.addAll(preferencias.getStringSet(chaveVideos, emptySet()).orEmpty())

        vouchersResgatados.clear()
        vouchersResgatados.addAll(lerVouchers(idPerfil))
    }

    fun iniciarSessaoCMD(idPerfil: String = "1") {
        utenteSessaoId = idPerfil
        autenticado = true
        preferencias.edit()
            .putBoolean("cmd_autenticado", true)
            .putString("cmd_utente_id", idPerfil)
            .apply()
        atualizarListaPessoasCuidadas()
        carregarDadosPerfil(perfilAtivo.id)
    }

    fun terminarSessaoCMD() {
        autenticado = false
        preferencias.edit()
            .putBoolean("cmd_autenticado", false)
            .apply()
    }

    fun atualizarTextoGrande(valor: Boolean) {
        textoGrande = valor
        preferencias.edit().putBoolean("big_text", valor).apply()
    }

    fun atualizarModoCuidador(valor: Boolean) {
        modoCuidador = valor
        preferencias.edit().putBoolean("caregiver", valor).apply()
        carregarDadosPerfil(perfilAtivo.id)
    }

    fun definirPerfilCuidado(indice: Int) {
        indicePerfilCuidado = indice
        preferencias.edit().putInt("cared_profile", indice).apply()
        carregarDadosPerfil(perfilAtivo.id)
    }

    fun atualizarReduzirMovimento(valor: Boolean) {
        reduzirMovimento = valor
        preferencias.edit().putBoolean("reduce_motion", valor).apply()
    }

    fun atualizarNotificacoesAtivas(valor: Boolean) {
        notificacoesAtivas = valor
        preferencias.edit().putBoolean("notificacoes_ativas", valor).apply()
    }

    fun atualizarAgendaEAlarmesAtivos(valor: Boolean) {
        agendaEAlarmesAtivos = valor
        preferencias.edit().putBoolean("agenda_alarmes_ativos", valor).apply()
    }

    fun alternarModulo(id: String) {
        if (id in modulosConcluidos) modulosConcluidos.remove(id) else modulosConcluidos.add(id)
        preferencias.edit().putStringSet(chavePerfil("completed_modules"), modulosConcluidos.toSet()).apply()
        salvarPerfilNoRoom(perfilAtivo)
        sincronizarRecompensasFirestore()
    }

    fun adicionarPontos(quantidade: Int) {
        pontosSeguranca = (pontosSeguranca + quantidade).coerceAtLeast(0)
        preferencias.edit().putInt(chavePerfil("pontos_seguranca"), pontosSeguranca).apply()
        salvarPerfilNoRoom(perfilAtivo)
        sincronizarRecompensasFirestore()
    }

    fun premiarOuvirModulo(idModulo: String): Int {
        if (idModulo !in modulosOuvidos) {
            modulosOuvidos.add(idModulo)
            preferencias.edit().putStringSet(chavePerfil("listened_modules"), modulosOuvidos.toSet()).apply()
            adicionarPontos(25)
            salvarPerfilNoRoom(perfilAtivo)
            sincronizarRecompensasFirestore()
            return 25
        }
        return 0
    }

    fun premiarVerVideo(idVideo: String): Int {
        if (idVideo !in videosAssistidos) {
            videosAssistidos.add(idVideo)
            preferencias.edit().putStringSet(chavePerfil("watched_videos"), videosAssistidos.toSet()).apply()
            adicionarPontos(30)
            salvarPerfilNoRoom(perfilAtivo)
            sincronizarRecompensasFirestore()
            return 30
        }
        return 0
    }

    fun alternarModuloComPontos(id: String): Int {
        val estavaConcluido = id in modulosConcluidos
        val chaveModulos = chavePerfil("completed_modules")
        val pontosGanhos = if (estavaConcluido) {
            modulosConcluidos.remove(id)
            0
        } else {
            modulosConcluidos.add(id)
            adicionarPontos(50)
            50
        }
        preferencias.edit().putStringSet(chaveModulos, modulosConcluidos.toSet()).apply()
        salvarPerfilNoRoom(perfilAtivo)
        sincronizarRecompensasFirestore()
        return pontosGanhos
    }

    fun resgatarRecompensa(recompensa: RecompensaSeguranca): VoucherResgatado? {
        if (pontosSeguranca < recompensa.pontos) return null
        adicionarPontos(-recompensa.pontos)
        val codigo = "ULSBE-" + (1000..9999).random() + "-" + recompensa.id.take(3).uppercase()
        val dataFormatada = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "PT")).format(java.util.Date())
        val voucher = VoucherResgatado(
            id = java.util.UUID.randomUUID().toString(),
            idRecompensa = recompensa.id,
            titulo = recompensa.titulo,
            codigo = codigo,
            dataResgate = dataFormatada,
            localLevantamento = recompensa.localLevantamento,
            pontosUsados = recompensa.pontos,
        )
        vouchersResgatados.add(0, voucher)
        escreverVouchers()
        salvarPerfilNoRoom(perfilAtivo)
        sincronizarRecompensasFirestore()
        return voucher
    }

    private fun sincronizarRecompensasFirestore(idPerfil: String = perfilAtivo.id) {
        if (!estaOnline) return
        val pts = pontosSeguranca
        val concl = modulosConcluidos.toList()
        val ouv = modulosOuvidos.toList()
        val vid = videosAssistidos.toList()
        val vouch = vouchersResgatados.toList()
        scope.launch {
            runCatching {
                servicoFirestore.atualizarDadosRecompensas(
                    idPerfil = idPerfil,
                    pontos = pts,
                    modulosConcluidos = concl,
                    modulosOuvidos = ouv,
                    videosAssistidos = vid,
                    vouchers = vouch,
                )
            }
        }
    }

    fun registarAtualizacao() {
        val agora = System.currentTimeMillis()
        timestampUltimaAtualizacao = agora
        preferencias.edit().putLong("ts_ultima_atualizacao", agora).apply()
    }

    fun formatarUltimaAtualizacao(): String {
        return formatarMomentoAtualizacao(timestampUltimaAtualizacao)
    }

    // ==========================================
    // MEDICAMENTOS (Room + Firestore)
    // ==========================================

    fun medicamentos(perfil: Perfil): List<Medicamento> = listaMedicamentos[perfil.id].orEmpty()

    fun definirMedicamentoAtivo(perfil: Perfil, id: Long, ativo: Boolean) {
        val lista = medicamentos(perfil).map { if (it.id == id) it.copy(ativo = ativo) else it }
        atualizarMedicamentos(perfil, lista)
        val med = lista.find { it.id == id }
        if (med != null) {
            scope.launch(Dispatchers.IO) {
                dbRoom.medicamentoDao().inserirOuAtualizar(med.paraEntidade(perfil.id, !estaOnline))
            }
            if (estaOnline) {
                scope.launch { runCatching { servicoFirestore.guardarMedicamento(perfil.id, med) } }
            }
        }
    }

    fun adicionarMedicamento(perfil: Perfil, medicamento: Medicamento) {
        atualizarMedicamentos(perfil, medicamentos(perfil) + medicamento)
        scope.launch(Dispatchers.IO) {
            dbRoom.medicamentoDao().inserirOuAtualizar(medicamento.paraEntidade(perfil.id, !estaOnline))
        }
        if (estaOnline) {
            scope.launch { runCatching { servicoFirestore.guardarMedicamento(perfil.id, medicamento) } }
        }
    }

    fun atualizarMedicamento(perfil: Perfil, medicamento: Medicamento) {
        atualizarMedicamentos(perfil, medicamentos(perfil).map { if (it.id == medicamento.id) medicamento else it })
        scope.launch(Dispatchers.IO) {
            dbRoom.medicamentoDao().inserirOuAtualizar(medicamento.paraEntidade(perfil.id, !estaOnline))
        }
        if (estaOnline) {
            scope.launch { runCatching { servicoFirestore.guardarMedicamento(perfil.id, medicamento) } }
        }
    }

    fun apagarMedicamento(perfil: Perfil, id: Long) {
        atualizarMedicamentos(perfil, medicamentos(perfil).filterNot { it.id == id })
        scope.launch(Dispatchers.IO) {
            dbRoom.medicamentoDao().apagarPorId(id)
        }
        if (estaOnline) {
            scope.launch { runCatching { servicoFirestore.apagarMedicamento(perfil.id, id) } }
        }
    }

    // ==========================================
    // CONSULTAS & CHECKLISTS (Room + Firestore)
    // ==========================================

    fun consultas(perfil: Perfil): List<Consulta> = listaConsultas[perfil.id] ?: perfil.consultas

    fun adicionarConsulta(perfil: Perfil, consulta: Consulta) {
        val atualizadas = consultas(perfil) + consulta
        atualizarConsultas(perfil, atualizadas)
        scope.launch(Dispatchers.IO) {
            dbRoom.consultaDao().inserirOuAtualizar(consulta.paraEntidade(perfil.id, pendente = !estaOnline))
        }
        if (estaOnline) {
            scope.launch { runCatching { servicoFirestore.guardarConsulta(perfil.id, consulta) } }
        }
    }

    fun atualizarConsultas(perfil: Perfil, atualizadas: List<Consulta>) {
        listaConsultas[perfil.id] = atualizadas
        atualizarConsultasLocal(perfil, atualizadas)
    }

    fun obterChecklistSalva(chave: String): Set<String> {
        return preferencias.getStringSet("chk_$chave", emptySet()).orEmpty()
    }

    fun salvarChecklist(chave: String, marcados: Set<String>) {
        preferencias.edit().putStringSet("chk_$chave", marcados).apply()
        if (chave.startsWith("consulta_")) {
            val idConsulta = chave.removePrefix("consulta_").toIntOrNull()
            if (idConsulta != null) {
                scope.launch(Dispatchers.IO) {
                    val consEnt = dbRoom.consultaDao().obterPorPerfil(perfilAtivo.id).find { it.id == idConsulta }
                    if (consEnt != null) {
                        consEnt.checklistMarcadosJson = marcados.toList().serializarListaString()
                        consEnt.pendenteSincronizacao = !estaOnline
                        dbRoom.consultaDao().inserirOuAtualizar(consEnt)
                    }
                }
                if (estaOnline) {
                    scope.launch {
                        runCatching {
                            servicoFirestore.atualizarChecklistConsulta(perfilAtivo.id, idConsulta, marcados.toList())
                        }
                    }
                }
            }
        }
    }

    fun obterItensCustomChecklist(chave: String): List<String> {
        val str = preferencias.getString("chk_custom_$chave", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(str)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun salvarItensCustomChecklist(chave: String, itens: List<String>) {
        salvarItensCustomChecklistLocal(chave, itens)
        if (chave.startsWith("consulta_")) {
            val idConsulta = chave.removePrefix("consulta_").toIntOrNull()
            if (idConsulta != null) {
                scope.launch(Dispatchers.IO) {
                    val consEnt = dbRoom.consultaDao().obterPorPerfil(perfilAtivo.id).find { it.id == idConsulta }
                    if (consEnt != null) {
                        consEnt.checklistCustomJson = itens.serializarListaString()
                        consEnt.pendenteSincronizacao = !estaOnline
                        dbRoom.consultaDao().inserirOuAtualizar(consEnt)
                    }
                }
                if (estaOnline) {
                    scope.launch {
                        runCatching {
                            servicoFirestore.atualizarChecklistCustomConsulta(perfilAtivo.id, idConsulta, itens)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // PRODUTOS & NOTAS (Room + Firestore)
    // ==========================================

    fun produtos(perfil: Perfil): List<String> = listaProdutos[perfil.id].orEmpty()

    fun adicionarProduto(perfil: Perfil, produto: String) {
        val limpo = produto.trim()
        if (limpo.isBlank() || limpo in produtos(perfil)) return
        val atualizados = produtos(perfil) + limpo
        listaProdutos[perfil.id] = atualizados
        escreverTextos("products_${perfil.id}", atualizados)
        salvarPerfilNoRoom(perfil)
        if (estaOnline) {
            scope.launch { runCatching { servicoFirestore.atualizarProdutos(perfil.id, atualizados) } }
        }
    }

    fun apagarProduto(perfil: Perfil, produto: String) {
        val atualizados = produtos(perfil).filterNot { it == produto }
        listaProdutos[perfil.id] = atualizados
        escreverTextos("products_${perfil.id}", atualizados)
        salvarPerfilNoRoom(perfil)
        if (estaOnline) {
            scope.launch { runCatching { servicoFirestore.atualizarProdutos(perfil.id, atualizados) } }
        }
    }

    fun notas(perfil: Perfil): List<Nota> = listaNotas[perfil.id] ?: perfil.notas

    fun adicionarNota(perfil: Perfil, nota: Nota) {
        val atualizadas = listOf(nota) + notas(perfil)
        guardarNotas(perfil, atualizadas)
        scope.launch(Dispatchers.IO) {
            dbRoom.notaDao().inserir(NotaEntity(0, perfil.id, nota.texto, nota.autor, !estaOnline))
        }
        if (estaOnline) {
            scope.launch { runCatching { servicoFirestore.atualizarNotas(perfil.id, atualizadas) } }
        }
    }

    fun apagarNota(perfil: Perfil, indice: Int) {
        val atuais = notas(perfil).toMutableList()
        if (indice in atuais.indices) {
            atuais.removeAt(indice)
            guardarNotas(perfil, atuais)
            scope.launch(Dispatchers.IO) {
                dbRoom.notaDao().apagarPorPerfil(perfil.id)
                atuais.forEach { dbRoom.notaDao().inserir(NotaEntity(0, perfil.id, it.texto, it.autor, !estaOnline)) }
            }
            if (estaOnline) {
                scope.launch { runCatching { servicoFirestore.atualizarNotas(perfil.id, atuais) } }
            }
        }
    }

    // ==========================================
    // PESSOAS CUIDADAS
    // ==========================================

    fun adicionarPessoaCuidada(
        nome: String,
        relacao: String = "Utente",
        telemovel: String = "",
        nif: String = "",
        numeroUtente: String = "",
        tipoSanguineo: String = "A+",
        alergias: String = "Sem alergias conhecidas",
    ): Perfil {
        val todosIds = todosOsPerfis.mapNotNull { it.id.toLongOrNull() }
        val proximoId = ((todosIds.maxOrNull() ?: 0L) + 1).toString()
        val nomeTrim = nome.trim()
        val partes = nomeTrim.split(" ").filter { it.isNotBlank() }
        val nomeCurto = if (partes.size > 1) "${partes.first()} ${partes.last()}" else nomeTrim
        val iniciais = partes.mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifBlank { "CU" }

        val novoPerfil = Perfil(
            id = proximoId,
            nome = nomeTrim,
            nomeCurto = nomeCurto,
            relacao = "Utente",
            iniciais = iniciais,
            tipoSanguineo = tipoSanguineo,
            alergias = alergias,
            consultas = listOf(
                Consulta(1, "18", "Nov", "Medicina Geral e Familiar", "ULSBE · Centro de Saúde", "Consulta"),
                Consulta(2, "02", "Dez", "Análises de Rotina", "ULSBE · Hospital", "Exame", true),
            ),
            medicamentos = listOf(
                Medicamento(System.currentTimeMillis(), "Paracetamol 1000 mg", "Dores / Febre", "paracetamol", "08:00", 0xFF3E63DD, true),
                Medicamento(System.currentTimeMillis() + 10, "Vitamina B12", "Suplemento", "cianocobalamina", "12:00", 0xFF059669, true),
            ),
            outrosProdutos = emptyList(),
            notas = listOf(Nota("Processo clínico vinculado com sucesso por SMS.", "Cuidador")),
            nif = nif.trim().ifBlank { "999 ${proximoId.padStart(6, '0')}" },
            numeroUtente = numeroUtente.trim().ifBlank { "123 ${proximoId.padStart(6, '0')}" },
            cartaoCidadao = "00000000 0 ZZ0",
            dataNascimento = "01/01/1950",
            telemovel = telemovel.trim(),
            email = "cuidado@ulsbe.min-saude.pt",
            perfisCuidadosIds = emptyList(),
            cuidadoresIds = listOf(utenteSessaoId),
        )

        todosOsPerfis.add(novoPerfil)
        listaMedicamentos[proximoId] = novoPerfil.medicamentos
        listaConsultas[proximoId] = novoPerfil.consultas
        listaProdutos[proximoId] = novoPerfil.outrosProdutos
        listaNotas[proximoId] = novoPerfil.notas

        salvarPerfilNoRoom(novoPerfil)
        if (estaOnline) {
            scope.launch { runCatching { servicoFirestore.salvarPerfilCompleto(novoPerfil) } }
        }

        associarPerfilCuidado(novoPerfil.id)
        return novoPerfil
    }

    /**
     * Associa um utente existente ou recém-criado aos cuidados do perfil da sessão atual.
     * Guarda a relação bidirecional (perfisCuidadosIds no cuidador, cuidadoresIds no utente)
     * localmente e no Firestore.
     */
    fun associarPerfilCuidado(idCuidado: String) {
        if (idCuidado == utenteSessaoId) return
        val meuPerfil = todosOsPerfis.find { it.id == utenteSessaoId } ?: return
        val outroPerfil = todosOsPerfis.find { it.id == idCuidado } ?: return

        val novosCuidados = (meuPerfil.perfisCuidadosIds + idCuidado).distinct()
        val novosCuidadores = (outroPerfil.cuidadoresIds + utenteSessaoId).distinct()

        val meuAtualizado = meuPerfil.copy(perfisCuidadosIds = novosCuidados)
        val outroAtualizado = outroPerfil.copy(cuidadoresIds = novosCuidadores)

        val idxMeu = todosOsPerfis.indexOfFirst { it.id == utenteSessaoId }
        if (idxMeu >= 0) todosOsPerfis[idxMeu] = meuAtualizado
        val idxOutro = todosOsPerfis.indexOfFirst { it.id == idCuidado }
        if (idxOutro >= 0) todosOsPerfis[idxOutro] = outroAtualizado

        salvarPerfilNoRoom(meuAtualizado)
        salvarPerfilNoRoom(outroAtualizado)

        atualizarListaPessoasCuidadas()
        val novoIdx = pessoasCuidadas.indexOfFirst { it.id == idCuidado }
        if (novoIdx >= 0) {
            definirPerfilCuidado(novoIdx)
        }
        atualizarModoCuidador(true)

        if (estaOnline) {
            scope.launch {
                servicoFirestore.associarPerfilCuidado(utenteSessaoId, idCuidado)
            }
        }
    }

    /**
     * Remove a associação de um utente cuidado do perfil da sessão.
     */
    fun desassociarPerfilCuidado(idCuidado: String) {
        val meuPerfil = todosOsPerfis.find { it.id == utenteSessaoId } ?: return
        val outroPerfil = todosOsPerfis.find { it.id == idCuidado }

        val novosCuidados = meuPerfil.perfisCuidadosIds.filter { it != idCuidado }
        val meuAtualizado = meuPerfil.copy(perfisCuidadosIds = novosCuidados)

        val idxMeu = todosOsPerfis.indexOfFirst { it.id == utenteSessaoId }
        if (idxMeu >= 0) todosOsPerfis[idxMeu] = meuAtualizado
        salvarPerfilNoRoom(meuAtualizado)

        if (outroPerfil != null) {
            val novosCuidadores = outroPerfil.cuidadoresIds.filter { it != utenteSessaoId }
            val outroAtualizado = outroPerfil.copy(cuidadoresIds = novosCuidadores)
            val idxOutro = todosOsPerfis.indexOfFirst { it.id == idCuidado }
            if (idxOutro >= 0) todosOsPerfis[idxOutro] = outroAtualizado
            salvarPerfilNoRoom(outroAtualizado)
        }

        atualizarListaPessoasCuidadas()

        if (estaOnline) {
            scope.launch {
                servicoFirestore.desassociarPerfilCuidado(utenteSessaoId, idCuidado)
            }
        }
    }

    fun removerPessoaCuidada(id: String) {
        desassociarPerfilCuidado(id)
    }

    // ==========================================
    // SINCRONIZAÇÃO AUTOMÁTICA OFFLINE-FIRST
    // ==========================================

    private fun carregarTudoDoRoomESincronizar() {
        scope.launch(Dispatchers.IO) {
            val modulosDb = dbRoom.moduloDao().obterTodos().map { it.paraModelo() }
            val videosDb = dbRoom.videoDao().obterTodos().map { it.paraModelo() }
            val recompsDb = dbRoom.recompensaDao().obterTodas().map { it.paraModelo() }
            if (modulosDb.isNotEmpty() || videosDb.isNotEmpty() || recompsDb.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    if (modulosDb.isNotEmpty()) {
                        modulosSaude.clear()
                        modulosSaude.addAll(modulosDb)
                    }
                    if (videosDb.isNotEmpty()) {
                        videosBiblioteca.clear()
                        videosBiblioteca.addAll(videosDb)
                    }
                    if (recompsDb.isNotEmpty()) {
                        catalogoRecompensas.clear()
                        catalogoRecompensas.addAll(recompsDb)
                    }
                }
            }

            val perfisDb = dbRoom.perfilDao().obterTodos()
            if (perfisDb.isNotEmpty()) {
                val medsMap = mutableMapOf<String, List<Medicamento>>()
                val consMap = mutableMapOf<String, List<Consulta>>()
                val notasMap = mutableMapOf<String, List<Nota>>()
                val prodsMap = mutableMapOf<String, List<String>>()
                val modelosCarregados = mutableListOf<Perfil>()

                for (ent in perfisDb) {
                    val meds = dbRoom.medicamentoDao().obterPorPerfil(ent.id).map { it.paraModelo() }
                    medsMap[ent.id] = meds

                    val cons = dbRoom.consultaDao().obterPorPerfil(ent.id).map { it.paraModelo() }
                    consMap[ent.id] = cons

                    val notasDb = dbRoom.notaDao().obterPorPerfil(ent.id).map { it.paraModelo() }
                    notasMap[ent.id] = notasDb

                    val prods = ent.outrosProdutosJson.deserializarListaString()
                    prodsMap[ent.id] = prods

                    modelosCarregados.add(ent.paraModelo(cons, meds, notasDb))
                }

                withContext(Dispatchers.Main) {
                    listaMedicamentos.putAll(medsMap)
                    listaConsultas.putAll(consMap)
                    listaNotas.putAll(notasMap)
                    listaProdutos.putAll(prodsMap)
                    todosOsPerfis.clear()
                    todosOsPerfis.addAll(modelosCarregados)
                    atualizarListaPessoasCuidadas()
                    carregarDadosPerfil(perfilAtivo.id)
                }
            }

            // Sincronizar automaticamente com o Firestore
            sincronizarAutomatico()
        }
    }

    fun sincronizarAutomatico(online: Boolean = estaOnline) {
        estaOnline = online
        val primeiraVezConcluida = preferencias.getBoolean("primeira_vez_concluida", false)

        scope.launch(Dispatchers.IO) {
            val totalNoRoom = dbRoom.perfilDao().contar()

            if (!online) {
                if (!primeiraVezConcluida && totalNoRoom == 0) {
                    withContext(Dispatchers.Main) {
                        erroPrimeiraConexao = true
                        estadoSincronizacaoFirestore = "Sem ligação à Internet na primeira abertura"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        erroPrimeiraConexao = false
                        estadoSincronizacaoFirestore = "Modo offline ativo (Room Database)"
                    }
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                estaASincronizarFirestore = true
                estadoSincronizacaoFirestore = "A sincronizar dados da nuvem..."
            }

            // Sincronizar Módulos, Vídeos e Recompensas diretamente do Cloud Firestore
            runCatching {
                val modulosRemotos = servicoFirestore.obterModulosSaude()
                val videosRemotos = servicoFirestore.obterVideosBiblioteca()
                val recompsRemotas = servicoFirestore.obterCatalogoRecompensas()

                dbRoom.moduloDao().inserirVarios(modulosRemotos.map { it.paraEntidade() })
                dbRoom.videoDao().inserirVarios(videosRemotos.map { it.paraEntidade() })
                dbRoom.recompensaDao().inserirVarias(recompsRemotas.map { it.paraEntidade() })

                withContext(Dispatchers.Main) {
                    modulosSaude.clear()
                    modulosSaude.addAll(modulosRemotos)
                    videosBiblioteca.clear()
                    videosBiblioteca.addAll(videosRemotos)
                    catalogoRecompensas.clear()
                    catalogoRecompensas.addAll(recompsRemotas)
                }
            }

            val resultado = servicoFirestore.carregarTodosOsPerfis()
            if (resultado.isSuccess) {
                val remotos = resultado.getOrNull().orEmpty()
                val idsRemotos = remotos.map { it.perfil.id }.toSet()

                // 1. ELIMINAR DO ROOM E DA MEMÓRIA OS PERFIS QUE JÁ NÃO EXISTEM NO FIRESTORE
                val perfisNoRoom = dbRoom.perfilDao().obterTodos()
                for (pLocal in perfisNoRoom) {
                    if (pLocal.id !in idsRemotos) {
                        dbRoom.perfilDao().apagar(pLocal.id)
                        dbRoom.medicamentoDao().apagarPorPerfil(pLocal.id)
                        dbRoom.consultaDao().apagarPorPerfil(pLocal.id)
                        dbRoom.notaDao().apagarPorPerfil(pLocal.id)
                        withContext(Dispatchers.Main) {
                            listaMedicamentos.remove(pLocal.id)
                            listaConsultas.remove(pLocal.id)
                            listaNotas.remove(pLocal.id)
                            listaProdutos.remove(pLocal.id)
                        }
                    }
                }

                if (remotos.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        todosOsPerfis.clear()
                        pessoasCuidadas.clear()
                        estadoSincronizacaoFirestore = "Nenhum utente encontrado no Firestore"
                    }
                } else {
                    // Firestore tem dados: aplicar na memória e na Room
                    for (remoto in remotos) {
                        val p = remoto.perfil
                        withContext(Dispatchers.Main) {
                            listaMedicamentos[p.id] = p.medicamentos
                            atualizarMedicamentosLocal(p, p.medicamentos)

                            listaConsultas[p.id] = p.consultas
                            atualizarConsultasLocal(p, p.consultas)

                            listaNotas[p.id] = p.notas
                            guardarNotasLocal(p, p.notas)

                            listaProdutos[p.id] = p.outrosProdutos
                            escreverTextos("products_${p.id}", p.outrosProdutos)

                            for ((consId, pair) in remoto.checklistsConsultas) {
                                val chave = "consulta_$consId"
                                preferencias.edit().putStringSet("chk_$chave", pair.first.toSet()).apply()
                                salvarItensCustomChecklistLocal(chave, pair.second)
                            }

                            preferencias.edit()
                                .putInt(chavePerfil("pontos_seguranca", p.id), remoto.pontosSeguranca)
                                .putStringSet(chavePerfil("completed_modules", p.id), remoto.modulosConcluidos.toSet())
                                .putStringSet(chavePerfil("listened_modules", p.id), remoto.modulosOuvidos.toSet())
                                .putStringSet(chavePerfil("watched_videos", p.id), remoto.videosAssistidos.toSet())
                                .apply()
                            escreverVouchersLocal(p.id, remoto.vouchersResgatados)
                        }

                        // Persistir no Room
                        val pEnt = p.paraEntidade(
                            pontos = remoto.pontosSeguranca,
                            modulosConcluidos = remoto.modulosConcluidos,
                            modulosOuvidos = remoto.modulosOuvidos,
                            videos = remoto.videosAssistidos,
                            vouchers = remoto.vouchersResgatados,
                            pendente = false,
                        )
                        dbRoom.perfilDao().inserirOuAtualizar(pEnt)

                        val medsEnt = p.medicamentos.map { it.paraEntidade(p.id, false) }
                        dbRoom.medicamentoDao().inserirVarios(medsEnt)

                        val consEnt = p.consultas.map { c ->
                            val pair = remoto.checklistsConsultas[c.id]
                            c.paraEntidade(p.id, pair?.first.orEmpty(), pair?.second.orEmpty(), false)
                        }
                        dbRoom.consultaDao().inserirVarios(consEnt)

                        dbRoom.notaDao().apagarPorPerfil(p.id)
                        p.notas.forEach { n ->
                            dbRoom.notaDao().inserir(NotaEntity(0, p.id, n.texto, n.autor, false))
                        }
                    }

                    val listaPerfisRemotos = remotos.map { it.perfil }
                    withContext(Dispatchers.Main) {
                        todosOsPerfis.clear()
                        todosOsPerfis.addAll(listaPerfisRemotos)
                        atualizarListaPessoasCuidadas()
                    }
                }

                // Sincronizar itens pendentes offline para o Firestore
                val medsPendentes = dbRoom.medicamentoDao().obterPendentes()
                for (m in medsPendentes) {
                    servicoFirestore.guardarMedicamento(m.idPerfil, m.paraModelo())
                    m.pendenteSincronizacao = false
                    dbRoom.medicamentoDao().inserirOuAtualizar(m)
                }

                val consPendentes = dbRoom.consultaDao().obterPendentes()
                for (c in consPendentes) {
                    servicoFirestore.guardarConsulta(c.idPerfil, c.paraModelo())
                    servicoFirestore.atualizarChecklistConsulta(c.idPerfil, c.id, c.checklistMarcadosJson.deserializarListaString())
                    servicoFirestore.atualizarChecklistCustomConsulta(c.idPerfil, c.id, c.checklistCustomJson.deserializarListaString())
                    c.pendenteSincronizacao = false
                    dbRoom.consultaDao().inserirOuAtualizar(c)
                }

                withContext(Dispatchers.Main) {
                    erroPrimeiraConexao = false
                    preferencias.edit().putBoolean("primeira_vez_concluida", true).apply()
                    carregarDadosPerfil(perfilAtivo.id)
                    registarAtualizacao()
                    estaASincronizarFirestore = false
                    estadoSincronizacaoFirestore = "Sincronizado automaticamente com o Firestore"
                }
            } else {
                withContext(Dispatchers.Main) {
                    estaASincronizarFirestore = false
                    estadoSincronizacaoFirestore = "Modo offline ativo (Room Database)"
                }
            }
        }
    }

    fun tentarNovamentePrimeiraConexao(temInternet: Boolean) {
        estaOnline = temInternet
        if (temInternet) {
            erroPrimeiraConexao = false
            sincronizarAutomatico(true)
        }
    }

    suspend fun sincronizarComFirestore() {
        sincronizarAutomatico(online = true)
    }

    /**
     * Inicia a escuta em tempo real da coleção de perfis no Google Cloud Firestore.
     * Quando um perfil é apagado ou criado no Firebase Console, reflete imediatamente na app na hora.
     */
    fun iniciarEscutaTempoRealFirestore() {
        servicoFirestore.iniciarEscutaPerfisEmTempoReal {
            scope.launch(Dispatchers.IO) {
                sincronizarAutomatico(online = true)
            }
        }
    }

    suspend fun criarPerfilAleatorio(): Perfil = withContext(Dispatchers.IO) {
        val nomesPossiveis = listOf(
            Pair("Manuel António Ferreira", "Manuel"),
            Pair("Ana Sofia Costa", "Ana"),
            Pair("Carlos Alberto Martins", "Carlos"),
            Pair("Teresa Maria Pinho", "Teresa"),
            Pair("João Pedro Ribeiro", "João"),
            Pair("Maria da Conceição Sousa", "Maria"),
            Pair("Rui Jorge Barbosa", "Rui"),
            Pair("Helena Isabel Santos", "Helena"),
            Pair("Fernando Miguel Silva", "Fernando"),
            Pair("Rosa Maria Oliveira", "Rosa"),
        )
        val tiposSangue = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+")
        val alergiasPossiveis = listOf(
            "Sem alergias conhecidas",
            "Penicilina",
            "Aspirina / AINEs",
            "Iodo",
            "Sem alergias conhecidas",
        )

        val random = java.util.Random()
        val nomePar = nomesPossiveis[random.nextInt(nomesPossiveis.size)]
        val relacao = "Utente"
        val tipoSangue = tiposSangue[random.nextInt(tiposSangue.size)]
        val alergias = alergiasPossiveis[random.nextInt(alergiasPossiveis.size)]

        val todosIds = todosOsPerfis.mapNotNull { it.id.toLongOrNull() }
        val proximoId = ((todosIds.maxOrNull() ?: 0L) + 1).toString()

        val anoNasc = 1940 + random.nextInt(45)
        val mesNasc = (1 + random.nextInt(12)).toString().padStart(2, '0')
        val diaNasc = (1 + random.nextInt(28)).toString().padStart(2, '0')
        val dataNasc = "$diaNasc/$mesNasc/$anoNasc"

        val numUtente = "${100 + random.nextInt(900)} ${100 + random.nextInt(900)} ${100 + random.nextInt(900)}"
        val nif = "${200 + random.nextInt(99)} ${100 + random.nextInt(900)} ${100 + random.nextInt(900)}"
        val tel = "+351 91${random.nextInt(10)} ${100 + random.nextInt(900)} ${100 + random.nextInt(900)}"

        val agora = System.currentTimeMillis()
        val medicamentosExemplo = listOf(
            Medicamento(agora, "Paracetamol 1000 mg", "Dores / Febre", "paracetamol", "08:00", 0xFF3E63DD, true),
            Medicamento(agora + 10, "Omeprazol 20 mg", "Proteção do estômago", "omeprazol", "08:30", 0xFFE5484D, true),
            Medicamento(agora + 20, "Atorvastatina 20 mg", "Controlo do colesterol", "atorvastatina", "21:00", 0xFFF5D90A, true),
        )

        val consultasExemplo = listOf(
            Consulta(1, "18", "Nov", "Medicina Geral e Familiar", "ULSBE · Centro de Saúde", "Consulta"),
            Consulta(2, "04", "Dez", "Análises Clínicas de Rotina", "ULSBE · Hospital", "Exame", true),
        )

        val notasExemplo = listOf(
            Nota("Processo clínico criado automaticamente no ambiente de testes da ULSBE.", "Sistema ULSBE"),
            Nota("Confirmar próxima toma e esclarecer dúvidas com o médico assistente.", nomePar.second),
        )

        val novo = Perfil(
            id = proximoId,
            nome = nomePar.first,
            nomeCurto = nomePar.second,
            relacao = relacao,
            iniciais = nomePar.first.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifBlank { "UT" },
            tipoSanguineo = tipoSangue,
            alergias = alergias,
            consultas = consultasExemplo,
            medicamentos = medicamentosExemplo,
            outrosProdutos = listOf("Suplemento Vitamina C"),
            notas = notasExemplo,
            nif = nif,
            numeroUtente = numUtente,
            cartaoCidadao = "00000000 0 ZZ0",
            dataNascimento = dataNasc,
            telemovel = tel,
            email = "${nomePar.second.lowercase()}@exemplo.pt",
        )

        // 1. Gravar no Firestore com subcoleções completas
        servicoFirestore.salvarPerfilCompleto(
            perfil = novo,
            pontos = 50,
            modulosConcluidos = emptyList(),
            modulosOuvidos = emptyList(),
            videosAssistidos = emptyList(),
            vouchers = emptyList(),
        )

        // 2. Gravar no Room
        val pEnt = novo.paraEntidade(50, emptyList(), emptyList(), emptyList(), emptyList(), false)
        dbRoom.perfilDao().inserirOuAtualizar(pEnt)
        val medsEnt = medicamentosExemplo.map { it.paraEntidade(novo.id, false) }
        dbRoom.medicamentoDao().inserirVarios(medsEnt)
        val consEnt = consultasExemplo.map { it.paraEntidade(novo.id, emptyList(), emptyList(), false) }
        dbRoom.consultaDao().inserirVarios(consEnt)
        notasExemplo.forEach { n ->
            dbRoom.notaDao().inserir(NotaEntity(0, novo.id, n.texto, n.autor, false))
        }

        withContext(Dispatchers.Main) {
            todosOsPerfis.add(novo)
            listaMedicamentos[novo.id] = medicamentosExemplo
            listaConsultas[novo.id] = consultasExemplo
            listaNotas[novo.id] = notasExemplo
            listaProdutos[novo.id] = listOf("Suplemento Vitamina C")
            atualizarListaPessoasCuidadas()
        }

        novo
    }

    private fun salvarPerfilNoRoom(perfil: Perfil, pendente: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            val chavePontos = chavePerfil("pontos_seguranca", perfil.id)
            val chaveModulos = chavePerfil("completed_modules", perfil.id)
            val pts = preferencias.getInt(chavePontos, 50)
            val concl = preferencias.getStringSet(chaveModulos, emptySet()).orEmpty().toList()
            val vouchers = lerVouchers(perfil.id)

            val entidade = perfil.paraEntidade(
                pontos = pts,
                modulosConcluidos = concl,
                modulosOuvidos = modulosOuvidos.toList(),
                videos = videosAssistidos.toList(),
                vouchers = vouchers,
                pendente = pendente,
            )
            dbRoom.perfilDao().inserirOuAtualizar(entidade)

            val meds = medicamentos(perfil).map { it.paraEntidade(perfil.id, pendente) }
            dbRoom.medicamentoDao().inserirVarios(meds)

            val cons = consultas(perfil).map { c ->
                val chave = "consulta_${c.id}"
                val marcados = obterChecklistSalva(chave).toList()
                val custom = obterItensCustomChecklist(chave)
                c.paraEntidade(perfil.id, marcados, custom, pendente)
            }
            dbRoom.consultaDao().inserirVarios(cons)

            dbRoom.notaDao().apagarPorPerfil(perfil.id)
            notas(perfil).forEach { n ->
                dbRoom.notaDao().inserir(NotaEntity(0, perfil.id, n.texto, n.autor, pendente))
            }
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES DE SERIALIZAÇÃO LOCAL
    // ==========================================

    private fun atualizarMedicamentos(perfil: Perfil, atualizados: List<Medicamento>) {
        listaMedicamentos[perfil.id] = atualizados
        atualizarMedicamentosLocal(perfil, atualizados)
    }

    private fun atualizarMedicamentosLocal(perfil: Perfil, atualizados: List<Medicamento>) {
        val json = JSONArray().apply {
            atualizados.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.nome)
                        .put("purpose", it.finalidade)
                        .put("ingredient", it.principioAtivo)
                        .put("time", it.hora)
                        .put("color", it.cor)
                        .put("enabled", it.ativo)
                        .put("photo", it.fotografia ?: JSONObject.NULL),
                )
            }
        }
        preferencias.edit().putString("medications_${perfil.id}", json.toString()).apply()
    }

    private fun lerMedicamentos(perfil: Perfil): List<Medicamento> = runCatching {
        val bruto = preferencias.getString("medications_${perfil.id}", null)
            ?: return perfil.medicamentos
        val array = JSONArray(bruto)
        List(array.length()) { indice ->
            val item = array.getJSONObject(indice)
            Medicamento(
                id = item.getLong("id"),
                nome = item.getString("name"),
                finalidade = item.getString("purpose"),
                principioAtivo = item.getString("ingredient"),
                hora = item.getString("time"),
                cor = item.getLong("color"),
                ativo = item.getBoolean("enabled"),
                fotografia = if (item.isNull("photo")) null else item.optString("photo").takeUnless { it.isBlank() || it == "null" },
            )
        }
    }.getOrDefault(perfil.medicamentos)

    private fun atualizarConsultasLocal(perfil: Perfil, atualizadas: List<Consulta>) {
        val json = JSONArray().apply {
            atualizadas.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("day", it.dia)
                        .put("month", it.mes)
                        .put("title", it.titulo)
                        .put("location", it.localizacao)
                        .put("type", it.tipo)
                        .put("fasting", it.jejum),
                )
            }
        }
        preferencias.edit().putString("consultations_${perfil.id}", json.toString()).apply()
    }

    private fun lerConsultas(perfil: Perfil): List<Consulta> = runCatching {
        val bruto = preferencias.getString("consultations_${perfil.id}", null) ?: return perfil.consultas
        val array = JSONArray(bruto)
        List(array.length()) { indice ->
            val item = array.getJSONObject(indice)
            Consulta(
                id = item.getInt("id"),
                dia = item.getString("day"),
                mes = item.getString("month"),
                titulo = item.getString("title"),
                localizacao = item.getString("location"),
                tipo = item.getString("type"),
                jejum = item.getBoolean("fasting"),
            )
        }
    }.getOrDefault(perfil.consultas)

    private fun guardarNotas(perfil: Perfil, atualizadas: List<Nota>) {
        listaNotas[perfil.id] = atualizadas
        guardarNotasLocal(perfil, atualizadas)
    }

    private fun guardarNotasLocal(perfil: Perfil, atualizadas: List<Nota>) {
        val json = JSONArray().apply {
            atualizadas.forEach { put(JSONObject().put("text", it.texto).put("author", it.autor)) }
        }
        preferencias.edit().putString("notes_${perfil.id}", json.toString()).apply()
    }

    private fun lerNotas(perfil: Perfil): List<Nota> = runCatching {
        val bruto = preferencias.getString("notes_${perfil.id}", null) ?: return perfil.notas
        val array = JSONArray(bruto)
        List(array.length()) {
            array.getJSONObject(it).let { item -> Nota(item.getString("text"), item.getString("author")) }
        }
    }.getOrDefault(perfil.notas)

    private fun lerTextos(chave: String, inicial: List<String>): List<String> = runCatching {
        val bruto = preferencias.getString(chave, null) ?: return inicial
        val array = JSONArray(bruto)
        List(array.length()) { array.getString(it) }
    }.getOrDefault(inicial)

    private fun escreverTextos(chave: String, valores: List<String>) {
        preferencias.edit().putString(chave, JSONArray(valores).toString()).apply()
    }

    private fun lerVouchers(idPerfil: String = perfilAtivo.id): List<VoucherResgatado> = runCatching {
        val bruto = preferencias.getString(chavePerfil("vouchers_resgatados", idPerfil), null) ?: return emptyList()
        bruto.deserializarVouchers()
    }.getOrDefault(emptyList())

    private fun escreverVouchers(idPerfil: String = perfilAtivo.id) {
        escreverVouchersLocal(idPerfil, vouchersResgatados)
    }

    private fun escreverVouchersLocal(idPerfil: String, lista: List<VoucherResgatado>) {
        preferencias.edit().putString(chavePerfil("vouchers_resgatados", idPerfil), lista.serializarVouchers()).apply()
    }

    private fun salvarItensCustomChecklistLocal(chave: String, itens: List<String>) {
        preferencias.edit().putString("chk_custom_$chave", itens.serializarListaString()).apply()
    }

    private fun lerPessoasCuidadasPersonalizadas(): List<Perfil> {
        val str = preferencias.getString("cared_profiles_custom_json", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(str)
            val lista = mutableListOf<Perfil>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                lista.add(
                    Perfil(
                        id = o.getString("id"),
                        nome = o.getString("nome"),
                        nomeCurto = o.optString("nomeCurto", o.getString("nome")),
                        relacao = o.optString("relacao", "Utente"),
                        iniciais = o.optString("iniciais", "CU"),
                        tipoSanguineo = o.optString("tipoSanguineo", "A+"),
                        alergias = o.optString("alergias", "Sem alergias conhecidas"),
                        consultas = listOf(
                            Consulta(1, "18", "Nov", "Medicina Geral e Familiar", "ULSBE · Centro de Saúde", "Consulta"),
                            Consulta(2, "02", "Dez", "Análises de Rotina", "ULSBE · Hospital", "Exame", true),
                        ),
                        medicamentos = listOf(
                            Medicamento(System.currentTimeMillis() + i, "Paracetamol 1000 mg", "Dores / Febre", "paracetamol", "08:00", 0xFF3E63DD, true),
                            Medicamento(System.currentTimeMillis() + 100 + i, "Vitamina B12", "Suplemento", "cianocobalamina", "12:00", 0xFF059669, true),
                        ),
                        outrosProdutos = emptyList(),
                        notas = listOf(Nota("Processo clínico vinculado com sucesso por SMS.", "Cuidador")),
                        nif = o.optString("nif", "999 888 777"),
                        numeroUtente = o.optString("numeroUtente", "123 456 789"),
                        cartaoCidadao = o.optString("cartaoCidadao", "00000000 0 ZZ0"),
                        dataNascimento = o.optString("dataNascimento", "01/01/1950"),
                        telemovel = o.optString("telemovel", "+351 912 345 678"),
                        email = o.optString("email", "cuidado@ulsbe.min-saude.pt"),
                    ),
                )
            }
            lista
        }.getOrDefault(emptyList())
    }

    private fun guardarPessoasCuidadasPersonalizadas() {
        val custom = pessoasCuidadas.filter { it.id.length > 5 }
        val arr = JSONArray()
        custom.forEach { p ->
            val o = JSONObject().apply {
                put("id", p.id)
                put("nome", p.nome)
                put("nomeCurto", p.nomeCurto)
                put("relacao", p.relacao)
                put("iniciais", p.iniciais)
                put("tipoSanguineo", p.tipoSanguineo)
                put("alergias", p.alergias)
                put("nif", p.nif)
                put("numeroUtente", p.numeroUtente)
                put("cartaoCidadao", p.cartaoCidadao)
                put("dataNascimento", p.dataNascimento)
                put("telemovel", p.telemovel)
                put("email", p.email)
            }
            arr.put(o)
        }
        preferencias.edit().putString("cared_profiles_custom_json", arr.toString()).apply()
    }
}

/**
 * Converte um timestamp em milissegundos para uma descrição amigável em português:
 * "Há instantes", "Hoje às 14:30", "Ontem às 10:15", ou "12/04 às 09:00".
 */
fun formatarMomentoAtualizacao(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "Hoje às 10:00"
    val agora = System.currentTimeMillis()
    val diferencaSegundos = (agora - timestampMillis) / 1000
    if (diferencaSegundos < 60) {
        return "Há instantes"
    }

    val calAtual = java.util.Calendar.getInstance()
    val calRegisto = java.util.Calendar.getInstance().apply { timeInMillis = timestampMillis }

    val horaMinuto = java.text.SimpleDateFormat("HH:mm", java.util.Locale("pt", "PT")).format(java.util.Date(timestampMillis))

    val mesmoAno = calAtual.get(java.util.Calendar.YEAR) == calRegisto.get(java.util.Calendar.YEAR)
    val mesmoDia = mesmoAno && calAtual.get(java.util.Calendar.DAY_OF_YEAR) == calRegisto.get(java.util.Calendar.DAY_OF_YEAR)
    val ontem = mesmoAno && (calAtual.get(java.util.Calendar.DAY_OF_YEAR) - calRegisto.get(java.util.Calendar.DAY_OF_YEAR) == 1)

    return when {
        mesmoDia -> "Hoje às $horaMinuto"
        ontem -> "Ontem às $horaMinuto"
        else -> {
            val formatoData = java.text.SimpleDateFormat("dd/MM 'às' HH:mm", java.util.Locale("pt", "PT"))
            formatoData.format(java.util.Date(timestampMillis))
        }
    }
}

data class InfoNivelSeguranca(
    val nivel: Int,
    val titulo: String,
    val pontosMinimos: Int,
    val pontosProximoNivel: Int,
    val progresso: Float,
)

fun calcularNivelSeguranca(pontos: Int): InfoNivelSeguranca {
    return when {
        pontos < 100 -> InfoNivelSeguranca(1, "Utente Informado", 0, 100, (pontos / 100f).coerceIn(0f, 1f))
        pontos < 250 -> InfoNivelSeguranca(2, "Praticante de Prevenção Ativa", 100, 250, ((pontos - 100) / 150f).coerceIn(0f, 1f))
        pontos < 500 -> InfoNivelSeguranca(3, "Consultor de Segurança Clínica", 250, 500, ((pontos - 250) / 250f).coerceIn(0f, 1f))
        else -> InfoNivelSeguranca(4, "Embaixador de Literacia em Saúde ULSBE", 500, 500, 1.0f)
    }
}

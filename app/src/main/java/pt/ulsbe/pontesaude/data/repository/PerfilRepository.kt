package pt.ulsbe.pontesaude.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.local.BaseDadosLocal
import pt.ulsbe.pontesaude.data.local.paraEntidade
import pt.ulsbe.pontesaude.data.local.paraModelo
import pt.ulsbe.pontesaude.data.remote.AutenticadorFirebaseCMD
import pt.ulsbe.pontesaude.data.remote.ClienteFirestore
import pt.ulsbe.pontesaude.data.remote.IntegracaoIA
import pt.ulsbe.pontesaude.model.Perfil

/**
 * Repositório de Perfis Clínicos, Sessão CMD (Chave Móvel Digital) e Modo Cuidador.
 *
 * Gere a autenticação, seleção de perfil do SNS, lista de dependentes cuidados
 * e geração de perfis de teste com dados validados para Portugal.
 */
class PerfilRepository(
    context: Context,
    private val dbLocal: BaseDadosLocal,
    private val clienteFirestore: ClienteFirestore,
    private val scope: CoroutineScope,
    private val estaOnline: () -> Boolean,
    private val integracaoIa: IntegracaoIA = IntegracaoIA(),
) {
    private val preferencias: SharedPreferences =
        context.getSharedPreferences("ponte_saude", Context.MODE_PRIVATE)

    var autenticado by mutableStateOf(preferencias.getBoolean("cmd_autenticado", false))
        private set

    var utenteSessaoId by mutableStateOf(preferencias.getString("cmd_utente_id", "1") ?: "1")
        private set

    var identificadorCMD by mutableStateOf(preferencias.getString("cmd_identificador", "") ?: "")
        private set

    var modoCuidador by mutableStateOf(preferencias.getBoolean("caregiver", false))
        private set

    var indicePerfilCuidado by mutableIntStateOf(preferencias.getInt("cared_profile", 0))
        private set

    val todosOsPerfis = mutableStateListOf<Perfil>()
    val pessoasCuidadas = mutableStateListOf<Perfil>()

    init {
        carregarPerfisLocais()
    }

    fun carregarPerfisLocais() {
        val locais = dbLocal.perfilDao().obterTodos().map { it.paraModelo() }
        if (locais.isNotEmpty()) {
            todosOsPerfis.clear()
            todosOsPerfis.addAll(locais)
            atualizarListaPessoasCuidadas()
        }
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
            return if (!modoCuidador || pessoasCuidadas.isEmpty()) {
                perfilBase
            } else {
                pessoasCuidadas.getOrNull(indicePerfilCuidado.coerceIn(0, (pessoasCuidadas.size - 1).coerceAtLeast(0))) ?: perfilBase
            }
        }

    fun iniciarSessaoCMD(idPerfil: String, identificador: String = "") {
        autenticado = true
        utenteSessaoId = idPerfil
        if (identificador.isNotBlank()) {
            identificadorCMD = identificador
        }
        preferencias.edit()
            .putBoolean("cmd_autenticado", true)
            .putString("cmd_utente_id", idPerfil)
            .putString("cmd_identificador", identificadorCMD)
            .apply()
        atualizarListaPessoasCuidadas()
    }

    fun terminarSessaoCMD() {
        autenticado = false
        identificadorCMD = ""
        preferencias.edit()
            .putBoolean("cmd_autenticado", false)
            .remove("cmd_utente_id")
            .remove("cmd_identificador")
            .apply()
        AutenticadorFirebaseCMD.terminarSessao()
    }

    fun atualizarModoCuidador(valor: Boolean) {
        modoCuidador = valor
        preferencias.edit().putBoolean("caregiver", valor).apply()
    }

    fun definirPerfilCuidado(indice: Int) {
        indicePerfilCuidado = indice
        preferencias.edit().putInt("cared_profile", indice).apply()
    }

    fun atualizarListaPessoasCuidadas() {
        val idsCuidados = perfilSessao.perfisCuidadosIds.filter { it != perfilSessao.id }
        val encontrados = todosOsPerfis.filter { it.id in idsCuidados && it.id != perfilSessao.id }
        pessoasCuidadas.clear()
        pessoasCuidadas.addAll(encontrados)
    }

    private fun gerarTelemovelAleatorio(): String {
        val random = java.util.Random()
        return "+351 91${random.nextInt(10)} ${100 + random.nextInt(900)} ${100 + random.nextInt(900)}"
    }

    suspend fun criarPerfilAleatorio(): Perfil {
        val telemovelGerado = gerarTelemovelAleatorio()
        val uid = AutenticadorFirebaseCMD.criarOuObterConta(telemovelGerado)

        // Gerar perfil clínico completo 100% dinâmico com IA (Gemini)
        val novo = integracaoIa.gerarPerfilClinicoComIA(id = uid, telemovel = telemovelGerado).getOrThrow()

        val online = estaOnline()
        val pEnt = novo.paraEntidade(50, emptyList(), emptyList(), emptyList(), emptyList(), pendente = !online)

        dbLocal.perfilDao().inserirOuAtualizar(pEnt)
        todosOsPerfis.add(novo)

        if (online) {
            runCatching {
                clienteFirestore.salvarPerfilCompleto(novo)
            }
        }
        atualizarListaPessoasCuidadas()
        return novo
    }

    fun adicionarPessoaCuidada(
        nome: String,
        relacao: String = "Utente",
        telemovel: String = "",
        nif: String = "",
        numeroUtente: String = "",
        tipoSanguineo: String = "A+",
        alergias: String = "Sem alergias conhecidas",
    ): Perfil {
        val idCuidado = "cuidado_${System.currentTimeMillis()}"
        val nomeTrim = nome.trim()
        val partes = nomeTrim.split(" ").filter { it.isNotBlank() }
        val nomeCurto = if (partes.size > 1) "${partes.first()} ${partes.last()}" else nomeTrim
        val iniciais = partes.mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifBlank { "CU" }

        val novoPerfil = Perfil(
            id = idCuidado,
            nome = nomeTrim,
            nomeCurto = nomeCurto,
            relacao = relacao.ifBlank { "Utente" },
            iniciais = iniciais,
            tipoSanguineo = tipoSanguineo,
            alergias = alergias,
            nif = nif.ifBlank { "999 888 777" },
            numeroUtente = numeroUtente.ifBlank { "123 456 789" },
            cartaoCidadao = "00000000 0 ZZ0",
            dataNascimento = "01/01/1950",
            telemovel = telemovel.ifBlank { "+351 912 345 678" },
            email = "cuidado@ulsbe.min-saude.pt",
            cuidadoresIds = listOf(utenteSessaoId).filter { it != idCuidado },
        )

        val online = estaOnline()
        dbLocal.perfilDao().inserirOuAtualizar(novoPerfil.paraEntidade(50, emptyList(), emptyList(), emptyList(), emptyList(), pendente = !online))
        todosOsPerfis.add(novoPerfil)

        if (online) {
            scope.launch {
                runCatching {
                    clienteFirestore.salvarPerfilCompleto(novoPerfil)
                }
            }
        }

        associarPerfilCuidado(novoPerfil.id)
        return novoPerfil
    }

    fun associarPerfilCuidado(idCuidado: String) {
        if (idCuidado == utenteSessaoId) return
        val meuPerfil = todosOsPerfis.find { it.id == utenteSessaoId } ?: return
        val outroPerfil = todosOsPerfis.find { it.id == idCuidado } ?: return

        val novosCuidados = (meuPerfil.perfisCuidadosIds + idCuidado).filter { it != utenteSessaoId }.distinct()
        val novosCuidadores = (outroPerfil.cuidadoresIds + utenteSessaoId).filter { it != idCuidado }.distinct()

        val meuAtualizado = meuPerfil.copy(perfisCuidadosIds = novosCuidados)
        val outroAtualizado = outroPerfil.copy(cuidadoresIds = novosCuidadores)

        val idxMeu = todosOsPerfis.indexOfFirst { it.id == utenteSessaoId }
        if (idxMeu >= 0) todosOsPerfis[idxMeu] = meuAtualizado
        val idxOutro = todosOsPerfis.indexOfFirst { it.id == idCuidado }
        if (idxOutro >= 0) todosOsPerfis[idxOutro] = outroAtualizado

        val online = estaOnline()
        dbLocal.perfilDao().inserirOuAtualizar(meuAtualizado.paraEntidade(50, emptyList(), emptyList(), emptyList(), emptyList(), pendente = !online))
        dbLocal.perfilDao().inserirOuAtualizar(outroAtualizado.paraEntidade(50, emptyList(), emptyList(), emptyList(), emptyList(), pendente = !online))

        atualizarListaPessoasCuidadas()
        val novoIdx = pessoasCuidadas.indexOfFirst { it.id == idCuidado }
        if (novoIdx >= 0) {
            definirPerfilCuidado(novoIdx)
        }
        atualizarModoCuidador(true)

        if (online) {
            scope.launch {
                runCatching {
                    clienteFirestore.associarPerfilCuidado(utenteSessaoId, idCuidado)
                }
            }
        }
    }

    fun desassociarPerfilCuidado(idCuidado: String) {
        val meuPerfil = todosOsPerfis.find { it.id == utenteSessaoId } ?: return
        val outroPerfil = todosOsPerfis.find { it.id == idCuidado }

        val novosCuidados = meuPerfil.perfisCuidadosIds.filter { it != idCuidado && it != utenteSessaoId }
        val meuAtualizado = meuPerfil.copy(perfisCuidadosIds = novosCuidados)

        val idxMeu = todosOsPerfis.indexOfFirst { it.id == utenteSessaoId }
        if (idxMeu >= 0) todosOsPerfis[idxMeu] = meuAtualizado

        val online = estaOnline()
        dbLocal.perfilDao().inserirOuAtualizar(meuAtualizado.paraEntidade(50, emptyList(), emptyList(), emptyList(), emptyList(), pendente = !online))

        if (outroPerfil != null) {
            val novosCuidadores = outroPerfil.cuidadoresIds.filter { it != utenteSessaoId && it != idCuidado }
            val outroAtualizado = outroPerfil.copy(cuidadoresIds = novosCuidadores)
            val idxOutro = todosOsPerfis.indexOfFirst { it.id == idCuidado }
            if (idxOutro >= 0) todosOsPerfis[idxOutro] = outroAtualizado
            dbLocal.perfilDao().inserirOuAtualizar(outroAtualizado.paraEntidade(50, emptyList(), emptyList(), emptyList(), emptyList(), pendente = !online))
        }

        atualizarListaPessoasCuidadas()

        if (online) {
            scope.launch {
                runCatching {
                    clienteFirestore.desassociarPerfilCuidado(utenteSessaoId, idCuidado)
                }
            }
        }
    }

    fun removerPessoaCuidada(id: String) = desassociarPerfilCuidado(id)

    suspend fun apagarPerfil(idPerfil: String) {
        val perfil = todosOsPerfis.find { it.id == idPerfil }
        val identificadorParaAuth = perfil?.telemovel?.ifBlank { idPerfil } ?: idPerfil

        todosOsPerfis.removeAll { it.id == idPerfil }
        dbLocal.perfilDao().apagar(idPerfil)
        dbLocal.medicamentoDao().apagarPorPerfil(idPerfil)
        dbLocal.consultaDao().apagarPorPerfil(idPerfil)
        dbLocal.notaDao().apagarPorPerfil(idPerfil)

        // Limpar dados em SharedPreferences associados ao perfil apagado
        val chavesParaApagar = preferencias.all.keys.filter {
            it.endsWith("_$idPerfil") || it == "historico_ia_$idPerfil"
        }
        val editor = preferencias.edit()
        chavesParaApagar.forEach { editor.remove(it) }
        editor.apply()

        if (estaOnline()) {
            runCatching {
                clienteFirestore.apagarPerfil(idPerfil)
            }
            runCatching {
                AutenticadorFirebaseCMD.eliminarContaAuth(identificadorParaAuth)
            }
        }
        atualizarListaPessoasCuidadas()
    }

    suspend fun apagarPerfisAleatorios(): Int {
        val aleatorios = todosOsPerfis.filter { it.id !in setOf("1", "2") }
        if (aleatorios.isEmpty()) return 0
        val perfisParaApagar = aleatorios.toList()
        val idsParaApagar = perfisParaApagar.map { it.id }

        todosOsPerfis.removeAll { it.id in idsParaApagar }
        val editor = preferencias.edit()
        idsParaApagar.forEach { id ->
            dbLocal.perfilDao().apagar(id)
            dbLocal.medicamentoDao().apagarPorPerfil(id)
            dbLocal.consultaDao().apagarPorPerfil(id)
            dbLocal.notaDao().apagarPorPerfil(id)

            preferencias.all.keys.filter { it.endsWith("_$id") || it == "historico_ia_$id" }
                .forEach { editor.remove(it) }
        }
        editor.apply()

        if (estaOnline()) {
            runCatching {
                clienteFirestore.apagarPerfisEmMassa(idsParaApagar)
            }
            for (p in perfisParaApagar) {
                runCatching {
                    val ident = p.telemovel.ifBlank { p.id }
                    AutenticadorFirebaseCMD.eliminarContaAuth(ident)
                }
            }
        }
        atualizarListaPessoasCuidadas()
        return idsParaApagar.size
    }
}

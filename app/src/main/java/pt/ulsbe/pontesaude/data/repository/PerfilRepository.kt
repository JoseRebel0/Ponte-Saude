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
import pt.ulsbe.pontesaude.data.local.deserializarListaString
import pt.ulsbe.pontesaude.data.local.paraEntidade
import pt.ulsbe.pontesaude.data.local.paraModelo
import pt.ulsbe.pontesaude.data.remote.ClienteFirestore
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.util.GeradorDadosPortugal

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
) {
    private val preferencias: SharedPreferences =
        context.getSharedPreferences("ponte_saude", Context.MODE_PRIVATE)

    var autenticado by mutableStateOf(preferencias.getBoolean("cmd_autenticado", false))
        private set

    var utenteSessaoId by mutableStateOf(preferencias.getString("cmd_utente_id", "1") ?: "1")
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

    fun iniciarSessaoCMD(idPerfil: String) {
        autenticado = true
        utenteSessaoId = idPerfil
        preferencias.edit()
            .putBoolean("cmd_autenticado", true)
            .putString("cmd_utente_id", idPerfil)
            .apply()
        atualizarListaPessoasCuidadas()
    }

    fun terminarSessaoCMD() {
        autenticado = false
        preferencias.edit()
            .putBoolean("cmd_autenticado", false)
            .remove("cmd_utente_id")
            .apply()
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
        val idsCuidados = perfilSessao.perfisCuidadosIds
        val encontrados = todosOsPerfis.filter { it.id in idsCuidados }
        pessoasCuidadas.clear()
        pessoasCuidadas.addAll(encontrados)
    }

    private fun proximoIdPerfil(): String {
        val gravado = preferencias.getLong("contador_id_perfil", 0L)
        val maiorAtual = todosOsPerfis.mapNotNull { it.id.toLongOrNull() }.maxOrNull() ?: 0L
        val proximo = maxOf(gravado, maiorAtual) + 1L
        preferencias.edit().putLong("contador_id_perfil", proximo).apply()
        return proximo.toString()
    }

    fun criarPerfilAleatorio(): Perfil {
        val proximoId = proximoIdPerfil()
        val novo = GeradorDadosPortugal.gerarPerfilAleatorio(proximoId)
        val online = estaOnline()
        val pEnt = novo.paraEntidade(50, emptyList(), emptyList(), emptyList(), emptyList(), pendente = !online)

        dbLocal.perfilDao().inserirOuAtualizar(pEnt)
        todosOsPerfis.add(novo)

        if (online) {
            scope.launch {
                runCatching {
                    clienteFirestore.salvarPerfilCompleto(novo)
                }
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
        val proximoId = proximoIdPerfil()
        val nomeTrim = nome.trim()
        val partes = nomeTrim.split(" ").filter { it.isNotBlank() }
        val nomeCurto = if (partes.size > 1) "${partes.first()} ${partes.last()}" else nomeTrim
        val iniciais = partes.mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifBlank { "CU" }

        val novoPerfil = Perfil(
            id = proximoId,
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

        val novosCuidados = (meuPerfil.perfisCuidadosIds + idCuidado).distinct()
        val novosCuidadores = (outroPerfil.cuidadoresIds + utenteSessaoId).distinct()

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

        val novosCuidados = meuPerfil.perfisCuidadosIds.filter { it != idCuidado }
        val meuAtualizado = meuPerfil.copy(perfisCuidadosIds = novosCuidados)

        val idxMeu = todosOsPerfis.indexOfFirst { it.id == utenteSessaoId }
        if (idxMeu >= 0) todosOsPerfis[idxMeu] = meuAtualizado

        val online = estaOnline()
        dbLocal.perfilDao().inserirOuAtualizar(meuAtualizado.paraEntidade(50, emptyList(), emptyList(), emptyList(), emptyList(), pendente = !online))

        if (outroPerfil != null) {
            val novosCuidadores = outroPerfil.cuidadoresIds.filter { it != utenteSessaoId }
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

    fun apagarPerfil(idPerfil: String) {
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
            scope.launch {
                runCatching {
                    clienteFirestore.apagarPerfil(idPerfil)
                }
            }
        }
        atualizarListaPessoasCuidadas()
    }

    fun apagarPerfisAleatorios(): Int {
        val aleatorios = todosOsPerfis.filter { it.id !in setOf("1", "2") }
        val idsParaApagar = aleatorios.map { it.id }

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
            scope.launch {
                runCatching {
                    clienteFirestore.apagarPerfisEmMassa(idsParaApagar)
                }
            }
        }
        atualizarListaPessoasCuidadas()
        return idsParaApagar.size
    }
}

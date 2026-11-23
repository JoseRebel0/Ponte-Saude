package pt.ulsbe.pontesaude.data.repository

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.local.BaseDadosLocal
import pt.ulsbe.pontesaude.data.local.entity.NotaEntity
import pt.ulsbe.pontesaude.data.local.paraModelo
import pt.ulsbe.pontesaude.data.remote.ClienteFirestore
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil

/**
 * Repositório de Apontamentos, Dúvidas Clínicas e Produtos de Farmácia.
 *
 * Gere as notas do utente/cuidador e a lista de produtos de parafarmácia.
 * Implementa sincronização bidirecional Offline-First (Room SQLite + Cloud Firestore).
 */
class NotaRepository(
    private val dbLocal: BaseDadosLocal,
    private val clienteFirestore: ClienteFirestore,
    private val scope: CoroutineScope,
    private val estaOnline: () -> Boolean,
    private val aoAtualizarNotasPerfil: ((String, List<Nota>) -> Unit)? = null,
) {
    private val listaNotas = mutableStateMapOf<String, List<Nota>>()
    private val listaProdutos = mutableStateMapOf<String, List<String>>()

    fun carregarDadosLocais(idPerfil: String) {
        val notas = dbLocal.notaDao().obterPorPerfil(idPerfil).map { it.paraModelo() }
        listaNotas[idPerfil] = notas
    }

    fun notas(perfil: Perfil): List<Nota> {
        if (listaNotas.containsKey(perfil.id)) {
            return listaNotas[perfil.id].orEmpty()
        }
        val doRoom = dbLocal.notaDao().obterPorPerfil(perfil.id).map { it.paraModelo() }
        if (doRoom.isNotEmpty()) {
            listaNotas[perfil.id] = doRoom
            return doRoom
        }
        val iniciais = perfil.notas
        listaNotas[perfil.id] = iniciais
        if (iniciais.isNotEmpty()) {
            iniciais.forEach {
                dbLocal.notaDao().inserir(NotaEntity(0, perfil.id, it.texto, it.autor, false))
            }
        }
        return iniciais
    }

    fun produtos(perfil: Perfil): List<String> =
        listaProdutos[perfil.id] ?: perfil.outrosProdutos

    fun atualizarNotasEmMemoria(idPerfil: String, notas: List<Nota>) {
        listaNotas[idPerfil] = notas
        aoAtualizarNotasPerfil?.invoke(idPerfil, notas)
    }

    fun atualizarProdutosEmMemoria(idPerfil: String, prods: List<String>) {
        listaProdutos[idPerfil] = prods
    }

    fun adicionarNota(perfil: Perfil, nota: Nota) {
        val atuais = notas(perfil) + nota
        listaNotas[perfil.id] = atuais
        aoAtualizarNotasPerfil?.invoke(perfil.id, atuais)

        val online = estaOnline()
        dbLocal.notaDao().inserir(NotaEntity(0, perfil.id, nota.texto, nota.autor, !online))

        if (online) {
            scope.launch {
                runCatching {
                    clienteFirestore.atualizarNotas(perfil.id, atuais)
                }
            }
        }
    }

    fun apagarNota(perfil: Perfil, indice: Int) {
        val atuais = notas(perfil).toMutableList()
        if (indice in atuais.indices) {
            atuais.removeAt(indice)
            listaNotas[perfil.id] = atuais
            aoAtualizarNotasPerfil?.invoke(perfil.id, atuais)

            val online = estaOnline()
            dbLocal.notaDao().apagarPorPerfil(perfil.id)
            atuais.forEach {
                dbLocal.notaDao().inserir(NotaEntity(0, perfil.id, it.texto, it.autor, !online))
            }

            if (online) {
                scope.launch {
                    runCatching {
                        clienteFirestore.atualizarNotas(perfil.id, atuais)
                    }
                }
            }
        }
    }

    fun apagarNota(perfil: Perfil, nota: Nota) {
        val atuais = notas(perfil).toMutableList()
        val idx = atuais.indexOf(nota)
        if (idx >= 0) {
            apagarNota(perfil, idx)
        }
    }

    fun adicionarProduto(perfil: Perfil, produto: String) {
        val atuais = produtos(perfil) + produto
        listaProdutos[perfil.id] = atuais

        if (estaOnline()) {
            scope.launch {
                runCatching {
                    clienteFirestore.atualizarProdutos(perfil.id, atuais)
                }
            }
        }
    }

    fun apagarProduto(perfil: Perfil, produto: String) {
        val atuais = produtos(perfil).filterNot { it == produto }
        listaProdutos[perfil.id] = atuais

        if (estaOnline()) {
            scope.launch {
                runCatching {
                    clienteFirestore.atualizarProdutos(perfil.id, atuais)
                }
            }
        }
    }
}

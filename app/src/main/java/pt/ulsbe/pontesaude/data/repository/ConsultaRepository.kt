package pt.ulsbe.pontesaude.data.repository

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.local.BaseDadosLocal
import pt.ulsbe.pontesaude.data.local.deserializarListaString
import pt.ulsbe.pontesaude.data.local.paraEntidade
import pt.ulsbe.pontesaude.data.local.paraModelo
import pt.ulsbe.pontesaude.data.local.serializarListaString
import pt.ulsbe.pontesaude.data.remote.ClienteFirestore
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Perfil

/**
 * Repositório de Consultas e Exames Médicos.
 *
 * Responsável pelo agendamento, checklists preparatórias de consultas e sincronização
 * Offline-First (Room SQLite local + Cloud Firestore em tempo real).
 */
class ConsultaRepository(
    private val dbLocal: BaseDadosLocal,
    private val clienteFirestore: ClienteFirestore,
    private val scope: CoroutineScope,
    private val estaOnline: () -> Boolean,
) {
    private val listaConsultas = mutableStateMapOf<String, List<Consulta>>()

    fun carregarDadosLocais(idPerfil: String) {
        val consultas = dbLocal.consultaDao().obterPorPerfil(idPerfil).map { it.paraModelo() }
        listaConsultas[idPerfil] = consultas
    }

    fun consultas(perfil: Perfil): List<Consulta> {
        if (listaConsultas.containsKey(perfil.id)) {
            return listaConsultas[perfil.id].orEmpty()
        }
        val doRoom = dbLocal.consultaDao().obterPorPerfil(perfil.id).map { it.paraModelo() }
        if (doRoom.isNotEmpty()) {
            listaConsultas[perfil.id] = doRoom
            return doRoom
        }
        val iniciais = perfil.consultas
        listaConsultas[perfil.id] = iniciais
        return iniciais
    }

    fun atualizarListaEmMemoria(idPerfil: String, consultas: List<Consulta>) {
        listaConsultas[idPerfil] = consultas
    }

    fun adicionarConsulta(perfil: Perfil, consulta: Consulta) {
        val atualizadas = consultas(perfil) + consulta
        listaConsultas[perfil.id] = atualizadas

        val online = estaOnline()
        dbLocal.consultaDao().inserirOuAtualizar(consulta.paraEntidade(perfil.id, pendente = !online))

        if (online) {
            scope.launch {
                runCatching {
                    clienteFirestore.guardarConsulta(perfil.id, consulta)
                }
            }
        }
    }

    fun agendarConsulta(perfil: Perfil, consulta: Consulta) =
        adicionarConsulta(perfil, consulta)

    fun obterChecklistSalva(idConsulta: Int, idPerfil: String): Set<String> {
        val cons = if (idPerfil.isNotBlank()) {
            dbLocal.consultaDao().obterPorIdEPerfil(idConsulta, idPerfil)
                ?: dbLocal.consultaDao().obterPorPerfil(idPerfil).find { it.id == idConsulta }
        } else {
            dbLocal.consultaDao().obterPorId(idConsulta)
        }
        return cons?.checklistMarcadosJson?.deserializarListaString()?.toSet() ?: emptySet()
    }

    fun obterChecklistSalva(chave: String, idPerfil: String = ""): Set<String> {
        val idConsulta = chave.removePrefix("consulta_").substringBefore("_").toIntOrNull()
        if (idConsulta != null) {
            return obterChecklistSalva(idConsulta, idPerfil)
        }
        return emptySet()
    }

    fun salvarChecklist(idConsulta: Int, idPerfil: String, marcados: Set<String>) {
        val consEnt = if (idPerfil.isNotBlank()) {
            dbLocal.consultaDao().obterPorIdEPerfil(idConsulta, idPerfil)
                ?: dbLocal.consultaDao().obterPorPerfil(idPerfil).find { it.id == idConsulta }
        } else {
            dbLocal.consultaDao().obterPorId(idConsulta)
        }
        if (consEnt != null) {
            val perfilDestino = if (idPerfil.isNotBlank()) idPerfil else consEnt.idPerfil
            consEnt.checklistMarcadosJson = marcados.toList().serializarListaString()
            consEnt.pendenteSincronizacao = !estaOnline()
            dbLocal.consultaDao().inserirOuAtualizar(consEnt)
            if (estaOnline() && perfilDestino.isNotBlank()) {
                scope.launch {
                    runCatching {
                        clienteFirestore.atualizarChecklistConsulta(perfilDestino, idConsulta, marcados.toList())
                    }
                }
            }
        }
    }

    fun salvarChecklist(chave: String, marcados: Set<String>, idPerfil: String = "") {
        val idConsulta = chave.removePrefix("consulta_").substringBefore("_").toIntOrNull()
        if (idConsulta != null) {
            salvarChecklist(idConsulta, idPerfil, marcados)
        }
    }

    fun obterItensCustomChecklist(idConsulta: Int, idPerfil: String): List<String> {
        val cons = if (idPerfil.isNotBlank()) {
            dbLocal.consultaDao().obterPorIdEPerfil(idConsulta, idPerfil)
                ?: dbLocal.consultaDao().obterPorPerfil(idPerfil).find { it.id == idConsulta }
        } else {
            dbLocal.consultaDao().obterPorId(idConsulta)
        }
        return cons?.checklistCustomJson?.deserializarListaString() ?: emptyList()
    }

    fun obterItensCustomChecklist(chave: String, idPerfil: String = ""): List<String> {
        val idConsulta = chave.removePrefix("consulta_").substringBefore("_").toIntOrNull()
        if (idConsulta != null) {
            return obterItensCustomChecklist(idConsulta, idPerfil)
        }
        return emptyList()
    }

    fun salvarItensCustomChecklist(idConsulta: Int, idPerfil: String, itens: List<String>) {
        val consEnt = if (idPerfil.isNotBlank()) {
            dbLocal.consultaDao().obterPorIdEPerfil(idConsulta, idPerfil)
                ?: dbLocal.consultaDao().obterPorPerfil(idPerfil).find { it.id == idConsulta }
        } else {
            dbLocal.consultaDao().obterPorId(idConsulta)
        }
        if (consEnt != null) {
            val perfilDestino = if (idPerfil.isNotBlank()) idPerfil else consEnt.idPerfil
            consEnt.checklistCustomJson = itens.serializarListaString()
            consEnt.pendenteSincronizacao = !estaOnline()
            dbLocal.consultaDao().inserirOuAtualizar(consEnt)
            if (estaOnline() && perfilDestino.isNotBlank()) {
                scope.launch {
                    runCatching {
                        clienteFirestore.atualizarChecklistCustomConsulta(perfilDestino, idConsulta, itens)
                    }
                }
            }
        }
    }

    fun salvarItensCustomChecklist(chave: String, itens: List<String>, idPerfil: String = "") {
        val idConsulta = chave.removePrefix("consulta_").substringBefore("_").toIntOrNull()
        if (idConsulta != null) {
            salvarItensCustomChecklist(idConsulta, idPerfil, itens)
        }
    }

    fun sincronizarPendentes(idPerfil: String) {
        val pendentes = dbLocal.consultaDao().obterPendentes()
        if (pendentes.isNotEmpty() && estaOnline()) {
            scope.launch {
                for (c in pendentes) {
                    clienteFirestore.guardarConsulta(c.idPerfil, c.paraModelo())
                    clienteFirestore.atualizarChecklistConsulta(c.idPerfil, c.id, c.checklistMarcadosJson.deserializarListaString())
                    clienteFirestore.atualizarChecklistCustomConsulta(c.idPerfil, c.id, c.checklistCustomJson.deserializarListaString())
                    c.pendenteSincronizacao = false
                    dbLocal.consultaDao().inserirOuAtualizar(c)
                }
            }
        }
    }
}

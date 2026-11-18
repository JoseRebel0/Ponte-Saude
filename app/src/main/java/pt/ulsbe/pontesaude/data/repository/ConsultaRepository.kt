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

    fun consultas(perfil: Perfil): List<Consulta> =
        listaConsultas[perfil.id] ?: perfil.consultas

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

    fun atualizarConsultas(perfil: Perfil, atualizadas: List<Consulta>) {
        listaConsultas[perfil.id] = atualizadas

        val online = estaOnline()
        val entidades = atualizadas.map { it.paraEntidade(perfil.id, pendente = !online) }
        dbLocal.consultaDao().apagarPorPerfil(perfil.id)
        dbLocal.consultaDao().inserirVarios(entidades)

        if (online) {
            scope.launch {
                runCatching {
                    clienteFirestore.atualizarConsultas(perfil.id, atualizadas)
                }
            }
        }
    }

    fun agendarConsulta(perfil: Perfil, consulta: Consulta) =
        adicionarConsulta(perfil, consulta)

    fun cancelarConsulta(perfil: Perfil, id: Int) {
        val atuais = consultas(perfil).filterNot { it.id == id }
        atualizarConsultas(perfil, atuais)
    }

    fun atualizarChecklist(idConsulta: Int, item: String, marcado: Boolean, idPerfil: String) {
        val consEnt = dbLocal.consultaDao().obterPorPerfil(idPerfil).find { it.id == idConsulta }
        if (consEnt != null) {
            val marcados = consEnt.checklistMarcadosJson.deserializarListaString().toMutableSet()
            if (marcado) marcados.add(item) else marcados.remove(item)
            consEnt.checklistMarcadosJson = marcados.toList().serializarListaString()
            consEnt.pendenteSincronizacao = !estaOnline()
            dbLocal.consultaDao().inserirOuAtualizar(consEnt)

            if (estaOnline()) {
                scope.launch {
                    runCatching {
                        clienteFirestore.atualizarChecklistConsulta(idPerfil, idConsulta, marcados.toList())
                    }
                }
            }
        }
    }

    fun atualizarChecklistCustom(idConsulta: Int, itens: List<String>, idPerfil: String) {
        val consEnt = dbLocal.consultaDao().obterPorPerfil(idPerfil).find { it.id == idConsulta }
        if (consEnt != null) {
            consEnt.checklistCustomJson = itens.serializarListaString()
            consEnt.pendenteSincronizacao = !estaOnline()
            dbLocal.consultaDao().inserirOuAtualizar(consEnt)

            if (estaOnline()) {
                scope.launch {
                    runCatching {
                        clienteFirestore.atualizarChecklistCustomConsulta(idPerfil, idConsulta, itens)
                    }
                }
            }
        }
    }

    fun obterChecklistSalva(chave: String): Set<String> {
        val idConsulta = chave.removePrefix("consulta_").toIntOrNull()
        if (idConsulta != null) {
            val todas = dbLocal.consultaDao().obterTodos()
            val cons = todas.find { it.id == idConsulta }
            if (cons != null) {
                return cons.checklistMarcadosJson.deserializarListaString().toSet()
            }
        }
        return emptySet()
    }

    fun salvarChecklist(chave: String, marcados: Set<String>, idPerfil: String = "1") {
        val idConsulta = chave.removePrefix("consulta_").toIntOrNull()
        if (idConsulta != null) {
            val consEnt = dbLocal.consultaDao().obterPorPerfil(idPerfil).find { it.id == idConsulta }
                ?: dbLocal.consultaDao().obterTodos().find { it.id == idConsulta }
            if (consEnt != null) {
                consEnt.checklistMarcadosJson = marcados.toList().serializarListaString()
                consEnt.pendenteSincronizacao = !estaOnline()
                dbLocal.consultaDao().inserirOuAtualizar(consEnt)
                if (estaOnline()) {
                    scope.launch {
                        runCatching {
                            clienteFirestore.atualizarChecklistConsulta(consEnt.idPerfil, idConsulta, marcados.toList())
                        }
                    }
                }
            }
        }
    }

    fun obterItensCustomChecklist(chave: String): List<String> {
        val idConsulta = chave.removePrefix("consulta_").toIntOrNull()
        if (idConsulta != null) {
            val todas = dbLocal.consultaDao().obterTodos()
            val cons = todas.find { it.id == idConsulta }
            if (cons != null) {
                return cons.checklistCustomJson.deserializarListaString()
            }
        }
        return emptyList()
    }

    fun salvarItensCustomChecklist(chave: String, itens: List<String>, idPerfil: String = "1") {
        val idConsulta = chave.removePrefix("consulta_").toIntOrNull()
        if (idConsulta != null) {
            val consEnt = dbLocal.consultaDao().obterPorPerfil(idPerfil).find { it.id == idConsulta }
                ?: dbLocal.consultaDao().obterTodos().find { it.id == idConsulta }
            if (consEnt != null) {
                consEnt.checklistCustomJson = itens.serializarListaString()
                consEnt.pendenteSincronizacao = !estaOnline()
                dbLocal.consultaDao().inserirOuAtualizar(consEnt)
                if (estaOnline()) {
                    scope.launch {
                        runCatching {
                            clienteFirestore.atualizarChecklistCustomConsulta(consEnt.idPerfil, idConsulta, itens)
                        }
                    }
                }
            }
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

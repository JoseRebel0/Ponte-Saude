package pt.ulsbe.pontesaude.data.repository

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.local.BaseDadosLocal
import pt.ulsbe.pontesaude.data.local.paraEntidade
import pt.ulsbe.pontesaude.data.local.paraModelo
import pt.ulsbe.pontesaude.data.remote.ClienteFirestore
import pt.ulsbe.pontesaude.data.remote.IntegracaoIA
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Perfil

/**
 * Repositório de Medicação e Farmácia.
 *
 * Responsável pelo plano terapêutico do utente, fotos de caixas e posologia.
 * Implementa o padrão Offline-First (Room SQLite + Cloud Firestore) e tradução por IA Gemini.
 */
class MedicacaoRepository(
    private val dbLocal: BaseDadosLocal,
    private val clienteFirestore: ClienteFirestore,
    private val integracaoIa: IntegracaoIA = IntegracaoIA(),
    private val scope: CoroutineScope,
    private val estaOnline: () -> Boolean,
) {
    private val listaMedicamentos = mutableStateMapOf<String, List<Medicamento>>()

    fun carregarDadosLocais(idPerfil: String) {
        val meds = dbLocal.medicamentoDao().obterPorPerfil(idPerfil).map { it.paraModelo() }
        listaMedicamentos[idPerfil] = meds
    }

    fun medicamentos(perfil: Perfil): List<Medicamento> =
        listaMedicamentos[perfil.id] ?: perfil.medicamentos

    fun atualizarListaEmMemoria(idPerfil: String, meds: List<Medicamento>) {
        listaMedicamentos[idPerfil] = meds
    }

    fun adicionarMedicamento(perfil: Perfil, medicamento: Medicamento) {
        val atuais = medicamentos(perfil) + medicamento
        listaMedicamentos[perfil.id] = atuais

        val online = estaOnline()
        dbLocal.medicamentoDao().inserirOuAtualizar(medicamento.paraEntidade(perfil.id, !online))

        if (online) {
            scope.launch {
                runCatching {
                    clienteFirestore.guardarMedicamento(perfil.id, medicamento)
                }
            }
        }
    }

    fun atualizarMedicamento(perfil: Perfil, medicamento: Medicamento) {
        val atuais = medicamentos(perfil).map { if (it.id == medicamento.id) medicamento else it }
        listaMedicamentos[perfil.id] = atuais

        val online = estaOnline()
        dbLocal.medicamentoDao().inserirOuAtualizar(medicamento.paraEntidade(perfil.id, !online))

        if (online) {
            scope.launch {
                runCatching {
                    clienteFirestore.guardarMedicamento(perfil.id, medicamento)
                }
            }
        }
    }

    fun apagarMedicamento(perfil: Perfil, id: Long) {
        val atuais = medicamentos(perfil).filterNot { it.id == id }
        listaMedicamentos[perfil.id] = atuais

        dbLocal.medicamentoDao().apagarPorIdEPerfil(id, perfil.id)

        if (estaOnline()) {
            scope.launch {
                runCatching {
                    clienteFirestore.apagarMedicamento(perfil.id, id)
                }
            }
        }
    }

    fun definirMedicamentoAtivo(perfil: Perfil, id: Long, ativo: Boolean) {
        val med = medicamentos(perfil).find { it.id == id } ?: return
        atualizarMedicamento(perfil, med.copy(ativo = ativo))
    }

    fun atualizarFotoMedicamento(perfil: Perfil, idMedicamento: Long, novaFotoBase64: String?) {
        val med = medicamentos(perfil).find { it.id == idMedicamento } ?: return
        val medAtualizado = med.copy(fotografia = novaFotoBase64)
        atualizarMedicamento(perfil, medAtualizado)
    }

    suspend fun explicarMedicamentoComIa(nomeMedicamento: String): Result<String> =
        integracaoIa.explicarMedicamento(nomeMedicamento)

    fun sincronizarPendentes(idPerfil: String) {
        val pendentes = dbLocal.medicamentoDao().obterPendentes()
        if (pendentes.isNotEmpty() && estaOnline()) {
            scope.launch {
                for (m in pendentes) {
                    clienteFirestore.guardarMedicamento(m.idPerfil, m.paraModelo())
                    m.pendenteSincronizacao = false
                    dbLocal.medicamentoDao().inserirOuAtualizar(m)
                }
            }
        }
    }
}

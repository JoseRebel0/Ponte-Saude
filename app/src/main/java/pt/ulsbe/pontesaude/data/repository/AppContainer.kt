package pt.ulsbe.pontesaude.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ulsbe.pontesaude.data.local.BaseDadosLocal
import pt.ulsbe.pontesaude.data.local.paraEntidade
import pt.ulsbe.pontesaude.data.remote.ClienteFirestore
import pt.ulsbe.pontesaude.data.remote.IntegracaoIA
import pt.ulsbe.pontesaude.model.Perfil

/**
 * Contentor de Injeção de Dependências e Coordenação de Sincronização (Manual Dependency Container).
 *
 * Fornece os 6 repositórios de domínio modulares recomendados pela arquitetura Android:
 * - [configuracao]: Gestão de preferências e acessibilidade.
 * - [perfil]: Gestão de utentes, autenticação CMD e modo cuidador.
 * - [medicacao]: Gestão do plano terapêutico e alarmes.
 * - [consultas]: Agendamento de consultas médicas e checklists.
 * - [saude]: Literacia, vídeos e Clube de Segurança.
 * - [notas]: Apontamentos clínicos e produtos de parafarmácia.
 */
class AppContainer(context: Context) {
    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val dbLocal: BaseDadosLocal = BaseDadosLocal.obterInstancia(context)
    val clienteFirestore = ClienteFirestore()
    val integracaoIa = IntegracaoIA()

    var estaOnline by mutableStateOf(true)
        private set
    var erroPrimeiraConexao by mutableStateOf(false)
        private set
    var estadoSincronizacaoFirestore by mutableStateOf("Sincronização automática ativa")
        private set
    var estaASincronizarFirestore by mutableStateOf(false)
        private set

    val configuracao = ConfiguracaoRepository(context)
    val perfil = PerfilRepository(context, dbLocal, clienteFirestore, scope, estaOnline = { estaOnline })
    val medicacao = MedicacaoRepository(dbLocal, clienteFirestore, integracaoIa, scope, estaOnline = { estaOnline })
    val consultas = ConsultaRepository(dbLocal, clienteFirestore, scope, estaOnline = { estaOnline })
    val saude = SaudeRepository(context, dbLocal, clienteFirestore, integracaoIa, scope, estaOnline = { estaOnline })
    val notas = NotaRepository(dbLocal, clienteFirestore, scope, estaOnline = { estaOnline })

    val perfilAtivoCompleto: Perfil
        get() {
            val base = perfil.perfilAtivo
            return base.copy(
                medicamentos = medicacao.medicamentos(base),
                consultas = consultas.consultas(base),
                notas = notas.notas(base),
                outrosProdutos = notas.produtos(base),
            )
        }

    init {
        carregarTudoDoRoomESincronizar()
        iniciarEscutaTempoRealFirestore()
    }

    private fun carregarTudoDoRoomESincronizar() {
        scope.launch(Dispatchers.IO) {
            saude.carregarCatalogoLocal()
            perfil.carregarPerfisLocais()

            for (p in perfil.todosOsPerfis) {
                medicacao.carregarDadosLocais(p.id)
                consultas.carregarDadosLocais(p.id)
                notas.carregarDadosLocais(p.id)
            }

            sincronizarAutomatico()
        }
    }

    fun sincronizarAutomatico(online: Boolean = estaOnline) {
        estaOnline = online
        scope.launch(Dispatchers.IO) {
            val totalNoRoom = dbLocal.perfilDao().contar()
            if (!online) {
                if (totalNoRoom == 0) {
                    withContext(Dispatchers.Main) { erroPrimeiraConexao = true }
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                erroPrimeiraConexao = false
                estaASincronizarFirestore = true
                estadoSincronizacaoFirestore = "A sincronizar em tempo real com o Firestore..."
            }

            runCatching {
                sincronizarComFirestore()
            }

            withContext(Dispatchers.Main) {
                estaASincronizarFirestore = false
                estadoSincronizacaoFirestore = "Sincronizado em tempo real com o Firestore"
            }
        }
    }

    suspend fun sincronizarComFirestore() {
        val remotos = clienteFirestore.carregarTodosOsPerfis().getOrNull()?.map { it.perfil }.orEmpty()
        if (remotos.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                perfil.todosOsPerfis.clear()
                perfil.todosOsPerfis.addAll(remotos)
                perfil.atualizarListaPessoasCuidadas()

                for (p in remotos) {
                    medicacao.atualizarListaEmMemoria(p.id, p.medicamentos)
                    consultas.atualizarListaEmMemoria(p.id, p.consultas)
                    notas.atualizarNotasEmMemoria(p.id, p.notas)
                    notas.atualizarProdutosEmMemoria(p.id, p.outrosProdutos)

                    dbLocal.perfilDao().inserirOuAtualizar(p.paraEntidade(saude.pontosSeguranca, emptyList(), emptyList(), emptyList(), emptyList(), false))
                    dbLocal.medicamentoDao().inserirVarios(p.medicamentos.map { it.paraEntidade(idPerfil = p.id, pendente = false) })
                    dbLocal.consultaDao().inserirVarios(p.consultas.map { it.paraEntidade(idPerfil = p.id, pendente = false) })
                }
            }
        }

        // Sincronizar dados pendentes criados offline
        for (p in perfil.todosOsPerfis) {
            medicacao.sincronizarPendentes(p.id)
            consultas.sincronizarPendentes(p.id)
        }
        configuracao.registarAtualizacao()
    }

    fun iniciarEscutaTempoRealFirestore() {
        clienteFirestore.iniciarEscutaPerfisEmTempoReal {
            scope.launch {
                sincronizarComFirestore()
            }
        }
    }

    fun tentarNovamentePrimeiraConexao(temInternet: Boolean) {
        sincronizarAutomatico(temInternet)
    }
}

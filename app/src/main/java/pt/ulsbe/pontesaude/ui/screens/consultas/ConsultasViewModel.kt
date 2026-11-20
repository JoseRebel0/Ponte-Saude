package pt.ulsbe.pontesaude.ui.screens.consultas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.repository.ConfiguracaoRepository
import pt.ulsbe.pontesaude.data.repository.ConsultaRepository
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.util.ServicosDispositivo

/**
 * Estado imutável da interface do Ecrã de Consultas e Exames ([EcraConsultas]).
 *
 * @property perfil Perfil do utente com as suas informações clínicas associadas.
 * @property consultas Lista ordenada de consultas médicas e exames de diagnóstico agendados.
 * @property consultaSelecionada Consulta atualmente em foco para exibição detalhada da checklist.
 * @property agendaEAlarmesAtivos Indica se a sincronização com o calendário nativo do Android está ativa.
 * @property modoCuidador Indica se o ecrã está a ser visualizado no papel de cuidador.
 * @property emCarregamento Indica se está a decorrer sincronização ou operação na base de dados.
 * @property mensagemErro Mensagem descritiva em caso de falha de agendamento ou cancelamento.
 * @property mensagemSucesso Mensagem de confirmação de ações concluídas com sucesso.
 */
data class ConsultasUiState(
    val perfil: Perfil? = null,
    val consultas: List<Consulta> = emptyList(),
    val consultaSelecionada: Consulta? = null,
    val agendaEAlarmesAtivos: Boolean = true,
    val modoCuidador: Boolean = false,
    val emCarregamento: Boolean = false,
    val mensagemErro: String? = null,
    val mensagemSucesso: String? = null,
)

/**
 * ViewModel responsável pela gestão da agenda de consultas e listas de verificação (Checklists).
 *
 * Garante a sincronização bidirecional Offline-First entre a persistência local (Room SQLite),
 * a nuvem (Cloud Firestore) e o calendário nativo do sistema Android via [CalendarContract].
 */
class ConsultasViewModel(
    private val consultaRepository: ConsultaRepository,
    private val perfilRepository: PerfilRepository,
    private val configuracaoRepository: ConfiguracaoRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsultasUiState())
    val uiState: StateFlow<ConsultasUiState> = _uiState.asStateFlow()

    init {
        recarregar()
    }

    /**
     * Atualiza o estado da UI com as consultas mais recentes do perfil ativo no repositório.
     */
    fun recarregar() {
        val perfil = perfilRepository.perfilAtivo
        val perfilMudou = _uiState.value.perfil?.id != perfil.id
        val consultas = consultaRepository.consultas(perfil)
        val modoCuidador = perfilRepository.modoCuidador
        val agendaAtiva = configuracaoRepository?.agendaEAlarmesAtivos ?: true
        _uiState.update {
            it.copy(
                perfil = perfil,
                consultas = consultas,
                consultaSelecionada = if (perfilMudou) {
                    consultas.firstOrNull()
                } else {
                    it.consultaSelecionada?.takeIf { c -> consultas.any { it.id == c.id } } ?: consultas.firstOrNull()
                },
                agendaEAlarmesAtivos = agendaAtiva,
                modoCuidador = modoCuidador,
            )
        }
    }

    /**
     * Dispara o pull-to-refresh e sincroniza consultas com a cloud.
     */
    fun dispararAtualizacao(aoSincronizar: (suspend () -> Unit)? = null) {
        _uiState.update { it.copy(emCarregamento = true) }
        viewModelScope.launch {
            aoSincronizar?.invoke()
            delay(500)
            recarregar()
            _uiState.update { it.copy(emCarregamento = false) }
        }
    }


    /**
     * Agenda uma nova consulta médica ou exame para o perfil ativo.
     * Persiste localmente em Room e sincroniza com o Firestore quando houver rede.
     */
    fun agendarConsulta(consulta: Consulta) {
        val perfil = perfilRepository.perfilAtivo
        consultaRepository.agendarConsulta(perfil, consulta)
        recarregar()
    }


    /**
     * Sincroniza todas as consultas agendadas com a aplicação de Calendário/Agenda nativa do telemóvel.
     */
    fun sincronizarComCalendario(plataforma: ServicosDispositivo) {
        val perfil = perfilRepository.perfilAtivo
        val consultas = consultaRepository.consultas(perfil)
        val agendaAtiva = configuracaoRepository?.agendaEAlarmesAtivos ?: true
        if (agendaAtiva) {
            plataforma.sincronizarConsultasNoCalendario(consultas)
            _uiState.update { it.copy(mensagemSucesso = "Consultas sincronizadas com a sua agenda!") }
        } else {
            plataforma.removerConsultasDoCalendario()
        }
    }
}

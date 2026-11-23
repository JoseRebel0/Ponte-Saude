@file:Suppress("SpellCheckingInspection")

package pt.ulsbe.pontesaude.ui.screens.inicio

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
import pt.ulsbe.pontesaude.data.repository.MedicacaoRepository
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.util.ServicosDispositivo
import kotlin.time.Duration.Companion.milliseconds

/**
 * Estado imutável da interface do Ecrã de Início ([EcraInicio]).
 *
 * @property perfil Dados clínicos e de identificação do utente ou pessoa cuidada ativa.
 * @property proximaConsulta Próxima consulta médica ou exame agendado (se existir).
 * @property medicamentosHoje Lista de medicamentos com tomas ativas programadas para o dia.
 * @property modoCuidadorAtivo Indica se o utente está no papel de cuidador a gerir um familiar.
 * @property pessoasCuidadas Lista de familiares/dependentes associados à conta do cuidador.
 * @property nomePessoaCuidada Nome resumido do familiar atualmente em visualização.
 * @property ultimaAtualizacaoFormatada Texto amigável da última sincronização com a cloud.
 * @property emCarregamento Indica se está a decorrer uma operação assíncrona de atualização.
 * @property mensagemErro Mensagem de erro descritiva em caso de falha de carregamento ou rede.
 */
data class InicioUiState(
    val perfil: Perfil? = null,
    val perfilSessao: Perfil? = null,
    val indicePerfilCuidado: Int = 0,
    val proximaConsulta: Consulta? = null,
    val medicamentosHoje: List<Medicamento> = emptyList(),
    val modoCuidadorAtivo: Boolean = false,
    val pessoasCuidadas: List<Perfil> = emptyList(),
    val nomePessoaCuidada: String = "",
    val ultimaAtualizacaoFormatada: String = "Hoje às 10:00",
    val emCarregamento: Boolean = false,
    val mensagemErro: String? = null,
)

/**
 * ViewModel responsável pela lógica de apresentação do Ecrã de Início.
 *
 * Gere a visualização do Cartão de Emergência Nacional (SNS), atalhos rápidos,
 * resumo de consultas iminentes e chamadas de emergência (112 e Linha SNS 24).
 */
class InicioViewModel(
    private val perfilRepository: PerfilRepository,
    private val consultaRepository: ConsultaRepository,
    private val medicacaoRepository: MedicacaoRepository,
    private val configuracaoRepository: ConfiguracaoRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InicioUiState())
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    init {
        atualizarEstado()
    }

    /**
     * Recarrega os dados do perfil ativo, consultas e medicamentos a partir dos repositórios.
     */
    fun atualizarEstado() {
        val perfil = perfilRepository.perfilAtivo
        val perfilSessao = perfilRepository.perfilSessao
        val indice = perfilRepository.indicePerfilCuidado
        val consultas = consultaRepository.consultas(perfil)
        val medicamentos = medicacaoRepository.medicamentos(perfil)
        val modoCuidador = perfilRepository.modoCuidador
        val cuidados = perfilRepository.pessoasCuidadas
        val textoAtualizacao = configuracaoRepository?.formatarUltimaAtualizacao() ?: "Hoje às 10:00"

        _uiState.update {
            it.copy(
                perfil = perfil,
                perfilSessao = perfilSessao,
                indicePerfilCuidado = indice,
                proximaConsulta = consultas.firstOrNull(),
                medicamentosHoje = medicamentos.filter { med -> med.ativo },
                modoCuidadorAtivo = modoCuidador,
                pessoasCuidadas = cuidados,
                nomePessoaCuidada = if (modoCuidador) perfil.nomeCurto else "",
                ultimaAtualizacaoFormatada = textoAtualizacao,
            )
        }
    }

    /**
     * Dispara o pull-to-refresh e sincroniza dados clínicos com a cloud.
     */
    fun dispararAtualizacao(aoSincronizar: (suspend () -> Unit)? = null) {
        _uiState.update { it.copy(emCarregamento = true) }
        viewModelScope.launch {
            delay(600.milliseconds) // Transição suave de carregamento
            aoSincronizar?.invoke()
            configuracaoRepository?.registarAtualizacao()
            atualizarEstado()
            _uiState.update { it.copy(emCarregamento = false) }
        }
    }

    /**
     * Inicia a marcação telefónica direta para o Número Europeu de Emergência (112).
     */
    fun ligarSOS(plataforma: ServicosDispositivo) {
        plataforma.ligar112()
    }

    /**
     * Inicia a marcação telefónica direta para a Linha SNS 24 (808 24 24 24).
     */
    fun ligarSaude24(plataforma: ServicosDispositivo) {
        plataforma.ligarSaude24()
    }

    /**
     * Define o perfil acompanhado ativo no ecrã de início.
     */
    fun definirPerfilCuidado(indice: Int) {
        perfilRepository.definirPerfilCuidado(indice)
        atualizarEstado()
    }

    /**
     * Alterna o modo cuidador no ecrã de início.
     */
    fun atualizarModoCuidador(ativo: Boolean) {
        perfilRepository.atualizarModoCuidador(ativo)
        atualizarEstado()
    }
}

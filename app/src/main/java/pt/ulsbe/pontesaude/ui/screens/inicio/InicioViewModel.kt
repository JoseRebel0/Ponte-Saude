package pt.ulsbe.pontesaude.ui.screens.inicio

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pt.ulsbe.pontesaude.data.repository.ConsultaRepository
import pt.ulsbe.pontesaude.data.repository.MedicacaoRepository
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.util.ServicosDispositivo

/**
 * Estado imutável da interface do Ecrã de Início ([EcraInicio]).
 *
 * @property perfil Dados clínicos e de identificação do utente ou pessoa cuidada ativa.
 * @property proximaConsulta Próxima consulta médica ou exame agendado (se existir).
 * @property medicamentosHoje Lista de medicamentos com tomas ativas programadas para o dia.
 * @property modoCuidadorAtivo Indica se o utente está no papel de cuidador a gerir um familiar.
 * @property pessoasCuidadas Lista de familiares/dependentes associados à conta do cuidador.
 * @property nomePessoaCuidada Nome resumido do familiar atualmente em visualização.
 * @property emCarregamento Indica se está a decorrer uma operação assíncrona de atualização.
 * @property mensagemErro Mensagem de erro descritiva em caso de falha de carregamento ou rede.
 */
data class InicioUiState(
    val perfil: Perfil? = null,
    val proximaConsulta: Consulta? = null,
    val medicamentosHoje: List<Medicamento> = emptyList(),
    val modoCuidadorAtivo: Boolean = false,
    val pessoasCuidadas: List<Perfil> = emptyList(),
    val nomePessoaCuidada: String = "",
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
        val consultas = consultaRepository.consultas(perfil)
        val medicamentos = medicacaoRepository.medicamentos(perfil)
        val modoCuidador = perfilRepository.modoCuidador
        val cuidados = perfilRepository.pessoasCuidadas

        _uiState.update {
            it.copy(
                perfil = perfil,
                proximaConsulta = consultas.firstOrNull(),
                medicamentosHoje = medicamentos.filter { med -> med.ativo },
                modoCuidadorAtivo = modoCuidador,
                pessoasCuidadas = cuidados,
                nomePessoaCuidada = if (modoCuidador) perfil.nomeCurto else "",
            )
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
}

package pt.ulsbe.pontesaude.ui.screens.medicacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.repository.MedicacaoRepository
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Perfil

/**
 * Estado imutável da interface do Ecrã de Medicação e Receitas ([EcraMedicacao]).
 *
 * @property perfil Perfil do utente com a respetiva lista de terapêuticas ativas.
 * @property medicamentos Lista completa de medicamentos prescritos e registados.
 * @property medicamentoSelecionado Fármaco atualmente em destaque para visualização ou edição.
 * @property aCarregarIa Indica se está a decorrer um pedido de simplificação de bula/termos à IA (Gemini).
 * @property explicacaoIa Resposta simplificada da IA explicada em linguagem sénior acessível.
 * @property mensagemErro Mensagem de erro em caso de falha de rede ou validação.
 * @property mensagemSucesso Mensagem de confirmação de registo ou atualização bem-sucedida.
 */
data class MedicacaoUiState(
    val perfil: Perfil? = null,
    val medicamentos: List<Medicamento> = emptyList(),
    val medicamentoSelecionado: Medicamento? = null,
    val aCarregarIa: Boolean = false,
    val explicacaoIa: String? = null,
    val mensagemErro: String? = null,
    val mensagemSucesso: String? = null,
)

/**
 * ViewModel responsável pela gestão do plano terapêutico e medicação diária.
 *
 * Suporta adição, edição, ativação/desativação de tomas, registo de fotografias das caixas
 * e esclarecimento de dúvidas sobre a posologia através de Inteligência Artificial generativa.
 */
class MedicacaoViewModel(
    private val medicacaoRepository: MedicacaoRepository,
    private val perfilRepository: PerfilRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicacaoUiState())
    val uiState: StateFlow<MedicacaoUiState> = _uiState.asStateFlow()

    init {
        recarregar()
    }

    /**
     * Recarrega a lista de medicamentos do perfil ativo a partir do repositório.
     */
    fun recarregar() {
        val perfil = perfilRepository.perfilAtivo
        val meds = medicacaoRepository.medicamentos(perfil)
        _uiState.update {
            it.copy(
                perfil = perfil,
                medicamentos = meds,
            )
        }
    }

    /**
     * Alterna o estado ativo de um medicamento (ativando ou pausando os respetivos alarmes diários).
     */
    fun alternarAtivo(id: Long, ativo: Boolean) {
        val perfil = perfilRepository.perfilAtivo
        medicacaoRepository.definirMedicamentoAtivo(perfil, id, ativo)
        recarregar()
    }

    /**
     * Adiciona um novo medicamento ao plano terapêutico do utente.
     */
    fun adicionarMedicamento(med: Medicamento) {
        val perfil = perfilRepository.perfilAtivo
        medicacaoRepository.adicionarMedicamento(perfil, med)
        recarregar()
    }

    /**
     * Atualiza as informações (nome, dosagem, horários, finalidade) de um medicamento existente.
     */
    fun atualizarMedicamento(med: Medicamento) {
        val perfil = perfilRepository.perfilAtivo
        medicacaoRepository.atualizarMedicamento(perfil, med)
        recarregar()
    }

    /**
     * Remove permanentemente um medicamento do plano terapêutico.
     */
    fun apagarMedicamento(id: Long) {
        val perfil = perfilRepository.perfilAtivo
        medicacaoRepository.apagarMedicamento(perfil, id)
        recarregar()
    }

    /**
     * Atualiza a fotografia associada à embalagem do medicamento (formato Base64).
     */
    fun atualizarFoto(id: Long, fotoBase64: String?) {
        val perfil = perfilRepository.perfilAtivo
        medicacaoRepository.atualizarFotoMedicamento(perfil, id, fotoBase64)
        recarregar()
    }

    /**
     * Consulta a IA do Google (Gemini) para explicar de forma simples para que serve o medicamento
     * e quais as precauções fundamentais a ter durante o tratamento.
     */
    fun pedirExplicacaoIa(nome: String) {
        _uiState.update { it.copy(aCarregarIa = true, explicacaoIa = null, mensagemErro = null) }
        viewModelScope.launch {
            val resultado = medicacaoRepository.explicarMedicamentoComIa(nome)
            resultado.onSuccess { explicacao ->
                _uiState.update { it.copy(aCarregarIa = false, explicacaoIa = explicacao) }
            }.onFailure { erro ->
                _uiState.update {
                    it.copy(
                        aCarregarIa = false,
                        mensagemErro = "Não foi possível obter a explicação da IA: ${erro.localizedMessage}",
                    )
                }
            }
        }
    }
}

package pt.ulsbe.pontesaude.ui.screens.apontamentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.repository.NotaRepository
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.data.repository.SaudeRepository
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil

/**
 * Estado imutável da interface do Ecrã de Apontamentos e Dúvidas ([EcraApontamentos]).
 *
 * @property perfil Perfil do utente com os respetivos apontamentos e lista de produtos.
 * @property notas Lista de notas de texto com identificação de autor (utente ou cuidador).
 * @property produtos Lista de outros produtos de saúde (suplementos, pomadas, compressas).
 * @property explicacaoIa Explicação simplificada do termo médico pela IA (se pesquisado).
 * @property aCarregarIa Indica se está a decorrer tradução de termo médico via IA.
 * @property mensagemErro Mensagem de erro em caso de falha de gravação ou sincronização.
 * @property mensagemSucesso Mensagem de confirmação de nota guardada ou apagada.
 */
data class ApontamentosUiState(
    val perfil: Perfil? = null,
    val notas: List<Nota> = emptyList(),
    val produtos: List<String> = emptyList(),
    val explicacaoIa: String? = null,
    val termoIa: String? = null,
    val aCarregarIa: Boolean = false,
    val mensagemErro: String? = null,
    val mensagemSucesso: String? = null,
)

/**
 * ViewModel responsável pela gestão de dúvidas, sintomas e notas a colocar ao médico na consulta.
 *
 * Permite tanto ao próprio utente como ao cuidador formal/informal adicionar observações
 * diárias e gerir a lista de produtos de farmácia e parafarmácia em uso.
 */
class ApontamentosViewModel(
    private val notaRepository: NotaRepository,
    private val perfilRepository: PerfilRepository,
    private val saudeRepository: SaudeRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApontamentosUiState())
    val uiState: StateFlow<ApontamentosUiState> = _uiState.asStateFlow()

    init {
        recarregar()
    }

    /**
     * Recarrega a lista de notas e produtos do perfil ativo a partir do repositório.
     * Limpa respostas residuais de IA se o perfil ativo tiver mudado.
     */
    fun recarregar() {
        val perfil = perfilRepository.perfilAtivo
        val perfilMudou = _uiState.value.perfil?.id != perfil.id
        val notas = notaRepository.notas(perfil)
        val produtos = notaRepository.produtos(perfil)
        _uiState.update {
            it.copy(
                perfil = perfil,
                notas = notas,
                produtos = produtos,
                explicacaoIa = if (perfilMudou) null else it.explicacaoIa,
                termoIa = if (perfilMudou) null else it.termoIa,
                aCarregarIa = if (perfilMudou) false else it.aCarregarIa,
                mensagemErro = if (perfilMudou) null else it.mensagemErro,
            )
        }
    }

    /**
     * Limpa a explicação clínica da IA atualmente em exibição.
     */
    fun limparExplicacaoIa() {
        _uiState.update { it.copy(explicacaoIa = null, termoIa = null, aCarregarIa = false, mensagemErro = null) }
    }

    /**
     * Marca a explicação atual como já consumida/guardada no histórico do perfil ativo.
     */
    fun consumirExplicacaoIa() {
        _uiState.update { it.copy(termoIa = null) }
    }

    /**
     * Adiciona uma nova nota clínica ou dúvida registada pelo utente para a próxima consulta.
     *
     * @param texto Conteúdo textual da nota registada.
     * @param autor Identificação de quem efetuou o registo (sempre atribuído ao utente).
     */
    fun adicionarNota(texto: String, autor: String = "Utente") {
        val limpo = texto.trim()
        if (limpo.isBlank()) return
        val perfil = perfilRepository.perfilAtivo
        notaRepository.adicionarNota(perfil, Nota(limpo, "Utente"))
        recarregar()
    }

    /**
     * Remove uma nota existente pela sua posição na lista.
     */
    fun apagarNota(indice: Int) {
        val perfil = perfilRepository.perfilAtivo
        notaRepository.apagarNota(perfil, indice)
        recarregar()
    }

    /**
     * Adiciona um produto de parafarmácia, penso ou suplemento à lista do utente.
     */
    fun adicionarProduto(produto: String) {
        val limpo = produto.trim()
        if (limpo.isBlank()) return
        val perfil = perfilRepository.perfilAtivo
        notaRepository.adicionarProduto(perfil, limpo)
        recarregar()
    }

    /**
     * Remove um produto da lista do utente.
     */
    fun apagarProduto(produto: String) {
        val perfil = perfilRepository.perfilAtivo
        notaRepository.apagarProduto(perfil, produto)
        recarregar()
    }

    /**
     * Traduz e simplifica um termo médico ou diagnóstico através da IA (Gemini).
     */
    fun traduzirTermo(termo: String) {
        val termoLimpo = termo.trim()
        if (termoLimpo.isBlank() || saudeRepository == null) return
        val perfilOrigem = perfilRepository.perfilAtivo
        _uiState.update {
            it.copy(
                aCarregarIa = true,
                explicacaoIa = null,
                termoIa = termoLimpo,
                mensagemErro = null,
                perfil = perfilOrigem,
            )
        }
        viewModelScope.launch {
            val res = saudeRepository.explicarTermoMedicoComIa(termoLimpo)
            res.onSuccess { explicacao ->
                if (perfilRepository.perfilAtivo.id == perfilOrigem.id) {
                    _uiState.update { it.copy(aCarregarIa = false, explicacaoIa = explicacao) }
                }
            }.onFailure { erro ->
                if (perfilRepository.perfilAtivo.id == perfilOrigem.id) {
                    _uiState.update {
                        it.copy(
                            aCarregarIa = false,
                            mensagemErro = "Erro ao simplificar termo: ${erro.localizedMessage}",
                        )
                    }
                }
            }
        }
    }
}

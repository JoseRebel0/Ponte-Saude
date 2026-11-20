package pt.ulsbe.pontesaude.ui.screens.saude

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.data.repository.SaudeRepository
import pt.ulsbe.pontesaude.model.ModuloSaude
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.RecompensaSeguranca
import pt.ulsbe.pontesaude.model.VideoBibliotecaSaude
import pt.ulsbe.pontesaude.model.VoucherResgatado
import pt.ulsbe.pontesaude.util.ServicosDispositivo

/**
 * Estado imutável da interface do Ecrã de Saúde e Literacia ([EcraSaude]).
 *
 * @property perfil Perfil do utente com a sua pontuação e estado do Clube de Segurança.
 * @property pontosSeguranca Saldo acumulado de pontos obtidos por boas práticas e leitura de módulos.
 * @property modulosConcluidos Conjunto de identificadores de módulos interativos concluídos.
 * @property modulosOuvidos Conjunto de identificadores de módulos escutados via voz TTS.
 * @property videosAssistidos Conjunto de identificadores de vídeos educativos visualizados.
 * @property modulos Catálogo completo de módulos de literacia em saúde disponíveis.
 * @property videos Lista de vídeos informativos da biblioteca ULSBE/SNS.
 * @property recompensas Catálogo de vouchers e benefícios disponíveis para resgate.
 * @property vouchers Histórico de vouchers já resgatados pelo utente (com código `SEC-XXXX`).
 * @property moduloAbertoId Identificador do módulo atualmente aberto em diálogo ou ecrã detalhado.
 * @property aTraduzirTermoIa Indica se está a decorrer tradução de termo médico via IA.
 * @property explicacaoIa Explicação simplificada do termo médico retornada pela IA.
 * @property mensagemErro Mensagem de erro em caso de falha de operação.
 * @property mensagemSucesso Mensagem de confirmação de resgate de recompensa ou ação concluída.
 */
data class SaudeUiState(
    val perfil: Perfil? = null,
    val pontosSeguranca: Int = 50,
    val modulosConcluidos: List<String> = emptyList(),
    val modulosOuvidos: List<String> = emptyList(),
    val videosAssistidos: List<String> = emptyList(),
    val modulos: List<ModuloSaude> = emptyList(),
    val videos: List<VideoBibliotecaSaude> = emptyList(),
    val recompensas: List<RecompensaSeguranca> = emptyList(),
    val vouchers: List<VoucherResgatado> = emptyList(),
    val moduloAbertoId: String? = null,
    val aTraduzirTermoIa: Boolean = false,
    val explicacaoIa: String? = null,
    val mensagemErro: String? = null,
    val mensagemSucesso: String? = null,
)

/**
 * ViewModel responsável pela literacia em saúde, gamificação e programa "Clube de Segurança".
 *
 * Recompensa o utente pela conclusão de práticas preventivas, visualização de vídeos clínicos,
 * leitura assistida por voz (TTS) e permite a conversão de pontos em vouchers institucionais.
 */
class SaudeViewModel(
    private val saudeRepository: SaudeRepository,
    private val perfilRepository: PerfilRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaudeUiState())
    val uiState: StateFlow<SaudeUiState> = _uiState.asStateFlow()

    init {
        recarregar()
    }

    /**
     * Recarrega todos os pontos, módulos, vídeos e histórico de vouchers a partir do repositório.
     */
    fun recarregar() {
        val perfil = perfilRepository.perfilAtivo
        saudeRepository.carregarDadosPerfil(perfil.id)
        _uiState.update {
            it.copy(
                perfil = perfil,
                pontosSeguranca = saudeRepository.pontosSeguranca,
                modulosConcluidos = saudeRepository.modulosConcluidos.toList(),
                modulosOuvidos = saudeRepository.modulosOuvidos.toList(),
                videosAssistidos = saudeRepository.videosAssistidos.toList(),
                modulos = saudeRepository.modulosSaude.toList(),
                videos = saudeRepository.videosBiblioteca.toList(),
                recompensas = saudeRepository.catalogoRecompensas.toList(),
                vouchers = saudeRepository.vouchersResgatados.toList(),
            )
        }
    }

    /**
     * Abre ou fecha o detalhe de um módulo de literacia interativo.
     */
    fun abrirModulo(id: String?) {
        _uiState.update { it.copy(moduloAbertoId = id) }
    }

    /**
     * Alterna a conclusão de um módulo educativo, creditando ou debitando os pontos correspondentes.
     */
    fun alternarConclusaoModulo(id: String) {
        val perfil = perfilRepository.perfilAtivo
        saudeRepository.alternarModuloComPontos(id, perfil.id)
        recarregar()
    }

    /**
     * Recompensa a audição do módulo via voz TTS com 15 pontos de segurança adicionais.
     */
    fun ouvirModulo(id: String, texto: String, plataforma: ServicosDispositivo) {
        plataforma.falar(texto)
        val perfil = perfilRepository.perfilAtivo
        val pontosGanhos = saudeRepository.premiarOuvirModulo(id, perfil.id)
        if (pontosGanhos > 0) {
            _uiState.update { it.copy(mensagemSucesso = "Ganhou +$pontosGanhos pontos de segurança!") }
        }
        recarregar()
    }

    /**
     * Recompensa a visualização de um vídeo clínico com 25 pontos de segurança.
     */
    fun verVideo(idVideo: String) {
        val perfil = perfilRepository.perfilAtivo
        val pontosGanhos = saudeRepository.premiarVerVideo(idVideo, perfil.id)
        if (pontosGanhos > 0) {
            _uiState.update { it.copy(mensagemSucesso = "Parabéns! Ganhou +$pontosGanhos pontos por ver o vídeo.") }
        }
        recarregar()
    }

    /**
     * Resgata um voucher institucional debitando o custo em pontos da conta do utente.
     */
    fun resgatarVoucher(recompensa: RecompensaSeguranca) {
        val perfil = perfilRepository.perfilAtivo
        val voucher = saudeRepository.resgatarRecompensa(recompensa, perfil.id)
        if (voucher != null) {
            _uiState.update {
                it.copy(
                    mensagemSucesso = "Voucher resgatado com sucesso! Código: ${voucher.codigo}",
                )
            }
            recarregar()
        } else {
            _uiState.update {
                it.copy(mensagemErro = "Pontos insuficientes para resgatar este voucher.")
            }
        }
    }

    /**
     * Simplifica a definição de termos clínicos complexos com auxílio do Gemini AI.
     */
    fun explicarTermoComIa(termo: String) {
        if (termo.isBlank()) return
        _uiState.update { it.copy(aTraduzirTermoIa = true, explicacaoIa = null, mensagemErro = null) }
        viewModelScope.launch {
            val resultado = saudeRepository.explicarTermoMedicoComIa(termo)
            resultado.onSuccess { explicacao ->
                _uiState.update { it.copy(aTraduzirTermoIa = false, explicacaoIa = explicacao) }
            }.onFailure { erro ->
                _uiState.update {
                    it.copy(
                        aTraduzirTermoIa = false,
                        mensagemErro = "Não foi possível traduzir o termo: ${erro.localizedMessage}",
                    )
                }
            }
        }
    }
}

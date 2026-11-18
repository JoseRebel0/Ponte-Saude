package pt.ulsbe.pontesaude.ui.screens.definicoes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pt.ulsbe.pontesaude.data.repository.ConfiguracaoRepository
import pt.ulsbe.pontesaude.data.repository.MedicacaoRepository
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.util.ServicosDispositivo

/**
 * Estado imutável da interface do Ecrã de Definições e Acessibilidade ([EcraDefinicoes]).
 *
 * @property perfil Perfil do utente com as preferências do utilizador.
 * @property textoGrande Ativa tipografia de grande escala em toda a aplicação (acessibilidade sénior).
 * @property altoContraste Ativa esquema de cores de contraste aumentado.
 * @property reduzirMovimento Desativa transições complexas e animações para evitar tonturas e facilitar o foco.
 * @property notificacoesAtivas Controla se a app pode disparar notificações locais no sistema Android.
 * @property somVozAtivo Permite ligar ou silenciar a leitura automática por voz (TTS).
 * @property agendaEAlarmesAtivos Controla a sincronização de alarmes de relógio e agenda de consultas.
 * @property modoCuidador Ativa a interface de gestão de dependentes/familiares associados.
 * @property pessoasCuidadas Lista de familiares sob a alçada do cuidador atual.
 * @property mensagemSucesso Mensagem informativa apresentada após salvar alterações.
 */
data class DefinicoesUiState(
    val perfil: Perfil? = null,
    val textoGrande: Boolean = false,
    val altoContraste: Boolean = false,
    val reduzirMovimento: Boolean = false,
    val notificacoesAtivas: Boolean = true,
    val somVozAtivo: Boolean = true,
    val agendaEAlarmesAtivos: Boolean = true,
    val modoCuidador: Boolean = false,
    val pessoasCuidadas: List<Perfil> = emptyList(),
    val mensagemSucesso: String? = null,
)

/**
 * ViewModel responsável pelas preferências de acessibilidade, modo cuidador e encerramento de sessão.
 *
 * Permite adaptar a interface para pessoas com dificuldades visuais ou motoras e gerir
 * a relação entre cuidadores informais e os utentes assistidos.
 */
class DefinicoesViewModel(
    private val configuracaoRepository: ConfiguracaoRepository,
    private val perfilRepository: PerfilRepository,
    private val medicacaoRepository: MedicacaoRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DefinicoesUiState())
    val uiState: StateFlow<DefinicoesUiState> = _uiState.asStateFlow()

    init {
        recarregar()
    }

    /**
     * Sincroniza o estado da interface com as preferências guardadas em SharedPreferences e Room.
     */
    fun recarregar() {
        val perfil = perfilRepository.perfilAtivo
        _uiState.update {
            it.copy(
                perfil = perfil,
                textoGrande = configuracaoRepository.textoGrande,
                altoContraste = configuracaoRepository.altoContraste,
                reduzirMovimento = configuracaoRepository.reduzirMovimento,
                notificacoesAtivas = configuracaoRepository.notificacoesAtivas,
                somVozAtivo = configuracaoRepository.somVozAtivo,
                agendaEAlarmesAtivos = configuracaoRepository.agendaEAlarmesAtivos,
                modoCuidador = perfilRepository.modoCuidador,
                pessoasCuidadas = perfilRepository.pessoasCuidadas,
            )
        }
    }

    /**
     * Alterna a preferência de texto grande e acessibilidade tipográfica.
     */
    fun definirTextoGrande(ativo: Boolean) {
        configuracaoRepository.atualizarTextoGrande(ativo)
        recarregar()
    }

    /**
     * Alterna o modo de alto contraste.
     */
    fun definirAltoContraste(ativo: Boolean) {
        configuracaoRepository.atualizarAltoContraste(ativo)
        recarregar()
    }

    /**
     * Alterna a redução de animações e movimentos no Jetpack Compose.
     */
    fun definirReduzirMovimento(ativo: Boolean) {
        configuracaoRepository.atualizarReduzirMovimento(ativo)
        recarregar()
    }

    /**
     * Alterna a autorização de emissão de notificações locais.
     */
    fun definirNotificacoes(ativas: Boolean) {
        configuracaoRepository.atualizarNotificacoesAtivas(ativas)
        recarregar()
    }

    /**
     * Liga ou desliga o motor de síntese de voz (TTS) para leitura assistida.
     */
    fun definirSomVoz(ativo: Boolean) {
        configuracaoRepository.atualizarSomVoz(ativo)
        recarregar()
    }

    /**
     * Ativa ou desativa a integração com os alarmes de medicação e calendário nativo.
     */
    fun definirAgendaEAlarmes(ativos: Boolean, plataforma: ServicosDispositivo?) {
        configuracaoRepository.atualizarAgendaEAlarmesAtivos(ativos)
        val perfil = perfilRepository.perfilAtivo
        if (medicacaoRepository != null) {
            val meds = medicacaoRepository.medicamentos(perfil)
            if (!ativos) {
                plataforma?.cancelarTodosOsAlarmes(meds)
            } else {
                meds.filter { it.ativo }.forEach {
                    medicacaoRepository.definirMedicamentoAtivo(perfil, it.id, true)
                }
            }
        }
        recarregar()
    }

    /**
     * Termina a sessão autenticada com a Chave Móvel Digital (CMD) e regressa ao ecrã inicial de login.
     */
    fun terminarSessaoCMD() {
        perfilRepository.terminarSessaoCMD()
    }
}

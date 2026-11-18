package pt.ulsbe.pontesaude.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.repository.PerfilRepository
import pt.ulsbe.pontesaude.model.Perfil

/**
 * Estado imutável da interface do Ecrã de Autenticação CMD ([EcraLoginCMD]).
 *
 * @property perfisDisponiveis Lista de perfis de utentes e cuidadores disponíveis para autenticação.
 * @property perfilSelecionadoId Identificador do perfil pré-selecionado no ecrã de boas-vindas.
 * @property aCriarPerfil Indica se o gerador de perfis fictícios portugueses está em execução.
 * @property aApagarPerfis Indica se a limpeza de dados de teste está em curso.
 * @property emProgresso Indica se a validação do 2º fator por SMS da CMD está em processamento.
 * @property textoProgresso Mensagem de progresso apresentada no diálogo de ligação à Autenticação.gov.
 * @property mensagemErro Mensagem de erro em caso de credenciais inválidas ou falha de rede.
 * @property mensagemSucesso Mensagem de confirmação após criação, limpeza ou autenticação.
 */
data class LoginCMDUiState(
    val perfisDisponiveis: List<Perfil> = emptyList(),
    val perfilSelecionadoId: String = "1",
    val aCriarPerfil: Boolean = false,
    val aApagarPerfis: Boolean = false,
    val emProgresso: Boolean = false,
    val textoProgresso: String = "A contactar Chave Móvel Digital...",
    val mensagemErro: String? = null,
    val mensagemSucesso: String? = null,
)

/**
 * ViewModel responsável pela simulação da autenticação oficial com a Chave Móvel Digital (CMD).
 *
 * Implementa a validação de número de telemóvel/NIF, envio simulado de código de segurança por SMS,
 * seleção de utente da base de dados e geração de perfis de teste portugueses com dados realistas.
 */
class LoginCMDViewModel(
    private val perfilRepository: PerfilRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginCMDUiState())
    val uiState: StateFlow<LoginCMDUiState> = _uiState.asStateFlow()

    init {
        recarregar()
    }

    /**
     * Atualiza a lista de perfis disponíveis carregados do Room ou Firestore.
     */
    fun recarregar() {
        val perfis = perfilRepository.todosOsPerfis
        _uiState.update {
            it.copy(
                perfisDisponiveis = perfis,
                perfilSelecionadoId = it.perfilSelecionadoId.ifBlank { perfis.firstOrNull()?.id ?: "1" },
            )
        }
    }

    /**
     * Define o perfil ativo a autenticar na sessão.
     */
    fun selecionarPerfil(id: String) {
        _uiState.update { it.copy(perfilSelecionadoId = id) }
    }

    /**
     * Cria um novo perfil aleatório com nomes, NIFs e medicamentos aprovados pelo Infarmed em Portugal.
     * Guarda o registo no Room e na Cloud Firestore.
     */
    fun criarPerfilAleatorio() {
        _uiState.update { it.copy(aCriarPerfil = true, mensagemSucesso = null, mensagemErro = null) }
        viewModelScope.launch {
            try {
                val criado = perfilRepository.criarPerfilAleatorio()
                _uiState.update {
                    it.copy(
                        aCriarPerfil = false,
                        perfilSelecionadoId = criado.id,
                        mensagemSucesso = "Perfil '${criado.nome}' criado com sucesso no Firebase!",
                    )
                }
                recarregar()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        aCriarPerfil = false,
                        mensagemErro = "Erro ao criar perfil: ${e.localizedMessage}",
                    )
                }
            }
        }
    }

    /**
     * Remove todos os perfis gerados aleatoriamente, preservando os perfis essenciais da demonstração.
     */
    fun limparPerfisDemonstracao() {
        _uiState.update { it.copy(aApagarPerfis = true, mensagemSucesso = null, mensagemErro = null) }
        viewModelScope.launch {
            try {
                val apagados = perfilRepository.apagarPerfisAleatorios()
                _uiState.update {
                    it.copy(
                        aApagarPerfis = false,
                        mensagemSucesso = "Foram removidos $apagados perfis de teste.",
                    )
                }
                recarregar()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        aApagarPerfis = false,
                        mensagemErro = "Erro ao limpar perfis: ${e.localizedMessage}",
                    )
                }
            }
        }
    }

    /**
     * Conclui o processo de autenticação CMD e inicia a sessão para o perfil selecionado.
     */
    fun concluirAutenticacao(aoSucesso: (String) -> Unit) {
        val id = _uiState.value.perfilSelecionadoId
        perfilRepository.iniciarSessaoCMD(id)
        aoSucesso(id)
    }
}

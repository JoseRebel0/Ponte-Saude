package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Sessão no Firebase Authentication da Ponte Saúde.
 *
 * Usa autenticação ANÓNIMA: cada instalação recebe um UID aleatório e impossível de adivinhar,
 * que passa a ser o "dono" (`donoUid`) dos dados guardados no Firestore. Nada do que está no
 * Firebase permite descobrir quem é a pessoa: o nome, o NIF, o telemóvel, etc. ficam só no
 * telemóvel (Room).
 *
 * O login "Chave Móvel Digital" da app é uma simulação visual e não cria nem altera contas.
 *
 * Limitações conhecidas:
 * - O UID está ligado à instalação. Se a app for desinstalada ou os dados limpos, o acesso aos
 *   dados da cloud perde-se (para recuperar noutro aparelho seria preciso login real por SMS).
 * - Terminar a sessão CMD NÃO faz signOut do Firebase, porque isso perderia o UID para sempre.
 *
 * Requer "Anonymous" ativado em Firebase Console → Authentication → Sign-in method.
 */
object AutenticadorFirebaseCMD {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val uidAtual: String?
        get() = auth.currentUser?.uid

    val estaAutenticado: Boolean
        get() = auth.currentUser != null

    /**
     * Garante que existe uma sessão anónima e devolve o UID (ou null se não houver rede/permissão).
     */
    suspend fun garantirSessao(): String? = withContext(Dispatchers.IO) {
        auth.currentUser?.uid
            ?: runCatching { auth.signInAnonymously().await().user?.uid }.getOrNull()
    }

    /**
     * Mantida por compatibilidade com o ecrã de login simulado: o identificador introduzido
     * (telemóvel/NIF) NÃO é enviado para o Firebase.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun autenticarComCMD(identificador: String): String? = garantirSessao()

    /**
     * Sinónimo de [garantirSessao] (o identificador é ignorado).
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun criarOuObterConta(identificador: String): String? = garantirSessao()

    /**
     * Terminar a sessão CMD é apenas local. Não se faz signOut do Firebase (perderia o UID anónimo).
     */
    fun terminarSessao() {
        // Intencionalmente vazio.
    }
}

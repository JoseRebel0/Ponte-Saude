package pt.ulsbe.pontesaude.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Autenticador Firebase baseado em Autenticação Anónima por instalação/dispositivo.
 *
 * Cada instalação da aplicação obtém um UID anónimo único e persistente no Firebase Auth.
 * Esse UID é utilizado como "donoUid" dos perfis clínicos no Firestore, garantindo que
 * cada utilizador acede exclusivamente aos perfis criados no seu telemóvel.
 */
object AutenticadorFirebaseCMD {
    private const val TAG = "AutenticadorFirebase"
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val uidAtual: String?
        get() = auth.currentUser?.uid

    val estaAutenticado: Boolean
        get() = auth.currentUser != null

    /**
     * Garante que existe uma sessão anónima ativa no Firebase Auth.
     * Se o utilizador já estiver autenticado nesta instalação, reutiliza o UID existente.
     * Caso contrário, autentica anonimamente no Firebase.
     */
    suspend fun garantirSessaoAnonima(): String = withContext(Dispatchers.IO) {
        val userAtual = auth.currentUser
        if (userAtual != null) {
            return@withContext userAtual.uid
        }
        try {
            val res = auth.signInAnonymously().await()
            val uid = res.user?.uid ?: auth.currentUser?.uid ?: ""
            Log.d(TAG, "Sessão anónima iniciada com sucesso. UID: $uid")
            uid
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao autenticar anonimamente no Firebase", e)
            auth.currentUser?.uid ?: ""
        }
    }

    /**
     * Mapeia credenciais virtuais (mantido para compatibilidade, sem envio de email/password).
     */
    fun obterCredenciaisVirtuais(identificador: String): Pair<String, String> {
        val idLimpo = identificador.replace(Regex("[^0-9a-zA-Z]"), "").lowercase().ifBlank { "utente_sns" }
        return Pair("cmd_${idLimpo}@pontesaude.ulsbe.pt", "PonteSaude_CMD_${idLimpo}#2026")
    }

    /**
     * Garante a sessão anónima do Firebase e retorna o UID do dispositivo.
     */
    suspend fun criarOuObterConta(identificador: String = ""): String = withContext(Dispatchers.IO) {
        garantirSessaoAnonima()
    }

    /**
     * Autentica no Firebase Auth via sessão anónima da instalação.
     */
    suspend fun autenticarComCMD(identificador: String = ""): String = withContext(Dispatchers.IO) {
        garantirSessaoAnonima()
    }

    /**
     * Na autenticação anónima por instalação, não apagamos a conta Auth do Firebase,
     * pois a conta pertence ao dispositivo e serve de âncora a todos os dados.
     */
    suspend fun eliminarContaAuth(identificador: String = "") = withContext(Dispatchers.IO) {
        // Intencionalmente no-op: a sessão anónima da instalação é preservada.
    }

    /**
     * O término de sessão na app é apenas local (limpar estado de sessão na UI).
     * Não chamamos auth.signOut(), para preservar o UID anónimo da instalação
     * e não perder o acesso aos documentos no Firestore.
     */
    fun terminarSessao() {
        // Sessão Firebase Auth anónima mantida no dispositivo.
    }
}

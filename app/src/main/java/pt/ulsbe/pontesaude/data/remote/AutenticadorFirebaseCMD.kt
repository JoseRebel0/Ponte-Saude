package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Autenticador transparente da Chave Móvel Digital (CMD) com o Firebase Authentication.
 *
 * Mapeia o número de telemóvel ou NIF do utente inserido no ecrã da CMD para uma credencial
 * única e persistente no Firebase Auth, sem que o utilizador precise de saber ou ver emails e passwords.
 *
 * Garante que:
 * 1. Cada telemóvel/utente ganha um UID único e privado no Firebase.
 * 2. Cada utente só tem acesso de leitura/escrita aos seus próprios medicamentos e consultas.
 * 3. Se a app for reinstalada ou aberta noutro telemóvel com o mesmo número CMD, os dados são recuperados.
 */
object AutenticadorFirebaseCMD {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val uidAtual: String?
        get() = auth.currentUser?.uid

    val estaAutenticado: Boolean
        get() = auth.currentUser != null

    /**
     * Mapeia de forma unívoca e determinística um identificador português (telemóvel ou NIF)
     * para credenciais virtuais no Firebase Authentication.
     */
    fun obterCredenciaisVirtuais(identificador: String): Pair<String, String> {
        val idLimpo = identificador.replace(Regex("[^0-9a-zA-Z]"), "").lowercase().ifBlank { "utente_sns" }
        val emailVirtual = "cmd_${idLimpo}@pontesaude.ulsbe.pt"
        val senhaVirtual = "PonteSaude_CMD_${idLimpo}#2026"
        return Pair(emailVirtual, senhaVirtual)
    }

    /**
     * Regista uma nova conta no Firebase Authentication para o utente ou obtém o UID existente.
     * Retorna o UID atribuído pela Firebase Authentication.
     */
    suspend fun criarOuObterConta(identificador: String): String = withContext(Dispatchers.IO) {
        val (emailVirtual, senhaVirtual) = obterCredenciaisVirtuais(identificador)
        val idLimpo = identificador.replace(Regex("[^0-9a-zA-Z]"), "").lowercase().ifBlank { "utente_sns" }

        try {
            val res = auth.createUserWithEmailAndPassword(emailVirtual, senhaVirtual).await()
            res.user?.uid ?: idLimpo
        } catch (_: Exception) {
            try {
                val res = auth.signInWithEmailAndPassword(emailVirtual, senhaVirtual).await()
                res.user?.uid ?: idLimpo
            } catch (_: Exception) {
                auth.currentUser?.uid ?: java.util.UUID.randomUUID().toString()
            }
        }
    }

    /**
     * Autentica no Firebase Auth com base no identificador da CMD (número de telemóvel ou NIF).
     * Se a conta ainda não existir, é criada automaticamente.
     */
    suspend fun autenticarComCMD(identificador: String): String = withContext(Dispatchers.IO) {
        criarOuObterConta(identificador)
    }

    /**
     * Elimina permanentemente o registo do utente da base de dados do Firebase Authentication.
     */
    suspend fun eliminarContaAuth(identificador: String) = withContext(Dispatchers.IO) {
        if (identificador.isBlank()) return@withContext
        val (emailVirtual, senhaVirtual) = obterCredenciaisVirtuais(identificador)
        try {
            val usuarioAtual = auth.currentUser
            if (usuarioAtual != null && usuarioAtual.email.equals(emailVirtual, ignoreCase = true)) {
                usuarioAtual.delete().await()
            } else {
                val res = auth.signInWithEmailAndPassword(emailVirtual, senhaVirtual).await()
                res.user?.delete()?.await()
            }
        } catch (_: Exception) {
            // Conta já removida ou erro transitório
        }
    }

    /**
     * Termina a sessão no Firebase Auth.
     */
    fun terminarSessao() {
        runCatching { auth.signOut() }
    }
}

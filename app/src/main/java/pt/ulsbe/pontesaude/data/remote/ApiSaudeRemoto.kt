package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.ulsbe.pontesaude.model.ModuloSaude
import pt.ulsbe.pontesaude.model.RecompensaSeguranca
import pt.ulsbe.pontesaude.model.TipoModulo
import pt.ulsbe.pontesaude.model.VideoBibliotecaSaude
import pt.ulsbe.pontesaude.model.VoucherResgatado

/**
 * Módulo remoto responsável pelo catálogo de literacia em saúde (módulos educativos, vídeos)
 * e pelo sistema de gamificação e recompensas do Clube de Segurança no Cloud Firestore.
 */
class ApiSaudeRemoto(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * Obtém da base de dados Firestore os módulos informativos e educativos de saúde.
     */
    suspend fun obterModulosSaude(): List<ModuloSaude> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = db.collection("modulos_saude").get().await()
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val titulo = doc.getString("titulo") ?: return@mapNotNull null
                val subtitulo = doc.getString("subtitulo") ?: ""
                val icone = doc.getString("icone") ?: "info"
                val tipoStr = doc.getString("tipo") ?: "INFO"
                val tipo = runCatching { TipoModulo.valueOf(tipoStr) }.getOrDefault(TipoModulo.INFO)
                @Suppress("UNCHECKED_CAST")
                val conteudos = doc.get("conteudos") as? List<String> ?: emptyList()
                val fala = doc.getString("fala") ?: conteudos.joinToString(" ")
                ModuloSaude(id, titulo, subtitulo, icone, tipo, conteudos, fala)
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Obtém da base de dados Firestore a biblioteca multimédia de vídeos de literacia.
     */
    suspend fun obterVideosBiblioteca(): List<VideoBibliotecaSaude> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = db.collection("videos_biblioteca").get().await()
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val titulo = doc.getString("titulo") ?: return@mapNotNull null
                val descricao = doc.getString("descricao") ?: ""
                val duracao = doc.getString("duracao") ?: "3 min"
                val url = doc.getString("urlYoutube") ?: ""
                val pontos = doc.getLong("pontos")?.toInt() ?: 30
                VideoBibliotecaSaude(id, titulo, descricao, duracao, url, pontos)
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Obtém da base de dados Firestore o catálogo de prémios e recompensas do Clube de Segurança.
     */
    suspend fun obterCatalogoRecompensas(): List<RecompensaSeguranca> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = db.collection("recompensas").get().await()
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val titulo = doc.getString("titulo") ?: return@mapNotNull null
                val descricao = doc.getString("descricao") ?: ""
                val pontos = doc.getLong("pontos")?.toInt() ?: 100
                val icone = doc.getString("icone") ?: "star"
                val local = doc.getString("localLevantamento") ?: "ULSBE"
                RecompensaSeguranca(id, titulo, descricao, pontos, icone, local)
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Atualiza os pontos acumulados, módulos concluídos, vídeos assistidos e vouchers resgatados
     * de um utente no Cloud Firestore.
     */
    suspend fun atualizarDadosRecompensas(
        idPerfil: String,
        pontos: Int,
        modulosConcluidos: List<String>? = null,
        modulosOuvidos: List<String>? = null,
        videosAssistidos: List<String>? = null,
        vouchers: List<VoucherResgatado>? = null,
    ) = withContext(Dispatchers.IO) {
        val doc = db.collection("perfis").document(idPerfil)
        doc.update("pontosSeguranca", pontos).await()
        modulosConcluidos?.let { doc.update("modulosConcluidos", it).await() }
        modulosOuvidos?.let { doc.update("modulosOuvidos", it).await() }
        videosAssistidos?.let { doc.update("videosAssistidos", it).await() }
        vouchers?.let { vList ->
            val dtos = vList.map { v ->
                VoucherResgatadoDtoRemoto(
                    id = v.id,
                    idRecompensa = v.idRecompensa,
                    titulo = v.titulo,
                    codigo = v.codigo,
                    dataResgate = v.dataResgate,
                    localLevantamento = v.localLevantamento,
                    pontosUsados = v.pontosUsados,
                )
            }
            doc.update("vouchersResgatados", dtos).await()
        }
    }
}

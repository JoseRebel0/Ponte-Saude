package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.ulsbe.pontesaude.model.Consulta

/**
 * Módulo remoto responsável pelas operações de consultas médicas e listas de preparação no Cloud Firestore.
 */
class ApiConsultasRemoto(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * Guarda ou atualiza os detalhes de uma consulta na subcoleção do perfil no Firestore.
     */
    suspend fun guardarConsulta(idPerfil: String, consulta: Consulta) = withContext(Dispatchers.IO) {
        val dados = hashMapOf(
            "id" to consulta.id,
            "dia" to consulta.dia,
            "mes" to consulta.mes,
            "titulo" to consulta.titulo,
            "localizacao" to consulta.localizacao,
            "tipo" to consulta.tipo,
            "jejum" to consulta.jejum,
        )
        db.collection("perfis")
            .document(idPerfil)
            .collection("consultas")
            .document(consulta.id.toString())
            .set(dados, SetOptions.merge())
            .await()
    }

    /**
     * Remove uma consulta da subcoleção do perfil no Firestore.
     */
    suspend fun removerConsulta(idPerfil: String, idConsulta: Int) = withContext(Dispatchers.IO) {
        db.collection("perfis")
            .document(idPerfil)
            .collection("consultas")
            .document(idConsulta.toString())
            .delete()
            .await()
    }

    /**
     * Atualiza os itens selecionados na checklist de preparação da consulta no Firestore.
     */
    suspend fun atualizarChecklistConsulta(
        idPerfil: String,
        idConsulta: Int,
        marcados: List<String>,
    ) = withContext(Dispatchers.IO) {
        db.collection("perfis")
            .document(idPerfil)
            .collection("consultas")
            .document(idConsulta.toString())
            .set(hashMapOf("checklistMarcados" to marcados), SetOptions.merge())
            .await()
    }

    /**
     * Atualiza os itens personalizados adicionados à checklist da consulta no Firestore.
     */
    suspend fun atualizarChecklistCustomConsulta(
        idPerfil: String,
        idConsulta: Int,
        custom: List<String>,
    ) = withContext(Dispatchers.IO) {
        db.collection("perfis")
            .document(idPerfil)
            .collection("consultas")
            .document(idConsulta.toString())
            .set(hashMapOf("checklistCustom" to custom), SetOptions.merge())
            .await()
    }
}

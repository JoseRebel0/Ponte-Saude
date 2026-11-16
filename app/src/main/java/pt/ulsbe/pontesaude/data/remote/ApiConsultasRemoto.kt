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
        val dto = ConsultaDtoRemoto(
            id = consulta.id,
            dia = consulta.dia,
            mes = consulta.mes,
            titulo = consulta.titulo,
            localizacao = consulta.localizacao,
            tipo = consulta.tipo,
            jejum = consulta.jejum,
        )
        db.collection("perfis")
            .document(idPerfil)
            .collection("consultas")
            .document(consulta.id.toString())
            .set(dto, SetOptions.merge())
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
            .update("checklistMarcados", marcados)
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
            .update("checklistCustom", custom)
            .await()
    }
}

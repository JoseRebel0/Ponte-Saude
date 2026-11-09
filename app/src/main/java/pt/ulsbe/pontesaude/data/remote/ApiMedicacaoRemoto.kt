package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.ulsbe.pontesaude.model.Medicamento

/**
 * Módulo remoto responsável pelas operações de medicação no Cloud Firestore.
 */
class ApiMedicacaoRemoto(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * Guarda ou atualiza a informação de um medicamento na subcoleção do perfil no Firestore.
     */
    suspend fun guardarMedicamento(idPerfil: String, med: Medicamento) = withContext(Dispatchers.IO) {
        val dados = hashMapOf(
            "id" to med.id,
            "nome" to med.nome,
            "finalidade" to med.finalidade,
            "principioAtivo" to med.principioAtivo,
            "hora" to med.hora,
            "cor" to med.cor,
            "ativo" to med.ativo,
            "fotografia" to (med.fotografia ?: ""),
        )
        db.collection("perfis")
            .document(idPerfil)
            .collection("medicamentos")
            .document(med.id.toString())
            .set(dados, SetOptions.merge())
            .await()
    }

    /**
     * Remove um medicamento da subcoleção do perfil no Firestore.
     */
    suspend fun removerMedicamento(idPerfil: String, idMedicamento: Long) = withContext(Dispatchers.IO) {
        db.collection("perfis")
            .document(idPerfil)
            .collection("medicamentos")
            .document(idMedicamento.toString())
            .delete()
            .await()
    }

    /**
     * Apaga um medicamento (sinónimo de [removerMedicamento]).
     */
    suspend fun apagarMedicamento(idPerfil: String, idMedicamento: Long) = removerMedicamento(idPerfil, idMedicamento)
}

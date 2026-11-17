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
     * Grava a fotografia da caixa no campo "fotografia".
     */
    suspend fun guardarMedicamento(idPerfil: String, med: Medicamento) = withContext(Dispatchers.IO) {
        val dto = MedicamentoDtoRemoto(
            id = med.id,
            nome = med.nome,
            finalidade = med.finalidade,
            principioAtivo = med.principioAtivo,
            hora = med.hora,
            cor = med.cor,
            ativo = med.ativo,
            fotografia = med.fotografia ?: "",
            laboratorio = med.laboratorio,
        )
        db.collection("perfis")
            .document(idPerfil)
            .collection("medicamentos")
            .document(med.id.toString())
            .set(dto, SetOptions.merge())
            .await()
    }

    /**
     * Atualiza diretamente o campo "fotografia" do medicamento na Cloud Firestore.
     */
    suspend fun atualizarImagemMedicamento(idPerfil: String, idMedicamento: Long, fotografiaBase64: String) = withContext(Dispatchers.IO) {
        val campos = mapOf(
            "fotografia" to fotografiaBase64,
        )
        db.collection("perfis")
            .document(idPerfil)
            .collection("medicamentos")
            .document(idMedicamento.toString())
            .set(campos, SetOptions.merge())
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

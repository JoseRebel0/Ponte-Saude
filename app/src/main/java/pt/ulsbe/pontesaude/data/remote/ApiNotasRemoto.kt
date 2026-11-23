package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.ulsbe.pontesaude.model.Nota

/**
 * Módulo remoto responsável pela sincronização de notas/apontamentos clínicos
 * e outros produtos de saúde (suplementos, ervanária) no Cloud Firestore.
 */
class ApiNotasRemoto(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * Atualiza toda a lista de apontamentos clínicos de um utente no Firestore.
     */
    suspend fun atualizarNotas(idPerfil: String, notas: List<Nota>) = withContext(Dispatchers.IO) {
        val colNotas = db.collection("perfis").document(idPerfil).collection("notas")
        val existentes = colNotas.get().await()
        val batch = db.batch()
        for (doc in existentes.documents) {
            batch.delete(doc.reference)
        }
        for ((indice, nota) in notas.withIndex()) {
            val dto = NotaDtoRemoto(
                texto = nota.texto,
                autor = nota.autor,
            )
            batch.set(colNotas.document("nota_${indice + 1}"), dto)
        }
        batch.commit().await()
    }

    /**
     * Atualiza a lista de outros produtos de saúde (suplementos, remédios naturais) no perfil no Firestore.
     */
    suspend fun atualizarProdutos(idPerfil: String, produtos: List<String>) = withContext(Dispatchers.IO) {
        db.collection("perfis")
            .document(idPerfil)
            .update("outrosProdutos", produtos)
            .await()
    }
}

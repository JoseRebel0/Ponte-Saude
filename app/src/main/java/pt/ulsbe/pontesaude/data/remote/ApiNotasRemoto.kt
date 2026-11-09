package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
        for (doc in existentes.documents) {
            doc.reference.delete().await()
        }
        for ((indice, nota) in notas.withIndex()) {
            colNotas.document("nota_${indice + 1}").set(
                hashMapOf(
                    "texto" to nota.texto,
                    "autor" to nota.autor,
                ),
            ).await()
        }
    }

    /**
     * Atualiza a lista de outros produtos de saúde (suplementos, remédios naturais) no perfil no Firestore.
     */
    suspend fun atualizarProdutos(idPerfil: String, produtos: List<String>) = withContext(Dispatchers.IO) {
        db.collection("perfis")
            .document(idPerfil)
            .set(hashMapOf("outrosProdutos" to produtos), SetOptions.merge())
            .await()
    }
}

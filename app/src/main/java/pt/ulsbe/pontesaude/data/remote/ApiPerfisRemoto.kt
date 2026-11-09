package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.ulsbe.pontesaude.data.Criptografia
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.VoucherResgatado

/**
 * Módulo remoto responsável pela gestão de perfis de utentes, associações cuidador-utente
 * e sincronização em tempo real no Cloud Firestore.
 *
 * Implementa a cifra ponta-a-ponta (RGPD / AES-256) dos dados pessoais de saúde antes
 * de serem transmitidos para a cloud.
 */
class ApiPerfisRemoto(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    private var listenerPerfis: ListenerRegistration? = null

    /**
     * Guarda um perfil completo no Firestore, incluindo subcoleções de medicamentos, consultas e notas.
     * Os dados de identificação pessoal são cifrados com AES-256 (RGPD).
     */
    suspend fun salvarPerfilCompleto(
        perfil: Perfil,
        pontos: Int = 50,
        modulosConcluidos: List<String> = emptyList(),
        modulosOuvidos: List<String> = emptyList(),
        videosAssistidos: List<String> = emptyList(),
        vouchers: List<VoucherResgatado> = emptyList(),
        obterChecklistConsulta: ((Int) -> Pair<List<String>, List<String>>)? = null,
    ) = withContext(Dispatchers.IO) {
        val docPerfil = db.collection("perfis").document(perfil.id)

        val vouchersMapeados = vouchers.map { v ->
            hashMapOf(
                "id" to v.id,
                "idRecompensa" to v.idRecompensa,
                "titulo" to v.titulo,
                "codigo" to v.codigo,
                "dataResgate" to v.dataResgate,
                "localLevantamento" to v.localLevantamento,
                "pontosUsados" to v.pontosUsados,
            )
        }

        val dadosPerfil = hashMapOf(
            "id" to perfil.id,
            "nome" to perfil.nome,
            "nomeCurto" to perfil.nomeCurto,
            "relacao" to perfil.relacao,
            "iniciais" to perfil.iniciais,
            "tipoSanguineo" to perfil.tipoSanguineo,
            "alergias" to perfil.alergias,
            "nif" to Criptografia.cifrar(perfil.nif),
            "numeroUtente" to Criptografia.cifrar(perfil.numeroUtente),
            "cartaoCidadao" to Criptografia.cifrar(perfil.cartaoCidadao),
            "dataNascimento" to Criptografia.cifrar(perfil.dataNascimento),
            "telemovel" to Criptografia.cifrar(perfil.telemovel),
            "email" to Criptografia.cifrar(perfil.email),
            "perfisCuidadosIds" to perfil.perfisCuidadosIds,
            "cuidadoresIds" to perfil.cuidadoresIds,
            "outrosProdutos" to perfil.outrosProdutos,
            "pontosSeguranca" to pontos,
            "modulosConcluidos" to modulosConcluidos,
            "modulosOuvidos" to modulosOuvidos,
            "videosAssistidos" to videosAssistidos,
            "vouchersResgatados" to vouchersMapeados,
        )

        docPerfil.set(dadosPerfil, SetOptions.merge()).await()

        // 1. Guardar Medicamentos na subcoleção remota
        val colMedicamentos = docPerfil.collection("medicamentos")
        for (med in perfil.medicamentos) {
            val dadosMed = hashMapOf(
                "id" to med.id,
                "nome" to med.nome,
                "finalidade" to med.finalidade,
                "principioAtivo" to med.principioAtivo,
                "hora" to med.hora,
                "cor" to med.cor,
                "ativo" to med.ativo,
                "fotografia" to (med.fotografia ?: ""),
            )
            colMedicamentos.document(med.id.toString()).set(dadosMed, SetOptions.merge()).await()
        }

        // 2. Guardar Consultas na subcoleção remota
        val colConsultas = docPerfil.collection("consultas")
        for (cons in perfil.consultas) {
            val pairChecklist = obterChecklistConsulta?.invoke(cons.id)
            val marcados = pairChecklist?.first ?: emptyList()
            val custom = pairChecklist?.second ?: emptyList()

            val dadosCons = hashMapOf(
                "id" to cons.id,
                "dia" to cons.dia,
                "mes" to cons.mes,
                "titulo" to cons.titulo,
                "localizacao" to cons.localizacao,
                "tipo" to cons.tipo,
                "jejum" to cons.jejum,
                "checklistMarcados" to marcados,
                "checklistCustom" to custom,
            )
            colConsultas.document(cons.id.toString()).set(dadosCons, SetOptions.merge()).await()
        }

        // 3. Guardar Notas na subcoleção remota
        val colNotas = docPerfil.collection("notas")
        for ((idx, nota) in perfil.notas.withIndex()) {
            val dadosNota = hashMapOf(
                "texto" to nota.texto,
                "autor" to nota.autor,
            )
            colNotas.document("nota_${idx + 1}").set(dadosNota, SetOptions.merge()).await()
        }
    }

    /**
     * Carrega todos os perfis, medicamentos, consultas e notas de todos os utentes da base de dados Firestore.
     * Decifra automaticamente no dispositivo os dados de identificação pessoal protegidos pelo RGPD.
     */
    suspend fun carregarPerfisFirestore(): Result<List<DadosPerfilRemoto>> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = db.collection("perfis").get().await()
            val lista = mutableListOf<DadosPerfilRemoto>()

            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val nomeRaw = doc.getString("nome")
                val nome = Criptografia.decifrar(nomeRaw).ifBlank { "Utente SNS" }
                val nomeCurto = Criptografia.decifrar(doc.getString("nomeCurto")).ifBlank { nome }
                val relacao = doc.getString("relacao") ?: "Utente"
                val iniciais = doc.getString("iniciais") ?: "UT"
                val tipoSanguineo = Criptografia.decifrar(doc.getString("tipoSanguineo")).ifBlank { "A+" }
                val alergias = Criptografia.decifrar(doc.getString("alergias")).ifBlank { "Sem alergias conhecidas" }
                val nif = Criptografia.decifrar(doc.getString("nif")).ifBlank { "999 999 999" }
                val numeroUtente = Criptografia.decifrar(doc.getString("numeroUtente")).ifBlank { "123 456 789" }
                val cartaoCidadao = Criptografia.decifrar(doc.getString("cartaoCidadao"))
                val dataNascimento = Criptografia.decifrar(doc.getString("dataNascimento")).ifBlank { "01/01/1970" }
                val telemovel = Criptografia.decifrar(doc.getString("telemovel"))
                val email = Criptografia.decifrar(doc.getString("email"))

                @Suppress("UNCHECKED_CAST")
                val perfisCuidadosIds = doc.get("perfisCuidadosIds") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val cuidadoresIds = doc.get("cuidadoresIds") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val outrosProdutosRaw = doc.get("outrosProdutos") as? List<String> ?: emptyList()
                val outrosProdutos = Criptografia.decifrarLista(outrosProdutosRaw)

                val pontos = doc.getLong("pontosSeguranca")?.toInt() ?: 50

                @Suppress("UNCHECKED_CAST")
                val modulosConcluidos = doc.get("modulosConcluidos") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val modulosOuvidos = doc.get("modulosOuvidos") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val videosAssistidos = doc.get("videosAssistidos") as? List<String> ?: emptyList()

                @Suppress("UNCHECKED_CAST")
                val vouchersRaw = doc.get("vouchersResgatados") as? List<Map<String, Any>> ?: emptyList()
                val vouchers = vouchersRaw.mapNotNull { m ->
                    runCatching {
                        VoucherResgatado(
                            id = m["id"] as? String ?: "",
                            idRecompensa = m["idRecompensa"] as? String ?: "",
                            titulo = m["titulo"] as? String ?: "",
                            codigo = m["codigo"] as? String ?: "",
                            dataResgate = m["dataResgate"] as? String ?: "",
                            localLevantamento = m["localLevantamento"] as? String ?: "",
                            pontosUsados = (m["pontosUsados"] as? Long)?.toInt() ?: 0,
                        )
                    }.getOrNull()
                }

                // Subcoleção: Medicamentos
                val medsSnapshot = doc.reference.collection("medicamentos").get().await()
                val medicamentos = medsSnapshot.documents.mapNotNull { mDoc ->
                    runCatching {
                        Medicamento(
                            id = mDoc.getLong("id") ?: mDoc.id.toLongOrNull() ?: 0L,
                            nome = Criptografia.decifrar(mDoc.getString("nome")),
                            finalidade = Criptografia.decifrar(mDoc.getString("finalidade")),
                            principioAtivo = Criptografia.decifrar(mDoc.getString("principioAtivo")),
                            hora = Criptografia.decifrar(mDoc.getString("hora")).ifBlank { "08:00" },
                            cor = mDoc.getLong("cor") ?: 0xFF3E63DD,
                            ativo = mDoc.getBoolean("ativo") ?: true,
                            fotografia = Criptografia.decifrar(mDoc.getString("fotografia")).takeIf { it.isNotBlank() },
                        )
                    }.getOrNull()
                }

                // Subcoleção: Consultas
                val consSnapshot = doc.reference.collection("consultas").get().await()
                val mapaChecklists = mutableMapOf<Int, Pair<List<String>, List<String>>>()
                val consultas = consSnapshot.documents.mapNotNull { cDoc ->
                    runCatching {
                        val cId = cDoc.getLong("id")?.toInt() ?: cDoc.id.toIntOrNull() ?: 1
                        @Suppress("UNCHECKED_CAST")
                        val marcados = cDoc.get("checklistMarcados") as? List<String> ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val customRaw = cDoc.get("checklistCustom") as? List<String> ?: emptyList()
                        val custom = Criptografia.decifrarLista(customRaw)
                        mapaChecklists[cId] = Pair(marcados, custom)

                        Consulta(
                            id = cId,
                            dia = cDoc.getString("dia") ?: "01",
                            mes = cDoc.getString("mes") ?: "Jan",
                            titulo = Criptografia.decifrar(cDoc.getString("titulo")),
                            localizacao = Criptografia.decifrar(cDoc.getString("localizacao")),
                            tipo = Criptografia.decifrar(cDoc.getString("tipo")),
                            jejum = cDoc.getBoolean("jejum") ?: false,
                        )
                    }.getOrNull()
                }

                // Subcoleção: Notas
                val notasSnapshot = doc.reference.collection("notas").get().await()
                val notas = notasSnapshot.documents.mapNotNull { nDoc ->
                    runCatching {
                        Nota(
                            texto = Criptografia.decifrar(nDoc.getString("texto")),
                            autor = Criptografia.decifrar(nDoc.getString("autor")),
                        )
                    }.getOrNull()
                }

                val perfilCarregado = Perfil(
                    id = id,
                    nome = nome,
                    nomeCurto = nomeCurto,
                    relacao = relacao,
                    iniciais = iniciais,
                    tipoSanguineo = tipoSanguineo,
                    alergias = alergias,
                    consultas = consultas,
                    medicamentos = medicamentos,
                    outrosProdutos = outrosProdutos,
                    notas = notas,
                    nif = nif,
                    numeroUtente = numeroUtente,
                    cartaoCidadao = cartaoCidadao,
                    dataNascimento = dataNascimento,
                    telemovel = telemovel,
                    email = email,
                    perfisCuidadosIds = perfisCuidadosIds,
                    cuidadoresIds = cuidadoresIds,
                )

                lista.add(
                    DadosPerfilRemoto(
                        perfil = perfilCarregado,
                        pontosSeguranca = pontos,
                        modulosConcluidos = modulosConcluidos,
                        modulosOuvidos = modulosOuvidos,
                        videosAssistidos = videosAssistidos,
                        vouchersResgatados = vouchers,
                        checklistsConsultas = mapaChecklists,
                    ),
                )
            }
            lista
        }
    }

    /**
     * Sinónimo de [carregarPerfisFirestore].
     */
    suspend fun carregarTodosOsPerfis(): Result<List<DadosPerfilRemoto>> = carregarPerfisFirestore()

    /**
     * Associa um utente cuidado ao perfil do cuidador e vice-versa.
     */
    suspend fun associarPerfilCuidado(idCuidador: String, idCuidado: String) = withContext(Dispatchers.IO) {
        runCatching {
            db.collection("perfis").document(idCuidador)
                .set(hashMapOf("perfisCuidadosIds" to FieldValue.arrayUnion(idCuidado)), SetOptions.merge())
                .await()
            db.collection("perfis").document(idCuidado)
                .set(hashMapOf("cuidadoresIds" to FieldValue.arrayUnion(idCuidador)), SetOptions.merge())
                .await()
        }
    }

    /**
     * Desassocia um utente cuidado do perfil do cuidador e vice-versa.
     */
    suspend fun desassociarPerfilCuidado(idCuidador: String, idCuidado: String) = withContext(Dispatchers.IO) {
        runCatching {
            db.collection("perfis").document(idCuidador)
                .set(hashMapOf("perfisCuidadosIds" to FieldValue.arrayRemove(idCuidado)), SetOptions.merge())
                .await()
            db.collection("perfis").document(idCuidado)
                .set(hashMapOf("cuidadoresIds" to FieldValue.arrayRemove(idCuidador)), SetOptions.merge())
                .await()
        }
    }

    /**
     * Remove todas as associações de cuidadores e utentes cuidados no Firestore.
     */
    suspend fun limparTodasAssociacoesFirestore() = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = db.collection("perfis").get().await()
            for (doc in snapshot.documents) {
                doc.reference.set(
                    hashMapOf(
                        "perfisCuidadosIds" to emptyList<String>(),
                        "cuidadoresIds" to emptyList<String>(),
                    ),
                    SetOptions.merge(),
                ).await()
            }
        }
    }

    /**
     * Escuta em tempo real alterações na coleção de perfis no Firestore.
     */
    fun iniciarEscutaPerfisEmTempoReal(aoAlterar: () -> Unit) {
        listenerPerfis?.remove()
        listenerPerfis = db.collection("perfis").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                aoAlterar()
            }
        }
    }

    /**
     * Cancela a escuta em tempo real das alterações de perfis.
     */
    fun pararEscutaPerfis() {
        listenerPerfis?.remove()
        listenerPerfis = null
    }
}

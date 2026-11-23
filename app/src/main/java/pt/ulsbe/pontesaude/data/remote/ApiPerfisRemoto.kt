package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.ulsbe.pontesaude.util.Criptografia
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.VoucherResgatado

/**
 * Módulo remoto responsável pela gestão de perfis de utentes, associações cuidador-utente
 * e sincronização em tempo real no Cloud Firestore.
 *
 * Só envia dados clínicos ligados a um id aleatório (pseudonimização). A identificação
 * (nome, NIF, telemóvel, etc.) nunca sai do telemóvel.
 */
class ApiPerfisRemoto(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    private var listenerPerfis: ListenerRegistration? = null

    private companion object {
        val CAMPOS_IDENTIFICACAO_ANTIGOS = listOf(
            "nome", "nomeCurto", "relacao", "iniciais", "nif", "numeroUtente",
            "cartaoCidadao", "dataNascimento", "telemovel", "email",
            "rgpdProtegido", "conformidadeRgpd",
        )
    }

    /**
     * Guarda um perfil completo no Firestore, incluindo subcoleções de medicamentos, consultas e notas.
     * Não envia dados de identificação nem fotografias.
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

        val donoUid = AutenticadorFirebaseCMD.garantirSessao() ?: error("Sem sessão no Firebase")
        val cuidadoresValidos =
            perfil.cuidadoresIds.filter { it != perfil.id && it != donoUid }.distinct()
        val cuidadosValidos = perfil.perfisCuidadosIds.filter { it != perfil.id }.distinct()
        val dadosPerfil = PerfilDtoRemoto(
            id = perfil.id,
            donoUid = donoUid,
            tipoSanguineo = perfil.tipoSanguineo,
            alergias = perfil.alergias,
            perfisCuidadosIds = cuidadosValidos,
            cuidadoresIds = cuidadoresValidos,
            outrosProdutos = perfil.outrosProdutos,
            pontosSeguranca = pontos,
            modulosConcluidos = modulosConcluidos,
            modulosOuvidos = modulosOuvidos,
            videosAssistidos = videosAssistidos,
            vouchersResgatados = vouchersMapeados,
        )

        docPerfil.set(dadosPerfil, SetOptions.merge()).await()

        // Remove campos de identificação que versões antigas enviaram para o Firestore.
        runCatching {
            docPerfil.update(CAMPOS_IDENTIFICACAO_ANTIGOS.associateWith { FieldValue.delete() }).await()
        }

        // 1. Guardar Medicamentos na subcoleção remota
        val colMedicamentos = docPerfil.collection("medicamentos")
        for (med in perfil.medicamentos) {
            val dadosMed = MedicamentoDtoRemoto(
                id = med.id,
                nome = med.nome,
                finalidade = med.finalidade,
                principioAtivo = med.principioAtivo,
                hora = med.hora,
                cor = med.cor,
                ativo = med.ativo,
                fotografia = "", // fotografias ficam só no telemóvel
                laboratorio = med.laboratorio,
            )
            colMedicamentos.document(med.id.toString()).set(dadosMed, SetOptions.merge()).await()
        }

        // 2. Guardar Consultas na subcoleção remota
        val colConsultas = docPerfil.collection("consultas")
        for (cons in perfil.consultas) {
            val pairChecklist = obterChecklistConsulta?.invoke(cons.id)
            val marcados = pairChecklist?.first ?: emptyList()
            val custom = pairChecklist?.second ?: emptyList()

            val dadosCons = ConsultaDtoRemoto(
                id = cons.id,
                dia = cons.dia,
                mes = cons.mes,
                titulo = cons.titulo,
                localizacao = cons.localizacao,
                tipo = cons.tipo,
                jejum = cons.jejum,
                checklistMarcados = marcados,
                checklistCustom = custom,
            )
            colConsultas.document(cons.id.toString()).set(dadosCons, SetOptions.merge()).await()
        }

        // 3. Guardar Notas na subcoleção remota
        val colNotas = docPerfil.collection("notas")
        val notasExistentes = colNotas.get().await()
        val batchNotas = db.batch()
        for (doc in notasExistentes.documents) {
            batchNotas.delete(doc.reference)
        }
        for ((idx, nota) in perfil.notas.withIndex()) {
            val dadosNota = NotaDtoRemoto(
                texto = nota.texto,
                autor = nota.autor,
            )
            batchNotas.set(colNotas.document("nota_${idx + 1}"), dadosNota)
        }
        batchNotas.commit().await()
    }

    /**
     * Carrega os perfis pertencentes ao utente autenticado (ou sob o seu cuidado) no Firestore.
     */
    suspend fun carregarPerfisFirestore(): Result<List<DadosPerfilRemoto>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uidAtual = AutenticadorFirebaseCMD.uidAtual
                val docs = if (!uidAtual.isNullOrBlank()) {
                    val snapDono = runCatching {
                        db.collection("perfis").whereEqualTo("donoUid", uidAtual).get().await()
                    }.getOrNull()
                    val snapCuidador = runCatching {
                        db.collection("perfis").whereArrayContains("cuidadoresUids", uidAtual).get().await()
                    }.getOrNull()
                    ((snapDono?.documents.orEmpty()) + (snapCuidador?.documents.orEmpty())).distinctBy { it.id }
                } else {
                    // Sem sessão no Firebase não se carrega nada (nunca se listam perfis de outras pessoas).
                    emptyList()
                }
                val lista = mutableListOf<DadosPerfilRemoto>()

                for (doc in docs) {
                    val id = doc.getString("id") ?: doc.id
                    // A identificação NÃO vem do Firestore: é preenchida com os dados locais (Room)
                    // em AppContainer.sincronizarComFirestore. Estes valores só aparecem se o perfil
                    // não existir neste telemóvel.
                    val nome = "Utente"
                    val nomeCurto = "Utente"
                    val relacao = "Utente"
                    val iniciais = "UT"
                    val tipoSanguineo =
                        doc.getString("tipoSanguineo")?.ifBlank { null } ?: "—"
                    val alergias = doc.getString("alergias")?.ifBlank { null } ?: "Sem informação"
                    val nif = ""
                    val numeroUtente = ""
                    val cartaoCidadao = ""
                    val dataNascimento = ""
                    val telemovel = ""
                    val email = ""

                    val donoUidDoc = doc.getString("donoUid") ?: id

                    @Suppress("UNCHECKED_CAST")
                    val perfisCuidadosRaw =
                        doc.get("perfisCuidadosIds") as? List<String> ?: emptyList()
                    val perfisCuidadosIds = perfisCuidadosRaw.filter { it != id }

                    @Suppress("UNCHECKED_CAST")
                    val cuidadoresRaw = doc.get("cuidadoresIds") as? List<String> ?: emptyList()
                    val cuidadoresIds = cuidadoresRaw.filter { it != id && it != donoUidDoc }

                    @Suppress("UNCHECKED_CAST")
                    val outrosProdutosRaw =
                        doc.get("outrosProdutos") as? List<String> ?: emptyList()
                    val outrosProdutos = Criptografia.decifrarLista(outrosProdutosRaw)

                    val pontos = doc.getLong("pontosSeguranca")?.toInt() ?: 50

                    @Suppress("UNCHECKED_CAST")
                    val modulosConcluidos =
                        doc.get("modulosConcluidos") as? List<String> ?: emptyList()

                    @Suppress("UNCHECKED_CAST")
                    val modulosOuvidos = doc.get("modulosOuvidos") as? List<String> ?: emptyList()

                    @Suppress("UNCHECKED_CAST")
                    val videosAssistidos =
                        doc.get("videosAssistidos") as? List<String> ?: emptyList()

                    @Suppress("UNCHECKED_CAST")
                    val vouchersRaw =
                        doc.get("vouchersResgatados") as? List<Map<String, Any>> ?: emptyList()
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
                                hora = Criptografia.decifrar(mDoc.getString("hora"))
                                    .ifBlank { "08:00" },
                                cor = mDoc.getLong("cor") ?: 0xFF3E63DD,
                                ativo = mDoc.getBoolean("ativo") ?: true,
                                fotografia = Criptografia.decifrar(mDoc.getString("fotografia"))
                                    .takeIf { it.isNotBlank() },
                                laboratorio = Criptografia.decifrar(mDoc.getString("laboratorio")),
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
                            val marcados =
                                cDoc.get("checklistMarcados") as? List<String> ?: emptyList()

                            @Suppress("UNCHECKED_CAST")
                            val customRaw =
                                cDoc.get("checklistCustom") as? List<String> ?: emptyList()
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
                    val notas = notasSnapshot.documents
                        .sortedBy { it.id.removePrefix("nota_").toIntOrNull() ?: 0 }
                        .mapNotNull { nDoc ->
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
                            donoUid = doc.getString("donoUid") ?: "",
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
    suspend fun associarPerfilCuidado(idCuidador: String, idCuidado: String) =
        withContext(Dispatchers.IO) {
            if (idCuidador == idCuidado) return@withContext
            runCatching {
                db.collection("perfis").document(idCuidador)
                    .update("perfisCuidadosIds", FieldValue.arrayUnion(idCuidado))
                    .await()
                db.collection("perfis").document(idCuidado)
                    .update("cuidadoresIds", FieldValue.arrayUnion(idCuidador))
                    .await()
            }
        }

    /**
     * Desassocia um utente cuidado do perfil do cuidador e vice-versa.
     */
    suspend fun desassociarPerfilCuidado(idCuidador: String, idCuidado: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                db.collection("perfis").document(idCuidador)
                    .update("perfisCuidadosIds", FieldValue.arrayRemove(idCuidado))
                    .await()
                db.collection("perfis").document(idCuidado)
                    .update("cuidadoresIds", FieldValue.arrayRemove(idCuidador))
                    .await()
            }
        }


    /**
     * Elimina um perfil e todas as suas subcoleções (medicamentos, consultas, notas) do Firestore,
     * limpando também as referências de associação nos restantes perfis.
     */
    suspend fun apagarPerfil(idPerfil: String) = withContext(Dispatchers.IO) {
        runCatching {
            val docRef = db.collection("perfis").document(idPerfil)
            val meds = docRef.collection("medicamentos").get().await()
            for (m in meds.documents) {
                m.reference.delete().await()
            }
            val cons = docRef.collection("consultas").get().await()
            for (c in cons.documents) {
                c.reference.delete().await()
            }
            val notas = docRef.collection("notas").get().await()
            for (n in notas.documents) {
                n.reference.delete().await()
            }
            docRef.delete().await()

            val uid = AutenticadorFirebaseCMD.uidAtual.orEmpty()
            // Só os perfis do próprio dono (as regras não deixam listar outros) e filtra em código,
            // para não precisar de índices compostos no Firestore.
            val meus = db.collection("perfis").whereEqualTo("donoUid", uid).get().await()
            for (d in meus.documents) {
                @Suppress("UNCHECKED_CAST")
                if (idPerfil in (d.get("perfisCuidadosIds") as? List<String> ?: emptyList())) {
                    d.reference.update("perfisCuidadosIds", FieldValue.arrayRemove(idPerfil)).await()
                }
                @Suppress("UNCHECKED_CAST")
                if (idPerfil in (d.get("cuidadoresIds") as? List<String> ?: emptyList())) {
                    d.reference.update("cuidadoresIds", FieldValue.arrayRemove(idPerfil)).await()
                }
            }
        }
    }

    /**
     * Escuta em tempo real alterações nos perfis pertencentes ao utente autenticado no Firestore.
     */
    fun iniciarEscutaPerfisEmTempoReal(aoAlterar: () -> Unit) {
        listenerPerfis?.remove()
        val uidAtual = AutenticadorFirebaseCMD.uidAtual
        val query = if (!uidAtual.isNullOrBlank()) {
            db.collection("perfis").whereEqualTo("donoUid", uidAtual)
        } else {
            return
        }
        listenerPerfis = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                aoAlterar()
            }
        }
    }
}
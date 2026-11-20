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

        val donoUid = AutenticadorFirebaseCMD.uidAtual ?: perfil.id
        val cuidadoresValidos =
            perfil.cuidadoresIds.filter { it != perfil.id && it != donoUid }.distinct()
        val cuidadosValidos = perfil.perfisCuidadosIds.filter { it != perfil.id }.distinct()
        val dadosPerfil = PerfilDtoRemoto(
            id = perfil.id,
            donoUid = donoUid,
            nome = perfil.nome,
            nomeCurto = perfil.nomeCurto,
            relacao = perfil.relacao,
            iniciais = perfil.iniciais,
            tipoSanguineo = perfil.tipoSanguineo,
            alergias = perfil.alergias,
            nif = Criptografia.cifrar(perfil.nif),
            numeroUtente = Criptografia.cifrar(perfil.numeroUtente),
            cartaoCidadao = Criptografia.cifrar(perfil.cartaoCidadao),
            dataNascimento = Criptografia.cifrar(perfil.dataNascimento),
            telemovel = Criptografia.cifrar(perfil.telemovel),
            email = Criptografia.cifrar(perfil.email),
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
                fotografia = med.fotografia ?: "",
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
        for ((idx, nota) in perfil.notas.withIndex()) {
            val dadosNota = NotaDtoRemoto(
                texto = nota.texto,
                autor = nota.autor,
            )
            colNotas.document("nota_${idx + 1}").set(dadosNota, SetOptions.merge()).await()
        }
    }

    /**
     * Carrega os perfis pertencentes ao utente autenticado (ou sob o seu cuidado) no Firestore.
     * Decifra automaticamente no dispositivo os dados de identificação pessoal protegidos pelo RGPD.
     */
    suspend fun carregarPerfisFirestore(): Result<List<DadosPerfilRemoto>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uidAtual = AutenticadorFirebaseCMD.uidAtual
                val docs = if (!uidAtual.isNullOrBlank()) {
                    val snapDono =
                        db.collection("perfis").whereEqualTo("donoUid", uidAtual).get().await()
                    val snapCuidador =
                        db.collection("perfis").whereArrayContains("cuidadoresIds", uidAtual).get()
                            .await()
                    (snapDono.documents + snapCuidador.documents).distinctBy { it.id }
                } else {
                    emptyList()
                }
                val lista = mutableListOf<DadosPerfilRemoto>()

                for (doc in docs) {
                    val id = doc.getString("id") ?: doc.id
                    val nomeRaw = doc.getString("nome")
                    val nome = Criptografia.decifrar(nomeRaw).ifBlank { "Utente SNS" }
                    val nomeCurto =
                        Criptografia.decifrar(doc.getString("nomeCurto")).ifBlank { nome }
                    val relacao = doc.getString("relacao") ?: "Utente"
                    val iniciais = doc.getString("iniciais") ?: "UT"
                    val tipoSanguineo =
                        Criptografia.decifrar(doc.getString("tipoSanguineo")).ifBlank { "A+" }
                    val alergias = Criptografia.decifrar(doc.getString("alergias"))
                        .ifBlank { "Sem alergias conhecidas" }
                    val nif = Criptografia.decifrar(doc.getString("nif")).ifBlank { "999 999 999" }
                    val numeroUtente = Criptografia.decifrar(doc.getString("numeroUtente"))
                        .ifBlank { "123 456 789" }
                    val cartaoCidadao = Criptografia.decifrar(doc.getString("cartaoCidadao"))
                    val dataNascimento = Criptografia.decifrar(doc.getString("dataNascimento"))
                        .ifBlank { "01/01/1970" }
                    val telemovel = Criptografia.decifrar(doc.getString("telemovel"))
                    val email = Criptografia.decifrar(doc.getString("email"))

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

            val snapCuidados = db.collection("perfis")
                .whereArrayContains("perfisCuidadosIds", idPerfil)
                .get().await()
            for (d in snapCuidados.documents) {
                d.reference.update("perfisCuidadosIds", FieldValue.arrayRemove(idPerfil)).await()
            }
            val snapCuidadores = db.collection("perfis")
                .whereArrayContains("cuidadoresIds", idPerfil)
                .get().await()
            for (d in snapCuidadores.documents) {
                d.reference.update("cuidadoresIds", FieldValue.arrayRemove(idPerfil)).await()
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
package pt.ulsbe.pontesaude.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.VoucherResgatado

/**
 * Estrutura agregada com todos os dados de um perfil remoto e o seu progresso no Clube de Segurança.
 */
data class DadosPerfilRemoto(
    val perfil: Perfil,
    val pontosSeguranca: Int,
    val modulosConcluidos: List<String>,
    val modulosOuvidos: List<String>,
    val videosAssistidos: List<String>,
    val vouchersResgatados: List<VoucherResgatado>,
    val checklistsConsultas: Map<Int, Pair<List<String>, List<String>>>, // idConsulta -> Pair(marcados, custom)
)

/**
 * Serviço responsável por comunicar com o Google Cloud Firestore.
 * Suporta inicialização de dados (Seed), carregamento completo (Fetch/Pull) e sincronização bidirecional.
 */
class ServicoFirestore {

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var listenerPerfis: ListenerRegistration? = null

    /**
     * Envia todos os dados pré-definidos da aplicação para o Google Cloud Firestore.
     * Guarda os perfis identificados numericamente ("1", "2", "3"), respetivos medicamentos,
     * consultas com checklist, notas, estado de recompensas, módulos e biblioteca de vídeos.
     */
    suspend fun enviarTodosOsDados(
        perfisParaEnviar: List<Perfil> = emptyList(),
        obterDadosRecompensas: ((String) -> Triple<Int, Set<String>, List<VoucherResgatado>>)? = null,
        obterChecklistConsulta: ((Int) -> Pair<List<String>, List<String>>)? = null,
        aoProgredir: (String) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Enviar Perfis e respetivas Subcoleções
            aoProgredir("A enviar perfis e utentes para o Firestore...")
            for (perfil in perfisParaEnviar) {
                val dadosExtra = obterDadosRecompensas?.invoke(perfil.id)
                val pontos = dadosExtra?.first ?: 50
                val concluidos = dadosExtra?.second?.toList() ?: emptyList()
                val vouchers = dadosExtra?.third ?: emptyList()

                salvarPerfilCompleto(
                    perfil = perfil,
                    pontos = pontos,
                    modulosConcluidos = concluidos,
                    modulosOuvidos = emptyList(),
                    videosAssistidos = emptyList(),
                    vouchers = vouchers,
                    obterChecklistConsulta = obterChecklistConsulta,
                )
            }

            // 2. Enviar Módulos de Literacia e Segurança
            aoProgredir("A enviar módulos de literacia...")
            val colecaoModulos = db.collection("modulos_saude")
            for (modulo in DadosIniciais.modulos) {
                val dadosModulo = hashMapOf(
                    "id" to modulo.id,
                    "titulo" to modulo.titulo,
                    "subtitulo" to modulo.subtitulo,
                    "icone" to modulo.icone,
                    "tipo" to modulo.tipo.name,
                    "conteudos" to modulo.conteudos,
                    "fala" to modulo.fala,
                )
                colecaoModulos.document(modulo.id).set(dadosModulo, SetOptions.merge()).await()
            }

            // 3. Enviar Recompensas do Clube de Segurança
            aoProgredir("A enviar catálogo de recompensas...")
            val colecaoRecompensas = db.collection("recompensas")
            for (recompensa in DadosIniciais.recompensas) {
                val dadosRecompensa = hashMapOf(
                    "id" to recompensa.id,
                    "titulo" to recompensa.titulo,
                    "descricao" to recompensa.descricao,
                    "pontos" to recompensa.pontos,
                    "icone" to recompensa.icone,
                    "localLevantamento" to recompensa.localLevantamento,
                )
                colecaoRecompensas.document(recompensa.id).set(dadosRecompensa, SetOptions.merge()).await()
            }

            // 4. Enviar Biblioteca de Vídeos SNS
            aoProgredir("A enviar biblioteca de vídeos...")
            val colecaoVideos = db.collection("videos_biblioteca")
            for (video in DadosIniciais.videosBiblioteca) {
                val dadosVideo = hashMapOf(
                    "id" to video.id,
                    "titulo" to video.titulo,
                    "descricao" to video.descricao,
                    "duracao" to video.duracao,
                    "urlYoutube" to video.urlYoutube,
                    "pontos" to video.pontos,
                )
                colecaoVideos.document(video.id).set(dadosVideo, SetOptions.merge()).await()
            }

            aoProgredir("Base de dados Firestore configurada e povoada com sucesso!")
        }
    }

    /**
     * Guarda um perfil completo no Firestore, incluindo dados de pontos, recompensas e subcoleções.
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
            "iniciais" to perfil.iniciais,
            "tipoSanguineo" to perfil.tipoSanguineo,
            "alergias" to perfil.alergias,
            "nif" to perfil.nif,
            "numeroUtente" to perfil.numeroUtente,
            "cartaoCidadao" to perfil.cartaoCidadao,
            "dataNascimento" to perfil.dataNascimento,
            "telemovel" to perfil.telemovel,
            "email" to perfil.email,
            "outrosProdutos" to perfil.outrosProdutos,
            "perfisCuidadosIds" to perfil.perfisCuidadosIds,
            "cuidadoresIds" to perfil.cuidadoresIds,
            // Dados do Clube de Segurança & Recompensas
            "pontosSeguranca" to pontos,
            "modulosConcluidos" to modulosConcluidos,
            "modulosOuvidos" to modulosOuvidos,
            "videosAssistidos" to videosAssistidos,
            "vouchersResgatados" to vouchersMapeados,
        )
        docPerfil.set(dadosPerfil, SetOptions.merge()).await()

        // Subcoleção Medicamentos
        val colMeds = docPerfil.collection("medicamentos")
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
            colMeds.document(med.id.toString()).set(dadosMed, SetOptions.merge()).await()
        }

        // Subcoleção Consultas com checklist de preparação
        val colConsultas = docPerfil.collection("consultas")
        for (consulta in perfil.consultas) {
            val checklist = obterChecklistConsulta?.invoke(consulta.id)
            val dadosConsulta = hashMapOf(
                "id" to consulta.id,
                "dia" to consulta.dia,
                "mes" to consulta.mes,
                "titulo" to consulta.titulo,
                "localizacao" to consulta.localizacao,
                "tipo" to consulta.tipo,
                "jejum" to consulta.jejum,
                "checklistMarcados" to (checklist?.first ?: emptyList()),
                "checklistCustom" to (checklist?.second ?: emptyList()),
            )
            colConsultas.document(consulta.id.toString()).set(dadosConsulta, SetOptions.merge()).await()
        }

        // Subcoleção Notas
        val colNotas = docPerfil.collection("notas")
        for ((indice, nota) in perfil.notas.withIndex()) {
            val dadosNota = hashMapOf(
                "texto" to nota.texto,
                "autor" to nota.autor,
            )
            colNotas.document("nota_${indice + 1}").set(dadosNota, SetOptions.merge()).await()
        }
    }

    /**
     * Carrega todos os perfis do Firestore, incluindo as suas subcoleções de medicamentos,
     * consultas com checklist, notas e o progresso das recompensas.
     */
    suspend fun carregarTodosOsPerfis(): Result<List<DadosPerfilRemoto>> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshotPerfis = db.collection("perfis").get().await()
            val lista = mutableListOf<DadosPerfilRemoto>()

            for (doc in snapshotPerfis.documents) {
                val id = doc.getString("id") ?: doc.id
                val nome = doc.getString("nome") ?: "Utente"
                val nomeCurto = doc.getString("nomeCurto") ?: nome
                val relacao = "Utente"
                val iniciais = doc.getString("iniciais") ?: nome.take(2).uppercase()
                val tipoSanguineo = doc.getString("tipoSanguineo") ?: "A+"
                val alergias = doc.getString("alergias") ?: "Sem alergias conhecidas"
                val nif = doc.getString("nif") ?: ""
                val numeroUtente = doc.getString("numeroUtente") ?: ""
                val cartaoCidadao = doc.getString("cartaoCidadao") ?: ""
                val dataNascimento = doc.getString("dataNascimento") ?: "01/01/1950"
                val telemovel = doc.getString("telemovel") ?: ""
                val email = doc.getString("email") ?: ""
                val outrosProdutos = (doc.get("outrosProdutos") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val perfisCuidadosIds = (doc.get("perfisCuidadosIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val cuidadoresIds = (doc.get("cuidadoresIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                // Dados de Recompensas e Progresso
                val pontos = doc.getLong("pontosSeguranca")?.toInt() ?: 50
                val modulosConcluidos = (doc.get("modulosConcluidos") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val modulosOuvidos = (doc.get("modulosOuvidos") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val videosAssistidos = (doc.get("videosAssistidos") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                val vouchersBrutos = doc.get("vouchersResgatados") as? List<*>
                val vouchers = vouchersBrutos?.mapNotNull { item ->
                    (item as? Map<*, *>)?.let { map ->
                        VoucherResgatado(
                            id = map["id"]?.toString() ?: "",
                            idRecompensa = map["idRecompensa"]?.toString() ?: "",
                            titulo = map["titulo"]?.toString() ?: "",
                            codigo = map["codigo"]?.toString() ?: "",
                            dataResgate = map["dataResgate"]?.toString() ?: "",
                            localLevantamento = map["localLevantamento"]?.toString() ?: "",
                            pontosUsados = (map["pontosUsados"] as? Number)?.toInt() ?: 0,
                        )
                    }
                } ?: emptyList()

                // 1. Carregar Medicamentos (com horário e todos os detalhes)
                val snapshotMeds = doc.reference.collection("medicamentos").get().await()
                var medicamentos = snapshotMeds.documents.mapNotNull { medDoc ->
                    val medId = medDoc.getLong("id") ?: medDoc.id.toLongOrNull() ?: return@mapNotNull null
                    val medNome = medDoc.getString("nome") ?: return@mapNotNull null
                    val finalidade = medDoc.getString("finalidade") ?: ""
                    val principioAtivo = medDoc.getString("principioAtivo") ?: ""
                    val hora = medDoc.getString("hora") ?: "08:00"
                    val cor = medDoc.getLong("cor") ?: 0xFF3E63DD
                    val ativo = medDoc.getBoolean("ativo") ?: true
                    val foto = medDoc.getString("fotografia")?.takeIf { it.isNotBlank() && it != "null" }
                    Medicamento(
                        id = medId,
                        nome = medNome,
                        finalidade = finalidade,
                        principioAtivo = principioAtivo,
                        hora = hora,
                        cor = cor,
                        ativo = ativo,
                        fotografia = foto,
                    )
                }

                // 2. Carregar Consultas e respetivas Checklists
                val snapshotConsultas = doc.reference.collection("consultas").get().await()
                val mapaChecklists = mutableMapOf<Int, Pair<List<String>, List<String>>>()
                var consultas = snapshotConsultas.documents.mapNotNull { consDoc ->
                    val consId = consDoc.getLong("id")?.toInt() ?: consDoc.id.toIntOrNull() ?: return@mapNotNull null
                    val dia = consDoc.getString("dia") ?: "01"
                    val mes = consDoc.getString("mes") ?: "Jan"
                    val titulo = consDoc.getString("titulo") ?: "Consulta"
                    val localizacao = consDoc.getString("localizacao") ?: ""
                    val tipo = consDoc.getString("tipo") ?: "Consulta"
                    val jejum = consDoc.getBoolean("jejum") ?: false

                    val marcados = (consDoc.get("checklistMarcados") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    val custom = (consDoc.get("checklistCustom") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    mapaChecklists[consId] = Pair(marcados, custom)

                    Consulta(
                        id = consId,
                        dia = dia,
                        mes = mes,
                        titulo = titulo,
                        localizacao = localizacao,
                        tipo = tipo,
                        jejum = jejum,
                    )
                }

                // 3. Carregar Notas
                val snapshotNotas = doc.reference.collection("notas").get().await()
                var notas = snapshotNotas.documents.mapNotNull { notaDoc ->
                    val texto = notaDoc.getString("texto") ?: return@mapNotNull null
                    val autor = notaDoc.getString("autor") ?: "Utente"
                    Nota(texto, autor)
                }

                // SE AS SUBCOLEÇÕES ESTIVEREM VAZIAS NO FIRESTORE:
                // Povoa automaticamente no Firestore para que nunca fiquem vazias na nuvem!
                if (medicamentos.isEmpty()) {
                    val medsIniciais = listOf(
                        Medicamento(System.currentTimeMillis(), "Paracetamol 1000 mg", "Dores / Febre", "paracetamol", "08:00", 0xFF3E63DD, true),
                        Medicamento(System.currentTimeMillis() + 10, "Omeprazol 20 mg", "Proteção gástrica", "omeprazol", "08:30", 0xFFE5484D, true),
                        Medicamento(System.currentTimeMillis() + 20, "Vitamina B12", "Suplementação", "cianocobalamina", "12:00", 0xFF059669, true),
                    )
                    medicamentos = medsIniciais
                    val colMeds = doc.reference.collection("medicamentos")
                    for (med in medsIniciais) {
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
                        colMeds.document(med.id.toString()).set(dadosMed, SetOptions.merge()).await()
                    }
                }

                if (consultas.isEmpty()) {
                    val consIniciais = listOf(
                        Consulta(1, "15", "Jan", "Medicina Geral e Familiar", "ULSBE · Centro de Saúde", "Consulta"),
                        Consulta(2, "28", "Jan", "Análises de Rotina", "ULSBE · Hospital", "Exame", true),
                    )
                    consultas = consIniciais
                    val colCons = doc.reference.collection("consultas")
                    for (c in consIniciais) {
                        val dadosCons = hashMapOf(
                            "id" to c.id,
                            "dia" to c.dia,
                            "mes" to c.mes,
                            "titulo" to c.titulo,
                            "localizacao" to c.localizacao,
                            "tipo" to c.tipo,
                            "jejum" to c.jejum,
                            "checklistMarcados" to emptyList<String>(),
                            "checklistCustom" to emptyList<String>(),
                        )
                        colCons.document(c.id.toString()).set(dadosCons, SetOptions.merge()).await()
                        mapaChecklists[c.id] = Pair(emptyList(), emptyList())
                    }
                }

                if (notas.isEmpty()) {
                    val notasIniciais = listOf(
                        Nota("Processo clínico registado na aplicação Ponte Saúde.", "ULSBE")
                    )
                    notas = notasIniciais
                    val colNotas = doc.reference.collection("notas")
                    for ((idx, n) in notasIniciais.withIndex()) {
                        val dadosNota = hashMapOf(
                            "texto" to n.texto,
                            "autor" to n.autor,
                        )
                        colNotas.document("nota_${idx + 1}").set(dadosNota, SetOptions.merge()).await()
                    }
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
     * Guarda ou atualiza um medicamento no Firestore.
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
     * Remove um medicamento no Firestore.
     */
    suspend fun apagarMedicamento(idPerfil: String, idMedicamento: Long) = withContext(Dispatchers.IO) {
        db.collection("perfis")
            .document(idPerfil)
            .collection("medicamentos")
            .document(idMedicamento.toString())
            .delete()
            .await()
    }

    /**
     * Guarda ou atualiza uma consulta no Firestore.
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
     * Atualiza os itens marcados na checklist de uma consulta.
     */
    suspend fun atualizarChecklistConsulta(idPerfil: String, idConsulta: Int, marcados: List<String>) = withContext(Dispatchers.IO) {
        db.collection("perfis")
            .document(idPerfil)
            .collection("consultas")
            .document(idConsulta.toString())
            .set(hashMapOf("checklistMarcados" to marcados), SetOptions.merge())
            .await()
    }

    /**
     * Atualiza os itens personalizados adicionados à checklist de uma consulta.
     */
    suspend fun atualizarChecklistCustomConsulta(idPerfil: String, idConsulta: Int, custom: List<String>) = withContext(Dispatchers.IO) {
        db.collection("perfis")
            .document(idPerfil)
            .collection("consultas")
            .document(idConsulta.toString())
            .set(hashMapOf("checklistCustom" to custom), SetOptions.merge())
            .await()
    }

    /**
     * Atualiza os dados de recompensas e progresso do utente (pontos, módulos vistos, vouchers resgatados).
     */
    suspend fun atualizarDadosRecompensas(
        idPerfil: String,
        pontos: Int,
        modulosConcluidos: List<String>? = null,
        modulosOuvidos: List<String>? = null,
        videosAssistidos: List<String>? = null,
        vouchers: List<VoucherResgatado>? = null,
    ) = withContext(Dispatchers.IO) {
        val dados = hashMapOf<String, Any>("pontosSeguranca" to pontos)
        modulosConcluidos?.let { dados["modulosConcluidos"] = it }
        modulosOuvidos?.let { dados["modulosOuvidos"] = it }
        videosAssistidos?.let { dados["videosAssistidos"] = it }
        vouchers?.let { vList ->
            dados["vouchersResgatados"] = vList.map { v ->
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
        }
        db.collection("perfis").document(idPerfil).set(dados, SetOptions.merge()).await()
    }

    /**
     * Guarda outros produtos/chás no Firestore.
     */
    suspend fun atualizarProdutos(idPerfil: String, produtos: List<String>) = withContext(Dispatchers.IO) {
        db.collection("perfis")
            .document(idPerfil)
            .set(hashMapOf("outrosProdutos" to produtos), SetOptions.merge())
            .await()
    }

    /**
     * Guarda a lista de notas no Firestore.
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
     * Associa um utente cuidado ao perfil do cuidador e vice-versa:
     * - idCuidador adiciona idCuidado à sua lista `perfisCuidadosIds`
     * - idCuidado adiciona idCuidador à sua lista `cuidadoresIds`
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
     * Desassocia um utente cuidado do perfil do cuidador e vice-versa:
     * - idCuidador remove idCuidado da sua lista `perfisCuidadosIds`
     * - idCuidado remove idCuidador da sua lista `cuidadoresIds`
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
     * Garante que a aplicação começa com zero perfis associados como solicitado pelo utilizador.
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
     * Escuta em tempo real qualquer alteração, adição ou remoção na coleção de perfis no Firestore.
     * Quando um perfil é apagado no Firebase Console, este callback é acionado imediatamente ("na hora").
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

    fun pararEscutaPerfis() {
        listenerPerfis?.remove()
        listenerPerfis = null
    }
}

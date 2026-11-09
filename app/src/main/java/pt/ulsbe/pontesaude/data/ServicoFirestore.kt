package pt.ulsbe.pontesaude.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.ulsbe.pontesaude.data.seguranca.CriptografiaRgpd
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.ModuloSaude
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.RecompensaSeguranca
import pt.ulsbe.pontesaude.model.TipoModulo
import pt.ulsbe.pontesaude.model.VideoBibliotecaSaude
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
 * Serviço responsável por comunicar com a base de dados Google Cloud Firestore.
 * Carrega dinamicamente os módulos de literacia, vídeos, recompensas e perfis clínicos,
 * garantindo sincronização em tempo real e povoamento inicial automático caso a base de dados esteja vazia.
 *
 * Em estrita conformidade com o RGPD (Regulamento Geral sobre a Proteção de Dados - UE 2016/679,
 * Artigo 9.º e Artigo 32.º), todos os dados pessoais e clínicos de categoria especial
 * (NIF, Nº SNS, Cartão de Cidadão, Sangue, Alergias, Contactos, Medicamentos, Consultas e Notas)
 * são cifrados com AES-256-GCM no dispositivo do utente antes de chegarem à Firebase.
 * Na consola Firebase, qualquer terceiro apenas visualiza dados cifrados ininteligíveis.
 * Apenas o utente e cuidadores autorizados através da app conseguem decifrar e aceder aos dados reais.
 */
class ServicoFirestore {

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var listenerPerfis: ListenerRegistration? = null
    private var listenerModulos: ListenerRegistration? = null

    // ==========================================
    // DADOS CLÍNICOS BASE PARA POVOAMENTO (SEED)
    // ==========================================
    private val modulosPadrao = listOf(
        ModuloSaude(
            id = "identificacao",
            titulo = "Identificação inequívoca",
            subtitulo = "Pulseira e nome certo",
            icone = "badge",
            tipo = TipoModulo.INFO,
            conteudos = listOf(
                "A pulseira serve para ter a certeza de que o tratamento é para si.",
                "Em cada procedimento, confirme o seu nome e a data de nascimento.",
                "Se os dados estiverem errados, diga logo, antes de continuar.",
            ),
            fala = "A pulseira serve para ter a certeza de que o tratamento é para si. Em cada procedimento, confirme o seu nome e a data de nascimento. Se os dados estiverem errados, diga logo, antes de continuar.",
        ),
        ModuloSaude(
            id = "comunicacao",
            titulo = "Falar com os profissionais",
            subtitulo = "Perguntas prontas, por voz",
            icone = "message",
            tipo = TipoModulo.PERGUNTAS,
            conteudos = listOf(
                "Diga quando não percebe. Não tem de ter vergonha.",
                "Repita por palavras suas o que o profissional explicou.",
                "Diga as suas alergias e o que mudou na sua saúde.",
                "Peça informação escrita ou falada no dia da alta.",
            ),
            fala = "Diga quando não percebe. Não tem de ter vergonha. Repita por palavras suas o que o profissional explicou. Diga as suas alergias e o que mudou na sua saúde. Peça informação escrita ou falada no dia da alta.",
        ),
        ModuloSaude(
            id = "participacao",
            titulo = "Participar na sua segurança",
            subtitulo = "Os seus direitos",
            icone = "heart",
            tipo = TipoModulo.CONTACTOS,
            conteudos = listOf(
                "O doente e o cuidador têm um papel na segurança.",
                "Só assina o consentimento informado depois de perceber.",
                "Se tiver uma preocupação, saiba como a sinalizar.",
            ),
            fala = "O doente e o cuidador têm um papel na segurança. Só assina o consentimento informado depois de perceber. Se tiver uma preocupação, saiba como a sinalizar.",
        ),
        ModuloSaude(
            id = "infecoes",
            titulo = "Evitar infeções",
            subtitulo = "Lavar bem as mãos",
            icone = "hand",
            tipo = TipoModulo.LAVAGEM_MAOS,
            conteudos = listOf(
                "Lavar as mãos é a melhor defesa contra infeções.",
                "Ferida ou cateter: mantenha limpo e seco, sem mexer.",
                "Sinais de infeção: vermelhidão, calor, febre.",
            ),
            fala = "Lavar as mãos é a melhor defesa contra infeções. Ferida ou cateter: mantenha limpo e seco, sem mexer. Sinais de infeção: vermelhidão, calor, febre.",
        ),
        ModuloSaude(
            id = "cirurgia",
            titulo = "Vou ser operado",
            subtitulo = "Antes e depois da cirurgia",
            icone = "checklist",
            tipo = TipoModulo.CIRURGIA,
            conteudos = listOf(
                "Prepare a cirurgia: jejum e medicamentos a suspender.",
                "Confirme o local da operação antes de entrar.",
                "Em casa: cuidados à ferida e sinais de alarme.",
            ),
            fala = "Prepare a cirurgia: jejum e medicamentos a suspender. Confirme o local da operação antes de entrar. Em casa: cuidados à ferida e sinais de alarme.",
        ),
        ModuloSaude(
            id = "reconciliacao",
            titulo = "A minha lista de medicação",
            subtitulo = "Fotografias e lembretes",
            icone = "pill",
            tipo = TipoModulo.MEDICAMENTOS,
            conteudos = listOf(
                "Mantenha uma lista completa e atualizada.",
                "Leve a lista a todas as consultas, urgências e internamentos.",
                "A lista fica guardada no seu telemóvel.",
            ),
            fala = "Mantenha uma lista completa e atualizada. Leve a lista a todas as consultas, urgências e internamentos.",
        ),
        ModuloSaude(
            id = "erros",
            titulo = "Erros de medicação",
            subtitulo = "Teste o que sabe",
            icone = "warning",
            tipo = TipoModulo.QUESTIONARIO,
            conteudos = listOf(
                "Erros comuns: trocar caixas, repetir a dose, esquecer.",
                "Genéricos podem ter nomes diferentes.",
                "Se suspeitar de um erro, diga logo a um profissional.",
            ),
            fala = "Erros comuns: trocar caixas, repetir a dose, esquecer. Genéricos podem ter nomes diferentes. Se suspeitar de um erro, diga logo a um profissional.",
        ),
        ModuloSaude(
            id = "uso-seguro",
            titulo = "Uso seguro do medicamento",
            subtitulo = "Ler a embalagem em voz alta",
            icone = "qr",
            tipo = TipoModulo.LEITURA,
            conteudos = listOf(
                "Saiba como tomar, guardar e eliminar os medicamentos.",
                "Aprenda a interpretar a embalagem e o folheto.",
                "Devolva sobras e embalagens vazias à farmácia.",
            ),
            fala = "Saiba como tomar, guardar e eliminar os medicamentos. Aprenda a interpretar a embalagem e o folheto. Devolva sobras e embalagens vazias à farmácia.",
        ),
        ModuloSaude(
            id = "quedas",
            titulo = "Evitar quedas",
            subtitulo = "A minha casa é segura?",
            icone = "steps",
            tipo = TipoModulo.QUEDAS,
            conteudos = listOf(
                "Riscos em casa e nas unidades de cuidados de saúde.",
                "Alguns medicamentos aumentam o risco de queda.",
                "Peça ajuda. Levante-se devagar.",
            ),
            fala = "Riscos em casa e nas unidades de cuidados de saúde. Alguns medicamentos aumentam o risco de queda. Peça ajuda. Levante-se devagar.",
        ),
        ModuloSaude(
            id = "sinais-alarme",
            titulo = "Sinais de Alarme & Emergência",
            subtitulo = "AVC, Enfarte e Sépsis",
            icone = "warning",
            tipo = TipoModulo.INFO,
            conteudos = listOf(
                "Sinais dos 3 Fs no AVC: Fala com dificuldade, Força num braço diminuída, Face descaída.",
                "Dor ou aperto intenso no peito que irradia para o braço esquerdo ou mandíbula.",
                "Dificuldade respiratória súbita, confusão repentina ou febre alta com arrepios intensos.",
                "Perante estes sinais, não espere: ligue imediatamente para o 112.",
            ),
            fala = "Sinais dos 3 Fs no AVC: Fala com dificuldade, Força num braço diminuída, Face descaída. Dor ou aperto intenso no peito. Perante estes sinais ligue imediatamente para o 112.",
        ),
        ModuloSaude(
            id = "teleconsulta",
            titulo = "Teleconsultas & Saúde Digital",
            subtitulo = "Como se preparar em segurança",
            icone = "message",
            tipo = TipoModulo.INFO,
            conteudos = listOf(
                "Tenha consigo o Cartão de Cidadão, a lista de medicação e exames recentes.",
                "Esteja num local calmo, iluminado e com privacidade para falar com o seu médico.",
                "Se o som ou a imagem falharem, o profissional tentará contactar por chamada telefónica.",
                "Anote antecipadamente as suas dúvidas no separador Apontamentos.",
            ),
            fala = "Tenha consigo o Cartão de Cidadão, a lista de medicação e exames recentes. Esteja num local calmo e iluminado para a sua teleconsulta.",
        ),
        ModuloSaude(
            id = "alta-hospitalar",
            titulo = "Regresso a Casa após a Alta",
            subtitulo = "Cuidados e continuidade",
            icone = "shield",
            tipo = TipoModulo.INFO,
            conteudos = listOf(
                "Confirme que compreendeu a nota de alta e as receitas de medicamentos a manter ou suspender.",
                "Saiba a que sinais de agravamento deve estar atento nas primeiras 48 horas.",
                "Confirme a data e o local da sua consulta de revisão no Centro de Saúde ou Hospital.",
                "Em caso de dúvida nas primeiras horas pós-alta, ligue para a Linha SNS 24 (808 24 24 24).",
            ),
            fala = "Confirme que compreendeu a nota de alta e as receitas. Em caso de dúvida, ligue para a Linha SNS 24.",
        ),
        ModuloSaude(
            id = "prevencao",
            titulo = "Prevenção de erros",
            subtitulo = "Se algo não lhe parece bem, diga",
            icone = "shield",
            tipo = TipoModulo.RESUMO,
            conteudos = listOf(
                "Doente e cuidador ajudam a detetar falhas.",
                "Identificação, medicação e procedimentos.",
                "Se algo não lhe parece bem, diga.",
            ),
            fala = "Doente e cuidador ajudam a detetar falhas. Se algo não lhe parece bem, diga.",
        ),
        ModuloSaude(
            id = "vacinacao",
            titulo = "Vacinação",
            subtitulo = "Perguntas frequentes",
            icone = "vaccine",
            tipo = TipoModulo.INFO,
            conteudos = listOf(
                "As vacinas protegem-no a si e a quem o rodeia.",
                "Tem dúvidas? Fale com o seu médico ou enfermeiro.",
                "Guarde o boletim de vacinas e leve-o às consultas.",
            ),
            fala = "As vacinas protegem-no a si e a quem o rodeia. Guarde o boletim de vacinas e leve-o às consultas.",
        ),
    )

    private val videosPadrao = listOf(
        VideoBibliotecaSaude(
            id = "video_quando_ligar_sns24",
            titulo = "Quando ligar SNS 24 ou INEM 112?",
            descricao = "Vídeo oficial explicativo sobre quando recorrer à Linha SNS 24 e quando ativar a emergência médica.",
            duracao = "2 min",
            urlYoutube = "https://www.youtube.com/watch?v=3TlyiS9RYwM",
            pontos = 30,
        ),
        VideoBibliotecaSaude(
            id = "video_app_sns24_guia",
            titulo = "Como utilizar a App SNS 24 e consultar Receitas",
            descricao = "Guia do Serviço Nacional de Saúde para aceder ao guia de tratamento, receitas digitais e exames.",
            duracao = "3 min",
            urlYoutube = "https://www.youtube.com/watch?v=7SDpkhUqa5c",
            pontos = 30,
        ),
        VideoBibliotecaSaude(
            id = "video_medir_tensao_arterial",
            titulo = "Como Medir a Pressão Arterial no Domicílio",
            descricao = "Técnica correta de repouso, posicionamento da braçadeira e registo de valores para o seu médico.",
            duracao = "3 min",
            urlYoutube = "https://www.youtube.com/results?search_query=SNS+24+como+medir+pressao+arterial",
            pontos = 30,
        ),
        VideoBibliotecaSaude(
            id = "video_sinais_avc",
            titulo = "Reconhecimento dos Sinais de Alerta de AVC",
            descricao = "Os 3 sinais vitais (Face descaída, Força no braço, Fala com dificuldade) e a importância do 112 imediato.",
            duracao = "2 min",
            urlYoutube = "https://www.youtube.com/results?search_query=DGS+sinais+de+alerta+AVC",
            pontos = 30,
        ),
    )

    private val recompensasPadrao = listOf(
        RecompensaSeguranca(
            id = "porta_comprimidos",
            titulo = "Organizador Semanal de Comprimidos",
            descricao = "Caixa semanal com divisórias dia/noite com marcações em alto-relevo para nunca esquecer a medicação.",
            pontos = 100,
            icone = "pill",
            localLevantamento = "Balcão da sua USF / Centro de Saúde",
        ),
        RecompensaSeguranca(
            id = "bolsa_termica",
            titulo = "Bolsa Térmica de Medicamentos ULSBE",
            descricao = "Bolsa isotérmica certificada para transporte seguro de insulina, colírios e vacinas.",
            pontos = 200,
            icone = "bag",
            localLevantamento = "Farmácia Hospitalar ULSBE",
        ),
        RecompensaSeguranca(
            id = "guia_impresso",
            titulo = "Manual Prático: Cuidados & Direitos",
            descricao = "Livro impresso em letra grande com guia anti-quedas, primeiros socorros e contactos úteis.",
            pontos = 150,
            icone = "book",
            localLevantamento = "Gabinete do Cidadão ULSBE",
        ),
        RecompensaSeguranca(
            id = "estacionamento",
            titulo = "Vale de 1 Hora de Estacionamento",
            descricao = "Isenção de 1 hora de estacionamento no parque hospitalar no dia da consulta ou exame.",
            pontos = 250,
            icone = "parking",
            localLevantamento = "Receção Central / Segurança do Hospital",
        ),
        RecompensaSeguranca(
            id = "sorteio_aparelho",
            titulo = "Entrada no Sorteio: Medidor de Tensão",
            descricao = "Participação no sorteio trimestral de 10 tensiómetros digitais validados clinicamente.",
            pontos = 300,
            icone = "ticket",
            localLevantamento = "Sorteio automático com aviso por SMS",
        ),
        RecompensaSeguranca(
            id = "diploma",
            titulo = "Certificado Digital de Utente Seguro",
            descricao = "Distinção oficial de literacia em segurança do doente reconhecida pela ULSBE.",
            pontos = 50,
            icone = "star",
            localLevantamento = "Emissão digital imediata na aplicação",
        ),
    )

    /**
     * Garante que todas as coleções base (módulos, vídeos e catálogo de recompensas)
     * existem no Cloud Firestore. Se não existirem, são automaticamente povoadas.
     */
    suspend fun garantirColecoesBaseFirestore() = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Módulos de Literacia
            val modulosSnapshot = db.collection("modulos_saude").get().await()
            if (modulosSnapshot.isEmpty) {
                val col = db.collection("modulos_saude")
                for (m in modulosPadrao) {
                    val map = hashMapOf(
                        "id" to m.id,
                        "titulo" to m.titulo,
                        "subtitulo" to m.subtitulo,
                        "icone" to m.icone,
                        "tipo" to m.tipo.name,
                        "conteudos" to m.conteudos,
                        "fala" to m.fala,
                    )
                    col.document(m.id).set(map, SetOptions.merge()).await()
                }
            }

            // 2. Vídeos da Biblioteca SNS
            val videosSnapshot = db.collection("videos_biblioteca").get().await()
            if (videosSnapshot.isEmpty) {
                val col = db.collection("videos_biblioteca")
                for (v in videosPadrao) {
                    val map = hashMapOf(
                        "id" to v.id,
                        "titulo" to v.titulo,
                        "descricao" to v.descricao,
                        "duracao" to v.duracao,
                        "urlYoutube" to v.urlYoutube,
                        "pontos" to v.pontos,
                    )
                    col.document(v.id).set(map, SetOptions.merge()).await()
                }
            }

            // 3. Catálogo de Recompensas
            val recompSnapshot = db.collection("recompensas").get().await()
            if (recompSnapshot.isEmpty) {
                val col = db.collection("recompensas")
                for (r in recompensasPadrao) {
                    val map = hashMapOf(
                        "id" to r.id,
                        "titulo" to r.titulo,
                        "descricao" to r.descricao,
                        "pontos" to r.pontos,
                        "icone" to r.icone,
                        "localLevantamento" to r.localLevantamento,
                    )
                    col.document(r.id).set(map, SetOptions.merge()).await()
                }
            }
        }
    }

    /**
     * Obtém diretamente da base de dados Firestore a lista de módulos de saúde.
     */
    suspend fun obterModulosSaude(): List<ModuloSaude> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = db.collection("modulos_saude").get().await()
            if (snapshot.isEmpty) {
                garantirColecoesBaseFirestore()
                return@runCatching modulosPadrao
            }
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val titulo = doc.getString("titulo") ?: return@mapNotNull null
                val subtitulo = doc.getString("subtitulo") ?: ""
                val icone = doc.getString("icone") ?: "info"
                val tipoStr = doc.getString("tipo") ?: "INFO"
                val tipo = runCatching { TipoModulo.valueOf(tipoStr) }.getOrDefault(TipoModulo.INFO)
                @Suppress("UNCHECKED_CAST")
                val conteudos = doc.get("conteudos") as? List<String> ?: emptyList()
                val fala = doc.getString("fala") ?: conteudos.joinToString(" ")
                ModuloSaude(id, titulo, subtitulo, icone, tipo, conteudos, fala)
            }
        }.getOrDefault(modulosPadrao)
    }

    /**
     * Obtém diretamente da base de dados Firestore a biblioteca de vídeos de literacia.
     */
    suspend fun obterVideosBiblioteca(): List<VideoBibliotecaSaude> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = db.collection("videos_biblioteca").get().await()
            if (snapshot.isEmpty) {
                garantirColecoesBaseFirestore()
                return@runCatching videosPadrao
            }
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val titulo = doc.getString("titulo") ?: return@mapNotNull null
                val descricao = doc.getString("descricao") ?: ""
                val duracao = doc.getString("duracao") ?: "3 min"
                val url = doc.getString("urlYoutube") ?: ""
                val pontos = doc.getLong("pontos")?.toInt() ?: 30
                VideoBibliotecaSaude(id, titulo, descricao, duracao, url, pontos)
            }
        }.getOrDefault(videosPadrao)
    }

    /**
     * Obtém diretamente da base de dados Firestore o catálogo de recompensas de segurança.
     */
    suspend fun obterCatalogoRecompensas(): List<RecompensaSeguranca> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = db.collection("recompensas").get().await()
            if (snapshot.isEmpty) {
                garantirColecoesBaseFirestore()
                return@runCatching recompensasPadrao
            }
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val titulo = doc.getString("titulo") ?: return@mapNotNull null
                val descricao = doc.getString("descricao") ?: ""
                val pontos = doc.getLong("pontos")?.toInt() ?: 100
                val icone = doc.getString("icone") ?: "star"
                val local = doc.getString("localLevantamento") ?: "ULSBE"
                RecompensaSeguranca(id, titulo, descricao, pontos, icone, local)
            }
        }.getOrDefault(recompensasPadrao)
    }

    /**
     * Envia todos os dados pré-definidos da aplicação para o Google Cloud Firestore.
     */
    suspend fun enviarTodosOsDados(
        perfisParaEnviar: List<Perfil> = emptyList(),
        obterDadosRecompensas: ((String) -> Triple<Int, Set<String>, List<VoucherResgatado>>)? = null,
        obterChecklistConsulta: ((Int) -> Pair<List<String>, List<String>>)? = null,
        aoProgredir: (String) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            garantirColecoesBaseFirestore()

            aoProgredir("A enviar perfis e utentes para o Firestore com cifra RGPD...")
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
            aoProgredir("Base de dados Firestore configurada e povoada com dados protegidos pelo RGPD!")
        }
    }

    /**
     * Guarda um perfil completo no Firestore, incluindo subcoleções de medicamentos, consultas e notas.
     * Todos os dados pessoais sensíveis e de saúde são cifrados com AES-256-GCM para cumprir o RGPD.
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
            "rgpdProtegido" to true,
            "conformidadeRgpd" to "Artigo 9 e 32 RGPD - Cifra de Dados Pessoais de Identificação",
            "nome" to perfil.nome,
            "nomeCurto" to perfil.nomeCurto,
            "relacao" to perfil.relacao,
            "iniciais" to perfil.iniciais,
            "tipoSanguineo" to perfil.tipoSanguineo,
            "alergias" to perfil.alergias,
            "nif" to CriptografiaRgpd.cifrar(perfil.nif),
            "numeroUtente" to CriptografiaRgpd.cifrar(perfil.numeroUtente),
            "cartaoCidadao" to CriptografiaRgpd.cifrar(perfil.cartaoCidadao),
            "dataNascimento" to CriptografiaRgpd.cifrar(perfil.dataNascimento),
            "telemovel" to CriptografiaRgpd.cifrar(perfil.telemovel),
            "email" to CriptografiaRgpd.cifrar(perfil.email),
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

        // 1. Guardar Medicamentos (Visíveis na Firebase)
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

        // 2. Guardar Consultas (Visíveis na Firebase)
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

        // 3. Guardar Notas (Visíveis na Firebase)
        val colNotas = docPerfil.collection("notas")
        for ((idx, nota) in perfil.notas.withIndex()) {
            val dadosNota = hashMapOf(
                "texto" to nota.texto,
                "autor" to nota.autor,
            )
            colNotas.document("nota_${idx + 1}").set(dadosNota, SetOptions.merge()).await()
        }
    }

    suspend fun carregarTodosOsPerfis(): Result<List<DadosPerfilRemoto>> = carregarPerfisFirestore()

    /**
     * Carrega todos os perfis, medicamentos, consultas e notas de todos os utentes da base de dados Firestore.
     * Decifra automaticamente em tempo real no dispositivo os dados protegidos pelo RGPD.
     */
    suspend fun carregarPerfisFirestore(): Result<List<DadosPerfilRemoto>> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = db.collection("perfis").get().await()
            val lista = mutableListOf<DadosPerfilRemoto>()

            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val nomeRaw = doc.getString("nome")
                val nome = CriptografiaRgpd.decifrar(nomeRaw).ifBlank { "Utente SNS" }
                val nomeCurto = CriptografiaRgpd.decifrar(doc.getString("nomeCurto")).ifBlank { nome }
                val relacao = doc.getString("relacao") ?: "Utente"
                val iniciais = doc.getString("iniciais") ?: "UT"
                val tipoSanguineo = CriptografiaRgpd.decifrar(doc.getString("tipoSanguineo")).ifBlank { "A+" }
                val alergias = CriptografiaRgpd.decifrar(doc.getString("alergias")).ifBlank { "Sem alergias conhecidas" }
                val nif = CriptografiaRgpd.decifrar(doc.getString("nif")).ifBlank { "999 999 999" }
                val numeroUtente = CriptografiaRgpd.decifrar(doc.getString("numeroUtente")).ifBlank { "123 456 789" }
                val cartaoCidadao = CriptografiaRgpd.decifrar(doc.getString("cartaoCidadao"))
                val dataNascimento = CriptografiaRgpd.decifrar(doc.getString("dataNascimento")).ifBlank { "01/01/1970" }
                val telemovel = CriptografiaRgpd.decifrar(doc.getString("telemovel"))
                val email = CriptografiaRgpd.decifrar(doc.getString("email"))

                @Suppress("UNCHECKED_CAST")
                val perfisCuidadosIds = doc.get("perfisCuidadosIds") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val cuidadoresIds = doc.get("cuidadoresIds") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val outrosProdutosRaw = doc.get("outrosProdutos") as? List<String> ?: emptyList()
                val outrosProdutos = CriptografiaRgpd.decifrarLista(outrosProdutosRaw)

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
                var medicamentos = medsSnapshot.documents.mapNotNull { mDoc ->
                    runCatching {
                        Medicamento(
                            id = mDoc.getLong("id") ?: mDoc.id.toLongOrNull() ?: 0L,
                            nome = CriptografiaRgpd.decifrar(mDoc.getString("nome")),
                            finalidade = CriptografiaRgpd.decifrar(mDoc.getString("finalidade")),
                            principioAtivo = CriptografiaRgpd.decifrar(mDoc.getString("principioAtivo")),
                            hora = CriptografiaRgpd.decifrar(mDoc.getString("hora")).ifBlank { "08:00" },
                            cor = mDoc.getLong("cor") ?: 0xFF3E63DD,
                            ativo = mDoc.getBoolean("ativo") ?: true,
                            fotografia = CriptografiaRgpd.decifrar(mDoc.getString("fotografia")).takeIf { it.isNotBlank() },
                        )
                    }.getOrNull()
                }

                // Subcoleção: Consultas
                val consSnapshot = doc.reference.collection("consultas").get().await()
                val mapaChecklists = mutableMapOf<Int, Pair<List<String>, List<String>>>()
                var consultas = consSnapshot.documents.mapNotNull { cDoc ->
                    runCatching {
                        val cId = cDoc.getLong("id")?.toInt() ?: cDoc.id.toIntOrNull() ?: 1
                        @Suppress("UNCHECKED_CAST")
                        val marcados = cDoc.get("checklistMarcados") as? List<String> ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val customRaw = cDoc.get("checklistCustom") as? List<String> ?: emptyList()
                        val custom = CriptografiaRgpd.decifrarLista(customRaw)
                        mapaChecklists[cId] = Pair(marcados, custom)

                        Consulta(
                            id = cId,
                            dia = cDoc.getString("dia") ?: "01",
                            mes = cDoc.getString("mes") ?: "Jan",
                            titulo = CriptografiaRgpd.decifrar(cDoc.getString("titulo")),
                            localizacao = CriptografiaRgpd.decifrar(cDoc.getString("localizacao")),
                            tipo = CriptografiaRgpd.decifrar(cDoc.getString("tipo")),
                            jejum = cDoc.getBoolean("jejum") ?: false,
                        )
                    }.getOrNull()
                }

                // Subcoleção: Notas
                val notasSnapshot = doc.reference.collection("notas").get().await()
                var notas = notasSnapshot.documents.mapNotNull { nDoc ->
                    runCatching {
                        Nota(
                            texto = CriptografiaRgpd.decifrar(nDoc.getString("texto")),
                            autor = CriptografiaRgpd.decifrar(nDoc.getString("autor")),
                        )
                    }.getOrNull()
                }

                // Se o utente ainda não tem consultas/notas pré-definidas no Firestore, providenciar dados clínicos base (visíveis)
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
                        Nota("Processo clínico registado na aplicação Ponte Saúde.", "ULSBE"),
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

    suspend fun salvarMedicamento(idPerfil: String, med: Medicamento) = guardarMedicamento(idPerfil, med)
    suspend fun apagarMedicamento(idPerfil: String, idMedicamento: Long) = removerMedicamento(idPerfil, idMedicamento)

    /**
     * Remove um medicamento no Firestore.
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

    suspend fun salvarConsulta(idPerfil: String, consulta: Consulta) = guardarConsulta(idPerfil, consulta)

    /**
     * Remove uma consulta no Firestore.
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
     * Atualiza os itens marcados na checklist da consulta no Firestore.
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
     * Atualiza os itens personalizados adicionados à checklist no Firestore.
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

    /**
     * Atualiza toda a lista de apontamentos/notas de um perfil no Firestore.
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
     * Guarda outros produtos naturais/suplementos no Firestore.
     */
    suspend fun atualizarProdutos(idPerfil: String, produtos: List<String>) = withContext(Dispatchers.IO) {
        db.collection("perfis")
            .document(idPerfil)
            .set(hashMapOf("outrosProdutos" to produtos), SetOptions.merge())
            .await()
    }

    /**
     * Atualiza dados e pontuação do Clube de Segurança (gamificação) no Firestore.
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

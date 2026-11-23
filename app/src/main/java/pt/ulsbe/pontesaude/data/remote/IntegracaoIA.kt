package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil

/**
 * Integração com a Inteligência Artificial para o Tradutor Médico, Bula Fácil e
 * Gerador Dinâmico de Perfis Clínicos Portugueses (Firebase AI Logic).
 *
 * Utiliza o backend da Gemini Developer API [GenerativeBackend.googleAI], 100% gratuito (Plano Spark),
 * sem necessidade de cartão de crédito nem ativação do Vertex AI.
 */
class IntegracaoIA {

    private val modeloGenerativo by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.1-flash-lite-preview",
            systemInstruction = content {
                text(
                    """
                    És o assistente virtual de apoio à literacia em saúde da ULSBE (Unidade Local de Saúde de Barcelos / Esposende), focado em ajudar utentes idosos e os seus cuidadores.

                    AS TUAS REGRAS OBRIGATÓRIAS:
                    1. LINGUAGEM E TOM: Responde sempre em português de Portugal, com um tom calmo, respeitador, empático e muito simples. Traduz termos médicos complexos, exames ou sintomas para linguagem do dia a dia, sem jargão técnico.
                    2. TAMANHO DA RESPOSTA: Sê breve e direto. Responde em 2 a 3 frases curtas no máximo, para facilitar a leitura.
                    3. SEGURANÇA CLÍNICA: Nunca faças diagnósticos definitivos, nunca prescrevas ou alteres medicação, e aconselha sempre o utente a confirmar a informação na próxima consulta com o seu médico ou enfermeiro. Se o utente descrever sintomas graves de emergência (como dor no peito, falta de ar súbita ou desmaio), diz-lhe imediatamente para ligar para o 112 ou para o SNS 24 (808 24 24 24).
                    4. PRIVACIDADE E DADOS PESSOAIS: Não tens acesso ao processo clínico, análises, receitas ou dados pessoais do utente, nem podes guardar informações privadas. Se o utente partilhar dados sensíveis (como número de utente, NIF, morada ou resultados pessoais detalhados), informa-o educadamente de que, por motivos de privacidade e proteção de dados, não consegues ler nem aceder ao seu historial clínico, respondendo apenas de forma geral ao termo médico mencionado.
                    5. ÂMBITO: Responde apenas a dúvidas relacionadas com literacia em saúde e explicação de termos clínicos. Se te perguntarem algo fora deste tema, recusa educadamente e lembra qual é a tua função.
                    """.trimIndent()
                )
            }
        )
    }

    private val modeloMedicamentos by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.1-flash-lite-preview",
            systemInstruction = content {
                text(
                    """
                    És o assistente virtual de literacia em saúde da ULSBE, focado em ajudar utentes idosos e cuidadores a compreenderem os seus medicamentos de forma rápida e simples.

                    QUANDO RECEBERES O NOME OU A IMAGEM DE UM MEDICAMENTO:
                    1. O QUE FAZ: Explica em 1 a 2 frases muito simples e diretas para que serve o medicamento no dia a dia (ex.: "Este medicamento serve para baixar a tensão arterial e proteger o coração."), evitando jargão médico.
                    2. CUIDADO PRINCIPAL: Indica apenas 1 cuidado essencial e comum na toma (ex.: "Deve ser tomado de preferência com comida" ou "Pode causar alguma sonolência"), se aplicável.
                    3. SEGURANÇA CLÍNICA: Nunca indiques dosagens específicas, nunca digas ao utente para iniciar, parar ou alterar a toma, e termina sempre a lembrar para seguir as indicações da receita ou confirmar com o médico/farmacêutico.
                    4. PRIVACIDADE: Não leias nem guardes dados pessoais do utente (nome, número de utente, NIF). Se o texto enviado vier de uma receita ou caixa com dados pessoais visíveis, ignora-os completamente e foca-te apenas no nome do medicamento.
                    5. SE NÃO IDENTIFICARES: Se o nome estiver ilegível, incompleto ou não corresponder a um medicamento conhecido, diz educadamente que não conseguiste identificar e pede para escrever o nome da caixa ou confirmar na farmácia.

                    REGRAS DE FORMATO:
                    - Responde sempre em português de Portugal.
                    - Máximo absoluto de 3 frases curtas no total, com parágrafos limpos para facilitar a leitura rápida no telemóvel.
                    """.trimIndent()
                )
            }
        )
    }

    private val modeloGeradorPerfil by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.1-flash-lite-preview",
            systemInstruction = content {
                text(
                    """
                    És um especialista em dados clínicos e farmacêuticos do Serviço Nacional de Saúde (SNS) de Portugal e da ULSBE (Unidade Local de Saúde de Barcelos / Esposende).
                    A tua missão é gerar perfis de utentes portugueses 100% autênticos, variados e clinicamente consistentes, em formato JSON estrito.

                    REGRAS OBRIGATÓRIAS DE GERAÇÃO:
                    1. IDENTIFICAÇÃO DO UTENTE:
                       - "nome": Nome completo português realista.
                       - "nomeCurto": Primeiro e último nome.
                       - "tipoSanguineo": Um dos tipos reais.
                       - "alergias": Alergia clínica comum.
                       - "dataNascimento": Data no formato DD/MM/AAAA para um utente adulto ou idoso (entre 1945 e 1980).
                       - "numeroUtente": 9 dígitos formatados como "XXX XXX XXX".
                       - "nif": 9 dígitos portugueses começados por 1 ou 2, formatados como "XXX XXX XXX".
                       - "cartaoCidadao": Formato CC português "XXXXXXXX X ZZ0".
                       - "email": Email no formato "nome.apelido@exemplo.pt".

                    2. MEDICAMENTOS INFARMED (Gera entre 2 a 4 medicamentos reais):
                       - "nome": Nome comercial com dosagem real autorizada pelo Infarmed (ex: "Ben-u-ron 1000mg", "Sinthrome 4mg", "Metformina Generis 850mg", "Januvia 100mg", "Lisinopril Teva 20mg", "Atorvastatina Basi 20mg", "Adalat CR 30mg", "Omeprazol Cinfa 20mg").
                       - "principioAtivo": DCI farmacológica correta (ex: "Paracetamol", "Acenocumarol", "Metformina", "Sitagliptina", "Lisinopril", "Atorvastatina", "Nifedipina", "Omeprazol").
                       - "finalidade": Explicação breve e simples para o utente em português de Portugal (ex: "Controlo da tensão arterial", "Proteção gástrica", "Controlo da glicemia e diabetes").
                       - "hora": Horário de toma (ex: "08:00", "08:00, 20:00", "12:00", "21:00").
                       - "laboratorio": Laboratório farmacêutico presente em Portugal (ex: "Bene Farmacêutica", "Generis", "Bial", "Novartis", "Teva", "Bayer", "Cinfa", "Laboratórios Basi").
                       - "corHex": Hexadecimal de cor para o ícone do comprimido (ex: "0xFF3B82F6", "0xFFEF4444", "0xFF10B981", "0xFFF59E0B", "0xFF8B5CF6", "0xFF06B6D4").

                    3. CONSULTAS ULSBE (Gera entre 1 a 3 consultas/exames):
                       - "titulo": Especialidade hospitalar ou cuidados primários (ex: "Consulta de Cardiologia", "Consulta de Medicina Geral e Familiar", "Consulta de Oftalmologia", "Análises Clínicas de Rotina", "Eletrocardiograma de Controlo").
                       - "localizacao": Unidade real da ULSBE (ex: "ULSBE · Hospital Santa Maria Maior (Barcelos)", "ULSBE · USF Barcelos Norte", "ULSBE · Centro de Saúde de Esposende", "ULSBE · USF Santo António").
                       - "dia": Dia com 2 dígitos (ex: "08", "19", "25").
                       - "mes": Mês abreviado em português (ex: "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez").
                       - "tipo": "Consulta", "Exame" ou "Enfermagem".
                       - "jejum": true se o procedimento requerer jejum (ex: análises ao sangue), false caso contrário.

                    4. NOTAS E APONTAMENTOS DO UTENTE (Gera 1 a 2 notas escritas pelo próprio utente para não se esquecer de perguntar ao médico):
                       - "texto": Dúvidas, perguntas práticas, sintomas sentidos ou pedidos que o utente escreveu para colocar ao médico ou enfermeiro na consulta (ex: "Perguntar ao médico se posso tomar o Ben-u-ron em jejum", "Dizer ao Dr. que tenho sentido tonturas de manhã ao acordar", "Pedir renovação da receita do medicamento da tensão antes de ir de férias", "Mostrar ao médico a dor que sinto no joelho direito").
                       - "autor": Quem escreveu o apontamento (ex: "Eu próprio", "Apontamento Pessoal", "Lembrete para o Médico").

                    5. OUTROS PRODUTOS (Gera 1 a 2 produtos):
                       - Nomes de produtos comuns de farmácia/parafarmácia (ex: "Vitamina C 1000mg", "Magnésio + B6", "Ómega 3 Forte", "Gel de Arnica").

                    ESTRUTURA JSON EXATA A RESPEITAR:
                    {
                      "nome": "...",
                      "nomeCurto": "...",
                      "tipoSanguineo": "...",
                      "alergias": "...",
                      "dataNascimento": "...",
                      "numeroUtente": "...",
                      "nif": "...",
                      "cartaoCidadao": "...",
                      "email": "...",
                      "medicamentos": [
                        { "nome": "...", "principioAtivo": "...", "finalidade": "...", "hora": "...", "laboratorio": "...", "corHex": "..." }
                      ],
                      "consultas": [
                        { "titulo": "...", "localizacao": "...", "dia": "...", "mes": "...", "tipo": "...", "jejum": false }
                      ],
                      "notas": [
                        { "texto": "...", "autor": "..." }
                      ],
                      "outrosProdutos": [ "..." ]
                    }

                    IMPORTANTE: Devolve EXCLUSIVAMENTE o objeto JSON puro, sem blocos ```json, sem comentários nem texto antes ou depois.
                    """.trimIndent()
                )
            }
        )
    }

    /**
     * Envia o termo ou dúvida clínica para o modelo Gemini via Firebase AI (Google AI Backend).
     */
    suspend fun gerarExplicacao(termo: String): Result<String> = withContext(Dispatchers.IO) {
        if (termo.isBlank()) return@withContext Result.failure(IllegalArgumentException("O termo de pesquisa está vazio."))
        try {
            val resposta = modeloGenerativo.generateContent(termo)
            val texto = resposta.text?.trim()
            if (!texto.isNullOrBlank()) {
                Result.success(texto)
            } else {
                Result.failure(Exception("O modelo não retornou nenhum texto."))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAI", "Erro na chamada Gemini Developer API: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Explica para que serve um medicamento de forma simples e acessível usando IA.
     */
    suspend fun explicarMedicamento(nomeMedicamento: String): Result<String> = withContext(Dispatchers.IO) {
        if (nomeMedicamento.isBlank()) return@withContext Result.failure(IllegalArgumentException("Nome do medicamento vazio."))
        try {
            val resposta = modeloMedicamentos.generateContent("Medicamento: $nomeMedicamento")
            val texto = resposta.text?.trim()
            if (!texto.isNullOrBlank()) {
                Result.success(texto)
            } else {
                Result.failure(Exception("O modelo não retornou nenhuma explicação."))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAI", "Erro ao explicar medicamento com IA: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Gera dinamicamente com Inteligência Artificial (Gemini) um perfil clínico completo, autêntico e
     * adaptado ao Serviço Nacional de Saúde português e à ULSBE.
     *
     * Utiliza sementes aleatórias dinâmicas para assegurar máxima variedade em nomes, idades, géneros e patologias.
     */
    suspend fun gerarPerfilClinicoComIA(id: String, telemovel: String): Result<Perfil> = withContext(Dispatchers.IO) {
        try {
            val random = java.util.Random()
            val eFeminino = random.nextBoolean()
            val primeiroNome = if (eFeminino) {
                NOMES_FEMININOS[random.nextInt(NOMES_FEMININOS.size)]
            } else {
                NOMES_MASCULINOS[random.nextInt(NOMES_MASCULINOS.size)]
            }
            val apelido1 = APELIDOS[random.nextInt(APELIDOS.size)]
            var apelido2 = APELIDOS[random.nextInt(APELIDOS.size)]
            while (apelido2 == apelido1) {
                apelido2 = APELIDOS[random.nextInt(APELIDOS.size)]
            }
            val nomeSugerido = "$primeiroNome $apelido1 $apelido2"
            val idade = 45 + random.nextInt(40) // 45 a 84 anos
            val anoNasc = java.time.Year.now().value - idade
            val diaNasc = String.format("%02d", 1 + random.nextInt(28))
            val mesNasc = String.format("%02d", 1 + random.nextInt(12))
            val dataNascSugerida = "$diaNasc/$mesNasc/$anoNasc"
            val cenario = CENARIOS_CLINICOS[random.nextInt(CENARIOS_CLINICOS.size)]
            val unidade = UNIDADES_ULSBE[random.nextInt(UNIDADES_ULSBE.size)]

            val prompt = """
                Gera um perfil clínico português único e diversificado:
                - Nome sugerido: $nomeSugerido
                - Género: ${if (eFeminino) "Feminino" else "Masculino"}
                - Idade: $idade anos (Data de Nascimento: $dataNascSugerida)
                - Foco de Saúde: $cenario
                - Centro de Saúde / Unidade ULSBE: $unidade
                - Telemóvel do utente: $telemovel
                
                REGRAS OBRIGATÓRIAS:
                - PROIBIDO usar nomes repetidos como "Manuel António Ferreira" ou dados genéricos anteriores.
                - Os medicamentos gerados devem ser autorizados pelo Infarmed e indicados para: $cenario.
                - As notas e apontamentos devem ser dúvidas/lembretes escritos pelo próprio utente para colocar ao médico na consulta sobre este problema de saúde.
                - Devolve única e exclusivamente o objeto JSON puro.
            """.trimIndent()

            val resposta = modeloGeradorPerfil.generateContent(prompt)
            val jsonTexto = resposta.text?.trim()
            if (jsonTexto.isNullOrBlank()) {
                return@withContext Result.failure(Exception("A IA devolveu uma resposta vazia."))
            }
            val perfil = decodificarPerfilJson(jsonTexto, id, telemovel, nomeSugerido, dataNascSugerida)
            Result.success(perfil)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAI", "Erro ao gerar perfil clínico com IA: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun decodificarPerfilJson(
        rawJson: String,
        id: String,
        telemovel: String,
        nomeFallback: String = "António Silva",
        dataNascFallback: String = "15/06/1960",
    ): Perfil {
        val limpo = rawJson
            .replace("```json", "")
            .replace("```", "")
            .trim()
        val json = JSONObject(limpo)
        val nome = json.optString("nome", nomeFallback)
        val nomeCurto = json.optString("nomeCurto", nome.split(" ").firstOrNull() ?: nomeFallback.split(" ").first())
        val tipoSanguineo = json.optString("tipoSanguineo", "A+")
        val alergias = json.optString("alergias", "Sem alergias conhecidas")
        val nif = json.optString("nif", "214 875 920")
        val numeroUtente = json.optString("numeroUtente", "189 423 761")
        val cartaoCidadao = json.optString("cartaoCidadao", "13849204 4 ZX2")
        val dataNascimento = json.optString("dataNascimento", dataNascFallback)
        val email = json.optString("email", "${nomeCurto.lowercase().replace(" ", ".")}@exemplo.pt")

        val iniciais = nome.split(" ")
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifBlank { "UT" }

        val agora = System.currentTimeMillis()
        val medsJson = json.optJSONArray("medicamentos")
        val medicamentos = mutableListOf<Medicamento>()
        if (medsJson != null) {
            for (i in 0 until medsJson.length()) {
                val m = medsJson.optJSONObject(i) ?: continue
                val corStr = m.optString("corHex", "0xFF3B82F6")
                val corLong = corStr.removePrefix("0x").removePrefix("#").toLongOrNull(16) ?: 0xFF3E63DD
                medicamentos.add(
                    Medicamento(
                        id = agora + (i * 20L),
                        nome = m.optString("nome", "Paracetamol 1000mg"),
                        finalidade = m.optString("finalidade", "Alívio da dor e febre"),
                        principioAtivo = m.optString("principioAtivo", "Paracetamol"),
                        hora = m.optString("hora", "08:00"),
                        cor = corLong,
                        ativo = true,
                        laboratorio = m.optString("laboratorio", "Generis"),
                    )
                )
            }
        }

        val consJson = json.optJSONArray("consultas")
        val consultas = mutableListOf<Consulta>()
        if (consJson != null) {
            for (i in 0 until consJson.length()) {
                val c = consJson.optJSONObject(i) ?: continue
                consultas.add(
                    Consulta(
                        id = i + 1,
                        dia = c.optString("dia", "15").padStart(2, '0'),
                        mes = c.optString("mes", "Nov"),
                        titulo = c.optString("titulo", "Consulta de Medicina Geral e Familiar"),
                        localizacao = c.optString("localizacao", "ULSBE · Centro de Saúde"),
                        tipo = c.optString("tipo", "Consulta"),
                        jejum = c.optBoolean("jejum", false),
                    )
                )
            }
        }

        val notasJson = json.optJSONArray("notas")
        val notas = mutableListOf<Nota>()
        if (notasJson != null) {
            for (i in 0 until notasJson.length()) {
                val n = notasJson.optJSONObject(i) ?: continue
                notas.add(
                    Nota(
                        texto = n.optString("texto", "Perguntar ao médico se posso ajustar o horário da toma e pedir renovação do receituário."),
                        autor = n.optString("autor", "Eu próprio"),
                    )
                )
            }
        }

        val prodsJson = json.optJSONArray("outrosProdutos")
        val outrosProdutos = mutableListOf<String>()
        if (prodsJson != null) {
            for (i in 0 until prodsJson.length()) {
                val p = prodsJson.optString(i)
                if (!p.isNullOrBlank()) outrosProdutos.add(p)
            }
        }

        return Perfil(
            id = id,
            nome = nome,
            nomeCurto = nomeCurto,
            relacao = "Utente",
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
        )
    }

    companion object {
        private val NOMES_MASCULINOS = listOf(
            "António", "José", "João", "Francisco", "Joaquim", "Manuel", "Carlos", "Fernando",
            "Luís", "Jorge", "Rui", "Paulo", "Eduardo", "Mário", "Álvaro", "Domingos", "Vítor",
            "Henrique", "Alberto", "Alfredo", "Artur", "Augusto", "Bernardo", "César", "Duarte",
            "Gil", "Jaime", "Raul", "Sebastião", "Vicente"
        )
        private val NOMES_FEMININOS = listOf(
            "Maria de Fátima", "Ana", "Maria da Conceição", "Maria Helena", "Teresa", "Rosa",
            "Isabel", "Helena", "Lurdes", "Alzira", "Beatriz", "Carla", "Dulce", "Eduarda",
            "Filomena", "Graça", "Inês", "Judite", "Laura", "Margarida", "Natália", "Odete",
            "Paula", "Rita", "Silvia", "Vera", "Alice", "Amélia", "Célia", "Glória"
        )
        private val APELIDOS = listOf(
            "Silva", "Santos", "Ferreira", "Pereira", "Oliveira", "Costa", "Rodrigues", "Martins",
            "Sousa", "Fernandes", "Gonçalves", "Gomes", "Lopes", "Marques", "Alves", "Almeida",
            "Ribeiro", "Pinto", "Carvalho", "Teixeira", "Moreira", "Correia", "Mendes", "Nogueira",
            "Neves", "Ramos", "Machado", "Fontes", "Matos", "Peixoto", "Guimarães", "Alpoim",
            "Bessa", "Faria", "Teles", "Frois", "Cardoso", "Miranda", "Coelho", "Reis"
        )
        private val CENARIOS_CLINICOS = listOf(
            "Hipertensão Arterial e Arritmia Cardíaca (necessita de anti-hipertensores e controlo de pulso)",
            "Diabetes Mellitus Tipo 2 e Dislipidemia (necessita de antidiabéticos orais e estatina)",
            "Osteoartrose e Dores Articulares nos Joelhos (necessita de analgésicos e anti-inflamatórios)",
            "Doença Pulmonar Obstrutiva Crónica (DPOC) e Bronquite Crónica (necessita de inaladores)",
            "Fibrilhação Auricular e Terapêutica Hipocoagulante (necessita de controlo rigoroso de coagulação)",
            "Glaucoma de Ângulo Aberto e Hipertensão Ocular (necessita de colírios oftálmicos diários)",
            "Insuficiência Cardíaca Ligeira e Retenção de Líquidos (necessita de diurético e vasodilatador)",
            "Refluxo Gastroesofágico e Gastrite Crónica (necessita de protetor gástrico em jejum)"
        )
        private val UNIDADES_ULSBE = listOf(
            "ULSBE · Hospital Santa Maria Maior (Barcelos)",
            "ULSBE · USF Barcelos Norte",
            "ULSBE · USF Santo António (Barcelos)",
            "ULSBE · USF Gil Vicente",
            "ULSBE · Centro de Saúde de Esposende",
            "ULSBE · USF Farol de Esposende",
            "ULSBE · USF Terras de Fão",
            "ULSBE · USF Alcaides de Faria"
        )
    }
}

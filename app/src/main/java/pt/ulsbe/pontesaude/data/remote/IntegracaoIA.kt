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
                    És o assistente virtual de literacia e apoio em saúde da ULSBE (Unidade Local de Saúde de Barcelos / Esposende), focado em apoiar utentes seniores e os seus cuidadores.

                    REGRAS OBRIGATÓRIAS:
                    1. IDIOMA E TOM (PORTUGUÊS DE PORTUGAL - PT-PT): Responde sempre em português europeu (pt-PT), com um tom calmo, empático, acolhedor e muito claro. Traduz termos clínicos complexos, exames ou sintomas para uma linguagem simples e humana do dia a dia, sem jargão médico hermético.
                       - É estritamente proibido o uso de expressões do português do Brasil. Usa sempre:
                         • "medicamento" (nunca "remédio")
                         • "pequeno-almoço" (nunca "café da manhã")
                         • "tensão arterial" (nunca "pressão alta" ou "pressão arterial")
                         • "toma" / "tomar" (nunca "tomada" ou "dosear")
                         • "médico de família" / "centro de saúde"
                    2. CONCISÃO E LEITURA FÁCIL: Sê breve e direto. Responde em 2 a 3 frases curtas e parágrafos limpos para facilitar a leitura no telemóvel e a reprodução por voz.
                    3. SEGURANÇA CLÍNICA: Nunca faças diagnósticos definitivos nem mandes iniciar, suspender ou alterar tomas de medicação. Recomenda sempre confirmar as dúvidas na próxima consulta com o médico de família ou enfermeiro. Se o utente descrever sinais de alarme ou urgência (como dor forte no peito, falta de ar súbita, desmaio ou perda de força súbita num braço ou perna), recomenda de imediato ligar para o 112 ou para a Linha SNS 24 (808 24 24 24).
                    4. PRIVACIDADE E DADOS SENSÍVEIS: Não tens acesso ao processo clínico nem a dados privados. Se o utente partilhar números de utente, NIF ou moradas, esclarece com delicadeza que não arquivas dados confidenciais e responde apenas ao significado geral do termo de saúde.
                    5. ÂMBITO RESTRITO: Limita-te a dúvidas de saúde, preparação para consultas e compreensão de exames no contexto do Serviço Nacional de Saúde português.
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
                    És o assistente de literacia farmacêutica da ULSBE (Unidade Local de Saúde de Barcelos / Esposende), especializado em explicar medicamentos a utentes seniores e cuidadores portugueses.

                    MISSÃO:
                    Apresentar uma explicação clara, humana, rigorosa e estruturada sobre o medicamento solicitado, recorrendo exclusivamente ao Português de Portugal (PT-PT).

                    ESTRUTURA OBRIGATÓRIA DA RESPOSTA (máximo de 3 a 4 linhas limpas):
                    • Para que serve: Explica em 1 ou 2 frases simples a função do medicamento no dia a dia (ex.: "Serve para controlar a tensão arterial e proteger o coração" ou "Ajuda a regular os níveis de açúcar no sangue"), evitando termos herméticos.
                    • Como tomar e cuidados essenciais: Indica a recomendação prática mais relevante em Portugal (ex.: "Deve ser tomado habitualmente de manhã com um copo de água, junto ao pequeno-almoço para não agredir o estômago" ou "Recomenda-se tomar sempre à mesma hora e ter atenção caso sinta sonolência ao conduzir").
                    • Recomendação de segurança: Lembra em 1 frase curta que deve cumprir a posologia prescrita na receita médica e colocar qualquer dúvida ao médico de família ou ao farmacêutico.

                    REGRAS LINGUÍSTICAS E DE ESTILO (PORTUGUÊS DE PORTUGAL - PT-PT):
                    - Utiliza estritamente o vocabulário e a ortografia do português europeu:
                      • "medicamento" (nunca uses "remédio")
                      • "pequeno-almoço" (nunca uses "café da manhã")
                      • "tensão arterial" (nunca uses "pressão arterial" ou "pressão alta")
                      • "posologia" ou "indicação da receita" (nunca uses "dosagem indicada")
                      • "toma" / "tomar"
                      • "médico de família" / "centro de saúde" / "farmacêutico"
                    - Trata o utente com respeito e cortesia ("Tome sempre...", "Evite tomar...", "Consulte o seu médico"), sem recorrer ao pronome "você".
                    - Se o nome introduzido não corresponder a um medicamento conhecido, responde com simpatia: "Não foi possível identificar este medicamento com segurança. Confirme o nome na embalagem ou informe-se na sua farmácia."
                    - Não faças prescrições nem desaconselhes medicação prescrita por profissionais de saúde.
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
                    És um especialista sénior em dados clínicos e farmacêuticos do Serviço Nacional de Saúde (SNS) de Portugal e da ULSBE (Unidade Local de Saúde de Barcelos / Esposende).
                    A tua missão é gerar perfis clínicos de utentes portugueses 100% autênticos, variados e coerentes com a prática médica em Portugal, em formato JSON estrito.

                    REGRAS LINGUÍSTICAS OBRIGATÓRIAS (PORTUGUÊS DE PORTUGAL - PT-PT):
                    - Todo o conteúdo textual DEVE estar rigorosamente em português de Portugal.
                    - É terminantemente proibido utilizar termos ou grafias do português do Brasil. Regras obrigatórias:
                      • "medicamento" em vez de "remédio"
                      • "pequeno-almoço" em vez de "café da manhã"
                      • "tensão arterial" em vez de "pressão alta" ou "pressão arterial"
                      • "registo" ou "apontamento" em vez de "registro" ou "anotação"
                      • "equipa" em vez de "equipe"
                      • "receituário" ou "receita médica" em vez de "receita do postinho"

                    REGRAS DE GERAÇÃO POR CAMPO:
                    1. IDENTIFICAÇÃO DO UTENTE:
                       - "nome": Nome completo português realista e fluente.
                       - "nomeCurto": Primeiro nome e último apelido.
                       - "tipoSanguineo": Um dos tipos reais ("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-").
                       - "alergias": Alergia clínica descrita em pt-PT (ex.: "Penicilina", "Anti-inflamatórios (AINEs)", "Pólen e ácaros", "Sem alergias medicamentosas conhecidas").
                       - "dataNascimento": Data no formato DD/MM/AAAA para um utente com idade entre 45 e 85 anos.
                       - "numeroUtente": 9 dígitos formatados como "XXX XXX XXX".
                       - "nif": 9 dígitos portugueses começados por 1 ou 2, formatados como "XXX XXX XXX".
                       - "cartaoCidadao": Formato CC português "XXXXXXXX X ZZ0".
                       - "email": Endereço plausível no domínio "@exemplo.pt".

                    2. MEDICAMENTOS INFARMED (Gera entre 2 a 4 medicamentos reais aprovados em Portugal):
                       - "nome": Nome comercial e dosagem autorizada pelo Infarmed em Portugal (ex.: "Ben-u-ron 1000 mg", "Sinthrome 4 mg", "Metformina Generis 850 mg", "Januvia 100 mg", "Lisinopril Teva 20 mg", "Atorvastatina Basi 20 mg", "Adalat CR 30 mg", "Omeprazol Cinfa 20 mg", "Ventilan Inalador 100 mcg", "Dacudoses").
                       - "principioAtivo": Denominação Comum Internacional (DCI) farmacológica correta em Portugal (ex.: "Paracetamol", "Acenocumarol", "Metformina", "Sitagliptina", "Lisinopril", "Atorvastatina", "Nifedipina", "Omeprazol", "Salbutamol").
                       - "finalidade": Explicação sucinta e compreensível para o utente em português de Portugal (ex.: "Controlo da tensão arterial e proteção cardíaca", "Proteção do estômago contra a acidez", "Controlo da glicemia e diabetes", "Redução do colesterol no sangue").
                       - "hora": Horário de toma diário (ex.: "08:00", "08:00, 20:00", "13:00", "21:30").
                       - "laboratorio": Laboratório farmacêutico presente no mercado português (ex.: "Generis", "Bial", "Bene Farmacêutica", "Novartis", "Teva", "Laboratórios Basi", "Cinfa", "Bayer").
                       - "corHex": Cor hexadecimal para identificação visual (ex.: "0xFF3B82F6", "0xFFEF4444", "0xFF10B981", "0xFFF59E0B", "0xFF8B5CF6", "0xFF06B6D4").

                    3. CONSULTAS E EXAMES NA ULSBE (Gera entre 1 a 3 agendamentos):
                       - "titulo": Especialidade hospitalar ou dos cuidados de saúde primários (ex.: "Consulta de Medicina Geral e Familiar", "Consulta de Cardiologia de Apoio", "Consulta de Oftalmologia", "Análises Clínicas de Sangue e Urina", "Eletrocardiograma de Controlo", "Consulta de Enfermagem - Vigilância de Saúde").
                       - "localizacao": Estabelecimento real da ULSBE (ex.: "ULSBE · Hospital Santa Maria Maior (Barcelos)", "ULSBE · USF Barcelos Norte", "ULSBE · USF Santo António (Barcelos)", "ULSBE · Centro de Saúde de Esposende", "ULSBE · USF Farol de Esposende").
                       - "dia": Dia com 2 dígitos (ex.: "08", "16", "24").
                       - "mes": Mês abreviado em português (ex.: "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez").
                       - "tipo": "Consulta", "Exame" ou "Enfermagem".
                       - "jejum": true se o procedimento requerer jejum de pelo menos 8 horas (ex.: análises ao sangue), false caso contrário.

                    4. APONTAMENTOS E DÚVIDAS DO UTENTE (Gera 1 a 2 notas reais):
                       - "texto": Dúvidas práticas, sintomas ligeiros ou pedidos que o próprio utente português quer colocar ao seu médico ou enfermeiro de família (ex.: "Perguntar ao médico se posso tomar o protetor gástrico antes do pequeno-almoço", "Avisar que sinto algumas tonturas ao levantar da cama de manhã", "Pedir a renovação do receituário crónico para três meses", "Mostrar o boletim com os registos da tensão medidos na farmácia").
                       - "autor": "Apontamento Pessoal" ou "Eu próprio".

                    5. PRODUTOS COMPLEMENTARES DE FARMÁCIA (Gera 1 a 2 produtos):
                       - Nomes habituais em farmácias portuguesas (ex.: "Vitamina C e Zinco", "Suplemento de Magnésio e Vitamina B6", "Soro Fisiológico em unidoses", "Pensos rápidos estéreis", "Creme emoliente para pernas cansadas").

                    ESTRUTURA JSON ESTRITA:
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

                    IMPORTANTE: Devolve EXCLUSIVAMENTE o JSON puro, sem blocos ```json, sem comentários nem texto antes ou depois.
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
            val prompt = """
                Termo clínico ou dúvida de saúde: "$termo"

                Por favor, explica em português de Portugal (pt-PT):
                - O que significa este termo em linguagem simples e compreensível para o utente.
                - Para que serve ou quais os cuidados práticos habituais no SNS.
            """.trimIndent()
            val resposta = modeloGenerativo.generateContent(prompt)
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
    suspend fun explicarMedicamento(
        nomeMedicamento: String,
        principioAtivo: String = "",
        finalidade: String = "",
    ): Result<String> = withContext(Dispatchers.IO) {
        if (nomeMedicamento.isBlank()) return@withContext Result.failure(IllegalArgumentException("Nome do medicamento vazio."))
        try {
            val detalhesContexto = buildString {
                append("Medicamento: \"$nomeMedicamento\"")
                if (principioAtivo.isNotBlank()) {
                    append("\nPrincípio Ativo (DCI): $principioAtivo")
                }
                if (finalidade.isNotBlank()) {
                    append("\nIndicação clínica registada: $finalidade")
                }
            }
            val prompt = """
                $detalhesContexto

                Explica ao utente sénior ou cuidador em Português de Portugal (pt-PT):
                • Para que serve no dia a dia (em termos claros e simples).
                • Como tomar e cuidados fundamentais (refeições, pequenos-almoços, precauções).
                • Recomendação de segurança e cumprimento da receita médica.
            """.trimIndent()
            val resposta = modeloMedicamentos.generateContent(prompt)
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
            val anoNasc = 2026 - idade
            val diaNasc = String.format("%02d", 1 + random.nextInt(28))
            val mesNasc = String.format("%02d", 1 + random.nextInt(12))
            val dataNascSugerida = "$diaNasc/$mesNasc/$anoNasc"
            val cenario = CENARIOS_CLINICOS[random.nextInt(CENARIOS_CLINICOS.size)]
            val unidade = UNIDADES_ULSBE[random.nextInt(UNIDADES_ULSBE.size)]

            val prompt = """
                Gera um perfil clínico português autêntico para o Serviço Nacional de Saúde (SNS) e a ULSBE:
                - Nome sugerido: $nomeSugerido
                - Género: ${if (eFeminino) "Feminino" else "Masculino"}
                - Idade: $idade anos (Data de Nascimento: $dataNascSugerida)
                - Diagnóstico ou foco de saúde: $cenario
                - Estabelecimento da ULSBE: $unidade
                - Telemóvel do utente: $telemovel
                
                REGRAS OBRIGATÓRIAS (PORTUGUÊS DE PORTUGAL - PT-PT):
                - Todos os nomes, finalidades de medicamentos, consultas e notas devem estar rigorosamente em português de Portugal.
                - Não utilizar termos do português do Brasil (ex.: "medicamento" em vez de "remédio", "pequeno-almoço" em vez de "café da manhã", "tensão arterial" em vez de "pressão arterial").
                - Os medicamentos gerados devem ser autorizados pelo Infarmed e indicados para: $cenario.
                - As notas e apontamentos devem ser 1 a 2 dúvidas ou lembretes redigidos na primeira pessoa pelo próprio utente português para o seu médico de família na consulta.
                - Devolve única e exclusivamente o objeto JSON puro, sem blocos ```json nem texto antes ou depois.
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

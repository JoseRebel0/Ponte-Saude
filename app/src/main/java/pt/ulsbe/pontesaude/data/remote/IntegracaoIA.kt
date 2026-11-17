package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Integração com a Inteligência Artificial para o Tradutor Médico (Firebase AI Logic).
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
}

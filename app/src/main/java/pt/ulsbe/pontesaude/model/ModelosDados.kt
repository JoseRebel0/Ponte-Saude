package pt.ulsbe.pontesaude.model

// ============================================================================
// 1. NAVEGAÇÃO E SISTEMA
// ============================================================================

/**
 * Destinos e rotas principais da barra de navegação inferior da aplicação Ponte Saúde.
 */
enum class SeparadorApp {
    /** Ecrã principal com cartão SNS, resumo de consultas e atalhos rápidos de emergência. */
    INICIO,

    /** Literacia clínica, módulos interativos de segurança, vídeos e catálogo de recompensas. */
    SAUDE,

    /** Agenda de consultas médicas, exames complementares e checklists de preparação. */
    CONSULTAS,

    /** Gestão terapêutica, horários de tomas, fotografias de caixas e bula simplificada por IA. */
    MEDICACAO,

    /** Registo de dúvidas, sintomas e lista de produtos de farmácia/parafarmácia. */
    NOTAS,

    /** Definições de acessibilidade (texto grande, redução de movimento) e modo cuidador. */
    DEFINICOES,
}

// ============================================================================
// 2. PERFIL DE UTENTE E SESSÃO
// ============================================================================

/**
 * Representa o perfil de um utente ou pessoa cuidada no ecossistema da ULSBE.
 * Contém dados clínicos essenciais, identificadores de saúde (SNS, NIF),
 * listas relacionais e associações de cuidadores.
 */
data class Perfil(
    val id: String,
    val nome: String,
    val nomeCurto: String,
    val relacao: String = "Utente",
    val iniciais: String,
    val tipoSanguineo: String = "A+",
    val alergias: String = "Sem alergias conhecidas",
    val consultas: List<Consulta> = emptyList(),
    val medicamentos: List<Medicamento> = emptyList(),
    val outrosProdutos: List<String> = emptyList(),
    val notas: List<Nota> = emptyList(),
    val nif: String = "214 875 920",
    val numeroUtente: String = "189 423 761",
    val cartaoCidadao: String = "13849204 4 ZX2",
    val dataNascimento: String = "14/05/1958",
    val telemovel: String = "+351 912 345 678",
    val email: String = "utente@exemplo.pt",
    val perfisCuidadosIds: List<String> = emptyList(),
    val cuidadoresIds: List<String> = emptyList(),
) {
    /**
     * Idade calculada dinamicamente a partir do ano de nascimento.
     */
    val idadeFormatada: String
        get() {
            val ano = dataNascimento.split("/").lastOrNull()?.toIntOrNull() ?: return ""
            val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val idade = anoAtual - ano
            return if (idade > 0) "$idade anos" else ""
        }
}

// ============================================================================
// 3. MEDICAÇÃO E PLANO TERAPÊUTICO
// ============================================================================

/**
 * Representa um fármaco ou receita prescrita no plano terapêutico do utente.
 *
 * @property id Identificador único numérico do medicamento.
 * @property nome Nome comercial do medicamento (ex: "Ben-u-ron 1000mg").
 * @property finalidade Motivo ou condição tratada (ex: "Alívio da dor e febre").
 * @property principioAtivo Substância ativa farmacológica (DCI) (ex: "Paracetamol").
 * @property hora Horários programados para as tomas diárias (ex: "08:00, 20:00").
 * @property cor Cor identificativa em formato hexadecimal para facilidade de reconhecimento visual.
 * @property ativo Indica se o plano terapêutico está ativo (com alarmes e notificações ligados).
 * @property fotografia Imagem da embalagem ou comprimido em formato Base64 para identificação visual.
 * @property laboratorio Nome do laboratório farmacêutico com autorização em Portugal (Infarmed).
 */
data class Medicamento(
    val id: Long,
    val nome: String,
    val finalidade: String,
    val principioAtivo: String,
    val hora: String,
    val cor: Long,
    val ativo: Boolean,
    val fotografia: String? = null,
    val laboratorio: String = "",
) {
    /**
     * Lista individual de cada horário de toma diária após separação por delimitadores.
     */
    val listaHorarios: List<String>
        get() = hora.split(",", ";", "·").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf(hora.trim().ifBlank { "08:00" }) }

    /**
     * Descrição em linguagem clara da frequência de tomas diárias para apoio a idosos e cuidadores.
     */
    val frequenciaFormatada: String
        get() = when (listaHorarios.size) {
            1 -> "1 toma diária"
            2 -> "2 tomas diárias"
            3 -> "3 tomas diárias"
            4 -> "4 tomas diárias"
            else -> "${listaHorarios.size} tomas diárias"
        }
}

// ============================================================================
// 4. CONSULTAS MÉDICAS E CHECKLISTS DE PREPARAÇÃO
// ============================================================================

/**
 * Representa uma consulta médica, exame de diagnóstico ou tratamento agendado na ULSBE / SNS.
 *
 * @property id Identificador único da consulta.
 * @property dia Dia do agendamento (ex: "18").
 * @property mes Mês abreviado em português (ex: "NOV").
 * @property titulo Especialidade clínica ou designação do exame (ex: "Cardiologia - Consulta de Seguimento").
 * @property localizacao Unidade hospitalar ou Centro de Saúde (ex: "Hospital de Beja - Piso 1, Bloco B").
 * @property tipo Tipo de consulta (ex: "Presencial", "Teleconsulta").
 * @property jejum Indica se o procedimento exige período de jejum prévio pelo utente.
 */
data class Consulta(
    val id: Int,
    val dia: String,
    val mes: String,
    val titulo: String,
    val localizacao: String,
    val tipo: String,
    val jejum: Boolean = false,
)

/**
 * Categorias temáticas para os itens da lista de verificação (Checklist) pré-consulta.
 */
enum class CategoriaChecklist(val rotulo: String, val corFundo: Long, val corTexto: Long) {
    JEJUM("Jejum", 0xFFFFF7ED, 0xFFC2410C),
    DOCUMENTOS("Documentos", 0xFFEFF6FF, 0xFF1D4ED8),
    MEDICACAO("Medicação", 0xFFFAF5FF, 0xFF7E22CE),
    SEGURANCA("Segurança", 0xFFECFDF5, 0xFF047857),
    GERAL("Recomendação", 0xFFF1F5F9, 0xFF475569),
}

/**
 * Tarefa ou recomendação individual integrante da checklist preparatória da consulta.
 */
data class ItemChecklist(
    val id: String,
    val texto: String,
    val categoria: CategoriaChecklist = CategoriaChecklist.GERAL,
    val obrigatorio: Boolean = false,
    val concluido: Boolean = false,
    val personalizado: Boolean = false,
)

// ============================================================================
// 5. APONTAMENTOS E NOTAS CLÍNICAS
// ============================================================================

/**
 * Nota clínica, sintoma registado ou dúvida para esclarecimento na consulta médica.
 *
 * @property texto Conteúdo textual do apontamento registado pelo utente ou cuidador.
 * @property autor Nome ou etiqueta de quem efetuou o registo.
 */
data class Nota(
    val texto: String,
    val autor: String,
)

// ============================================================================
// 6. LITERACIA EM SAÚDE E MÓDULOS EDUCATIVOS
// ============================================================================

/**
 * Categorias temáticas e funcionais dos módulos de literacia em saúde e segurança do utente.
 */
enum class TipoModulo {
    INFO,
    LAVAGEM_MAOS,
    CIRURGIA,
    QUESTIONARIO,
    QUEDAS,
    CRIANCAS,
    CONTACTOS,
    PERGUNTAS,
    MEDICAMENTOS,
    LEITURA,
    RESUMO,
}

/**
 * Módulo educativo interativo de literacia clínica adaptado à população idosa.
 *
 * @property id Identificador único do módulo.
 * @property titulo Título principal claro e compreensível.
 * @property subtitulo Breve síntese do objetivo pedagógico.
 * @property icone Nome do ícone representativo.
 * @property tipo Categoria funcional que determina a apresentação da UI.
 * @property conteudos Lista de parágrafos informativos simplificados.
 * @property fala Texto formatado para leitura automática em voz alta (TTS).
 */
data class ModuloSaude(
    val id: String,
    val titulo: String,
    val subtitulo: String,
    val icone: String,
    val tipo: TipoModulo,
    val conteudos: List<String>,
    val fala: String,
)

/**
 * Vídeo educativo e clínico produzido pela ULSBE / SNS para literacia e segurança do utente.
 *
 * @property id Identificador único do conteúdo multimédia.
 * @property titulo Título temático do vídeo.
 * @property descricao Resumo do tema abordado (ex: preparação para colonoscopia ou gestão de diabetes).
 * @property duracao Duração formatada em minutos e segundos (ex: "4:30 min").
 * @property urlYoutube Ligação externa direta para reprodução na aplicação oficial do YouTube.
 * @property pontos Quantidade de Pontos de Segurança atribuídos após a visualização completa.
 */
data class VideoBibliotecaSaude(
    val id: String,
    val titulo: String,
    val descricao: String,
    val duracao: String,
    val urlYoutube: String,
    val pontos: Int = 30,
)

// ============================================================================
// 7. CLUBE DE SEGURANÇA E RECOMPENSAS
// ============================================================================

/**
 * Benefício ou recompensa institucional resgatável através dos Pontos de Segurança do utente.
 *
 * @property id Identificador único da recompensa no catálogo.
 * @property titulo Nome descritivo da recompensa (ex: "Kit de Higiene Hospitalar ULSBE").
 * @property descricao Detalhes sobre o artigo ou benefício atribuído.
 * @property pontos Custo em pontos de segurança necessários para efetuar o resgate.
 * @property icone Identificador do ícone ilustrativo no catálogo.
 * @property localLevantamento Balcão ou serviço da ULSBE onde o utente pode levantar o artigo.
 */
data class RecompensaSeguranca(
    val id: String,
    val titulo: String,
    val descricao: String,
    val pontos: Int,
    val icone: String,
    val localLevantamento: String,
)

/**
 * Registo de um voucher ativado pelo utente após a troca dos seus pontos acumulados.
 *
 * @property id Identificador único da transação de resgate.
 * @property idRecompensa Identificador da recompensa original do catálogo.
 * @property titulo Nome da recompensa adquirida.
 * @property codigo Código alfanumérico único para validação presencial nos serviços (ex: "SEC-8492").
 * @property dataResgate Data e hora em que o voucher foi emitido.
 * @property localLevantamento Posto de atendimento ou balcão da ULSBE para levantamento.
 * @property pontosUsados Quantidade de pontos debitada do saldo do utente.
 */
data class VoucherResgatado(
    val id: String,
    val idRecompensa: String,
    val titulo: String,
    val codigo: String,
    val dataResgate: String,
    val localLevantamento: String,
    val pontosUsados: Int,
)

package pt.ulsbe.pontesaude.model

/**
 * Categorias interativas dos módulos de literacia e segurança do utente.
 */
enum class TipoModulo {
    INFO, LAVAGEM_MAOS, CIRURGIA, QUESTIONARIO, QUEDAS, CRIANCAS, CONTACTOS, PERGUNTAS, MEDICAMENTOS, LEITURA, RESUMO,
}

/**
 * Módulo de literacia em saúde e segurança do doente.
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
 * Medicamento com plano posológico e horários de toma diária.
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
) {
    val listaHorarios: List<String>
        get() = hora.split(",", ";", "·").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf(hora.trim().ifBlank { "08:00" }) }

    val frequenciaFormatada: String
        get() = when (listaHorarios.size) {
            1 -> "1 toma diária"
            2 -> "2 tomas diárias"
            3 -> "3 tomas diárias"
            4 -> "4 tomas diárias"
            else -> "${listaHorarios.size} tomas diárias"
        }
}

/**
 * Consulta ou exame agendado na ULSBE.
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
 * Nota clínica ou dúvida registada para a consulta.
 */
data class Nota(val texto: String, val autor: String)

/**
 * Perfil do utente com dados clínicos e de identificação.
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
    val idadeFormatada: String
        get() = calcularIdade(dataNascimento)
}

/**
 * Calcula a idade com base na data de nascimento (formato dd/MM/yyyy).
 */
fun calcularIdade(dataNascimento: String): String {
    val partes = dataNascimento.split("/")
    val ano = partes.lastOrNull()?.toIntOrNull() ?: return ""
    val idade = 2026 - ano
    return if (idade > 0) "$idade anos" else ""
}

/**
 * Separadores da barra de navegação principal da aplicação.
 */
enum class SeparadorApp {
    INICIO, SAUDE, CONSULTAS, MEDICACAO, NOTAS, DEFINICOES
}

/**
 * Recompensa resgatável através dos pontos acumulados de literacia.
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
 * Registo de voucher resgatado pelo utente.
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

/**
 * Categorias dos itens de verificação da checklist de consulta.
 */
enum class CategoriaChecklist(val rotulo: String, val corFundo: Long, val corTexto: Long) {
    JEJUM("Jejum", 0xFFFFF7ED, 0xFFC2410C),
    DOCUMENTOS("Documentos", 0xFFEFF6FF, 0xFF1D4ED8),
    MEDICACAO("Medicação", 0xFFFAF5FF, 0xFF7E22CE),
    SEGURANCA("Segurança", 0xFFECFDF5, 0xFF047857),
    GERAL("Recomendação", 0xFFF1F5F9, 0xFF475569),
}

/**
 * Item individual da checklist de preparação de consulta ou procedimento.
 */
data class ItemChecklist(
    val id: String,
    val texto: String,
    val categoria: CategoriaChecklist = CategoriaChecklist.GERAL,
    val obrigatorio: Boolean = false,
    val concluido: Boolean = false,
    val personalizado: Boolean = false,
)

/**
 * Vídeo educativo da biblioteca de literacia em saúde.
 */
data class VideoBibliotecaSaude(
    val id: String,
    val titulo: String,
    val descricao: String,
    val duracao: String,
    val urlYoutube: String,
    val pontos: Int = 30,
)

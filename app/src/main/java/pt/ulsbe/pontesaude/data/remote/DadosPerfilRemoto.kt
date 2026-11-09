package pt.ulsbe.pontesaude.data.remote

import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.VoucherResgatado

/**
 * Estrutura de transferência de dados (DTO) que agrega os dados de um perfil remoto
 * e o seu progresso no Clube de Segurança e Literacia.
 */
data class DadosPerfilRemoto(
    val perfil: Perfil,
    val pontosSeguranca: Int,
    val modulosConcluidos: List<String>,
    val modulosOuvidos: List<String>,
    val videosAssistidos: List<String>,
    val vouchersResgatados: List<VoucherResgatado>,
    val checklistsConsultas: Map<Int, Pair<List<String>, List<String>>>,
)

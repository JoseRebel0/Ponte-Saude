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
    val donoUid: String = "",
)

/**
 * DTOs fortemente tipados para a API remota Cloud Firestore.
 * Garantem segurança de tipos (Type-Safety) e validação em tempo de compilação.
 */

data class PerfilDtoRemoto(
    val id: String = "",
    val donoUid: String = "",
    val nome: String = "",
    val nomeCurto: String = "",
    val relacao: String = "",
    val iniciais: String = "",
    val tipoSanguineo: String = "",
    val alergias: String = "",
    val nif: String = "",
    val numeroUtente: String = "",
    val cartaoCidadao: String = "",
    val dataNascimento: String = "",
    val telemovel: String = "",
    val email: String = "",
    val perfisCuidadosIds: List<String> = emptyList(),
    val cuidadoresIds: List<String> = emptyList(),
    val outrosProdutos: List<String> = emptyList(),
    val pontosSeguranca: Int = 0,
    val modulosConcluidos: List<String> = emptyList(),
    val modulosOuvidos: List<String> = emptyList(),
    val videosAssistidos: List<String> = emptyList(),
    val vouchersResgatados: List<VoucherResgatadoDtoRemoto> = emptyList(),
)

data class MedicamentoDtoRemoto(
    val id: Long = 0L,
    val nome: String = "",
    val finalidade: String = "",
    val principioAtivo: String = "",
    val hora: String = "",
    val cor: Long = 0L,
    val ativo: Boolean = true,
    val fotografia: String = "",
    val laboratorio: String = "",
)

data class ConsultaDtoRemoto(
    val id: Int = 0,
    val dia: String = "",
    val mes: String = "",
    val titulo: String = "",
    val localizacao: String = "",
    val tipo: String = "",
    val jejum: Boolean = false,
    val checklistMarcados: List<String> = emptyList(),
    val checklistCustom: List<String> = emptyList(),
)

data class NotaDtoRemoto(
    val texto: String = "",
    val autor: String = "",
)

data class VoucherResgatadoDtoRemoto(
    val id: String = "",
    val idRecompensa: String = "",
    val titulo: String = "",
    val codigo: String = "",
    val dataResgate: String = "",
    val localLevantamento: String = "",
    val pontosUsados: Int = 0,
)

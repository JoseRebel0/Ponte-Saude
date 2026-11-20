package pt.ulsbe.pontesaude.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.ModuloSaude
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.RecompensaSeguranca
import pt.ulsbe.pontesaude.model.VideoBibliotecaSaude
import pt.ulsbe.pontesaude.model.VoucherResgatado

/**
 * Cliente de serviço unificado para comunicação com a API remota Google Cloud Firestore.
 *
 * Funciona segundo o padrão Facade / Coordinator:
 * - Agrega os módulos de domínio especializados ([ApiPerfisRemoto], [ApiMedicacaoRemoto],
 *   [ApiConsultasRemoto], [ApiNotasRemoto], [ApiSaudeRemoto]).
 * - Expõe as operações para o repositório central com delegação transparente e tipada.
 */
class ClienteFirestore(
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    // Módulos remotos por domínio
    val perfis = ApiPerfisRemoto(db)
    val medicacao = ApiMedicacaoRemoto(db)
    val consultas = ApiConsultasRemoto(db)
    val notas = ApiNotasRemoto(db)
    val saude = ApiSaudeRemoto(db)

    // --- Módulo: Literacia e Saúde ---

    suspend fun obterModulosSaude(): List<ModuloSaude> = saude.obterModulosSaude()

    suspend fun obterVideosBiblioteca(): List<VideoBibliotecaSaude> = saude.obterVideosBiblioteca()

    suspend fun obterCatalogoRecompensas(): List<RecompensaSeguranca> = saude.obterCatalogoRecompensas()

    suspend fun atualizarDadosRecompensas(
        idPerfil: String,
        pontos: Int,
        modulosConcluidos: List<String>? = null,
        modulosOuvidos: List<String>? = null,
        videosAssistidos: List<String>? = null,
        vouchers: List<VoucherResgatado>? = null,
    ) = saude.atualizarDadosRecompensas(idPerfil, pontos, modulosConcluidos, modulosOuvidos, videosAssistidos, vouchers)

    // --- Módulo: Perfis e Cuidadores ---

    suspend fun salvarPerfilCompleto(
        perfil: Perfil,
        pontos: Int = 50,
        modulosConcluidos: List<String> = emptyList(),
        modulosOuvidos: List<String> = emptyList(),
        videosAssistidos: List<String> = emptyList(),
        vouchers: List<VoucherResgatado> = emptyList(),
        obterChecklistConsulta: ((Int) -> Pair<List<String>, List<String>>)? = null,
    ) = perfis.salvarPerfilCompleto(
        perfil,
        pontos,
        modulosConcluidos,
        modulosOuvidos,
        videosAssistidos,
        vouchers,
        obterChecklistConsulta,
    )

    suspend fun carregarTodosOsPerfis(): Result<List<DadosPerfilRemoto>> = perfis.carregarTodosOsPerfis()

    suspend fun associarPerfilCuidado(idCuidador: String, idCuidado: String) =
        perfis.associarPerfilCuidado(idCuidador, idCuidado)

    suspend fun desassociarPerfilCuidado(idCuidador: String, idCuidado: String) =
        perfis.desassociarPerfilCuidado(idCuidador, idCuidado)

    suspend fun apagarPerfil(idPerfil: String) = perfis.apagarPerfil(idPerfil)

    suspend fun apagarPerfisEmMassa(ids: List<String>) {
        for (id in ids) {
            perfis.apagarPerfil(id)
        }
    }

    fun iniciarEscutaPerfisEmTempoReal(aoAlterar: () -> Unit) = perfis.iniciarEscutaPerfisEmTempoReal(aoAlterar)

    // --- Módulo: Medicação ---

    suspend fun guardarMedicamento(idPerfil: String, med: Medicamento) =
        medicacao.guardarMedicamento(idPerfil, med)

    suspend fun apagarMedicamento(idPerfil: String, idMedicamento: Long) =
        medicacao.apagarMedicamento(idPerfil, idMedicamento)

    // --- Módulo: Consultas Médicas ---

    suspend fun guardarConsulta(idPerfil: String, consulta: Consulta) =
        consultas.guardarConsulta(idPerfil, consulta)

    suspend fun atualizarConsultas(idPerfil: String, consultas: List<Consulta>) =
        this.consultas.atualizarConsultas(idPerfil, consultas)

    suspend fun atualizarChecklistConsulta(
        idPerfil: String,
        idConsulta: Int,
        marcados: List<String>,
    ) = consultas.atualizarChecklistConsulta(idPerfil, idConsulta, marcados)

    suspend fun atualizarChecklistCustomConsulta(
        idPerfil: String,
        idConsulta: Int,
        custom: List<String>,
    ) = consultas.atualizarChecklistCustomConsulta(idPerfil, idConsulta, custom)

    // --- Módulo: Apontamentos e Produtos ---

    suspend fun atualizarNotas(idPerfil: String, notas: List<Nota>) =
        this.notas.atualizarNotas(idPerfil, notas)

    suspend fun atualizarProdutos(idPerfil: String, produtos: List<String>) =
        this.notas.atualizarProdutos(idPerfil, produtos)
}


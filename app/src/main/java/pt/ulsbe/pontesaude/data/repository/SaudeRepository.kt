package pt.ulsbe.pontesaude.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pt.ulsbe.pontesaude.data.local.BaseDadosLocal
import pt.ulsbe.pontesaude.data.local.deserializarVouchers
import pt.ulsbe.pontesaude.data.local.paraModelo
import pt.ulsbe.pontesaude.data.local.serializarVouchers
import pt.ulsbe.pontesaude.data.remote.ClienteFirestore
import pt.ulsbe.pontesaude.data.remote.IntegracaoIA
import pt.ulsbe.pontesaude.model.ModuloSaude
import pt.ulsbe.pontesaude.model.RecompensaSeguranca
import pt.ulsbe.pontesaude.model.VideoBibliotecaSaude
import pt.ulsbe.pontesaude.model.VoucherResgatado
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Repositório de Saúde, Literacia Clínica, Vídeos e Clube de Segurança.
 *
 * Gere a gamificação por pontos, módulos educativos, resgate de vouchers institucionais
 * e simplificação de linguagem clínica via Gemini AI.
 */
class SaudeRepository(
    private val context: Context,
    private val dbLocal: BaseDadosLocal,
    private val clienteFirestore: ClienteFirestore,
    private val integracaoIa: IntegracaoIA = IntegracaoIA(),
    private val scope: CoroutineScope,
    private val estaOnline: () -> Boolean,
) {
    private val preferencias: SharedPreferences =
        context.getSharedPreferences("ponte_saude", Context.MODE_PRIVATE)

    var pontosSeguranca by mutableIntStateOf(preferencias.getInt("pontos_seguranca", 50))
        private set

    val modulosConcluidos = mutableStateListOf<String>()
    val modulosOuvidos = mutableStateListOf<String>()
    val videosAssistidos = mutableStateListOf<String>()
    val vouchersResgatados = mutableStateListOf<VoucherResgatado>()

    val modulosSaude = mutableStateListOf<ModuloSaude>()
    val videosBiblioteca = mutableStateListOf<VideoBibliotecaSaude>()
    val catalogoRecompensas = mutableStateListOf<RecompensaSeguranca>()

    init {
        modulosConcluidos.addAll(preferencias.getStringSet("completed_modules", emptySet()).orEmpty())
        modulosOuvidos.addAll(preferencias.getStringSet("listened_modules", emptySet()).orEmpty())
        videosAssistidos.addAll(preferencias.getStringSet("watched_videos", emptySet()).orEmpty())
        vouchersResgatados.addAll(lerVouchers())
        carregarCatalogoLocal()
    }

    private fun chavePerfil(chaveBase: String, idPerfil: String = ""): String =
        if (idPerfil.isBlank()) chaveBase else "${chaveBase}_$idPerfil"

    fun carregarCatalogoLocal() {
        val mods = dbLocal.moduloDao().obterTodos().map { it.paraModelo() }
        val vids = dbLocal.videoDao().obterTodos().map { it.paraModelo() }
        val recomps = dbLocal.recompensaDao().obterTodas().map { it.paraModelo() }

        if (mods.isNotEmpty()) {
            modulosSaude.clear()
            modulosSaude.addAll(mods)
        }
        if (vids.isNotEmpty()) {
            videosBiblioteca.clear()
            videosBiblioteca.addAll(vids)
        }
        if (recomps.isNotEmpty()) {
            catalogoRecompensas.clear()
            catalogoRecompensas.addAll(recomps)
        }
    }

    fun alternarModulo(id: String, idPerfil: String = "") {
        if (id in modulosConcluidos) modulosConcluidos.remove(id) else modulosConcluidos.add(id)
        preferencias.edit().putStringSet(chavePerfil("completed_modules", idPerfil), modulosConcluidos.toSet()).apply()
        sincronizarRecompensasFirestore(idPerfil)
    }

    fun alternarModuloComPontos(id: String, idPerfil: String = ""): Int {
        val estavaConcluido = id in modulosConcluidos
        alternarModulo(id, idPerfil)
        return if (!estavaConcluido) {
            adicionarPontos(20, idPerfil)
            20
        } else {
            adicionarPontos(-20, idPerfil)
            -20
        }
    }

    fun premiarOuvirModulo(idModulo: String, idPerfil: String = ""): Int {
        if (idModulo !in modulosOuvidos) {
            modulosOuvidos.add(idModulo)
            preferencias.edit().putStringSet(chavePerfil("listened_modules", idPerfil), modulosOuvidos.toSet()).apply()
            adicionarPontos(15, idPerfil)
            sincronizarRecompensasFirestore(idPerfil)
            return 15
        }
        return 0
    }

    fun marcarModuloOuvido(id: String, idPerfil: String = ""): Int =
        premiarOuvirModulo(id, idPerfil)

    fun premiarVerVideo(idVideo: String, idPerfil: String = ""): Int {
        if (idVideo !in videosAssistidos) {
            videosAssistidos.add(idVideo)
            preferencias.edit().putStringSet(chavePerfil("watched_videos", idPerfil), videosAssistidos.toSet()).apply()
            adicionarPontos(25, idPerfil)
            sincronizarRecompensasFirestore(idPerfil)
            return 25
        }
        return 0
    }

    fun marcarVideoAssistido(id: String, idPerfil: String = ""): Int =
        premiarVerVideo(id, idPerfil)

    fun adicionarPontos(quantidade: Int, idPerfil: String = "") {
        pontosSeguranca = (pontosSeguranca + quantidade).coerceAtLeast(0)
        preferencias.edit().putInt(chavePerfil("pontos_seguranca", idPerfil), pontosSeguranca).apply()
    }

    fun resgatarRecompensa(recompensa: RecompensaSeguranca, idPerfil: String = ""): VoucherResgatado? {
        if (pontosSeguranca < recompensa.pontos) return null

        pontosSeguranca -= recompensa.pontos
        preferencias.edit().putInt(chavePerfil("pontos_seguranca", idPerfil), pontosSeguranca).apply()

        val codigo = "SEC-${Random.nextInt(1000, 9999)}"
        val dataFormatada = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "PT")).format(Date())

        val novoVoucher = VoucherResgatado(
            id = "v_${System.currentTimeMillis()}",
            idRecompensa = recompensa.id,
            titulo = recompensa.titulo,
            codigo = codigo,
            dataResgate = dataFormatada,
            localLevantamento = recompensa.localLevantamento,
            pontosUsados = recompensa.pontos,
        )

        vouchersResgatados.add(0, novoVoucher)
        guardarVouchers(vouchersResgatados, idPerfil)
        sincronizarRecompensasFirestore(idPerfil)
        return novoVoucher
    }

    fun lerVouchers(idPerfil: String = ""): List<VoucherResgatado> = runCatching {
        val bruto = preferencias.getString(chavePerfil("vouchers_resgatados", idPerfil), null) ?: return emptyList()
        bruto.deserializarVouchers()
    }.getOrDefault(emptyList())

    private fun guardarVouchers(vouchers: List<VoucherResgatado>, idPerfil: String = "") {
        preferencias.edit()
            .putString(chavePerfil("vouchers_resgatados", idPerfil), vouchers.serializarVouchers())
            .apply()
    }

    private fun sincronizarRecompensasFirestore(idPerfil: String) {
        if (estaOnline() && idPerfil.isNotBlank()) {
            scope.launch {
                runCatching {
                    clienteFirestore.atualizarDadosRecompensas(
                        idPerfil = idPerfil,
                        pontos = pontosSeguranca,
                        modulosConcluidos = modulosConcluidos.toList(),
                        modulosOuvidos = modulosOuvidos.toList(),
                        videosAssistidos = videosAssistidos.toList(),
                        vouchers = vouchersResgatados.toList(),
                    )
                }
            }
        }
    }

    suspend fun explicarTermoMedicoComIa(termo: String): Result<String> =
        integracaoIa.gerarExplicacao(termo)
}

/**
 * Representa os patamares de progressão e literacia no Clube de Segurança do Utente.
 */
data class InfoNivelSeguranca(
    val nivel: Int,
    val titulo: String,
    val pontosMinimos: Int,
    val pontosProximoNivel: Int,
    val progresso: Float,
)

/**
 * Calcula o nível atingido pelo utente com base no seu saldo total de pontos acumulados.
 */
fun calcularNivelSeguranca(pontos: Int): InfoNivelSeguranca {
    return when {
        pontos < 100 -> InfoNivelSeguranca(1, "Utente Informado", 0, 100, (pontos / 100f).coerceIn(0f, 1f))
        pontos < 250 -> InfoNivelSeguranca(2, "Praticante de Prevenção Ativa", 100, 250, ((pontos - 100) / 150f).coerceIn(0f, 1f))
        pontos < 500 -> InfoNivelSeguranca(3, "Consultor de Segurança Clínica", 250, 500, ((pontos - 250) / 250f).coerceIn(0f, 1f))
        else -> InfoNivelSeguranca(4, "Embaixador de Literacia em Saúde ULSBE", 500, 500, 1.0f)
    }
}

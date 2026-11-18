package pt.ulsbe.pontesaude.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Repositório responsável pela persistência e gestão das preferências de acessibilidade
 * e configurações de sistema da aplicação Ponte Saúde.
 */
class ConfiguracaoRepository(context: Context) {
    private val preferencias: SharedPreferences =
        context.getSharedPreferences("ponte_saude", Context.MODE_PRIVATE)

    var textoGrande by mutableStateOf(preferencias.getBoolean("big_text", false))
        private set

    var reduzirMovimento by mutableStateOf(preferencias.getBoolean("reduce_motion", false))
        private set

    var altoContraste by mutableStateOf(preferencias.getBoolean("alto_contraste", false))
        private set

    var somVozAtivo by mutableStateOf(preferencias.getBoolean("som_voz_ativo", true))
        private set

    var notificacoesAtivas by mutableStateOf(preferencias.getBoolean("notificacoes_ativas", true))
        private set

    var agendaEAlarmesAtivos by mutableStateOf(preferencias.getBoolean("agenda_alarmes_ativos", true))
        private set

    var timestampUltimaAtualizacao by mutableStateOf(
        preferencias.getLong("ts_ultima_atualizacao", System.currentTimeMillis()),
    )
        private set

    fun atualizarTextoGrande(valor: Boolean) {
        textoGrande = valor
        preferencias.edit().putBoolean("big_text", valor).apply()
    }

    fun atualizarReduzirMovimento(valor: Boolean) {
        reduzirMovimento = valor
        preferencias.edit().putBoolean("reduce_motion", valor).apply()
    }

    fun atualizarAltoContraste(valor: Boolean) {
        altoContraste = valor
        preferencias.edit().putBoolean("alto_contraste", valor).apply()
    }

    fun atualizarSomVoz(valor: Boolean) {
        somVozAtivo = valor
        preferencias.edit().putBoolean("som_voz_ativo", valor).apply()
    }

    fun atualizarNotificacoesAtivas(valor: Boolean) {
        notificacoesAtivas = valor
        preferencias.edit().putBoolean("notificacoes_ativas", valor).apply()
    }

    fun atualizarAgendaEAlarmesAtivos(valor: Boolean) {
        agendaEAlarmesAtivos = valor
        preferencias.edit().putBoolean("agenda_alarmes_ativos", valor).apply()
    }

    fun registarAtualizacao() {
        timestampUltimaAtualizacao = System.currentTimeMillis()
        preferencias.edit().putLong("ts_ultima_atualizacao", timestampUltimaAtualizacao).apply()
    }

    fun formatarUltimaAtualizacao(): String =
        formatarMomentoAtualizacao(timestampUltimaAtualizacao)
}

/**
 * Converte um timestamp em milissegundos para uma descrição legível em português de Portugal:
 * "Há instantes", "Hoje às 14:30", "Ontem às 10:15", ou "12/04 às 09:00".
 */
fun formatarMomentoAtualizacao(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "Hoje às 10:00"
    val agora = System.currentTimeMillis()
    val diferencaSegundos = (agora - timestampMillis) / 1000
    if (diferencaSegundos < 60) {
        return "Há instantes"
    }

    val calAtual = Calendar.getInstance()
    val calRegisto = Calendar.getInstance().apply { timeInMillis = timestampMillis }

    val horaMinuto = SimpleDateFormat("HH:mm", Locale("pt", "PT")).format(Date(timestampMillis))

    val mesmoAno = calAtual.get(Calendar.YEAR) == calRegisto.get(Calendar.YEAR)
    val mesmoDia = mesmoAno && calAtual.get(Calendar.DAY_OF_YEAR) == calRegisto.get(Calendar.DAY_OF_YEAR)
    val ontem = mesmoAno && (calAtual.get(Calendar.DAY_OF_YEAR) - calRegisto.get(Calendar.DAY_OF_YEAR) == 1)

    return when {
        mesmoDia -> "Hoje às $horaMinuto"
        ontem -> "Ontem às $horaMinuto"
        else -> {
            val formatoData = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "PT"))
            formatoData.format(Date(timestampMillis))
        }
    }
}

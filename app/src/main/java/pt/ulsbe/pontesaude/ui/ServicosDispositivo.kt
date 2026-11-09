package pt.ulsbe.pontesaude.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pt.ulsbe.pontesaude.MainActivity
import pt.ulsbe.pontesaude.R
import pt.ulsbe.pontesaude.data.RepositorioApp
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import java.util.Calendar
import java.util.Locale

/**
 * Serviços de integração com o dispositivo Android:
 * - Voz/TTS (TextToSpeech)
 * - Chamadas telefónicas e Partilha
 * - Notificações Locais (Canais de Medicação, Segurança/Pontos e Consultas)
 * - Alarmes do Relógio e AlarmManager (Agendamento exato e cancelamento imediato)
 * - Agenda e Calendário do Sistema (CalendarContract)
 */
class ServicosDispositivo(context: Context?) {
    private val contextoApp = context?.applicationContext
    private var pronto = false
    private var falaPendente: String? = null
    private var sintetizadorVoz: TextToSpeech? = null

    companion object {
        const val CANAL_MEDICACAO = "canal_medicacao"
        const val CANAL_SEGURANCA = "canal_seguranca"
        const val CANAL_CONSULTAS = "canal_consultas"
    }

    private val gestorNotificacoes: NotificationManager? =
        contextoApp?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    init {
        criarCanaisNotificacoes()
        if (contextoApp != null) {
            runCatching {
                sintetizadorVoz = TextToSpeech(contextoApp) { estado ->
                    pronto = estado == TextToSpeech.SUCCESS
                    if (pronto) {
                        val ptPt = Locale.forLanguageTag("pt-PT")
                        val resultado = sintetizadorVoz?.setLanguage(ptPt)
                        if (resultado == TextToSpeech.LANG_MISSING_DATA || resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
                            val ptFallback = Locale("pt", "PT")
                            val resFallback = sintetizadorVoz?.setLanguage(ptFallback)
                            if (resFallback == TextToSpeech.LANG_MISSING_DATA || resFallback == TextToSpeech.LANG_NOT_SUPPORTED) {
                                sintetizadorVoz?.setLanguage(Locale("pt"))
                            }
                        }
                        sintetizadorVoz?.setSpeechRate(0.9f)
                        falaPendente?.let { fala ->
                            falar(fala)
                            falaPendente = null
                        }
                    }
                }
            }
        }
    }

    private fun criarCanaisNotificacoes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && gestorNotificacoes != null) {
            val canalMed = NotificationChannel(
                CANAL_MEDICACAO,
                "Lembretes de Medicação",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Avisos na hora exata de tomar medicamentos"
                enableVibration(true)
                setShowBadge(true)
            }
            val canalSeg = NotificationChannel(
                CANAL_SEGURANCA,
                "Segurança do Doente & Pontos",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Lembretes para ouvir módulos e resgatar recompensas"
                enableVibration(true)
                setShowBadge(true)
            }
            val canalCons = NotificationChannel(
                CANAL_CONSULTAS,
                "Lembretes de Consultas & Exames",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Lembretes com detalhes e preparação para consultas"
                enableVibration(true)
                setShowBadge(true)
            }
            gestorNotificacoes.createNotificationChannels(listOf(canalMed, canalSeg, canalCons))
        }
    }

    fun temPermissaoNotificacoes(): Boolean {
        val ctx = contextoApp ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        }
    }

    fun enviarNotificacaoMedicacao(id: Int = 1001, nome: String, dose: String, hora: String): Boolean {
        val ctx = contextoApp ?: return false
        return runCatching {
            val notificacao = NotificationCompat.Builder(ctx, CANAL_MEDICACAO)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("Hora da Medicação: $nome")
                .setContentText("Está na hora de tomar $dose (às $hora). Toque para confirmar.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build()
            gestorNotificacoes?.notify(id, notificacao)
            true
        }.getOrElse {
            // Fallback caso haja restrição no ícone customizado
            runCatching {
                val notificacao = NotificationCompat.Builder(ctx, CANAL_MEDICACAO)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Hora da Medicação: $nome")
                    .setContentText("Está na hora de tomar $dose (às $hora).")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
                gestorNotificacoes?.notify(id, notificacao)
                true
            }.getOrDefault(false)
        }
    }

    fun enviarNotificacaoSeguranca(titulo: String, mensagem: String): Boolean {
        val ctx = contextoApp ?: return false
        return runCatching {
            val notificacao = NotificationCompat.Builder(ctx, CANAL_SEGURANCA)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setStyle(NotificationCompat.BigTextStyle().bigText(mensagem))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            gestorNotificacoes?.notify(2001, notificacao)
            true
        }.getOrElse {
            runCatching {
                val notificacao = NotificationCompat.Builder(ctx, CANAL_SEGURANCA)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(titulo)
                    .setContentText(mensagem)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(mensagem))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
                gestorNotificacoes?.notify(2001, notificacao)
                true
            }.getOrDefault(false)
        }
    }

    fun enviarNotificacaoConsulta(titulo: String, data: String, local: String): Boolean {
        val ctx = contextoApp ?: return false
        return runCatching {
            val notificacao = NotificationCompat.Builder(ctx, CANAL_CONSULTAS)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("Lembrete de Consulta: $titulo")
                .setContentText("$data no $local. Verifique as recomendações de jejum e medicação.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("$data no $local. Verifique as recomendações de jejum e medicação."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            gestorNotificacoes?.notify(3001, notificacao)
            true
        }.getOrElse {
            runCatching {
                val notificacao = NotificationCompat.Builder(ctx, CANAL_CONSULTAS)
                    .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                    .setContentTitle("Lembrete de Consulta: $titulo")
                    .setContentText("$data no $local.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
                gestorNotificacoes?.notify(3001, notificacao)
                true
            }.getOrDefault(false)
        }
    }

    fun enviarNotificacaoChecklist(tituloConsulta: String, itensChecklist: String): Boolean {
        val ctx = contextoApp ?: return false
        return runCatching {
            val notificacao = NotificationCompat.Builder(ctx, CANAL_CONSULTAS)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("Lembrete de Checklist: $tituloConsulta")
                .setContentText("Não se esqueça de verificar a sua checklist: $itensChecklist.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Não se esqueça de conferir a sua checklist antes de sair:\n• $itensChecklist"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            gestorNotificacoes?.notify(3002, notificacao)
            true
        }.getOrElse {
            runCatching {
                val notificacao = NotificationCompat.Builder(ctx, CANAL_CONSULTAS)
                    .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                    .setContentTitle("Lembrete de Checklist: $tituloConsulta")
                    .setContentText("Não se esqueça de conferir a sua checklist: $itensChecklist.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
                gestorNotificacoes?.notify(3002, notificacao)
                true
            }.getOrDefault(false)
        }
    }

    /**
     * Agenda um alarme interno através do AlarmManager para a hora exata da toma.
     */
    fun agendarAlarmeMedicacao(id: Long, nome: String, dose: String, hora: Int, minuto: Int) {
        val ctx = contextoApp ?: return
        runCatching {
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(ctx, AlarmeMedicacaoReceiver::class.java).apply {
                action = "pt.ulsbe.pontesaude.ALARME_MEDICACAO_${id}"
                putExtra("id", id)
                putExtra("nome", nome)
                putExtra("dose", dose)
                putExtra("hora", String.format(Locale.getDefault(), "%02d:%02d", hora, minuto))
            }
            val pendingIntent = PendingIntent.getBroadcast(
                ctx,
                id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val agora = Calendar.getInstance()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hora)
                set(Calendar.MINUTE, minuto)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(agora)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val showIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val showPendingIntent = PendingIntent.getActivity(
                ctx,
                id.toInt(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            // Prioriza setAlarmClock para que o sistema Android ative o ícone de despertador
            // na barra de estado e mostre o alarme no ecrã de bloqueio
            runCatching {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(cal.timeInMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            }.onFailure {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        pendingIntent,
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        pendingIntent,
                    )
                }
            }
        }
    }

    /**
     * Cancela o alarme no AlarmManager, descarta a notificação e remove do relógio nativo se aplicável.
     */
    fun cancelarAlarmeMedicacao(id: Long, rotuloRelogio: String? = null) {
        val ctx = contextoApp ?: return
        runCatching {
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(ctx, AlarmeMedicacaoReceiver::class.java).apply {
                action = "pt.ulsbe.pontesaude.ALARME_MEDICACAO_${id}"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                ctx,
                id.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            gestorNotificacoes?.cancel(id.toInt())
        }
    }

    /**
     * Cancela todos os alarmes programados para a lista de medicamentos.
     */
    fun cancelarTodosOsAlarmes(medicamentos: List<Medicamento>) {
        medicamentos.forEach { med ->
            cancelarAlarmeMedicacao(med.id)
        }
        cancelarTodasNotificacoes()
    }

    /**
     * Cancela todas as notificações ativas da aplicação.
     */
    fun cancelarTodasNotificacoes() {
        runCatching {
            gestorNotificacoes?.cancelAll()
        }
    }

    /**
     * Tenta dispensar/desativar todos os alarmes no relógio nativo do sistema Android.
     */
    fun tentarDesativarTodosAlarmesRelogio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_ALL)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                contextoApp?.startActivity(intent)
            }
        }
    }

    /**
     * Tenta dispensar/desativar um alarme no relógio nativo do sistema Android.
     */
    fun tentarDesativarAlarmeRelogio(mensagem: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
                    putExtra(AlarmClock.EXTRA_MESSAGE, mensagem)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                contextoApp?.startActivity(intent)
            }
        }
    }

    /**
     * Abre o relógio nativo do telemóvel para consulta ou ajuste manual de alarmes.
     */
    fun abrirRelogioAlarmes() {
        runCatching {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            contextoApp?.startActivity(intent)
        }
    }

    fun definirAlarmeRelogio(hora: Int, minuto: Int, mensagem: String): Boolean {
        val ctx = contextoApp ?: return false
        return runCatching {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hora)
                putExtra(AlarmClock.EXTRA_MINUTES, minuto)
                putExtra(AlarmClock.EXTRA_MESSAGE, mensagem)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                putExtra(AlarmClock.EXTRA_VIBRATE, true)
                putExtra(
                    AlarmClock.EXTRA_DAYS,
                    arrayListOf(
                        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
                    ),
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)

            // Caso o relógio do fabricante ignore o EXTRA_SKIP_UI e tente mostrar o ecrã,
            // restaura a app PonteSaúde imediatamente para primeiro plano sem incomodar o utilizador
            val voltaAppIntent = Intent(ctx, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching { ctx.startActivity(voltaAppIntent) }
            }, 300)

            true
        }.getOrDefault(false)
    }

    fun sincronizarConsultaNoCalendario(
        titulo: String,
        descricao: String,
        localizacao: String,
        dia: String,
        mes: String,
    ): Boolean {
        val ctx = contextoApp ?: return false
        return runCatching {
            val temPermissao = ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
            if (!temPermissao) {
                return false
            }

            val uriCalendarios = CalendarContract.Calendars.CONTENT_URI
            val projecao = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
            var idCalendario: Long = 1
            ctx.contentResolver.query(uriCalendarios, projecao, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idxId = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    val idxPrimary = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                    while (!cursor.isAfterLast) {
                        val id = cursor.getLong(idxId)
                        val primary = if (idxPrimary >= 0) cursor.getInt(idxPrimary) == 1 else false
                        if (primary) {
                            idCalendario = id
                            break
                        }
                        idCalendario = id
                        cursor.moveToNext()
                    }
                }
            }

            val diaInt = dia.toIntOrNull() ?: 15
            val mesMap = mapOf(
                "JAN" to 0, "FEV" to 1, "MAR" to 2, "ABR" to 3, "MAI" to 4, "JUN" to 5,
                "JUL" to 6, "AGO" to 7, "SET" to 8, "OUT" to 9, "NOV" to 10, "DEZ" to 11,
            )
            val mesInt = mesMap[mes.uppercase().trim()] ?: Calendar.getInstance().get(Calendar.MONTH)
            val anoAtual = Calendar.getInstance().get(Calendar.YEAR)
            val calInicio = Calendar.getInstance().apply {
                set(Calendar.YEAR, anoAtual)
                set(Calendar.MONTH, mesInt)
                set(Calendar.DAY_OF_MONTH, diaInt)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calFim = Calendar.getInstance().apply {
                timeInMillis = calInicio.timeInMillis + 60 * 60 * 1000
            }

            val uriEventos = CalendarContract.Events.CONTENT_URI
            val tituloCompleto = "$titulo · ULSBE"

            var jaExiste = false
            ctx.contentResolver.query(
                uriEventos,
                arrayOf(CalendarContract.Events._ID),
                "${CalendarContract.Events.TITLE} = ? AND ${CalendarContract.Events.DTSTART} = ?",
                arrayOf(tituloCompleto, calInicio.timeInMillis.toString()),
                null,
            )?.use { cursor ->
                if (cursor.count > 0) jaExiste = true
            }

            if (!jaExiste) {
                val valores = android.content.ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, idCalendario)
                    put(CalendarContract.Events.TITLE, tituloCompleto)
                    put(CalendarContract.Events.DESCRIPTION, descricao)
                    put(CalendarContract.Events.EVENT_LOCATION, localizacao)
                    put(CalendarContract.Events.DTSTART, calInicio.timeInMillis)
                    put(CalendarContract.Events.DTEND, calFim.timeInMillis)
                    put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
                    put(CalendarContract.Events.HAS_ALARM, 1)
                }
                val uriInserido = ctx.contentResolver.insert(uriEventos, valores)
                uriInserido?.lastPathSegment?.toLongOrNull()?.let { eventoId ->
                    val valoresLembrete = android.content.ContentValues().apply {
                        put(CalendarContract.Reminders.EVENT_ID, eventoId)
                        put(CalendarContract.Reminders.MINUTES, 24 * 60)
                        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    }
                    ctx.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, valoresLembrete)
                }
            }
            true
        }.getOrElse { false }
    }

    fun sincronizarConsultasNoCalendario(consultas: List<Consulta>): Boolean {
        var todosSucessos = true
        consultas.forEach { consulta ->
            val checklist = if (consulta.jejum) {
                listOf("Jejum de 8 horas", "Levar cartão de cidadão", "Levar lista de medicação")
            } else {
                listOf("Levar cartão de cidadão", "Levar exames anteriores", "Escrever dúvidas para o médico")
            }
            val res = sincronizarConsultaNoCalendario(
                titulo = consulta.titulo,
                descricao = "Consulta no Serviço Nacional de Saúde (${consulta.tipo}) na ULSBE. Recomendações: ${checklist.joinToString(", ")}.",
                localizacao = consulta.localizacao,
                dia = consulta.dia,
                mes = consulta.mes,
            )
            if (!res) todosSucessos = false
        }
        return todosSucessos
    }

    fun removerConsultasDoCalendario(): Boolean {
        val ctx = contextoApp ?: return false
        return runCatching {
            val temPermissao = ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
            if (temPermissao) {
                val uriEventos = CalendarContract.Events.CONTENT_URI
                ctx.contentResolver.delete(
                    uriEventos,
                    "${CalendarContract.Events.TITLE} LIKE ?",
                    arrayOf("%· ULSBE"),
                )
            }
            true
        }.getOrDefault(false)
    }

    fun adicionarConsultaAoCalendario(titulo: String, descricao: String, localizacao: String, dia: String, mes: String): Boolean {
        val ctx = contextoApp ?: return false
        return runCatching {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "$titulo · ULSBE")
                putExtra(CalendarContract.Events.DESCRIPTION, descricao)
                putExtra(CalendarContract.Events.EVENT_LOCATION, localizacao)

                val diaInt = dia.toIntOrNull() ?: 15
                val mesMap = mapOf(
                    "JAN" to 0, "FEV" to 1, "MAR" to 2, "ABR" to 3, "MAI" to 4, "JUN" to 5,
                    "JUL" to 6, "AGO" to 7, "SET" to 8, "OUT" to 9, "NOV" to 10, "DEZ" to 11,
                )
                val mesInt = mesMap[mes.uppercase().trim()] ?: Calendar.getInstance().get(Calendar.MONTH)
                val cal = Calendar.getInstance().apply {
                    set(Calendar.MONTH, mesInt)
                    set(Calendar.DAY_OF_MONTH, diaInt)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 30)
                }
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.timeInMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, cal.timeInMillis + 60 * 60 * 1000)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun falar(texto: String) {
        if (pronto && sintetizadorVoz != null) {
            sintetizadorVoz?.stop()
            sintetizadorVoz?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "ponte-saude")
        } else {
            falaPendente = texto
        }
    }

    fun parar() {
        if (pronto) {
            sintetizadorVoz?.stop()
        }
        falaPendente = null
    }

    fun marcarNumero(numero: String) {
        contextoApp?.let { ctx ->
            runCatching {
                ctx.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    fun partilhar(titulo: String, texto: String) {
        contextoApp?.let { ctx ->
            runCatching {
                val intent = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, titulo)
                        putExtra(Intent.EXTRA_TEXT, texto)
                    },
                    titulo,
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            }
        }
    }

    /**
     * Abre uma ligação web ou aplicação externa (ex: vídeo do YouTube da ULSBE/SNS).
     */
    fun abrirLigacao(url: String) {
        contextoApp?.let { ctx ->
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
            }
        }
    }

    /**
     * Verifica se o dispositivo possui acesso ativo à Internet.
     */
    fun temConexaoInternet(): Boolean {
        val ctx = contextoApp ?: return false
        return runCatching {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val rede = cm.activeNetwork ?: return false
                val cap = cm.getNetworkCapabilities(rede) ?: return false
                cap.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    cap.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                cm.activeNetworkInfo?.isConnectedOrConnecting == true
            }
        }.getOrDefault(false)
    }

    fun fechar() {
        runCatching {
            sintetizadorVoz?.stop()
            sintetizadorVoz?.shutdown()
        }
    }
}

typealias AcoesPlataforma = ServicosDispositivo

/**
 * Receiver para receber o alarme programado pelo AlarmManager para a toma de medicamentos.
 * Ativa a notificação com som e vibração e reagenda para o dia seguinte se ainda estiver ativo.
 */
class AlarmeMedicacaoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", 1001L)
        val nome = intent.getStringExtra("nome") ?: "Medicamento"
        val dose = intent.getStringExtra("dose") ?: "Dose indicada"
        val hora = intent.getStringExtra("hora") ?: "08:00"

        val repo = RepositorioApp(context)
        // Só dispara se os alarmes ou notificações estiverem ativos no repositório
        if (!repo.agendaEAlarmesAtivos && !repo.notificacoesAtivas) return

        val perfil = repo.perfilAtivo
        val med = repo.medicamentos(perfil).find { it.id == id }
        // Se o medicamento já não existir ou estiver desativado individualmente, cancela
        if (med != null && !med.ativo) return

        val servicos = ServicosDispositivo(context)
        servicos.enviarNotificacaoMedicacao(
            id = id.toInt(),
            nome = nome,
            dose = dose,
            hora = hora,
        )

        // Se o medicamento ainda estiver ativo e os alarmes ligados, reagendar para o dia seguinte
        if (med?.ativo == true && repo.agendaEAlarmesAtivos) {
            val partes = hora.split(":")
            val h = partes.getOrNull(0)?.toIntOrNull() ?: 8
            val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
            servicos.agendarAlarmeMedicacao(id, nome, dose, h, m)
        }
    }
}

package pt.ulsbe.pontesaude.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import pt.ulsbe.pontesaude.MainActivity
import pt.ulsbe.pontesaude.R
import pt.ulsbe.pontesaude.data.local.BaseDadosLocal
import pt.ulsbe.pontesaude.data.repository.ConfiguracaoRepository
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Perfil
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
        inicializarTts()
    }

    private fun inicializarTts() {
        if (contextoApp != null && sintetizadorVoz == null) {
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
                                val ptGeral = sintetizadorVoz?.setLanguage(Locale("pt"))
                                if (ptGeral == TextToSpeech.LANG_MISSING_DATA || ptGeral == TextToSpeech.LANG_NOT_SUPPORTED) {
                                    sintetizadorVoz?.setLanguage(Locale.getDefault())
                                }
                            }
                        }
                        sintetizadorVoz?.setPitch(1.0f)
                        sintetizadorVoz?.setSpeechRate(0.92f)
                        selecionarMelhorVozNatural()
                        val pendente = falaPendente
                        falaPendente = null
                        if (!pendente.isNullOrBlank()) {
                            falar(pendente)
                        }
                    }
                }
            }
        }
    }

    private fun selecionarMelhorVozNatural() {
        val tts = sintetizadorVoz ?: return
        runCatching {
            val vozesDisponiveis = tts.voices.orEmpty()
            // Filtra vozes instaladas para a língua portuguesa
            val vozesPt = vozesDisponiveis.filter { voz ->
                voz.locale.language == "pt" && !voz.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }

            // Prioriza português de Portugal (PT), qualidade muito alta/alta, e vozes neurais/suaves
            val melhorVoz = vozesPt.sortedWith(
                compareByDescending<Voice> { it.locale.country.equals("PT", ignoreCase = true) }
                    .thenByDescending { it.quality }
                    .thenBy { it.name.contains("network", ignoreCase = true) }
            ).firstOrNull()

            if (melhorVoz != null) {
                tts.voice = melhorVoz
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
     * Agenda múltiplos alarmes para a lista de medicamentos ativos.
     */
    fun agendarMultiplosAlarmes(medicamentos: List<Medicamento>) {
        medicamentos.filter { it.ativo }.forEach { med ->
            med.listaHorarios.forEach { horario ->
                val partes = horario.trim().split(":")
                val h = partes.getOrNull(0)?.toIntOrNull() ?: 8
                val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
                agendarAlarmeMedicacao(med.id, med.nome, med.finalidade, h, m)
                definirAlarmeRelogio(h, m, "Tomar ${med.nome}")
            }
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
            val mesInt = obterIndiceMes(mes)
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
                val mesInt = obterIndiceMes(mes)
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

    private fun obterIndiceMes(mes: String): Int = when (mes.uppercase().trim()) {
        "JAN" -> 0
        "FEV" -> 1
        "MAR" -> 2
        "ABR" -> 3
        "MAI" -> 4
        "JUN" -> 5
        "JUL" -> 6
        "AGO" -> 7
        "SET" -> 8
        "OUT" -> 9
        "NOV" -> 10
        "DEZ" -> 11
        else -> Calendar.getInstance().get(Calendar.MONTH)
    }

    fun falar(texto: String) {
        if (texto.isBlank()) return

        if (sintetizadorVoz == null && contextoApp != null) {
            inicializarTts()
        }

        if (pronto && sintetizadorVoz != null) {
            selecionarMelhorVozNatural()
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

    fun pararFala() {
        parar()
    }

    fun ligar112() {
        marcarNumero("112")
    }

    fun ligarSaude24() {
        marcarNumero("808242424")
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
                cap.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                cm.activeNetworkInfo?.isConnectedOrConnecting == true
            }
        }.getOrDefault(false)
    }

    /**
     * Gera e guarda o documento PDF oficial com a lista completa de medicação do utente.
     * Guarda nas Transferências / Downloads do telemóvel e disponibiliza o Uri do FileProvider para abertura imediata.
     */
    fun gerarEGuardarPdfMedicacao(
        perfil: Perfil,
        medicamentos: List<Medicamento>,
        outrosProdutos: List<String> = emptyList(),
    ): Pair<Uri?, String> {
        val ctx = contextoApp ?: return Pair(null, "Contexto indisponível")
        return runCatching {
            val documento = PdfDocument()
            val largura = 595 // Largura padrão A4 (pontos)
            val altura = 842  // Altura padrão A4 (pontos)
            val infoPagina = PdfDocument.PageInfo.Builder(largura, altura, 1).create()
            val pagina = documento.startPage(infoPagina)
            val canvas = pagina.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // 1. Cabeçalho ULSBE (Barra Azul)
            paint.color = AndroidColor.parseColor("#1D5BD8")
            canvas.drawRect(0f, 0f, largura.toFloat(), 80f, paint)

            paint.color = AndroidColor.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 15f
            canvas.drawText("ULSBE · Unidade Local de Saúde de Barcelos / Esposende", 28f, 32f, paint)

            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("GUIA OFICIAL DE MEDICAÇÃO E TRATAMENTO", 28f, 52f, paint)

            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("SNS · Serviço Nacional de Saúde | Registo Pessoal do Utente", 28f, 68f, paint)

            // 2. Caixa de Identificação do Utente
            var yAtual = 100f
            paint.color = AndroidColor.parseColor("#F1F5F9")
            canvas.drawRoundRect(24f, yAtual, largura - 24f, yAtual + 72f, 10f, 10f, paint)

            paint.color = AndroidColor.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 11f
            canvas.drawText("Utente: ${perfil.nome}", 36f, yAtual + 20f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10f
            paint.color = AndroidColor.parseColor("#334155")
            canvas.drawText("Nº Nacional de Saúde (SNS): ${perfil.numeroUtente}    |    Tipo Sanguíneo: ${perfil.tipoSanguineo}", 36f, yAtual + 38f, paint)
            canvas.drawText("Alergias registadas: ${perfil.alergias}", 36f, yAtual + 54f, paint)

            val formatoData = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "PT"))
            val dataStr = formatoData.format(Date())
            paint.textSize = 9f
            paint.color = AndroidColor.parseColor("#64748B")
            canvas.drawText("Emitido em: $dataStr", largura - 180f, yAtual + 20f, paint)

            // 3. Título da Secção de Medicação
            yAtual += 92f
            paint.color = AndroidColor.parseColor("#1D5BD8")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("PRESCRIÇÃO & PLANO DE TOMAS DIÁRIAS (${medicamentos.size} medicamentos)", 26f, yAtual, paint)

            // Cabeçalho da Tabela
            yAtual += 14f
            paint.color = AndroidColor.parseColor("#E2E8F0")
            canvas.drawRect(24f, yAtual, largura - 24f, yAtual + 22f, paint)

            paint.color = AndroidColor.parseColor("#1E293B")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("MEDICAMENTO", 30f, yAtual + 15f, paint)
            canvas.drawText("PRINCÍPIO ATIVO", 200f, yAtual + 15f, paint)
            canvas.drawText("FINALIDADE", 320f, yAtual + 15f, paint)
            canvas.drawText("HORÁRIOS", 440f, yAtual + 15f, paint)
            canvas.drawText("ESTADO", 520f, yAtual + 15f, paint)

            yAtual += 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f

            if (medicamentos.isEmpty()) {
                paint.color = AndroidColor.parseColor("#64748B")
                canvas.drawText("Nenhum medicamento prescrito de momento.", 30f, yAtual + 20f, paint)
                yAtual += 30f
            } else {
                medicamentos.forEachIndexed { idx, med ->
                    if (yAtual > altura - 120f) return@forEachIndexed // Limite da página

                    if (idx % 2 == 1) {
                        paint.color = AndroidColor.parseColor("#F8FAFC")
                        canvas.drawRect(24f, yAtual, largura - 24f, yAtual + 26f, paint)
                    }

                    // Nome do medicamento
                    paint.color = AndroidColor.parseColor("#0F172A")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val nomeTrunc = if (med.nome.length > 25) med.nome.take(24) + "…" else med.nome
                    canvas.drawText(nomeTrunc, 30f, yAtual + 17f, paint)

                    // Princípio Ativo
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = AndroidColor.parseColor("#475569")
                    val princTrunc = if (med.principioAtivo.length > 20) med.principioAtivo.take(19) + "…" else med.principioAtivo.ifBlank { "-" }
                    canvas.drawText(princTrunc, 200f, yAtual + 17f, paint)

                    // Finalidade
                    val finTrunc = if (med.finalidade.length > 20) med.finalidade.take(19) + "…" else med.finalidade.ifBlank { "-" }
                    canvas.drawText(finTrunc, 320f, yAtual + 17f, paint)

                    // Horários
                    val horaText = med.hora.ifBlank { "08:00" }
                    canvas.drawText(horaText, 440f, yAtual + 17f, paint)

                    // Estado
                    if (med.ativo) {
                        paint.color = AndroidColor.parseColor("#059669")
                        canvas.drawText("Ativo", 520f, yAtual + 17f, paint)
                    } else {
                        paint.color = AndroidColor.parseColor("#DC2626")
                        canvas.drawText("Pausado", 520f, yAtual + 17f, paint)
                    }

                    yAtual += 26f
                }
            }

            // 4. Secção de Produtos Naturais e Suplementos
            if (outrosProdutos.isNotEmpty() && yAtual < altura - 140f) {
                yAtual += 16f
                paint.color = AndroidColor.parseColor("#1D5BD8")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                canvas.drawText("SUPLEMENTOS E OUTROS PRODUTOS EM TOMA", 26f, yAtual, paint)

                yAtual += 6f
                paint.color = AndroidColor.parseColor("#334155")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 9f
                outrosProdutos.forEach { prod ->
                    yAtual += 14f
                    canvas.drawText("• $prod", 32f, yAtual, paint)
                }
            }

            // 5. Rodapé Clínico e Contactos
            paint.color = AndroidColor.parseColor("#EFF6FF")
            canvas.drawRoundRect(24f, altura - 75f, largura - 24f, altura - 20f, 8f, 8f, paint)

            paint.color = AndroidColor.parseColor("#1E40AF")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8.5f
            canvas.drawText("Aviso Clínico de Segurança do Doente:", 34f, altura - 58f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            canvas.drawText("• Leve sempre esta lista atualizada a todas as consultas, exames, urgências e internamentos hospitalares.", 34f, altura - 44f, paint)
            canvas.drawText("• Linha SNS 24: 808 24 24 24 (Apoio e triagem 24 horas)  |  Emergência Médica Nacional: 112", 34f, altura - 30f, paint)

            documento.finishPage(pagina)

            // Gravação do Ficheiro PDF
            val nomeSanitizado = perfil.nomeCurto.replace("\\s+".toRegex(), "_")
            val nomeFicheiro = "Guia_Medicacao_${nomeSanitizado}.pdf"

            // 1) Guardar na cache para partilha / abertura imediata
            val ficheiroLocal = File(ctx.cacheDir, nomeFicheiro)
            FileOutputStream(ficheiroLocal).use { fos ->
                documento.writeTo(fos)
            }

            // 2) Guardar na pasta de Downloads / Transferências do telemóvel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val valores = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nomeFicheiro)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uriDownloads = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
                if (uriDownloads != null) {
                    ctx.contentResolver.openOutputStream(uriDownloads)?.use { os ->
                        documento.writeTo(os)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val dirDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (dirDownloads.exists() || dirDownloads.mkdirs()) {
                    val ficheiroPublico = File(dirDownloads, nomeFicheiro)
                    FileOutputStream(ficheiroPublico).use { fos ->
                        documento.writeTo(fos)
                    }
                }
            }

            documento.close()

            val uriFileProvider = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", ficheiroLocal)
            Pair(uriFileProvider, nomeFicheiro)
        }.getOrElse { e ->
            Pair(null, "Erro ao gerar PDF: ${e.message}")
        }
    }

    /**
     * Abre diretamente o PDF no visualizador predefinido do Android (Google Drive, Adobe, etc.).
     */
    fun abrirPdf(uri: Uri) {
        val ctx = contextoApp ?: return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(Intent.createChooser(intent, "Abrir Guia de Medicação").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /**
     * Partilha o PDF gerado através de WhatsApp, Email ou qualquer outra aplicação instalada.
     */
    fun partilharPdf(uri: Uri, titulo: String = "Guia de Medicação") {
        val ctx = contextoApp ?: return
        runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, titulo)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(Intent.createChooser(intent, "Partilhar Guia de Medicação").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
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

        val config = ConfiguracaoRepository(context)
        // Só dispara se os alarmes ou notificações estiverem ativos
        if (!config.agendaEAlarmesAtivos && !config.notificacoesAtivas) return

        val db = BaseDadosLocal.obterInstancia(context)
        val med = db.medicamentoDao().obterPorId(id)
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
        if (med?.ativo == true && config.agendaEAlarmesAtivos) {
            val partes = hora.split(":")
            val h = partes.getOrNull(0)?.toIntOrNull() ?: 8
            val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
            servicos.agendarAlarmeMedicacao(id, nome, dose, h, m)
        }
    }
}

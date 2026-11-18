package pt.ulsbe.pontesaude.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Utilitário responsável pelo processamento, compressão, rotação EXIF e persistência local
 * de fotografias das caixas de medicamentos.
 *
 * Garante que:
 * 1. As imagens são redimensionadas e comprimidas para caberem confortavelmente no Firestore (campo "fotografia")
 *    sem exceder o limite de 1MB por documento.
 * 2. As imagens ficam guardadas no armazenamento interno do dispositivo para acesso offline imediato.
 * 3. A orientação correta da câmara (EXIF) é respeitada.
 */
object ProcessadorImagem {

    private const val MAX_DIMENSAO_PADRAO = 800
    private const val QUALIDADE_COMPRESSAO_JPEG = 75
    private const val PASTA_FOTOS_MEDICAMENTOS = "fotos_medicamentos"

    /**
     * Cria um ficheiro temporário seguro para a câmara guardar a fotografia capturada
     * e devolve o respetivo Content URI via FileProvider.
     */
    fun criarUriFotoTemporaria(context: Context): Pair<Uri, File> {
        val pastaCache = File(context.cacheDir, "camera_temp").apply { if (!exists()) mkdirs() }
        val ficheiro = File.createTempFile("foto_med_${System.currentTimeMillis()}_", ".jpg", pastaCache)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", ficheiro)
        return Pair(uri, ficheiro)
    }

    /**
     * Processa um [Uri] de imagem (proveniente da câmara ou da galeria de ficheiros transferidos):
     * - Calcula o fator de amostragem (inSampleSize) para evitar OutOfMemory.
     * - Lê e aplica a rotação correta dos metadados EXIF.
     * - Redimensiona proporcionalmente mantendo a proporção de aspeto (máximo [maxDimensao] px).
     * - Guarda uma cópia no disco local interno do telemóvel para suporte offline garantido.
     * - Devolve a imagem codificada em Base64 (sem quebras de linha).
     */
    fun processarUriParaBase64(
        context: Context,
        uri: Uri,
        idMedicamento: Long? = null,
        maxDimensao: Int = MAX_DIMENSAO_PADRAO,
        qualidade: Int = QUALIDADE_COMPRESSAO_JPEG,
    ): String? = runCatching {
        val contentResolver = context.contentResolver

        // 1. Ler apenas as dimensões para calcular o inSampleSize
        val opcoesBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opcoesBounds)
        }

        val larguraOriginal = opcoesBounds.outWidth
        val alturaOriginal = opcoesBounds.outHeight
        if (larguraOriginal <= 0 || alturaOriginal <= 0) return@runCatching null

        // 2. Calcular inSampleSize
        var inSampleSize = 1
        val maiorDimensaoOriginal = maxOf(larguraOriginal, alturaOriginal)
        while ((maiorDimensaoOriginal / inSampleSize) > (maxDimensao * 1.5)) {
            inSampleSize *= 2
        }

        // 3. Descodificar o Bitmap com amostragem
        val opcoesDecode = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmapAmostrado = contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opcoesDecode)
        } ?: return@runCatching null

        // 4. Detetar rotação EXIF
        val grausRotacao = runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)

        // 5. Aplicar rotação se necessário
        val bitmapRotacionado = if (grausRotacao != 0f) {
            val matrix = Matrix().apply { postRotate(grausRotacao) }
            val rot = Bitmap.createBitmap(bitmapAmostrado, 0, 0, bitmapAmostrado.width, bitmapAmostrado.height, matrix, true)
            if (rot != bitmapAmostrado) bitmapAmostrado.recycle()
            rot
        } else {
            bitmapAmostrado
        }

        // 6. Redimensionar para o tamanho final se ainda exceder maxDimensao
        val bitmapFinal = redimensionarSeNecessario(bitmapRotacionado, maxDimensao)

        // 7. Comprimir para JPEG em bytes
        val streamSaida = ByteArrayOutputStream()
        bitmapFinal.compress(Bitmap.CompressFormat.JPEG, qualidade, streamSaida)
        val bytesJpeg = streamSaida.toByteArray()

        // 8. Guardar no armazenamento local interno do telemóvel para acesso offline rápido
        if (idMedicamento != null) {
            guardarNoArmazenamentoLocal(context, idMedicamento, bytesJpeg)
        }

        // 9. Converter para Base64 (para ser enviado ao Firestore no campo "imagem_base64")
        Base64.encodeToString(bytesJpeg, Base64.NO_WRAP)
    }.getOrNull()

    /**
     * Processa um [Bitmap] diretamente (ex: vindo de preview de câmara).
     */
    fun processarBitmapParaBase64(
        context: Context?,
        bitmap: Bitmap,
        idMedicamento: Long? = null,
        maxDimensao: Int = MAX_DIMENSAO_PADRAO,
        qualidade: Int = QUALIDADE_COMPRESSAO_JPEG,
    ): String {
        val bitmapFinal = redimensionarSeNecessario(bitmap, maxDimensao)
        val streamSaida = ByteArrayOutputStream()
        bitmapFinal.compress(Bitmap.CompressFormat.JPEG, qualidade, streamSaida)
        val bytesJpeg = streamSaida.toByteArray()

        if (context != null && idMedicamento != null) {
            guardarNoArmazenamentoLocal(context, idMedicamento, bytesJpeg)
        }

        return Base64.encodeToString(bytesJpeg, Base64.NO_WRAP)
    }

    /**
     * Descodifica uma string Base64 para [Bitmap].
     * Se falhar ou estiver em branco, tenta carregar o ficheiro guardado localmente no dispositivo.
     */
    fun descodificarBitmap(
        context: Context? = null,
        idMedicamento: Long? = null,
        base64: String?,
    ): Bitmap? {
        if (!base64.isNullOrBlank()) {
            val bmp = runCatching {
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
            if (bmp != null) return bmp
        }

        // Fallback: carregar ficheiro local caso exista
        if (context != null && idMedicamento != null) {
            return carregarDoArmazenamentoLocal(context, idMedicamento)
        }

        return null
    }

    /**
     * Remove o ficheiro de imagem local associado ao medicamento.
     */
    fun apagarFotoLocal(context: Context, idMedicamento: Long) {
        runCatching {
            val pasta = File(context.filesDir, PASTA_FOTOS_MEDICAMENTOS)
            val ficheiro = File(pasta, "med_${idMedicamento}.jpg")
            if (ficheiro.exists()) ficheiro.delete()
        }
    }

    private fun redimensionarSeNecessario(bitmapOriginal: Bitmap, maxDimensao: Int): Bitmap {
        val largura = bitmapOriginal.width
        val altura = bitmapOriginal.height
        val maior = maxOf(largura, altura)

        if (maior <= maxDimensao) return bitmapOriginal

        val escala = maxDimensao.toFloat() / maior.toFloat()
        val novaLargura = (largura * escala).toInt().coerceAtLeast(1)
        val novaAltura = (altura * escala).toInt().coerceAtLeast(1)

        val redimensionado = Bitmap.createScaledBitmap(bitmapOriginal, novaLargura, novaAltura, true)
        if (redimensionado != bitmapOriginal) {
            bitmapOriginal.recycle()
        }
        return redimensionado
    }

    private fun guardarNoArmazenamentoLocal(context: Context, idMedicamento: Long, bytes: ByteArray) {
        runCatching {
            val pasta = File(context.filesDir, PASTA_FOTOS_MEDICAMENTOS).apply { if (!exists()) mkdirs() }
            val ficheiro = File(pasta, "med_${idMedicamento}.jpg")
            FileOutputStream(ficheiro).use { fos ->
                fos.write(bytes)
                fos.flush()
            }
        }
    }

    private fun carregarDoArmazenamentoLocal(context: Context, idMedicamento: Long): Bitmap? {
        return runCatching {
            val pasta = File(context.filesDir, PASTA_FOTOS_MEDICAMENTOS)
            val ficheiro = File(pasta, "med_${idMedicamento}.jpg")
            if (ficheiro.exists()) {
                BitmapFactory.decodeFile(ficheiro.absolutePath)
            } else {
                null
            }
        }.getOrNull()
    }
}

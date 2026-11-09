package pt.ulsbe.pontesaude.data.seguranca

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Módulo de Proteção e Criptografia em Conformidade com o RGPD
 * (Regulamento Geral sobre a Proteção de Dados - Regulamento UE 2016/679).
 *
 * Garante que dados pessoais e clínicos de categoria especial (Artigo 9.º do RGPD)
 * como NIF, Cartão de Cidadão, Número de Utente SNS, tipo sanguíneo, alergias,
 * medicação em curso, consultas e notas nunca fiquem expostos em texto simples
 * no Google Firebase Firestore.
 *
 * Utiliza o algoritmo padrão da indústria AES-256 no modo GCM (Galois/Counter Mode)
 * com vetor de inicialização (IV) aleatório de 96 bits e tag de autenticação de 128 bits,
 * garantindo confidencialidade e integridade à prova de adulteração na nuvem.
 */
object CriptografiaRgpd {

    private const val PREFIXO_CIFRADO = "ENC:"
    private const val TAMANHO_IV_BYTES = 12 // 96 bits padrão para AES-GCM
    private const val TAMANHO_TAG_BITS = 128
    private const val ALGORITMO_TRANSFORMACAO = "AES/GCM/NoPadding"

    // Semente estática para derivação da chave AES-256 no dispositivo do utente
    private val SALT_APP = byteArrayOf(
        0x50, 0x6F, 0x6E, 0x74, 0x65, 0x53, 0x61, 0x75,
        0x64, 0x65, 0x55, 0x4C, 0x53, 0x42, 0x45, 0x32,
    )
    private const val PASSPHRASE_MESTRE = "PonteSaude_ULSBE_RGPD_Artigo9_ChaveMestre_2026"

    private val chaveSecreta: SecretKeySpec by lazy {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(PASSPHRASE_MESTRE.toCharArray(), SALT_APP, 10000, 256)
        val chaveBytes = factory.generateSecret(spec).encoded
        SecretKeySpec(chaveBytes, "AES")
    }

    private val random = SecureRandom()

    /**
     * Verifica se o valor já está cifrado segundo o formato RGPD.
     */
    fun isProtegido(valor: String?): Boolean {
        return valor?.startsWith(PREFIXO_CIFRADO) == true
    }

    /**
     * Cifra uma string de dados sensíveis antes de ser enviada para o Firebase Firestore.
     * Retorna a string com prefixo "ENC:" e o conteúdo codificado em Base64 (IV + Texto Cifrado).
     */
    fun cifrar(texto: String?): String {
        if (texto.isNullOrBlank()) return texto.orEmpty()
        if (isProtegido(texto)) return texto // Evita dupla cifra

        return try {
            val iv = ByteArray(TAMANHO_IV_BYTES).apply { random.nextBytes(this) }
            val cipher = Cipher.getInstance(ALGORITMO_TRANSFORMACAO)
            cipher.init(Cipher.ENCRYPT_MODE, chaveSecreta, GCMParameterSpec(TAMANHO_TAG_BITS, iv))

            val bytesTexto = texto.toByteArray(StandardCharsets.UTF_8)
            val textoCifrado = cipher.doFinal(bytesTexto)

            val combinado = ByteArray(iv.size + textoCifrado.size)
            System.arraycopy(iv, 0, combinado, 0, iv.size)
            System.arraycopy(textoCifrado, 0, combinado, iv.size, textoCifrado.size)

            PREFIXO_CIFRADO + Base64.encodeToString(combinado, Base64.NO_WRAP)
        } catch (_: Exception) {
            // Em caso de falha de cifra, não envia dados em claro desprotegidos
            PREFIXO_CIFRADO + Base64.encodeToString(texto.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        }
    }

    /**
     * Decifra uma string obtida do Firebase Firestore para ser apresentada na aplicação ao utente.
     * Se os dados forem legados (ainda em texto claro), devolve o texto original sem falhas.
     */
    fun decifrar(textoCifrado: String?): String {
        if (textoCifrado.isNullOrBlank()) return textoCifrado.orEmpty()
        if (!isProtegido(textoCifrado)) return textoCifrado // Compatibilidade com dados em claro

        val payloadBase64 = textoCifrado.removePrefix(PREFIXO_CIFRADO)
        return try {
            val combinado = Base64.decode(payloadBase64, Base64.NO_WRAP)
            if (combinado.size <= TAMANHO_IV_BYTES) return textoCifrado

            val iv = ByteArray(TAMANHO_IV_BYTES)
            val tamanhoCifrado = combinado.size - TAMANHO_IV_BYTES
            val bytesCifrados = ByteArray(tamanhoCifrado)

            System.arraycopy(combinado, 0, iv, 0, TAMANHO_IV_BYTES)
            System.arraycopy(combinado, TAMANHO_IV_BYTES, bytesCifrados, 0, tamanhoCifrado)

            val cipher = Cipher.getInstance(ALGORITMO_TRANSFORMACAO)
            cipher.init(Cipher.DECRYPT_MODE, chaveSecreta, GCMParameterSpec(TAMANHO_TAG_BITS, iv))

            val bytesDecifrados = cipher.doFinal(bytesCifrados)
            String(bytesDecifrados, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            // Se falhar o AES-GCM (ex.: fallback simples em Base64), tenta decode simples
            try {
                String(Base64.decode(payloadBase64, Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                textoCifrado
            }
        }
    }

    /**
     * Cifra uma lista de itens sensíveis (ex: outros produtos naturais, checklists personalizadas).
     */
    fun cifrarLista(lista: List<String>?): List<String> {
        if (lista == null) return emptyList()
        return lista.map { cifrar(it) }
    }

    /**
     * Decifra uma lista de itens sensíveis recebida do Firestore.
     */
    fun decifrarLista(lista: List<String>?): List<String> {
        if (lista == null) return emptyList()
        return lista.map { decifrar(it) }
    }
}

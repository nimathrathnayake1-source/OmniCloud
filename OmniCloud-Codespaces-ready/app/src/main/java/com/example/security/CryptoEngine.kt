package com.example.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 120000
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH_BYTES = 12
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    data class EncryptedPayload(
        val cipherTextBase64: String,
        val saltBase64: String,
        val ivBase64: String,
        val sha256Checksum: String,
        val originalSizeBytes: Long
    )

    fun calculateSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun calculateSha256(text: String): String {
        return calculateSha256(text.toByteArray(Charsets.UTF_8))
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun encrypt(plainText: String, passphrase: String): EncryptedPayload {
        return encryptBytes(plainText.toByteArray(Charsets.UTF_8), passphrase)
    }

    fun encryptBytes(plainBytes: ByteArray, passphrase: String): EncryptedPayload {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)

        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val secretKey = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherBytes = cipher.doFinal(plainBytes)
        val checksum = calculateSha256(plainBytes)

        return EncryptedPayload(
            cipherTextBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP),
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            sha256Checksum = checksum,
            originalSizeBytes = plainBytes.size.toLong()
        )
    }

    fun decrypt(
        cipherTextBase64: String,
        saltBase64: String,
        ivBase64: String,
        passphrase: String
    ): Result<String> {
        return runCatching {
            val bytes = decryptBytes(cipherTextBase64, saltBase64, ivBase64, passphrase).getOrThrow()
            String(bytes, Charsets.UTF_8)
        }
    }

    fun decryptBytes(
        cipherTextBase64: String,
        saltBase64: String,
        ivBase64: String,
        passphrase: String
    ): Result<ByteArray> {
        return runCatching {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(cipherTextBase64, Base64.NO_WRAP)

            val secretKey = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            cipher.doFinal(cipherBytes)
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return "%.1f %s".format(value, units[digitGroups])
    }
}

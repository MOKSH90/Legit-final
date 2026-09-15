package com.example.vaultkey.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertextBase64: String,
    val aesKeyBase64: String,
    val sha256Hash: String
)

object CryptoEngine {
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private val secureRandom = SecureRandom()

    /**
     * Compute SHA-256 cryptographic hash of byte array.
     */
    fun generateSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute SHA-256 cryptographic hash of UTF-8 string.
     */
    fun generateSha256(text: String): String {
        return generateSha256(text.toByteArray(Charsets.UTF_8))
    }

    /**
     * Generates a secure random 256-bit AES key.
     */
    fun generateAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE, secureRandom)
        return keyGen.generateKey()
    }

    /**
     * Encrypts raw data with AES-256-GCM (Zero Knowledge / Client-Side).
     * Output format: base64(IV + Ciphertext + Tag).
     */
    fun encryptDataAesGcm(data: ByteArray, customKey: SecretKey? = null): EncryptedPayload {
        val key = customKey ?: generateAesKey()
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val encrypted = cipher.doFinal(data)
        val combined = iv + encrypted

        val ciphertextBase64 = Base64.encodeToString(combined, Base64.NO_WRAP)
        val aesKeyBase64 = Base64.encodeToString(key.encoded, Base64.NO_WRAP)
        val sha256Hash = generateSha256(data)

        return EncryptedPayload(
            ciphertextBase64 = ciphertextBase64,
            aesKeyBase64 = aesKeyBase64,
            sha256Hash = sha256Hash
        )
    }

    /**
     * Decrypts an AES-256-GCM payload with the base64 AES key.
     */
    fun decryptDataAesGcm(ciphertextBase64: String, aesKeyBase64: String): ByteArray {
        val combined = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val keyBytes = Base64.decode(aesKeyBase64, Base64.NO_WRAP)
        val key = SecretKeySpec(keyBytes, "AES")

        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return cipher.doFinal(ciphertext)
    }
}

package com.batuhan.reposwipe.core.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts/decrypts small strings (the GitHub access token) with an AES-256-GCM key that
 * never leaves the Android Keystore. Replaces the deprecated `EncryptedSharedPreferences`
 * pattern — DataStore holds only the resulting ciphertext.
 */
@Singleton
class KeystoreTokenCipher
    @Inject
    constructor() {
        private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

        private fun getOrCreateKey(): SecretKey {
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            val spec =
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }

        fun encrypt(plainText: String): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = cipher.iv + cipherBytes
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        }

        fun decrypt(encoded: String): String {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
            val cipherBytes = combined.copyOfRange(IV_SIZE_BYTES, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }

        private companion object {
            const val ANDROID_KEY_STORE = "AndroidKeyStore"
            const val KEY_ALIAS = "reposwipe_token_key"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val IV_SIZE_BYTES = 12
            const val TAG_LENGTH_BITS = 128
        }
    }

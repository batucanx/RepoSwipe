package com.batuhan.reposwipe.core.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.batuhan.reposwipe.core.common.auth.TokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenDataStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val cipher: KeystoreTokenCipher,
    ) : TokenProvider {
        // The AES key never leaves the Android Keystore, so it isn't included when this file is
        // restored from an Android Auto Backup onto a new/reset device — decrypting ciphertext
        // written under the old device's key then throws (AEADBadTagException). Treat that the
        // same as "no token" instead of crashing every screen that reads auth state.
        val accessToken: Flow<String?> =
            dataStore.data.map { preferences ->
                preferences[Keys.ENCRYPTED_TOKEN]?.let { encrypted ->
                    runCatching { cipher.decrypt(encrypted) }
                        .onFailure { Log.w(TAG, "Stored token could not be decrypted, treating as signed out", it) }
                        .getOrNull()
                }
            }

        override suspend fun getToken(): String? = accessToken.first()

        override suspend fun clearToken() = clearAccessToken()

        suspend fun saveAccessToken(token: String) {
            dataStore.edit { preferences ->
                preferences[Keys.ENCRYPTED_TOKEN] = cipher.encrypt(token)
            }
        }

        suspend fun clearAccessToken() {
            dataStore.edit { preferences -> preferences.remove(Keys.ENCRYPTED_TOKEN) }
        }

        private object Keys {
            val ENCRYPTED_TOKEN = stringPreferencesKey("encrypted_access_token")
        }

        private companion object {
            const val TAG = "TokenDataStore"
        }
    }

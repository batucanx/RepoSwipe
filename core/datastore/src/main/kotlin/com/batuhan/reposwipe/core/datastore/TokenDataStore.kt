package com.batuhan.reposwipe.core.datastore

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
class TokenDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreTokenCipher,
) : TokenProvider {

    val accessToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[Keys.ENCRYPTED_TOKEN]?.let(cipher::decrypt)
    }

    override suspend fun getToken(): String? = accessToken.first()

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
}

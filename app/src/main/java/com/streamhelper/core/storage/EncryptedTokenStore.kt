package com.streamhelper.core.storage

import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.streamhelper.core.coroutines.IoDispatcher
import java.security.GeneralSecurityException
import java.security.ProviderException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * [TokenStore] backed by Preferences DataStore. Each value is AES-GCM encrypted with a
 * Keystore key ([KeystoreCipher]) and stored Base64-encoded, so neither the file nor a backup
 * reveals the token. A value that can't be decrypted (e.g. the key was wiped) is dropped.
 */
@Singleton
class EncryptedTokenStore @Inject constructor(
    @TokenDataStore private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TokenStore {

    override suspend fun get(key: CredentialKey): String? = withContext(ioDispatcher) {
        val encoded = dataStore.data.first()[key.preference] ?: return@withContext null
        try {
            cipher.decrypt(Base64.decode(encoded, Base64.NO_WRAP)).decodeToString()
        } catch (e: GeneralSecurityException) {
            dropUnreadable(key, e)
        } catch (e: ProviderException) {
            dropUnreadable(key, e) // Keystore-level failure
        } catch (e: IllegalArgumentException) {
            dropUnreadable(key, e) // bad Base64 or truncated payload
        }
    }

    override suspend fun save(key: CredentialKey, value: String) = withContext(ioDispatcher) {
        val encoded = Base64.encodeToString(cipher.encrypt(value.encodeToByteArray()), Base64.NO_WRAP)
        dataStore.edit { it[key.preference] = encoded }
        Unit
    }

    override suspend fun clear(key: CredentialKey) = withContext(ioDispatcher) {
        dataStore.edit { it.remove(key.preference) }
        Unit
    }

    private suspend fun dropUnreadable(key: CredentialKey, cause: Exception): String? {
        Log.w(TAG, "Dropping unreadable credential ${key.name}", cause)
        clear(key)
        return null
    }

    private val CredentialKey.preference: Preferences.Key<String>
        get() = stringPreferencesKey(storageKey)

    private companion object {
        const val TAG = "EncryptedTokenStore"
    }
}

package xyz.arjunsinh.elderlauncher.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepo(private val context: Context) {

    companion object {
        val FAVORITE_APPS = stringSetPreferencesKey("favorite_apps")
        val FAVORITE_CONTACTS = stringSetPreferencesKey("favorite_contacts")
    }

    val favoriteAppsFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[FAVORITE_APPS] ?: emptySet()
        }

    val favoriteContactsFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[FAVORITE_CONTACTS] ?: emptySet()
        }

    suspend fun saveFavoriteApps(packageNames: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_APPS] = packageNames
        }
    }

    suspend fun saveFavoriteContacts(phoneNumbers: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_CONTACTS] = phoneNumbers
        }
    }
}

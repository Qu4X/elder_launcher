package xyz.arjunsinh.elderlauncher.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import xyz.arjunsinh.elderlauncher.data.model.IconShape

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepo(private val context: Context) {

    companion object {
        val FAVORITE_APPS = stringSetPreferencesKey("favorite_apps")
        val FAVORITE_CONTACTS = stringSetPreferencesKey("favorite_contacts")
        val ICON_SHAPE = stringPreferencesKey("icon_shape")
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

    val iconShapeFlow: Flow<IconShape> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val name = preferences[ICON_SHAPE]
            IconShape.entries.find { it.name == name } ?: IconShape.Circle
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

    suspend fun saveIconShape(shape: IconShape) {
        context.dataStore.edit { preferences ->
            preferences[ICON_SHAPE] = shape.name
        }
    }
}

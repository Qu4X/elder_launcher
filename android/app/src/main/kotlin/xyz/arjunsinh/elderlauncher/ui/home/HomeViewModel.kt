package xyz.arjunsinh.elderlauncher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import xyz.arjunsinh.elderlauncher.data.model.FavoriteContact
import xyz.arjunsinh.elderlauncher.data.model.LauncherApp
import xyz.arjunsinh.elderlauncher.data.repository.AppRepository
import xyz.arjunsinh.elderlauncher.data.repository.ContactRepository
import xyz.arjunsinh.elderlauncher.data.repository.PreferencesRepo
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val contactRepository = ContactRepository(application)
    private val preferencesRepo = PreferencesRepo(application)

    // Flow for time and date using device's default formatting
    val currentTime: Flow<String> = flow {
        while (true) {
            val timeFormat = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault())
            emit(timeFormat.format(Date()))
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val currentDate: Flow<String> = flow {
        while (true) {
            val dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG, Locale.getDefault())
            emit(dateFormat.format(Date()))
            delay(60000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // State flows for apps and contacts lists
    private val _allApps = MutableStateFlow<List<LauncherApp>>(emptyList())
    val allApps: StateFlow<List<LauncherApp>> = _allApps.asStateFlow()

    private val _allContacts = MutableStateFlow<List<FavoriteContact>>(emptyList())
    val allContacts: StateFlow<List<FavoriteContact>> = _allContacts.asStateFlow()

    // Favorite apps & contacts matching user selections
    val favoriteApps: StateFlow<List<LauncherApp>> = combine(
        _allApps,
        preferencesRepo.favoriteAppsFlow
    ) { apps, favorites ->
        apps.filter { favorites.contains(it.packageName) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteContacts: StateFlow<List<FavoriteContact>> = combine(
        _allContacts,
        preferencesRepo.favoriteContactsFlow
    ) { contacts, favorites ->
        contacts.filter { favorites.contains(it.phoneNumber) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshApps()
        refreshContacts()
    }

    fun refreshApps() {
        viewModelScope.launch {
            try {
                _allApps.value = appRepository.getInstalledApps()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshContacts() {
        viewModelScope.launch {
            try {
                _allContacts.value = contactRepository.getContacts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addFavoriteApp(packageName: String) {
        viewModelScope.launch {
            val current = preferencesRepo.favoriteAppsFlow.first().toMutableSet()
            current.add(packageName)
            preferencesRepo.saveFavoriteApps(current)
        }
    }

    fun removeFavoriteApp(packageName: String) {
        viewModelScope.launch {
            val current = preferencesRepo.favoriteAppsFlow.first().toMutableSet()
            current.remove(packageName)
            preferencesRepo.saveFavoriteApps(current)
        }
    }

    fun addFavoriteContact(phoneNumber: String) {
        viewModelScope.launch {
            val current = preferencesRepo.favoriteContactsFlow.first().toMutableSet()
            current.add(phoneNumber)
            preferencesRepo.saveFavoriteContacts(current)
        }
    }

    fun removeFavoriteContact(phoneNumber: String) {
        viewModelScope.launch {
            val current = preferencesRepo.favoriteContactsFlow.first().toMutableSet()
            current.remove(phoneNumber)
            preferencesRepo.saveFavoriteContacts(current)
        }
    }
}

package xyz.arjunsinh.elderlauncher.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class FavoriteContact(
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
) {
    val key: String get() = if (phoneNumber.isNotEmpty()) "$name#$phoneNumber" else "$name#${hashCode()}"
}

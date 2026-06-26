package xyz.arjunsinh.elderlauncher.data.repository

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.arjunsinh.elderlauncher.data.model.FavoriteContact

class ContactRepository(private val context: Context) {

    suspend fun getContacts(): List<FavoriteContact> = withContext(Dispatchers.IO) {
        val contactList = mutableListOf<FavoriteContact>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex) ?: ""
                    val number = cursor.getString(numberIndex) ?: ""
                    val photoUri = cursor.getString(photoIndex)
                    
                    contactList.add(FavoriteContact(name, number, photoUri))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Deduplicate contacts by normalized phone number to handle formatting variations (spaces, dashes, etc.)
        val deduplicatedList = mutableListOf<FavoriteContact>()
        val seenNumbers = mutableSetOf<String>()
        for (contact in contactList) {
            val normalized = normalizePhoneNumber(contact.phoneNumber)
            if (seenNumbers.add(normalized)) {
                deduplicatedList.add(contact)
            }
        }

        deduplicatedList.sortedBy { it.name.lowercase() }
    }

    private fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "")
    }
}

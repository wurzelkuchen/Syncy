package com.example.data.dav

data class DiscoveredCalendar(
    val url: String,
    val displayName: String,
    val color: String = "#0288D1"
)

data class DiscoveredAddressBook(
    val url: String,
    val displayName: String
)

data class DavHomeSets(
    val calendarHomeSets: List<String> = emptyList(),
    val addressBookHomeSets: List<String> = emptyList(),
    val principalUrls: List<String> = emptyList()
)

data class ParsedIcsEvent(
    val uid: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String = "",
    val description: String = ""
)

data class ParsedVCardContact(
    val uid: String,
    val fullName: String,
    val phoneNumber: String = "",
    val email: String = "",
    val organization: String = "",
    val notes: String = ""
)

sealed class DavResult<out T> {
    data class Success<out T>(val data: T) : DavResult<T>()
    data class Error(val message: String, val code: Int? = null, val cause: Throwable? = null) : DavResult<Nothing>()
}

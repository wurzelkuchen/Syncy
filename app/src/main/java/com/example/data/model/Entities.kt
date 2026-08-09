package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: Int = 1,
    val serverUrl: String,
    val username: String,
    val encryptedPassword: String,
    val syncIntervalMinutes: Long = 60,
    val autoSyncEnabled: Boolean = true,
    val syncWifiOnly: Boolean = false,
    val syncChargingOnly: Boolean = false,
    val lastSyncTimestamp: Long = 0,
    val lastSyncStatus: String = "NEVER" // NEVER, SUCCESS, ERROR, WARNING
)

@Entity(tableName = "calendars")
data class CalendarEntity(
    @PrimaryKey val id: String, // Calendar URL or unique path
    val accountId: Int = 1,
    val displayName: String,
    val customName: String = "",
    val color: String = "#0288D1",
    val url: String,
    val syncEnabled: Boolean = true,
    val eventCount: Int = 0,
    val lastSyncTimestamp: Long = 0
) {
    val effectiveName: String get() = customName.ifBlank { displayName }
}

@Entity(tableName = "address_books")
data class AddressBookEntity(
    @PrimaryKey val id: String, // Address book URL
    val accountId: Int = 1,
    val displayName: String,
    val customName: String = "",
    val url: String,
    val syncEnabled: Boolean = true,
    val contactCount: Int = 0,
    val lastSyncTimestamp: Long = 0
) {
    val effectiveName: String get() = customName.ifBlank { displayName }
}

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calendarId: String,
    val uid: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String = "",
    val description: String = "",
    val syncTag: String = ""
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val addressBookId: String,
    val uid: String,
    val fullName: String,
    val phoneNumber: String = "",
    val email: String = "",
    val organization: String = "",
    val notes: String = ""
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val syncType: String, // "MANUAL", "SCHEDULED", "TEST"
    val status: String, // "SUCCESS", "ERROR", "WARNING"
    val calendarsSynced: Int = 0,
    val contactsSynced: Int = 0,
    val durationMs: Long = 0,
    val summary: String,
    val detailedTrace: String
)

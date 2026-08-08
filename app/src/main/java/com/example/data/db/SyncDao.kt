package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.AccountEntity
import com.example.data.model.AddressBookEntity
import com.example.data.model.CalendarEntity
import com.example.data.model.CalendarEventEntity
import com.example.data.model.ContactEntity
import com.example.data.model.SyncLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE id = 1 LIMIT 1")
    fun getAccountFlow(): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = 1 LIMIT 1")
    suspend fun getAccount(): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun deleteAccount()
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendars ORDER BY displayName ASC")
    fun getCalendarsFlow(): Flow<List<CalendarEntity>>

    @Query("SELECT * FROM calendars")
    suspend fun getCalendars(): List<CalendarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendars(calendars: List<CalendarEntity>)

    @Query("DELETE FROM calendars")
    suspend fun clearCalendars()
}

@Dao
interface AddressBookDao {
    @Query("SELECT * FROM address_books ORDER BY displayName ASC")
    fun getAddressBooksFlow(): Flow<List<AddressBookEntity>>

    @Query("SELECT * FROM address_books")
    suspend fun getAddressBooks(): List<AddressBookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddressBooks(addressBooks: List<AddressBookEntity>)

    @Query("DELETE FROM address_books")
    suspend fun clearAddressBooks()
}

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    fun getAllEventsFlow(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE calendarId = :calendarId ORDER BY startTime ASC")
    fun getEventsByCalendarFlow(calendarId: String): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEventEntity>)

    @Query("DELETE FROM calendar_events WHERE calendarId = :calendarId")
    suspend fun deleteEventsByCalendar(calendarId: String)

    @Query("DELETE FROM calendar_events")
    suspend fun clearEvents()

    @Transaction
    suspend fun replaceCalendarEvents(calendarId: String, events: List<CalendarEventEntity>) {
        deleteEventsByCalendar(calendarId)
        insertEvents(events)
    }
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY fullName ASC")
    fun getAllContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE addressBookId = :addressBookId ORDER BY fullName ASC")
    fun getContactsByAddressBookFlow(addressBookId: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("DELETE FROM contacts WHERE addressBookId = :addressBookId")
    suspend fun deleteContactsByAddressBook(addressBookId: String)

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()

    @Transaction
    suspend fun replaceAddressBookContacts(addressBookId: String, contacts: List<ContactEntity>) {
        deleteContactsByAddressBook(addressBookId)
        insertContacts(contacts)
    }
}

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC")
    fun getLogsFlow(): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM sync_logs WHERE status = :status ORDER BY timestamp DESC")
    fun getLogsByStatusFlow(status: String): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM sync_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): SyncLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SyncLogEntity): Long

    @Query("DELETE FROM sync_logs")
    suspend fun clearLogs()
}

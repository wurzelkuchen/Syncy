package com.example.data.repository

import android.content.Context
import com.example.data.dav.DavClient
import com.example.data.dav.DavResult
import com.example.data.db.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AddressBookEntity
import com.example.data.model.CalendarEntity
import com.example.data.model.CalendarEventEntity
import com.example.data.model.ContactEntity
import com.example.data.model.SyncLogEntity
import com.example.data.security.SecureStorage
import com.example.data.system.SystemCalendarSync
import com.example.data.system.SystemContactsSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val accountDao = db.accountDao()
    private val calendarDao = db.calendarDao()
    private val addressBookDao = db.addressBookDao()
    private val calendarEventDao = db.calendarEventDao()
    private val contactDao = db.contactDao()
    private val syncLogDao = db.syncLogDao()

    private val secureStorage = SecureStorage(context)
    private val davClient = DavClient()

    val accountFlow: Flow<AccountEntity?> = accountDao.getAccountFlow()
    val calendarsFlow: Flow<List<CalendarEntity>> = calendarDao.getCalendarsFlow()
    val addressBooksFlow: Flow<List<AddressBookEntity>> = addressBookDao.getAddressBooksFlow()
    val eventsFlow: Flow<List<CalendarEventEntity>> = calendarEventDao.getAllEventsFlow()
    val contactsFlow: Flow<List<ContactEntity>> = contactDao.getAllContactsFlow()
    val logsFlow: Flow<List<SyncLogEntity>> = syncLogDao.getLogsFlow()

    fun getLogsByStatusFlow(status: String): Flow<List<SyncLogEntity>> = syncLogDao.getLogsByStatusFlow(status)

    suspend fun getLogById(id: Long): SyncLogEntity? = syncLogDao.getLogById(id)

    suspend fun clearLogs() = syncLogDao.clearLogs()

    suspend fun saveAccountConfig(
        serverUrl: String,
        username: String,
        password: String,
        syncIntervalMinutes: Long,
        autoSyncEnabled: Boolean,
        syncWifiOnly: Boolean,
        syncChargingOnly: Boolean
    ) = withContext(Dispatchers.IO) {
        secureStorage.savePassword(password)
        val existing = accountDao.getAccount()
        val updated = AccountEntity(
            id = 1,
            serverUrl = serverUrl.trim(),
            username = username.trim(),
            encryptedPassword = "ENCRYPTED_KEYSTORE",
            syncIntervalMinutes = syncIntervalMinutes,
            autoSyncEnabled = autoSyncEnabled,
            syncWifiOnly = syncWifiOnly,
            syncChargingOnly = syncChargingOnly,
            lastSyncTimestamp = existing?.lastSyncTimestamp ?: 0,
            lastSyncStatus = existing?.lastSyncStatus ?: "NEVER"
        )
        accountDao.saveAccount(updated)
    }

    suspend fun testConnection(serverUrl: String, username: String, password: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val trace = StringBuilder()
            val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            trace.appendLine("=== TEST CONNECTION RUN [${timeFmt.format(Date())}] ===")
            val result = davClient.testConnection(serverUrl, username, password, trace)
            val isSuccess = result is DavResult.Success
            Pair(isSuccess, trace.toString())
        }

    suspend fun performSync(triggerType: String = "MANUAL"): SyncLogEntity = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val trace = StringBuilder()
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        trace.appendLine("=== SYNC RUN LOG [$triggerType] - ${timeFmt.format(Date(startTime))} ===")

        val account = accountDao.getAccount()
        if (account == null || account.serverUrl.isBlank() || account.username.isBlank()) {
            val errLog = SyncLogEntity(
                timestamp = startTime,
                syncType = triggerType,
                status = "ERROR",
                durationMs = System.currentTimeMillis() - startTime,
                summary = "Sync failed: Account not configured.",
                detailedTrace = trace.appendLine("✖ Error: Missing server URL or username.").toString()
            )
            syncLogDao.insertLog(errLog)
            return@withContext errLog
        }

        val password = secureStorage.getPassword()
        if (password.isBlank()) {
            val errLog = SyncLogEntity(
                timestamp = startTime,
                syncType = triggerType,
                status = "ERROR",
                durationMs = System.currentTimeMillis() - startTime,
                summary = "Sync failed: Credentials missing in SecureStorage.",
                detailedTrace = trace.appendLine("✖ Error: Password unavailable in Android KeyStore.").toString()
            )
            syncLogDao.insertLog(errLog)
            return@withContext errLog
        }

        trace.appendLine("➜ Account: ${account.username} @ ${account.serverUrl}")
        trace.appendLine("➜ Security: Password decrypted from Android KeyStore.")

        var hasErrors = false
        var hasWarnings = false
        var calendarsCount = 0
        var contactsCount = 0
        var totalEventsParsed = 0
        var totalContactsParsed = 0

        // Read existing settings to preserve user custom names and syncEnabled states
        val existingCalsMap = calendarDao.getCalendars().associateBy { it.id }
        val existingBooksMap = addressBookDao.getAddressBooks().associateBy { it.id }

        // 1. Discover Calendars
        trace.appendLine("\n--- STEP 1: DISCOVER CALENDARS ---")
        val discoveredCalendars = davClient.discoverCalendars(account.serverUrl, account.username, password, trace)
        calendarsCount = discoveredCalendars.size

        if (discoveredCalendars.isEmpty()) {
            trace.appendLine("⚠ Warning: No calendars discovered directly via DAV PROPFIND.")
            hasWarnings = true
        } else {
            val calEntities = discoveredCalendars.map {
                val prev = existingCalsMap[it.url]
                CalendarEntity(
                    id = it.url,
                    displayName = it.displayName,
                    customName = prev?.customName ?: "",
                    color = it.color,
                    url = it.url,
                    syncEnabled = prev?.syncEnabled ?: true,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            }
            calendarDao.insertCalendars(calEntities)
        }

        // 2. Discover Address Books
        trace.appendLine("\n--- STEP 2: DISCOVER ADDRESS BOOKS ---")
        val discoveredAddressBooks = davClient.discoverAddressBooks(account.serverUrl, account.username, password, trace)
        contactsCount = discoveredAddressBooks.size

        if (discoveredAddressBooks.isEmpty()) {
            trace.appendLine("⚠ Warning: No address books discovered via CardDAV.")
            hasWarnings = true
        } else {
            val bookEntities = discoveredAddressBooks.map {
                val prev = existingBooksMap[it.url]
                AddressBookEntity(
                    id = it.url,
                    displayName = it.displayName,
                    customName = prev?.customName ?: "",
                    url = it.url,
                    syncEnabled = prev?.syncEnabled ?: true,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            }
            addressBookDao.insertAddressBooks(bookEntities)
        }

        // 3. Fetch Events for each discovered Calendar
        trace.appendLine("\n--- STEP 3: SYNC CALENDAR EVENTS ---")
        val allStoredCalendars = calendarDao.getCalendars().associateBy { it.id }
        for (cal in discoveredCalendars) {
            val calEntity = allStoredCalendars[cal.url] ?: CalendarEntity(
                id = cal.url,
                displayName = cal.displayName,
                color = cal.color,
                url = cal.url
            )

            if (!calEntity.syncEnabled) {
                trace.appendLine("  └ [Skipped] '${calEntity.effectiveName}' sync is disabled by user.")
                SystemCalendarSync.removeSystemCalendar(appContext, account.username, cal.url)
                continue
            }

            val fetchRes = davClient.fetchCalendarEvents(cal.url, account.username, password, trace)
            if (fetchRes.error != null) {
                hasWarnings = true
                trace.appendLine("  └ Warning on ${calEntity.effectiveName}: ${fetchRes.error}")
            }
            val eventEntities = fetchRes.events.map {
                CalendarEventEntity(
                    calendarId = cal.url,
                    uid = it.uid,
                    title = it.title,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    location = it.location,
                    description = it.description
                )
            }
            totalEventsParsed += eventEntities.size
            calendarEventDao.replaceCalendarEvents(cal.url, eventEntities)
            val updatedCalEntity = calEntity.copy(
                eventCount = eventEntities.size,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            calendarDao.insertCalendars(listOf(updatedCalEntity))

            // Sync to Android System Calendar Provider
            SystemCalendarSync.syncCalendarToSystem(
                context = appContext,
                accountUsername = account.username,
                calendar = updatedCalEntity,
                events = eventEntities,
                traceBuilder = trace
            )
        }

        // 4. Fetch Contacts for each Address Book
        trace.appendLine("\n--- STEP 4: SYNC CONTACTS ---")
        val allStoredBooks = addressBookDao.getAddressBooks().associateBy { it.id }
        for (book in discoveredAddressBooks) {
            val bookEntity = allStoredBooks[book.url] ?: AddressBookEntity(
                id = book.url,
                displayName = book.displayName,
                url = book.url
            )

            if (!bookEntity.syncEnabled) {
                trace.appendLine("  └ [Skipped] '${bookEntity.effectiveName}' sync is disabled by user.")
                SystemContactsSync.removeSystemContacts(appContext, account.username)
                continue
            }

            val fetchRes = davClient.fetchAddressBookContacts(book.url, account.username, password, trace)
            if (fetchRes.error != null) {
                hasWarnings = true
                trace.appendLine("  └ Warning on ${bookEntity.effectiveName}: ${fetchRes.error}")
            }
            val contactEntities = fetchRes.contacts.map {
                ContactEntity(
                    addressBookId = book.url,
                    uid = it.uid,
                    fullName = it.fullName,
                    phoneNumber = it.phoneNumber,
                    email = it.email,
                    organization = it.organization,
                    notes = it.notes
                )
            }
            totalContactsParsed += contactEntities.size
            contactDao.replaceAddressBookContacts(book.url, contactEntities)
            val updatedBookEntity = bookEntity.copy(
                contactCount = contactEntities.size,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            addressBookDao.insertAddressBooks(listOf(updatedBookEntity))

            // Sync to Android System Contacts Provider
            SystemContactsSync.syncContactsToSystem(
                context = appContext,
                accountUsername = account.username,
                addressBook = updatedBookEntity,
                contacts = contactEntities,
                traceBuilder = trace
            )
        }

        val duration = System.currentTimeMillis() - startTime
        trace.appendLine("\n=== SYNC SUMMARY ===")
        trace.appendLine("Duration: ${duration}ms")
        trace.appendLine("Calendars: $calendarsCount found ($totalEventsParsed events stored)")
        trace.appendLine("Address Books: $contactsCount found ($totalContactsParsed contacts stored)")

        val status = when {
            hasErrors -> "ERROR"
            hasWarnings && (totalEventsParsed == 0 && totalContactsParsed == 0) -> "WARNING"
            else -> "SUCCESS"
        }

        val summaryMsg = if (status == "SUCCESS") {
            "Synced $calendarsCount calendar(s) ($totalEventsParsed events) and $contactsCount contact list(s) ($totalContactsParsed contacts)."
        } else if (status == "WARNING") {
            "Sync completed with warnings. $totalEventsParsed events and $totalContactsParsed contacts retrieved."
        } else {
            "Sync encountered errors. Check debug trace logs."
        }

        val logEntity = SyncLogEntity(
            timestamp = startTime,
            syncType = triggerType,
            status = status,
            calendarsSynced = calendarsCount,
            contactsSynced = contactsCount,
            durationMs = duration,
            summary = summaryMsg,
            detailedTrace = trace.toString()
        )

        syncLogDao.insertLog(logEntity)

        // Update account last sync info
        val updatedAccount = account.copy(
            lastSyncTimestamp = startTime,
            lastSyncStatus = status
        )
        accountDao.saveAccount(updatedAccount)

        logEntity
    }

    suspend fun updateCalendarSyncEnabled(calendarId: String, enabled: Boolean) {
        calendarDao.setCalendarSyncEnabled(calendarId, enabled)
        val account = accountDao.getAccount()
        if (account != null && !enabled) {
            SystemCalendarSync.removeSystemCalendar(appContext, account.username, calendarId)
        }
    }

    suspend fun updateCalendarCustomName(calendarId: String, customName: String) {
        calendarDao.setCalendarCustomName(calendarId, customName)
        val account = accountDao.getAccount()
        val cals = calendarDao.getCalendars()
        val cal = cals.find { it.id == calendarId }
        if (account != null && cal != null && cal.syncEnabled) {
            // Re-sync metadata to system calendar provider
            SystemCalendarSync.syncCalendarToSystem(
                context = appContext,
                accountUsername = account.username,
                calendar = cal,
                events = emptyList()
            )
        }
    }

    suspend fun updateAddressBookSyncEnabled(addressBookId: String, enabled: Boolean) {
        addressBookDao.setAddressBookSyncEnabled(addressBookId, enabled)
        val account = accountDao.getAccount()
        if (account != null && !enabled) {
            SystemContactsSync.removeSystemContacts(appContext, account.username)
        }
    }

    suspend fun updateAddressBookCustomName(addressBookId: String, customName: String) {
        addressBookDao.setAddressBookCustomName(addressBookId, customName)
    }
}

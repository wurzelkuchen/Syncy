package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountEntity
import com.example.data.model.AddressBookEntity
import com.example.data.model.CalendarEntity
import com.example.data.model.CalendarEventEntity
import com.example.data.model.ContactEntity
import com.example.data.model.SyncLogEntity
import com.example.data.repository.SyncRepository
import com.example.worker.SyncWorkManagerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TestConnectionState {
    object Idle : TestConnectionState()
    object Loading : TestConnectionState()
    data class Success(val trace: String) : TestConnectionState()
    data class Error(val message: String, val trace: String) : TestConnectionState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SyncRepository(application)

    val accountState: StateFlow<AccountEntity?> = repository.accountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val calendarsState: StateFlow<List<CalendarEntity>> = repository.calendarsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val addressBooksState: StateFlow<List<AddressBookEntity>> = repository.addressBooksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eventsState: StateFlow<List<CalendarEventEntity>> = repository.eventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contactsState: StateFlow<List<ContactEntity>> = repository.contactsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logsState: StateFlow<List<SyncLogEntity>> = repository.logsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _testState = MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val testState: StateFlow<TestConnectionState> = _testState.asStateFlow()

    private val _selectedLog = MutableStateFlow<SyncLogEntity?>(null)
    val selectedLog: StateFlow<SyncLogEntity?> = _selectedLog.asStateFlow()

    private val _logFilter = MutableStateFlow("ALL") // ALL, SUCCESS, ERROR
    val logFilter: StateFlow<String> = _logFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLogFilter(filter: String) {
        _logFilter.value = filter
    }

    fun selectLog(log: SyncLogEntity?) {
        _selectedLog.value = log
    }

    fun triggerManualSync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.performSync("MANUAL")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun testConnection(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _testState.value = TestConnectionState.Loading
            val (success, trace) = repository.testConnection(serverUrl, username, password)
            if (success) {
                _testState.value = TestConnectionState.Success(trace)
            } else {
                _testState.value = TestConnectionState.Error("Connection test failed", trace)
            }
        }
    }

    fun resetTestState() {
        _testState.value = TestConnectionState.Idle
    }

    fun saveAccountAndSchedule(
        serverUrl: String,
        username: String,
        password: String,
        syncIntervalMinutes: Long,
        autoSyncEnabled: Boolean,
        syncWifiOnly: Boolean,
        syncChargingOnly: Boolean
    ) {
        viewModelScope.launch {
            repository.saveAccountConfig(
                serverUrl = serverUrl,
                username = username,
                password = password,
                syncIntervalMinutes = syncIntervalMinutes,
                autoSyncEnabled = autoSyncEnabled,
                syncWifiOnly = syncWifiOnly,
                syncChargingOnly = syncChargingOnly
            )

            // Update WorkManager
            SyncWorkManagerHelper.schedulePeriodicSync(
                context = getApplication(),
                intervalMinutes = syncIntervalMinutes,
                autoSyncEnabled = autoSyncEnabled,
                wifiOnly = syncWifiOnly,
                chargingOnly = syncChargingOnly
            )
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}

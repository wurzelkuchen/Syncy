package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.DataViewerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.theme.OwnCloudBlue
import com.example.ui.theme.OwnCloudSyncTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OwnCloudSyncTheme {
                val account by viewModel.accountState.collectAsStateWithLifecycle()
                val calendars by viewModel.calendarsState.collectAsStateWithLifecycle()
                val addressBooks by viewModel.addressBooksState.collectAsStateWithLifecycle()
                val events by viewModel.eventsState.collectAsStateWithLifecycle()
                val contacts by viewModel.contactsState.collectAsStateWithLifecycle()
                val logs by viewModel.logsState.collectAsStateWithLifecycle()
                val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
                val testState by viewModel.testState.collectAsStateWithLifecycle()
                val selectedLog by viewModel.selectedLog.collectAsStateWithLifecycle()
                val logFilter by viewModel.logFilter.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("main_bottom_navigation")
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = OwnCloudBlue),
                                modifier = Modifier.testTag("tab_dashboard")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.FolderShared, contentDescription = "Synced Data") },
                                label = { Text("Synced Data") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = OwnCloudBlue),
                                modifier = Modifier.testTag("tab_data")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.BugReport, contentDescription = "Sync Logs") },
                                label = { Text("Debug Logs") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = OwnCloudBlue),
                                modifier = Modifier.testTag("tab_logs")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Account") },
                                label = { Text("Account") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = OwnCloudBlue),
                                modifier = Modifier.testTag("tab_account")
                            )
                        }
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> DashboardScreen(
                                account = account,
                                calendarsCount = calendars.size,
                                eventsCount = events.size,
                                addressBooksCount = addressBooks.size,
                                contactsCount = contacts.size,
                                latestLog = logs.firstOrNull(),
                                isSyncing = isSyncing,
                                onSyncClick = { viewModel.triggerManualSync() },
                                onNavigateToAccount = { selectedTab = 3 },
                                onNavigateToLogs = { selectedTab = 2 },
                                onNavigateToData = { selectedTab = 1 }
                            )

                            1 -> DataViewerScreen(
                                calendars = calendars,
                                addressBooks = addressBooks,
                                events = events,
                                contacts = contacts,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) }
                            )

                            2 -> LogsScreen(
                                logs = logs,
                                filter = logFilter,
                                selectedLog = selectedLog,
                                onFilterChange = { viewModel.setLogFilter(it) },
                                onSelectLog = { viewModel.selectLog(it) },
                                onClearLogs = { viewModel.clearAllLogs() }
                            )

                            3 -> AccountScreen(
                                account = account,
                                testState = testState,
                                onTestConnection = { url, user, pass ->
                                    viewModel.testConnection(url, user, pass)
                                },
                                onResetTestState = { viewModel.resetTestState() },
                                onSaveConfig = { url, user, pass, interval, autoSync, wifiOnly, chargingOnly ->
                                    viewModel.saveAccountAndSchedule(
                                        serverUrl = url,
                                        username = user,
                                        password = pass,
                                        syncIntervalMinutes = interval,
                                        autoSyncEnabled = autoSync,
                                        syncWifiOnly = wifiOnly,
                                        syncChargingOnly = chargingOnly
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

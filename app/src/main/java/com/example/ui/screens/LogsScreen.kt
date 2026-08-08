package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncLogEntity
import com.example.ui.theme.OwnCloudBlue
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.StatusSuccessGreen
import com.example.ui.theme.StatusWarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    logs: List<SyncLogEntity>,
    filter: String,
    selectedLog: SyncLogEntity?,
    onFilterChange: (String) -> Unit,
    onSelectLog: (SyncLogEntity?) -> Unit,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    val dateFmt = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

    val filteredLogs = when (filter) {
        "SUCCESS" -> logs.filter { it.status == "SUCCESS" }
        "ERROR" -> logs.filter { it.status == "ERROR" || it.status == "WARNING" }
        else -> logs
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header & Clear Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sync History & Debug Logs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Individual logs with detailed DAV error trace output",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }

            if (logs.isNotEmpty()) {
                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear All Logs",
                        tint = StatusErrorRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = filter == "ALL",
                onClick = { onFilterChange("ALL") },
                label = { Text("All (${logs.size})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OwnCloudBlue.copy(alpha = 0.2f)),
                modifier = Modifier.testTag("filter_all_logs")
            )
            FilterChip(
                selected = filter == "SUCCESS",
                onClick = { onFilterChange("SUCCESS") },
                label = { Text("Success (${logs.count { it.status == "SUCCESS" }})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StatusSuccessGreen.copy(alpha = 0.2f)),
                modifier = Modifier.testTag("filter_success_logs")
            )
            FilterChip(
                selected = filter == "ERROR",
                onClick = { onFilterChange("ERROR") },
                label = { Text("Errors (${logs.count { it.status != "SUCCESS" }})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StatusErrorRed.copy(alpha = 0.2f)),
                modifier = Modifier.testTag("filter_error_logs")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "No Logs",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No sync history logs found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Execute manual or scheduled syncs to populate debug history.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredLogs) { log ->
                    LogCard(
                        log = log,
                        dateFmt = dateFmt,
                        onClick = { onSelectLog(log) }
                    )
                }
            }
        }
    }

    // Detail Dialog for Log Inspection
    if (selectedLog != null) {
        LogDetailDialog(
            log = selectedLog,
            dateFmt = dateFmt,
            onDismiss = { onSelectLog(null) },
            onCopyTrace = { traceText ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("ownCloud Sync Debug Trace", traceText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Debug log copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun LogCard(
    log: SyncLogEntity,
    dateFmt: SimpleDateFormat,
    onClick: () -> Unit
) {
    val (badgeBg, badgeText, badgeIcon) = when (log.status) {
        "SUCCESS" -> Triple(StatusSuccessGreen, "SUCCESS", Icons.Default.CheckCircle)
        "ERROR" -> Triple(StatusErrorRed, "ERROR", Icons.Default.Error)
        else -> Triple(StatusWarningAmber, "WARNING", Icons.Default.Warning)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("sync_log_item_${log.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Debug Log",
                        tint = badgeBg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.syncType + " SYNC",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeBg)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = badgeText,
                            tint = badgeBg,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeText,
                            color = badgeBg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = log.summary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFmt.format(Date(log.timestamp)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Duration: ${log.durationMs}ms",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun LogDetailDialog(
    log: SyncLogEntity,
    dateFmt: SimpleDateFormat,
    onDismiss: () -> Unit,
    onCopyTrace: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sync Trace & Debug Log", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Status: ${log.status}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Trigger: ${log.syncType}", fontSize = 12.sp)
                }

                Text("Timestamp: ${dateFmt.format(Date(log.timestamp))}", fontSize = 12.sp)
                Text("Duration: ${log.durationMs}ms", fontSize = 12.sp)
                Text("Summary: ${log.summary}", fontSize = 12.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(6.dp))
                Text("Step-by-Step HTTP & DAV Trace:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = log.detailedTrace,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCopyTrace(log.detailedTrace) },
                colors = ButtonDefaults.buttonColors(containerColor = OwnCloudBlue),
                modifier = Modifier.testTag("copy_log_trace_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Log", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Trace")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

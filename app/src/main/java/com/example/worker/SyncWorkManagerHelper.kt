package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncWorkManagerHelper {

    private const val WORK_NAME = "owncloud_sync_periodic_work"

    fun schedulePeriodicSync(
        context: Context,
        intervalMinutes: Long,
        autoSyncEnabled: Boolean,
        wifiOnly: Boolean,
        chargingOnly: Boolean
    ) {
        val workManager = WorkManager.getInstance(context.applicationContext)

        if (!autoSyncEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val safeInterval = intervalMinutes.coerceAtLeast(15) // Minimum WorkManager interval is 15 minutes

        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

        val constraintsBuilder = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true) // Battery optimization strategy

        if (chargingOnly) {
            constraintsBuilder.setRequiresCharging(true)
        }

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            safeInterval, TimeUnit.MINUTES
        )
        .setConstraints(constraintsBuilder.build())
        .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncWorkRequest
        )
    }

    fun cancelPeriodicSync(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.cancelUniqueWork(WORK_NAME)
    }
}

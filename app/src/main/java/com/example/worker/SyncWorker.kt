package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.repository.SyncRepository

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = SyncRepository(applicationContext)
            val result = repository.performSync("SCHEDULED")
            if (result.status == "ERROR") {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

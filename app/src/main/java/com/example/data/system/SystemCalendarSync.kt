package com.example.data.system

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.data.model.CalendarEntity
import com.example.data.model.CalendarEventEntity
import java.util.TimeZone

object SystemCalendarSync {

    fun hasCalendarPermissions(context: Context): Boolean {
        val read = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    fun syncCalendarToSystem(
        context: Context,
        accountUsername: String,
        calendar: CalendarEntity,
        events: List<CalendarEventEntity>,
        traceBuilder: StringBuilder? = null
    ): Boolean {
        if (!hasCalendarPermissions(context)) {
            traceBuilder?.appendLine("  └ [SystemCalendar] Skipping system calendar export: READ/WRITE_CALENDAR permissions not granted.")
            return false
        }

        val contentResolver = context.contentResolver

        return try {
            val systemCalId = getOrCreateSystemCalendar(context, accountUsername, calendar)
            if (systemCalId <= 0) {
                traceBuilder?.appendLine("  └ [SystemCalendar] Could not create or find system calendar ID for ${calendar.displayName}")
                return false
            }

            // Remove existing system events for this calendar to ensure clean state
            val deleteUri = CalendarContract.Events.CONTENT_URI
            val deletedCount = contentResolver.delete(
                deleteUri,
                "${CalendarContract.Events.CALENDAR_ID} = ?",
                arrayOf(systemCalId.toString())
            )
            traceBuilder?.appendLine("  └ [SystemCalendar] Cleared $deletedCount old events from system calendar ID $systemCalId")

            var insertedEvents = 0
            val timeZone = TimeZone.getDefault().id

            for (event in events) {
                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, systemCalId)
                    put(CalendarContract.Events.TITLE, event.title.ifBlank { "Untitled Event" })
                    put(CalendarContract.Events.DESCRIPTION, event.description)
                    put(CalendarContract.Events.EVENT_LOCATION, event.location)
                    put(CalendarContract.Events.DTSTART, event.startTime)
                    val end = if (event.endTime > event.startTime) event.endTime else event.startTime + 3600000
                    put(CalendarContract.Events.DTEND, end)
                    put(CalendarContract.Events.EVENT_TIMEZONE, timeZone)
                    put(CalendarContract.Events._SYNC_ID, event.uid)
                }

                val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                if (uri != null) {
                    insertedEvents++
                }
            }

            traceBuilder?.appendLine("  └ [SystemCalendar] Successfully exported $insertedEvents event(s) to system calendar '${calendar.displayName}'")
            true
        } catch (e: Exception) {
            traceBuilder?.appendLine("  └ [SystemCalendar] Error syncing to system calendar: ${e.message}")
            false
        }
    }

    private fun getOrCreateSystemCalendar(
        context: Context,
        accountUsername: String,
        calendar: CalendarEntity
    ): Long {
        val contentResolver = context.contentResolver
        val accountName = accountUsername.ifBlank { "ownCloud" }

        // Query existing calendar
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.NAME)
        val selection = "(${CalendarContract.Calendars.ACCOUNT_NAME} = ?) AND (${CalendarContract.Calendars.NAME} = ?)"
        val selectionArgs = arrayOf(accountName, calendar.url)

        val calendarUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()

        contentResolver.query(calendarUri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }

        // Insert new system calendar
        val colorInt = try {
            if (calendar.color.isNotBlank()) Color.parseColor(calendar.color) else Color.parseColor("#1D5288")
        } catch (_: Exception) {
            Color.parseColor("#1D5288")
        }

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, calendar.url)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calendar.displayName.ifBlank { "ownCloud Calendar" })
            put(CalendarContract.Calendars.CALENDAR_COLOR, colorInt)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        }

        val uri = contentResolver.insert(calendarUri, values)
        return uri?.lastPathSegment?.toLongOrNull() ?: -1L
    }
}

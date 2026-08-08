package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AddressBookEntity
import com.example.data.model.CalendarEntity
import com.example.data.model.CalendarEventEntity
import com.example.data.model.ContactEntity
import com.example.data.model.SyncLogEntity

@Database(
    entities = [
        AccountEntity::class,
        CalendarEntity::class,
        AddressBookEntity::class,
        CalendarEventEntity::class,
        ContactEntity::class,
        SyncLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun calendarDao(): CalendarDao
    abstract fun addressBookDao(): AddressBookDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun contactDao(): ContactDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "owncloud_sync_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.pepperonas.netmonitor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pepperonas.netmonitor.data.dao.DailyTrafficDao
import com.pepperonas.netmonitor.data.dao.SpeedSampleDao
import com.pepperonas.netmonitor.data.dao.SpeedTestDao
import com.pepperonas.netmonitor.data.entity.DailyTrafficSummary
import com.pepperonas.netmonitor.data.entity.SpeedSample
import com.pepperonas.netmonitor.data.entity.SpeedTestResult

@Database(
    entities = [SpeedSample::class, DailyTrafficSummary::class, SpeedTestResult::class],
    version = 2,
    exportSchema = false
)
abstract class NetMonitorDatabase : RoomDatabase() {

    abstract fun speedSampleDao(): SpeedSampleDao
    abstract fun dailyTrafficDao(): DailyTrafficDao
    abstract fun speedTestDao(): SpeedTestDao

    companion object {
        @Volatile
        private var INSTANCE: NetMonitorDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS speed_test_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        downloadBytesPerSec INTEGER NOT NULL,
                        uploadBytesPerSec INTEGER NOT NULL,
                        latencyMs INTEGER NOT NULL,
                        connectionType TEXT NOT NULL,
                        serverUrl TEXT NOT NULL
                    )
                """)
            }
        }

        fun getInstance(context: Context): NetMonitorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetMonitorDatabase::class.java,
                    "netmonitor.db"
                ).addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}

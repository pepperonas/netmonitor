package com.pepperonas.netmonitor.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pepperonas.netmonitor.data.dao.SpeedSampleDao
import com.pepperonas.netmonitor.data.entity.SpeedSample
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpeedSampleDaoTest {

    private lateinit var database: NetMonitorDatabase
    private lateinit var dao: SpeedSampleDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NetMonitorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.speedSampleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieve() = runTest {
        val sample = SpeedSample(
            timestamp = System.currentTimeMillis(),
            rxBytesPerSec = 1024,
            txBytesPerSec = 512,
            connectionType = "wifi"
        )
        dao.insert(sample)

        val count = dao.getCount()
        assertEquals(1, count)
    }

    @Test
    fun getSamplesSince_filtersCorrectly() = runTest {
        val now = System.currentTimeMillis()
        dao.insert(SpeedSample(timestamp = now - 120_000, rxBytesPerSec = 100, txBytesPerSec = 50, connectionType = "wifi"))
        dao.insert(SpeedSample(timestamp = now - 30_000, rxBytesPerSec = 200, txBytesPerSec = 100, connectionType = "wifi"))
        dao.insert(SpeedSample(timestamp = now, rxBytesPerSec = 300, txBytesPerSec = 150, connectionType = "wifi"))

        val since60s = dao.getSamplesSince(now - 60_000).first()
        assertEquals(2, since60s.size)
        assertEquals(200L, since60s[0].rxBytesPerSec)
    }

    @Test
    fun deleteOlderThan_removesOldEntries() = runTest {
        val now = System.currentTimeMillis()
        dao.insert(SpeedSample(timestamp = now - 100_000, rxBytesPerSec = 100, txBytesPerSec = 50, connectionType = "wifi"))
        dao.insert(SpeedSample(timestamp = now, rxBytesPerSec = 200, txBytesPerSec = 100, connectionType = "wifi"))

        dao.deleteOlderThan(now - 50_000)
        assertEquals(1, dao.getCount())
    }

    @Test
    fun getPeakDownload_returnsMax() = runTest {
        val now = System.currentTimeMillis()
        dao.insert(SpeedSample(timestamp = now, rxBytesPerSec = 100, txBytesPerSec = 50, connectionType = "wifi"))
        dao.insert(SpeedSample(timestamp = now, rxBytesPerSec = 500, txBytesPerSec = 200, connectionType = "wifi"))
        dao.insert(SpeedSample(timestamp = now, rxBytesPerSec = 300, txBytesPerSec = 800, connectionType = "wifi"))

        assertEquals(500L, dao.getPeakDownloadSince(now - 60_000))
        assertEquals(800L, dao.getPeakUploadSince(now - 60_000))
    }

    @Test
    fun hourlyAverages_aggregatesCorrectly() = runTest {
        val now = System.currentTimeMillis()
        // Drei Samples in derselben Stunde
        dao.insert(SpeedSample(timestamp = now, rxBytesPerSec = 100, txBytesPerSec = 50, connectionType = "wifi"))
        dao.insert(SpeedSample(timestamp = now + 1000, rxBytesPerSec = 200, txBytesPerSec = 100, connectionType = "wifi"))
        dao.insert(SpeedSample(timestamp = now + 2000, rxBytesPerSec = 300, txBytesPerSec = 150, connectionType = "wifi"))

        val hourly = dao.getHourlyAverages(now - 60_000)
        assertEquals(1, hourly.size)
        assertEquals(200L, hourly[0].rxBytesPerSec) // AVG(100, 200, 300) = 200
        assertEquals(100L, hourly[0].txBytesPerSec) // AVG(50, 100, 150) = 100
    }
}

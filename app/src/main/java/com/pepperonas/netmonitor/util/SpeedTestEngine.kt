package com.pepperonas.netmonitor.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class SpeedTestEngine {

    data class Progress(
        val phase: Phase = Phase.IDLE,
        val progressPercent: Float = 0f,
        val currentSpeedBps: Long = 0
    )

    enum class Phase { IDLE, LATENCY, DOWNLOAD, UPLOAD, DONE, ERROR }

    data class Result(
        val downloadBytesPerSec: Long,
        val uploadBytesPerSec: Long,
        val latencyMs: Long,
        val serverUrl: String
    )

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress

    companion object {
        // Public test file endpoints (commonly used for speed tests)
        private val DOWNLOAD_URLS = listOf(
            "https://speed.hetzner.de/100MB.bin",
            "https://proof.ovh.net/files/100Mb.dat",
            "https://speedtest.tele2.net/10MB.zip"
        )
        private val UPLOAD_URLS = listOf(
            "https://speedtest.tele2.net/upload.php",
            "https://speed.hetzner.de/upload.php",
            "https://proof.ovh.net/files/upload.php"
        )
        // Small files for latency pings (minimal data transfer)
        private val LATENCY_URLS = listOf(
            "https://speedtest.tele2.net/1KB.zip",
            "https://speed.hetzner.de/100MB.bin",
            "https://proof.ovh.net/files/1Mb.dat"
        )
        private const val DOWNLOAD_DURATION_MS = 10_000L
        private const val UPLOAD_DURATION_MS = 8_000L
        private const val BUFFER_SIZE = 65536
        private const val LATENCY_PINGS = 5
    }

    suspend fun runTest(): Result? = withContext(Dispatchers.IO) {
        try {
            _progress.value = Progress(Phase.LATENCY, 0f)

            // 1. Latency test
            val latency = measureLatency()
            _progress.value = Progress(Phase.LATENCY, 1f)

            // 2. Download test
            _progress.value = Progress(Phase.DOWNLOAD, 0f)
            val (dlSpeed, serverUrl) = measureDownload()
            _progress.value = Progress(Phase.DOWNLOAD, 1f, dlSpeed)

            // Check if download completely failed (no server reachable)
            if (dlSpeed == 0L && latency == -1L) {
                _progress.value = Progress(Phase.ERROR, 0f)
                return@withContext null
            }

            // 3. Upload test
            _progress.value = Progress(Phase.UPLOAD, 0f)
            val ulSpeed = measureUpload()
            _progress.value = Progress(Phase.UPLOAD, 1f, ulSpeed)

            _progress.value = Progress(Phase.DONE, 1f)

            Result(
                downloadBytesPerSec = dlSpeed,
                uploadBytesPerSec = ulSpeed,
                latencyMs = latency,
                serverUrl = serverUrl
            )
        } catch (_: Exception) {
            _progress.value = Progress(Phase.ERROR, 0f)
            null
        }
    }

    fun reset() {
        _progress.value = Progress()
    }

    private fun measureLatency(): Long {
        // Find a working server first
        var workingUrl = LATENCY_URLS.first()
        for (urlStr in LATENCY_URLS) {
            var conn: HttpURLConnection? = null
            try {
                conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.setRequestProperty("Range", "bytes=0-0")
                conn.connect()
                conn.responseCode
                workingUrl = urlStr
                break
            } catch (_: Exception) {
                continue
            } finally {
                conn?.disconnect()
            }
        }

        val times = mutableListOf<Long>()
        repeat(LATENCY_PINGS) { i ->
            try {
                val c = URL(workingUrl).openConnection() as HttpURLConnection
                try {
                    c.requestMethod = "GET"
                    c.connectTimeout = 5000
                    c.readTimeout = 5000
                    c.setRequestProperty("Range", "bytes=0-0")
                    c.setRequestProperty("Cache-Control", "no-cache")

                    val start = System.nanoTime()
                    c.connect()
                    c.responseCode
                    val elapsed = (System.nanoTime() - start) / 1_000_000
                    times.add(elapsed)

                    _progress.value = Progress(
                        Phase.LATENCY,
                        (i + 1).toFloat() / LATENCY_PINGS
                    )
                } finally {
                    c.disconnect()
                }
            } catch (_: Exception) {
                // skip failed ping
            }
        }
        return if (times.isNotEmpty()) times.sorted()[times.size / 2] else -1L
    }

    private fun measureDownload(): Pair<Long, String> {
        val buffer = ByteArray(BUFFER_SIZE)

        for (urlStr in DOWNLOAD_URLS) {
            var conn: HttpURLConnection? = null
            try {
                conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 10000
                conn.setRequestProperty("Cache-Control", "no-cache")

                val startTime = System.currentTimeMillis()
                var totalBytes = 0L

                conn.inputStream.use { input ->
                    while (true) {
                        val elapsed = System.currentTimeMillis() - startTime
                        if (elapsed >= DOWNLOAD_DURATION_MS) break

                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        totalBytes += bytesRead

                        val elapsedSec = elapsed / 1000f
                        if (elapsedSec > 0) {
                            val currentSpeed = (totalBytes / elapsedSec).toLong()
                            _progress.value = Progress(
                                Phase.DOWNLOAD,
                                (elapsed.toFloat() / DOWNLOAD_DURATION_MS).coerceAtMost(1f),
                                currentSpeed
                            )
                        }
                    }
                }

                val totalElapsed = (System.currentTimeMillis() - startTime) / 1000f
                val speed = if (totalElapsed > 0) (totalBytes / totalElapsed).toLong() else 0L
                if (speed > 0) return Pair(speed, urlStr)
            } catch (_: Exception) {
                continue // Try next server
            } finally {
                conn?.disconnect()
            }
        }

        return Pair(0L, DOWNLOAD_URLS.first())
    }

    private fun measureUpload(): Long {
        val data = ByteArray(BUFFER_SIZE)

        for (urlStr in UPLOAD_URLS) {
            var conn: HttpURLConnection? = null
            try {
                conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/octet-stream")
                conn.setChunkedStreamingMode(BUFFER_SIZE)

                val startTime = System.currentTimeMillis()
                var totalBytes = 0L

                conn.outputStream.use { output ->
                    while (true) {
                        val elapsed = System.currentTimeMillis() - startTime
                        if (elapsed >= UPLOAD_DURATION_MS) break

                        output.write(data)
                        output.flush()
                        totalBytes += data.size

                        val elapsedSec = elapsed / 1000f
                        if (elapsedSec > 0) {
                            val currentSpeed = (totalBytes / elapsedSec).toLong()
                            _progress.value = Progress(
                                Phase.UPLOAD,
                                (elapsed.toFloat() / UPLOAD_DURATION_MS).coerceAtMost(1f),
                                currentSpeed
                            )
                        }
                    }
                }

                // Read response to ensure data was sent
                conn.responseCode

                val totalElapsed = (System.currentTimeMillis() - startTime) / 1000f
                val speed = if (totalElapsed > 0) (totalBytes / totalElapsed).toLong() else 0L
                if (speed > 0) return speed
            } catch (_: Exception) {
                continue // Try next server
            } finally {
                conn?.disconnect()
            }
        }
        return 0L
    }
}

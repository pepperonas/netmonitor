package com.pepperonas.netmonitor.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class SpeedTestEngine {

    data class Progress(
        val phase: Phase = Phase.IDLE,
        val progressPercent: Float = 0f,
        val currentSpeedBps: Long = 0
    )

    enum class Phase { IDLE, LATENCY, DOWNLOAD, UPLOAD, DONE }

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
        private const val UPLOAD_URL = "https://speed.hetzner.de/upload.php"
        private const val DOWNLOAD_DURATION_MS = 10_000L
        private const val UPLOAD_DURATION_MS = 8_000L
        private const val BUFFER_SIZE = 65536
        private const val LATENCY_PINGS = 5
    }

    suspend fun runTest(): Result = withContext(Dispatchers.IO) {
        _progress.value = Progress(Phase.LATENCY, 0f)

        // 1. Latency test
        val latency = measureLatency()
        _progress.value = Progress(Phase.LATENCY, 1f)

        // 2. Download test
        _progress.value = Progress(Phase.DOWNLOAD, 0f)
        val (dlSpeed, serverUrl) = measureDownload()
        _progress.value = Progress(Phase.DOWNLOAD, 1f, dlSpeed)

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
    }

    fun reset() {
        _progress.value = Progress()
    }

    private fun measureLatency(): Long {
        val times = mutableListOf<Long>()
        repeat(LATENCY_PINGS) { i ->
            try {
                val url = URL(DOWNLOAD_URLS.first())
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "HEAD"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val start = System.nanoTime()
                conn.connect()
                conn.responseCode
                val elapsed = (System.nanoTime() - start) / 1_000_000
                times.add(elapsed)
                conn.disconnect()

                _progress.value = Progress(
                    Phase.LATENCY,
                    (i + 1).toFloat() / LATENCY_PINGS
                )
            } catch (_: Exception) {
                // skip failed ping
            }
        }
        return if (times.isNotEmpty()) times.sorted()[times.size / 2] else -1L
    }

    private fun measureDownload(): Pair<Long, String> {
        val buffer = ByteArray(BUFFER_SIZE)
        var bestSpeed = 0L
        var bestUrl = DOWNLOAD_URLS.first()

        for (urlStr in DOWNLOAD_URLS) {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
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

                conn.disconnect()

                val totalElapsed = (System.currentTimeMillis() - startTime) / 1000f
                val speed = if (totalElapsed > 0) (totalBytes / totalElapsed).toLong() else 0L
                if (speed > bestSpeed) {
                    bestSpeed = speed
                    bestUrl = urlStr
                }
                break // First successful server is enough
            } catch (_: Exception) {
                continue // Try next server
            }
        }

        return Pair(bestSpeed, bestUrl)
    }

    private fun measureUpload(): Long {
        return try {
            val url = URL(UPLOAD_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 15000
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.setChunkedStreamingMode(BUFFER_SIZE)

            val data = ByteArray(BUFFER_SIZE)
            val startTime = System.currentTimeMillis()
            var totalBytes = 0L

            conn.outputStream.use { output ->
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed >= UPLOAD_DURATION_MS) break

                    output.write(data)
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

            conn.responseCode // flush
            conn.disconnect()

            val totalElapsed = (System.currentTimeMillis() - startTime) / 1000f
            if (totalElapsed > 0) (totalBytes / totalElapsed).toLong() else 0L
        } catch (_: Exception) {
            0L
        }
    }
}

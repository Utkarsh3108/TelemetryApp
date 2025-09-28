package com.example.telemetryapp.util


import android.view.Window
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Wraps JankStats and maintains a sliding 30s window of frame samples and jank counts.
 *
 * Usage: create and call start() from an Activity after window is available,
 * and call stop() on onDestroy.
 */
class JankTracker(private val window: Window) {
    private var jankStats: JankStats? = null
    /*private val state = PerformanceMetricsState()*/

    // circular buffer approach: maintain counts and timestamps
    private val durationsMillis = ArrayDeque<Pair<Long, Double>>() // timestamp -> ms

    fun start() {
        jankStats = JankStats.createAndTrack(window) { frameData ->
            val ms = frameData.frameDurationUiNanos / 1_000_000.0 // frameTotalDuration() available on frameData
            val ts = System.currentTimeMillis()
            synchronized(durationsMillis) {
                durationsMillis.addLast(ts to ms)
                val cutoff = ts - 30_000
                while (durationsMillis.isNotEmpty() && durationsMillis.first().first < cutoff) {
                    durationsMillis.removeFirst()
                }
            }
        }
        jankStats?.isTrackingEnabled = true
    }

    fun stop() {
        jankStats?.isTrackingEnabled = false

    }

    /**
     * Returns Triple(avgMs, stdMs, jankCount) for last 30s.
     * Jank definition: we rely on simple threshold > 16 ms OR if frameData.isJank available in your JankStats version, prefer that.
     */
    fun snapshotLast30s(): Triple<Double, Double, Int> {
        val list = synchronized(durationsMillis) { durationsMillis.map { it.second } }
        if (list.isEmpty()) return Triple(0.0, 0.0, 0)
        val avg = list.average()
        val std = kotlin.math.sqrt(list.map { (it - avg)*(it - avg) }.average())
        val jankCount = list.count { it > 16.0 } // coarse threshold
        return Triple(avg, std, jankCount)
    }
}

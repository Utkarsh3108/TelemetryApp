package com.example.telemetryapp.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.telemetryapp.MainActivity
import com.example.telemetryapp.R
import com.example.telemetryapp.compute.Convolution
import com.example.telemetryapp.model.TelemetrySummary
import com.example.telemetryapp.repo.TelemetryRepository
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class TelemetryForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isRunning = AtomicBoolean(false)
    private var computeLoad = 2
    private var basePeriodMs = 50L // 20 Hz
    private var pm: PowerManager? = null

    override fun onCreate() {
        super.onCreate()
        pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        registerReceiver(powerSaveReceiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        computeLoad = intent?.getIntExtra(EXTRA_COMPUTE_LOAD, computeLoad) ?: computeLoad
        when (intent?.action) {
            ACTION_START -> {
                startForegroundCompat()
                if (!isRunning.getAndSet(true)) startComputeLoop()
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_UPDATE -> {
                computeLoad = intent?.getIntExtra(EXTRA_COMPUTE_LOAD, computeLoad) ?: computeLoad
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat() {
        // ensure channel already created from MainActivity or Application
        val notification = buildNotification()
        startForeground(NOTIF_ID, notification)
    }

    private fun startComputeLoop() {
        serviceScope.launch {
            val baseInput = Convolution.makeInput()
            while (isActive && isRunning.get()) {
                val tickStart = SystemClock.elapsedRealtime()
                val powerSave = pm?.isPowerSaveMode ?: false
                val actualPeriod = if (powerSave) 100L else basePeriodMs // 10Hz vs 20Hz
                val actualLoad = if (powerSave) max(1, computeLoad - 1) else computeLoad

                val procStart = System.nanoTime()
                // CPU-bound compute on Default (we are already on Default)
                val out = Convolution.repeatedConvolve(baseInput, actualLoad)
                val procEnd = System.nanoTime()
                val frameMs = (procEnd - procStart) / 1_000_000.0

                // compute a tiny summary (mean/std) - cheap ops, acceptable on Default
                val (mean, std) = Convolution.summaryMeanStd(out)

                val summary = TelemetrySummary(
                    lastFrameMs = frameMs,
                    movingAvgMs = frameMs, // we'll let UI compute true moving avg via JankStats
                    movingStdMs = std,
                    jankPercentLast30s = 0.0,
                    jankCountLast30s = 0
                )

                TelemetryRepository.publish(summary)

                // sleep until next tick but do not accumulate if overrun
                val elapsed = SystemClock.elapsedRealtime() - tickStart
                val sleep = actualPeriod - elapsed
                if (sleep > 0) delay(sleep) else {
                    // overrun: skip sleeping (effectively drop the rest of this tick)
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        unregisterReceiver(powerSaveReceiver)
        super.onDestroy()
    }

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // no-op (loop checks PowerManager each tick)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, TelemetryForegroundService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Telemetry Lab — computing")
            .setContentText("Running edge inference simulation")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .addAction(R.drawable.ic_launcher_background, "Stop", stopPending)
            .setContentIntent(openPending)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    companion object {
        const val NOTIF_CHANNEL_ID = "telemetry_lab_channel"
        const val NOTIF_ID = 0xCAFE
        const val ACTION_START = "telemetry.action.START"
        const val ACTION_STOP = "telemetry.action.STOP"
        const val ACTION_UPDATE = "telemetry.action.UPDATE"
        const val EXTRA_COMPUTE_LOAD = "extra.compute_load"
    }
}

package com.example.telemetryapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.telemetryapp.service.TelemetryForegroundService
import com.example.telemetryapp.ui.screens.MainScreen
import com.example.telemetryapp.ui.theme.TelemetryAppTheme
import com.example.telemetryapp.util.JankTracker
import com.example.telemetryapp.viewmodel.TelemetryViewModel

class MainActivity : ComponentActivity() {
    private val vm: TelemetryViewModel by viewModels()
    private lateinit var jankTracker: JankTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        jankTracker = JankTracker(window)
        enableEdgeToEdge()
        setContent {
            TelemetryAppTheme {
                MainScreen(viewModel = vm, jankTracker = jankTracker)
            }
        }
        window.decorView.post {
            jankTracker.start()
        }
    }
    override fun onDestroy() {
        jankTracker.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TelemetryForegroundService.NOTIF_CHANNEL_ID,
                "Telemetry Lab",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Telemetry Lab compute notification"
            }
            nm.createNotificationChannel(channel)
        }
    }
}


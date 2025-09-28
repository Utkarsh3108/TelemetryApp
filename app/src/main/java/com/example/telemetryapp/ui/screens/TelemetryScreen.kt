package com.example.telemetryapp.ui.screens


import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.telemetryapp.model.TelemetrySummary
import com.example.telemetryapp.util.JankTracker
import com.example.telemetryapp.viewmodel.TelemetryViewModel
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TelemetryScreen(viewModel: TelemetryViewModel, jankTracker: JankTracker) {
    val isRunning by viewModel.isRunning.collectAsState()
    val computeLoad by viewModel.computeLoad.collectAsState()
    val summary by viewModel.summary.collectAsState()

    var jankStats by remember { mutableStateOf(Triple(0.0, 0.0, 0)) }

    val (avgMs, stdMs, jankCount) = jankStats

    var movingTriple by remember { mutableStateOf(Triple(0.0, 0.0, 0)) }

    // Poll jankTracker every 500ms for UI updates (coarse)
    LaunchedEffect(Unit) {
        while (true) {
            movingTriple = jankTracker.snapshotLast30s()
            delay(500)
        }
    }

    val pm = LocalContext.current.getSystemService(PowerManager::class.java)
    val powerSaveOn = pm?.isPowerSaveMode == true

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Telemetry Lab", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Switch(checked = isRunning, onCheckedChange = {
                if (it) viewModel.startTelemetry() else viewModel.stopTelemetry()
            })
        }

        Spacer(Modifier.height(12.dp))
        Text("Compute Load: $computeLoad")
        Slider(
            value = computeLoad.toFloat(),
            onValueChange = { viewModel.setComputeLoad(it.roundToInt()) },
            valueRange = 1f..5f,
            steps = 3
        )

        Spacer(Modifier.height(12.dp))
        if (powerSaveOn) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(8.dp)
            ) {
                Text("Power-save mode: reduced sampling & compute", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Last frame (ms): ${"%.2f".format(summary.lastFrameMs)}")
                Text("Moving avg (ms): ${"%.2f".format(movingTriple.first)}")
                Text("Moving std (ms): ${"%.2f".format(movingTriple.second)}")
                val jankPct = run {
                    // estimate total frames in 30s by measuring 30s/period (approx)
                    val periodMs = if (powerSaveOn) 100 else 50
                    val totalFrames = max(1, (30_000 / periodMs))
                    movingTriple.third.toDouble() * 100.0 / totalFrames
                }
                Text("Jank % (last 30s): ${"%.2f".format(jankPct)}")
                Text("Jank frames: ${movingTriple.third}")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Busy list to show UI activity — simple repeated items
        val items = remember { (1..1000).map { "Item #$it" } }
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(items) { idx, label ->
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(label)
                    Spacer(Modifier.weight(1f))
                    Text("Cnt: ${idx % 100}")
                }
            }
        }
    }
}


@Composable
fun MainScreen(viewModel: TelemetryViewModel, jankTracker: JankTracker) {
    val isRunning by viewModel.isRunning.collectAsState()
    val computeLoad by viewModel.computeLoad.collectAsState()
    val summary by viewModel.summary.collectAsState()

    // Jank metrics state
    val jankStatsState = remember { mutableStateOf(Triple(0.0, 0.0, 0)) }
    LaunchedEffect(Unit) {
        while (true) {
            jankStatsState.value = jankTracker.snapshotLast30s()
            delay(1000L)
        }
    }
    val (avgFrame, stdFrame, jankCount) = jankStatsState.value

    // Compute jank %
    val pm = LocalContext.current.getSystemService(PowerManager::class.java)
    val powerSaveOn = pm?.isPowerSaveMode == true
    val totalFrames = if (powerSaveOn) (30_000 / 100.0) else (30_000 / 50.0) // 10Hz vs 20Hz
    val jankPercent = (jankCount / totalFrames * 100.0).coerceAtMost(100.0)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top row: title + toggle

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Telemetry Lab", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Switch(checked = isRunning, onCheckedChange = {
                if (it) viewModel.startTelemetry() else viewModel.stopTelemetry()
            })
        }

        Spacer(Modifier.height(12.dp))

        // Slider
        Text("Compute Load: $computeLoad")
        Slider(
            value = computeLoad.toFloat(),
            onValueChange = { viewModel.setComputeLoad(it.roundToInt()) },
            valueRange = 1f..5f,
            steps = 3
        )

        Spacer(Modifier.height(12.dp))

        // Power save banner
        if (powerSaveOn) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(8.dp)
            ) {
                Text("Power-save mode: reduced sampling & compute", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
        }

        // Merged dashboard
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Telemetry Dashboard", style = MaterialTheme.typography.bodySmall)

                // Compute metrics
                Text("Current latency: ${"%.2f".format(summary.lastFrameMs)} ms")
                Text("Moving avg latency: ${"%.2f".format(summary.movingAvgMs)} ms")

                // Jank metrics
                Text("Frame time avg: ${"%.2f".format(avgFrame)} ms")
                Text("Frame time std: ${"%.2f".format(stdFrame)} ms")
                Text("Jank frames (30s): $jankCount")
                Text("Jank % (30s): ${"%.1f".format(jankPercent)}%")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Scrolling list to show UI activity
        val items = remember { (1..1000).map { "Item #$it" } }
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(items) { idx, label ->
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(label)
                    Spacer(Modifier.weight(1f))
                    Text("Cnt: ${idx % 100}")
                }
            }
        }
    }
}

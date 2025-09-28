package com.example.telemetryapp.viewmodel



import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemetryapp.model.TelemetrySummary
import com.example.telemetryapp.repo.TelemetryRepository
import com.example.telemetryapp.service.TelemetryForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.core.content.ContextCompat

class TelemetryViewModel(application: Application) : AndroidViewModel(application) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _computeLoad = MutableStateFlow(2)
    val computeLoad: StateFlow<Int> = _computeLoad

    private val _summary = MutableStateFlow(TelemetrySummary())
    val summary: StateFlow<TelemetrySummary> = _summary

    init {
        viewModelScope.launch {
            TelemetryRepository.summaries.collect { s ->
                // UI-level summary dispatch; further aggregation (moving avg / jank) done via JankStats
                _summary.value = s
            }
        }
    }

    fun setComputeLoad(value: Int) {
        _computeLoad.value = value
        // inform service
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, TelemetryForegroundService::class.java).apply {
            action = TelemetryForegroundService.ACTION_UPDATE
            putExtra(TelemetryForegroundService.EXTRA_COMPUTE_LOAD, value)
        }
        ContextCompat.startForegroundService(ctx, intent)
    }

    fun startTelemetry() {
        _isRunning.value = true
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, TelemetryForegroundService::class.java).apply {
            action = TelemetryForegroundService.ACTION_START
            putExtra(TelemetryForegroundService.EXTRA_COMPUTE_LOAD, _computeLoad.value)
        }
        ContextCompat.startForegroundService(ctx, intent)
    }

    fun stopTelemetry() {
        _isRunning.value = false
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, TelemetryForegroundService::class.java).apply {
            action = TelemetryForegroundService.ACTION_STOP
        }
        ctx.startService(intent)
    }
}

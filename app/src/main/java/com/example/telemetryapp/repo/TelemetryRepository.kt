package com.example.telemetryapp.repo


import com.example.telemetryapp.model.TelemetrySummary
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object TelemetryRepository {
    // replay=1 so UI can get the latest summary when it reconnects
    private val _summaries = MutableSharedFlow<TelemetrySummary>(replay = 1, extraBufferCapacity = 4)
    val summaries: SharedFlow<TelemetrySummary> = _summaries

    suspend fun publish(summary: TelemetrySummary) {
        _summaries.emit(summary)
    }
}

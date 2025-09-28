package com.example.telemetryapp.model

data class TelemetrySummary(
    val lastFrameMs: Double = 0.0,
    val movingAvgMs: Double = 0.0,
    val movingStdMs: Double = 0.0,
    val jankPercentLast30s: Double = 0.0,
    val jankCountLast30s: Int = 0,
    val timestampMs: Long = System.currentTimeMillis()
)
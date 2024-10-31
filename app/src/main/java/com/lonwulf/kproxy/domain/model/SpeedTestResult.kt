package com.lonwulf.kproxy.domain.model

data class SpeedTestResult(
    val bytesTransferred: Long,
    val durationSeconds: Double,
    val speedMbps: Double
)
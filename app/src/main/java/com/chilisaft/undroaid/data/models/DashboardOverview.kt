package com.chilisaft.undroaid.data.models

data class ArrayStatus(
    val statusLabel: String,
    val healthy: Boolean,
    val usedKb: Long?,
    val totalKb: Long?
)

data class SystemMetrics(
    val bootTimeIso: String?,
    val cpuLoadPercent: Double?,
    val memoryLoadPercent: Double?
)

/** A single CPU/memory reading from the live polling loop. See [SystemMetrics] for the full widget shape. */
data class MetricsSample(
    val cpuLoadPercent: Double?,
    val memoryLoadPercent: Double?
)

data class DockerContainerSummary(
    val name: String,
    val isRunning: Boolean
)

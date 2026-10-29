package com.chilisaft.undroid.data.models

data class ArrayStatus(
    val statusLabel: String,
    val healthy: Boolean,
    val usedKb: Long?,
    val totalKb: Long?,
    /** Size of the (first) parity disk, in KB - the scan size for parity check ETA math, not the same as [totalKb] (sum of data disks). */
    val paritySizeKb: Long?
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
    val state: DockerContainerState,
    /** Docker's own status, e.g. "Up 3 hours (healthy)" or "Created" - the same text as the Docker tab. */
    val statusText: String,
    val iconUrl: String?
)

/** `metrics.temperature`: every sensor the server reads, plus its own count of warm/hot ones. */
data class TemperatureOverview(
    val sensors: List<TemperatureSensorReading>,
    val warningCount: Int,
    val criticalCount: Int
) {
    /** The CPU package if there is one, otherwise the hottest sensor. */
    val headline: TemperatureSensorReading?
        get() = sensors.firstOrNull { it.kind == SensorKind.CPU_PACKAGE } ?: sensors.maxByOrNull { it.value }
}

data class TemperatureSensorReading(
    val id: String,
    val name: String,
    val kind: SensorKind,
    val value: Double,
    val unitSymbol: String,
    val level: TemperatureLevel,
    val warning: Double?,
    val critical: Double?
)

enum class SensorKind(val label: String) {
    CPU_PACKAGE("CPU"), CPU_CORE("CPU core"), MOTHERBOARD("Motherboard"), CHIPSET("Chipset"), GPU("GPU"),
    DISK("Disk"), NVME("NVMe"), AMBIENT("Ambient"), VRM("VRM"), OTHER("Sensor")
}

enum class TemperatureLevel { NORMAL, WARNING, CRITICAL, UNKNOWN }

/** Live throughput of one network interface, in bytes per second. */
data class NetworkRate(val interfaceName: String, val rxBytesPerSecond: Double, val txBytesPerSecond: Double)

/** One tick of the Dashboard's temperature + network poll; each half fails on its own. */
data class SensorsSample(val temperatures: WidgetResult<TemperatureOverview>, val network: WidgetResult<NetworkRate?>)

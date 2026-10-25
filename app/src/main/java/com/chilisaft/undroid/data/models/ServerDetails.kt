package com.chilisaft.undroid.data.models

/** The Server screen's identity section (the API's `vars`, VARS permission). */
data class ServerIdentity(
    val name: String?,
    val comment: String?,
    val timeZone: String?,
    /** Licence type, e.g. "Pro", "Basic", "Trial". */
    val licence: String?
)

/** The Server screen's system section (the API's `info`, INFO permission). */
data class ServerSystem(
    val unraidVersion: String?,
    val apiVersion: String?,
    val kernel: String?,
    val bootTimeIso: String?,
    /** e.g. "Intel Core i5-12400". */
    val cpu: String?,
    val cpuCores: Int?,
    val cpuThreads: Int?,
    /** Sum of all installed modules. */
    val memoryTotalBytes: Long?,
    /** e.g. "DDR4" - only when all modules agree. */
    val memoryType: String?,
    /** e.g. "ASUSTeK PRIME B660M-A". */
    val motherboard: String?,
    val network: NetworkSummary?
)

data class NetworkSummary(
    val interfaceName: String,
    val ipAddress: String?,
    val speedMbps: Int?
)

enum class ConnectStatus { ONLINE, OFFLINE, NEVER_CONNECTED }

/** Unraid Connect (the API's `server`, SERVERS permission). Blank fields from the API are null here. */
data class ConnectInfo(
    val status: ConnectStatus,
    val owner: String?,
    val lanIp: String?,
    val wanIp: String?,
    val localUrl: String?,
    val remoteUrl: String?
)

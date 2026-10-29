package com.chilisaft.undroid.data.models

import java.time.Instant

/** The Server screen's identity section (the API's `vars`, VARS permission). */
data class ServerIdentity(
    val name: String?,
    val comment: String?,
    val timeZone: String?,
    /** Licence type, e.g. "Pro", "Basic", "Trial". */
    val licence: String?,
    /** Set when Unraid reports the licence as unusable (`vars.regState` is one of its E* states). */
    val licenceProblem: LicenceProblem? = null
)

/** Unraid's licence error states, grouped by what the user has to do about them. */
enum class LicenceProblem { TRIAL_EXPIRED, TRIAL_INVALID, WRONG_FLASH_DRIVE, BLACKLISTED, NO_KEY_FILE, NO_FLASH_DRIVE, NOT_VALIDATED }

/**
 * Licence dates (the API's `registration`, REGISTRATION permission) - both null for a perpetual
 * licence without an update period, e.g. a real Basic key returns expiration "0" and no update date.
 */
data class LicenceDates(
    /** When a trial (or other time-limited key) stops working. */
    val expiresAt: Instant?,
    /** Until when OS updates are included (newer licences only). */
    val updatesUntil: Instant?
)

/** The Server screen's system section (the API's `info`, INFO permission). */
data class ServerSystem(
    val unraidVersion: String?,
    val apiVersion: String?,
    val kernel: String?,
    val dockerVersion: String?,
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
    val motherboard: String?
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

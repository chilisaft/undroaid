package com.chilisaft.undroid.data.models

/**
 * What the API can tell us about a share - checked against a real server: `Share.free` (and
 * `used`) describe the *storage the share lives on* (every share on the cache pool reports the
 * pool's numbers), not the share itself, so only free space is shown, labeled as such. The
 * share's own size is only there when Unraid has computed it (it does so lazily - otherwise 0).
 * `Share.cache` read false even for shares that clearly live on the pool, so it isn't used.
 *
 * Where a share lives (cache pool vs array) can't be told from the API: on a real server a
 * cache-only and an array-only share reported identical include/exclude/cache/allocator values.
 * `luksStatus` was "0" everywhere (no encrypted disks to compare against), so it isn't shown.
 */
data class ShareInfo(
    val id: String,
    val name: String,
    val comment: String?,
    /** Free space available to this share (on the disks/pool it can write to). */
    val freeKb: Long?,
    /** The share's own size, or null when Unraid hasn't computed it. */
    val sizeKb: Long?,
    /** From Unraid's share colour; null when it's a value this app doesn't know. */
    val protection: ShareProtection? = null,
    val settings: ShareSettings = ShareSettings()
)

/** Unraid's share colour: green = "All files protected", yellow/red = some or all unprotected. */
enum class ShareProtection { PROTECTED, UNPROTECTED }

/** The share's allocation settings, as shown in the details sheet. */
data class ShareSettings(
    /** Raw allocator: "highwater", "mostfree", "fillup". */
    val allocator: String? = null,
    /** Raw split level: "" = split any directory, "0" = manual, "N" = top N levels. Null when absent. */
    val splitLevel: String? = null,
    /** Minimum free space in KB; 0 = none. */
    val minimumFreeKb: Long? = null,
    /** Disks the share may use; empty = all. */
    val includedDisks: List<String> = emptyList(),
    val excludedDisks: List<String> = emptyList()
)

package com.chilisaft.undroid.data.models

/**
 * What the API can tell us about a share - checked against a real server: `Share.free` (and
 * `used`) describe the *storage the share lives on* (every share on the cache pool reports the
 * pool's numbers), not the share itself, so only free space is shown, labeled as such. The
 * share's own size is only there when Unraid has computed it (it does so lazily - otherwise 0).
 * `Share.cache` read false even for shares that clearly live on the pool, so it isn't used.
 */
data class ShareInfo(
    val id: String,
    val name: String,
    val comment: String?,
    /** Free space available to this share (on the disks/pool it can write to). */
    val freeKb: Long?,
    /** The share's own size, or null when Unraid hasn't computed it. */
    val sizeKb: Long?
)

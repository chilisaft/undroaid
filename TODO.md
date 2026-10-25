# Undroid — pending implementation

Features that are planned but not built yet. Each has an entry point in the app already (or is
noted as hidden), so this file is the list of what's still missing behind them. Background and
history live in [NOTES.md](NOTES.md); this file is only the to-do side.

When building any of these, use the shared UI building blocks (`ui/components`, see NOTES.md's
"UI design system"): `TabTopBar`/`DetailTopBar`, `ScreenList`, `AppCard`, `WidgetSection` for
load/error/permission states, `StatusLine` + `StatusTone` for any state indicator, `EmptyState`,
`ConfirmActionDialog` for anything destructive, and `UserMessages.showFailure` for failed actions.

---

## 1. Shares screen - built 2026-10-25

More tab -> Shares: read-only list of user shares (`SharesQuery` -> `SharesRepository` ->
`SharesViewModel` -> `SharesScreen`), sorted by name, each with its comment, free space, and its
own size when Unraid has computed it. Needs the SHARE permission (otherwise the lock placeholder).

Checked against a real server (Unraid 7.3.3), which corrected the first version:
- `Share.used`/`free` describe the *storage the share lives on* - every cache-pool share reported the
  pool's numbers, every array share the array's - so a per-share usage bar was misleading. Only
  free space is shown ("space available to this share").
- `Share.size` was 0 for every share (Unraid computes it lazily), so "in share" only appears when set.
- `Share.cache` was false even for shares that clearly live on the cache pool - not used.

Possible follow-ups:
- [ ] Show where a share lives (cache pool vs array). The API has no direct field; `include`/`exclude`
      list disks, and matching `free` against Main's pool/array numbers is a heuristic - only if worth it.
- [ ] `Share.color` (Unraid's protection indicator) and `Share.luksStatus` (encryption) - check their
      real values against a server, then show them as a `StatusLine` / "encrypted" hint.
- [ ] Tap a share for details (include/exclude disks, split level, allocator)? Only if wanted -
      still read-only.

---

## 2. Server screen

**Where:** More tab → Server (`ui/server/ServerScreen.kt`, route `server`). Currently a
"coming soon" placeholder.

**Goal:** server identity and connectivity at a glance - what this box is, how to reach it.

**Data:** root query `server: Server` - needs the `SERVERS` resource permission. `Server` fields:
`name`, `comment`, `status` (`ONLINE`/`OFFLINE`/`NEVER_CONNECTED` - Unraid Connect status),
`lanip`, `wanip`, `localurl`, `remoteurl`, `guid`, `owner { ... }`. The old
`ServerInformationQuery` that used this was deleted when the Dashboard moved off it; write a
fresh, purpose-built query.

> **Do not select `Server.apikey`** - it's the server's Connect API key, not something the app
> needs, and fetching it would put a secret in memory/logs for no reason.

**To do:**
- [ ] `ServerDetailsQuery.graphql` (`server { name comment status lanip wanip localurl remoteurl owner { username } }` - check `ProfileModel`'s fields in the schema)
- [ ] Model + `ServerRepository.getServerDetails()` returning `WidgetResult`
- [ ] Screen: `DetailTopBar`, identity card (name, comment), connectivity card (LAN/WAN IP, local/remote URL with tap-to-open via `LocalUriHandler`), Connect status as a `StatusLine` (`ONLINE` → Ok, `OFFLINE` → Error, `NEVER_CONNECTED` → Neutral)
- [ ] Consider also showing OS/version/uptime from `info` (already partly fetched for the account sheet and Dashboard - reuse, don't duplicate queries)
- [ ] Tests for repository mapping (including a server that has never used Connect - `remoteurl`/`wanip` likely empty) and the ViewModel
- [ ] Decide whether WAN IP should be shown by default or behind a tap (it's a privacy-sensitive value on a screen people may screenshot)

---

## 3. "Show core devices" / system devices screen (deferred)

**Status:** deliberately parked - hardware inventory isn't a glance-and-act concern for a first
version. The **Settings switch is hidden** (`ui/settings/SettingsScreen.kt` has a comment where it
was); the setting itself still persists (`Storage.showCoreList`, `SettingsRepository.showCoreList`,
`SettingsViewModel.setShowCoreList`) but nothing reads it.

**To do when picked up:**
- [ ] Decide placement: a new "Devices" entry in the More sheet (preferred) vs. a section in Main
- [ ] Query `info.devices { gpu pci usb network }`
- [ ] "Core device" filtering: the schema has no `isCore` flag - hide PCI devices by `class` (Host bridge, ISA bridge, SMBus, PCI bridge, System peripheral…), tuned against a real device dump
- [ ] Show `InfoGpu.blacklisted` (Unraid's "unsuitable for passthrough" flag)
- [ ] Restore the Settings switch once the screen exists

Full analysis: NOTES.md → "Deferred: Show core devices".

---

## 4. Drop the deprecated security-crypto dependency

Credentials moved to the app's own Keystore AES-GCM store (`utils/SecureStore.kt`) on 2026-10-25.
`androidx.security:security-crypto` is now only used by `migrateLegacyEncryptedPreferences()` to
read the old `encrypted_prefs` file once, on the first launch after updating.

- [ ] Once every install has launched a post-2026-10-25 build (for a single-user app: now - the
      developer's phone migrated on 2026-10-25), delete `migrateLegacyEncryptedPreferences()`, its
      instrumented test, the `encrypted_prefs.xml` backup exclusions, and the dependency itself.

---

## Blocked by the Unraid API (not buildable client-side yet)

These were requested but the API has no way to do them. Don't add disabled placeholder buttons -
revisit when a new schema adds the capability (diff the schema's `Mutation` type first).

- **Disk spin up/down, start mover, system reboot/shutdown** - no mutations exist
  (only `array.*` and `parityCheck.*`). See NOTES.md → Main tab section.
- **VM logs** - no VM log endpoint; the generic log-file API can't reach `/var/log/libvirt/qemu/`.
  See NOTES.md → "VM logs and Hibernate".
- **VM hibernate/suspend** - `VmMutations` has no such field (the `PMSUSPENDED` state is
  recognized, but can't be triggered).

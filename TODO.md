# Undroid — pending implementation

Features that are planned but not built yet. Each has an entry point in the app already (or is
noted as hidden), so this file is the list of what's still missing behind them. Background and
history live in [NOTES.md](NOTES.md); this file is only the to-do side.

When building any of these, use the shared UI building blocks (`ui/components`, see NOTES.md's
"UI design system"): `TabTopBar`/`DetailTopBar`, `ScreenList`, `AppCard`, `WidgetSection` for
load/error/permission states, `StatusLine` + `StatusTone` for any state indicator, `EmptyState`,
`ConfirmActionDialog` for anything destructive, and `UserMessages.showFailure` for failed actions.

---

## 1. Shares screen

**Where:** More tab → Shares (`ui/shares/SharesScreen.kt`, route `shares`). Currently a
"coming soon" placeholder.

**Goal:** a read-only list of user shares - glanceable size/usage, not share management.
Creating/editing shares (allocation method, split level, include/exclude disks, SMB/NFS export
settings) is desktop-webUI territory and out of scope.

**Data:** root query `shares: [Share!]!` - needs the API key's `SHARE` resource permission.
Useful `Share` fields (`schema.graphqls`, `type Share`):

| Field | Use |
|---|---|
| `id`, `name`, `comment` | row title / subtitle |
| `size`, `used`, `free` (KB, `BigInt` → mapped to `String`) | usage line + bar, via `kilobytesToHumanReadable()` |
| `cache` | "uses cache pool" hint |
| `include` / `exclude` | which disks the share spans (detail sheet, if any) |
| `luksStatus` | encryption hint |

**To do:**
- [ ] `SharesQuery.graphql` with the fields above
- [ ] `ShareInfo` model + `SharesRepository.getShares(): WidgetResult<List<ShareInfo>>` (via `runWidgetQuery`, same as the other repositories)
- [ ] `SharesViewModel` (load + pull-to-refresh, same shape as `VmsViewModel` minus actions)
- [ ] `SharesScreen`: `DetailTopBar` + `PullToRefreshBox` + `WidgetSection` + `ScreenList` of `AppCard` rows (name, comment, "used / size" with a usage bar)
- [ ] Empty state ("No shares found") and permission-denied state (handled by `WidgetSection`)
- [ ] Repository + ViewModel unit tests (same patterns as `VmRepositoryTest`/`VmsViewModelTest`)
- [ ] Verify against a real server what `size` reports for shares (it may be 0/unset for some share types - Unraid computes share size lazily)

**Open questions:** tap action (detail sheet with include/exclude disks, or nothing)? Should the
Dashboard get a shares summary? Probably not - keep the Dashboard to glance-and-act.

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

## Blocked by the Unraid API (not buildable client-side yet)

These were requested but the API has no way to do them. Don't add disabled placeholder buttons -
revisit when a new schema adds the capability (diff the schema's `Mutation` type first).

- **Disk spin up/down, start mover, system reboot/shutdown** - no mutations exist
  (only `array.*` and `parityCheck.*`). See NOTES.md → Main tab section.
- **VM logs** - no VM log endpoint; the generic log-file API can't reach `/var/log/libvirt/qemu/`.
  See NOTES.md → "VM logs and Hibernate".
- **VM hibernate/suspend** - `VmMutations` has no such field (the `PMSUSPENDED` state is
  recognized, but can't be triggered).

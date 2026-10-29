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

## 2. Server screen - built 2026-10-25

More tab -> Server: four independently loading sections, each needing its own API-key
permission (same pattern as the Dashboard widgets):
- **Identity** (`vars`, VARS): name, comment, time zone, licence tag (`regTy`), and a warning
  line when `regState` is one of Unraid's E* error states. Licence dates (trial expiry, "updates
  until") come from `registration` (REGISTRATION permission, own query) and are simply left out
  when missing or not permitted - a real Basic key sends expiration "0" and no update date.
- **System** (`info`, INFO): Unraid/API/kernel/Docker versions, uptime, CPU, cores/threads,
  total RAM + type, motherboard.
- **Network** (`info.primaryNetwork`, INFO): interface + IP. Split out of System on 2026-10-29
  because it alone took ~2 s of System's ~2.8 s; cached (see NOTES.md → Data-layer conventions).
- **Unraid Connect** (`server`, SERVERS): status (Online / Offline / "Not set up" when `server`
  is null), account, LAN IP, WAN IP (hidden until tapped), local/remote URL (tap opens). The
  server's Connect `apikey` is deliberately never selected (a test guards it).

Checked against a real server (Unraid 7.3.3), which corrected the first version:
- `MemoryLayout.size` is documented as bytes but came back in KiB - both are accepted (a module
  under 1 GiB can only be KiB).
- `InfoCpu.brand` already contains the vendor and ®/™ marks ("Gen Intel® Core™ i3-12100") - tidied
  to "Intel Core i3-12100".

Long-press copies IPs and URLs (`InfoRow(copyText = ...)`). Licence-date format (Unix seconds vs
milliseconds) is only verified for a key without dates - check against a trial or newer key.

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

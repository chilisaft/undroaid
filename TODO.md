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

Tapping a share opens a read-only details sheet: protection, free space, allocation method, split
level, minimum free space, included/excluded disks. The list only flags shares whose colour says
some files are unprotected (`yellow`/`red`; only `green-on` has been seen on a real server).

Checked against a real server on 2026-10-29 and deliberately not built:
- Where a share lives (cache pool vs array): a cache-only and an array-only share reported identical
  include/exclude/cache/allocator/colour values, so the API can't tell.
- Encryption (`luksStatus`): "0" on every share, with no encrypted disk to compare against.

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

## 3. Devices screen - built 2026-10-29

More tab -> Devices: PCI devices grouped (Graphics, Storage, Network, USB controllers, Other) and
USB device names, two independently loading sections (INFO permission). "Core" devices - PCI
class 05/06/07/08, SMBus 0c05, other serial bus 0c80, USB root hubs - are hidden unless Settings ->
"Show core devices" is on; a hint under each list says how many are hidden.

Checked against a real server (Unraid 7.3.4), which shaped what's queried:
- `InfoPci.type`/`productid` and `InfoGpu.type`/`productid` come back null although non-nullable,
  failing the whole list - so only `id`, `typeid`, `vendorname`, `productname` are selected, and
  the GPU list (nothing beyond the PCI entry) isn't used. `class` is "other" for almost everything
  and `vendorid` repeats `typeid` - both unused. `typeid` is the PCI class code the grouping uses.
- USB `bus`/`device` are null; `info.devices.network` is an empty list - not shown.
- `blacklisted` was false everywhere - not shown until a real true value can be checked.

Possible follow-ups:
- [ ] If a later API version fixes `type`/`productid`, the GPU list's `blacklisted` (unsuitable for
      passthrough) would be worth a warning line on the Graphics group.

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

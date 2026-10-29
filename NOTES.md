# Undroid — project notes

Android companion app for Unraid, talking to the Unraid GraphQL API (Apollo Kotlin) over
HTTP + a login-tested API key. MVVM + Hilt + Jetpack Compose. Intended use case (see
"Product direction" below): glance-and-act monitoring/quick-actions, not a full port of the
desktop webUI.

Last updated: 2026-07-24. Branch: `main`, last pushed commit `1d87628` - everything described
below as "this session" is **uncommitted** in the working tree as of this note.

**Session update**: Docker tab got a log viewer (live-tailed, in a near-fullscreen modal) - see
below. Fixed a real bug where the bell icon's unread badge never updated after dismissing
notifications via the system back gesture (only the in-app back-arrow tap triggered a refresh).
Notifications now show `subject` in addition to `description` (the API's `description` field is
often blank; the actual message usually lives in `subject`). Added an account icon (top-right on
Dashboard, next to the bell) opening a sheet with server URL/version, app version, and API key
name/roles (via the schema's `me` field - resolves to whatever's authenticating the request,
which for this app is always the API key, not a human user) - **Settings** and **Logout** moved
into that sheet, and Settings was removed from the "More" bottom-sheet entirely (still reachable
via the account sheet's Settings button). A log-line error/warning coloring heuristic was tried
and reverted per user feedback ("doesn't add much to the output") - see git-less history in this
conversation, not worth re-attempting without a concrete signal to key off of.

**Later in the same session**: Built out the Main tab for real (see below) - array/parity/cache/
boot device tables plus an Array Operation section, closing out item 2 from this file's previous
"suggested next steps." Also noticed (but did not create or touch) an untracked
`app/src/main/graphql/schema.graphql` file that appeared in the working tree partway through this
session - different content than the tracked `schema.graphqls` (note the trailing "s" - an extra
onboarding-related type), most likely written by Android Studio's own Apollo plugin/background
sync given the user has the project open there too. Builds/tests were unaffected either way
(Apollo picks up `schema.graphqls`), so left alone - worth the user's own look since I didn't
create it and don't know its actual origin.

**Later still**: the user downloaded a fresh schema from their own server (saved as
`schema.graphql`) hoping the previously-missing spin up/down/mover/reboot/shutdown mutations
might now exist. Diffed it against the tracked `schema.graphqls`: purely additive (0 lines
removed, 114 added - extra network-metrics types, expanded `InfoNetworkInterface`, and one real
new mutation, `docker.restart(id)` - see "Known limitations" below), but the `Mutation` type's
array/power-related section is byte-for-byte identical to before. Confirmed: spin up/down, mover,
and system reboot/shutdown still don't exist anywhere in this API. Replaced the tracked schema
with the new one anyway (it's a strict superset) - had to rename it from `schema.graphql` to
`schema.graphqls`, since Apollo Kotlin only recognizes `.graphqls`/`.json` as schema files and
treats bare `.graphql` as an operation document (that's *why* the two files coexisted peacefully
all along - Apollo was silently ignoring the untracked one). Also fixed a real theme bug where
Card containers were barely visible against their screen background - see below.

## What's actually working

- **Login** (`ui/login`) — server URL + API token, validated via `TestLoginQuery` (checks
  `info { os { hostname } }`, not the old fragile `server` field), stored in
  the Keystore-backed `SecureStore` via `Storage`. Auto-login on relaunch if credentials exist.
- **"Where do I get an API key?" help section** (`LoginScreen.kt`'s `ApiKeyHelpSection`) - the
  login form itself was barebones with zero onboarding for first-time setup. Collapsed by
  default (a `Row` that toggles an `AnimatedVisibility` block, not a dedicated component - Compose
  Material3 has no built-in expandable/accordion widget), so it doesn't clutter the form for
  anyone who already knows the steps. Content is a short numbered list (Settings → Management
  Access → API Keys on the Unraid webUI → create a key → pick a role, Admin for full access to
  every feature in this app vs. a narrower role to limit it → paste the key above) plus a link to
  the official docs at `docs.unraid.net/API/how-to-use-the-api/` - confirmed that page (and the
  webUI path/role names) via a live fetch this session rather than guessing, so both are accurate
  as of now. Deliberately just this one official link, not a custom-written full guide - "now is
  not the time to build those" per the user.
- **No more login-screen flash on relaunch** (`graphs/AppStartupViewModel.kt`,
  `UndroidActivity.kt`, `res/values/themes.xml`). Previously `RootNavGraph` always started at
  `AUTH_ROUTE`, so on every cold start the login form rendered and was fully visible for however
  long the auto-login's network validation took, before `LoginScreen` navigated away once it
  resolved - a real, if brief, flash for anyone with a saved session. Fixed the industry-standard
  way: `androidx.core:core-splashscreen` (`Theme.Undroid.Starting` in `themes.xml`, applied to
  the activity in the manifest, `postSplashScreenTheme` swaps back to the original
  `Theme.AppCompat`) holds the OS splash on screen via
  `splashScreen.setKeepOnScreenCondition { appStartupViewModel.state.value is Loading }` while a
  new `AppStartupViewModel` makes the routing decision - instantly if there's no saved session
  (nothing to check), or after one background validation call if there is. `RootNavGraph`'s
  `startDestination` is then set directly to the correct route (`UNDROID_ROUTE` or `AUTH_ROUTE`)
  before anything is ever composed, instead of always starting at login and navigating away after
  the fact. Deliberately does **not** touch `LoginViewModel`/`LoginScreen` at all - if the saved
  session's token turns out to be invalid, we still land on `AUTH_ROUTE` and `LoginScreen` mounts
  exactly as before, its own existing auto-login-on-init logic fires once more and surfaces the
  real error - a minor redundant network call in that (hopefully rare) case, traded for a much
  simpler, lower-risk implementation than trying to thread one shared login state across the
  activity-level startup gate and the per-route login screen.
- **Dashboard** (`ui/dashboard`) — real data, not mocks:
  - Array status, health (derived from disk statuses), storage used/total. A compact
    **Parity Check** card (progress bar, %, speed) appears directly below it, but only while a
    check is actually running/paused - hidden entirely otherwise, so it doesn't take up space
    most of the time. This is the one piece of Main-tab info that felt worth surfacing on
    Dashboard too (per user discussion) - a full copy of Main's device tables would just
    duplicate the array status card already here, against the "glance-and-act, not a full webUI
    port" framing. Reuses the existing `ParityCheckInfo` model from `ArrayOverview.kt` (Main
    tab's model file) rather than a duplicate - `ArrayStatusQuery` gained a `parityCheckStatus`
    selection alongside its existing `state`/`capacity`/disk-status fields, same ARRAY
    permission, no new query needed. Polls every 5s while running (mirrors
    `MainViewModel`'s parity-check poll, same "only while running, not merely paused" condition,
    same reasoning: progress needs to move without a manual refresh). Also refreshes on every
    return to this tab - see "Screen visibility & polling" below (an earlier note here described a
    `LaunchedEffect(Unit)` doing this, but that code never existed; it was added for real on
    2026-10-25)
  - CPU + memory load, polled every 1s (`ServerRepository.observeSystemMetricsPoll`) —
    deliberately polling, not a GraphQL subscription; see "Known limitations" below
  - Uptime, ticking client-side every 30s from `info.os.uptime` (boot time)
  - Docker containers, capped to 3 with a "Show all" button → jumps to the Docker tab, each
    row showing its real icon (same `ContainerIconBadge` component as the Docker tab). The card's
    header row has "Docker" on the left and the per-state counts on the right ("N Running   N
    Paused   N Stopped", omitting any state with a zero count) sharing one line - originally the
    title lived outside the card (a separate `SectionHeader`, since removed as dead code) with
    the counts on their own line below it, which per user feedback left dead space on the right
    of both lines; merging them into one `Arrangement.SpaceBetween` row uses that width instead.
    Getting real counts needed `DockerContainerSummary.isRunning: Boolean` widened to a real
    `state: DockerContainerState` (was previously collapsing PAUSED into "not running" - the
    per-row status dot/label was mislabeling paused containers as "STOPPED" before this; fixed
    as a side effect)
  - Notification unread-count badge on the bell icon
  - Every widget loads/retries independently; a permission failure on one Unraid resource
    doesn't blank the others. Failures distinguish "no permission" from generic errors
    (`data/models/WidgetResult.kt`, `data/api/GraphQlErrors.kt`)
  - Pull-to-refresh (`PullToRefreshBox`), `isRefreshing` derived from whether any of
    arrayStatus/systemMetrics/containers is currently `Loading` - calls the same
    `DashboardViewModel.refresh()` that reloads all of them together
- **Docker** (`ui/docker`, route `"docker"`, formerly the "Apps"/`virtualization` stub - renamed
  since it's specifically the Docker container list, not general virtualization) — full container
  list via `DockerRepository.getContainers()` (`DockerContainersQuery`, richer than the Dashboard's
  summary query). Each row shows the container's real icon (`iconUrl`, loaded via Coil3
  `SubcomposeAsyncImage` at 52dp, falls back to a generic layers glyph when there's no icon or it
  fails to load) plus name and running/stopped status - no image/tag text in the row itself
  (removed per user feedback, wasn't useful at a glance in the compact list). Tapping a row opens
  a `ModalBottomSheet`: a `ContainerInfoCard` at the top (bigger 56dp icon, name, image/tag,
  status/uptime text - the image line lives here now instead of the row) followed by
  context-appropriate actions - **Restart** (RUNNING/PAUSED only),
  Pause/Resume, Stop, Start, plus "Open Web UI" when `webUiUrl` is set (launched via
  `LocalUriHandler`). Restart originally had to chain `stopContainer` then `startContainer`
  client-side, since the schema had no native restart mutation - once the schema refresh (see
  above) added a real `docker.restart(id): DockerContainer!`, switched `DockerRepository`/
  `DockerViewModel.restart()` over to it directly, same shape as every other single-mutation
  action (`runAction`, refresh-on-success-only). The row still shows a spinner while its mutation is in flight
  (`actioningIds`, same shape as Notifications' `dismissingIds`). **Logs** action opens a
  near-fullscreen `Dialog` (`ContainerLogsDialog`) live-tailing `docker.logs(id, tail, since)` -
  first fetch grabs the last 300 lines, then polls every 3s using the previous response's
  `cursor` to fetch only new lines (capped at 2000 lines total). No level/stream field exists on
  `DockerContainerLogLine` (just `timestamp` + `message`), so there's no structured way to tell
  stdout from stderr or classify severity - a keyword-based coloring heuristic was tried and
  reverted (see Session update above). Pull-to-refresh on the container list
  (`isRefreshing = uiState.containers is WidgetResult.Loading`, calls the existing `onRetry`).
  **Updates** (2026-10-29, see TODO.md section 4): `isUpdateAvailable` drives an updates card,
  row tags and a sheet action; `refreshDockerDigests` is the manual "Check". Long-running calls
  raise OkHttp's 10 s read timeout per request via the `X-Read-Timeout-Seconds` header
  (`UnraidInterceptor.READ_TIMEOUT_HEADER`, applied with `chain.withReadTimeout` and stripped before
  sending; `runWidgetMutation(readTimeoutSeconds = ...)`): 60 s for the check, 30 min for updates.
  `ConfirmActionDialog` gained an optional `content` slot (body in `ConfirmDialogBody`, which
  previews use since Android Studio's static preview can't draw a dialog window).
- **VMs** (`ui/vms`, route `"vms"`) — built out later this session, deliberately mirroring the
  Docker tab's shape (`VmRepository`/`VmsViewModel`/`VmsScreen` are near-identical structurally to
  their Docker counterparts). Backed by `VmsQuery` (`vms.domains { id name state }`) and one
  mutation per action (`VmStartMutation`/`VmStopMutation`/`VmForceStopMutation`/
  `VmPauseMutation`/`VmResumeMutation`/`VmRebootMutation` - each just `vm { <action>(id: $id) }`,
  returning a bare `Boolean` rather than an object like Docker's mutations, so the query files have
  no sub-selection). `VmDomain` is a much thinner schema type than `DockerContainer` - just
  `id`/`name`/`state`, no icon, image, status text, or web UI URL - so each row uses a fixed
  generic computer icon (no Coil/iconUrl loading needed) instead of `ContainerIconBadge`, and the
  action sheet has no "Open Web UI"/"View Logs" entries (the schema has no VM logs concept at
  all - confirmed, nothing resembling `docker.logs` exists for `vm`). `VmState` has 8 enum values
  (`NOSTATE`/`RUNNING`/`IDLE`/`PAUSED`/`SHUTDOWN`/`SHUTOFF`/`CRASHED`/`PMSUSPENDED`) collapsed to a
  3-bucket `VmRunState` (RUNNING/PAUSED/STOPPED - everything else is "STOPPED" for the purposes of
  which actions are available), same coarse split as `DockerContainerState`, while the row/sheet
  still show the real specific label text (e.g. "Crashed", "Idle") via a separate `statusLabel`
  string - the bucket only decides button availability, not what's displayed. Action set per
  bucket: RUNNING gets Restart (`reboot`)/Pause/Stop/Force Stop; PAUSED gets Restart/Resume/Stop/
  Force Stop; STOPPED gets Start only (mirrors Docker's EXITED bucket). Deliberately left
  `VmMutations.reset` unwired - it's a genuinely different, more destructive concept (a hard
  power-cycle/reset-button equivalent) with no Docker analog and wasn't asked for; easy to add
  later if wanted. Same `runAction`/`actioningIds` per-row-spinner pattern as Docker throughout.
- **Notifications** (`ui/notifications`) — own screen via the bell icon, not on the
  dashboard anymore. Lists up to 50 unread, per-item dismiss + dismiss-all, both via
  `NotificationsRepository`. Each row shows `title`, then `subject` and `description` (each only
  if non-blank and not a duplicate of the previous line - the API populates these inconsistently
  across notification types). The bell's unread-count badge refresh
  (`DashboardViewModel.refreshUnreadCount`) is wired via a `DisposableEffect` in
  `NotificationsScreen`, not the back-arrow's `onClick` - Compose Navigation's system back
  gesture/button bypasses that lambda entirely, which was the bug. Pull-to-refresh on the list,
  same pattern as Docker.
- **Settings** (`ui/settings`) — theme (system/light/dark, actually wired into `AppTheme`),
  dynamic color toggle (actually wired), show-core-devices toggle (persists, **no consumer
  yet** — see "Deferred" below). Logout lives in the account menu now, not here (see below).
- **Theme** (`ui/theme/Color.kt`, `Theme.kt`) - the custom (non-dynamic-color) light/dark
  schemes only ever set the "key" Material3 roles (primary/secondary/tertiary/error/background/
  surface/surfaceVariant); the tonal-elevation `surfaceContainer*` roles were left unset, so
  Material3 defaulted them to the baseline M3 neutral palette - which happens to sit at nearly
  the same lightness as this app's custom background/surface in both themes. Net effect: every
  `Card` using `surfaceContainerLow`/`High` (Docker rows, Main tab device rows, Settings
  sections, the account menu card, etc.) barely stood out from the screen behind it. Fixed by
  explicitly defining all five `surfaceContainer*` roles with real, visible separation from
  background in both schemes. Dynamic color (Android 12+, the default) was never affected - the
  system already computes a coherent full tonal palette from the wallpaper.
- **Account menu** (`ui/usermenu`) — round account icon in the top bar of **every bottom-nav tab**
  (Dashboard - right of the bell; Main, Docker, VMs - each has its own `onUserClick` param wired
  to the same `showUserSheet` state in `UndroidNavGraph.kt`, so there's exactly one sheet
  instance regardless of which tab opened it), per user feedback that this should be available
  everywhere, not just Dashboard. Building this out is also why `VmsScreen.kt` went from a bare
  `Text` stub to a real `Scaffold`/`TopAppBar` shell (still just placeholder body content -
  no VM functionality yet, see "Open gaps"). Opens a `ModalBottomSheet` (`UserMenuSheet`) showing
  API Key name + roles (schema's `me` field - resolves to whatever's authenticating the request,
  which for this app is always the API key itself, never a human user - see
  `ServerRepository.getApiKeyInfo()`'s doc comment), Server URL/Version, App Version, then
  **Settings** and **Logout** buttons. Logout (with its confirm dialog) used to live in the
  Settings screen; moved here wholesale, not duplicated. Main's TopAppBar had a manual refresh
  icon (added earlier this session to diagnose the parity-check staleness bug below) - removed
  per user feedback once pull-to-refresh covered the same need, replaced with this account icon.
- **Main** (`ui/main`, route `"main"`) — array/parity/cache/boot device tables + array control,
  modeled on Unraid's own Main webUI tab. `MainArrayQuery` (`array { state capacity boot parities
  disks caches parityCheckStatus }`, using a shared `ArrayDiskFields` fragment) backs the device
  tables; `SystemDisksQuery` (root `disks`, a separate `DISK`-resource permission from `ARRAY`)
  supplies model/vendor/serial number - `ArrayDisk` itself has none of those, so rows are enriched
  by matching `device` path client-side, best-effort (missing `DISK` permission just means no
  identification column, not a blocked screen). Bottom "Array Operation" card: Start/Stop Array
  (Start opens a password dialog for `decryptionPassword` - blank is valid for unencrypted
  arrays), Parity Check controls (Start/Resume, Pause, and Stop buttons via `parityCheck.*`
  mutations - always all three visible, individually enabled/disabled by state rather than
  swapping between different button sets, see "Known limitations" below for why that swap
  approach was the actual cause of a real bug) with a progress bar that polls every 5s only while
  a check is actually running/paused - **only if the overview was already `running`/`paused` at
  the moment of a fetch**; a check that starts while the user is already sitting on an idle Main
  tab is never noticed, since nothing re-fetches on
  its own. Compounded by the bottom nav's `saveState`/`restoreState` navigation (`
  UndroidNavGraph.kt`) - switching tabs away and back does NOT recreate `MainViewModel` or
  re-trigger its `init` fetch, it restores the same already-fetched state. Net effect: before this
  session's fix, there was no way to get a fresh look at parity check status short of killing the
  app - a real bug the user hit (parity check actually running server-side, screen stuck showing
  "Cancelled" from a stale/earlier fetch, since confirmed as an actual upstream Unraid API bug -
  see the dedicated note further down, not something client-side to fix). Fixed by adding
  swipe-down-to-refresh (`PullToRefreshBox`, wrapping the `WidgetSection`,
  `isRefreshing = overview is WidgetResult.Loading`), wired to the same `onRetry`/`refresh()`
  used by the failure-retry path - a manual refresh icon was tried first but removed once
  pull-to-refresh covered the same need (see "What's actually working" → Account menu) and the
  icon slot went to the account menu instead. Same `PullToRefreshBox` pattern was then rolled out
  to Dashboard/Docker/Notifications too, once it fixed this real bug here first). Originally also
  had four **disabled** placeholder buttons below Parity Check - Spin Up/Down, Start Mover,
  Reboot, Shutdown - each captioned "Not available via this server's API," added after reading
  the entire `Mutation` type end to end and finding no mutation anywhere for spinning a disk
  up/down, starting the mover, or rebooting/shutting down the system (only `array`
  (setState/addDisk/removeDisk/mount/unmount/clearStatistics) and `parityCheck`
  (start/pause/resume/cancel) exist). Later in the session the user asked to re-check this against
  the newly-downloaded schema (see the schema-swap note above) - re-read the full `Mutation` type
  a second time, confirmed still nothing there for any of the four, and removed the buttons
  entirely per the user's request rather than continuing to show permanent dead UI. If the API
  ever adds these, `ParityCheckMutations`'s sibling groups (`array`, a hypothetical `power` or
  `system`) are where to look first. Section order (per user feedback): Array Devices, then Pool
  Devices, then Boot Device. Pool devices are grouped by `ArrayDeviceInfo.name` - the schema has
  no dedicated "pool" concept, so a multi-device pool (e.g. a mirror) only shows up as multiple
  `ArrayDisk` entries sharing the same name; grouping by that name is the only signal available,
  and only states "N devices," not the actual redundancy profile (mirror/raid/single), which
  isn't exposed anywhere. The boot device's `name` is always literally "flash" (Unraid's fixed
  internal slot name for it, `ArrayDisk.idx` 54 - not a bug, same for every Unraid install) - a
  new `FlashInfoQuery` (root `flash { vendor product }`, its own `FLASH` permission, since neither
  `ArrayDisk` nor the `disks`/`DISK` query cover the boot USB drive) supplies the real vendor/
  product as an identification line underneath, same pattern as the disk identification widget.
- **Main tab list spacing/grouping, revisited later this session.** Three follow-up rounds after
  the initial Main tab build:
  1. Docker's container list and Main's device list used different `LazyColumn` item gaps
     (`spacing.small` vs `spacing.medium`) while sharing the same card-internal padding
     (`spacing.medium`) - a smaller gap between cards than the padding inside them read as
     visually inconsistent. Settled on `spacing.itemSpacing` (12dp, an existing theme token that
     was barely used) for both lists as a middle ground, tighter than the original 16dp but not
     as tight as 8dp.
  2. Replaced the flat "one `Card` + `HorizontalDivider` between rows" grouping style (used for
     multi-disk pools and, briefly, for Array Devices) with Android Settings' own grouped-list
     look: each row is its own `Surface`, corners at the shared edges flatten to a small 4dp
     radius while the outer top/bottom corners keep the normal card radius, with a 2dp gap between
     rows (`DeviceGroupCard`/`GroupedItemSurface`/`groupItemShape` in `MainScreen.kt`) - reads as
     one cohesive cluster rather than either separate floating cards or one solid card with lines.
     Array Devices (parities + disks) now renders as a single such cluster; Pool Devices keeps one
     cluster per pool (see below) rather than merging every cache disk into one group, so
     differently-named pools stay visually distinct.
  3. **Pool grouping key fixed - was silently broken for the most common real case (a multi-disk
     pool).** The original `groupBy { it.name ?: it.id }` only merges disks whose `name` is
     *exactly* identical - but a real mirrored 2-disk "cache" pool actually reported as two
     `ArrayDisk` entries named `"cache"` and `"cache2"`, so they never grouped and showed as two
     separate cards despite being one pool (confirmed by the user against their own server).
     Investigated the actual `unraid-api` source (github.com/unraid/api) to find the real
     convention rather than guessing: `array.caches` (`get-array-data.ts`) is a flat
     `disk.type === CACHE` filter with zero pool-awareness, and `ArrayDisk.name`
     (`state-parsers/slots.ts`) is passed straight through from the OS's `disks.ini` state file,
     unmodified by the API - so the "cache"/"cache2" naming is an *OS-level* convention: a pool's
     first member is named exactly as the pool, additional members get the same base name suffixed
     with their slot order. Fixed by grouping on `name` with a trailing digit run stripped
     (`ArrayDeviceInfo.poolGroupKey()`) instead of the raw name. **Documented as a heuristic, not a
     guaranteed invariant** - nothing stops a user from naming an unrelated single-disk pool
     "cache2", which would wrongly merge into a real "cache" pool's group; there is no pool-ID
     field anywhere in the schema to do this properly (confirmed - `poolName`/`poolNames` only
     exist for the unrelated internal-boot-pool onboarding feature). Good enough for the common
     case (one multi-disk pool, e.g. a mirror) given the API exposes nothing better.
  4. **Pool name header, revisited immediately after (3) above.** Originally rendered as the top
     item of the capsule cluster itself (pool name + a `RoleBadge(CACHE)` + device count, styled
     like a row). User feedback: that read as redundant, since every device row already carries
     its own `CACHE` badge - the whole extra capsule wasn't adding information, just repeating it.
     Replaced with a small plain-text title (`labelLarge`, `onSurfaceVariant`, no background) sitting
     above the `DeviceGroupCard`, not inside it - dropped the badge and device count entirely.
     `DeviceGroupCard` lost its `header` slot as a result (was only ever used for this one case, so
     kept as dead flexibility would have been premature).
- **Navigation** (`graphs/UndroidNavGraph.kt`) — bottom nav: Dashboard, Main, Docker, VMs.
  A 5th "More" item opens an `ActionSheet` with Shares and Server (both built - see
  [TODO.md](TODO.md) #1/#2 for what they show). Settings is in the account menu. Bottom bar hides itself on drill-in screens
  (Settings/Notifications/Shares/Server), which have their own back button instead.

## Screen visibility & polling (2026-10-25)

- Every tab's screen tells its ViewModel when it's actually on screen, via
  `LifecycleStartEffect` -> `onScreenVisible()` / `onScreenHidden()` (the nav back-stack entry
  stops when another tab/drill-in screen is shown, and when the app goes to the background).
  **All polling only runs while visible**: Dashboard's 1s CPU/memory poll and 5s parity poll,
  Main's parity poll, Docker's 3s log tail (which pauses and resumes from its cursor). Previously
  they ran for the ViewModel's whole lifetime - i.e. the whole session, background included.
- Tab ViewModels survive tab switches, so every visit after the first calls `refresh()`.
- Refreshes keep current data on screen (`WidgetResult.reloading()`) and drive the pull-to-refresh
  indicator from a separate `isRefreshing` flag, instead of swapping lists for a loading card.
- Login/logout navigation lives in `navigateToMainAfterLogin()` / `navigateToLoginAfterLogout()`
  (`RootNavGraph.kt`), covered by the instrumented `RootNavigationTest`. Run instrumented tests
  with `installDebugAndroidTest` + `adb shell am instrument -w -e class <Test>
  com.chilisaft.undroid.test/com.chilisaft.undroid.HiltTestRunner` - **not** `connectedAndroidTest`,
  which uninstalls the app afterwards and wipes the saved login.
- Startup (`AppStartupViewModel`) only treats a *rejected* key as logged out; unreachable/slow/5xx
  keeps the session (checked with a 4s cap so the splash can't hang).
- The encrypted credentials file is excluded from backup/device transfer (its Keystore key can't
  move devices), and an undecryptable file is reset instead of crashing on launch.

## Security model (review 2026-10-25)

- **Where the API key may go**: only to the configured server. The OkHttpClient doesn't follow
  redirects (`followRedirects(false)`) - OkHttp keeps custom headers like `X-API-KEY` across
  cross-host and https->http redirects, so a redirect is reported as an error naming its target
  instead. `UnraidInterceptor` fails the request when there's no usable server URL (it used to fall
  through to Apollo's placeholder URL, key attached). Both covered by `ApiKeyExposureTest`.
- **Plain HTTP only on private networks** (`CleartextGuardInterceptor`, a *network* interceptor):
  judged by the IP actually connected to - loopback, RFC 1918, link-local, CGNAT 100.64/10
  (Tailscale) and IPv6 ULA are allowed; anything else needs https. So `http://name` works when the
  name resolves to a LAN address. `usesCleartextTraffic` stays true because a network security
  config can't express IP ranges.
- **Credentials at rest**: `SecureStore` (AES-256-GCM, key in the Android Keystore under
  `undroid_credentials`, random IV per value) in `secure_prefs.xml`, excluded from backup/transfer.
  Replaced the deprecated EncryptedSharedPreferences on 2026-10-25; the one-time migration out of
  the old `encrypted_prefs` file and the `security-crypto` dependency were removed on 2026-10-29
  (an install still on a pre-2026-10-25 build would have to log in again). `PreferencesModuleTest` uses its own alias/files and
  is now safe to run on a device with a real login. The Server screen's network cache (LAN IP)
  is stored there too and cleared on logout.
- Login screen sets `FLAG_SECURE` (the key can be shown in plain text there). Links from servers
  only open if they're http(s) (`tryOpenUri`). Only the INTERNET permission is declared.
- Not issues, checked: no request/token logging anywhere; no secret fields queried (`me` returns
  only the key's name/roles; `Server.apikey` deliberately never selected); only the launcher
  activity is exported; release builds are minified and not debuggable. Container icons load
  through Coil's own client without the API key (the icon host sees the phone's IP - accepted).

## Data-layer conventions (2026-10-25)

- Mappings shared between repositories live once in `data/repository/GraphQlMappers.kt` (array
  state / parity / disk-status labels, `toParityCheckInfo`, container state, device health).
- Device health is `ArrayDeviceInfo.health: DeviceHealth` (OK / NOT_PRESENT / PROBLEM), derived from
  the `ArrayDiskStatus` enum - never compare display labels. `DISK_NP` (an unassigned slot, e.g. no
  2nd parity) is NOT_PRESENT: neutral dot, doesn't make the array unhealthy (Dashboard badge and
  Main status line both use `allHealthy()`).
- User-facing error text comes from `ApolloException.toUserMessage()` (`data/api/ApiErrorMessages.kt`)
  - "Can't reach the server…", "The server rejected the API key", etc. - for widget errors, the
  failure Snackbar and the login form. Server-written GraphQL error messages pass through as-is.
- `PreferencesModule` (formerly an `object SharedPreferences` that shadowed Android's type) provides
  the default preferences and the `SecureStore`.
- The window theme is `Theme.Undroid` (platform Material, light + `values-night`), no AppCompat.
- Slow queries (2026-10-29): `info.primaryNetwork` takes ~2 s on Unraid 7.3.x; everything else in
  `info` is under 0.3 s (`baseboard`/`memory.layout` are fast despite going through dmidecode).
  **While it runs, the API answers nothing else** - requests sent alongside it (even `vars`, ~10 ms
  alone) all waited ~1.5 s. So it's its own query, `ServerNetworkRepository` caches the result (tied
  to the server URL), and the Server screen shows the cache and refetches only *after* its other
  sections have loaded - on open only if the cache is over an hour old, on pull-to-refresh always.
  Not warmed at app start: that would stall the Dashboard's first queries. Measure per field (and
  concurrently) before splitting or caching anything else.

## UI design system (2026-10-25)

After a consistency review, screens were moved onto shared building blocks instead of
copy-pasted variants. New screens should use these rather than hand-rolling a `Card`/`Row`:

- **Theme** (`ui/theme`): `AppShapes` (cards = `shapes.large` 20dp, badges = `medium`, grouped-row
  inner corners = `extraSmall`); `StatusTone` (Ok/Warning/Neutral/Error) + `StatusColors` -
  **stopped is Neutral, not Error**; Error is reserved for real faults (non-OK disk, crashed VM -
  `VmDomain.crashed`). Ok/Warning stay fixed green/gold under dynamic color. Typography bakes in
  weights (titles Bold, labelMedium Bold, labelSmall SemiBold) and Inter loads one real weight per
  `wght` axis value instead of synthesized bold - call sites shouldn't set `fontWeight`.
- **Components** (`ui/components`): `AppCard`, `ScreenList` (standard list spacing/padding),
  `SectionHeader`, `StatusDot`/`StatusLine`, `IconBadge` (+ `ContainerIconBadge`/`VmIconBadge`),
  `ResourceListItem`/`ResourceRowCard`/`ResourceHeaderCard` (Compact/Regular/Header sizes shared by
  Dashboard previews, tab lists, and action-sheet headers), `ActionSheet`/`ActionSheetItem`
  (`destructive` = error-colored, used for Stop/Force Stop), `TabTopBar`/`DetailTopBar`/
  `TabBarActions`, `EmptyState`, `ConfirmActionDialog`, `ParityCheckProgress`.
- Every tab now shows the notification bell + account icon (`TabBarActions`, built once in
  `UndroidNavGraph` and passed as `topBarActions`). Tab titles match the nav labels (Docker tab
  icon is `Layers`, matching the container fallback glyph).
- Dashboard hero gradient now runs `primary` → 35% towards `onPrimaryContainer` (was →
  `primaryFixed`, which washed out the white text on its right side in light mode).
- **Action feedback**: failed actions are no longer silent. ViewModels report failures through the
  `@Singleton` `UserMessages` (`showFailure("stop plex", failure)` → "Couldn't stop plex: <reason>"),
  shown by the one `SnackbarHost` on `UndroidNavGraph`'s Scaffold. Start/Stop Array now polls the
  overview until the new state is visible (like Docker/VM actions), instead of one refresh.
- **Confirmations** (`ConfirmActionDialog`): Stop Array, Stop Parity Check, VM Force Stop, Dismiss
  All, Log Out. Plain Docker/VM Stop is deliberately one tap - graceful and easily undone.
- **Copy conventions**: uppercase only for small overline labels (`SectionHeader`, "ARRAY STATUS",
  "CPU LOAD"); buttons and actions are Title Case ("Show All", "Dismiss All", "Log Out"); dialog
  titles are sentence-case questions ("Stop the array?"). It's an "API key" everywhere, never
  "token". UI strings live inline in composables - `strings.xml` only holds `app_name`. Unused
  fonts/resources (Open Sans, Chakra Petch light/italic cuts, Inter italic, legacy
  `dimens.xml`/`attrs.xml`/`font_certs.xml`) were removed.
- **Login** uses the brand identity: `BrandMark` (the launcher icon's own background color +
  foreground vector, so it can't drift from the home-screen icon), "Undroid" title, theme shapes
  for the fields/button, and it scrolls/pads for the keyboard instead of a fixed 150dp top gap.
- **Pending features**: tracked in [TODO.md](TODO.md). (Shares and Server were "coming soon"
  placeholders for a while; both have since been built, and so has Devices, which brought back
  Settings' "Show core devices" switch.)

## Open gaps — screens that are stubs

These exist as routes/nav destinations with a "coming soon" placeholder only, no real
functionality. The implementation plan for each is in [TODO.md](TODO.md).

(Shares and Server were both built on 2026-10-25 - see TODO.md #1/#2. No stub screens remain.)


## Known limitations / verified against a real server

Since 2026-10-29 there is a real server to test against (Unraid 7.3.4, API 4.37.9, the user's own,
reached from the `Medium_Phone` emulator and from read-only curl scripts the user runs with their
key). What that settled:

- **Verified by real use**: Docker start/stop/pause/restart, updates, icons (`iconUrl` loads),
  Web UI links, live logs; the Main tab's array/disk tables and parity-check controls (see the
  parity entries below for what real use uncovered); Dashboard; Shares; Server; Devices.
- **`BigInt` arrives as a JSON number**, not a string (`ArrayDisk.size`/`fsSize`/`fsFree`/`fsUsed`
  are all ints). The `kotlin.String` mapping still works - Apollo's string adapter reads a number
  token as its text - and the Main tab shows correct sizes. `fsSize`/`fsFree`/`fsUsed` are null
  for a second pool member (`cache2`), which the disk row handles by showing size only.
- **`me`** resolves for an API key: `name` and `description` are the key's own (e.g. "undroaid" /
  "Undroaid app api key"), `roles` the key's role (`ADMIN`), plus a full `permissions` list. The
  user menu shows name and roles; `description` is queried but not shown.
- **An invalid API key** gets `extensions.code = "UNAUTHENTICATED"` ("Invalid API key format",
  `originalError.statusCode` 401, over HTTP 200) on every field. This is not a missing permission:
  `GraphQlErrors.kt` keeps it out of `anyPermissionDenied()` and reports it via `anyKeyRejected()`.
  `KeyRejectedInterceptor` (an Apollo interceptor on the app's client) watches every response for
  it (or an HTTP 401) and sets the singleton `ApiKeyStatus`; `ApiKeyRejectedDialog`, over the
  logged-in graph in `RootNavGraph`, then asks the user to log in again - it forgets the key, keeps
  the server URL (the login form prefills it) and goes to login. A successful login clears the
  status. Login checks themselves (`X-API-KEY-OVERRIDE` requests) are skipped - the form shows
  their error.
- **The login check is `me { name }`** (`TestLoginQuery`), not `info.os.hostname`: `me` answers for
  any valid key, even a GUEST key with no permissions, so a key limited to some sections can log in
  and those sections show the lock placeholder. Previously such a key couldn't log in at all.
- **Permission-denied detection** (`data/api/GraphQlErrors.kt`) is confirmed: a valid key without
  the needed permission gets `extensions.code = "FORBIDDEN"` ("Forbidden resource",
  `originalError.statusCode` 403) on the field, which the code check catches. Tested with a key that
  has no permissions at all (`me` still resolves for it: role `GUEST`, empty `permissions`).
- **Notification bell badge** (`DisposableEffect` in `NotificationsScreen`) confirmed by the user:
  it updates after dismissing a notification and leaving with the system back gesture.
- **WebSocket subscriptions were tried and abandoned** for CPU/memory - `ApolloClient`'s
  WebSocket transport is correctly wired (`data/api/GraphQlServiceModule.kt`, explicit
  `httpEngine`/`subscriptionNetworkTransport` construction — the `.okHttpClient()`
  convenience shortcut crashes if combined with `.webSocketServerUrl()`, see git history),
  but a subscription's push cadence isn't client-controllable, so CPU/memory use a 1s HTTP poll
  instead, which is guaranteed and has test coverage. If push-based live updates are ever
  wanted (e.g. for array status via `arraySubscription`, or notifications via
  `notificationAdded`), the transport groundwork is already there.
- **`array.parityCheckStatus.status` genuinely read `CANCELLED` while a check was actively
  running** - confirmed by the user directly against their own server's API (not a client bug;
  our query/mapping matched the schema exactly, and the UI's "Cancelled" text only ever rendered
  when the response itself said `running: false, paused: false`). Root cause, traced through
  Unraid's own GitHub: the field was only added recently
  ([unraid/api#1611](https://github.com/unraid/api/pull/1611), merged Aug 2025 / Unraid 4.16.0,
  closing [#1372](https://github.com/unraid/api/issues/1372)) and derives status from raw
  `mdResyncPos`/`sbSyncExit` state vars rather than a dedicated "current check" tracker. "Parity
  check wrongly reported as cancelled" is also a long-standing pattern in classic Unraid
  (webGUI/email notifications), independent of this API - see forum reports
  [here](https://forums.unraid.net/bug-reports/stable-releases/incorrect-parity-check-result-reported-r3061/)
  and [here](https://forums.unraid.net/topic/150484-notification-that-parity-check-was-cancelled-but-it-shows-finished-in-gui/).
  **Update**: the user later restarted the `unraid-api` service (a separate Node.js daemon from
  core Unraid, distinct from the classic PHP webGUI) and `status` started reporting `RUNNING`
  correctly. Restarting that service to clear stale/broken state is a well-documented general
  troubleshooting step for it - multiple independent Unraid forum threads describe needing
  `unraid-api restart` to fix stuck state (GraphQL going offline, remote access breaking daily,
  etc.), so this looks like a genuine, fairly common reliability characteristic of that service
  in general, not something specific to this user's setup - not confirmed as *this exact* symptom
  anywhere else though, so treat that connection as a plausible inference, not a confirmed match.
- **`running`/`paused` booleans can disagree with the `status` enum in the same response** - found
  via the above: even after the service restart fixed `status` to correctly read `RUNNING`, the
  `running` boolean field itself was still `false` in the same response, which meant the Main
  tab's Pause/Stop buttons and the Dashboard's parity-check banner (both gated on the `running`/
  `paused` booleans, not `status`) still didn't work correctly. Fixed in both
  `ArrayRepository.toParityCheckInfo()` and `ServerRepository.toParityCheckInfo()` by OR-ing each
  boolean with the matching `status` enum value (`running = (running ?: false) || status ==
  ParityCheckStatus.RUNNING`, same for `paused`) - either signal being right is now enough, rather
  than trusting the booleans alone and silently hiding an active check. Also rebuilt Main tab's
  parity-check button row: previously swapped between "Start" (idle) and "Pause + Cancel"
  (active), which is exactly the layout that was hiding the buttons when `running` disagreed with
  `status` in the wrong direction; now Start/Resume, Pause, and Stop are all always visible,
  individually enabled/disabled by state - more resilient to exactly this kind of disagreement
  since no button ever fully disappears. Also added (then, later this session, removed - see
  below) a `LaunchedEffect(Unit)` in `DashboardScreen` to refresh on every entry into that tab
  (not just app launch) - `DashboardViewModel` is a persistent singleton that doesn't auto-refetch
  on tab switches, so a parity check starting while the user is elsewhere previously wouldn't show
  until a manual pull-to-refresh. **Note this fix is correct for both running and paused** -
  `paused` gets the identical OR-with-`status` fix (`status == ParityCheckStatus.PAUSED`) as a
  separate condition from `running`, and both widgets' visibility checks already test
  `running || paused` - a paused check does not make either widget disappear.
- **Removed the Dashboard's per-tab-switch full refresh** (later in the session, after the parity
  check split above). The `LaunchedEffect(Unit) { viewModel.refresh() }` added earlier for the
  parity-check-staleness reason above made Dashboard the only tab that re-fetched everything on
  every re-entry - Main/Docker/VMs just show whatever their ViewModel last loaded, refreshed only
  via pull-to-refresh, because their ViewModels are also tab-switch-persistent but never had an
  equivalent re-fetch-on-entry effect. That inconsistency was user-visible (a loading-spinner
  flash across every Dashboard widget on every tab switch) for a problem that turned out to
  already be covered elsewhere: the parity-check widget self-polls every 5s while a check is
  actually active (see the split above), and the notification bell's unread-count badge already
  refreshes independently via `NotificationsScreen`'s own `DisposableEffect` calling
  `dashboardViewModel::refreshUnreadCount` through `onNotificationsChanged` - unrelated to tab
  switching, and unaffected by this removal. Dashboard now behaves like the other three tabs:
  loaded once, refreshed via pull-to-refresh or a specific retry/callback.
- **Parity check speed/ETA** (`utils/Formatting.kt`): the `speed` field arrives as a bare number
  string (confirmed by the user - schema docs say "in MB/s" but don't include the unit in the
  value itself), so both widgets now append " MB/s" when displaying it. Added
  `estimatedSecondsRemaining(progressPercent, speed, totalSizeKb)` + `Long.toRemainingTimeLabel()`
  to compute a "~Xh Ym remaining" estimate from remaining-bytes-over-current-speed (the same
  approach Unraid's own webGUI uses) - deliberately *not* based on elapsed time: the API's
  `duration` field looks like it reflects the *previous completed* run (derived from
  `sbSynced`/`sbSynced2` per the PR analysis above), not live elapsed time of an active check, so
  extrapolating from it would likely be wrong. Needs a total size to scan against - Main tab
  already has this via `overview.parities.firstOrNull()?.sizeKb`; Dashboard's `ArrayStatusQuery`
  gained `parities { size }` and `ArrayStatus` a new `paritySizeKb` field to get the same
  capability. Uses the *parity* disk's size specifically, not `ArrayStatus.totalKb` (sum of data
  disks) - a parity check scans the parity disk, a different quantity entirely.
- **`BigInt` scalar mapped to `kotlin.String`** (`app/build.gradle.kts`, alongside the existing
  `PrefixedID`/`DateTime` mappings) - `ArrayDisk.size` and friends were the first `BigInt` fields
  queried. Unmapped, Apollo generates `kotlin.Any?`; as `String` the repository parses them with
  the same `.toLongOrNull()` pattern as `Capacity.used`/`total`. The server sends JSON numbers
  (see the top of this section), which the string adapter accepts.
- **Main tab doesn't cover every array mutation the schema has** - only `setState` (start/stop)
  and `parityCheck.*` are wired up. `addDiskToArray`/`removeDiskFromArray`/`mountArrayDisk`/
  `unmountArrayDisk` still have no UI (array topology editing felt like a separate, riskier
  feature from "monitor + start/stop/parity-check" - not attempted this session).
- **"Unknown error" on Main tab and Dashboard's array status when resuming the app from the
  background - real root cause found, first fix attempt reverted, correct fix applied.** First
  attempt (now reverted): added a retry-once-after-a-short-delay to `runWidgetQuery`, reasoning
  that `toWidgetResult()`'s `"Unknown error"` fallback (response parsed with no `data` and no
  `errors` - not a GraphQL-level error, more like a dropped/truncated connection) was a one-off
  transient blip after resuming. The user confirmed this **did not fix it** - reproducible
  reliably by locking the device and unlocking ~20s later. That timing was the tell: a 500ms
  retry can't help if the network is actually down for several/~20s, and the *real* bug turned
  out to be structural, not a networking blip:
  - `DashboardViewModel`'s and `MainViewModel`'s parity-check poll loops (5s interval, only
    active while a check is running) unconditionally wrote **any** fetch result - success or
    failure - into UI state, and used `if (!result.isParityCheckRunning()) break` to decide when
    to stop polling. `isParityCheckRunning()` returns `false` for a `Failure` (the
    `as? WidgetResult.Success` cast fails), making "the fetch failed" indistinguishable from "the
    check actually finished" - so a **single bad tick permanently killed the poll loop**, leaving
    whatever error that one tick produced on screen forever, with no way to recover short of a
    manual retry (which calls `refresh()`, re-detects the check as running, and starts a fresh
    poll job).
  - This explains every symptom precisely: not a WebSocket (confirmed zero `.subscription(...)`
    calls anywhere in the app - the wired-but-unused transport is a red herring); the Array
    Health card and the Parity Check banner disappear together because they render from the
    *same* `WidgetResult<ArrayStatus>`/`WidgetResult<ArrayOverview>`, so one failed tick blanks
    both; and CPU/Memory look unaffected not because their connection survives, but because
    `observeSystemMetricsPoll` was already written to silently skip a failed tick and keep
    polling every 1s regardless - the parity poll just never got the same treatment. ~20s of
    screen-off is enough for Wi-Fi to still be reconnecting when one of the 5s ticks lands.
  - **Fix**: both poll loops now mirror `observeSystemMetricsPoll`'s resilience exactly - on a
    `Failure`, skip the tick entirely (don't touch UI state, don't break) and just try again next
    interval; only a **successful** fetch reporting `running == false` stops the loop. Regression
    tests added to both `MainViewModelTest` and `DashboardViewModelTest` (a failed tick between
    two successful ones: state stays on the last good value, then recovers, poll never stops).
  - Follow-up, same session: the user asked to split parity check into its own widget (see below)
    rather than leaving it folded into `ArrayOverview`/`ArrayStatus` - done.

**Parity check split into its own independent widget (same session, follow-up to the poll-loop
fix above).** `ArrayOverview.parityCheck` and `ArrayStatus.parityCheck` are gone - parity check is
now fetched by a standalone `ParityCheckStatusQuery` (`array { parityCheckStatus { ... } }`),
backed by `ArrayRepository.getParityCheckStatus()` / `ServerRepository.getParityCheckStatus()`,
each with their own `WidgetResult<ParityCheckInfo>` in `MainScreenState`/`DashboardScreenState`
and their own poll loop (`MainViewModel`/`DashboardViewModel`, unchanged 5s interval and
failed-tick-skipping behavior - just polling the parity query alone now instead of the whole
array query). `MainArrayQuery`/`ArrayStatusQuery` no longer fetch `parityCheckStatus` at all. On
Main, `ArrayOperationSection`'s parity controls are now their own `WidgetSection` (independent
loading/error/retry), decoupled from the array/cache/boot device tables above. On Dashboard,
`ArrayStatusCard` and `ParityCheckBanner` are now two separate `LazyColumn` items - the banner
item only renders at all when `parityCheck` is a `Success` with `running || paused` true (loading
and failure states render nothing, preserving the "glanceable, disappears when idle" behavior
rather than showing an empty placeholder card most of the time). Net effect: a parity-specific
hiccup no longer blanks the array device tables/health card, and vice versa - the exact isolation
gap called out in the previous "Suggested next steps" entry.

## Test coverage

252 JVM unit tests, all passing as of 2026-10-29. The history below is from the 2026-10-25
session, when the count was 160.

160 JVM unit tests (`./gradlew testDebugUnitTest`), all passing as of this note (up from 96 at
the start of this session - see the "Session update" notes above for what added the difference:
`DockerRepository`/`ServerRepository` coverage for the log/version/API-key queries,
`UserMenuViewModelTest`, minus the logout test removed from `SettingsViewModelTest`; then a new
`ArrayRepositoryTest` and `MainViewModelTest` - the latter including a virtual-time test of the
parity-check-running poll loop, using `StandardTestDispatcher`'s `runCurrent()`/`advanceTimeBy()`
rather than `advanceUntilIdle()`, which would fast-forward through all pending poll ticks at
once and defeat the point of the test; then a `restartContainer` test in `DockerRepositoryTest`
and simplified restart tests in `DockerViewModelTest` once restart switched to the native
mutation; then `getFlashInfo` coverage in `ArrayRepositoryTest` and `MainViewModelTest`; then two
more `DashboardViewModelTest` cases for its own parity-check poll loop, same virtual-time pattern;
then 3 regression tests (2 in `ArrayRepositoryTest`, 1 in `ServerRepositoryTest`) for the
running/paused-vs-status-enum mismatch bug; then 4 new `FormattingTest` cases for
`estimatedSecondsRemaining`/`toRemainingTimeLabel`; then a short-lived `ApolloWidgetExtensionsTest`
(5 cases) for a query-retry-once fix that was reverted this same session once proven ineffective
(see "Known limitations" above) - removed along with the fix; then 2 regression tests (one each
in `MainViewModelTest`/`DashboardViewModelTest`) for the poll-loop-breaks-on-any-failure bug that
turned out to be the actual cause; then, splitting parity check into its own widget, 4 new
`getParityCheckStatus` tests each in `ArrayRepositoryTest`/`ServerRepositoryTest` (replacing the
old parity-check assertions embedded in the `getArrayOverview`/`getArrayStatus` tests) and a
reworked `MainViewModelTest`/`DashboardViewModelTest` targeting the now-independent `parityCheck`
state field instead of `overview.parityCheck`/`arrayStatus.parityCheck`; then, building out the
VMs tab, a new `VmRepositoryTest` and `VmsViewModelTest`, same shape as their Docker counterparts;
then a new `AppStartupViewModelTest` for the login-flash fix) -
repositories, ViewModels, the formatting utils, the permission-detection heuristic, and one
test (`GraphQlServiceModuleTest`) that actually constructs the `ApolloClient` the way Hilt
does, specifically because a prior version of that wiring compiled fine but crashed at
runtime on every launch (see git log). No Compose UI tests exist - previews only.

## Repo/branch housekeeping

- Renamed `master` → `main` this session (local + pushed to `origin/main`). `origin/master` is
  confirmed deleted on GitHub as of this session's `git fetch --prune` - the default-branch
  switch + old-branch deletion is done, nothing left to do here.
- This session also cleaned up local git clutter left over from prior sessions: removed a
  detached-HEAD worktree (`undroid-android-review-def47e`) and 3 branches that were fully
  merged into `main` but never deleted, fixed a dangling `origin/HEAD` (now points at `main`),
  and pushed 2 commits that were sitting unpushed on local `main`.
- `.idea/` is now untracked and fully gitignored (files stay on disk for the IDE, git just stops
  following them) - it was already listed in `.gitignore` but 13 files had been committed before
  that rule existed, so ignoring alone didn't stop `deploymentTargetSelector.xml` etc. from
  showing as modified after routine IDE/adb activity. Also deduped `.gitignore`, which had
  several entries listed twice from being appended to over time.

## App icon / branding exploration

Explored a real app identity this session via an Artifact (SVG concept pitches, iterated over
several rounds) after the user asked for logo help - the previous launcher icon was the untouched
Android Studio "Empty Activity" template (green grid background, white robot-head glyph), never
actually designed.

- **Brief**: independent identity (no Unraid orange, no Android green), playful/not-corporate
  register, flat/dual-tone finish, incorporating a small winking-face detail the user specifically
  liked once it existed.
- **Palette**: forest tones - `#16321F` ink / `#2F6B48` pine / `#E3A94A` gold pop /
  `#EEF2EA` paper. Confirmed by the user, stable across every round.
- **Two directions approved and saved** as standalone SVGs at the repo root (literal hex colors,
  not the artifact's CSS custom properties, so they survive independent of the Artifact page):
  - `branding/undroid-mark-home-server.svg` - a literal "home server" pun: a conventional
    rounded-corner gable roof over a chassis, bay-slit siding, the wink where a window would sit.
  - `branding/undroid-mark-platter.svg` - a top-down hard drive: rounded case, corner screws,
    spindle hub, and a tapered actuator arm (thick at the pivot, thin at the head) entering
    diagonally from a side pivot - case/platter/hub/face all share one vertical axis of symmetry,
    with the arm as the one deliberate diagonal against that symmetric frame. Went through several
    rounds first (a tilted drive-bay stack, rejected as asymmetric/corporate; three fresh
    directions incl. Little Tower, dropped without a mention; an off-center arm-and-face layout,
    still too asymmetric) before landing here.
- **Platter is now the live app icon**, wired up for real (not just saved as reference art):
  - `res/values/ic_launcher_background.xml` - background color changed to the pine `#2F6B48`.
  - `res/drawable/ic_launcher_background.xml` - the OTHER (non-`-v26`) adaptive-icon background
    vector, previously the default template's grid pattern, replaced with a matching solid pine
    fill for consistency (see below on why two parallel resource sets exist).
  - `res/drawable/ic_launcher_foreground.xml` **and**
    `res/mipmap-anydpi-v26/ic_launcher_foreground.xml` (both, kept identical) - the Platter mark as
    a `<vector>`, scaled 0.75x and translated (18,18) from its native 96x96 space into the 108x108
    adaptive-icon canvas; verified every shape stays within the 33dp safe-zone radius from center
    regardless of launcher mask shape (circle/squircle/rounded-square all fine). Placed in *both*
    locations because `mipmap-anydpi-v26/ic_launcher.xml` references `@mipmap/ic_launcher_foreground`
    while the parallel `mipmap-anydpi/ic_launcher.xml` (no version qualifier - lower priority than
    `-v26` for this app's `minSdk 28`, likely vestigial, but cheap to keep consistent) references
    `@drawable/ic_launcher_foreground` - same content, two reference paths.
  - `res/drawable/ic_launcher_monochrome.xml` (new) - the Material You "themed icon"/dynamic-icon
    layer the user asked for. **Deliberately not just a solid recolor of the full mark** - Android
    re-tints this to one flat system color, so reusing the same filled shapes everywhere would
    merge into a single blob with no visible structure. First pass drew the case/platter as
    outlines (stroke, no fill) with the small accents solid on top - worked, but the user pointed
    out it didn't match the full-color mark's own logic. Redone to mirror that logic instead: in
    the full-color version ink (case/arm/hub) and gold (the wink) are the two "solid" tones, while
    pine (platter/screws) is really just a second surface color - so now the case is one solid
    shape with the platter circle and all four screws punched out as genuine transparent holes
    (one `<path>`, `android:fillType="evenOdd"`, case + platter + 4 screws as subpaths - a point
    covered by the case alone stays filled, a point covered by both the case *and* an inner shape
    becomes a hole), and the arm/hub/wink stay solid on top, floating in that cutout. Wired into
    both `<monochrome>` slots - `mipmap-anydpi-v26/ic_launcher.xml`/`ic_launcher_round.xml` (added;
    didn't exist before) and `mipmap-anydpi/ic_launcher.xml`/`ic_launcher_round.xml` (existed
    already but pointed at the full-color foreground by mistake in the scaffolding - fixed to
    point here instead).
  - Per-density legacy PNG rasters (`mipmap-hdpi/ic_launcher*.png` etc.) were left untouched -
    with `minSdk 28` (already ≥26) the vector adaptive-icon path always wins, so those PNGs are
    now genuinely dead weight in the APK. Cheap future cleanup, not done this session (touching
    binary assets felt like unnecessary scope beyond what was asked).
  - Verified: `assembleDebug` resource-links cleanly (confirms the hand-written VectorDrawable arc
    path data and group transforms are all valid), full test suite still green (resource-only
    change), installed on-device.
  - **Icon didn't show up on the home screen after the first install** - a plain `adb install -r`
    (what `installDebug` does) updates the APK but many launchers cache the generated icon bitmap
    and don't notice a changed adaptive-icon resource on a same-version reinstall. Fixed with a
    full `adb uninstall` + fresh install, which reliably forces the cache to drop. Worth remembering
    for any future icon-only change - it's an icon-cache quirk, not a build/wiring problem.
  - **Fixed, same session, once it was visible at real icon size**: two geometry issues only
    became obvious once Platter was actually wired up as the app icon. (1) The wink was crowding
    the platter's top edge - moved down (face `translate(48 33)` → `translate(48 36)`, computed
    for real clearance from both the platter edge and the spindle hub, not just eyeballed). (2)
    The case itself wasn't vertically centered in the mark's own 96x96 space - its margins were
    18px top / 10px bottom (an unintentional offset from when the case position was first chosen),
    visibly off-center once cropped to a launcher icon even though it was barely noticeable in the
    original exploration Artifact's square preview tiles. Recentered the whole composition (case,
    screws, platter, arm, hub) around the viewBox's true center instead of just patching the
    Android-specific transform, so the fix is at the source and stays correct everywhere the mark
    is used. All five places that carry this mark's coordinates were updated together and re-
    verified consistent: the exploration Artifact's `mark-platter` def, `branding/
    undroid-mark-platter.svg`, `ic_launcher_foreground.xml` (both the `drawable/` and
    `mipmap-anydpi-v26/` copies, kept identical), and `ic_launcher_monochrome.xml`.
  - **Themed/dynamic icon "doesn't show up" - verified this is a launcher setting, not a wiring
    bug.** The user's test device runs Kvaesitso (a third-party launcher), Android 17/API 37 -
    confirmed via `adb getprop` this is nowhere near the API 33 floor for Material You themed
    icons, so that wasn't it either. Checked Kvaesitso's own docs
    (kvaesitso.mm20.de/docs/user-guide/customization/themed-icons.html): it does support themed
    icons and explicitly documents "if the drawable has a `<monochrome>` layer, this will be used"
    - exactly what's wired up here - but it's an opt-in toggle in the launcher's own settings
    (Settings → Grid & icons → Themed Icons), separate from Android's system-level toggle. Nothing
    to fix on the app side; the user needs to enable it in Kvaesitso.
- Home Server remains saved-but-not-wired - only Platter was asked to go live.
- **App theme expanded into a full custom Material3 scheme** (`ui/theme/Color.kt`/`Theme.kt`),
  not just used for the app icon. Built proper tonal ramps (10 stops each) for a primary hue
  (forest green - `ForestInk`/`ForestPine` sit at tones 20/40 of the *same* ramp, not a
  coincidence), a cooler secondary "moss" hue for variety, and the tertiary gold
  (`ForestGold` used directly as the light-theme tertiary rather than a computed tone, so the
  exact brand color shows up front and center - this is also the Docker/VMs "Paused" indicator
  color). Neutral/neutral-variant ramps get a faint green-gray bias rather than pure gray, per the
  same "considered neutral, not default" principle used when designing the branding Artifact
  pages. `errorLight`/`errorDark` deliberately kept close to M3's standard red rather than forest-
  tinted - errors need to read as unambiguously wrong at a glance. Every M3 color role is now
  explicitly set (previously only a subset was - primary/secondary/tertiary/error/background/
  surface/surfaceVariant/the custom surfaceContainer tiers - leaving `outline`, `scrim`,
  `inverseSurface`, all the `*Fixed` roles, `surfaceDim`/`surfaceBright` etc. on M3's uncustomized
  defaults).
  - **Also flipped the app's default theme mode**: `Storage.useDynamicColor` and `AppTheme`'s own
    `dynamicColor` parameter both defaulted to `true` (defer to the system's Android 12+
    wallpaper-derived color on every device that supports it) - changed both defaults to `false`
    so a fresh install actually shows the new forest theme rather than rarely surfacing it. Users
    can still opt into dynamic color from Settings; existing installs with an already-stored
    preference are unaffected either way (this only changes what *new* installs default to).
    Fixed one stale bit of Settings copy in the same pass - the dynamic-color toggle's description
    still said "Turn off to use the Unraid orange theme" (accurate before this session; wrong now
    on two counts, both the color and the "off = fallback" framing).
  - **Follow-up, same session**: the user noticed the wink's gold never actually showed up
    anywhere in real app UI, and that the Dashboard's Memory metric card had lost the visual
    distinction it used to have from the CPU card (both used to be different hues - blue vs. the
    old Unraid orange's secondary pairing - but the new secondary "moss" green sits too close to
    primary pine to tell the two cards apart at a glance). One fix solves both:
    `SystemMetricsRow`'s Memory `MetricCard` now uses `colorScheme.tertiary` (gold) instead of
    `colorScheme.secondary` (moss) - CPU stays pine green, Memory is now gold, and the brand's
    signature accent color is visible on the very first screen instead of buried in the rarely-
    seen Docker/VMs "Paused" dot. `colorScheme.secondary` is now unused in real app UI (only
    referenced in `Theme.kt`'s own preview/demo composable) - not a problem, plenty of M3 apps
    lean mainly on primary+tertiary and don't force secondary into service everywhere.
  - **Follow-up, same session: audited every other "paused" indicator in the app for the same
    gold convention and found it was applied inconsistently.**
    - `VmsScreen.kt`'s `VmRunState.dotColor()` had its own hardcoded `Color(0xFFFFA726)` for
      `PAUSED` - a different amber than the theme's actual gold, and not theme-aware at all (was a
      plain non-`@Composable` function; made it `@Composable` so it can read
      `MaterialTheme.colorScheme.tertiary` directly). Docker's equivalent dot (on the Dashboard's
      mini widget) already used `colorScheme.tertiary` for the same state - this just brings VMs
      in line with it.
    - The Docker **tab's own** container list (`ContainerRow`/`ContainerInfoCard` in
      `DockerScreen.kt`) never distinguished paused at all - its status dot was a plain
      `isRunning ? green : error`, so a paused container showed the same red as a stopped one.
      The Dashboard's mini Docker widget already got this right; the full tab's list didn't.
      Both now branch on all three `DockerContainerState` values, matching the Dashboard widget.
    - Parity check had no color cue for paused vs. running anywhere - both `MainScreen.kt`'s
      `ParityCheckControls` and `DashboardScreen.kt`'s `ParityCheckBanner` now tint their
      `LinearProgressIndicator` (and the Dashboard banner's percentage label) tertiary while
      paused, primary while running.
    - **Considered and explicitly declined**: using tertiary for the bold header icon
      (`ScreenTitle`, every tab's leading icon) too. Gold's meaning up to this point is
      specifically "paused/waiting" - making it the ambient color of every screen's chrome as
      well would dilute that signal (a gold dot in a list means something; a gold header icon on
      every single tab wouldn't mean anything in particular, just decoration) and doesn't fit the
      brand mark's own logic either, where the header icon's role is closer to what ink/pine (the
      case) represents, not the wink's accent. Headers stay on primary.

1. **Server tab** (Connect identity) - lower priority per the monitoring-and-quick-actions
   product framing discussed this session; genuinely optional depending on how much you use
   Unraid Connect.
2. **Array topology editing** (add/remove/mount/unmount a disk) on the Main tab, if wanted -
   deliberately left out this session as a riskier, separate feature from monitoring + basic
   array/parity-check control (see "Known limitations" above).
3. **VM reset** (`VmMutations.reset`) on the VMs tab, if wanted - deliberately left unwired when
   the VMs tab was built out (see "What's actually working" above); a hard power-cycle-equivalent
   action with no Docker analog, not asked for at the time.
4. **Clean up the now-dead legacy per-density launcher PNGs** (`mipmap-hdpi/ic_launcher*.png`
   etc.) - unreachable now that the vector adaptive icon is live and `minSdk` already guarantees
   `-v26` support, just never deleted this session (see "App icon / branding exploration" above).

## VM logs and Hibernate - asked for, not possible with the current API

Both investigated and confirmed absent (not just unwired) after the VMs tab was built:

- **VM logs**: no VM-specific logs field exists anywhere in the schema (only Docker has
  `docker.logs`). The schema does have a generic system log-file browser (`logFiles`/`logFile`,
  gated by a `LOGS` resource permission, unrelated to `VMS`) - checked whether that could reach a
  VM's libvirt/qemu log indirectly. It can't: read the actual `unraid-api` source
  (`api/src/unraid-api/graph/resolvers/logs/logs.service.ts`) - `listLogFiles()` does one
  non-recursive `readdir` of `/var/log` only (`isFile()` filters out subdirectories entirely), and
  `getLogFileContent()` normalizes any requested path through `join(logBasePath, basename(path))`
  - `basename()` strips directory components as a path-traversal guard, so even a client-supplied
  `libvirt/qemu/myvm.log` collapses to looking for `/var/log/myvm.log`, which doesn't exist. Real
  VM logs live at `/var/log/libvirt/qemu/<vmname>.log` - one directory level down, structurally
  unreachable through this API. Separately, that log-file query does 2-3 full-file reads per call
  (counts lines, then re-reads to extract a tail - no seeking/caching), so it wouldn't have made a
  good poll-based tail source even if the path issue didn't exist. Would need `unraid-api` itself
  to add a VM-log endpoint mirroring `docker.logs` before this is buildable.
- **Hibernate**: `VmMutations` has exactly 7 fields -
  `start`/`stop`/`pause`/`resume`/`forceStop`/`reboot`/`reset` - no `hibernate`/`suspend`/
  `managedSave` or similar. `VmState` does have a `PMSUSPENDED` value (so the *state* is
  recognized if a VM ever reports it), but nothing in the API can trigger that transition. Same
  shape as the Main tab's missing spin-up/down/mover mutations - a real gap in what Unraid
  exposes, not a client-side oversight. Per established preference (see the Main tab section
  above), not adding a disabled placeholder button for either of these - would need `unraid-api`
  to add the capability first.

## Devices screen (was deferred; built 2026-10-29)

Built as a More-sheet entry (the placement the earlier notes leaned towards). What a real server
returns, and why the filter works the way it does, is in TODO.md #3. The one design point worth
keeping here: the schema has no "core device" flag, so the filter uses the PCI class code
(`typeid`, a fixed PCI-SIG standard) rather than device names - it doesn't need tuning per board.

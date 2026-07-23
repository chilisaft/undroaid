# Undroaid — project notes

Android companion app for Unraid, talking to the Unraid GraphQL API (Apollo Kotlin) over
HTTP + a login-tested API key. MVVM + Hilt + Jetpack Compose. Intended use case (see
"Product direction" below): glance-and-act monitoring/quick-actions, not a full port of the
desktop webUI.

Last updated: 2026-07-23. Branch: `main` (pushed to `origin/main`).

## What's actually working

- **Login** (`ui/login`) — server URL + API token, validated via `TestLoginQuery` (checks
  `info { os { hostname } }`, not the old fragile `server` field), stored in
  `EncryptedSharedPreferences` via `Storage`. Auto-login on relaunch if credentials exist.
- **Dashboard** (`ui/dashboard`) — real data, not mocks:
  - Array status, health (derived from disk statuses), storage used/total
  - CPU + memory load, polled every 1s (`ServerRepository.observeSystemMetricsPoll`) —
    deliberately polling, not a GraphQL subscription; see "Known limitations" below
  - Uptime, ticking client-side every 30s from `info.os.uptime` (boot time)
  - Docker containers, capped to 3 with a "Show all" button → jumps to the Apps tab
    (which is currently just a stub — see Open gaps)
  - Notification unread-count badge on the bell icon
  - Every widget loads/retries independently; a permission failure on one Unraid resource
    doesn't blank the others. Failures distinguish "no permission" from generic errors
    (`data/models/WidgetResult.kt`, `data/api/GraphQlErrors.kt`)
- **Notifications** (`ui/notifications`) — own screen via the bell icon, not on the
  dashboard anymore. Lists up to 50 unread, per-item dismiss + dismiss-all, both via
  `NotificationsRepository`.
- **Settings** (`ui/settings`) — theme (system/light/dark, actually wired into `AppTheme`),
  dynamic color toggle (actually wired), show-core-devices toggle (persists, **no consumer
  yet** — see Open gaps), logout (clears credentials, resets nav stack to login).
- **Navigation** (`graphs/UndroaidNavGraph.kt`) — bottom nav: Dashboard, Main, Apps, VMs.
  A 5th "More" item opens a `ModalBottomSheet` with Shares, Server, Settings (lower-frequency
  screens, not worth a permanent tab slot — see PROJECT chat history for the reasoning).
  Bottom bar hides itself on drill-in screens (Settings/Notifications/Shares/Server), which
  have their own back button instead.

## Open gaps — screens that are stubs

These exist as routes/nav destinations with placeholder text only, no real functionality:

- **Main** (`ui/main/MainScreen.kt`) — intended for array/disk management. Schema has
  `ArrayMutations.setState` (start/stop array), `addDiskToArray`/`removeDiskFromArray`,
  `mountArrayDisk`/`unmountArrayDisk`.
- **Apps** (`ui/virtualization/VirtualizationScreen.kt`, route `"virtualization"`, nav label
  "Apps") — intended for Docker container list + actions. Schema has `Docker.containers`
  (already used for the Dashboard's top-3 preview, just needs the full list here) and
  `DockerMutations.start`/`stop`/`pause`/`unpause`/`removeContainer`, all keyed by
  `id: PrefixedID!` (mapped to `kotlin.String` in `build.gradle.kts`'s apollo scalar config,
  no extra scalar work needed).
- **VMs** (`ui/vms/VmsScreen.kt`) — intended for VM list + actions. Schema has
  `Vms.domains` and `VmMutations.start`/`stop`/`pause`/`resume`/`forceStop`.
- **Shares** (`ui/shares/SharesScreen.kt`) — share list at minimum; full share
  creation/editing is probably desktop-webUI territory, not phone territory.
- **Server** (`ui/server/ServerScreen.kt`) — was going to show Unraid Connect identity
  (owner, guid, WAN/LAN IP, remote URL). The original query for this
  (`ServerInformationQuery`) was deleted when the Dashboard moved off it, since nothing else
  used it — would need a fresh purpose-built query when this screen gets built for real.

**Known inconsistency right now**: Dashboard's "Show all" docker button navigates to Apps,
but Apps is still a stub — so that button currently leads to a placeholder screen. Worth
fixing whenever Apps gets built.

## Known limitations / things worth verifying against a real server

I don't have a live Unraid server or a persistent Android emulator in my sandbox this whole
session — everything below is compile + JVM-unit-test verified only, not manually confirmed
against real Unraid data. The user has been testing on their own device.

- **Permission-denied detection** (`data/api/GraphQlErrors.kt`) is a heuristic — checks
  `extensions.code` for FORBIDDEN/UNAUTHORIZED/etc, falls back to message keyword matching.
  The Unraid API schema documents *which* permission each field needs but not the actual
  error shape on failure. Worth testing with a genuinely restricted API key to confirm the
  soft-fail UI ("Your API key doesn't have permission...") actually triggers correctly.
- **WebSocket subscriptions were tried and abandoned** for CPU/memory - `ApolloClient`'s
  WebSocket transport is correctly wired (`data/api/GraphQlServiceModule.kt`, explicit
  `httpEngine`/`subscriptionNetworkTransport` construction — the `.okHttpClient()`
  convenience shortcut crashes if combined with `.webSocketServerUrl()`, see git history),
  but I couldn't verify the actual subscription handshake/protocol against a live server, and
  a subscription's push cadence isn't client-controllable anyway. Switched to a 1s HTTP poll
  instead, which is guaranteed and now has test coverage. If push-based live updates are ever
  wanted (e.g. for array status via `arraySubscription`, or notifications via
  `notificationAdded`), the transport groundwork is already there.

## Test coverage

82 JVM unit tests (`./gradlew testDebugUnitTest`), all passing as of the last commit -
repositories, ViewModels, the formatting utils, the permission-detection heuristic, and one
test (`GraphQlServiceModuleTest`) that actually constructs the `ApolloClient` the way Hilt
does, specifically because a prior version of that wiring compiled fine but crashed at
runtime on every launch (see git log). No Compose UI tests exist - previews only.

## Repo/branch housekeeping

- Renamed `master` → `main` this session (local + pushed to `origin/main`).
- `origin/master` on GitHub is **still the default branch** - the user needs to switch it in
  GitHub Settings → Branches themselves (I don't have `gh` CLI access), then can delete the
  old `origin/master`.

## Suggested next steps, roughly in priority order

1. **Docker container actions on the Apps tab** - list all containers (reuse
   `DockerStatusQuery`/`ServerRepository.getDockerContainers()` pattern) + start/stop/restart
   via `DockerMutations`. Fixes the Dashboard's "Show all" dead-end at the same time.
2. **VM list + actions** on the VMs tab, same shape as Docker.
3. **Array start/stop** on the Main tab - lowest-hanging "quick action" left, and array
   health is already the Dashboard's headline stat, so this closes the loop.
4. **Wire up "show core devices"** once Main has an actual device list to filter.
5. **Server tab** (Connect identity) - lower priority than the above three per the
   monitoring-and-quick-actions product framing discussed this session; genuinely optional
   depending on how much you use Unraid Connect.
6. **GitHub housekeeping** - switch default branch, delete old `origin/master` (user's own
   todo).

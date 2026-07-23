# Undroaid — project notes

Android companion app for Unraid, talking to the Unraid GraphQL API (Apollo Kotlin) over
HTTP + a login-tested API key. MVVM + Hilt + Jetpack Compose. Intended use case (see
"Product direction" below): glance-and-act monitoring/quick-actions, not a full port of the
desktop webUI.

Last updated: 2026-07-23. Branch: `main` (pushed to `origin/main`).

**Session update (same day)**: Built out the Docker tab (see below) and did some git housekeeping -
removed a leftover detached-HEAD worktree and 3 fully-merged duplicate branches, fixed a dangling
`origin/HEAD`, and pushed 2 unpushed `main` commits. `origin/master` is confirmed gone (see
"Repo/branch housekeeping" below).

## What's actually working

- **Login** (`ui/login`) — server URL + API token, validated via `TestLoginQuery` (checks
  `info { os { hostname } }`, not the old fragile `server` field), stored in
  `EncryptedSharedPreferences` via `Storage`. Auto-login on relaunch if credentials exist.
- **Dashboard** (`ui/dashboard`) — real data, not mocks:
  - Array status, health (derived from disk statuses), storage used/total
  - CPU + memory load, polled every 1s (`ServerRepository.observeSystemMetricsPoll`) —
    deliberately polling, not a GraphQL subscription; see "Known limitations" below
  - Uptime, ticking client-side every 30s from `info.os.uptime` (boot time)
  - Docker containers, capped to 3 with a "Show all" button → jumps to the Docker tab, each
    row showing its real icon (same `ContainerIconBadge` component as the Docker tab)
  - Notification unread-count badge on the bell icon
  - Every widget loads/retries independently; a permission failure on one Unraid resource
    doesn't blank the others. Failures distinguish "no permission" from generic errors
    (`data/models/WidgetResult.kt`, `data/api/GraphQlErrors.kt`)
- **Docker** (`ui/docker`, route `"docker"`, formerly the "Apps"/`virtualization` stub - renamed
  since it's specifically the Docker container list, not general virtualization) — full container
  list via `DockerRepository.getContainers()` (`DockerContainersQuery`, richer than the Dashboard's
  summary query). Each row shows the container's real icon (`iconUrl`, loaded via Coil3
  `SubcomposeAsyncImage` at 52dp, falls back to a generic layers glyph when there's no icon or it
  fails to load) plus name and running/stopped status - no image/tag text in the row anymore
  (removed per user feedback, wasn't useful at a glance). Tapping a row opens a
  `ModalBottomSheet`: a `ContainerInfoCard` at the top (bigger 56dp icon, name, status/uptime text)
  followed by context-appropriate actions - **Restart** (new, RUNNING/PAUSED only),
  Pause/Resume, Stop, Start, plus "Open Web UI" when `webUiUrl` is set (launched via
  `LocalUriHandler`). Restart has no native mutation in the schema (only
  start/stop/pause/unpause/removeContainer/updateContainer(s)), so `DockerViewModel.restart()`
  chains `stopContainer` then `startContainer` and always refreshes afterward (even if start
  fails after a successful stop, the container really is stopped now, so the list needs to catch
  up) - see its doc comment. The row still shows a spinner while its mutation is in flight
  (`actioningIds`, same shape as Notifications' `dismissingIds`).
- **Notifications** (`ui/notifications`) — own screen via the bell icon, not on the
  dashboard anymore. Lists up to 50 unread, per-item dismiss + dismiss-all, both via
  `NotificationsRepository`.
- **Settings** (`ui/settings`) — theme (system/light/dark, actually wired into `AppTheme`),
  dynamic color toggle (actually wired), show-core-devices toggle (persists, **no consumer
  yet** — see Open gaps), logout (clears credentials, resets nav stack to login).
- **Navigation** (`graphs/UndroaidNavGraph.kt`) — bottom nav: Dashboard, Main, Docker, VMs.
  A 5th "More" item opens a `ModalBottomSheet` with Shares, Server, Settings (lower-frequency
  screens, not worth a permanent tab slot — see PROJECT chat history for the reasoning).
  Bottom bar hides itself on drill-in screens (Settings/Notifications/Shares/Server), which
  have their own back button instead.

## Open gaps — screens that are stubs

These exist as routes/nav destinations with placeholder text only, no real functionality:

- **Main** (`ui/main/MainScreen.kt`) — intended for array/disk management. Schema has
  `ArrayMutations.setState` (start/stop array), `addDiskToArray`/`removeDiskFromArray`,
  `mountArrayDisk`/`unmountArrayDisk`.
- **VMs** (`ui/vms/VmsScreen.kt`) — intended for VM list + actions. Schema has
  `Vms.domains` and `VmMutations.start`/`stop`/`pause`/`resume`/`forceStop`.
- **Shares** (`ui/shares/SharesScreen.kt`) — share list at minimum; full share
  creation/editing is probably desktop-webUI territory, not phone territory.
- **Server** (`ui/server/ServerScreen.kt`) — was going to show Unraid Connect identity
  (owner, guid, WAN/LAN IP, remote URL). The original query for this
  (`ServerInformationQuery`) was deleted when the Dashboard moved off it, since nothing else
  used it — would need a fresh purpose-built query when this screen gets built for real.

## Known limitations / things worth verifying against a real server

I don't have a live Unraid server or a persistent Android emulator in my sandbox this whole
session — everything below is compile + JVM-unit-test verified only, not manually confirmed
against real Unraid data. The user has been testing on their own device.

- **Permission-denied detection** (`data/api/GraphQlErrors.kt`) is a heuristic — checks
  `extensions.code` for FORBIDDEN/UNAUTHORIZED/etc, falls back to message keyword matching.
  The Unraid API schema documents *which* permission each field needs but not the actual
  error shape on failure. Worth testing with a genuinely restricted API key to confirm the
  soft-fail UI ("Your API key doesn't have permission...") actually triggers correctly.
- **Docker start/stop/pause/unpause, icons, and Web UI links** (`DockerRepository`,
  `ServerRepository`, `ui/docker`, `ui/dashboard`, shared `ui/components/ContainerIconBadge.kt`) -
  compile + JVM-unit-test verified only, same caveat as everything else in this section: no live
  Unraid server or persistent emulator in this sandbox to confirm the mutations actually work
  against a real Docker daemon, that `iconUrl` actually resolves to a loadable image for real
  containers (Dashboard preview and Docker tab both use it now), or that `webUiUrl` opens the
  right thing via `LocalUriHandler`.
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

96 JVM unit tests (`./gradlew testDebugUnitTest`), all passing as of the last commit -
repositories, ViewModels, the formatting utils, the permission-detection heuristic, and one
test (`GraphQlServiceModuleTest`) that actually constructs the `ApolloClient` the way Hilt
does, specifically because a prior version of that wiring compiled fine but crashed at
runtime on every launch (see git log). No Compose UI tests exist - previews only.

## Repo/branch housekeeping

- Renamed `master` → `main` this session (local + pushed to `origin/main`). `origin/master` is
  confirmed deleted on GitHub as of this session's `git fetch --prune` - the default-branch
  switch + old-branch deletion is done, nothing left to do here.
- This session also cleaned up local git clutter left over from prior sessions: removed a
  detached-HEAD worktree (`undroaid-android-review-def47e`) and 3 branches that were fully
  merged into `main` but never deleted, fixed a dangling `origin/HEAD` (now points at `main`),
  and pushed 2 commits that were sitting unpushed on local `main`.

## Suggested next steps, roughly in priority order

1. **VM list + actions** on the VMs tab, same shape as the Docker tab (`ui/docker`) built this
   session - reuse that as the template: `Vms.domains` for the list,
   `VmMutations.start`/`stop`/`pause`/`resume`/`forceStop` for actions.
2. **Array start/stop** on the Main tab - lowest-hanging "quick action" left, and array
   health is already the Dashboard's headline stat, so this closes the loop.
3. **Wire up "show core devices"** once Main has an actual device list to filter.
4. **Server tab** (Connect identity) - lower priority than the above three per the
   monitoring-and-quick-actions product framing discussed this session; genuinely optional
   depending on how much you use Unraid Connect.

# Native input and private network checks

This Windows-only QA add-on uses hidden AWT canvases, a client and two sequential integrated-server hosts. The real TCP listeners bind only `127.0.0.1` on automatically allocated ports. It does not publish LAN discovery, touch the user instance, inject OS input, or contact a public server.

Prerequisites: the pinned game/launcher bundle has been built from committed game sources, and `.reports/native-menu-qa` contains the existing isolated, prepared runtime and `settings.json`. An unprepared clone intentionally fails rather than downloading game files or finding a user's Minecraft installation.

From the repository root:

```powershell
./scripts/game.ps1 -Tasks @('remapInputNetworkQa','--init-script','../scripts/qa/input-network/init.gradle','--offline')
node scripts/qa/input-network/run.mjs
```

The runner links immutable runtime artifacts into its own second QA instance, installs the pinned mod in both QA instances, temporarily sets QA options, and restores those options after the processes finish. Worlds/configuration created in the two QA instances are test data. Concurrent runs of this runner are rejected by `.reports/input-network-qa/run.lock`; do not run another task against these same QA instances concurrently.

The Java driver temporarily writes the **test process's** LWJGL held-key buffer and event fields, then calls the real Minecraft handler. Virtual in-game focus is independent of Windows focus; `closeScreen` is guarded so the mouse cannot be grabbed. This validates module/event integration, not physical keyboard, mouse movement, Alt+Tab or device delivery.

Coverage: all 29 gameplay toggle shortcuts, 26 HUD text samples on a real network world, FreeLook held/menu/focus/release, Zoom reset/precedence, RightShift duplicate handling, ToggleSprint's client and server state, Auto Text repeat/release/cooldown/menu/focus guards with messages actually received by the host, native hurt/death packets and one sound cue, reconnect and change to a second host/world. A deliberately queued entity-status task is closed before execution to exercise the stale-packet regression. Healthy connections still deliver login, movement, chat and entity packets.

Each run has a fresh ignored folder, referenced by `.reports/input-network-qa/latest-run.json`. Acceptance requires `result.json`, a positive closed-play-packet count, three exited processes, and no ERROR/FATAL in their saved logs. Inspect the generated captures as well. Initial diagnostics and repeat failures remain in their own run folders.

Physical controls, public online-mode servers, latency/loss conditions, anticheat, shaders/resource packs and the full per-feature multiplayer behavior of every module require additional qualification.

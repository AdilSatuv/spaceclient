# Native HUD observations

Test-only hidden Canvas helper, outside the product JAR. No OS input, user instance, network account, or visible window. It requires the repository's pinned JDK/loader and the coordinator's own prepared `.reports/menu-help-runtime` cache. It hardlinks immutable libraries/assets/natives and creates new worlds/config/mod directories for every JVM.

```powershell
& ./scripts/game.ps1 -Tasks @('--no-daemon','--init-script','../scripts/qa/performance/init.gradle','remapNativePerf')
# Wait for the single-use Gradle process to finish before measuring.
node scripts/qa/performance/run.mjs --smoke
node scripts/qa/performance/run.mjs
```

The runner refuses an existing run lock or another Java process at trial boundaries. Coordinate a quiet window with other agents and do not run builds, installer tests, browsers, or game QA during measurements. An abandoned lock is evidence to inspect, not permission to start a second series. Each failed attempt and its raw log remains under `.reports/native-perf-qa`.

Four profiles: Minecraft + pinned Fabric + this identical helper without Space; Space with global HUD off; Space with the five default HUD modules; Space with all 26 HUD modules. All 29 gameplay modules are disabled in the three Space profiles. Wings, halo and public appearance are absent. Heavy means HUD density, not every gameplay effect or cosmetics stress. The control is **not pure vanilla**.

Every trial is a fresh JVM (-Xms256M/-Xmx1024M), flat creative peaceful world, seed 54321, first person, empty inventory, 1280×720, GUI2, render distance4, Vsync off. A server-side teleport establishes [0.5,4,0.5,0,0]; movement invalidates sampling. Time is fixed at1000 and mob spawning disabled. Three seconds of stable-position warmup precede six seconds of sampling. The full series runs three repetitions per profile in rotated/reversed order; smoke is excluded. The 260 return value is Minecraft1.8.9's unlimited sentinel, not an enforced260FPS cap.

The helper counts render callbacks and intervals with System.nanoTime and waits for GL completion using glFinish in every profile. That synchronization changes the workload. These are hidden render-loop observations, **not ETW/PresentMon, displayed-frame percentiles, sustained play, or competitive FPS promises**. GameRenderer callback duration includes glFinish; interval duration also includes the rest of the loop. Per-trial means/medians/p95/p99 are preserved; group summaries are medians across three independent trials, not pooled percentiles.

Startup is Node's monotonic time from JVM spawn to the stdout marker at the first TitleScreen render callback return. It includes initialization and pipe delivery, with warm OS caches; it is not cold boot, launcher download time, visible presentation, or time to a usable world. Working/private/peak working-set bytes come from Get-Process **after** the timed sample and JSON/statistics allocation. Peak includes startup; heap is a separate JVM observation. Neither is an in-sample RAM trace or retained-heap measurement. Do not infer a small memory regression from these snapshots.

Reports carry source tree/JAR SHA, actual GPU/driver string, position, enabled HUD list, dimensions, frame budget and raw logs. Validate those and coordination timestamps before comparing results. A successful helper exit alone does not qualify a benchmark; discard scene drift, overlapping work, errors and incomplete runs explicitly.

Per-trial `median` uses the lower central order statistic for an even frame count; P95/P99 use nearest rank. Three-trial group medians have an odd count. Disabling mob spawning after joining does not remove animals already generated during world creation. The controlled camera does not make this an entity-heavy or exhaustive scene.

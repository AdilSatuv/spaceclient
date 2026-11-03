# Native menu theme QA

Hidden Canvas, fresh private runtime/world/config for every run, no desktop input. Immutable libraries/assets/natives are hardlinked from the coordinator's own prepared `.reports/menu-help-runtime`. Nothing is copied from the user's instance. The helper is not included in the product JAR.

```powershell
& ./scripts/game.ps1 -Tasks @('--no-daemon','--offline','--init-script','../scripts/qa/menu-theme/init.gradle','remapSettingsQa')
node scripts/qa/menu-theme/run.mjs
```

The runner verifies the actual loaded game tree, exact JAR hash, exit0, GL errors, three actual native slider drag/release callbacks and native logs. A new UUID output directory retains every failed attempt. The helper opens the real native screens; a test-only redirect disables creation/start of the LAN multicast detector for the empty multiplayer list. No server connection is submitted, no LAN world is published, no desktop window or OS input is used.

Coverage: title/focus, native settings/video/controls/language/sound/skin/chat/resources, world creation/flat/custom presets, server forms/confirmation, grid/list and all seven Space categories, settings tabs, cosmetics, pause/difficulty lock, LAN form, achievements/statistics, HUD editor/arrange, old mod controls and waypoints. EN1280×720/GUI2, RU854×480/GUI2 and RU1280×720/GUI3. Actual OptionSlider, SoundButton and generic SliderWidget updates/release semantics are checked while their new theme renders. Native lock is shown unlocked and disabled/locked. Inspect captures to catch clipping, opacity, compact labels and header/footer issues; successful exit alone does not establish visual quality.

This is native handler/render QA. It does not establish physical mouse/keyboard behavior, assistive technology, third-party screens/resource packs, all DPI combinations, live Realms/streaming services or public release readiness. The HUD editor intentionally uses a world overlay so widget coordinates retain their real context. Scope the final report to the exact run and JAR rather than an earlier successful candidate.

The current matrix also captures Aurora, Ember and Crystal on the real in-world player preview, verifies each saved choice, and enables an Orbit halo only in the private QA preferences. Production defaults and user cosmetics remain unchanged. The graphite HUD/settings/editor appearance follows the user's correction; native menu controls keep their orbital theme.

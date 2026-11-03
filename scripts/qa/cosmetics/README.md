# Native cosmetic checks

Windows, local JDK21, committed game sources and the existing isolated `.reports/menu-help-runtime` cache are required. The runner copies only immutable libraries/assets/version/native files into its own `.reports/cosmetics-runtime`. It never uses a user's Minecraft folder. Missing caches fail without downloads.

```powershell
./scripts/game.ps1 -Tasks @('test','build','remapSettingsQa','-I','../scripts/qa/cosmetics/init.gradle','--offline','--no-daemon')
node scripts/qa/cosmetics/run.mjs slim
node scripts/qa/cosmetics/run.mjs classic
```

Run sequentially. The helper attaches LWJGL to an invisible AWT canvas and guards `closeScreen` against mouse capture. It creates an isolated flat world, validated public appearance fixtures and cosmetic preferences. No session, credentials, network server, OS input or visible window is used.

Checks cover classic64×32/slim64×64 dispatch, local-only skin/cape hooks and vanilla fallback, one feature per renderer constructor, wings/halo rendering, visibility, RU/EN scale2/3 settings, keyboard traversal and unobscured focused rows. Each model produces 17 screenshots plus JSON with the loaded source-tree stamp and exact JAR SHA-256 in `.reports/cosmetics-qa`. Inspect the screenshots and native logs; a completed process alone is insufficient. The preview uses Minecraft's real player renderer, including its native cape animation.

Physical F5/input delivery, public-server cosmetics visible to other clients, third-party resource packs/shaders and a full performance qualification are outside this check.

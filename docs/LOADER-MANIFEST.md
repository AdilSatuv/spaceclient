# Закреплённый Legacy Fabric manifest

Цель: Windows x64, Minecraft 1.8.9, Fabric Loader **0.18.3**, как в game/gradle.properties. Новые loader-версии из списка latest автоматически не выбираются. Main class в нашем коде фиксирован: `net.fabricmc.loader.impl.launch.knot.KnotClient`; удалённые launch arguments не исполняются.

## Происхождение

Изучен [профиль Legacy Meta](https://meta.legacyfabric.net/v2/versions/loader/1.8.9/0.18.3/profile/json), API описан в [репозитории Legacy-Fabric/legacy-meta](https://github.com/Legacy-Fabric/legacy-meta). Обычный Fabric Meta для этой игровой версии возвращает `no mappings version found`; используется Legacy intermediary `net.legacyfabric:intermediary:1.8.9`.

Profile JSON генерирует time/releaseTime при запросе. Его байтовый hash не закрепляется как постоянный документ: вместо сетевого resolver при каждой установке исходники содержат reviewed snapshot артефактов в `loader-libraries.ts`. Для каждого записаны точные URL, размер, SHA-1 и SHA-256. Hashes сверены с metadata/sidecar и фактически скачанными байтами. Это content pins, не подпись издателя.

Legacy Maven URL из профиля перенаправляет на `https://repo.legacyfabric.net/legacyfabric/…`. Canonical адрес проверен отдельно и закреплён; production downloader по-прежнему не следует redirects. Fabric Maven разрешён только для известных pins. Нельзя передать произвольную библиотеку/версию с этих hosts из renderer или remote metadata.

## Состав и объединение

- Пять ASM 9.9 artifacts, sponge-mixin `0.16.5+mixin.0.8.7`, intermediary 1.8.9, loader 0.18.3.
- LWJGL и LWJGL util **2.9.4+legacyfabric.17**, а также соответствующий Windows native-архив. В профиле у lwjgl-platform нет обычного jar: скачивается Windows classifier.
- Три исходных Mojang LWJGL entries исключаются целиком вместе с native-архивом. Старые Log4j также исключаются и заменяются отдельным controlled override. Native jar не попадает в classpath.
- Знак `+` допустим в безопасном Windows filename и нужен фиксированным версиям Mixin/LWJGL; path traversal, device names и прочие проверки сохраняются.

Итого loader snapshot: 11 артефактов, 7 734 742 байта. После объединения с Mojang/Log4j: 767 уникальных файлов, 153 351 771 байт, 39 classpath entries, четыре native-архива. Patched LWJGL содержит шесть DLL, в том числе несколько архитектур; полный набор даёт 16 DLL. Наш target остаётся Windows x64; наличие ARM DLL внутри общего архива не означает поддержку ARM-лаунчера.

## Проверки и пределы

```powershell
node apps/launcher/scripts/smoke-loader.mjs
```

Production downloader проверил все 11 pins обоими digest. На JDK 21 собственная smoke-программа загрузила определения ClassReader, MixinBootstrap, FabricLoader и KnotClient через `Class.forName(..., false, ...)`. Из intermediary jar прочитан header `v1 / official / intermediary`. Шесть patched LWJGL DLL распакованы с CRC/размером/SHA-256. [Отчёт](qa/launcher-loader-2026-10-23.json).

`initialize=false` принципиален: эта проверка не запускает loader или Minecraft и не загружает DLL. Через окно подготовлен весь набор; [аудит](qa/launcher-base-2026-10-23.json) прошёл. [Remapped Space jar](GAME-BUNDLE.md) теперь включён в development-сборку. Полная совместимость проверяется после Microsoft/ownership flow; `launchReady` остаётся false.

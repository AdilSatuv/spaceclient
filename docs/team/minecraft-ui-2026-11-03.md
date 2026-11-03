# Minecraft UI: подписи, клавиатура и build ID · 3 ноября 2026

Завершён локальный небольшой UI-срез от `0231800a960c6195bd528c040053607dbd2bd6ac`. Копия: `C:/Users/user/Documents/Codex/2026-11-03/space-client-minecraft-ui-agent/work/spaceclient-minecraft-ui`, ветка `feat/menu-help-navigation`. Это отдельный clone, не worktree. Основная копия и пользовательский runtime не изменялись.

Кодовые коммиты для последовательного переноса: `cc7a3ab` и `9195423`. Этот документ — отдельный документационный коммит после QA. Push, main, deploy, auth и установщик не выполнялись.

Изменённое поведение:

- Служебная оболочка каталога и общих настроек переключается RU/EN: разделы, поиск, фильтры, пустые состояния, клавиатурная помощь, вкладки HUD, назад/сброс/редактор, autosave/read-only и On/Off. Другие языки используют английский fallback. Названия модулей, их индивидуальные строки параметров, описания и прежние сообщения валидации остаются прежними, преимущественно английскими; полной локализации 55 модулей этот срез не заявляет.
- Недоступная настройка показывает `Unavailable` / `Недоступно`, read-only — отдельную подпись. Подсказка объясняет причину и не предлагает менять значение. Отключённый модуль остаётся настраиваемым; недоступность строки не приравнивается к выключенному модулю.
- Left/Right меняют числовое значение или вариант, а на вкладках переходят между вкладками. Стрелки на action/text/toggle и footer больше не запускают действие. Enter/Space сохраняют активацию, Tab/Shift+Tab и Up/Down — обход. Фокус недоступной строки сохраняется для чтения причины; запись блокируется прежним guard.
- Подписи поиска, кнопок и sidebar ограничены их областями. По первому native просмотру исправлены обрезанное русское «Настройки» и наложение build ID на декоративную линию минимального меню.
- Главное меню показывает `dev-<первые 12 hex gameSourceTree>` из `/spaceclient-build.properties`. `BuildIdentity` принимает schemaVersion=1/channel=development и 40 lowercase hex. При отсутствии/ошибке/чужой схеме — `dev-unknown`. Timestamp, system/env и modVersion не подменяют tree. Метка не объявляется подписью или verified-статусом.

Production scope: `BuildIdentity.java`; `menu/MenuStrings.java`, `menu/MenuTheme.java`; `hud/HudModulesScreen.java`, `hud/HudModuleSettingsScreen.java`; `settings/ModuleSettingsScreen.java`; обе `assets/spaceclient/lang/*.lang`. UI tests: `BuildIdentityTest`, `menu/MenuStringsTest`, дополнения `settings/SettingsInputTest`. Новых модов, action/card IDs, schema/runtime/network/mixin изменений нет. Левая композиция, космический фон и smooth switches сохранены.

**Единственный новый production class для required-classes контракта:** `dev/spaceclient/BuildIdentity.class`. Новых вложенных production classes нет. Class major 52 (Java 8). Существующие 36 production mixins не менялись.

Проверки:

- **278 Java tests, failures=0/errors=0**, включая шесть новых тестов. Проверены malformed/missing stamp, fallback/паритет языков, ширина компактных подписей, неактивация стрелками и объяснение недоступности.
- `scripts/game.ps1` с собственными `.tools/jdk-21` и `.tools/gradle-cache`: `test build` offline прошли. Source закоммичен до финальной сборки. Gradle 9.4.0, Loom/Legacy Looming 1.16.1, mappings build 604, Minecraft 1.8.9/Fabric loader 0.18.3 сохранены. Launcher/website тесты этим UI-срезом не перезапускались.
- Native QA **exit 0**, **32 PNG**, **845 вызовов клавиатурных методов**, Tab-обход всех 29 игровых и 26 HUD экранов в компактной геометрии; проверена видимость фокуса целыми строками. Каталог сохраняет 55 модулей. Проверены стрелки вкладок, Home/End/Right числового поля, unavailable anchor/tint и побайтная сохранность future-schema файла при попытке изменения.
- EN на hidden Canvas 1280×720: реальные scaled размеры 640×360/2x и 427×240/3x. При RU на том же Canvas запрос GUI 3 даёт фактические 640×360/2x; это записано отдельно. Для настоящего compact RU собственный невидимый Canvas уменьшен до **854×480**, получены **427×240/2x** и отдельные title/catalog/disabled captures. Затем Canvas восстановлен.
- Минимальный themed title дополнительно отрисован после явного `Screen.init(...,240,220)` в собственном framebuffer. Это native render геометрии 240×220, **не отдельное физическое окно** такого размера. Вне этой области тестовый framebuffer очищен чёрным. ID не пересекает brand/buttons; уменьшенная метка имеет отдельную подложку.
- В итоговом native журнале **0 ERROR/FATAL**. Миры не создавались и не открывались. Игра работала только в hidden AWT Canvas; OS keyboard/mouse/фокус и Computer Use не применялись. Итоговый собственный Java завершился штатно.

Все 32 финальных кадра действительно просмотрены в четырёх contact sheets. Полноразмерно дополнительно просмотрены: `en_US-3-title`, `ru_RU-compact-title`, `ru_RU-compact-catalog`, `ru_RU-compact-disabled`, `readonly`, `title-minimum-240x220`. Первый успешный диагностический прогон до визуального уточнения сохранён отдельно в `.reports/menu-help-qa/initial-success`; его старый JAR не используется для итоговой приёмки.

| Свойство итогового JAR | Значение |
|---|---|
| Game source tree (`HEAD:game`) | `92a2bd0a8b78f1ea6dbdacc55b3370e78dfbfb64` |
| Видимая метка | `dev-92a2bd0a8b78` |
| SHA-256 | `603d038573f6aa5903a44e315bf5aded90f17a3fe6faf05ab50b6748a4bb535a` |
| Размер | 1 082 361 bytes |

Runner копирует именно `game/build/libs/space-client-0.1.0.jar` в собственный QA профиль и сравнивает tree, прочитанный из загруженного JAR, с `git rev-parse HEAD:game`. `.reports/menu-help-qa/summary.json`, `run-identity.json`, `native.log`, `build-final.log` и `.reports/menu-help-runtime/qa-screen-dimensions.json` содержат исходные наблюдения. QA addon и подготовленные runtime/cache остаются ignored. QA source/runner/init/resources и PNG включены в отдельный пользовательский QA ZIP; runtime/библиотеки/миры в ZIP не входят.

Повторение при наличии своего скопированного QA runtime:

```powershell
# Per-command safe.directory может понадобиться при иной Windows identity.
./scripts/game.ps1 -Tasks @('test','build','remapSettingsQa','--init-script=../.reports/menu-help-qa/init.gradle','--offline','--no-daemon')
node .reports/menu-help-qa/run.mjs
```

Codebase Memory: запрошен **отдельный** индекс `spaceclient-menu-help-20261103` для этой копии. Сервис отклонил root как outside allowed root и предложил `allow-root`. Индекс не создан, успешная индексация/coverage не заявляются; global allowlist не менялся. Затронутые классы, вызывающие экраны, schema/runtime границы и build resources проверены напрямую по исходникам. Использован `ui-ux-pro-max`: поиск visible keyboard focus/disabled navigation и применимые desktop-рекомендации.

Диагностические сбои не скрыты: скопированные Gradle locks заставляли Loom перестраивать cache; mappings восстановлены в собственной копии без locks. Sandbox запрещал Java читать JAR, поэтому успешные Java/native проверки выполнены с разрешённым внешним запуском и только process-scoped safe.directory. Начальная проверка ширины нашла слишком тесную подпись; после уточнения все тесты проходят. Первый native sandbox child не дошёл до UI из-за AccessDenied; завершён только собственный аварийный PID после проверки executable/root/addon. Итоговый runner отключает Fabric error GUI. Эти попытки не считаются приёмкой.

Осталось вне приёмки: физические устройства/V/C/Alt+Tab (ранее отложены пользователем, повторный запрос не отправлялся), полноценная игровая/multiplayer матрица 55 модулей, screen reader, shader/resource-pack/performance matrix и полный перевод индивидуальных параметров. Сетевой guard не менялся. Интеграция нового class в строгий launcher contract остаётся у ведущего; установленная пользовательская сборка здесь не обновлялась.

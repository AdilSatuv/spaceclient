# Minecraft UI wave 2 · 3 ноября 2026

Завершён локальный UI-срез от foundation `cf20fc1c134f53848623097ff297d459640a6d44`. Собственный clone: `C:/Users/user/Documents/Codex/2026-11-03/space-client-minecraft-ui-agent/work/spaceclient-minecraft-ui`, ветка `feat/hud-views-loading`. README/общий handoff/лаунчер/сайт/пользовательские процессы и инстансы здесь не менялись. Push/main/deploy/auth не выполнялись.

Исходные косметические зависимости координатора перенесены без своих изменений: `b742722` → local `6e2542e`, `ed6ff6c` → `f5a5b23`, `bde4fbc` → `5f3c6ac`. Если исходные commits уже находятся в основной ветке, повторно переносить эти зависимости не нужно.

Собственные source commits для последовательного переноса:

1. `e93e24f96daf0409e1e0eb23e175f55fa324bb64` — виды каталога, групповой редактор, loading UI, локализация и маршрут косметики.
2. `8940d91bdb2192c94e875971621866cd6bb15486` — собственные keyboard overrides Connect/Disconnected; найдено настоящим native QA.
3. `679529b8a961d58d554ef4cd8ee862fe6b3a171b` — числовые пределы X/Y по видимым размерам и точное достижение края через End.
4. `e41ed504cb0dbbe0bfab9819ab4b6c944668d329` — безопасная disabled-строка при исчерпанной оси, без диапазона 0..0; compact Inventory HUD 300%.

Относительно imported cosmetic source `5f3c6ac` это **30 файлов, +857/−89** до данного отчёта. Основные области: `hud/HudModulesScreen`, `HudModulesLayout`, `HudSection`, `ModuleBrowserSettings`, `HudEditorScreen`, новые `HudArrangeScreen`, `HudLayoutOperations`, `HudLayoutHistory`, атомарные UI-команды `HudSettings`, необязательная подпись в `HudRenderer`; `menu/LoadingTheme`, `UiTranslations`, `MenuStrings`, `MenuTheme`; общая shell `settings/ModuleSettingsScreen`; UI mixins/resources и четыре UI test-файла. Cosmetics implementation, network guard, 55 модулей и их action/ID не менялись своим кодом.

**Каталог и предпочтения.** F6 и видимая кнопка переключают grid/cards и list. Grid сохраняет шесть карточек; list использует полную строку, краткое описание, текст состояния, избранное, switch и переход в настройки, до девяти строк на логическом canvas. Поиск, All/Enabled/Favorites, F, Enter/Space, страницы и Tab работают в обоих видах. При переключении сохраняется выбранная карточка/поиск. Отдельный вход «Косметика» вызывает настоящий `new CosmeticsScreen(parent)`; возвращается в каталог с прежним состоянием.

Browser preferences schema **2**, отдельные от HUD/игровых схем: `favorites`, `view`, `brand`, `locked.0`, `locked.1` плюс schemaVersion. V1 читается без изменений файла; первая успешная запись предварительно сохраняет побайтный `.v1-<uuid>.bak`. Тест покрывает v1 fixture, повторное чтение v2, independent profile locks, future/невалидный файл и неудачную запись. Future/нечитаемый browser остаётся read-only; оба browser-controlled поля Lock и Brand в Arrange недоступны. HUD schema **18** и import/export форматы не расширялись.

**Редактор.** Shift+click или Shift+PgUp/PgDn добавляют виджеты, Ctrl+A выбирает видимые. Групповой drag/стрелки сохраняют взаимные расстояния общим clamp у края; Shift даёт точный drag/крупный шаг стрелок, G сохраняет прежнюю сетку/привязки. Новый инспектор **E** содержит X/Y и scale основного виджета, Lock, Show/Hide, шесть выравниваний и распределение по горизонтали/вертикали. Lock хранится отдельно для каждого HUD-профиля, защищает position/scale; Show/Hide использует старые module flags профиля. Группа и undo-session не являются новым сериализуемым форматом профиля.

Ctrl+Z/Ctrl+Y и кнопки инспектора дают до **32** layout-only снимков позиции/масштаба. Стек сдвигается только после успешной атомарной записи; новая правка удаляет redo. При profile switch/import/reset история очищается. Visibility, browser locks/brand и содержимое/оформление HUD не входят в undo; это указано в подсказке. Batch save восстанавливает полный snapshot при отказе записи, остальные профили сохраняются.

X/Y ограничены размером видимого масштабированного виджета. End ставит его точно у правой/нижней границы; отображение координаты и max используют одинаковое округление. Если после fitting нет места для целой GUI-единицы, числовая модель остаётся валидной технически 0..1, строка disabled и объясняет причину. Общий invariant `SettingNumber` не менялся.

**Watermark и язык.** Постоянный крупный `SPACE CLIENT` убран. Маленькая подпись `SPACE` справа внизу включается отдельно в Arrange и по умолчанию выключена. Build ID/бренд/credits главного меню сохраняются. Добавлен presentation-only RU phrase catalog из **191** фразы: все 55 кратких catalog summaries, основные индивидуальные названия/параметры и общие служебные строки. Неизвестные/новые фразы остаются на английском; это не полный перевод всего Minecraft или всех 55 экранов. В grid RU использует переведённое краткое описание, если расширенный вариант ещё не переведён. Есть native RU grid/list/Arrange при реальных scaled **427×240** на hidden Canvas **854×480**.

**Loading и остальные screens.** Graphite/gold/cosmic оформление применено к раннему `MinecraftClient.loadLogo`, настоящему `LoadingScreenRenderer`, ProgressScreen, terrain, Connect/Disconnected и дополнительным обычным экранам настроек/подтверждений/создания мира. Ранний экран сохраняет загруженный Mojang logo и явный copyright. Ранний экран, terrain/progress, LoadingScreenRenderer и Connect выводят development ID и credits; error screen сохраняет нативный текст ошибки. Показывается только фактически переданный native progress 0..100; отрицательные/вне диапазона значения дают неопределённую стадию без процента. Нет wall-clock/fake completion. Early logo имеет фактическую подпись подготовки ресурсов, до доступного выбора языка — английскую.

LoadingScreenRenderer сохраняет нативные running/error guards, 100ms throttle, framebuffer lifecycle, updateDisplay и yield: hook меняет только уже подготовленный кадр перед unbind. ProgressScreen сохраняет native `done` branch. Terrain сохраняет tick/keepalive/key lifecycle; заменено рисование. Connect/Disconnected сохраняют native status/errors/buttons/cancel; отдельный bridge нужен потому, что их key handlers не делегируют базовому Screen. Нативные специализированные lists/sliders/формы не переписывались целиком, их lifecycle сохранён.

Пять новых production mixins, включённых вместе с прежними 36 и двумя косметическими (**43** записи):

- `EarlyLoadingMixin`
- `LoadingRendererMixin`
- `ProgressScreenMixin`
- `TerrainScreenMixin`
- `LoadingNavigationMixin`

Проверенные по итоговому JAR 12 новых class-файлов своего UI для строгого contract: `hud/HudArrangeScreen.class`, `hud/HudLayoutHistory.class`, `hud/HudLayoutOperations.class`, `hud/HudLayoutOperations$Align.class`, `hud/ModuleBrowserSettings$View.class`, `menu/LoadingTheme.class`, `menu/UiTranslations.class` и пять перечисленных `mixin/*.class` (все под `dev/spaceclient/`). Cosmetic classes принадлежат координатору. Java 8 target и закреплённый toolchain сохранены.

**Финальная собственная проверка:** **300 Java tests, 0 failures/errors**, offline game build и hidden addon build прошли. Исходники закоммичены до финального build/stamp. Launcher/site тесты этим агентом не запускались; общая приёмка и required-class/bundle контракт остаются у координатора.

Native run: **exit 0**, **37 UI PNG +5 loading PNG**, **267** keyboard handler steps. Один собственный QA-мир `wave2-ui-20261103`, собственные config/assets/runtime в `.reports/wave2-runtime`. System input, фокус, CUA, видимые тестовые окна, пользовательские миры/процессы не применялись. Два loopback connection fixtures используют только 127.0.0.1: настоящий удерживаемый сокет для Cancel и заранее закрытый порт для error page. Проверены возврат с ошибки и отмена через native кнопки клавиатурой.

QA покрыл persisted list, paging/search/favorites и сохранение поиска при F6, settings/Cosmetics round-trip, native ProgressScreen done, actual early/terrain/world-renderer hooks, group drag/spacing, undo/redo, Lock, Hide/Show, independent second profile, X/Y End100/300%, future-browser Lock/Brand, необязательную подпись, keyboard group selection и compact RU Inventory300% с недоступной заполненной осью. Текстовые stage traces содержат реальные merged handler origins (`space$early`, `space$terrain`, `space$loading`, `space$progress`), а не только вызовы helper напрямую. Значения **37%/73%** — обозначенные ProgressScreen QA fixtures, не проценты настоящей загрузки мира; actual world-renderer кадр использовал native **−1** и не показывал процента.

Финальный журнал содержит **один ожидаемый ERROR** `Couldn't connect to server`/`Connection refused` от закрытого loopback-порта. Не выдавать его за ноль ошибок; неожиданных ERROR и FATAL в финальном прогоне не найдено. Прежние неудачные попытки (fixture page/favorite state, addon inherited-field mapping) сохранены отдельно; они не подменяют финальный run. Native Cancel defect исправлен в `8940d91`, End bounds и zero-axis — в `679529b`/`e41ed50`.

Все **42** финальных кадра действительно просмотрены в пяти contact sheets. Дополнительно полноразмерно просмотрены `early--1`, `world-renderer--1`, `terrain--1`, `en-list`, `connect-loopback`, `disconnected-error`, `editor-group-drag`, `arrange-numeric`, `ru-compact-list`, `ru-inventory300-full-axis`, `cosmetics-in-world`, `world-optional-signature`. Compact RU status рядом со star выглядит тесно; это небольшой оставшийся visual polish, не основание менять продукт после координационного freeze. Сценарии действий работают.

| Свойство своего финального JAR | Значение |
|---|---|
| `HEAD:game` | `0d5b45e404adf26929e2d93faf2af1cf66c337de` |
| Видимая метка | `dev-0d5b45e404ad` |
| SHA-256 | `51f4203fa00e9ac3c0eb9b03a331bf6b3722622bb0aaf18e2989fa2cda4be57d` |
| Размер | 1 144 448 bytes |

Это собственный JAR, не интеграционный main JAR координатора. Runner сверяет stamp из **загруженного** JAR с `git rev-parse HEAD:game`. Первые/промежуточные successful native artifacts на других source trees не считаются финальным evidence. Последний инструмент вернул уже завершённый успешный запуск после длительного pending состояния: native log **07:46:02–07:46:20 UTC /13:46:02–13:46:20 Asia/Dhaka**, перед ним offline build занял 20s. Координатор уведомлён, что этот build/UI интервал нельзя включать в параллельные performance samples.

**Performance ownership изменён ведущим во время работы:** из-за ожидания внешнего execution gate он перенёс новую controlled-серию в своё окружение и прямо запретил запускать дублирующую серию здесь. Своих performance-процессов/замеров в этой копии **не было**; FPS/frametime/RAM/startup результатов и vanilla comparison этот отчёт не заявляет. Подготовленный addon был только скомпилирован и сохранён в ignored `.reports/wave2-ui/benchmark-draft-compiled-not-run`; он не входит в product JAR и не был подключён к UI QA. Не коммитить его как выполненный benchmark. Данные/отдельный benchmark commit — у ведущего.

Повторение своего UI QA при подготовленном own runtime: скопировать helper из QA ZIP в `.reports/wave2-ui`, затем `scripts/game.ps1 -Tasks @('test','build','remapSettingsQa','--init-script=../.reports/wave2-ui/init.gradle','--offline','--no-daemon')` и `node .reports/wave2-ui/run.mjs`. Понадобится только process-scoped safe.directory при другой Windows identity. Не запускать во время серии ведущего. Скрипт сохраняет старые QA captures в собственный attempt-directory и начинает с определённых собственных prefs; никакие пользовательские пути не ищет.

Codebase Memory: прежний отдельный index request этой копии отклонён как outside allowed root; глобальный allowlist не менялся, успешная индексация/полнота не заявляются. UI/model/runtime boundaries сверены непосредственно по исходникам и pinned native signatures/bytecode. Применён `ui-ux-pro-max`: keyboard alternatives for dragging, visible focus, disabled state и desktop navigation.

Остались вне собственной приёмки: физические V/C/Alt+Tab (отложены пользователем, повторно не запрашивались), полноценная multiplayer/input matrix всех 55, assistive-technology, все сторонние resource/shader packs, длительные сессии и полный перевод индивидуальных описаний. Этот UI QA не заменяет performance benchmark, официальный вход, регистрацию/HTTP или чистую установку. Пользовательский установленный JAR и видимые launcher/site не обновлялись этой копией.

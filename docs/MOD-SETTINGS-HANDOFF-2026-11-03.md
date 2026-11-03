# Настройки всех 55 модулей · 3 ноября 2026

**Обновление основной копии:** эта ветка интегрирована; после исправления QA-помощника общая версия прошла проверку настроек всех 55 модулей внутри Minecraft и установлена локально. [Совместная приёмка и границы](INTEGRATION-2026-11-03.md). Описание собственной проверки ветки ниже сохранено как история.

Реализован общий рабочий UI для **29 gameplay + 26 HUD модулей**, включая отдельный менеджер Waypoints. Результат локальный, в пользовательский runtime не установлен. Native-приёмка этого интерфейса **не выполнена**: запрос запуска скрытого QA-клиента был отклонён. Сборка, тесты обработчиков и исходников не выдаются за игровую проверку.

## Копия, основа и коммиты

- Root: `C:/Users/user/Documents/Codex/2026-11-03/space-client-mod-settings/work/spaceclient-settings`.
- Ветка: `space-mod-settings`; обычный clone `--no-hardlinks`, не worktree. Remote удалён; push и публикаций нет.
- Первоначальная основа: `2b2c0c144093b515b3ebefbbbe08bade739ba357` из `space-minecraft-menus`.
- Обновлённая локальная основа: `e7f85094b6326b6c74ebc323e191945a4b621a0a`. Сохранены source commits `1479935`, `4213be2`, `2a297a3`, `de9ec98` и docs `e068b4e`, `e7f8509`.
- Собственные коммиты: `2499583` — общий UI и исходные controls tests; `080fd40` — merge новой основы и перенос действий 49–71; `9d431eb5f8f51dc40df575db172b87a99ce697af` — Waypoints, input handlers и покрытие всех 55 модулей. Каждый содержит `Refs #2, #4, #5`.
- Итоговый game tree: `a7b35374de8508756111c4170e270e5834dd35eb`. Документационный коммит не меняет этот tree.

Исходный checkout, main, пользовательские миры и профиль не редактировались. Java/Git настройки задавались только для процессов или этой копии. JDK, Gradle cache и два необходимых Node-пакета скопированы в собственные игнорируемые каталоги; изменяемые build outputs и node_modules не разделяются.

## Общее поведение

`ModuleSettingsScreen`, `SettingRow`, `SettingNumber` и `SettingsLayout` отделяют представление от существующих settings owners. Добавление модуля не требует ещё одного layout/input implementation. Конфиги и runtime продолжают выполнять валидацию и атомарную запись.

- Фиксированные header/footer, целые прокручиваемые строки и автоматическое раскрытие фокуса. Проверена расчётная геометрия от 320×240 до 1920×1080. Это проверка координат, а не визуальный осмотр native кадров.
- Подпись, пояснение и значение занимают отдельные области. Сокращённое пояснение обозначено `...`; полное доступно в нижней подсказке при фокусе. Подсказки и ошибки имеют две отдельные строки над кнопками.
- Числа: кнопки −/+, ползунок, Left/Right, LShift для увеличенного шага, Home/End для пределов. Значения упираются в границы. Уже сохранённые значения вне сетки шагов сохраняются до правки и меняются относительно текущего числа.
- Во время drag изменяется только черновик. Запись — один раз при отпускании; Esc отменяет. Повторные действия на уже достигнутом пределе не записывают файл.
- Tab / LShift+Tab, Up/Down, Page Up/Down, Enter/Space и Esc; boolean actions защищены от repeat events. Плавные переключатели используют прежний `HudSwitch.Motion`.
- Read-only обозначен текстом, controls недоступны, просмотр и Back остаются доступны. Ошибки записи показываются отдельно; settings owners сохраняют прежнее состояние.
- Сброс gameplay сохраняет Enabled и сбрасывает параметры/клавиши выбранного модуля; Auto Text также очищает слоты. HUD reset scopes остаются раздельными: placement, appearance, content.
- HUD показывает профиль и master visibility. Предпросмотр использует прежний native renderer в отведённой области и fit без изменения сохранённого scale. Без мира показано явное состояние отсутствия live preview; anchor недоступен с пояснением.
- Crosshair и три существующих visual previews вынесены из области controls. Auto Text показывает полный короткий текст в отдельной области, счётчик длины, отклонённую вставку, save/cancel и повтор выбора конфликтующей клавиши.

Оставлены без изменений GameplayModule ordinals, ModSettings schema10/132, HUD18/export13/browser1, waypoint schema1, 35 production mixins и gameplay semantics. В diff к обновлённой основе нет ModSettings, HudConfig, WaypointStore, HUD renderer, runtime или launcher изменений.

## Gameplay: изменения каждого модуля

У всех строк ниже общие Enabled / Toggle key, объяснение эффекта, доступный keyboard focus, read-only/error states и scoped reset.

| Модуль | Параметры и уточнения UI |
|---|---|
| ToggleSprint | Auto/Toggle с объяснением native ограничений и sprint key. |
| FreeLook | Hold key, конфликт, unbind и возврат вида после отпускания. |
| Zoom | Hold key, bounded 2–8x, smooth toggle, восстановление FOV. |
| Crosshair | Shape/color, size 2–12, gap 0–8, thickness 1–4 с единицами; dot/outline; отдельный preview. |
| Fullbright | Soft/Bright/Full; явно сохраняется Minecraft gamma. |
| Time Changer | Sky preset; пояснение Overworld-only и сохранения настоящего времени. |
| No Hurt Cam | Пояснение steady damage camera и сохранённого death view. Новых параметров нет. |
| Snaplook | Hold key/conflicts и выбор held perspective. |
| Weather Changer | Визуальная погода, пояснение сохранения server weather. |
| Chat Timestamps | Формат локального времени новых сообщений. |
| Durability Warning | Порог и перечень проверяемой экипировки; зависимость от видимого HUD. |
| Block Overlay | Color/style, opacity 5–60%, width 1–5 px. Fill/outline control недоступен, если выбранный style его не использует. |
| Hit Color | Color, strength 5–80%, players-only; death tint остаётся native. |
| FOV Control | 60–110 degrees, dynamics 0–100%; пояснение steady/full и отдельный preview. |
| Animations | Раздельные block-hit/eating/bow; пояснение косметического эффекта без изменения combat timing. |
| Auto Text | Три слота, message hotkey, отдельный редактор текста, счётчик 100 UTF-16 units, save/cancel и конфликт клавиш. |
| GUI | Auto/1–4 и фактический window-limited scale. |
| Hitboxes | Color, width 1–3 px, Players/Mobs/Dropped items; пояснение depth/collision semantics. |
| Item Physics | Bounded air speed 1–5x и Flat on ground; пояснение сохранения pickup. |
| Item Size | First-person 50–150%, dropped 25–200%; 100% = native, карты исключены. |
| Sounds | Block/step и combat theme, target-down cue, volume 0–100%. Preview недоступен при Off cue или нулевой effect volume; kill credit не обещается. |
| Scoreboard | Numbers/background, scale 50–150%, X/Y −500…500 px с явным направлением смещения. |
| TNT Countdown | Color/background, distance 8–128 blocks; fuse обозначен как client estimate. |
| Scrollable Tooltips | Step 2–30 px; пояснение wheel и Shift+wheel. |
| Waypoints | Beam/distance, Manage markers с количеством; без мира переход недоступен. Отдельный manager описан ниже. |
| Particles | Amount 0–300%, size 50–200%, tint и color. Color недоступен при выключенном tint; ограничение block debris объяснено. |
| Enchantment Glint | Color, speed 25–300%, strength 10–100%; сохранение pack textures и native 100%. |
| Inventory Blur | Strength 1–8, требования графики; состояние unavailable при неподдерживаемой/ошибочной реализации blur. |
| Dynamic FPS | Background 5–60, idle 10–120 FPS, delay 10–300 s; native lower cap и activity guards объяснены. |

## HUD: изменения каждого модуля

Для каждого виджета: независимый профиль, Enabled, bounded scale 0.5–3.0x, понятные названия anchor, placement reset; color/background/shadow и appearance reset; fit native preview, master visibility, read-only/error/keyboard states. Content-настройки добавлены только там, где они уже существуют.

| Модуль | Content и пояснение |
|---|---|
| FPS | Show FPS label, отдельный reset content. |
| Coordinates | Decimal places 0–2 с bounded control и объяснением округления. |
| Keystrokes | Jump, attack/use, Line/Block layout; preview ограничен своей областью. |
| CPS | Left/Right/Both, previous/next с клавиатуры. |
| Compass | Общее layout/appearance/preview; описание направления. |
| Clock | 12/24h и секунды, пояснение локального времени. |
| Speed | Общее UI; описание горизонтальной скорости. |
| Health | Общее UI; текущее/максимальное здоровье. |
| Hunger | Общее UI; текущий food level. |
| Armor | Text/Icons, durability percent/remaining, held item; reset только своего content. |
| Potion Effects | Text/Icons, bounded 1–5 effects, duration; fitted preview. |
| Memory | Общее UI; used/max Java memory. |
| Playtime | Общее UI; время в мире без пауз. |
| Item Counter | Общее UI и native item preview без изменения counting semantics. |
| ToggleSprint HUD | Full/Compact; armed и running различены в пояснении. |
| Ping | Show label и пояснение value-only. |
| Biome | Show label и пояснение value-only. |
| Pack Display | Label, bounded 1–3 pack names, объяснение +N. |
| Day Counter | Общее UI; настоящий день мира с отсчётом от 1. |
| Light Level | Общее UI; sky/block light у ног. |
| Experience | Общее UI; native progress preview в отведённой области. |
| Arrow Counter | Общее UI; стрелы inventory/hotbar. |
| Inventory HUD | Hotbar/overlays с пояснениями counts/durability; fitted grid preview. |
| Combo Counter | Общее UI; client-observed hits и сброс при смене цели, уроне или 2s. |
| Reach Display | Общее UI; last client hit distance и `--` через 5s. |
| Saturation | Общее UI; объяснение приблизительного server значения `~`. |

## Менеджер Waypoints

Сохранены Add here, имя, X/Y/Z, color, visibility, delete и страницы по шесть точек. Они используют общий settings shell: Markers / Edit selected marker, прокрутку и фиксированные footer actions.

`WaypointEdit` — только UI draft. Enter сохраняет, Esc отменяет, Backspace удаляет code point, Ctrl+V вставляет допустимый текст. Числа вводятся напрямую; Left/Right корректируют координату на 1, Shift — на 10, с ограничениями X/Z ±30000000, Y 0–255. Ошибка поля видна и сохраняет черновик для исправления; failed write также сохраняет черновик и прежнюю точку. Контекст и dimension исходной точки остаются прежними.

Исправлены скрытая под save hint ошибка, недоступный в read-only просмотр, неочевидные пустые состояния и потеря фокуса при сокращении списка. Add недоступен без мира/при лимите. Delete требует второго нажатия, Esc отменяет. Новая точка сразу фокусирует Name. Схема и лимиты store не менялись.

## Проверки и границы

- **272 Java tests, 0 failures**, `scripts/game.ps1 test build` с собственным Temurin JDK 21, Gradle 9.4.0, Loom 1.16.1, loader 0.18.3, Yarn build604, Java8 major52. 249 tests новой основы +23 собственных.
- Production keyboard/mouse handlers протестированы без окна: числа и пределы, drag без ранней записи, cancel/commit, minus/plus, repeat guard, Tab/Shift+Tab/Page keys, disabled/read-only и сокращение списка. События ОС не инъецировались.
- Проверены уникальность/пояснения controls всех 29 gameplay/26 HUD, dispatch всех чисел до min/max с повторным чтением файлов, scoped resets и независимость профилей. Waypoint draft: валидация, Unicode/plain text, границы, persistence и failed-write retry.
- **10 game-bundle validator tests, 0 failures**. Строгий `prepareGameBundle` принят для чистого committed game tree. Итоговый JAR: 1 077 347 bytes, SHA-256 `3ad9c34f3f2c50512c0062f8be5c535bdeabd1edaeb76eb3aa77cc84640a67ae`.
- QA-addon с циклом по всем актуальным enum values и waypoint checks компилируется/remap проходит, хранится только в `.reports/settings-qa`. Native client **не запускался** после отказа разрешения; native captures этого UI: **0**.
- Не приняты: визуальное качество настоящих кадров, физические keys/wheel/clipboard, полная GUI/Unicode/DPI/resource-pack matrix, world preview разных content states, sound на слух и performance. Прежние 15 captures основного чата проверяют его runtime, не новый UI этого чата.
- Sandbox запуск Gradle не мог открыть Loom JAR (AccessDeniedException); успешные проверки выполнены с разрешённым запуском локального JDK и process-only Git safe.directory. Глобальные настройки не менялись.

## Codebase Memory

Индекс `spaceclient-mod-settings-20261103`, root этой копии. Graph-first поиск классов, snippets и traces использован до изменений; после итогового кода индекс обновлён 2026-11-03T01:40:11Z: **9269 nodes /24580 edges, skipped=0**. Проверены восемь изменённых production paths; `no_recorded_issue` сопровождается `metadata_changed`, поэтому source и staged diff прочитаны напрямую.

Fast mode намеренно исключает docs, helpers/assets/build outputs и 31 файл; partial parse остаётся у CSS/Gradle/wrapper. Граф не доказывает полноту. Старый trace WaypointsScreen.activate ошибочно связывал короткие имена с ModSettings.readOnly / SettingNumber.min; реальный source проверен. Новый `WaypointEdit.save → WaypointStore.replace` разрешён и подтверждён исходниками и тестами.

## Интеграция минимального diff

Сравнивать с `e7f8509`, а не с первоначальным `2b2c0c1`: runtime новых 11 уже находится в обновлённой основе. Собственный code diff — 13 файлов (8 production, 5 tests), плюс этот handoff и QA summary. Не переносить `.tools`, `.reports`, node_modules или отдельный JAR в пользовательский профиль.

В outputs этого чата подготовлен `space-mod-settings-vs-e7f8509.patch`. Проверить его в актуальной интеграционной копии через `git apply --check`, затем применить с `git apply --3way`. При новых изменениях этих экранов свести их вручную по actions IDs; **все 49–71 сохранены**. Альтернатива: fetch этой локальной ветки, cherry-pick merge `080fd40` с `-m 2` (UI diff относительно основного чата), затем `9d431eb` и docs commit. Не cherry-pick `2499583` поверх merge diff повторно.

После интеграции выполнить Java tests/build и строгий bundle build со stamp объединённого tree. Отдельная native приёмка должна проверить прокрутку/фокус/footer при минимальном GUI, высоту Inventory/Keystrokes/effects previews, key conflicts и text/clipboard, GUI resize, read-only/error states, Waypoints pages/add/edit/nudge/delete/cancel. Разрешение на native запуск в этом чате не получено; его нельзя считать уже данным.

Новых config/runtime параметров для этого среза не потребовалось. Дополнительные параметры модулей, расширенные gameplay previews и schema migrations не реализовывались. Установка — только будущей объединённой сборки в основном чате после её проверки.

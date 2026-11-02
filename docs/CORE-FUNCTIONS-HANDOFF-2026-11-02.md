# Space Client: отмена запуска и безопасная диагностика · 2 ноября 2026

Самостоятельный локальный срез launcher main/services. Исходный checkout использован только для чтения; UI, сайт, backend, игровые модули и пользовательские инстансы этим срезом не изменялись. Общий `CHAT-HANDOFF.md` сохранён.

## Копия и коммиты для интеграции

- Независимый clone без hardlinks и worktree: `C:/Users/user/Documents/Codex/2026-11-02/space-client-core-features/work/spaceclient-core`.
- База: `0af5658f2dc0d533e8e1a60c9587248fb2b49c26`.
- Ветка: `fix/core-launch-recovery`.
- `5ff589d4fa981b09426709ab29acda96e26f11c8` — отмена до проверки runtime, два воспроизводимых regression test.
- `03f11871196de3049453e622514489845d64d6ba` — потоковая диагностика Java, восемь дополнительных тестов и описание в `LOCAL-LAUNCH.md`.
- Этот документ передаётся отдельным завершающим documentation-коммитом; его hash виден в `git log -3`.

Переносить оба code-коммита по порядку в копию, которой управляет интеграционный чат. Конфликт возможен только при независимом изменении `launch.ts`, `launch.test.cjs` или того же абзаца `LOCAL-LAUNCH.md`; fixture и `main/game-bundle.ts` не затронуты. Push, публикация и установка в общий `.reports/launcher-desktop` не выполнялись. `node_modules` физически скопирован в собственную копию без junction; выходные каталоги сборки отдельные.

## Дефекты и результат

1. При закрытии лаунчера во время асинхронного получения каталога профиля отмена уже выставлялась, но `LaunchService` всё равно вызывал preflight. Сам `prepareLocalLaunch` также начинал Java `-version` при уже отменённом signal. До исправления два теста фиксировали по одному лишнему вызову. Теперь обе границы проверяют отмену: runtime/проверка файлов не начинаются, reservation освобождается после завершения уже начатого lookup. Работающая игра по-прежнему сохраняет lock до `close`; shutdown её не завершает.
2. Диагностика проверяла только первые 8192 байта каждого события вывода и не соединяла части сигнатуры. Разделённый `OutOfMemoryError` и поздний `Failed to create display` воспроизвели отсутствие причины в статусе. Новый main-only `LaunchDiagnostics` сканирует весь chunk блоками по 8192 байта с ограниченным хвостом отдельно для каждого stream. Повторы фиксированных меток не раздувают журнал.
3. При неуспешном выходе прежний `LaunchStatus.message` теперь сообщает распознанную причину и действие: память/RAM, OpenGL/драйвер, версия классов/Java 21 x64, отсутствующий класс/восстановление файлов. Более конкретная причина сохраняется при последующем общем исключении. Неизвестные ошибки используют прежний fallback. Выход 0 без сигнала остаётся штатным; exitCode сохраняется. Новый запуск не наследует причину предыдущего.

В DTO и журнал выходят только фиксированные строки. Тесты подмешивают искусственные токен, ник, путь и command arguments и проверяют отсутствие этих данных в результате. Raw output, хвосты matcher, stack traces и команды не отправляются renderer. Новых IPC, полей DTO, файлов логов или UI не добавлено; предел журнала 60 строк сохранён.

## Файлы

- `apps/launcher/src/main/launch-plan.ts` — ранняя отмена до runtime.
- `apps/launcher/src/main/launch.ts` — отмена после lookup и подключение диагностики к существующему статусу/журналу.
- `apps/launcher/src/main/launch-diagnostics.ts` — ограниченный сборщик фиксированных причин.
- `apps/launcher/test/launch.test.cjs` — regression отмены, разделённого вывода, поздней сигнатуры, повтора и штатного close.
- `apps/launcher/test/launch-diagnostics.test.cjs` — все границы разделения сигнатур в обоих streams, отсутствие склейки разных streams, внутренние границы блоков, приоритет и неизвестный вывод.
- `docs/LOCAL-LAUNCH.md` — фактическое поведение и ограничения.
- `docs/CORE-FUNCTIONS-HANDOFF-2026-11-02.md` — эта передача.
- `docs/qa/core-launch-reliability-2026-11-02.json` — компактная запись проверки и границ.

## Проверки

- `npm.cmd run test:launcher`: **122 PASS**, 0 failed/cancelled/skipped; включая architecture, IPC/error redaction, downloader, install cancellation, natives и строгий JAR inspector.
- `npm.cmd run check --workspace @space/launcher`: PASS.
- `SPACE_REQUIRE_GAME_BUNDLE=1 npm.cmd run build:launcher`: PASS, main/preload/renderer. Использована отдельная побайтная копия уже собранного JAR из исходного checkout, затем штатный inspector проверил его source stamp, состав и hashes. Java-исходники не менялись и заново здесь не компилировались.
- Проверенный bundle SHA-256: `ae93cb849aede7902f8328bb717b696d292c109bac569d7d61dc7bc69fbe0635`; `HEAD:game`: `9bbd0de084a345e438b7723c5a3ba6bb9c0f4b0d`. После объединения новых модов интеграционный чат собирает свой актуальный bundle штатно.
- `npm.cmd run check:ui --workspace @space/launcher`: PASS в скрытом headless Electron с новым userData, без игры/аккаунта/игровых загрузок. Проверены 13-функциональный bridge без Node globals, отказ из-за отсутствующей Java и повтор, профиль/сохранение, keyboard/dirty guards, три экрана на четырёх viewport, reduced-motion/forced-colors и отсутствие renderer errors. Отчёт: `.reports/launcher-ui/run-368326cf-9f28-418d-be99-98f09ffe4c8f/report.json`. Созданы 14 headless-снимков; физическое окно не осматривалось.
- Обычный Prettier-check всех пяти изменённых code/test-файлов: PASS. Полный launcher-check с `--end-of-line auto`: PASS. Обычный полный check сообщает о 85 неизменённых CRLF-файлах Windows checkout; массовое форматирование соседних областей не выполнялось.
- `git diff --check` и просмотр staged diff: PASS.

Первый запуск esbuild в sandbox получил Access denied на родительском каталоге; проверка завершена вне sandbox с временным process-scoped `safe.directory` только своей копии. Глобальные Java/Git-настройки не менялись.

## Codebase Memory

Сначала проверены проекты: старый `spaceclient` указывал на checkout 23 октября. На нём выполнены graph/trace и exact-path coverage: три main-пути были `metadata_changed`, тест исключён. Эти результаты не использованы как доказательство отсутствия кода.

Создан **отдельный full-индекс** `spaceclient-core-features-20261102` с root ровно своей копии. Штатный MCP отказал из-за allowed-root; CLI запущен с process-scoped `CBM_ALLOWED_ROOT` этой копии и уникальным именем в штатном кеше. Чужие индексы и глобальная конфигурация не переназначались. Последнее индексирование кода: `2026-11-02T15:43:42Z`, status ready, 8726 nodes / 19657 edges.

Новый `search_graph` находит `LaunchService`, `prepareLocalLaunch`, `LaunchDiagnostics` с правильными путями/строками. `trace_path` использован для preflight и нового collector; он пропустил связь service → prepare/accept и ошибочно предложил одноимённый Java `path`. Эти связи проверены непосредственно по исходникам и `rg`.

Exact-path coverage всех пяти code/test-файлов: `no_recorded_issue`, но **freshness = metadata_changed** даже сразу после полного индексирования. В `index_status` Git-метаданные недоступны (`is_git=false`, branch/head null) при существующем root; отдельный штатный `git` подтверждает ветку `fix/core-launch-recovery`, базу и hashes выше. Поэтому индекс не объявляется доказательством свежести или полноты. Исходники всех указанных путей прочитаны; пять parse-partial файлов индекса относятся к CSS/Gradle/PowerShell вне реализации этого среза. Документация обновлена после индексирования кода.

## Границы и продолжение

Настоящие OOM/OpenGL/class-loading аварии в Minecraft не провоцировались. Это детерминированная проверка обработки вывода и lifecycle плюс отдельный headless Electron smoke, а не игровая приёмка. Классификатор ограничен фиксированными сигнатурами и не является полноценным crash reporter.

Уже начатая Java-проверка остаётся ограниченной прежним таймаутом 5 с; новая отмена предотвращает её начало после отмены lookup. Пользовательские `options`, configs, миры, запущенная игра и общий launcher-инстанс не менялись. Integrity/source-stamp, ограничения JAR и mods, `launchReady=false`, shell=false, sandbox/CSP и процессные блокировки сохранены.

Microsoft, Space-регистрация, публичный релиз/сайт и закрытие связанных issues не выполнялись. Интеграция и установка объединённой сборки остаются у исходного чата.

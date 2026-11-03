# Space Client: отмена проверки Java · 2 ноября 2026

Продолжение независимой копии `C:/Users/user/Documents/Codex/2026-11-02/space-client-core-features/work/spaceclient-core`, ветка `fix/core-launch-recovery`. Предыдущий HEAD: `6877f280d64e7b6a79386986ffaab2f67463c962`. Новый code-коммит: **`e1649e8bb5c377311d9861c405450f138b6eca8b`**, `fix(launcher): cancel owned Java probes and await process close (Refs #25)`. Этот handoff и QA JSON передаются отдельным documentation-коммитом. Общий `CHAT-HANDOFF.md` не менялся.

## Поведение

Ранее отмена останавливала запуск перед runtime, но уже начатая Java-проверка не получала signal. Отдельная команда `java:check` также не учитывалась в `LauncherApplication.running` и shutdown. До исправления regression assertions показали отсутствие signal в preflight и `running=false` при активной проверке; тесты ранней отмены вместо отмены получали ошибку поиска Java.

`inspectJava` теперь принимает необязательный main-owned `AbortSignal`. Уже отменённая проверка не обращается к диску и не создаёт процесс; отмена во время `stat` не допускает spawn после завершения lookup. Signal передаётся только собственному `execFile` с прежними фиксированными аргументами `-XshowSettings:properties -version`. При отмене Node останавливает этот дочерний процесс.

Callback отмены `execFile` может предшествовать `close`. Promise Java-проверки завершается только после `close`, поэтому preflight и блокировка ручной проверки не освобождаются раньше завершения процесса. Callback сохраняет только ограниченный результат/признак ошибки внутри main; наружу выходят разобранный `JavaInfo` либо фиксированный `UserFacingError`. Текст отмены — «Проверка Java отменена.»; raw output/error/cause, пути, команды и произвольный abort reason не раскрываются.

`prepareLocalLaunch` передаёт свой signal runtime. Ручная команда проверки получает отдельный controller и tracked promise в application; она учитывается в `running`, отменяется и ожидается при shutdown. Новые Java-проверки после начала shutdown отклоняются. Существующая защита от параллельных ручных проверок и повтор после обычного отказа сохранены.

Игровой процесс этим кодом не завершается: используется отдельный diagnostic `execFile`, а `LaunchService` и его правило ожидания работающей игры до `close` не менялись. IPC/preload/renderer не расширялись. Аргументы, shell=false, windowsHide=true, timeout 5000 мс, maxBuffer 65536 байт, локальная Windows path-validation и очистка JVM-injection environment сохранены. Добавленная подстановка `stat/execFile` — только внутренний main/test seam, не IPC-вход.

## Проверки

Добавлены восемь tests: шесть в `java-probe.test.cjs`, один application lifecycle и один preflight signal. Проверены ранняя отмена, отмена во время успешного/неуспешного lookup, callback до close, обычный результат, асинхронная и синхронная ошибка без утечки, прежние execution limits и shutdown reservation.

Нативный regression использует **два собственных скрытых Node helper-процесса**. Один подставляется вместо намеренно медленного `java -version` и запускается настоящим `execFile` с production signal/options. После handshake выполняется отмена: диагностический helper уже выдал `close` до завершения probe promise, а независимый второй helper остаётся живым (`killed=false`, exitCode/signalCode null). Оба тестовых процесса очищаются только через сохранённые child handles. Поиск/остановка пользовательских PID не используется. Это проверка нативного управления процессом, не запуск настоящего JDK или Minecraft.

- **135 launcher tests PASS**, 0 failed/cancelled/skipped, включая предыдущие lifecycle/инстанс/integrity/architecture tests.
- TypeScript PASS; обычный Prettier всех семи изменённых code/test-путей PASS; полный launcher Prettier с `--end-of-line auto` PASS.
- `SPACE_REQUIRE_GAME_BUNDLE=1 npm.cmd run build:launcher` PASS; прежний отдельно скопированный JAR штатно проверен по source stamp/составу/hash. SHA-256 `ae93cb849aede7902f8328bb717b696d292c109bac569d7d61dc7bc69fbe0635`, game tree `9bbd0de084a345e438b7723c5a3ba6bb9c0f4b0d`. Java-исходники не менялись и не компилировались.
- Скрытый `check:ui` PASS: 10 checks, 14 generated screenshots, 0 renderer errors; прежние 13 bridge-функций, профиль/сохранение/dirty guards, missing-Java failure/retry, три экрана/четыре viewport. Отчёт `.reports/launcher-ui/run-95544080-a0d7-40c5-9727-a8661f212a4d/report.json`, `2026-11-02T16:21:47.865Z`. Настоящий Java picker/запуск игры/живой аккаунт не проверялись.
- Staged diff просмотрен; `git diff --check` PASS.

## Файлы и интеграция

Code-коммит меняет `apps/launcher/src/main/java.ts`, `main/launch-plan.ts`, `main/application/launcher.ts`, `main/application/ports.ts`, tests `java-probe.test.cjs`, `application.test.cjs`, `launch.test.cjs` и docs `LAUNCHER.md`, `LOCAL-LAUNCH.md`. Documentation-коммит добавляет эту передачу и `docs/qa/java-probe-cancellation-2026-11-02.json`.

Если предыдущие срезы уже интегрированы, нужен только новый code-коммит и документация. Экспорт `spaceclient-java-probe.bundle` содержит накопленную ветку с prerequisite `0af5658f2dc0d533e8e1a60c9587248fb2b49c26`. Конфликты в изменённых launcher/docs-файлах разрешать с учётом других локальных веток. Текущий `main/game-bundle.ts` и fixture не менялись; после объединения игровых изменений интеграционный чат пересобирает свой актуальный bundle.

## Codebase Memory и границы

Перед изменением использованы собственный индекс `spaceclient-core-features-20261102`, `index_status`, `search_graph`, `trace_path(inspectJava)` и coverage шести существующих code/test-путей. Trace ошибочно связал builtin resolve/execFile с HUD и API test; это не реальные зависимости и сверено с source. После изменений выполнен full reindex только своего root через CLI с process-scoped `CBM_ALLOWED_ROOT`, повторные graph и exact-path coverage семи изменённых code/test-путей. Ready, `2026-11-02T16:21:44Z`, 8858 nodes / 19842 edges; graph находит `inspectJava` и `probeError` по правильным путям/строкам. Coverage — `no_recorded_issue`, freshness всё ещё `metadata_changed`; свежесть/полнота этим не доказаны. Исходники проверены непосредственно. Прежнее отсутствие Git metadata у индекса не выдано за отсутствие Git-репозитория; branch/commit проверены штатным Git. Чужие roots/индексы и глобальные настройки не менялись.

`stat` сам по себе не прерывается; его завершение ожидается и отменённая проверка после него не создаёт процесс. Настоящее зависание Java, timeout/переполнение вывода физического JDK и Minecraft в этом срезе не воспроизводились. Работающий игровой процесс, пользовательские options/config/миры, общий launcher-инстанс и исходные checkout не менялись. Push, публикация, аккаунты и закрытие issues не выполнялись. Обнаружение игры после аварии лаунчера и отдельная диагностика сетевых таймаутов остаются предложениями для следующих срезов.

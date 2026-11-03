# Space Client: восстановление инстанса · продолжение 2 ноября 2026

Локальная копия: `C:/Users/user/Documents/Codex/2026-11-02/space-client-core-features/work/spaceclient-core`; ветка `fix/core-launch-recovery`. Продолжение после `98fb68adc61c9090b986a29a81d4e3ecc5e9c463`, исходная база ветки `0af5658f2dc0d533e8e1a60c9587248fb2b49c26`. Предыдущая передача — `CORE-FUNCTIONS-HANDOFF-2026-11-02.md`; общий `CHAT-HANDOFF.md` не менялся.

Новый code-коммит: **`42033c2ead0ab738ff0c81e8fa65ba9be4bbed21`**, `fix(launcher): close installer admission and explain recovery failures (Refs #9)`. Эта передача и QA JSON идут отдельным documentation-коммитом. Если предыдущий срез уже перенесён, интегрировать только новый code-коммит и документацию этого продолжения. Экспортированный `spaceclient-core-recovery.bundle` содержит всю ветку от `0af5658`, включая прежний срез; его prerequisite — этот базовый commit.

## Исправления

- `InstallationService.shutdown()` теперь синхронно закрывает приём новых задач, затем ожидает отмену активной. Ранее `start()` снова создавал подготовку после завершения shutdown. Регрессия воспроизведена до исправления как для пустого сервиса, так и для остановки активного задания. Обычный `cancel()` оставляет повтор доступным.
- Сообщение о нехватке места показывает вычисленный общий необходимый свободный объём, округлённый вверх до МиБ: план + Space + прежний резерв 160 МиБ. Ранее сообщение всегда называло 320 МиБ независимо от требования. Проверка использует крупный synthetic plan с требованием 673 МиБ; до исправления тест видел 320 МиБ. Порог и резерв не уменьшены.
- Ошибки места/квоты ENOSPC/EDQUOT, доступа EACCES/EPERM и занятого файла EBUSY/ETXTBSY преобразуются в фиксированные сообщения с действием для пользователя через существующий `InstallationStatus.message`. До исправления код ENOSPC давал только общее «Не удалось подготовить файлы». Явные безопасные `UserFacingError` сохраняются, неизвестные ошибки не раскрываются, отмена имеет приоритет.

В тестах для шести кодов проверены сохранность старого receipt, освобождение lock, успешный повтор и отсутствие искусственных token/path/command/cause в DTO. Raw message/cause/stack и сырые command lines наружу не передаются. IPC, renderer, состав JAR, source stamp и `launchReady=false` не менялись.

## Проверка восстановления

Добавлен интеграционный тест `InstallationService → downloadPlan → downloadArtifact` с настоящими временными файлами и hash-check, но подставленным HTTP transport и native extraction. В одном disposable-инстансе есть проверенная библиотека, повреждённая библиотека и старый receipt. При отмене после 7 байт `.part` старый receipt и повреждённый target остаются прежними. Повтор запрашивает `Range: bytes=7-`, проверяет и заменяет target; корректная библиотека не скачивается и не перезаписывается, её mtime и байты совпадают. Успешный receipt записан только после завершения, с `launchReady=false`; `.part` исчезает.

Это проверка существующего resume-контракта при изменении lifecycle. Нового downloader или изменения integrity-политики нет.

## Файлы и проверки

- `apps/launcher/src/main/installation.ts`: admission guard, безопасная диагностика, требуемое место.
- `apps/launcher/test/installation.test.cjs`: пять новых тестов, включая таблицу шести ошибок и интеграционное восстановление.
- `docs/INSTALLER.md`: описание фактического поведения.
- `docs/INSTALLATION-RECOVERY-HANDOFF-2026-11-02.md`: эта передача.
- `docs/qa/installation-recovery-2026-11-02.json`: краткое свидетельство.

**127 launcher tests PASS**, 0 failed/cancelled/skipped; TypeScript PASS; обычный Prettier-check изменённых code/test-файлов PASS; полный launcher Prettier с `--end-of-line auto` PASS; `git diff --check` и staged review PASS. Java-исходники не менялись и повторно не компилировались.

`SPACE_REQUIRE_GAME_BUNDLE=1 npm.cmd run build:launcher` — PASS с прежним отдельно скопированным и штатно проверенным bundle: SHA-256 `ae93cb849aede7902f8328bb717b696d292c109bac569d7d61dc7bc69fbe0635`, game tree `9bbd0de084a345e438b7723c5a3ba6bb9c0f4b0d`. Интеграционный чат после объединения модов собирает свой актуальный JAR.

Скрытый `npm.cmd run check:ui --workspace @space/launcher` — PASS: 10 checks, 14 generated screenshots, 0 renderer errors, три экрана/четыре viewport, прежний bridge из 13 функций, missing-Java failure/retry, профили и сохранение. Отчёт: `.reports/launcher-ui/run-fdb5100c-3ed5-4722-9f53-c1497aa63a6d/report.json`, `2026-11-02T16:05:30.793Z`. Игра, native picker, настоящая установка через UI и физическое окно в этом прогоне не проверялись.

## Codebase Memory и границы

Использован собственный `spaceclient-core-features-20261102`, root точно этой копии. До изменений выполнены `index_status`, `search_graph`, `trace_path(downloadArtifact)` и exact-path coverage пяти relevant paths. Граф неверно связывал файловый `open` с `ProfileStore.open`; source проверен напрямую. После изменений сделан full reindex собственного проекта через CLI с временным `CBM_ALLOWED_ROOT` этой копии, затем повторные graph/coverage: ready, `2026-11-02T16:05:28Z`, 8789 nodes / 19736 edges. Новый graph находит `failureMessage` и `InstallationService` по правильным путям/строкам. Coverage обоих изменённых code/test-путей: `no_recorded_issue`, freshness всё ещё `metadata_changed`; полнота или свежесть из этого не выводится. Прежняя недоступность Git-метаданных индекса остаётся ограничением; branch/HEAD подтверждены штатным Git. Отмеченные места прочитаны и сверены через `rg`. Чужие индексы/roots и глобальная конфигурация не менялись.

Пять новых тестов детерминированы и не обращаются к реальной сети. Физическое заполнение диска, системные ACL и настоящие блокировки NTFS не воспроизводились: их известные Node error codes подставлены. Native extraction в интеграционном тесте подставлен; его прежние отдельные тесты проходят в общей серии. Игра, пользовательские процессы/options/config/миры и общий `.reports/launcher-desktop` не затронуты. Изменения локальные, без push, закрытия issues, аккаунтов или публикации. Установка объединённой сборки остаётся у исходного чата.

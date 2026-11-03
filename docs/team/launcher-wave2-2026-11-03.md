# Launcher wave2 · Space account and recovery · 3 ноября 2026

Готовый локальный срез для ревью. Source, strict build, native installer mechanics и три hidden UI прогона проверены. Финальные тяжёлые процессы завершены, новые не запускаются; общий актуальный game bundle и его конечный setup собирает ведущий.

- Собственная копия: `C:/Users/user/Documents/Codex/2026-11-03/space-client-launcher-agent/work/spaceclient-launcher`.
- Ветка: `feat/launcher-account-recovery`, база `cf20fc1c134f53848623097ff297d459640a6d44`.
- Основной source commit: `8c6ddf0b3841fddb6ccc67cf9dcfa12b3b43cb35`.
- UI/guard QA fixes: `0464e32ea6656a2761167081180741fe70487fb4`.
- Короткий staging: `d89d56541840b089f9842e145c52354b56d4a8d4`.
- Process-only long paths и отдельный тест >260: `5835eea4a60dfca3566eca6470718d3eb48aae2b`.
- Владение: только `apps/launcher/**` и этот документ. Root package/lock, README, общий handoff, API/website/game source и чужие копии не менялись. `@space/account-contracts` уже присутствует в foundation dependency/lock; новых внешних зависимостей нет.

## Реальный аккаунт и явное применение

Main использует фиксированные HTTP paths по ACCOUNT-AND-COSMETICS-CONTRACT. Origin выбирает оператор через `SPACE_ACCOUNT_API_URL`; default — `http://127.0.0.1:3001`. Допускается HTTPS либо явный loopback HTTP; userinfo/path/query/hash отклоняются. Renderer не выбирает URL. Redirects запрещены, HTTP ограничен timeout и размером ответа, неизвестные ошибки заменяются безопасным сообщением. Пароли не trim-ятся; допускаются пробелы/вставка и 15–128 Unicode code points. Username/nickname и unknown fields повторно проверяются в main до привилегированных портов.

Реализованы register/login/logout, refresh profile, optimistic-revision nickname edit, skin/cape upload/delete. Space не подменяет Microsoft authentication: локальный launch по-прежнему использует offline identity, а не официальный access token/entitlement.

`SpaceSessionVault` отделён от MSAL. Session token остаётся в main; диск содержит только OS-encrypted `auth/space-session.bin`, привязанный к API origin. Используется существующий Windows async safeStorage adapter. Нет plaintext fallback. Envelope/размеры/срок действия валидируются; сохранение атомарное. В renderer возвращается allowlisted public profile, expiry и проверенные PNG previews, без session token, пароля, HTTP/OS ошибок или новых файловых путей. Старый Snapshot launcher не расширялся account секретами.

Вход не меняет локальный ник. Apply требует явного `confirmIdentityChange=true`, известного ID и отдельного UI подтверждения риска смены offline identity/инвентаря. Ник остаётся существующим общим локальным ником лаунчера; public appearance записывается только в выбранный instance. Одно только изменение профиля на сервере не переименовывает локального игрока.

До следующего spawn, внутри reservation LaunchService, main проверяет выбранный public appearance snapshot и при доступном API обновляет его. Сначала записываются проверенные hash-named assets, затем атомарно properties pointer. При Apply ошибка сохранения nickname откатывает properties. Старые непомеченные изображения не удаляются; credentials не попадают в instance.

Точный экспорт: `config/space-appearance.properties`, schemaVersion=1, ровно accountId/username/nickname/skinSha256/skinModel/capeSha256, плюс `config/space-appearance/skin-<sha256>.png` и `cape-<sha256>.png`. Проверяются descriptor size/hash/dimensions, фактические PNG chunks/CRC/zlib/filter/pixels, symlinks/hardlinks и bounds. Native picker доступен только в main. Renderer показывает подписанную развёртку изображения, без заявления о 3D-preview.

Ограничение PNG этого desktop-клиента: Adam7/interlaced PNG отклоняется до upload; сообщение и UI требуют PNG без чересстрочной развёртки. Поддержанные обычные PNG проверяются полностью, включая palette indices. HTTP DTO и appearance contract не менялись.

## Offline и границы мутаций

Локальный logout удаляет encrypted session независимо от доступности API. Revoke выполняется best-effort; при неуспехе UI честно сообщает, что серверная сессия истечёт по сроку. Локальный ник и последняя public appearance не переименовываются и не удаляются.

Guest без ранее применённой appearance не требует HTTP или доступного encryption provider. После Apply при выключенном API используются прежние properties/assets, повторно проверенные локально по hash/PNG. Ник не меняется. Повреждённый snapshot не проходит проверку; online смена nickname требует нового явного Apply.

Maintenance резервирует apply/backup/restore/diagnostics до первого await. Запущенная игра, startup или подготовка блокируют эти действия; конкурирующие local nickname/profile save/create/select/Java choice отклоняются. Profile mutations тоже резервируются, чтобы maintenance не обгонял уже начатое сохранение. Read-only state/launch/installation status доступны. Shutdown закрывает новые мутации и ожидает текущие операции; пользовательскую игру не завершает.

Bridge расширен до **26 явных typed-команд**: прежние 13 + account get/register/login/logout/edit/skin upload/cape upload/skin delete/cape delete/apply и profile backup/restore/diagnostics export. Generic invoke, renderer Node/fs/shell и произвольный путь не добавлены. Sender/mainFrame/URL guards сохранены; architecture test разрешает shared только type-only public import `@space/account-contracts`. CSP сохраняет запрет renderer network/inline/eval; `data:` разрешён только img-src для main-validated PNG.

## Резервная копия, диагностика и rollback установки

Backup включает name/memory выбранного launch profile и ровно:

- `options.txt`;
- `config/spaceclient-hud.properties`;
- `config/spaceclient-mods.properties`;
- `config/spaceclient-browser.properties`;
- `config/spaceclient-waypoints.properties`;
- `config/spaceclient-cosmetics.properties`.

Нет saves, Java path, глобального nickname, credentials, account session, public appearance или raw logs. Лимит файла — 512 КиБ, envelope — 4 МиБ; checksum detects corruption, exact whitelist/profile ID проверяются до изменения. Новая копия атомарно заменяет предыдущую в private launcher recovery folder. Restore требует подтверждения, возвращает все прежние bytes при промежуточной ошибке и сохраняет ProfileStore последним. Неизвестные поля, oversized/link файлы и другой profile отклоняются.

Safe diagnostics содержит только platform/version/build identity, memory/Java-selected boolean, installation phase/count/retries и launch phase/exit code. Отсутствуют ник, account ID/username, локальные пути, env/API URL, содержимое settings/worlds и журналы. Сохранение через main-only JSON picker атомарно; reserved Windows filenames, links и oversized destination отклоняются. В QA picker подставляется, системные диалоги не открываются.

InstallationService snapshot-ит только прежние JAR и receipt перед подготовкой. При ошибке/отмене восстанавливает `mods/space-client.jar` и `base-installation.json` побайтно; при прежнем отсутствии удаляет только собственные новые файлы. Compiled pin/full preflight не ослаблены. Late cancel после commit ждёт подготовленный результат вместо зависания cancelling. Download cache reuse сохранён; полный rollback всех базовых библиотек не заявляется.

## Setup и длинные пути

Setup SHA-256 payload проверяется до распаковки. Весь список ZIP entries валидируется до записи; traversal/duplicates/device names/links/size limits сохраняются. Распаковка идёт в частный sibling staging с commit через Directory.Move. Отмена проверяется до работы и между bounded copy chunks; очистка ограничена только своим новым staging, существующий target/user data не удаляется. Production GUI получил кнопку отмены; этот GUI по запрету видимых окон не запускался.

Источник должен быть committed до package-setup; сборщик требует strict game bundle, копирует актуальный launcher dist, обновляет icon и записывает setup-manifest с source commit/payload digest/game pin. Java и Minecraft binaries/assets в setup не входят.

Первый native run `build-PaCci8` на source 0464e32: обычная распаковка 98 файлов и все hashes совпали, existing target сохранён. Unicode/space destination дал exit1; исходный stderr старый assertion не сохранил, конкретное исключение не приписывается. Проверка именно `test-lK8lFq` даёт максимум 244 для target и 279 для первоначального staging. Причина с legacy MAX_PATH выведена из этих длин и старого source; предыдущая оценка249 ошибочно включала имя harness как каталог теста. Старые setup/payload/harness/source и `.reports/launcher-setup/build-PaCci8/failure-evidence.json` сохранены. Это не доказательство, что сама кириллица или пробелы не поддерживаются.

`d89d565` уменьшает служебное staging имя. `5835eea` отдельно задаёт .NET Framework 4.6.2 semantics, `UseLegacyPathHandling=false`, `BlockLongPaths=false` до I/O и longPathAware/asInvoker manifest — только внутри процесса/пакета, без изменения LongPathsEnabled/реестра. [Основание Microsoft для long paths](https://learn.microsoft.com/en-us/dotnet/framework/migration-guide/retargeting/4.6.x) и [path normalization](https://learn.microsoft.com/en-us/dotnet/framework/migration-guide/mitigation-path-normalization). Тест различает Unicode/space ниже260 и отдельный реально длинный target >260; успех первого не переносится на второй.

Финальный setup/native QA **pass**: `.reports/launcher-setup/build-o3Tm7g`, source `5835eea`. Все 98 файлов совпали по размерам и SHA-256 при обычной распаковке, Unicode/space path с максимумом **244**, и отдельном реально длинном path с максимумом **398** UTF-16 units. Три стадии отмены (0/6/20) не оставляют target/staging, existing target и его marker сохранены, повреждённый payload отвергается по SHA до записей, четыре hostile ZIP fixtures отвергаются до записей. Результат: `qa-result.json`, hashes/source pin: `setup-manifest.json`.

Собранный unsigned development setup: 162 676 224 bytes, SHA-256 `463b7163e31634ada920ad5ba0d4648e5ca4d4dae684a4922e7a78d028abd4dc`. Payload SHA-256 `02ffa55b7034e1f15f98b515d7e6cb29b10a16b8895b1d63c3624bd538636f75`, 12 bundled package license groups, 7 icon sizes. Production executable с manifest скомпилирован, но GUI/COM installation не запускались; native harness использует тот же SafeArchive/PathPolicy source. Старый failed `build-PaCci8` сохранён отдельно, его успехом не заменяли.

Игровой bundle этой копии не пересобирался: скопированы проверенные bytes из основной foundation-сборки, tree `92a2bd0a8b78f1ea6dbdacc55b3370e78dfbfb64`, SHA-256 `0f439a1731ec586c5d62c763020502a05b53bef2c0fac42f354ea34fb19319af`, 1 082 359 bytes. Pin/classes/mixins не менялись. Это проверка механики installer на своём source snapshot; финальный setup с актуальным общим bundle собирает ведущий после интеграции. Этот prototype EXE не выдаётся за новый общий игровой релиз.

## Проверки и артефакты

- **154/154 launcher tests**, 0 fail/skipped/cancelled: account HTTP/credentials/PNG/vault/real service fixtures; lifecycle; six-file recovery, corruption/rollback/diagnostic privacy; JAR+receipt rollback при ошибке и отмене; maintenance races при backup/restore/Apply и competing profile/nickname; прежние security/architecture/launcher tests.
- Prettier check всех 36 затронутых JS/TS/TSX/CSS/JSON файлов и этого отчёта (37) pass; diff/check и staged diff просмотрены. AGENTS/C#/manifest не массово форматировались.
- Launcher TypeScript pass; strict build на source5835eea pass, в финальном build нет duplicate-key warnings.
- Real local account E2E `http://127.0.0.1:55951`: финальный pass, 5 grouped checks/13 screenshots, rendererErrors=[]; report `.reports/account-recovery-ui/run-77d69c19-dd8f-4ad9-b151-32c5071b1210/report.json`. Созданы только новые QA accounts в разрешённой БД website-агента; новые пользовательские API/website ports не использовались.
- В этом E2E реально выполнены register/login/logout, me/PATCH/skin/cape PUT/DELETE, asset fetch, Windows OS-encrypted session и restart. Guest nickname не изменён до Apply. Нативные PNG/save pickers заменены main-only fixtures. Before-spawn hook настоящий, дальнейший процесс имитируется; Java/Minecraft не запускаются.
- E2E проверяет 26 bridge functions, no Node globals, native picker cancel/invalid PNG, profile/appearance preservation при recovery, safe diagnostic export, компактные widths, forced-colors/reduced-motion. Найденный в первом прогоне узкий sidebar overflow исправлен в 0464e32; отдельный sidebar width assertion прошёл в финальном повторе.
- `.reports/launcher-build-identity/wave2-tests.txt` — финальный unit output.
- Общий UI: 10 grouped checks, 14 report PNG + overview, rendererErrors=[]; `.reports/launcher-ui/run-cb214ee4-f10b-49fe-9e17-794e174245b8/report.json`. Identity UI: 5 групп, 15 PNG, rendererErrors=[]; `.reports/launcher-identity-ui/run-7c3ab223-8f97-4bbd-8f14-85f95dfd2f2a/report.json`.
- Все **43 финальных кадра** просмотрены в шести contact sheets: 15 general +15 identity +13 account/recovery. Четыре ключевых account/recovery кадра дополнительно полноразмерно: skin/cape previews, recovery/diagnostics, width500, forced colors. Два сырых PNG fixtures не посчитаны как UI кадры.
- Первый setup build/result — `.reports/launcher-setup/build-PaCci8`; не выдаётся за успешный Unicode/long-path run.

Промежуточные ошибки не скрыты: Node strip-only tests сначала отклонили TypeScript parameter properties, они заменены explicit fields; после изменения serialization сохранён прежний duplicate Java-picker message; duplicate prebuild/precheck hooks удалены в 0464e32. Успешный первый real account E2E `run-e81063c9-81e2-43d1-80dd-36902a1a5a50` имел duplicate-key warnings; финальные build и повтор77d69c19 прошли без них. Final native/UI прогоны выполнены в отдельном окне до performance freeze по сообщению ведущего; после них остались только чтение/отчёт/commit.

## Память кода и оставшиеся границы

Отдельный Codebase Memory `spaceclient-launcher-20261103`, root — собственная копия. Refresh `2026-11-03T06:37:55Z`: 9509 nodes/25782 edges, skipped0, до двух последних setup fixes. search_graph находит beforeLaunch; inbound trace возвращает0, реальный caller проверен в main/index и LaunchService напрямую. Exact paths metadata_changed, CSS partial; scripts/tests/docs/assets excluded. Scope coverage 24/24 просмотрен, исключённые исходники/QA и новый PathPolicy/manifest прочитаны с диска. Полнота графа не заявляется; новый тяжёлый reindex во время performance freeze не запускался.

Использован ui-ux-pro-max: accessible authentication, paste/autocomplete, явные labels/controls, error recovery и текущая Eclipse palette. Никаких новых внешних пакетов, сетевых renderer resources, Computer Use, OS input, видимых тестовых окон, user worlds/processes, push/main/deploy/Microsoft или новых чатов/агентов.

Это не fresh Windows VM и не новая Windows security identity: собственные app userData/instance/installation paths на текущем Windows, .NET Framework file version4.8.9363; installer требует современную .NET4.6.2 semantics. Production setup GUI/COM shortcut, clean Windows/full download/runtime/game/upgrade/uninstall, real assistive technology, packaged installed entry startup и полный offline-input/graphics/performance не приняты здесь. User runtime и видимые сайт/лаунчер оставлены. Физические V/C и Alt+Tab не запрашивались повторно. Финальная общая game integration/запуск и финальный setup принадлежат ведущему. Срез остановлен для ревью.

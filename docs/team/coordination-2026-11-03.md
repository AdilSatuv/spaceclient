# Команда Space Client · 3 ноября 2026

## Текущее поручение: вторая волна

После приёмки первого среза пользователь прямо запросил настоящие Space-аккаунты на сайте/в лаунчере, ник/скин/плащ, grid/list, расширенный HUD editor, загрузку/настройки и косметику. Регистрация больше не отложена. Пользователь отдельно разрешил параллельные задачи тем же трём существующим исполнителям. Старое ограничение в 13 IPC заменено конечным типизированным расширением с теми же проверками sender/payload и main-only secrets. Текущий общий контракт — [ACCOUNT-AND-COSMETICS-CONTRACT.md](../ACCOUNT-AND-COSMETICS-CONTRACT.md); промежуточный статус — [CHAT-HANDOFF.md](../CHAT-HANDOFF.md).

Launcher владеет аккаунтом и public export, encrypted session, backup/restore/diagnostics, installer/rollback и своими UI/QA. Website владеет persistent Nest service, BFF и /account, общими E2E и API-worker diagnostics. Minecraft UI владеет каталогом/редактором/переводами/темами загрузки и benchmark. Корень владеет контрактами, cosmetics/**, SpaceClient initialization, косметическими mixins, итоговым точным allowlist/pin и общими docs/приёмкой. Local-only, без push/main/deploy; Microsoft/public hosting отложены. Visible launcher/site первой волны запущены по явной просьбе пользователя; QA остаётся в отдельных hidden/headless profiles. Физическую проверку управления пользователь отложил, повторного вопроса не требуется.

Нижние разделы описывают историю первой волны. Их «без регистрации», 13 IPC, контрольные суммы и ограничения ранних checkpoints не являются текущим планом второй волны.

Координатор: чат `01a19f85-e481-7790-be82-7a265822256f`. Исходный чат `01a1838a-6e55-7f10-98bc-252606f045f8` передал работу и не выполняет параллельных изменений. Пользователь прямо поручил управление командой и создание отдельных чатов. Новое поручение заменяет прежнее ограничение делегирования. Работа только локальная: без push, изменения main, публикации, регистрации аккаунтов и нового публичного установщика.

Проверенная исходная точка: clean `0231800a960c6195bd528c040053607dbd2bd6ac`, ветка `space-minecraft-menus`, основная копия `C:/Users/user/Documents/Codex/2026-10-28/space-client-minecraft-menus/work/spaceclient-menus`. Главный Git, README, общий handoff и пользовательский runtime принадлежат только координатору. Исполнители создают отдельные local clones, не worktrees, с собственными зависимостями, caches, output и индексами Codebase Memory. Перенос в основную копию — только после ревью завершённых commits, последовательно.

| Чат, hostId=local | Рабочая копия | Владение и первый срез |
|---|---|---|
| Лаунчер `01a19f83-edd6-79e1-9bb7-93b9558cb122` | `C:/Users/user/Documents/Codex/2026-11-03/space-client-launcher-agent/work/spaceclient-launcher` | `apps/launcher/**`, собственный отчёт. Идентичность bundled/preflight-verified development сборки и понятная подготовка/повторная попытка. Ровно 13 IPC, безопасные DTO и изоляция renderer. |
| Сайт `01a19f83-f10b-7be0-874f-c263528ab41f` | `C:/Users/user/Documents/Codex/2026-11-03/space-client-website-agent/work/spaceclient-website` | `apps/website/**`, контент `apps/api/**`, релевантные tests и website QA scripts, собственный отчёт. Каталог фактических 55 модулей, поиск/категории/пустое состояние. Общий контракт менять только после согласования. |
| Minecraft UI `01a19f83-f3e5-7991-8e83-25a0e75e8d51` | `C:/Users/user/Documents/Codex/2026-11-03/space-client-minecraft-ui-agent/work/spaceclient-minecraft-ui` | Только UI в game menu/hud/settings и UI tests, собственный отчёт. Законченный компактный срез RU/EN помощи и keyboard navigation. Сохранить 55 cards/actions, schemas/runtime/network guard. |

Статусы читать через компактный `wait_threads`. Обычного final исполнителя достаточно; не требовать обратного сообщения. Новые чаты/агенты для этого среза не создавать.

Для UI используется `ui-ux-pro-max`; существующие graphite/gold, Eclipse/Ringworld, космический фон и левое главное меню сохраняются. Проверки только hidden/headless и с собственными профилями, без Computer Use, видимых окон, OS input и пользовательских миров/процессов. Ручную физическую проверку пользователь отложил; не спрашивать повторно без нового контекста.

После ревью: интеграция отдельных направлений, game source commit до окончательной сборки, обновление строгого bundle/class контракта при новых классах, общие tests/types/build и осмотр кадров. Сборка и hidden handler QA не означают публичный релиз, полную multiplayer-матрицу или приёмку физических клавиш. Текущий установленный JAR не переустанавливать без новой необходимости; не завершать пользовательскую игру.

## Промежуточное ревью · 3 ноября, 10:51 (Asia/Dhaka)

Координатор сохранил `9c48704` (границы команды) и `f260d5a` (Prettier `endOfLine:auto`, полный обычный `format:check` прошёл без массовой перезаписи). Основные продуктовые изменения остаются в отдельных копиях: source review выполнен, общей интеграции и установки ещё нет.

- Лаунчер: allowlisted build DTO, `dev-<12 hex>`, отдельная историческая проверка установленного JAR после полного preflight, native details, безопасная повторная попытка. Types/strict build, базовые 10 hidden UI checks с 13 IPC прошли. Первый синтетический прогон новых состояний: 5 групп / 15 кадров, без renderer errors; координатор просмотрел полные details, compact500 и state-error. Найден ID ниже fold1000×680; исправление и повтор ещё требуют проверки.
- Сайт: 55 записей сверяются с игровыми enum; два инструмента сохранены отдельно. DTO прежний. Агент сообщил 40 general +26 API +135 launcher tests, types/tokens/website build pass. Browser timeout разбирается, не считается успешной приёмкой.
- Minecraft: source commit `cc7a3ab`, 278 Java tests pass после исправления слишком длинной локализованной подписи. RU/EN UI shell/help, disabled help и безопасные стрелки, новый `BuildIdentity.class`, ID предусмотрен от width240. Native hidden QA ещё не выполнена.

На контрольной точке все три чата имеют `waitingOnApproval` для скрытых QA-процессов. Существующие запросы оставлены пользователю; повторять запуск из другого чата для обхода разрешения нельзя. Product branches пока не переносить в основную и не выдавать отдельные unit/build результаты за общую приёмку. Пробный required-class/omission patch для `BuildIdentity.class` прошёл 10 inspector tests и снят с основной копии до интеграции нового game source; добавить его тогда одновременно с positive fixture. Пользовательский JAR не менялся.

Codebase Memory отдельных copies сталкивался с allowed-root / secure CLI endpoint errors; успешную индексацию без нового evidence не заявлять. Основной индекс доступен; metadata_changed остаётся best-effort оговоркой.

## Завершение среза

Все три чата завершились обычным final и находятся idle. Итоги приняты координатором, последовательно объединены и дополнительно проверены в основной копии. [Ревью, исправление, рефакторинг и финальная приёмка](../TEAM-REVIEW-2026-11-03.md). Snapshot кода710a877; main/source/game и пользовательский runtime по-прежнему разделены. Выданные задачи не продолжаются автоматически. Исторический промежуточный блок waitingOnApproval выше больше не описывает текущий статус.

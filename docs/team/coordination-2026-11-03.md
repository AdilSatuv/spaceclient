# Команда Space Client · 3 ноября 2026

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

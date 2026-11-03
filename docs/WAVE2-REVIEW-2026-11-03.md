# Space Client: аккаунты, HUD, загрузка и косметика

Локальная вторая волна 3 ноября 2026 объединена из трёх существующих чатов, просмотрена и дополнительно проверена координатором в основной копии. Работа выполнена в `space-minecraft-menus`, без push/main/public deploy. Эта запись заменяет промежуточный статус второй волны; прежние отчёты остаются историческими.

## Что работает

- **Space-аккаунт на сайте и в лаунчере:** регистрация/вход/выход, сохраняемая серверная учётная запись, изменение игрового ника, загрузка/удаление скина и плаща, classic/slim и предпросмотр. Оба клиента используют один Nest-сервис. Вход не меняет локальную игровую личность: применение профиля явно подтверждается отдельной кнопкой.
- **Внешность в Minecraft:** перед запуском лаунчер экспортирует только проверенные общедоступные PNG и properties; production Java-парсер читает фактический экспорт. Игра применяет внешность локального игрока без HTTP в render loop. Это локальная внешность Space, не изменение аккаунта Mojang и не глобальная видимость другим клиентам.
- **Отдельная вкладка косметики:** три геометрии крыльев Aurora/Ember/Crystal, два нимба Ring/Orbit, четыре палитры, размер 60–140%, анимация и переключатели скина/плаща. В мире показана настоящая модель игрока; вне мира — явно обозначенная схема.
- **Каталог 55 модулей:** сохраняемые Grid/List, F6, поиск, категории, избранное и клавиатурное управление. Browser schema2 сохраняет миграцию/backup schema1.
- **HUD editor:** выделение нескольких элементов, Ctrl+A, групповое перемещение, выравнивание/распределение, числовые X/Y/масштаб 50–300%, lock/hide/show, undo/redo32 шага. Прежние профили/import/export сохранены. Постоянная надпись SPACE CLIENT убрана; маленькая подпись SPACE включается по желанию и по умолчанию выключена.
- **Загрузка и меню:** своё оформление раннего окна запуска Minecraft, загрузки мира/территории, progress, подключения и ошибок, дополнительные RU-подписи и настройки. Реальный процент показывается только когда vanilla предоставляет его; Cancel, возврат с ошибки и copyright сохранены.
- **Полезное в лаунчере:** backup/restore шести разрешённых файлов настроек, безопасный экспорт диагностики, откат JAR/receipt при ошибке/отмене обновления, понятное восстановление и отдельные состояния аккаунта. Миры и секреты в backup не входят.

## Ревью и исправления

Секреты остаются на стороне Nest/Electron main: scrypt, хеши ограниченных по времени сессий, Windows-encrypted vault без plaintext fallback; renderer получает только публичный DTO. Next BFF использует HttpOnly/SameSite cookie, origin/CSRF и фиксированные маршруты; общий пакет контрактов не зависит от Node/Next/Electron. Лаунчер имеет 26 конечных типизированных IPC. Никакого универсального invoke или URL из пользовательского профиля.

Сервер полностью декодирует ограниченный PNG и создаёт metadata-free RGBA PNG. Клиенты проверяют размер, формат и digest. Исправлены строгая проверка classic/slim, ограничение заголовка до browser decode, внешние слои скина и зеркальные конечности legacy64×32; цветные пиксельные fixtures проверяют результат. Смена локального ника и изменения instance сериализованы с запуском/maintenance; работающая игра защищена.

В игровом UI исправлены наследуемые обработчики клавиатуры Connect/Disconnected, граница End у числовых ползунков и заполненная ось Inventory при 300%: недоступная координата теперь безопасно отключена вместо исключения. Исходное падение favorites QA было ошибкой старого fixture-path; production filtering ради теста не меняли.

Строгий bundle инспектор требует все 43 точных production mixins, новые classes/resources и текущий SHA. Тесты удаления обязательных entries сохранены. В skin/cape mixin документированы обратные имена старых mappings; соответствие подтверждено native bytecode/рендером.

Installer проверяет SHA всего payload перед распаковкой, пишет в соседний staging и атомарно переносит готовую установку; ошибка/отмена сохраняет прежнюю. Исправлены длинные staging paths и process-only Windows long-path support: manifest/TargetFramework/AppContext, без изменения registry или глобальных настроек.

## Итоговая проверка

**530 тестов прошли:**45 общих,30 API,155 launcher,300 Java. Workspace types/tokens, API/Next/game/strict launcher builds и format прошли. Npm-набор проверен после интеграции; финальное изменение zero-axis затрагивало только Java, после него 300 Java и game/full bundle build повторены. Старые тестовые падения сохранены отдельно и не считаются успешной приёмкой.

| Проверка | Фактический результат |
|---|---|
| Основной сайт |8 HTTP;42 responsive layouts и 31 flow; аккаунт 1440/390, точные пиксели overlay/legacy preview, CSRF/HttpOnly/conflict/logout/restart |
| Лаунчер основной сборки |10 UI groups +5 identity groups; отдельный account/recovery E2E с настоящим loopback API и Windows encryption; ширины 1000/600/500 и forced colors |
| Общий аккаунт |4 группы: сайт → launcher main → фактические файлы → production Java parser; независимый logout; stale revisions |
| Основной native JAR |classic/slim: по 18 кадров и 40 native key-handler steps, exit0, GL error0; compact Inventory300% входит в проверку |
| Input/закрытый TCP на основном JAR |224 handler calls,29 shortcuts,26 HUD samples, FreeLook6, death chime1, macros2/1, reconnect/change-host;64 закрытых queued packets отброшены |
| UI/загрузка в изолированной копии агента |37 UI +5 loading PNG,267 handler steps; настоящий Cancel/error return и loading hooks; один ожидаемый Connection refused, без неожиданных ERROR/FATAL |
| Windows setup основной сборки |98 файлов совпали по SHA; прежняя цель сохранена; Unicode/spaces242 и реальный путь 396 символов;3 отмены, corrupted payload и hostile ZIP отклонены |

Account UI E2E проверяет настоящий before-launch export, но сам spawn симулирует. Cross-client harness использует isolated in-memory vault. Эти ограничения не подменяются результатом отдельного Windows-encrypted UI E2E или native игры. Вся автоматизация использует собственные профили/миры и hidden/headless surfaces, без OS input.

Основной frozen game tree `22056fa560909dc8cfd05fe7adec7886b0404177`, ID **dev-22056fa56090**, JAR **1144767bytes**, SHA-256 **d7119edc32c1e87af9966280a8e540cb05f07d804d9458d2b6e964ffad4fdc30**. Тот же файл включён в launcher и setup. Самостоятельный UI JAR агента имеет другой stamp; он не выдаётся за основной JAR.

Подробные исходные отчёты: [launcher](team/launcher-wave2-2026-11-03.md), [website](team/website-wave2-2026-11-03.md), [Minecraft UI](team/minecraft-ui-wave2-2026-11-03.md). Машиночитаемый итог — [QA JSON](qa/wave2-2026-11-03.json). Сырые журналы — ignored `.reports/team-review`; воспроизводимые helpers — `scripts/qa`.

## Производительность

Принята как короткое разведочное наблюдение серия `89f63943-a4b7-4706-8066-b4cfc86940bf`:12 новых JVM, по 3 повтора,3s warmup +6s sample, RTX3060,1280×720/GUI2/distance4, flat seed54321, фиксированная серверным teleport фактическая позиция 0.5/4/0.5/yaw0/pitch0 во всех 12. Gameplay29 выключены, public appearance/wings/halo отсутствуют. Vsync off,260 — unlimited sentinel. Весь helper вне production JAR, контроль включает тот же helper и Fabric. Последние тяжёлые agent jobs завершены 07:46:49Z; серии начаты позже, пересечения по журналам нет. Raw logs и отклонённые попытки сохранены.

| Профиль | Медиана loopFPS | Медиана P99 interval,ms | Working set после sample,MiB | JVM→Title marker,ms |
|---|---:|---:|---:|---:|
| Minecraft + Fabric без Space |763.0|2.201|829.9|5299|
| Space, HUD выключен |755.5|2.212|838.7|5476|
| Space,5 default HUD |667.9|2.427|971.2|5596|
| Space,26 HUD включены |437.2|3.395|967.0|5475|

Стандартный HUD в этой сцене даёт примерно на 11.6% ниже render-loop rate против Space с выключенным HUD; heavy примерно на 42.1% ниже. Не выдавать это за ускорение, универсальный overhead или реальный displayed FPS: glFinish сериализует CPU/GPU, короткий прогрев не доказывает JIT/GC steady state, фоновые ограничения намеренно обойдены одинаковым QA hook. Работа разрешённых widget IDs проверена, но пустой инвентарь/сцена не нагружает все игровые эффекты. Уже сгенерированные животные не удалялись.

RAM измерена после сортировки/JSON/запуска PowerShell; сильный разброс типичного профиля 708–997MiB не позволяет приписать разность HUD. Startup заканчивается доставкой stdout-маркера первого Title render callback и использует warm OS cache, это верхняя граница endpoint, не visible frame/cold boot. P99 — nearest rank по каждому trial, таблица содержит медиану 3 P99; per-trial median использует lower-middle order statistic. [Все 12 samples и условия](qa/native-hud-observations-2026-11-03.json), [повторение](../scripts/qa/performance/README.md).

Первая серия с различными server spawn positions отклонена целиком, smoke не включён. После исправления server teleport позиция проверяется на каждом sample frame; изменённый или незавершённый сценарий завершается ошибкой. Полноценная ETW/PresentMon, чистая vanilla, длинная игровая сессия и stress всех эффектов остаются отдельной приёмкой.

## Что открыто для просмотра

Сайт [127.0.0.1:50566](http://127.0.0.1:50566), [Space-аккаунт](http://127.0.0.1:50566/account); общий API50565. Preview поддерживает process-scoped фиксированные порты, чтобы перезапуск не менял cookie/encrypted-session origin. DB остаётся в ignored `.local/space-accounts`; это локальная служба, а не публичный аккаунт-сервис.

Видимый лаунчер перезапущен с новой bundled сборкой и тем же пользовательским профилем. Пользовательские миры/identity/Java не менялись; установленный в instance прежний игровой JAR автоматически не заменялся. Для установки новой bundled версии использовать «Подготовить файлы», затем запуск. Игру за пользователя не запускали.

## Оставшиеся границы

Microsoft-вход и public hosting отложены. Установщик проверен native на текущей Windows в собственной цели; clean VM, подписанный публичный installer и полный packaged-entry lifecycle ещё не приняты. Физические клавиши/Alt+Tab пользователь отложил; полная multiplayer/resource-pack/shader/длительная матрица 55 модулей и полный перевод каждого описания не заявляются. В compact RU строке списка остаётся небольшой визуальный polish.

Единичный ранний выход API test worker предыдущей волны не воспроизведён, первопричина неизвестна. Добавлены проверенные test-only start/exit/uncaught diagnostics без env/секретов и без скрытых retries; нынешние 30 API tests проходят. Это улучшение наблюдаемости, не доказанное исправление неизвестного сбоя.

Финальный refresh Codebase Memory и exact-path coverage завершились `Transport closed`; свежая индексация не заявляется. Последний успешный основной индекс первой волны 9336nodes/24860edges не содержит эту волну полностью. Изменённые источники и вызовы reviewed напрямую; partial/excluded/metadata_changed caveats прежнего индекса сохраняются.

# Block Overlay, Hit Color и FOV Control · 3 ноября 2026

Добавлены три opt-in модуля с эффектом в игровом мире: 14 игровых + 23 HUD = 37 карточек. Все три доступны через правый Shift → поиск названия или Visuals; Hit Color также в Combat, FOV Control в Player. Новые Toggle key по умолчанию не назначены. Настройки имеют живые векторные предпросмотры, поиск/избранное и плавные переключатели используют существующий каталог.

- **Block Overlay**: отдельная обводка, заливка или оба слоя; Gold/Aqua/Violet/Lime/Red/White и анимированный Rainbow с периодом 6 секунд. Заливка 5–60%, линия 1–5. По умолчанию Aqua, Both, 20%, ширина 2. Использует native selection box уже выбранного Minecraft блока, включая полублок; сохраняет depth test и восстанавливает GL state. Попадание курсора, дальность и блоки не меняются. Реальная толщина линии зависит от графического драйвера.
- **Hit Color**: выбранный цвет и сила 5–80% вместо красного hurt tint у видимых living-моделей. Та же палитра, опция Only players. По умолчанию Violet, 55%, все living-модели. Изменяются четыре компонента native tint buffer; здоровые/умирающие модели сохраняют native path. Положение, видимость, урон и здоровье не меняются. Это настройка цвета существующей подсветки урона.
- **FOV Control**: собственный угол 60–110° и сила movement FOV 0–100%; по умолчанию 90°/0%. Ноль подавляет расширение/сужение от спринта, полёта и лука, 100% сохраняет штатный множитель. Используются native interpolation и остальные эффекты камеры. Zoom делит итоговый мировой угол; hand projection сохраняется. Minecraft FOV и movement attributes не записываются. Чужая камера получает native значения. Смена угла/динамики/Zoom и выключение модуля обновляют список видимых участков даже при неподвижной камере или открытом меню; стабильная проекция и hand pass не вызывают лишнего обновления.

## Совместимость

Mods schema8 / 57 полей, чтение 1–7 сохранено; реальный fixture `mods-schema7-0c2c0f5.properties` создан старым JAR до изменения исходников. Все 11 старых модулей, параметры и клавиши сохраняются, новые выключены/unbound. Открытие старого файла его не переписывает; upgrade происходит при успешной атомарной записи. Reset касается настроек и Toggle key выбранного модуля, сохраняет Enabled. Неудачные/некорректные изменения откатываются, ошибочные и будущие файлы остаются read-only. Допустимые вручную заданные числа между шагами корректно переводятся на сетку кнопок. HUD17/export12/browser1 без изменений.

Лаунчер требует новые классы палитры/рендереров/preview, BlockMode и точные 20 production mixins. Лимиты JAR, stamp и Java8 class major52 сохранены. Игровые исходники: `ffb5829` + исправления `b85c3f7` и `154db3d`; тестовые помощники не входят в production JAR.

## Проверено

225 Java + 112 launcher tests, TypeScript, Prettier, game build и строгая launcher bundle build прошли. Native QA: скрытый Canvas 1280×720, отдельный ранее созданный QA-мир. Проверены реальные карточки, поиск, настройки/предпросмотры, capture свободных и отказ зарезервированной клавиши, цикл палитры/режимов, числовые изменения вне сетки и сохранение.

В мире сняты Aqua fill+outline, Rainbow outline и Gold slab; наблюдалась native selection box и включённый depth test. GL snapshot/cache и line width восстанавливаются во всех трёх режимах. Native hurt renderer наблюдался сначала с красным, затем фиолетовым buffer и новой alpha на клиентском образце pig. Players-only/healthy/death/disabled и границы buffer проверены отдельно через runtime policy, включая образец игрока; это не проверка сетевого игрока.

Для FOV использованы native samples спринта, полёта и натянутого лука с прямым заданием клиентского состояния. Проверены 0/50/100%, неизменный native getter, мировая и hand projection, совместная работа с Zoom и чужая камера. Два кадра показывают 90° и 60°. Флаги native terrain culling подтверждают обновление при stationary/paused смене FOV/Zoom, восстановление при выключении и отсутствие лишнего пересчёта при стабильной или hand проекции. Подводный/посмертный эффект отдельно не снимался.

Первый проход завершился с exit0, но на снимке расширенного FOV в меню были видны пропуски дальнего terrain: native visible-chunk cache не учитывает изменение угла у неподвижной камеры. Native regression затем поймал пропуск отдельного RETURN hook: Zoom завершал callback раньше него. Обновление перенесено в общий обработчик итоговой мировой проекции, без зависимости от порядка mixins. Добавлены проверки culling и выполнен полный повтор. Финальные десять кадров осмотрены, exit0, без наблюдавшихся fatal/mixin/texture errors.

Клиентские образцы block/entity создавались только в QA; серверный блок не менялся, клиентский блок восстановлен, образец entity удалён. Пользовательские миры не открывались. Physical input, multiplayer packets, полная resource-pack/shader матрица и производительность не приняты. Native samples не заменяют эти проверки.

Обновление установлено при закрытой пользовательской игре с preflight и сохранением options/config/level.dat; после установки игру не запускали. Pin, SHA, снимки и результаты — [QA](qa/visual-style-mods-2026-11-03.json).

## Codebase Memory и публичные идеи

Проект `spaceclient-mods-menus`, текущий root; graph navigation и source reads использованы. После исправления индекс обновлён 2026-11-02T18:20:29Z (UTC; локально 3 ноября), fast: 7697 nodes / 19219 edges, skipped=0. Trace `BlockOverlayRenderer.draw ← BlockOverlayMixin.spaceclient$blockStyle` и `FovRuntime.projection ← ZoomRendererMixin.spaceclient$zoomFov` разрешены; partial parse style.css/build.gradle/gradlew и `metadata_changed` оговорены. Docs и игнорируемые QA artifacts исключены из графа по правилам.

Публичные описания: [Badlion Block Overlay](https://www.badlion.net/wiki/block-overlay?lang=ja), [Lunar PvP Mods / Hit Color](https://www.lunarclient.com/news/lunar-client-mods-for-the-best-pvp-experience), [Lunar Patch Notes #7 / FOV](https://www.lunarclient.com/news/patch-notes-7). Прочитаны 2 ноября 2026 перед полуночью; реализация, интерфейс и иконки собственные, чужой код и ресурсы не переносились.

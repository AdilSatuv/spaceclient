# Локальное наблюдение запуска — подготовительная часть SC-13

24 октября 2026: выполнены диагностический и три повторных запуска **Space** в новом QA-инстансе, каждый с новым Java-процессом. Главное меню и переход Singleplayer → Select World визуально подтверждены; четыре штатных выхода с кодом 0. Собраны точные границы preflight/spawn и **верхние границы** времени до видимого меню. Это не измерение первого готового кадра мира и не полный сравнительный startup benchmark SC-13.

[Машиночитаемый отчёт, исходные hashes и наблюдения](qa/startup-2026-10-24.json). Старые девять FPS captures не изменены: их `startupMs=null` и `allStartupMeasurementsPresent=false` сохраняются.

## Что измеряется

Один Node 24 QA-процесс владеет монотонным `performance.now()`; UTC используется только для идентификации запуска. Часы Windows между разными процессами не вычитаются.

| Граница | Определение |
| --- | --- |
| Запрос, T0 | Вход в штатный `prepareLocalLaunch` после загрузки модуля. Общее время подготовки профиля, Electron, renderer/IPC и нажатие кнопки пользователем сюда не входят. |
| Preflight complete | Возврат `prepareLocalLaunch`: проверены Java 21 x64, закреплённые metadata, все artifacts, собственный JAR/source stamp, допустимый состав mods и native cache. Runtime и файловые проверки составляют одну общую длительность. |
| Spawn call | Момент непосредственно перед `spawn(executable, args)`. Это выбранный origin для Java-части, не timestamp создания процесса от ОС. |
| Spawn event | Node сообщил об успешном создании дочернего процесса. Это не готовность Minecraft. |
| Menu observed by | Завершён вызов наблюдателя, вернувший снимок именно этого игрового окна с главным меню: логотип, Singleplayer, Multiplayer, Options, Quit Game. Снимок осмотрен, исходные байты сохранены с SHA-256. |
| Response verified later | После первого снимка меню нажата Singleplayer, отдельно осмотрен Select World. Это последующая проверка отклика, не timestamp первого интерактивного кадра. |

Collector подтверждает `begin` до вызова native screenshot и ставит `end` после получения результата по локальным stdin/stdout. Сам интервал `begin/end` включает вызов наблюдателя и доставку снимка; общая верхняя граница от запроса/spawn также включает polling, выбор и активацию окна. Более поздняя визуальная классификация не меняет эти timestamps. В повторах сам первый screenshot-вызов занял около 3,1 с; это существенная часть верхней границы.

У screenshot API нет проверенного timestamp представленного игрового кадра. Сразу после Cancel один диагностический снимок ещё показывал Select World, следующий — главное меню. Поэтому **даже отрицательный снимок не сужает нижнюю границу**. Анализатор сохраняет только интервал `[spawn call, конец первого положительного наблюдения]`. Не вычисляются середина интервала, «точное startup time», первый Present или первый управляемый кадр. `startupMs=null` и `worldReadyMeasured=false` намеренно сохраняются и в новых отчётах.

Ни `Loading Minecraft`, ни инициализация Space/OpenAL, ни текст про texture atlas, ни появление окна не принимаются как готовность меню. Raw Java output сохраняется локально для диагностики, но не используется как endpoint.

## Результаты

| Запуск | Preflight, с | Запрос → spawn call, с | Spawn call → событие Node, мс | Меню наблюдено не позднее, с от запроса | Не позднее, с от spawn call |
| --- | ---: | ---: | ---: | ---: | ---: |
| Диагностический, отдельно | 1,008 | 1,008 | 3,200 | 7,879 | 6,871 |
| Повтор 1 | 1,084 | 1,084 | 1,946 | 6,550 | 5,465 |
| Повтор 2 | 1,015 | 1,015 | 1,980 | 6,392 | 5,377 |
| Повтор 3 | 1,022 | 1,022 | 2,050 | 6,512 | 5,491 |

Это отдельные верхние границы, а не четыре точечных startup latency. Диагностический запуск не смешивается с тремя повторами. Меню было видно уже на первом снимке каждого процесса; более тесной подтверждённой границы нет. Все четыре процесса дали exitCode=0; worlds не создавались и не открывались.

Условия: i5-12400F, RTX 3060, driver 32.0.16.1088, Windows 10.0.26200.0, 16 963 067 904 байт физической RAM; локальный Temurin 21.0.12.1+1 x64, Node 24.15.0, Minecraft 1.8.9, Legacy Fabric 0.18.3, Space 0.1.0. Неизменённый launch plan: Xms512M / Xmx3072M, 1000×680 client area (native screenshot 1002×712 с рамкой), GUI Normal, English, Fast, distance 8, minimal particles, mipmap 0, VSync OFF, maxFps=260, resource packs пусты, Unicode OFF, mouseSensitivity=0.5. Master и четыре HUD-модуля настроены ON, но в главном меню HUD не рисуется; это не тест накладных расходов HUD.

Создана **полная копия** runtime из прежнего изолированного QA-источника в `.reports/benchmark/startup-SlpVa0/instance`; hardlinks, личная `.minecraft`, `game/run`, saves, старые options/config не используются. Новый options/HUD создаются отдельно. До каждого процесса проходит полная штатная проверка файлов. Prelaunch hashes options/HUD совпадают во всех четырёх запусках. Snapshot мира здесь не требуется: endpoint — меню.

Режим кеша: подготовленные файлы, затем один диагностический запуск и три последовательных перезапуска того же QA-инстанса. Копирование и preflight читают artifacts и прогревают файловый кеш; Fabric может создавать собственные caches. OS/GPU caches не очищались и не контролировались. Это **не cold start**, не строгая оценка warm-cache performance и не сравнение с прежней FPS-серией. Фоновая нагрузка, температуры и частоты не измерялись; воспроизводим протокол, а не обещаем одинаковые числа на каждом запуске.

Game tree `ad06bc5f13ca76788f6c0832e9f72e77843219ac`, JAR SHA-256 `11299c35e8812de73373f75c0a3cef721f40308bb7f510fa7387c4f70c376257`. Launcher/game-код и IPC не менялись. Capture-source commit — `bf756e95ba785d69f6d4d207ae4b71394b1a648a` плюс новые QA-скрипты с hashes из session metadata; их точные версии сохранены в raw archive. Финальный анализатор дополнительно запрещает сужать границу по отрицательному снимку и повторно проверяет все evidence hashes; численные интервалы этих четырёх записей не изменились.

Первый отдельный probe `startup-hicBjX` дошёл до меню и штатно вышел, но collector тогда принимал только PNG, а native API вернул JPEG. У него нет принятого наблюдения/summary; он исключён. Сборщик теперь сохраняет исходный JPEG/PNG без перекодирования. Неудачный probe не используется вместо результата и не считается холодным запуском.

## Повторение протокола

Нужны уже подготовленный isolated `.reports` runtime, локальный `.tools/jdk-21`, matching `apps/launcher/dist/game/space-build.json` и чистый game tree. Сборщик ничего не скачивает, не устанавливает и не повышает права. Сначала проверить отсутствие предыдущего QA Java-процесса. Не запускать сборки, ETW или FPS captures параллельно.

Standalone collector использует JSON lines по stdin/stdout:

```powershell
npm.cmd run capture:startup -- .reports/hud-acceptance/instances/default/minecraft
```

Либо оператор Computer Use импортирует `scripts/benchmark/startup-observer.mjs` по абсолютному file URL в `node_repl`, после инициализации `@oai/sky` и чтения computer-use skill:

```javascript
const startupModule = await import(absoluteObserverFileUrl);
const observer = await startupModule.connectStartupObserver(absoluteQaSourceRoot);
await observer.request('start', { label: 'diagnostic' });
```

После spawn выбрать **ровно одно фактически возвращённое** Minecraft-окно локального Java, получить его через `sky.get_window` и активировать. До первой классификации меню ввод не выполнять. Снимки собирать так, сохраняя state для следующего действия:

```javascript
const acquisition = await observer.request('begin');
const state = await sky.get_window_state({
  window: selectedReturnedWindow, include_screenshot: true, include_text: false,
});
const evidence = await observer.request('end', {
  observationId: acquisition.id, dataUrl: state.screenshots[0].url,
});
// Осмотреть автоматически показанный снимок прежде, чем выбирать result/действие.
await observer.request('review', { observationId: evidence.id, result: 'main-menu' });
```

Для загрузочного экрана использовать `not-main-menu`, продолжить наблюдение; все предшествующие снимки должны быть классифицированы. На последнем осмотренном меню нажать Singleplayer, сразу обновить состояние, подтвердить именно Select World отдельным `begin/end/review` с `result: 'singleplayer-menu'`. Если кадр ещё старый, снова наблюдать; не повторять ввод вслепую. Затем `interaction` с ID первого main-menu и подтверждённого Select World (`{from, response}`). После Cancel повторно осмотреть главное меню и нажать Quit Game. Worlds открывать не требуется.

Дождаться `request('status')` с `running=false` и normal exit, затем `request('summary')`. Ненормальный выход, отсутствие визуального меню/response, незавершённый screenshot, нарушенные timestamps/hashes и повторная запись summary отклоняются. В той же session выполнить `start` с `repeat-1`, `repeat-2`, `repeat-3`, каждый раз выбирать новое возвращённое окно и повторять проверку. Не переиспользовать HWND, screenshot ID или координаты без свежего наблюдения. После последнего normal exit вызвать `observer.disconnect()`; отключение observer не убивает Java.

Read-only повторный анализ сохранённого запуска:

```powershell
npm.cmd run analyze:startup -- .reports/benchmark/startup-SlpVa0/repeat-1
node --test tests/startup.test.cjs
```

Новые raw directories уникальны, прежние summaries не перезаписываются. Raw/runtime не коммитятся. В локальном deliverable-архиве сохраняются session metadata, snapshots кода collector/observer, четыре run JSON/log/summary и исходные 16 JPEG с manifest hashes. Системные caches/настройки и прежний QA-профиль не меняются. Обычная чувствительность 0.5 сохраняется.

## Открытая часть SC-13

Нужен общий endpoint с timestamp самого готового игрового кадра и проверкой управляемого мира для vanilla / Space HUD OFF / Space HUD ON; нужны отдельные контролируемые режимы кеша и официальный runtime/авторизованный baseline после SC-10. Данный инструмент и наблюдаемые верхние границы полезны для локальной диагностики preflight/запуска, но не закрывают критерий времени запуска полного benchmark. Сравнительных оптимизаций и обещаний FPS нет.

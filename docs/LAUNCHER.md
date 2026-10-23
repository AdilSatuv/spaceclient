# Space Launcher — первый desktop-каркас

Приоритет пользователя: сначала лаунчер. Это исходный Windows x64 прототип: профили, Java, подготовка/восстановление файлов Minecraft и собственного development JAR Space. Установщика приложения, Microsoft-входа и запуска игры пока нет. SC-07 закрыт независимо от незавершённых игровых проверок SC-02.

## Стек и границы

Electron **45.0.0**, React/ReactDOM **19.3.1**, TypeScript **5.9.3**, esbuild **0.28.2**, Node.js разработки **24+**. Версии закреплены package-lock.json. Electron выбран для первого Windows-прототипа как предусмотренный SPEC кандидат; переход к установщику потребует отдельной оценки размера, RAM и подписания. Версии Electron сверены с npm и [официальным списком релизов](https://releases.electronjs.org/release); это не обещание бессрочной актуальности.

Начальный локальный срез стоимости: распакованный `node_modules/electron/dist` занимает 374 097 589 байт (~357 МиБ). Сумма WorkingSet64 процессов с точным путём этого electron.exe составила 376 340 480 байт (~359 МиБ). Working set может учитывать общие страницы несколько раз; это одиночный снимок, без подтверждённой UI-приёмки, а не private RAM, размер установщика или сравнительный benchmark.

- `src/main/`: настройки на диске, native dialog, диагностика Java, фиксированные каталоги профилей, подготовка и проверка файлов.
- `src/preload.ts`: десять конкретных функций через contextBridge; ipcRenderer/send/execute/fs наружу не передаются. Три installation-функции принимают только ID существующего профиля: get/start/cancel; progress опрашивается с интервалом 650 мс, прямых IPC event streams наружу нет.
- `src/shared/contracts.ts`: типы IPC-результатов и профилей; неизвестные значения повторно валидируются в main.
- `src/renderer/`: React и CSS, локальные ресурсы; нет Node.js и сетевых запросов.
- `scripts/build.mjs`: отдельные main/preload CJS bundles и статический renderer. Нет dev-server с ослабленной CSP.

Следуем [официальным рекомендациям Electron](https://www.electronjs.org/docs/latest/tutorial/security): sandbox/contextIsolation=true, nodeIntegration=false, webSecurity=true. Renderer загружается через `space://app/index.html`; протокол отдаёт только четыре известных ресурса. CSP запрещает inline/eval, сеть, frames, формы и объекты. Все IPC проверяют точные webContents, mainFrame и URL. Окна, навигация, webview и browser permissions запрещены. Это реализованные ограничения и модельные тесты, а не независимый аудит или завершённое runtime-испытание всех обходов.

## Настройки и поведение

`settings.json` schemaVersion=1: список до 10 профилей, активный ID, имя, memoryMb, путь к java.exe. Нет токенов, паролей и произвольных launch arguments. Записи последовательно проходят очередь и временный файл с rename; изменение памяти применяется после успешной записи, временный файл очищается при ошибке. Перед чтением проверяется размер: свыше 64 КиБ файл восстанавливается как повреждённый без JSON parse. При неподдерживаемом или повреждённом JSON сначала создаётся `settings.invalid-<time>-<uuid>.json`, оригинал остаётся до следующего сохранения. Если резервная копия невозможна, исходный файл не перезаписывается.

Память: минимум 1024 МБ, шаг 256 МБ, максимум — меньшее из 16384 МБ и аппаратного лимита с резервом 2048 МБ системе; нижняя граница аппаратного лимита 1024 МБ. Это ограничение настроек, а не доказательство пригодности маломощного устройства для игры. По умолчанию 2048 МБ либо доступный лимит. Профиль получает отдельный `instances/<id>`; ID не допускает traversal, существующая ссылка/junction каталога отклоняется.

Для текущего прототипа выбран уже проверенный dev-runtime **Java 21 x64**. Это временная политика до установочного манифеста игры. Выбор java.exe выполняется native picker; renderer не задаёт путь для запуска. Допускается абсолютный путь на локальном Windows-диске; UNC, включая вариант с прямыми слешами, и device paths отвергаются. Проверка делает только `-XshowSettings:properties -version` через execFile, shell=false, timeout 5 с, output limit 64 КиБ. JVM options из JAVA_TOOL_OPTIONS/_JAVA_OPTIONS/JDK_JAVA_OPTIONS/CLASSPATH не наследуются. В UI возвращаются разобранные major/bits и краткое сообщение; raw output не показывается. Проверки Java и native picker не запускаются параллельно сами с собой.

Интерфейс: обзор с оригинальной иллюстрацией Space, профили, настройки среды. Палитра Nebula сохранена от сайта. Кнопка запуска недоступна и объяснена реальной стадией проекта. Форма хранит черновик при переключении раздела; смена профиля до save/cancel блокируется, чтобы не потерять изменения. UI/UX Pro Max использован для рекомендаций по игровому стилю, contrast/focus/reduced-motion; точного совпадения базы по desktop launcher settings не найдено, структура приложения выбрана по требованиям SPEC.

## Запуск из исходников

Из корня репозитория:

```powershell
npm.cmd ci
npm.cmd run launcher
```

Electron при первом запуске загружает свой официальный runtime, если binary ещё отсутствует. Скрипт `launcher` сначала собирает исходники. Повторный запуск готовой сборки: `npm.cmd run start --workspace @space/launcher`.

Обычный каталог данных определяется Electron userData для имени Space Client. Для изолированного dev-теста:

```powershell
$env:SPACE_LAUNCHER_DATA = Join-Path (Get-Location) '.reports/launcher-desktop'
npm.cmd run launcher
```

Этот override действует только в unpackaged-приложении. Не использовать личную `.minecraft` для тестов. Не коммитить dist, Electron runtime, .reports и локальные профили.

## Проверено и что осталось

- Build main/preload/renderer и TypeScript проходят.
- **10 Node-тестов**: независимость двух профилей после перезапуска store, изоляция папок, валидация RAM/имён/полей/ID, резервирование исходного файла при восстановлении и превышении размера, последовательные записи и ошибка сохранения с cleanup, лимит профилей/junction, sender/frame/URL, allowlist ресурсов, разбор версий Java и локальные Windows paths.
- CI каркаса и последующих resolver/native-срезов прошёл для web/game и Windows launcher job. Визуальная проверка выполняется отдельно от CI.
- Настоящий `inspectJava` с локальным `.tools/jdk-21/bin/java.exe` вернул usable=true, major=21, bits=64.
- После явного разрешения пользователя осмотр возобновлён. Через окно проверены три раздела, независимые профили/RAM/Java, native picker, реальная Java 21 x64, сохранение и выбор после перезапуска, защита черновика активной карточки, Tab/focus/стрелка RAM/отмена. Подробности и ограничения — [LAUNCHER-QA.md](LAUNCHER-QA.md).
- Таймаут/переполнение вывода реального Java-процесса, внешний frame/навигация в живом окне, полный keyboard-only маршрут, zoom и reduced-motion ещё требуют runtime-проверки.

Установка: downloader/pins/cache/cancel/resume, очередь, resolver 1.8.9, Log4j 2.26.1, [loader/LWJGL manifest](LOADER-MANIFEST.md), natives и [собственный game bundle](GAME-BUNDLE.md). Через UI подготовлены 768 файлов/146,3 МиБ и 16 DLL; проверены отмена/продолжение/восстановление asset, полный аудит и reuse natives без новой копии DLL. Добавлено [хранилище будущей Microsoft-сессии](AUTH.md) с MSAL 7.1.1 и асинхронным Windows DPAPI; **67 launcher tests** проходят. Запуск остаётся заблокированным до авторизации/владения и runtime validation. Детали — [INSTALLER.md](INSTALLER.md).

SC-07 закрыт с записью базовой UI-приёмки; дополнительные QA-пути сохраняются в LAUNCHER-QA. Далее: регистрация Microsoft public client и права API (SC-06), OAuth/OS token storage (SC-08), Legacy Fabric/Space manifest (SC-09), авторизованный запуск Java (SC-10). OAuth-регистрация и действия с аккаунтом выполняются с участием владельца.

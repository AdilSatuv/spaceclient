# Space Launcher

- Прочитать docs/LAUNCHER.md перед изменением desktop-архитектуры.
- Границы слоёв описаны в docs/ARCHITECTURE.md. main/index собирает зависимости; main/application импортирует только application/shared; Electron API живёт в main/electron и точке сборки. Renderer-модель не зависит от React/DOM, только renderer/index читает window.space. Не обходить architecture.test.cjs для удобства.
- Контракт команд хранится в shared/ipc.ts; preload перечисляет конкретные функции. В main вход остаётся unknown и проверяется до обращения к привилегированному порту. Пользовательские ошибки помечать UserFacingError с безопасным текстом; raw error.message/cause/stack в IPC и installation DTO не передавать.
- Main владеет диском, диалогами и запуском процессов; renderer не получает Node, fs, shell, произвольные IPC или аргументы команд.
- Не ослаблять sandbox, contextIsolation, CSP, проверку sender/mainFrame, запрет навигации и окон ради удобства разработки.
- Renderer использует локальные shadcn/ui компоненты, Tailwind CLI и Lucide; см. docs/LAUNCHER-DESIGN.md. Не добавлять CDN, runtime CSS injection, inline style или hash navigation ради компонентов. После изменения интерфейса выполнить build, check:ui и осмотреть снимки; тест запускает отдельный userData, а не личный сеанс. Сохранять лицензии адаптированных компонентов.
- Renderer может передать ID профиля и разрешённые поля настроек. Путь Java приходит только из native picker в main. Исполнять только сохранённый java.exe с фиксированными аргументами проверки, без shell.
- Для настройки используется отдельный каталог данных лаунчера; профиль получает каталог instances/<validated-id>. Не использовать существующую .minecraft.
- Не добавлять настоящий launch до реализации авторизации, владения игрой, установки и проверки манифеста. Не сохранять токены в settings.json.
- Auth vault/callback пока не подключены к живому входу. Все PCA/cache операции проводить через один MsalCacheAccess.run; не передавать в renderer кеш, codes, tokens, claims, browser URL и raw SDK errors. Windows encryption — только async safeStorage без plaintext fallback. Свой public client ID и Xbox/Minecraft permissions ещё требуют участия владельца; web/confidential GDK flow не копировать в desktop с secret.
- Собственный JAR пакуется только из фиксированного game/build/libs с чистым game tree и совпадающим source stamp. CI Windows обязан собирать с SPACE_REQUIRE_GAME_BUNDLE=1. Не публиковать Minecraft binaries/assets вместе с собственным JAR; development bundle не является подписанным релизом или разрешением launch.
- Проверки: npm run test:launcher, npm run build:launcher, npm run check --workspace @space/launcher. Общая npm test включает launcher.
- Для локальной UI-проверки задавать SPACE_LAUNCHER_DATA в игнорируемую .reports/launcher-desktop. Указывать проверенное и непроверенное отдельно; успешная сборка не заменяет проверку окна.

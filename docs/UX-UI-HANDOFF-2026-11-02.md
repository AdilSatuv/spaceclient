# Space Client: UX/UI — 2 ноября 2026

Завершён локальный срез навигации и состояний лаунчера, страниц сайта и связанных меню Minecraft. Графитовые поверхности, золотой акцент, выбранный логотип и космические сцены сохранены. Главное игровое меню остаётся слева; кнопки имеют ограниченную ширину.

## Копия и коммиты

- Независимый clone без hardlinks: `C:/Users/user/Documents/Codex/2026-11-02/space-client-ux-ui/work/spaceclient-ux`.
- База: `0af5658f2dc0d533e8e1a60c9587248fb2b49c26` из исходного `space-client-minecraft-menus/work/spaceclient-menus`.
- Ветка: `ux/navigation-surfaces`. Origin удалён только из новой копии. Worktree не создавался, исходная ветка не переключалась.
- `711efe035f725e58672f0cb4c2de342ea7dbae46` — единое название «Играть», этапы подготовки, объяснения блокировок, подписи узкой навигации, страницы сайта и фокус Minecraft.
- `31b55323f2874bed4eb2ddf718e7ab12c56a6960` — подгонка длинных переводов штатных кнопок и проверка полного Tab-цикла мобильного меню сайта.
- Документ и безопасный QA JSON оформляются отдельным завершающим коммитом документации. Для интеграции нужны оба перечисленных коммита реализации; объединение и установка остаются у исходного чата.

## Изменённые файлы

Лаунчер:

- `apps/launcher/src/renderer/app.tsx`
- `apps/launcher/src/renderer/pages/home.tsx`
- `apps/launcher/src/renderer/installation-card.tsx`
- `apps/launcher/src/renderer/style.css`
- `apps/launcher/scripts/check-launcher-ui.mjs`

Сайт:

- `apps/website/src/shared/ui/page-intro.tsx`
- `apps/website/src/_app/ui/header.tsx`
- `apps/website/src/_app/styles/globals.css`
- `apps/website/src/_pages/features/ui/page.tsx`
- `apps/website/src/_pages/help/ui/page.tsx`
- `apps/website/src/_pages/updates/ui/page.tsx`
- `apps/website/src/_pages/download/ui/page.tsx`
- `scripts/check-website-accessibility.cjs`

Minecraft и документация:

- `game/src/main/java/dev/spaceclient/menu/MenuLayout.java`
- `game/src/main/java/dev/spaceclient/menu/MenuTheme.java`
- `game/src/test/java/dev/spaceclient/menu/MenuTest.java`
- `docs/UX-UI-HANDOFF-2026-11-02.md`
- `docs/qa/ux-navigation-2026-11-02.json`

## Результат

Лаунчер показывает сохранённый ник, состояние файлов и состояние запуска в компактной полосе. Ошибка и загрузка файлов отличаются от «не проверены»; причины блокировки подготовки находятся рядом с действием и связаны через `aria-describedby`. Название навигации совпадает с заголовком. На узких окнах видны текстовые подписи разделов. Статус «Процесс работает» не означает вход в мир. Подготовка, отмена, сохранение и запуск используют прежние реальные команды.

На сайте добавлены текущий раздел возле ссылки «Главная», сгруппированные графитовые фильтры, явный запрос в результатах и заметные действия сброса пустых состояний. Мобильное меню содержит «Главная». Поиск, query URL, история и отличия empty/error/withdrawn сохранены. Браузерный тест теперь проверяет полный фактический набор элементов меню вместо прежнего фиксированного числа.

В Minecraft минимальная ширина левой колонки от scaled 320 составляет 180, максимальная — 260. Компактные строки разделены четырьмя пикселями; клавиатурный фокус имеет отдельную светлую рамку. Длинные переводы обычных кнопок сначала уменьшаются до 75%, затем при необходимости получают многоточие. На фактическом русском снимке полностью читаются «Переименовать» и «Пересоздать». Штатные обработчики, hitbox, списки миров/серверов и специализированные слайдеры сохранены.

## Проверки

- `npm.cmd test`: 39 общих, 24 API и 112 launcher tests — pass.
- `npm.cmd run check`: TypeScript всех workspace и проверка токенов — pass.
- `npm.cmd run build --workspace @space/website`: production Next.js 16.4.2 — pass.
- `npm.cmd run check:web:states`: восемь сценариев production-состояний — pass.
- `npm.cmd run check:web:browser`: Chrome 156, 42 проверки компоновки, 27 сценариев; ошибки страницы и skipped пусты. Включены мобильная клавиатура/возврат фокуса, reduced-motion, forced-colors, zoom, история поиска, clipboard fallback и отказ API.
- `npm.cmd run check:ui --workspace @space/launcher`: три экрана × четыре размера (1180×760, 1000×680, 600×680, 500×380), persistence, dirty guards, ошибки ника/Java, accessibility tree, forced-colors и reduced-motion — pass. Подготовка и запуск видны на обоих desktop-размерах; горизонтальной прокрутки нет.
- `scripts/game.ps1 test build --offline --no-daemon`: 167 Java tests и remapped JAR — pass. Все Java-команды использовали собственные JDK 21 и Gradle cache через этот скрипт.
- `npm.cmd run build:launcher`: финальный source-stamped game bundle — pass.
- `npm.cmd run format:check`, `git diff --check`, просмотр staged diff — pass. CRLF новой Windows-копии нормализованы только как рабочие байты; посторонних изменений в коммитах нет.

Финальный game tree: `1154553da1bab37e904ee227733a20d9981627f7`. JAR SHA-256: `b7c8e996e0baf518100102f8567ee904d6acab90faf777cc382f4dfc6090e9df`. Renderer tree: `e0527e4c7337335230b611622cc03cab518ff0d6`; после прошедшего Electron QA он не менялся.

## Фактический Minecraft QA

Одноразовый addon из игнорируемой `.reports/ux-menu-qa` создал невидимый AWT Canvas и запретил захват desktop input. Два отдельных QA-инстанса использовали копии только подготовленных бинарных файлов и собственные новые options; пользовательские миры и настройки не копировались. Получены 14 настоящих framebuffer PNG 1280×720: по семь для RU и EN. Оба процесса штатно завершились, exitCode=0.

Методами настоящих экранов проверены настройки → назад, выбор мира → назад, серверы → назад и Tab на главном меню. Мир не создавался и не открывался; к серверу не подключались. На финальных запусках не наблюдались ошибки Mixin/загрузки фоновой текстуры. Native списки сохраняют vanilla dirt-фон — это видно на снимках.

Записаны именно фактические scaled размеры: RU при options 2 и 3 даёт 640×360; EN при 2 — 640×360, при 3 — 427×240. Поэтому RU option 3 не засчитывается как отдельная компактная геометрия. Компактная версия подтверждена EN 427×240.

Ограничения: физическая клавиатура/мышь, fullscreen/resize, все GUI scale и языки, demo/old-GL/Realms, resource packs, меню паузы внутри мира, native Java picker и screen reader не приняты этим срезом. FPS не измерялся. QA-addon отсутствует в обычном JAR. Firefox в этой итерации не запускался.

## Codebase Memory

Список проектов проверен до индексации. Старый `spaceclient` относится к другой папке и не переназначался. Создан отдельный `spaceclient-ux-ui-20261102`, root точно соответствует новой копии; последняя явная full-индексация — `2026-11-02T16:07:50Z`. Выполнены `search_graph`, `trace_path` (HomePage → карты/состояния, MenuLayout → TitleMenuMixin), чтение snippet и exact-path coverage всех 16 изменённых файлов реализации/проверок.

Ограничения сервера сохранены явно: `is_git=false`, branch/SHA отсутствуют в его метаданных, а freshness даже после reindex возвращает `metadata_changed`. Ветка и SHA отдельно сверены Git CLI; результаты графа сверены с диском. У launcher CSS partial parse на Tailwind at-rules (строки 1–6), поэтому CSS прочитан напрямую. При финальном reindex timeout отмечен у чужого для этого среза legacy fixture `profile-v7-57e1593.properties`; Java-тесты самого fixture проходят. Отсутствие записанной проблемы не трактуется как доказательство полного покрытия.

## Preview и границы интеграции

Собственный localhost preview оставлен на `http://127.0.0.1:55693`, API — `http://127.0.0.1:55692`. Процессы принадлежат только новой копии; сведения — `.reports/local-preview.json`. Отдельно сняты три реальных пустых состояния preview, сброс поиска возвращает каталог из шести текущих записей.

Пользовательские deliverables: `C:/Users/user/Documents/Codex/2026-11-02/space-client-ux-ui/outputs`. Там handoff, безопасные JSON-отчёты и фактические снимки. Полные disposable QA данные остаются в `.reports` новой копии.

Общий `CHAT-HANDOFF.md`, HUD/config/profile/gameplay-логика, backend, launch/download core и IPC-контракты не менялись. Исходный checkout использовался только для чтения. Пользовательская игра не останавливалась; финальный JAR не устанавливался в общий `.reports/launcher-desktop`. Push, публичной публикации, переименования и регистрации не было.

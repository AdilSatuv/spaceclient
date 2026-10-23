# Сайт и API

По выбору пользователя: Next.js + NestJS, оба на TypeScript. Один GitHub-репозиторий и npm workspaces.

Архитектура после разделения от 24 октября — [WEB-ARCHITECTURE.md](WEB-ARCHITECTURE.md): общий @space/content-contracts, прикладной сервис и repository API, server-only клиент Next.js и FSD-структура с автоматическими проверками слоёв, изоляции слайсов и публичных точек входа. Сборка общего пакета включена в prebuild/predev/precheck обоих приложений.

Спецификация — [WEBSITE-SPEC.md](WEBSITE-SPEC.md), дизайн — [FIGMA-DESIGN.md](FIGMA-DESIGN.md), фактические проверки — [WEBSITE-QA.md](WEBSITE-QA.md). Маршруты и контентный API уже реализованы. Очередь приёмки: SC-16…SC-24; сайт поддерживает отсутствие alpha-сборки.

```powershell
npm.cmd ci
npm.cmd run api
# Второй терминал:
npm.cmd run site
```

Next.js: http://127.0.0.1:3000, NestJS: http://127.0.0.1:3001/v1/health.

Для локального production-preview одной командой:

```powershell
npm.cmd run preview:web
```

Команда собирает общий контракт, API и сайт, затем запускает оба процесса только на loopback со свободными портами. URL выводится в терминал и сохраняется в игнорируемом `.reports/local-preview.json`. Ctrl+C останавливает только процессы этого preview. Каталог релизов настоящий и пустой, сайт не публикуется; preview собирается с выключенной индексацией. Docker, домен и аккаунт хостинга не требуются. Повторный запуск уже готовой сборки: `node scripts/preview-web.cjs`.

```powershell
npm.cmd test
npm.cmd run build
npm.cmd run check
# Production после build, в отдельных терминалах:
npm.cmd run start --workspace @space/api
npm.cmd run start --workspace @space/website
```

Версии закреплены в package.json/lockfile. Нужен Node.js 24+. `apps/website` использует App Router и Server Components. Главная и инструкции помощи доступны без API; возможности, загрузки и журнал получают серверный контент. Переменная `SPACE_API_URL` задаёт адрес NestJS для Next-сервера; по умолчанию localhost:3001. Токены и браузерный CORS здесь не нужны: запросы идут между серверами.

API v1:

- GET `/v1/health`: готовность процесса API.
- GET `/v1/project`: название, стадия разработки, целевые Minecraft/платформа.
- GET `/v1/releases`: `{items, total}`; пустой список до первого проверенного пользовательского релиза.
- GET `/v1/features`: `{items}` со статусами возможностей.
- GET `/v1/updates`: фильтр type, page и фиксированная страница из 12 записей.
- GET `/v1/updates/:slug`: запись или безопасная ошибка 400/404.

Контракты и проверки нового API описаны в [API-CONTENT.md](API-CONTENT.md). Веб-интерфейс пересобирается по [Figma-макету](FIGMA-DESIGN.md); предыдущий визуальный вариант отклонён пользователем.

Порт API можно переопределить через `PORT`, bind — через `HOST` (127.0.0.1 по умолчанию, 0.0.0.0 для приватной сети хостинга). Для Next использовать CLI-параметр порта, на хостинге — `start:hosted`. Локальные команды по умолчанию слушают только loopback. Публичному размещению нужны TLS/reverse proxy и server-to-server адрес API. База, аккаунты, CMS и платежи пока не нужны и не имитируются.

Сайт содержит главную, каталог возможностей, загрузки, журнал со статьями и помощь. Публичных артефактов пока нет; production-каталог пуст, а fixture-релизы существуют только в тестовом процессе. Размещению Next + Nest нужны два Node.js-процесса. Текущий этап — локальная production-проверка и GitHub; порядок публикации описан в [WEB-DEPLOY.md](WEB-DEPLOY.md).

## UI и данные сайта

Новый интерфейс основан на `docs/FIGMA-DESIGN.md`. Возможности, каталог загрузок и журнал читаются серверными компонентами Next.js из NestJS через `src/shared/api/index.server.ts`. `SPACE_API_URL` — HTTP/HTTPS-origin без path/query/credentials, только серверная переменная. Таймаут — 2,5 секунды, включая чтение тела, JSON ограничен 1 МиБ, redirect API не разрешается, автоматических повторов нет. Невалидный JSON/контракт и отказ сервиса не превращаются в пустой каталог. Содержимое инструкций помощи хранится в `apps/website/src/entities/help-article/model/articles.ts`.

- `npm.cmd run format` — форматировать веб-код закреплённым Prettier 3.9.10.
- `npm.cmd run format:check` — проверить формат без записи.
- `npm.cmd test` — проверки API, контентных контрактов и выбора рекомендуемой сборки.
- `npm.cmd run check:web:browser` — отдельный Chrome с временным профилем и fixture API: responsive, настоящий 200% browser zoom, dynamic reduced-motion, меню, clipboard success/denial. Нужны build API/Next и установленный Chrome; путь можно задать через SPACE_CHROME_PATH. Fixtures не публикуются, download запрещён. Предыдущее содержимое clipboard не записывается в отчёт и восстанавливается, если за время проверки его не изменил пользователь.
- `npm.cmd run build` → `npm.cmd run check:web:states` — HTTP-проверки production Next.js с изолированным fixture API на временных loopback-портах. Fixture-релизы не записываются в каталог и не скачиваются.
- `node scripts/check-website.cjs` — маршруты/фильтры/404 работающего сайта; origin можно изменить через `SPACE_SITE_URL`.

State-проверки покрывают empty/error, unsafe URL, stable preference, prerelease, withdrawn, hash controls, пагинацию и настоящий HTTP 404 неизвестной записи. Проверка clipboard требует браузера отдельно: наличие кнопки в HTML не доказывает успешную запись в буфер.

Loading boundaries находятся в конкретных разделах; список обновлений вынесен в route group `(index)`. У detail обновления нет предшествующего streaming loading: неизвестная запись возвращает HTTP 404, а не уже отправленный 200 с not-found интерфейсом.

## Metadata и индексация

`apps/website/.env.example` содержит серверные переменные. Без реального `SPACE_SITE_URL` canonical и Open Graph URL не выдумываются. Индексация включается только при одновременных production-сборке, HTTPS-origin без path/query/credentials и `SPACE_SITE_INDEXABLE=true`. По умолчанию robots запрещает обход, meta robots — noindex, sitemap пуст. Перед публичной публикацией rebuild выполняется с окончательными переменными: статические страницы получают metadata во время build.

У разделов и инструкций уникальные title/description; canonical у фильтров ведёт на основную страницу раздела. Ошибки получения данных получают noindex. `robots.txt`, `sitemap.xml` и исходный SVG favicon генерируются Next.js. Sitemap включает существующие help-статьи и до 1008 свежих заметок API; при росте журнала за этот предел потребуется разбиение sitemap. Если API недоступен, sitemap содержит только известные статические страницы.

React cache используется только для согласованного чтения одного endpoint между metadata и страницей в рамках серверного запроса. Долгосрочного кэша каталога и автоматических сетевых повторов нет.

Для ручной проверки download UI после build: `node scripts/check-website-states.cjs --preview=stable`. Команда выводит два временных localhost URL: обычный тестовый каталог и proxy с запретом clipboard-write. Оба используют только fixture-данные; Ctrl+C останавливает оба процесса. Режим не включается переменной окружения production приложения. Внешние download-ссылки fixtures не открывать: таких артефактов нет.

## Проверка пользовательских сценариев

`npm.cmd run check:web:browser` дополнительно запускает `scripts/check-website-flows.cjs`: поиск/фильтры/reload/Back возможностей, каналы загрузок, 14 тестовых новостей с пагинацией 12+2 и возвратом из статьи, поиск FAQ и инструкций, клавиатуру меню/FAQ и настоящий refresh после HTTP 503, invalid JSON и таймаута. Восстановление проверяется в трёх разделах с API; DOM-маркер подтверждает, что retry не перезагружает документ. Прогон использует собственные Next/fixture процессы, не меняет работающий preview и не скачивает установщики.

Для отладки только этого набора: `node scripts/check-website-browser.cjs --flows-only`. Этот режим не проверяет 42 responsive layouts, clipboard и zoom — полный приёмочный запуск остаётся командой без флага. Отчёты, ошибки и viewport-снимки сохраняются в `.reports/website-browser/run-*`; файлы профиля браузера не коммитятся. Проверка ошибки localStorage использует внедрённый SecurityError чтения/записи; это не проверка настроек браузера или ОС.

Сопоставление с ТЗ и оставшиеся условия публичного релиза — [WEB-ACCEPTANCE.md](WEB-ACCEPTANCE.md).

## Токены и состояния управления

Базовые значения хранятся в `docs/design/space-dark.tokens.json`, состояния — в `docs/design/control-states.tokens.json`, навигация — в `docs/design/navigation.tokens.json`. После изменения JSON выполнить `npm.cmd run tokens:web`, затем `npm.cmd run tokens:web:check`. Генерируемый `src/_app/styles/tokens.css` вручную не редактируется. Проверка ловит дрейф CSS, несогласованные RGB/HEX/alpha и проверяет 20 цветовых пар; включена в `npm run check` и оба browser CI jobs. Статус новых Figma-компонентов навигации — в [NAVIGATION.md](design/NAVIGATION.md).

Native components и витрина находятся в [Figma](https://www.figma.com/design/rlT7ixlB1AVZVvp9BfRLZJ/Space-Client-Website-Design?node-id=23-61), правила — в [CONTROL-STATES.md](design/CONTROL-STATES.md). Это versioned handoff, автоматического применения изменений из Figma в JSON нет. При обновлении палитры сверять обе стороны. Chrome и Firefox проверяют настоящие hover/press/focus и busy без hover-эффектов.

## Firefox: второй движок

Для отдельной проверки Gecko:

```powershell
npm.cmd run setup:web:firefox
npm.cmd run check:web:firefox
```

Требуется уже собранный API и Next, как для Chrome-набора. Setup загружает **Firefox 158.0.1** из официального архива Mozilla в игнорируемый `.tools/browsers`, проверяет закреплённый SHA-256 до распаковки и не устанавливает браузер в систему. Поддержаны Windows x64 и Linux x64; для Linux нужен xz. Контрольные суммы взяты из [SHA256SUMS этой версии](https://archive.mozilla.org/pub/firefox/releases/158.0.1/SHA256SUMS). Для другой версии менять pin и checksum явно; `latest` не используется. `SPACE_FIREFOX_PATH` позволяет явно выбрать другой бинарник для отдельной проверки.

Набор использует [Puppeteer с Firefox](https://pptr.dev/guides/configuration) через WebDriver BiDi, собственные временные профили и тот же изолированный fixture API, что Chrome. Проверяются семь маршрутов на шести ширинах и все 20 сценариев взаимодействия. Профили обычного и reduced-motion режимов запускаются последовательно. Reduced-motion задаётся Firefox preference `ui.prefersReducedMotion=1` только в тестовом профиле; настоящий matchMedia, остановка CSS-декорации и disabled-кнопка проверяются отдельно.

Настоящий browser zoom 200%, clipboard success/denial и live media emulation остаются в Chrome-наборе; их прохождение в Firefox не заявляется. Запросы к fixture download URL блокируются перехватом, ссылки не нажимаются. JSON/PNG находятся в `.reports/website-firefox/run-*`; профиль браузера и сам бинарник не коммитятся и не попадают в CI artifacts. Команда `node scripts/check-website-browser.cjs --firefox --flows-only` пропускает responsive-матрицу и предназначена только для отладки.

В Verify добавлен независимый `web-firefox` job: чистая установка зависимостей, сборка API/Next, setup закреплённой версии и браузерная проверка с сохранением JSON/PNG. Puppeteer Core 25.16.0 и browser installer 3.2.6 закреплены как прямые devDependencies корня; веб-проверки больше не зависят от случайного hoisting зависимостей лаунчера.

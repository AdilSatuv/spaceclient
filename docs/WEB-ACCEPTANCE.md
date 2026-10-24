# Состояние приёмки вебсайта

Срез: 24 октября 2026. Текущая цель пользователя — локальный preview. Матрица относится к ТЗ [WEBSITE-SPEC.md](WEBSITE-SPEC.md), а не объявляет готовность игрового клиента или публичного сайта.

Последний локальный срез SC-23 — [визуальная проверка и интервалы текста](WEB-ACCESSIBILITY.md): 26 полных снимков 13 маршрутов, исправление overflow шапки/подвала, 24 text-spacing случая и два short-menu сценария в каждом движке. Текущая регрессия: Chrome 42 layouts / 27 flows, Firefox 42 / 26. Тексты согласованы с принятой игровой проверкой. **Ручной screen reader не принят** из-за ограничений управления Narrator; Safari и production HTTPS/SEO остаются открытыми. Более ранние Lighthouse-числа ниже относятся к прежним срезам.

| ID | Результат локальной проверки | Свидетельство |
|---|---|---|
| WEB-01 | Главные маршруты и статьи доступны; unknown route/help/update возвращают HTTP 404 | check-website.cjs, check-website-states.cjs, browser flows |
| WEB-02 | Стадия разработки и отсутствие установщика явно показаны, ложной кнопки загрузки нет | Production route smoke, empty fixture |
| WEB-03 | Поиск, категория, статус, details с Enter, reload, empty/reset и Back согласованы с URL | Browser flows на 390/1440 px |
| WEB-04 | Empty/error/stable/prerelease/withdrawn и unsafe artifact проверены; смена канала и Back работают | HTTP state fixtures; browser channel flow на 390/1440 px |
| WEB-05 | Настоящий clipboard write/read и отказ с доступным для выделения хешем | Browser check: собственный профиль и Permissions-Policy proxy |
| WEB-06 | Пагинация 12+2, тип записи, статья и Back; смена фильтра начинает первую страницу | Browser fixture из 14 новостей на 390/1440 px; настоящие HTTP 404 |
| WEB-07 | Поиск находит инструкции и FAQ без учёта регистра; пустой результат содержит reset/issue; FAQ и ссылка работают с клавиатуры | Browser flows на 390/1440 px, viewport-снимки |
| WEB-08 | Схемы, пагинация, invalid query, unknown slug и изоляция каталогов | 9 API unit/HTTP tests; общие contract/transport tests |
| WEB-09 | HTTP 503, invalid JSON и timeout отличаются от empty; retry показывает busy, блокирует кнопку и восстанавливает данные | 9 browser случаев: features/download/updates × 3 сбоя; документ не перезагружается |
| WEB-10 | 42 responsive layouts, настоящий zoom 200%, Escape/focus trap, skip link, live reduced-motion | Полный Chrome browser run; localStorage fallback отдельно проверен внедрённым SecurityError |
| WEB-11 | Локальные Lighthouse-замеры записаны; public HTTPS/SEO ещё не проверены | Новый /help: три mobile-прогона; остальные страницы — предыдущий срез MVP |
| WEB-12 | Build, TypeScript, unit/HTTP/browser checks, local preview и runbook есть; публичное размещение/rollback не выполнялись | Verify workflow, WEB.md и WEB-DEPLOY.md |

Текущий [browser-отчёт](qa/website-user-flows-2026-10-24.json) содержит 20 сценариев взаимодействия, 42 проверки размеров и отдельные проверки zoom/clipboard/motion. Фиктивные релизы и новости существуют только в тестовом API; production-каталог релизов остаётся пустым. Никакие download-ссылки теста не открываются.

При проверке найдены и исправлены две ошибки: FAQ не участвовал в поиске, а история браузера могла восстанавливать несохранённый текст/выбор поверх результатов предыдущего URL. Поиск остаётся серверным; общая FilterForm восстанавливает committed defaults после history restoration.

[Новые замеры помощи](qa/website-help-search-lighthouse-2026-10-24.json) относятся к окончательному коду этого среза. [Предыдущие замеры пяти страниц](qa/website-mvp-lighthouse-2026-10-24.json) сохранены как отдельный baseline. Preview остаётся noindex; снижать ограничения индексации ради оценки SEO не требуется.

Дополнительный [Firefox-отчёт](qa/website-firefox-2026-10-24.json): Gecko 158.0.1, 42 responsive layouts, все 20 сценариев взаимодействия и reduced-motion в отдельном профиле. Проверки истории, фильтров, FAQ, клавиатуры, API recovery и 404 используют общий с Chrome набор. Zoom 200%, clipboard и динамическая эмуляция media feature в Firefox не проверялись. Повторный Chrome-отчёт после адаптации общего набора: [регрессия](qa/website-chrome-regression-2026-10-24.json).

SC-23 остаётся открытой: её полные критерии включают production SEO/performance. SC-17 требует законченной визуальной библиотеки Figma; этот срез её не заменяет. Safari и ручная assistive-technology проверка ещё не выполнены. SC-24 с публичным hosting/HTTPS отложена по выбору пользователя; локальный запуск от неё не зависит.

Срез Controls: [Chrome](qa/website-controls-chrome-2026-10-24.json) и [Firefox](qa/website-controls-firefox-2026-10-24.json) прошли по 42 responsive layouts и 21 сценарию. Новый WEB-CONTROLS проверяет реальные hover/press, сохранение layout и клавиатурный focus. WEB-03/07 дополнены сбросом непустой выдачи; WEB-09 проверяет disabled/busy под указателем без transform/glow. В [Figma](https://www.figma.com/design/rlT7ixlB1AVZVvp9BfRLZJ/Space-Client-Website-Design?node-id=23-61) добавлены две native component families; [QA привязок и значений](qa/website-controls-figma-2026-10-24.json). Полная визуальная приёмка всех экранов остаётся открытой.

[Актуальные локальные Lighthouse-замеры](qa/website-controls-lighthouse-2026-10-24.json) после Controls: 15 прогонов пяти страниц, медианы Performance 98–99, Accessibility 100, CLS 0. Они заменяют прежние локальные показатели для этого кода; публичный HTTPS/SEO по-прежнему отложен.

Текущая визуальная итерация — **Eclipse / Ringworld**: тёплые кнопки и текст, чёрный звёздный фон и астероиды. [Chrome](qa/website-eclipse-chrome-2026-10-24.json) / [Firefox](qa/website-eclipse-firefox-2026-10-24.json): по 42 layouts и 22 сценария. WEB-ECLIPSE добавляет проверку остановки анимаций вне экрана, при паузе и настоящем скрытии вкладки. Пользователь выбрал E — Ringworld; выбранный SVG применён к бренду, две витрины остаются материалом сравнения. [Тема](design/ECLIPSE.md), [новый Lighthouse главной](qa/website-eclipse-lighthouse-2026-10-24.json): три прогона, медиана Performance 99 / Accessibility 100 / LCP 2204 мс / CLS 0. Другие маршруты после финальной палитры повторно не измерялись. Figma не синхронизирована из-за лимита MCP; полные SC-17/23 не закрываются.

Следующий срез: объёмный Ringworld и [первый HTML без зависимости от React-переноса](WEB-RESILIENCE.md). [Chrome](qa/website-resilience-chrome-2026-10-24.json) — 42 layouts / 25 сценариев, [Firefox](qa/website-resilience-firefox-2026-10-24.json) — 42 / 24. WEB-NO-JS подтверждает базовую навигацию, поиск, disclosure, empty и 404 при полностью отключённых скриптах; WEB-PENDING — feedback обычного client navigation без layout shift; Chrome отдельно проверяет forced-colors dark/light. Критерии общего каркаса SC-18 подтверждены локально. Это не закрывает оставшиеся production-условия SC-23 и полную Figma-библиотеку SC-17.

[Текущий Lighthouse пяти страниц](qa/website-resilience-lighthouse-2026-10-24.json): по три последовательных прогона окончательного production build; медианы Performance 99 и Accessibility 100 везде, CLS 0, LCP 2048–2196 мс. Этот срез заменяет прежние локальные замеры для текущего кода. Production SEO/HTTPS остаются отложенными; preview сохраняет noindex.

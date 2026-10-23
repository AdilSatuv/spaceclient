# Состояние приёмки вебсайта

Срез: 24 октября 2026. Текущая цель пользователя — локальный preview. Матрица относится к ТЗ [WEBSITE-SPEC.md](WEBSITE-SPEC.md), а не объявляет готовность игрового клиента или публичного сайта.

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

SC-23 остаётся открытой: её полные критерии включают production SEO/performance. SC-17 требует законченной визуальной библиотеки Figma; этот срез её не заменяет. Firefox/Safari и ручная assistive-technology проверка ещё не выполнены. SC-24 с публичным hosting/HTTPS отложена по выбору пользователя; локальный запуск от неё не зависит.

# Приёмка сайта — рабочий журнал

Дата: 23.10.2026. Срез: новый интерфейс Next.js + NestJS. Локальная production-сборка, Chromium in-app browser. Это журнал фактических проверок; незаполненные пункты не считаются пройденными.

| Проверка | Результат и границы |
|---|---|
| Сборка и TypeScript | `npm run build`, `npm run check --workspaces` проходят |
| Тесты | 10 unit/HTTP-тестов (5 workspace + 5 API); отдельные production state checks проходят |
| Основные маршруты | Главная, возможности, загрузки, журнал, помощь и три инструкции — 200; неизвестные страницы/статьи — 404 |
| Источники контента | Features/releases/updates берутся из NestJS; help — локальный контент. Публичных артефактов нет |
| Download states | Изолированный fixture API: empty, failure, invalid URL, stable, prerelease, withdrawn; автоскачивания нет |
| Pagination | 14 тестовых записей, страница 2 содержит записи 12/13; тип сохраняется в ссылке назад |
| Features browser | HUD + prototype → 3; неизвестный запрос → 0; reset → 6; Back восстанавливает query и выбранные поля |
| Найденный дефект | После reset поля select могли оставаться старыми; формы получили key по query, повторная проверка пройдена |
| Мобильное меню | Открытие, Escape, возврат фокуса на trigger; переход к загрузкам закрывает dialog и восстанавливает body scroll |
| Responsive | Пять основных страниц × viewport 320/375/390/768/1024/1440: scrollWidth равен clientWidth, горизонтального переполнения нет. Скроллбар занимает 15 px |
| Визуальная проверка | Главная desktop/mobile, мобильные загрузки. Снимки сохранены в outputs чата |
| SEO preview | Уникальные metadata; без public origin отсутствует выдуманный canonical, noindex/robots disallow; поведение origin/index flags покрыто тестом |
| CI | Коммиты ec3e47f и 947f1c1 проверены GitHub Actions: success |

## Ещё проверить

- Полный keyboard-проход всех страниц и focus trap/видимость фокуса на всех размерах; уже проверенные Escape/return-focus не заменяют весь проход.
- Browser clipboard: успех и отказ записи хеша, ручное выделение.
- Help search/FAQ и updates filters в браузере (HTTP-проверки не заменяют взаимодействие).
- 200% zoom, reflow и reduced-motion в браузере; media rule в CSS сама по себе не подтверждает приёмку.
- Performance/accessibility/SEO lab по условиям WEBSITE-SPEC, включая три измерения production-сборки. Оценки Lighthouse пока не заявляются.
- Финальная библиотека tokens/components/control states в Figma; синхронизация мобильного фона. Созданные шесть макетов не означают завершение SC-17.
- Публичный hosting/origin/HTTPS, rebuild с production env, sitemap и rollback после размещения. Домен и хостинг ещё не выбраны.

SC-17, SC-18, SC-21, SC-22 и SC-23 не закрываются только на основании сборки.

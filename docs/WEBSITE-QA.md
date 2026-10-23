# Приёмка сайта — рабочий журнал

Дата: 23.10.2026. Срез: новый интерфейс Next.js + NestJS. Локальная production-сборка, Chromium in-app browser. Это журнал фактических проверок; незаполненные пункты не считаются пройденными.

| Проверка | Результат и границы |
|---|---|
| Сборка и TypeScript | `npm run build`, `npm run check --workspaces` проходят |
| Тесты | 11 unit/HTTP-тестов (5 workspace + 6 API); отдельные production state checks проходят |
| Основные маршруты | Главная, возможности, загрузки, журнал, помощь и три инструкции — 200; неизвестные страницы/статьи — 404 |
| Источники контента | Features/releases/updates берутся из NestJS; help — локальный контент. Публичных артефактов нет |
| Download states | Изолированный fixture API: empty, failure, invalid URL, stable, prerelease, withdrawn; автоскачивания нет |
| Pagination | 14 тестовых записей, страница 2 содержит записи 12/13; тип сохраняется в ссылке назад |
| Features browser | HUD + prototype → 3; неизвестный запрос → 0; reset → 6; Back восстанавливает query и выбранные поля |
| Найденный дефект | После reset поля select могли оставаться старыми; формы получили key по query, повторная проверка пройдена |
| Мобильное меню | Открытие, Escape, возврат фокуса на trigger; переход к загрузкам закрывает dialog и восстанавливает body scroll |
| Skip и touch targets | Skip-ссылка переводит фокус на MAIN#main после добавления tabIndex=-1. Brand, breadcrumb, summary и изолированные кнопки получили минимум 44 px |
| Clipboard browser | В разрешённом контексте Promise завершён и показано «Скопировано»; при Permissions-Policy clipboard-write=() — понятная ошибка. Read-only поле позволяет выделить все 64 символа. Системное содержимое буфера не подтверждено: clipboard read инструмента возвращал пустую строку |
| Help/updates browser | Поиск «совместимость» оставляет одну инструкцию; FAQ открывается Enter. Фильтр release в журнале сохраняется в URL и показывает отсутствие релизов |
| Responsive | Пять основных страниц × viewport 320/375/390/768/1024/1440: scrollWidth равен clientWidth, горизонтального переполнения нет. Скроллбар занимает 15 px |
| Визуальная проверка | Главная desktop/mobile, мобильные загрузки. Снимки сохранены в outputs чата |
| Mobile artwork | Отдельная вертикальная панорама выбрана picture source при <=760 px; широкая desktop-сцена сохранена. Текст читается на спокойном участке неба |
| SEO preview | Уникальные metadata; без public origin отсутствует выдуманный canonical, noindex/robots disallow; поведение origin/index flags покрыто тестом |
| CI | Коммиты ec3e47f и 947f1c1 проверены GitHub Actions: success |

## Ещё проверить

- Полный keyboard-проход всех страниц и focus trap/видимость фокуса на всех размерах; уже проверенные Escape/return-focus не заменяют весь проход.
- Сверить фактическое содержимое системного clipboard в обычном пользовательском браузере; уже проверены UI feedback и ручное выделение.
- 200% zoom, reflow и reduced-motion в браузере; media rule в CSS сама по себе не подтверждает приёмку.
- Performance/accessibility/SEO lab по условиям WEBSITE-SPEC, включая три измерения production-сборки. Оценки Lighthouse пока не заявляются.
- Финальная библиотека tokens/components/control states в Figma; синхронизация мобильного фона. Созданные шесть макетов не означают завершение SC-17.
- Публичный hosting/origin/HTTPS, rebuild с production env, sitemap и rollback после размещения. Домен и хостинг ещё не выбраны.

SC-19, SC-21 и SC-22 закрыты после проверки собственных критериев и записи результатов в Issues. SC-17/18/23 остаются открытыми для завершения визуальной системы и общей приёмки; SC-24 — для публичного размещения.

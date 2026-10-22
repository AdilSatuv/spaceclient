# Space Client: UI-проект v1

Основание: WEBSITE-SPEC.md. Выбранное направление — спокойный sci-fi, тёмный графит и мята. Рекомендации ui-ux-pro-max для gaming/HUD использованы для иерархии, видимого focus и reduced motion; интенсивные glow/анимации не приняты из-за читаемости и веса страницы.

## Система

- Background #090e13, surface #111a21, raised #18252c, border #384a53.
- Text #eff5f0, secondary #aebcb9, accent #c1f6cb, accent text #102219. Контраст обычного secondary по background около 9.8:1, основного около 17:1, CTA около 14:1; точные значения проверяются скриптом.
- Шрифт: Segoe UI/Arial/system sans, полная кириллица без внешнего запроса. Моноширинный Consolas только для меток/координат.
- Заголовок главной 52–88 px, H2 32–48 px, основной текст 16–18 px, line-height 1.6. Контент max-width 1200 px, gutters 20/32/48 px.
- Отступы 8/12/16/24/32/48/64/96. Радиусы 8 px controls, 16 px панели. Только один сильный accent CTA в группе.
- Touch controls >=44 px, focus outline 2 px accent с offset 4 px. Hover меняет фон/границу без сдвига layout. Disabled имеет текстовое пояснение.
- SVG-иллюстрация мира и HUD — подписанный концепт, не скриншот игры. Никаких фиктивных FPS-бенчмарков.
- Desktop header: logo / 4 раздела / GitHub. Mobile: logo / menu; нативный modal dialog, Esc, focus return, lock scroll. Текущая ссылка aria-current.
- Motion: только transition цвета 160 ms; без autoplay/parallax. Reduced motion отключает smooth scroll и transitions.

## Wireframes и шаблоны

| Экран | Desktop сверху вниз | Mobile |
|---|---|---|
| Главная | Header → hero 50/50: текст+CTA / мир-концепт → compatibility strip → три направления → широкая HUD-панель → 3 шага → свежая заметка → FAQ → footer | Текст, CTA, компактный концепт; все блоки в одну колонку |
| Features | Eyebrow+H1 → search/category/status → count → 2-column cards с details → empty/reset | Поля одно под другим, cards в одну колонку |
| Download | H1 → основная status/release panel + sidebar compatibility → help links | Status первым, требования следом; hash переносится |
| Updates | H1 → type filter → ordered story cards → pagination | Одна колонка, метаданные над заголовком |
| Update detail | Breadcrumb → type/date → H1/summary → текст 70ch → related action | Тот же порядок, без боковой колонки |
| Help | H1 → search → topics/articles → FAQ → issue link | Поля/карточки stacked |
| Help detail | Breadcrumb → title/version → steps → expected result → bug report | Текст 100% доступной ширины |
| 404/error | Shared shell → номер/сообщение → главная/help, retry при ошибке | Действия переносятся, не выходят за viewport |

Главная desktop: `[бренд | навигация | GitHub]`, затем `[H1/описание/CTA | SVG-концепт]`. Mobile: `[бренд | меню]`, `[H1] [описание] [CTA] [status] [концепт]`. Концепт не задерживает и не закрывает основной CTA.

## Состояния

Кнопки: normal/hover/active/focus; ссылки имеют различимые подписи. Status pills содержат слова. Inputs всегда с label. Details с видимым summary. Skeleton не заменяет постоянный текст. Empty — предметная причина и следующий путь; API error — сообщение и повтор, не «нет релизов». Download без артефактов не содержит активной download-кнопки. Released layout/clipboard описаны в WEBSITE-SPEC и остаются отдельным этапом API.

## Порядок визуальной приёмки

1. Главная и оболочка, desktop/mobile, readable above-fold CTA.
2. Все route templates без оборванных ссылок.
3. Клавиатура: skip → header → main → footer; dialog Tab/Esc/return.
4. Проверка viewport 375/768/1440, reflow/zoom и контрастов. Скриншоты отражают реализацию; полная матрица SC-23 закрывается отдельно.

Этот проект фиксирует компоновку и tokens; браузерный результат является визуальным прототипом. API релизов и финальная матрица производительности не объявляются готовыми вместе с первым UI-срезом.

# Space foundations

Рабочая Figma-коллекция: **Space / Foundations**, режим **space-dark**. 28 переменных импортированы штатным JSON-импортом и проверены в Variables. Источник для переноса между файлами — [space-dark.tokens.json](space-dark.tokens.json), формат DTCG. Значения соответствуют текущей Next.js CSS-палитре и выбранным размерам; это versioned-срез, автоматической двусторонней синхронизации нет.

| Группа | Состав |
|---|---|
| color, 11 | background, surface, raised, border, text, muted, accent, on-accent, violet, accent-hover, wordmark |
| space, 8 | 4, 8, 12, 16, 24, 32, 48, 64 px |
| radius, 2 | button 6 px, panel 10 px |
| size, 4 | touch-min 44 px, button 52 px, content-max 1280 px, header-desktop 96 px |
| font, 2 | Segoe UI, Consolas; в веб-CSS есть fallback-шрифты |
| motion, 1 | feedback 0.18 s; reduced-motion отключает переходы в CSS |

Подтверждённые привязки: корневые desktop/mobile homepage → color/background; заливка native **Button / Primary / Mobile** → color/accent. Кнопка преобразована в основной компонент, текст/стрелка остаются редактируемыми. Полный набор variants, текстовые стили, размеры/радиусы и остальные экраны ещё не связаны со всеми tokens. Например, исходный мобильный компонент пока имеет радиус 8 px; наличие radius/button=6 само по себе не означает его применение. SC-17 остаётся открытой.

Для нового Figma-файла создать пустую коллекцию, затем Import и выбрать JSON. Для обновления существующего режима использовать Import mode: он обновляет только совпадающие имена и типы, а не создаёт весь новый набор. При повторном импорте сверять число 28, группы и HEX. Подробности: [официальная инструкция Figma](https://help.figma.com/hc/en-us/articles/15343816063383-Modes-for-variables/#import-modes).

В файле остаётся исходная пробная Collection 1 с одной переменной background; новые привязки используют Space / Foundations. Пробная коллекция не является второй темой.

[Space-Client-Website-Design.fig](Space-Client-Website-Design.fig) — актуальная локальная копия шести макетов, обеих иллюстраций, переменных и первого компонента. Это резервная копия Design-файла; облачная синхронизация последнего среза не подтверждена. Восстановление — Import file в Figma, без удаления исходного облачного файла.

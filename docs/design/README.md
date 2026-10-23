# Space foundations

Рабочая Figma-коллекция: **Space / Foundations**, режим **space-dark**. 28 переменных импортированы штатным JSON-импортом и проверены в Variables. Источник для переноса между файлами — [space-dark.tokens.json](space-dark.tokens.json), формат DTCG. Значения соответствуют текущей Next.js CSS-палитре и выбранным размерам; это versioned-срез, автоматической двусторонней синхронизации нет.

| Группа | Состав |
|---|---|
| color, 11 | background, surface, raised, border, text, muted, accent, on-accent, violet, accent-hover, wordmark |
| space, 8 | 4, 8, 12, 16, 24, 32, 48, 64 px |
| radius, 2 | button 10 px, panel 10 px |
| size, 4 | touch-min 44 px, button 52 px, content-max 1280 px, header-desktop 96 px |
| font, 2 | Segoe UI, Consolas; в веб-CSS есть fallback-шрифты |
| motion, 1 | feedback 0.18 s; reduced-motion отключает переходы в CSS |

Исходные привязки: корневые desktop/mobile homepage → color/background; заливка native **Button / Primary / Mobile** → color/accent. Этот мобильный компонент сохраняет прежнюю геометрию 44 px / radius 8 px. Новая [библиотека Controls](CONTROL-STATES.md) использует высоту 52 px, radius/button=10, два text styles, три effect styles и 18 variants. Применение ко всем прежним экранам ещё не завершено; SC-17 остаётся открытой.

Для нового Figma-файла создать пустую коллекцию, затем Import и выбрать JSON. Для обновления существующего режима использовать Import mode: он обновляет только совпадающие имена и типы, а не создаёт весь новый набор. При повторном импорте сверять число 28, группы и HEX. Подробности: [официальная инструкция Figma](https://help.figma.com/hc/en-us/articles/15343816063383-Modes-for-variables/#import-modes).

В файле остаётся исходная пробная Collection 1 с одной переменной background; новые привязки используют Space / Foundations. Пробная коллекция не является второй темой.

[Space-Client-Website-Design.fig](Space-Client-Website-Design.fig) — резервная копия шести макетов, обеих иллюстраций, foundations и первого компонента до среза Controls. Новые компоненты от 24 октября находятся в облачном файле, проверены через Figma MCP и не включены в старый FIG. Восстановление резервной копии — Import file в Figma.

По запросу пользователя направление обновлено до Nebula: фон #080615, акцент #C6A4FF, violet #9E8AFF, текст #F5F0FF. Foundations содержит 28 переменных; Import mode подтвердил 13 изменённых значений. Коллекция **Space / Controls** добавляет 15 переменных, включая cyan #8DE8F5 и состояния кнопок/полей; четыре значения связаны aliases с Foundations. Генерация CSS: `npm run tokens:web`; проверка дрейфа и контрастов: `npm run tokens:web:check`. [Native QA и IDs](../qa/website-controls-figma-2026-10-24.json), [снимок витрины](control-states-figma.png). Анимации сайта остаются веб-реализацией; готовый анимированный Figma-прототип не заявляется.

# Space Launcher — shadcn/ui

Редизайн по просьбе пользователя: графитовая оболочка, голубой акцент, компактная навигация и спокойная космическая иллюстрация. Изменены **все три экрана лаунчера**: обзор, профили, настройки. Сайт этой итерацией не изменён.

## Компоненты и сборка

Использованы реальные Button, Card, Input и Badge из официального registry shadcn/ui, вариант new-york-v4, полученные 23.10.2026. Это локальные исходники в `apps/launcher/src/renderer/components/ui`, а не похожие CSS-классы поверх старой библиотеки. Импорты `cn` адаптированы к локальной функции clsx/tailwind-merge, `Slot` — к отдельному @radix-ui/react-slot; обычная высота Button/Input увеличена до 44 px. [MIT-лицензия](licenses/shadcn-ui.md) сохранена. [Manual installation](https://ui.shadcn.com/docs/installation/manual), [button source](https://ui.shadcn.com/r/styles/new-york-v4/button.json), [card source](https://ui.shadcn.com/r/styles/new-york-v4/card.json).

Конфигурация — `apps/launcher/components.json`; alias `@/*` ведёт в src. Зависимости закреплены: Tailwind CSS/CLI 4.3.3, class-variance-authority 0.7.1, clsx 2.1.1, tailwind-merge 3.7.0, @radix-ui/react-slot 1.5.0, Lucide React 1.61.0. Иконки — единый Lucide outline set; собственный знак Space — SVG. [Лицензия Lucide](licenses/lucide.txt) сохранена; обе лицензии копируются в `dist/licenses`.

Tailwind собирает статический `dist/renderer/app.css` перед esbuild. Сканирование ограничено исходниками renderer, внешних CSS/fonts/CDN нет. Сохранён существующий build без dev-server. RAM/progress используют native input/progress: не понадобились inline styles, которые нарушили бы действующую CSP. Sandbox, contextIsolation, точная проверка sender/mainFrame/URL и десять preload-функций сохранены. Main изменён только в цветах фона/titlebar.

## Визуальные правила

| Роль | Значение |
| --- | --- |
| Фон / sidebar | #0c0e12 / #0f1217 |
| Карточки | #12151b |
| Основной текст / вторичный | #f2f4f7 / #a2adbd |
| Акцент / текст кнопки | #8bd8f7 / #08202a |
| Focus ring / input border | #8bd8f7 / #586579 |
| Шрифт | Локальный Segoe UI Variable / Segoe UI / Arial |
| Радиусы | 8–14 px |
| Движение | Появление раздела 220 ms, hover иллюстрации 650 ms; без автопрокрутки/карусели |

Навигация постоянно доступна слева. Обзор показывает иллюстрацию, активный профиль и установку с одной основной кнопкой. Лишняя дублирующая кнопка баннера убрана. В небольшом окне 1000×680 кнопка подготовки видна без поиска внизу страницы. На узких viewport sidebar становится колонкой с подписанными aria-label иконками, карточки переходят в один столбец; main прокручивается отдельно.

Формы сохраняют прежние ограничения/черновики. Есть видимые labels, focus ring, aria-valuetext RAM, сообщения busy/error/status. После смены раздела focus переходит к заголовку. Skip-control переводит focus в main **без hash-навигации**, чтобы не изменить точный IPC URL. Prefers-reduced-motion отключает анимации/переходы, включая hover иллюстрации. Microsoft-вход честно помечен как разрабатываемый; кнопки запуска и фиктивной авторизации не добавлены.

UI/UX Pro Max использован для композиции и проверки contrast/focus/motion. Первый gaming-запрос предложил витрину с 3D и фиолетовым акцентом; он не соответствовал просьбе о новом desktop-дизайне и не применён. Повторный запрос дал Minimalism & Swiss Style, подходящий для dashboard; его типографическая/сеточная логика адаптирована к тёмному космическому бренду. Это решение проекта, не автоматически выбранная палитра каталога.

## Проверка

```powershell
npm.cmd run build:launcher
npm.cmd run check:ui --workspace @space/launcher
```

Проверка запускает **отдельный экземпляр Electron** с новым disposable userData и временным loopback DevTools endpoint. Не подключается к личному браузеру или текущему окну пользователя. Puppeteer Core 25.16.0 закреплён как dev dependency; debug flags добавляет только тестовый runner. Скрипт включён в Windows CI после build.

Проверены три экрана на 1180×760, 1000×680, 600×680 и 500×380: нет горизонтальной прокрутки; длинное имя на 40 символов переносится. Нижний control профиля виден при keyboard focus после автоматической прокрутки. Проверены создание двух профилей, запись имён в настоящий settings.json, переключение, RAM стрелкой, отмена, блокировка смены при dirty и сохранение черновика при клике активного профиля. Skip-control сохраняет exact URL. Проверено отсутствие Node globals, сохранение 10 bridge-функций, отсутствие inline style attributes и renderer/CSP ошибок.

Измеренные пары contrast: foreground/background **17,53:1**, muted/card **8,05:1**, CTA text/fill **10,61:1**, input boundary/background **3,27:1**, focus/background **12,2:1**. Это проверка перечисленных пар, не сертификат WCAG всего приложения. [JSON-отчёт](qa/launcher-shadcn-2026-10-23.json), [обзор](qa/launcher-shadcn-overview.png).

Снимки renderer осмотрены отдельно. Это **не** повторная приёмка native picker, OS zoom 200%, screen reader, полной матрицы клавиатуры, живой загрузки/отмены в новой оболочке или настоящего входа. 500×380 проверяет узкий layout, а не доказывает работу системного масштабирования. Прежние реальные install/repair проверки описаны в LAUNCHER-QA; честный запрет запуска сохраняется.

# Space / Navigation

Обновление Eclipse: текущий menu-border в JSON/CSS — #8B8079, контраст с surface 5.01:1; header #050507E8, backdrop #020304DB. Старые цвета и Figma-привязки ниже описывают предыдущий срез. [Текущая тема](ECLIPSE.md); новые значения в Figma пока не перенесены.

Срез 24 октября 2026, SC-17/18. Реализация сайта — источник поведения; Figma-библиотека навигации пока **черновик с известным дефектом**.

| Область | Правила |
|---|---|
| Desktop | От 761 px, четыре ссылки; текущий раздел учитывает вложенную статью. Hover/current имеют линию, focus — внешний контур. |
| Mobile | До 760 px включительно, кнопка «Меню» минимум 44 px; навигация открывается через native dialog. |
| Открытое меню | focus на «Закрыть ×», Tab/Shift+Tab остаются внутри; текущий пункт подчёркнут, имеет aria-current и акцентный цвет. |
| Закрытие | Escape/кнопка возвращают focus на trigger. Переход закрывает меню. При расширении до 761 px focus возвращается к текущей desktop-ссылке либо бренду. Scroll освобождается. |
| Размеры | Dialog: nominal min(viewport−32 px, 430 px), padding 24 px, radius 10 px, max-height 85dvh. Native UA max-width также влияет на итоговую ширину: в проверенном Chrome при 390 px — 352×459 px. На узких/низких экранах допускается внутренний scroll. |
| Движение | Reduced-motion отключает CSS transitions; изменение настройки ОС этим срезом не выполняется. |

В Figma созданы **Space / Navigation item** — Surface Desktop/Mobile × Current No/Yes × State Default/Hover/Focus (12 variants), **Space / Menu trigger** (3 states) и native **Space / Mobile navigation**. Панель собирается из instances навигации и ранее созданной secondary-кнопки. Основные компоненты расположены отдельно от [витрины](https://www.figma.com/design/rlT7ixlB1AVZVvp9BfRLZJ/Space-Client-Website-Design?node-id=30-94).

Витрина не принята: текст всех nav variants остаётся подчёркнутым по API readback, хотя у обычных ссылок нужен NONE. Попытка точечного исправления не дала ожидаемого результата; причина не установлена. После этого Figma MCP сообщил о лимите вызовов Starter, post-fix screenshot недоступен. Привязки и типографика новых компонентов требуют финальной сверки. Не считать наличие созданных слоёв выполнением SC-17. [IDs, состояние и дальнейшие действия](../qa/website-navigation-figma-2026-10-24.json). Старый FIG не включает новые компоненты.

Новые цвета — [navigation.tokens.json](navigation.tokens.json): header с alpha E8, backdrop с alpha DB, menu-border #7D639C. Они назначены отдельной коллекции **Space / Navigation**. CSS генерируется из трёх JSON; checker проверяет RGB/HEX/alpha и 20 контрастных пар, включая menu-border 3.67:1 на surface. Общий CSS содержит 46 переменных. Изменения JSON не импортируются в Figma автоматически.

На сайте добавлено только подчёркивание текущего пункта mobile menu; прежние цвета вынесены в переменные без изменения значений. Раскладка, Escape/focus и breakpoint проверены в собственном Chrome с настоящим API на 320/390/760/761/1050/1440 px. Figma использует Inter; сайт сохраняет Segoe UI с системными fallback. Это явное различие шрифтов.

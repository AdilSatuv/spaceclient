# Интеграция main и настройка Java · 3 ноября 2026

Пользователь прямо поручил перенести накопленные изменения в основную ветку и сообщил, что установленный лаунчер требует выбрать Java. Это заменяет прежнее ограничение этой волны на push/main. Публикация сайта и выпуск установщика остаются отдельными задачами.

## История и интеграция

Добавлен origin `https://github.com/AdilSatuv/spaceclient.git`. Удалённый main содержал два дополнительных коммита: 353604d и 4598d39 с обратной связью отправки фильтров и её QA. Merge 06a66af сохраняет обе истории. Перед интеграцией создана локальная safety/pre-main-20261103 на 313799d. Даты и авторство прежних коммитов не переписывались.

Единственный конфликт был в композиции browser flows: сохранены и полный каталог 55 модулей + 2 инструмента, и remote filter-submit checks. Ожидание старых шести записей в remote fixture заменено сравнением с реальным счётчиком до отправки: проверяется сохранение предыдущего документа, а полный состав каталога проверяется отдельно.

Firefox выявил дополнительную несовместимость QA: catalog fallback вызывал неподдерживаемую в закреплённом браузере команду BiDi emulation.setScriptingEnabled. В 5eabbbc общий сценарий без JavaScript вынесен в helper, который для Firefox использует существующий отдельный профиль javascript.enabled=false. Реальное отключение проверяется через media query scripting:none; отправка выполняется по фактическим границам кнопки. Проверка сохранена для обоих браузеров, исключения не подавляются.

На следующем прогоне Firefox обнаружил реальный дефект при Tab в мобильном меню 568×320 с увеличенными текстовыми интервалами: ссылка «Аккаунт» оставалась частично ниже границы dialog. В 9369b5c фокус явно раскрывает полный control через scrollIntoView(nearest), а scroll-padding сохраняет отступ для focus ring. Прежняя проверка видимости каждого control и циклического Tab/Shift+Tab/Escape сохранена без ослабления.

После этого обычные flows, accessibility и общий WEB-NO-JS прошли, но дополнительная отдельная catalog no-script страница завершилась script.callFunction timeout. В 6c71e40 убрано дублирование: performance/FPS filter и native GET проверяются на той же странице общего resilience-сценария без JavaScript, с прежними assertions и реальным pointer submit. Таймауты не увеличивались, неуспешные reports сохранены отдельно.

Короткий отдельный запуск точно локализовал зависание в $$eval при подсчёте performance cards. В 487ed25 используется синхронный document.querySelectorAll внутри page.evaluate, как в остальных работающих no-script assertions. Количество карточек, текст, отключение JavaScript и pointer submit по-прежнему проверяются. Отдельный Firefox no-script run95f41502-d423-4402-93e7-c63e729a9448 после этой замены прошёл; diagnostics теперь указывает фазу при ошибке.

## Проверки

- npm test: 45 общих + 30 API + 155 launcher = 230 passed; check и format:check passed.
- После исправления focus: повторно пройдены 21 website test, website types и форматирование изменённых файлов.
- Production API/Next build и 8 HTTP-сценариев passed. Сборка и browser QA выполняются в отдельной рабочей копии main-publication-verify; активный локальный сайт не пересобирается поверх его .next.
- Chrome156.0.8078.80: 42 layouts / 33 flows / errors0, run-fd0214e3-750f-41c5-8cae-92e24fac8757. После последнего изменения только QA дополнительно прошли общий no-script, module no-script и forced-colors на 487ed25, run-d86be9cf-16e8-425a-8e86-51d53f555310.
- Firefox158.0.1 на 487ed25: полный прогон 42 layouts / 31 flows / errors0, run-ccbcde0b-6947-454b-9f59-d6d569288217. Включает keyboard/pointer filter feedback, no-script filters, 24 text-spacing layouts, два short-screen focus loop и отдельный reduced-motion профиль.
- Источник игры при merge не менялся: tree19404e6dfa54414b4053945244243859fd483e6e. Для него уже пройдены 300 Java tests, strict launcher bundle и 61 native capture; подробности в qa/hud-graphite-restore-2026-11-03.json. Эта интеграция не является новой физической игровой приёмкой.

Локальные general/API/launcher логи: .reports/team-review/main-publish-{tests,types,format}.log. Browser reports находятся в собственной игнорируемой .reports проверочной рабочей копии; в Git не включаются caches, runtime, настройки и аккаунты.

## Причина запроса Java

Preview и установленный Space используют разные каталоги данных. В preview Java уже была выбрана; в активном профиле установленного приложения javaPath был null. Через настоящий inspectJava проверен локальный Java 21 x64; через ProfileStore.setJava сохранён только путь активного профиля, с предварительной резервной копией. Ник, RAM, остальные профили и глобальные настройки Java не менялись. После повторного открытия store и запуска probe получены usable=true, major=21, bits=64.

Уже открытый лаунчер должен перечитать settings.json после перезапуска. Его установленный bundle старый (treead06bc5f13ca76788f6c0832e9f72e77843219ac); push в main не обновляет Electron на диске. Для просмотра новых меню и косметики нужен текущий launcher bundle. Автоматическое обновление/перезапись работающего установленного приложения в этом срезе не выполнялось.

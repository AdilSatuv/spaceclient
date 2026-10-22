# Инструкции разработки

## Что уже проверено

В окружении есть Git, Node.js 24.15.0, npm 11.12.1 и системная Oracle JRE 8u503 без компилятора в PATH. Установлен локальный Temurin JDK 21 и Gradle wrapper; см. TOOLCHAIN.md. GitHub CLI не требуется: push работает через установленный Git Credential Manager. Установлены Next.js/NestJS-зависимости; см. WEB.md. Python для проекта не обязателен.

До установки инструментов сначала провести эксперимент из SPEC. Системный PATH и глобальную Java без необходимости не менять. JDK для legacy toolchain держать отдельно от возможного современного runtime. Версии зависимостей закреплять lockfile и wrapper; не использовать latest в принятом проекте.

## План установки

1. Проверить `git --version`, `node --version`, `npm.cmd --version`, `java -version`, `javac -version`.
2. Для кандидата Forge взять MDK из официального источника и выяснить совместимый toolchain по его wrapper/build-файлам. Для Legacy Fabric повторить проверку на официальном шаблоне. Старый Gradle запускать только подходящим JDK.
3. Установить нужный JDK локально, проверить источник и контрольную сумму, записать версии в ADR. Wrapper хранить в Git; не требовать глобальный Gradle.
4. После выбора frontend-стека установить локальные npm devDependencies и закоммитить package-lock.json. Для воспроизведения использовать `npm.cmd ci`.
5. Рабочие команды: `./scripts/game.ps1 build`, `npm.cmd test`, `npm.cmd run build`, `npm.cmd run check`. Запуск сайта/API описан в WEB.md, dev-клиента — в TOOLCHAIN.md.

## Git и contributions

Автор настроен локально для репозитория: `AdilSatuv <rrggaeton94@gmail.com>`. Remote: `https://github.com/AdilSatuv/spaceclient.git`. Работа в main для начального пустого репозитория; при появлении совместной работы — короткие ветки и PR.

После каждого законченного изменения:

```powershell
git diff --check
git status --short
# Выполнить проверки, соответствующие изменению.
git add <конкретные-файлы>
git commit -m "docs: describe completed research"
git push origin main
```

Коммит — реальный результат: исследование, ADR, функция или исправление. Не создавать пустые коммиты, искусственные даты или дробление строк ради счётчика. Пушить после каждого такого коммита и проверять результат. При ошибке доступа сохранить коммит локально и сообщить точную причину; пароль/токен не хранить в проекте.

По [GitHub Docs](https://docs.github.com/en/account-and-profile/how-tos/contribution-settings/troubleshooting-missing-contributions), email коммита должен быть связан с аккаунтом, а коммиты — находиться в default или gh-pages ветке; обновление графика может занимать до 24 часов. Связь данного email с аккаунтом из локальной конфигурации проверить невозможно. Сам по себе частый push не создаёт contributions без подходящих коммитов.

## Правила готовности

- Документация: ссылки, явное разделение фактов и предложений, `git diff --check`.
- Лаунчер: валидаторы, ошибки чтения/записи, перезапуск, IPC, отмена загрузки.
- Игра: реальный dev-запуск, переключение HUD, сохранение, разные разрешения.
- Сайт: мобильная/desktop-проверка, клавиатура, рабочие ссылки, отсутствие фиктивных загрузок.
- Релиз: сборка в CI с чистого checkout, smoke-test артефакта, changelog и tag.

Не коммитить node_modules, Java runtime, access/refresh tokens, .env, личные настройки, игровые assets и скачанные бинарные файлы Minecraft. Перед каждым пушем просмотреть staged diff.

# Собственный JAR в сборке лаунчера

Реализовано: remapped game artifact включается в desktop-сборку, проверяется и устанавливается в профиль. Это development-сборка, не публичный релиз. Запуск остаётся заблокированным до Microsoft-входа/владения/runtime validation.

## Происхождение игрового артефакта

Gradle добавляет `spaceclient-build.properties` в production remapped JAR:

- schemaVersion=1, channel=development;
- modVersion 0.1.0, Minecraft 1.8.9, loader 0.18.3;
- gameSourceTree — Git tree hash `HEAD:game` на момент сборки.

Tree hash задан как input processResources: смена committed game source заставляет обновить stamp и remapped JAR. Изменения вне game не требуют новой идентичной игровой сборки. Лаунчерный bundler должен отклонять грязное game tree и устаревший stamp; наличие JAR нужного имени само по себе недостаточно. Stamp описывает происхождение локальной сборки, не подпись издателя и не доказательство отсутствия чужой модификации.

У архивных задач отключены исходные timestamps и включён стабильный порядок файлов. Это устраняет два источника неодинаковых архивов; кроссплатформенная байтовая воспроизводимость отдельно не заявляется.

```powershell
./scripts/game.ps1 build
```

Game tests и remapJar проходят. Класс ClickCounter и остальные игровые/mixin классы присутствуют в remapped artifact. Пользовательский запуск из лаунчера этим не проверен.

## Упаковка

`apps/launcher/scripts/game-bundle.mjs` берёт только фиксированный `game/build/libs/space-client-0.1.0.jar`. Перед упаковкой проверяет чистоту `git status -- game` и совпадение stamp с `git rev-parse HEAD:game`. Не принимает путь JAR из UI или переменной окружения. Валидатор читает ограниченные байты один раз, поэтому между проверкой и записью не подменяется прочитанный source.

Ограничения инспектора: JAR до 4 МиБ, до 1024 entries, 1 МиБ на entry, 16 МиБ объявленного содержимого. Проверяются CRC/размер, недопустимые и повторные пути, обязательные классы, fabric.mod.json/mixins/stamp, версия игры/мода/loader и client entrypoint. `.class` должен иметь header JVM и bytecode не выше Java 8; известная named-ссылка MinecraftClient отклоняется. Классы `net/minecraft/` внутри собственного JAR запрещены. Это структурная проверка, не полный JVM verifier и не запуск кода из архива.

Проверенные bytes копируются в `dist/game/space-client.jar`. SHA-256, размер, версия и game tree компилируются непосредственно в main bundle; JSON рядом сохраняется для диагностики, main не использует его как новый источник доверия. Локальный manifest не заменяет подпись будущего публичного приложения.

Если game JAR отсутствует, обычный build создаёт вариант только с подготовкой базовых файлов. Лог сообщает об этом, старая generated resource удаляется по фиксированным именам. Для обязательной упаковки:

```powershell
# Игровые исходники сначала должны быть закоммичены.
./scripts/game.ps1 build
$env:SPACE_REQUIRE_GAME_BUNDLE = '1'
npm.cmd run launcher
```

Override действует в текущем shell-сеансе. Стандартная сборка после game build также включает JAR; REQUIRE запрещает тихий fallback при его отсутствии.

## Установка

Main вызывает `installSpaceBundle` с фиксированным ресурсом своего приложения, compiled pin и изолированным каталогом профиля. Renderer продолжает передавать только ID. Перед копированием снова проверяются структура JAR, tree, размер и SHA-256. Destination всегда `mods/space-client.jar`; parent junction и файл-ссылка отклоняются. Совпадающий файл переиспользуется, повреждённый восстанавливается; запись через уникальный temp + rename. Неудачная проверка source не заменяет старый JAR, временный файл очищается при ошибке/отмене.

После download/extraction появляется стадия installing. Только успешная копия добавляет файл/байты к итоговому прогрессу и spaceVersion к статусу. Receipt получает `kind: space-development`, metadata собственного JAR и `launchReady: false`; отсутствие bundle сохраняет честный `minecraft-base-only`. Старый receipt не заменяется при ошибке нового этапа. Существующие сторонние mods не удаляются; их совместимость пока не поддержана.

## CI

Game job собирает/тестирует production JAR и передаёт только наш JAR через Actions artifact с именем, включающим commit SHA. Windows launcher job зависит от game, скачивает artifact и собирает с REQUIRE=1. Upload/download actions закреплены commit SHA из официальных v4 refs. GitHub Release не создаётся; Minecraft jars/assets не входят в этот artifact. Web job может собрать вариант без игрового bundle.

## Приёмка

55 launcher tests: шесть bundle-инспектора/копирования и три дополнительных lifecycle проверяют source mismatch, metadata/bytecode/ZIP failures, постоянный путь, восстановление, junction, отмену, счётчики и сохранность receipt. Полные Node test/typecheck/format проходят; Gradle game build также прошёл.

Через настоящее окно подготовлена Space 0.1.0: 768 файлов, 153 362 112 байт и 16 DLL. Собственный JAR — 10 341 байт, SHA-256 `13c9de3c087aeff8ad1df5e11f7f113534f6682e463390e3ccb9e28b014c49f3`, gameSourceTree `637c3347d8a669ee6ff2a0cd280010cefd20479a`. Аудит сравнил установленный JAR с bundle и checked metadata; [результат](qa/launcher-base-2026-10-23.json), [окно](qa/launcher-installation.jpg). Это не live game launch; Microsoft-вход, владение, process lifecycle и публичный Windows installer остаются отдельными задачами.

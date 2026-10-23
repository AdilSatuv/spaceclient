# Собственный JAR в сборке лаунчера

В работе: связать remapped game artifact с desktop-сборкой и копировать его в изолированный профиль после проверки. Это локальная development-сборка, не публичный релиз и не обход авторизации Minecraft. Игра остаётся заблокированной до Microsoft-входа/владения/runtime validation.

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

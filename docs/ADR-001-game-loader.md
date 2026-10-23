# ADR 001: Legacy Fabric для первого игрового прототипа

Решение: использовать Legacy Fabric, Minecraft 1.8.9 / mappings build 604 / loader 0.18.3 / Loom и Legacy Looming 1.16.1 / Gradle 9.4.0 / JDK 21. Java bytecode — 8.

Сравнение проведено по официальным шаблонам. Forge MDK 11.15.1.2318 содержит Gradle 2.7, ForgeGradle 2.1-SNAPSHOT, jcenter и HTTP Maven. Полная сборка Forge не выполнялась: восстановление старой цепочки выделяется в отдельную задачу при необходимости совместимости с Forge-модами.

Legacy Fabric даёт рабочую сборку 1.8.9 на современном JDK; оба плагина доступны в фиксированной версии 1.16.1 вместо SNAPSHOT из шаблона. Проверены `build` и тесты конфигурации. Выбор не означает доказанного преимущества FPS или поддержки всех модов Fabric/Forge. Для первой версии используется только Fabric Loader + Mixin, без полного Legacy Fabric API.

Первый HUD: FPS, CPS, XYZ, состояния movement key bindings. F8 включает/выключает HUD и сохраняет `config/spaceclient-hud.properties`. Отдельные модули настраиваются в этом файле при закрытой игре; `cps=false` отключает CPS. Старые файлы схемы 1 без поля cps загружаются с cps=true. GUI редактор и переназначение hotkey остаются следующими задачами. При F1 и F3 HUD скрывается.

Выполнен `runClient` и вход в singleplayer: HUD виден, координаты сверены с F3, скрытие по F1/F3 проверено. Исправлен пропуск короткого F8: обработчик перенесён с опроса раз в tick на событие клавиатуры; repeat-события исключены. CPS наблюдает уже читаемое vanilla событие мыши и не вызывает Mouse.next(), не изменяет игровые действия. Раздельные окна используют System.nanoTime, при меню/потере фокуса/выходе из мира история сбрасывается. Контракты событий сверены с официальной документацией [Mouse](https://legacy.lwjgl.org/javadoc/org/lwjgl/input/Mouse.html) и [Keyboard](https://legacy.lwjgl.org/javadoc/org/lwjgl/input/Keyboard.html), точки Mixin — с локальным mapped bytecode закреплённой 1.8.9.

Ошибки совместимости Log4j с dev-toolchain исправлены: runtime API/Core 2.26.1 и отдельный клиентский XML. Журнал исключает строки session ID/access/refresh token. Ошибка старой Twitch-интеграции остаётся. Подробные результаты и непроверенные сценарии — в SMOKE-TEST.md. Команда: `./scripts/game.ps1 runClient`. Dev-клиент не заменяет пользовательский вход в Minecraft.

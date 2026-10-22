# Локальные инструменты

Windows x64, PowerShell. Node.js 24 и Git уже установлены. Системная Java — Oracle JRE 8u503, без javac. Для разработки установлен отдельный Eclipse Temurin JDK 21.0.12.1+1 в `.tools/jdk-21`; системная Java не изменяется.

```powershell
# Из корня репозитория; только для текущего процесса при необходимости:
Set-ExecutionPolicy -Scope Process Bypass
./scripts/bootstrap.ps1
./scripts/game.ps1 --version
./scripts/game.ps1 build
./scripts/game.ps1 runClient
```

Bootstrap загружает фиксированный официальный архив Temurin и сверяет SHA-256. Wrapper Gradle 9.4.0 также сверяет SHA-256 дистрибутива. Кэш находится в `.tools/gradle-cache`. Сборка компилирует Java 8 bytecode с JDK 21. Версия JDK сборки и минимальная версия runtime мода — разные параметры.

Источник wrapper и исходных настроек: [Legacy Fabric example](https://github.com/Legacy-Fabric/fabric-example-mod/tree/422896c188108632da97825f801719f0015b1b32), CC0. Используемые плагины закреплены на 1.16.1, loader 0.18.3, mappings 1.8.9 build 604. Полный Legacy Fabric API пока не нужен: минимальные события HUD будут подключены через Mixin.

Не добавлять `.tools`, `.gradle`, `game/run`, `game/build` в Git. Для обновления JDK изменить URL и хеш вместе, выполнить чистую установку и сборку. Загрузка инструментов не требует изменения глобального PATH.

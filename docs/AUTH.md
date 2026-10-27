# Microsoft-вход Space — реализация и настройка

Подготовка SC-06 от 27.10.2026: [пошаговые действия владельца, границы первичных источников и матрица приёмки](SC-06-REGISTRATION.md). Собственного Application ID ещё нет. Эта подготовка не подтверждает live OAuth или доступ к Minecraft API.

На 23.10.2026 реализованы зашифрованный кеш MSAL и ограниченный loopback callback в main-процессе. Настоящего входа, Xbox/XSTS, проверки владения и launch ещё нет. Собственный Application (client) ID пока не предоставлен владельцем; чужие регистрации не используются. SC-06 и SC-08 остаются открытыми.

## Хранилище

Закреплён официальный `@azure/msal-node` **7.1.1**. Приложение будет public client без client secret. Microsoft рекомендует сохранять кеш с шифрованием; официальный интерфейс интеграции — `ICachePlugin` с before/after callbacks ([MSAL caching](https://learn.microsoft.com/en-us/entra/msal/javascript/node/caching)).

`EncryptedMsalVault` хранит `userData/auth/msal-cache.bin`, отдельно от settings.json и игровых профилей. Зашифрованный envelope привязан к client ID и schemaVersion=1. Plaintext — только в памяти. Кеш ограничен 1 МиБ, ciphertext — 2 МиБ; контейнер MSAL и опасные object keys проверяются до передачи SDK. Это проверка контейнера, а не собственная реализация token validation.

`windowsCacheEncryption` использует только асинхронные методы Electron 45 после app.ready: `isAsyncEncryptionAvailable`, `encryptStringAsync`, `decryptStringAsync`. На Windows ключ защищает DPAPI. При недоступности ОС запись прекращается; незашифрованного fallback нет. Другие платформы пока отказаны. DPAPI защищает от других Windows-пользователей, но **не от программ с правами того же пользователя**. Синхронный API Electron уже deprecated ([safeStorage](https://www.electronjs.org/docs/latest/api/safe-storage)).

Чтение/запись последовательно проходят очередь. Запись создаёт уникальный временный файл только с ciphertext, выполняет sync и rename; временный файл очищается. Проверяются userData root, auth directory и cache file: ссылки/junction, не-файлы и hard links отклоняются. Это не защита от всех гонок или вредоносного кода того же пользователя; ACL каталога и целостность самого приложения остаются существенны.

Повреждённый, неподдерживаемый или привязанный к другой регистрации кеш не перезаписывается. Не создаются копии с plaintext. Сброс возможен через отдельный `clear()` для будущего явного logout/reset. Он удаляет локальный ciphertext и не означает отзыв Microsoft-сессии в браузере. Будущий auth service должен также отбросить in-memory PCA и Xbox/Minecraft tokens. Строки JavaScript нельзя надёжно обнулить после использования.

`MsalCacheAccess.run()` сериализует **всю операцию** PCA, включая чтения/удаление аккаунта, а не только файловые callbacks. При ошибке before callback последующий SDK finally не записывает случайное содержимое памяти. Нужен один координатор на приложение, single-instance lock сохраняется; несколько процессов не получают файловую блокировку через этот класс.

Классы пока не подключены к кнопке входа или renderer IPC. При интеграции наружу передаются только безопасные статусы; SDK errors, authorization codes, access/refresh tokens, serialized cache, browser URL и account claims не должны попадать в renderer, логи или отчёты. Сетевой слой ещё предстоит обернуть собственной безопасной обработкой ошибок.

## Проверки

- 9 тестов хранилища: восстановление и app binding, отсутствие plaintext, недоступность ОС и ошибка encrypt, повреждение/лимиты/schema, опасные keys, очередь после ошибки, rotation, junction/hard link, SDK finally после failed read, настоящая библиотека MSAL с двумя **вымышленными** аккаунтами и запрещённой сетью.
- Настоящий Electron 45/Windows DPAPI в двух отдельных процессах: запись fake cache, восстановление после restart, отклонение повреждения, сохранение ciphertext при failed write и явный reset. [Отчёт](qa/launcher-auth-vault-2026-10-23.json).
- Smoke прошёл в Windows CI вместе с web/game/launcher: [04edd1f](https://github.com/AdilSatuv/spaceclient/actions/runs/39930860045). Ни один из этих тестов не проверяет реальную лицензию, refresh или OAuth consent.

```powershell
npm.cmd run test:launcher
node apps/launcher/scripts/smoke-auth-vault.mjs
```

Smoke не создаёт BrowserWindow и не открывает Microsoft. Временные данные — только fake cache внутри игнорируемой `.reports/launcher-auth-vault-smoke`; секреты/учётные данные для него не нужны.

## Действия владельца регистрации

Регистрацию, подтверждение прав и вход выполняет владелец аккаунта. Не присылайте пароль, refresh token, client secret или recovery codes. Для настройки достаточно **публичного Application (client) ID** и сведений о разрешённых API/redirect URI.

1. В своём Microsoft Entra открыть App registrations → New registration, имя `Space Client Development`. Для Xbox/Minecraft нужен вариант с личными Microsoft-аккаунтами; для этого прототипа целевой вариант — **Personal accounts only**. Требования к tenant/правам/подписке смотреть в [официальной инструкции регистрации](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app); если доступа нет, сначала проверить доступные владельцу условия, без оформления платных услуг агентом.
2. Сохранить Application (client) ID из Overview. Для native public client **не создавать client secret**. Не использовать ID Lunar/Badlion/другого лаунчера.
3. Для запланированного system-browser loopback настроить платформу **Mobile and desktop applications**, redirect URI `http://localhost`. Это прототипная конфигурация; до живой проверки её надо сверить с фактическим callback и регистрацией. Порт localhost может быть динамическим: Microsoft игнорирует его при сопоставлении; путь обязан совпадать. Не добавлять wildcard, query или несколько одинаковых localhost URI с разными портами ([правила redirect URI](https://learn.microsoft.com/en-us/entra/identity-platform/reply-url)).
4. Проверить доступность Xbox/Minecraft API именно для этой регистрации. Наличие GUID или стандартного Graph User.Read не подтверждает доступ к Minecraft. Точные scopes/approval и результаты реального обмена надо зафиксировать в SC-06; ограничения нельзя обходить чужим ID. Агент пока не подтвердил текущую политику Minecraft для сторонних регистраций по доступному первичному источнику.
5. После настройки владелец проходит consent/вход в системном браузере самостоятельно. Только после проверки Microsoft → Xbox/XSTS → Minecraft profile/ownership можно разрешать игру. Покупки, согласия и изменения аккаунта автоматически не выполняются.

## Локальный callback

`listenForMicrosoftCode` открывает временный HTTP listener **только на 127.0.0.1**, с портом ОС и redirect URI `http://localhost:<port>`. Он ничего не открывает в браузере и не делает token exchange. State — 32 случайных байта (256 бит), сравнение через timingSafeEqual; accepted state расходуется один раз, после чего listener закрывается. Неверный запрос не расходует ожидаемую сессию.

Принимается только POST `/`, точный Host `localhost:<port>` и form-urlencoded без content encoding. OAuth в query, другой path/Host/method/media type, неизвестные/повторные параметры и несовпадение state отвергаются. Body ≤64 КиБ, code ≤16 КиБ, client_info ≤8 КиБ. Headers ≤8 КиБ, не более 8 соединений; IO deadline 5 с, общее ожидание по умолчанию 3 минуты. AbortSignal/close/timeout закрывают listener и принятые сокеты. Origin не используется как доказательство: браузерный ответ обязан пройти state и будущий PKCE token exchange.

Browser page всегда содержит общий текст «вернитесь в Space Client», no-store/CSP/no-referrer и не отражает code/state/provider details. Ответ провайдера `error` превращается в фиксированную ошибку, без raw error_description. Полученный код сам по себе не считается успешным входом.

9 тестов с настоящим локальным HTTP/TCP, **без Microsoft и браузера**: верный/повторный ответ; отсутствующий/неверный/дублированный state; host/path/query/method/media/encoding; declared/chunked oversize; provider refusal и отсутствие отражения данных; cancel/timeout/уже отменённый запрос; медленный незавершённый body. Это дополняет 9 vault tests; общий launcher-набор — **76 tests**. Реальная браузерная доставка, PKCE/code exchange, app quit и лицензия этим набором не подтверждены.

## Следующий слой и найденные ограничения

Кандидат для следующего слоя — MSAL `getAuthCodeUrl`/`acquireTokenByCode` и CryptoProvider PKCE S256, с нашим callback/state. В установленном MSAL 7.1.1 исходник `acquireTokenInteractive` создаёт свой loopback server; публичный InteractiveRequest не предоставляет AbortSignal. Его запуск как есть не удовлетворяет нашим требованиям отмены и ограниченного ожидания. URL должен открываться только через системный браузер после allowlist-проверки. При отмене во время token exchange требуется также остановить/отбросить поздний результат и не сохранить отменённую сессию; простого Promise.race недостаточно.

Найден первичный [Xbox GDK website authentication guide](https://learn.microsoft.com/en-us/gaming/gdk/docs/services/fundamentals/s2s-auth-calls/service-authentication/live-website-authentication?view=gdk-2604): он описывает consumers authority, XboxLive.signin/XboxLive.offline_access и обмен U token → XSTS, но относится к confidential website flow с secret и собственным relying party. Это **не готовая инструкция для Minecraft native public client**. Нельзя копировать secret в Electron, считать web-flow доказательством доступа Minecraft или подставлять чужой relying party. Различия scopes/автоматически добавляемых MSAL OIDC scopes и разрешений собственной регистрации требуют отдельного SC-06 smoke с владельцем до выбора окончательного token flow.

Остаются проверки PKCE/scopes/registration, запрета открытия произвольного URL, token exchange/refresh и отмены на каждом сетевом этапе, quit, Xbox/XSTS/профиля/владения. До них кнопка входа не должна изображать завершённую авторизацию.

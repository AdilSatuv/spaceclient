# SC-06: собственная регистрация Microsoft

Проверено 27 октября 2026. [Issue #6](https://github.com/AdilSatuv/spaceclient/issues/6) OPEN; все четыре критерия ещё требуют полной приёмки. Владелец сообщил, что собственной регистрации/Application ID нет. Готовы только vault и callback, не настоящий Microsoft-вход.

## Что сделать владельцу

1. Самостоятельно открыть [Microsoft Entra admin center](https://entra.microsoft.com/) → App registrations → New registration. Имя: `Space Client Development`; Supported account types: **Personal Microsoft accounts only**. [Официальная инструкция регистрации](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app) описывает необходимые права управления приложениями. Если каталог/регистрация недоступны, сообщить только этап и безопасный текст ошибки. Не оформлять подписку или платную услугу автоматически.
2. Завершить регистрацию и сохранить **Application (client) ID** из Overview. Это публичный идентификатор; его можно передать для настройки. Не путать с Object ID или Directory ID. Client secret для native public client не создавать; чужой ID не использовать.
3. Authentication → Add a platform → **Mobile and desktop applications** → `http://localhost` → Configure/Save. Это соответствует текущему callback: `http://localhost:<динамический порт>` с корневым путём, listener на IPv4 127.0.0.1. Не выбирать Web/SPA, implicit flow или wildcard. [Desktop quickstart](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-desktop-app-sign-in) подтверждает эту платформу и URI; [правила redirect URI](https://learn.microsoft.com/en-us/entra/identity-platform/reply-url) — сопоставление localhost без порта. Не менять URI на 127.0.0.1 без согласованного изменения кода/регистрации. Разрешение public client flows не включать наугад ради обхода ошибки; device-code/password flow в этот план не входят.
4. Открыть [Java Edition Support](https://help.minecraft.net/hc/en-us/sections/44460521913485), найти **Java Edition Game Service API Review or Application Process** ([прямая статья](https://help.minecraft.net/hc/en-us/articles/16254801392141)). Прочитать актуальные требования и, если статья требует заявку для нового ID, самостоятельно пройти указанную там форму. Подготовить название Space Client, свой Application ID, ссылку на репозиторий и описание: независимый desktop launcher, системный браузер, public client/PKCE, без встроенного пароля и client secret. Дополнительные поля определяет сама официальная форма; этот список не выдаётся за её точную схему.
5. Сообщить только client ID, выбранный тип аккаунтов, platform/redirect URI и статус проверки API: не подана / ожидает / одобрена / отказ. Контакты и переписку с поддержкой целиком передавать не нужно. Не присылать пароль, коды входа, authorization code, URL callback, токены, cookies, кеш или secret.

Регистрация и consent выполняются владельцем, агент их не выполняет. На текущем этапе готовой команды настоящего входа в Space ещё нет: открывать старую кнопку и считать её успешной проверкой нельзя. После подготовки безопасного auth service владелец отдельно начнёт вход и самостоятельно пройдёт Microsoft consent в системном браузере. Сейчас, пока пользователь занят за ПК, браузер не открывать.

## Что подтверждено источниками и что осталось неизвестным

Официальные Entra-документы подтверждают personal accounts и desktop localhost-конфигурацию. Их примеры с Microsoft Graph не доказывают разрешения Xbox/Minecraft. [Xbox website guide](https://learn.microsoft.com/en-us/gaming/gdk/docs/services/fundamentals/s2s-auth-calls/service-authentication/live-website-authentication?view=gdk-2604) описывает consumers, XboxLive.signin/XboxLive.offline_access и XSTS, но это confidential website flow с secret и своим relying party. Переносить его в Electron буквально нельзя; окончательный набор scopes для нашей public-client регистрации ещё не подтверждён.

Поисковая выдача официального Minecraft Help перечисляет статью API Review; прямое фоновое чтение статьи и раздела 27.10 вернуло только оболочку сайта. Поэтому актуальная форма, обязательность/условия approval, срок ответа и полный native API-контракт **не подтверждены прочитанным текстом первичного источника**. Ссылки из сторонних библиотек и ответы пользователей Microsoft Q&A не заменяют официальные условия. Владельцу нужно прочитать статью в своём браузере; если содержимое недоступно, использовать официальный Contact us и уточнить порядок для собственного ID. Агент ничего в поддержку не отправлял.

## Матрица будущей приёмки

| Этап | Условие принятия | Сейчас |
| --- | --- | --- |
| Регистрация | Свой public client, personal accounts, точный redirect, подтверждённые необходимые разрешения/API review | Нет ID |
| Microsoft | Настоящие PKCE S256 и одноразовый state; код обменивается с тем же verifier/redirect | Не реализовано/не проверено |
| Xbox и XSTS | Успешные отдельные обмены для собственной регистрации; корректные audience/relying party подтверждены | Не проверено |
| Minecraft | Успешная сессия, профиль и entitlement законного Java-аккаунта; публичный поиск ника не доказательство владения | Не проверено |
| Отказ | Отказ consent, недоступность API, неподходящий аккаунт/нет лицензии не разрешают официальный запуск | Только локальный callback refusal |
| Истечение | Истечение/refresh, interaction required и отзыв прав дают безопасный статус, без бесконечных повторов | Не проверено |
| Отмена/quit | На каждом сетевом этапе прекращается операция; поздние результаты не применяются и не сохраняются | Только локальный callback cancel/timeout |
| Данные | Реальные токены только в main/зашифрованном кеше; renderer, Git и логи не получают секреты | Fake vault tests; настоящий flow отсутствует |

HTTP 401/403 сам по себе не доказывает ни отсутствие лицензии, ни необходимость approval. Различать этап, безопасный код ошибки и проверенную конфигурацию; не сохранять raw body/SDK error. Нет лицензии или недоступен API — остановить официальный auth flow. Покупки и создание/изменение профиля не выполнять. Игру и загрузки для этой проверки не запускать.

## Независимая работа без ID

Следующий самостоятельный кодовый срез: main-only coordinator для getAuthCodeUrl/acquireTokenByCode с PKCE S256, allowlist системного browser URL, `response_mode=form_post` для существующего POST callback, фиксированными безопасными статусами и общим deadline/cancel/quit. Сначала проверить поддержку реального abort в закреплённом MSAL/network adapter; Promise.race не предотвращает позднюю запись кеша. Нужны fake transport tests для позднего ответа после отмены, refresh/denial/rate limit, logout и отсутствия секретов в renderer/логах. Этот план не означает, что слой уже реализован или что scopes/API разрешены.

Не выполнять пробные запросы Xbox/Minecraft без токенов ради отметки «API доступен»: DNS/TLS/HTTP-ответ не проверяет доступ собственной регистрации. Live smoke возможен только после конфигурации и отдельного действия владельца; отчёт содержит лишь этап, pass/fail, безопасный классифицированный код и время, без payload/идентификаторов аккаунта.

## Проверки этого среза

На исходниках `5bfb064c1e1abf969b3667a64470683b9d560d94`: `npm.cmd run test:launcher` — **108/108**, включая 9 callback и 9 vault tests. `node apps/launcher/scripts/smoke-auth-vault.mjs` — Electron 45/Windows DPAPI, fake cache, restart/corruption/failed-write/reset PASS; без BrowserWindow и настоящего аккаунта. Это не OAuth/API/ownership acceptance.

Codebase Memory: найден listenForMicrosoftCode, snippet сопоставлен с диском. Exact-path coverage auth-callback.ts/auth-vault.ts/msal-cache.ts: no_recorded_issue, freshness metadata_changed; docs/AUTH.md исключён из индекса. Все три исходника и AUTH прочитаны напрямую; полнота индекса не утверждается. Код, настройки аккаунта, игра, runtime и веб-preview не менялись.

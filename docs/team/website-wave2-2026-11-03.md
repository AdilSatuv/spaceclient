# Space account: API и сайт · 3 ноября 2026

Копия: `C:/Users/user/Documents/Codex/2026-11-03/space-client-website-agent/work/spaceclient-website`, ветка `feat/space-account-web`, база `cf20fc1c134f53848623097ff297d459640a6d44`. Согласованный architecture commit `ff6e11a` перенесён как `2226710`. Работа local-only; пользовательские приложения и runtime не используются.

## Первый отдельный срез: backend

AccountsModule отделён от content. Все HTTP DTO сохранены без breaking changes. Username и nickname уникальны без учёта регистра; username неизменяем. Пароли 15–128 Unicode code points со значимыми пробелами. Только случайная соль и async scrypt (N=65536,r=8,p=1,32-byte key,maxmem80MiB); максимум два KDF одновременно на сервис. Неизвестное имя проходит такой же KDF. Проверка ключа timingSafeEqual. Оpaque 32-byte session tokens хранятся только SHA-256-хешами; срок 7 дней, отзыв logout, максимум 10 сессий на аккаунт. Лимиты попыток/IP и mutation/assets сохранены в SQLite и ограничены по числу bucket-записей.

SQLite содержит пользователей, сессии и PNG BLOB: нет клиентских путей, remote URL или SSRF. Транзакции, unique constraints и expectedRevision предотвращают потерю изменений. Неиспользуемые медиа удаляются при изменении профиля. Startup запрещает symlink в пути к БД, WAL и SHM; неизвестная будущая версия БД не перезаписывается. `createApp()` создаёт отдельную in-memory БД для каждого fixture; только bootstrap и preview явно включают `.local/space-accounts` (или `SPACE_ACCOUNT_DATA`).

PNG: полные CRC и chunk boundaries/order/critical types, обязательные IHDR/IDAT/IEND, запрет APNG, точная длина ограниченной распаковки и полного zlib stream, фильтры 0–4 и восстановление каждой строки. Декодируются допустимые grayscale/RGB/palette/alpha глубины 1–16 bit, Adam7; индексы палитры проверяются. Результат сохраняется как metadata-free RGBA8 PNG. Дескриптор SHA-256/bytes относится к возвращённому очищенному изображению, а не исходным загруженным байтам. Skin64×64 classic/slim или64×32 classic; cape64×32/128×64/256×128; исходный PNG до256KiB. Строгий base64, JSON body до360KiB, безопасные ошибки без parser/raw input/password/token.

Первичные источники решений: [Node24 SQLite](https://nodejs.org/download/release/v24.15.0/docs/api/sqlite.html), [async scrypt и timingSafeEqual](https://nodejs.org/download/release/v24.15.0/docs/api/crypto.html), [Nest security headers](https://docs.nestjs.com/techniques/security), [PNG specification](https://www.w3.org/TR/png-3/). Installed NestExpressApplication.useBodyParser и Next route/security guides прочитаны локально. Внешних зависимостей не добавлено.

Ранние проверки: API build; **29 API tests pass** в штатном параллельном worker-наборе; **6 architecture tests pass**. Новые тесты подтверждают persistent restart, два клиента одного сервиса, wrong credentials, case-insensitive conflicts, session expiry/logout, stale revision, skin/cape upload/delete и digest, отдельные fixture stores, malformed/oversized JSON/media, rate limit, PNG CRC/zlib/filter/palette/Adam7. Это ранний backend commit; сайт/BFF и итоговая диагностика worker дополняются следующим коммитом.

## Сайт и BFF

`/account`: регистрация/вход/выход, изменение ника, загрузка/удаление скина и плаща, модель classic/slim, настоящий 2D preview скина и preview текстуры плаща. Отдельная ссылка в desktop/mobile navigation. Применение игрового профиля остаётся явным действием лаунчера; сайт не переименовывает запущенного игрока. Функции восстановления пароля/почты не имитируются.

Same-origin `/api/account/*` — фиксированный BFF к одному Account API. HttpOnly/SameSite=Strict host-only cookie, Secure для HTTPS (HTTP разрешён только local loopback). В браузерном JSON только allowlisted public profile, CSRF token или безопасная ошибка; sessionToken/пароли не попадают в JSON/localStorage/console. Проверяются Origin против протокола и Host реального запроса, CSRF cookie + custom header, JSON content-type, размер/время body и allowlist route/method. Forwarded headers не используются как доверенный origin. Upstream URL только серверная конфигурация, без redirect-follow и произвольных URL; transport/body timeout и лимиты, hash-verified PNG proxy. Account metadata noindex.

CSRF cookie сохраняется между вкладками, обновляется срок; новый выдаётся только если старого валидного нет. Выход очищает cookie после валидного same-origin/CSRF запроса даже при недоступном API. HTTP auth/rate/unavailable status и пояснение сохраняются: локальный выход не выдаётся за подтверждённый отзыв серверной сессии. Серверный отзыв выполняется best-effort; остаточная сессия может действовать до expiry.

Совместимые исправления после ревью: строгий typeof модели skin в shared parser и API, без String coercion. UI transport — отдельный `shared/api/index.client.ts`; server-only API остаётся в `index.server.ts`. Root architecture tests не ослаблялись.

До локального Image/Blob preview читаются только первые33 bytes PNG: signature/IHDR length+type и допустимые размеры. Лишь после этого создаётся Blob URL; ограничение256KiB проверяется до чтения. Асинхронная смена файлов защищена generation counter. Большой bitmap header и oversized file дают безопасную ошибку без canvas/blob preview, что проверено unit и headless UI. Полную проверку PNG по-прежнему выполняет сервер. Старый no-JS navigation regression ожидает пять ссылок вместо четырёх после добавления Account, без ослабления остальных assertions.

Ранний headless run `run-2363206c-3617-466a-93a9-6454b5bf0464`: HTTP security + desktop1440/mobile390 register/login/logout, nickname, skin/cape upload/preview/delete, persistence/reload, внешнее изменение и stale revision recovery прошли. Повторный HTTP run `run-7dfe9ae6-ad37-47a0-953d-21ac905bec58` дополнительно подтвердил стабильный CSRF второй вкладки и честный offline logout с cookie clear и действующей до отзыва серверной сессией. Итоговый browser rerun и осмотр кадров дополняются ниже.

Источники BFF: [Next data security](https://nextjs.org/docs/app/guides/data-security), [cookies](https://nextjs.org/docs/app/api-reference/functions/cookies), [BFF](https://nextjs.org/docs/app/guides/backend-for-frontend), дополнительно установленная документация Next16.4.2. Первоначальный HTTP-тест выявил несовпадение внутреннего route URL hostname и публичного Host; проверка Origin исправлена на реальный Host, чужие Origin по-прежнему отклоняются.

## Диагностика раннего выхода API worker

Прочитан исходный `.reports/team-review/final-workspace-tests.log` основной копии: `api.test.cjs` завершился через829.9053ms до обоих subtests, без причины кроме `test failed`. Причина не установлена и не объявляется исправленной. Сохранён `abortOnError:false` Nest (init errors передаются runner). API test script сохраняет обычные параллельные workers без retry/concurrency1 и добавляет test-only наблюдение start/exit/uncaughtExceptionMonitor: PID/PPID/thread, timestamp, memory и типы active resources. Error text, env, credentials и argv не записываются. Для native fatal error включён Node diagnostic report с exclude-env/exclude-network в ignored `.reports/api-workers`. Monitor не ловит/подавляет ошибку и не меняет exit code.

Отдельный self-test подтверждает неизменённые exit17 и uncaught exit1, наличие lifecycle записи и отсутствие private error text/env. Исправлен только путь preload в Windows fixture (file URL); диагностический провал fixture сохранён в раннем журнале. Первый полный API-набор с новым наблюдением: **30 pass**. Дополнительные raw parallel snapshots и итоговый workspace run фиксируются перед передачей; отсутствие повторения не доказывает устранения старой причины.

## Итог проверки и передача

| Проверка | Итог |
| --- | --- |
| Полный `npm test` на конечном source | **41 general +30 API +138 launcher =209 pass**, fail/cancelled/skipped0 |
| API parallel TAP/trace snapshots | Два отдельно запланированных штатных прогона, по30 pass, оба exit0; не retry и не concurrency1 |
| Types/tokens всех workspace | Pass |
| API / Next production build | Pass |
| Content HTTP states | Все8 pass |
| Raw `format:check`, `git diff --check` | Pass; общей конфигурации/lockfile не менял |
| Account HTTP + headless | `run-2a603af1-6a34-49bb-9b63-bb821ed4d689`, 3 группы, errors0, harness exit0 |

Финальный account run завершён `2026-11-03T06:36:15.289Z`: HTTP Origin/CSRF/content-type/body/secret checks; один persistent API с desktop bearer и web HttpOnly sessions; restart/logout/expiry/revision conflict; desktop1440/mobile390 регистрация, неверный пароль, вход, ник, skin/cape upload/preview/delete, огромный IHDR/oversized file до decode, две вкладки, реальное внешнее изменение и обновление, offline logout с удалением cookie и видимым предупреждением. Дополнительно account layout/text-spacing320/375/768/1024/1440, reduced-motion, mobile menu/Escape focus, фокус error summary, no-JS объяснение и отсутствие ложной формы. В browser JSON/localStorage/console session token/пароль не обнаружены. Кадры входа/профиля/offline logout на desktop/mobile осмотрены; встречающееся видимое состояние skip-link является keyboard focus внутри headless страницы, не системным вводом.

Все fixture API/Next/Chrome завершены; Next exit SIGTERM — штатная очистка harness. Отдельный ранее объявленный API55951 оставлен для E2E ведущего, не пользовательский сервис. Это процесс раннего backend source `f852388`; поздний strict-model input fix требует нового процесса при интеграции. Bootstrap: `HOST=127.0.0.1 PORT=55951 node apps/api/dist/main.js` после API build; сайт BFF использует серверный `SPACE_ACCOUNT_API_URL=http://127.0.0.1:55951` (или общий `SPACE_API_URL`). Не заменять текущий пользовательский preview.

API worker cause остаётся неизвестной: исходный ранний выход не воспроизведён. Native fatal report для старого сбоя отсутствует. Новая диагностика создаёт свидетельства для будущего повторения, не доказывает прошлую причину и не маскирует сбой. API self-test намеренно создаёт exit17/1 только в дочерних fixtures; это не аварии production API. Общая папка diagnostics содержит и ранний неудачный preload self-test, поэтому суммарный исторический nonzero1 нельзя выдавать за новый необъяснённый api.test.cjs crash.

Область: `apps/api` account module/bootstrap/parser/errors/tests/test diagnostics; `apps/website` account page/BFF/client-server adapters/navigation/scoped styles; совместимый typeof-fix `packages/account-contracts/src`; account QA, no-JS nav count, preview build; этот отчёт. Root architecture helper перенесён согласованным commit, root package.json/lock не изменены. Shared README/CHAT-HANDOFF, пользовательские runtime, игра, лаунчер и чужие копии не редактировались. Codebase Memory имеет прежний secure endpoint/DACL blocker; источники проверены напрямую, успешная индексация этой копии не заявляется.

Source commits: backend `f852388`, strict-model/worker `bf45066`, BFF/UI `5d4eed2`, bounded preview `76783c8`; отдельный commit отчёта идёт следом. `2226710` — cherry-pick уже принадлежащего ведущему `ff6e11a`, повторно переносить не нужно.

Границы: local account service и web UI, без Microsoft/public hosting/реальных аккаунтов/нового установщика/видимых окон/CUA/OS input. Настоящие DPAPI, запуск игры и применение appearance в production Java проверяются отдельными направлениями/ведущим; мои HTTP и UI fixtures этого не подменяют. Screen reader, Safari/Firefox и production HTTPS/proxy в этом срезе не проверены. Тяжёлые QA завершены; во время согласованного benchmark новые build/browser/Electron не запускались.

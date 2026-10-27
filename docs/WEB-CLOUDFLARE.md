# Бесплатный план и проверка OpenNext · SC-24

27 октября 2026 пользователь уточнил: **пока только бесплатный деплой**. Containers из исследования ниже исключены из текущего плана. Аккаунт найден в чате «Два лендинга для портфолио», повторно создавать его не нужно. `wrangler whoami` сообщил отсутствие CLI-аутентификации: browser login не равен CLI login. Login, временный аккаунт и публикация не запускались.

## Бесплатный кандидат

Next SSR через OpenNext на Workers Free + отдельный NestJS на Render Free Web Service. Это кандидат для preview; аккаунт Render и защищённое соединение ещё не настроены. Служебные workers.dev/onrender.com адреса позволяют начать без покупки домена, доступность имени не проверена. Автодеплой и платные binding не включены.

Workers Free: 100 000 запросов/день, 10 мс CPU/запрос и 3 МиБ сжатого Worker. Локальный dry-run показал 1121,01 KiB gzip: подходит по размеру, но не доказывает соблюдение CPU-квоты. Нужна удалённая проверка SSR на Free. Не включать платную Images binding; локальный image endpoint не доказывает production-оптимизацию. Для бесплатного preview сначала проверить отдачу готовых производных ресурсов без платного продукта.

Render Free засыпает после 15 минут простоя, пробуждение занимает около минуты. Это превышает наш API timeout 2500 мс: первый запрос получит штатный error/retry, после прогрева данные вернутся. Не увеличивать timeout вслепую и не обходить сон keep-alive. Часы общие для workspace (750/месяц), сборки/трафик тоже ограничены. При добавленном платёжном методе превышение трафика/сборок может оплачиваться: перед созданием проверить отсутствие автоматических расходов или доступный spend limit, карту не добавлять.

Render Free не принимает private-network ingress. Этот вариант **не выполняет прежнее требование приватного origin без дополнительной защиты**. До публикации нужен проверяемый server-to-server credential для HTTPS origin, без передачи в браузер и без изменения публичного `/v1` контракта. Открытый backend молча не размещать. Можно сохранить loopback, разместив оба Node-процесса в одном Free Web Service, но тогда хостинг сайта — Render, а Cloudflare frontend требует отдельного решения с доменом. Render рекомендует Free для preview, не production.

Следующий шаг требует фонового доступа к существующему Cloudflare аккаунту и выбранному бесплатному Node-хостингу. Пользователь подтвердил существующий аккаунт Render; фоновый API/CLI-доступ к нему пока не подключён. Пароли/токены не публиковать в чат/репозиторий. Ни один критерий публичного HTTPS ещё не принят.

## Фактический spike

Изолированная копия от `5bfb064c1e1abf969b3667a64470683b9d560d94`, Node 24.15.0, Next 16.4.2, OpenNext 1.20.13, Wrangler 4.148.0, @opennextjs/aws 4.1.12. Peer range адаптера допускает эту Next-версию. Зависимости установлены только в лаборатории, production lockfile не менялся.

Найден реальный дефект: workerd отклоняет `redirect: 'error'` до HTTP-запроса. Транспорт исправлен на `manual`; `!response.ok` отвергает 3xx. Настоящий HTTP-тест проверяет 301/302/303/307/308: ни один запрос не достигает Location и не становится данными каталога. Timeout, size, no-store и API `/v1` сохранены.

OpenNext build прошёл. Локальный runtime использовал `compatibility_date=2026-10-13` и `nodejs_compat`: дата 2026-10-27 была отвергнута закреплённым workerd как более новая, чем поддерживает бинарник. Это дата опыта, не финальный production manifest. OpenNext предупредил об ограниченной поддержке Windows; Linux/remote acceptance ещё нужны. Перед rebuild остановить свой Wrangler dev: Windows удерживает `.open-next`.

Worker:18824 → отдельный Nest:18825 на loopback: пять основных разделов и прямые статьи 200, неизвестная статья 404 при здоровом API, HTML noindex, запрещающий robots, пустой sitemap и nosniff. Next image endpoint локально отвечает image/png. При остановке своего Nest главная/help остаются 200, download показывает error/retry вместо empty. После восстановления возвращается пустой production-каталог, unknown update снова 404. Во время outage неизвестная статья имеет состояние недоступности: это не подтверждённый missing/404.

`wrangler deploy --dry-run` без входа завершился: 5421,03 KiB / gzip 1121,01 KiB, 46 assets. Публикации не было. Прошли npm test, Next/OpenNext build, check:web:states, check и format:check. Первая проверка форматирования обнаружила CRLF из local clone (core.autocrlf=true); форматирование выполнено только в лаборатории, перенесены только два изменённых файла. UI/keyboard/screen-reader/Firefox и production HTTPS в Worker в этом срезе не проверены.

Минимальная конфигурация опыта (не deployment manifest):

```ts
// apps/website/open-next.config.ts
import { defineCloudflareConfig } from '@opennextjs/cloudflare';
export default defineCloudflareConfig();
```

```json
{
  "name": "spaceclient-sc24-local",
  "main": ".open-next/worker.js",
  "compatibility_date": "2026-10-13",
  "compatibility_flags": ["nodejs_compat"],
  "assets": { "directory": ".open-next/assets", "binding": "ASSETS" },
  "vars": { "SPACE_SITE_INDEXABLE": "false" }
}
```

В чистой отдельной копии установить exact `@opennextjs/cloudflare@1.20.13` и `wrangler@4.148.0` как devDependencies workspace @space/website. Собрать API. Из apps/website выполнить `opennextjs-cloudflare build`, затем `wrangler dev --local --ip 127.0.0.1 --port 18824 --inspector-port 18826 --var SPACE_API_URL:http://127.0.0.1:18825 --var SPACE_SITE_INDEXABLE:false`. Nest запустить с `HOST=127.0.0.1 PORT=18825`. Инструменты брать из node_modules/.bin, без глобальной установки. Origin в опыте не задан: canonical с реальным HTTPS ещё не принят.

Источники: [Workers limits](https://developers.cloudflare.com/workers/platform/limits/), [Workers Request: manual вместо неподдерживаемого error](https://developers.cloudflare.com/workers/runtime-apis/request/), [Render Free: сон, расходы и private ingress](https://render.com/docs/free), [Render private network](https://render.com/docs/private-network).

Далее — исследование вариантов до уточнения бесплатного бюджета. Containers сохраняется только как справка, не план к исполнению.

---


# Cloudflare: справочное исследование вариантов до выбора Free

Проверено 27 октября 2026. Провайдер выбран пользователем; размещения Space Client ещё нет. В чате «Два лендинга для портфолио» подтверждены действующий аккаунт и статические сайты Pages (`adil-interface-portfolio.pages.dev`, `adil-orbit-light.pages.dev`). Это не подтверждает Workers Paid, свободный адрес Space или доступ интеграции к `AdilSatuv/spaceclient`. Существующие разрешения были ограничены репозиториями портфолио.

## Совместимость и выбор

| Путь | Что сохраняется | Что ещё доказать |
| --- | --- | --- |
| Cloudflare Containers: Next и Nest отдельными процессами в одном образе | Настоящие `next start` и Nest/Express на Node 24, приватный loopback API | Linux image, supervisor, Worker ingress, cold start, расходы и HTTPS |
| Workers + OpenNext для Next; отдельный Node origin для Nest | App Router/SSR через адаптер, API `/v1` | Workerd-проверка точных версий, приватная связь с Nest, Images, лимиты bundle |
| Workers + vinext | API-поверхность Next через Vite | Beta и отдельная полная проверка совместимости существующего сайта |
| Pages со static export | Только статические страницы | Не сохраняет текущий SSR/динамические запросы; не выбран |

Предлагаемый первый путь при требовании «всё внутри Cloudflare» — Containers с двумя отдельными Node-процессами в одном контейнере. Это архитектурный кандидат, не проверенное развёртывание. Nest нельзя объявлять совместимым с Workers только по наличию `nodejs_compat`. Если допустим отдельный сервер, существующие два Node-процесса можно оставить там, а Cloudflare использовать для HTTPS/проксирования или Tunnel; сервер оплачивается и обслуживается отдельно.

Cloudflare рекомендует vinext для новых приложений, но прямо помечает его beta. OpenNext адаптирует результат `next build`; поддержка SSR и App Router в таблице возможностей не доказывает работу всего приложения. Оптимизация изображений в этом пути использует Cloudflare Images. Не менять стек и не включать платные binding автоматически.

## Конкретная схема Containers для следующего среза

`Browser → HTTPS Worker → container:3000 (Next) → 127.0.0.1:3001 (Nest)`.

1. Собрать один Linux-образ из принятого Git SHA и lockfile на закреплённом Node 24. Сначала `npm ci`, API build (с контрактами), затем website build. Включить `@space/content-contracts/package.json` и `dist`, workspace symlinks и runtime dependencies. Не копировать один API dist и не использовать статический export.
2. Перед website build задать утверждённый HTTPS `SPACE_SITE_URL`, `SPACE_SITE_INDEXABLE=false` и `SPACE_API_URL=http://127.0.0.1:3001`. Сохранить эти значения в manifest версии. Отдельный build для другого origin: metadata может быть статической.
3. Supervisor запускает API с `HOST=127.0.0.1 PORT=3001` и website командой `npm run start:hosted --workspace @space/website -- --port 3000`. Обрабатывать SIGTERM, собирать stdout/stderr, завершать второй процесс при аварии первого. Предусмотреть записываемый Next image cache без долговременной зависимости от диска контейнера.
4. Worker пересылает только в Next:3000, сохраняет URL/query/status и security headers. Не публиковать Nest:3001 отдельным ingress, не отдавать fixture server. Readiness проверяет Nest `/v1/health` и Next. Обработать отказ контейнера ограниченным 503 без внутренних адресов.
5. Для preview создать отдельный Worker/environment только после согласования аккаунта/расходов. Не включать production auto-deploy до приёмки. У Containers нет полноценного Version URL для всей системы: `wrangler versions upload` обновляет Worker, но не образ.
6. До публикации: локальная Linux Docker/workerd проверка; затем HTTPS smoke на отдельном staging. Действие `wrangler deploy` не транзакционно: Worker может обновиться до ошибки сборки/публикации образа. Проверять фактическую пару Worker+image и завершение rollout.

Текущий Docker CLI доступен, но Linux engine не отвечает (`dockerDesktopLinuxEngine` pipe отсутствует). Приложение Docker не запускалось. Образ, supervisor и Worker ещё не созданы; ни container runtime, ни OpenNext runtime не считать принятыми по Node-тестам.

## Приёмка preview

- `/`, `/features`, `/download`, `/updates`, `/help`, прямые URL статей и неизвестная статья 404; `/v1/health` проверяется приватно. Без релиза нет выдуманной загрузки.
- `SPACE_SITE_INDEXABLE=false`: robots запрещает обход, sitemap пуст, HTML noindex; canonical/OG используют утверждённый HTTPS origin. `noindex` не является контролем доступа. Проверить и служебный workers.dev адрес, если он включён.
- Остановить только staging Nest: главная/help доступны, каталог показывает ошибку и ручной повтор; после восстановления данные возвращаются. Сохранить no-store, таймаут 2500 мс, JSON limit 1 МиБ и запрет redirect. Не кешировать динамический HTML и API через общие Cache Everything rules.
- Изображения/Next image endpoint, mobile menu, keyboard/focus, reduced-motion, forced-colors, no-JS, фильтры/Back и clipboard fallback. Повторить на HTTPS существующие headless проверки; локальные отчёты #23 не заменяют публичную приёмку, screen reader и Safari.
- Отдельно измерить холодный старт. Не расширять timeout API ради сокрытия неготового Nest. При общей упаковке readiness должен дождаться API до отправки запросов Next.

## Стоимость и условия

По официальному тарифу Containers требует Workers Paid (базово $5/месяц). Это не итоговый счёт: сверху учитываются ресурсы контейнера, сеть, Workers, Durable Objects и логи. Память и диск тарифицируются по выделенному размеру, CPU — по активному использованию. Sleep снижает активное время, но добавляет холодные старты. Бюджет нельзя оценивать без instance type, числа инстансов, времени активности, трафика и логирования; уведомление о расходах не считать жёстким ограничением счёта. До согласования не менять план, не покупать домен и не создавать ресурсы.

Исторический перечень до ответа пользователя (платные варианты теперь исключены): допустимы ли платный Workers/Containers и какой месячный потолок; адрес preview (можно сначала служебный адрес, свой домен не обязателен); нужно ли всё держать в Cloudflare или допустим отдельный Node origin. Аккаунт заново создавать не требуется. Токены и пароли не присылать в чат.

## Rollback и граница готовности

Хранить SHA, digest образа, Worker version, env manifest и предыдущую проверенную пару. Возвращать и Worker, и образ с соответствующим origin/env, затем дождаться rollout и повторить health/404/empty/error/noindex. Откат одного Worker не доказывает откат контейнеров. Смена origin или indexability требует повторной сборки Next. Базы и миграций пока нет.

SC-24 остаётся открытой: выбран Cloudflare и подготовлен runbook; hosting/domain/env/расходы и публичный HTTPS не приняты. Зависимость #23 остаётся открытой. Никаких внешних изменений, игровых запусков и реальных загрузок этот срез не выполняет.

## Официальные источники

- [Next.js / vinext, включая beta](https://developers.cloudflare.com/workers/framework-guides/web-apps/nextjs/).
- [OpenNext: адаптер, SSR и Images](https://developers.cloudflare.com/workers/framework-guides/web-apps/opennext/).
- [Workers Node.js compatibility](https://developers.cloudflare.com/workers/runtime-apis/nodejs/).
- [Containers: runtime и Node/Linux-среда](https://developers.cloudflare.com/containers/).
- [Публикация Containers, staging и границы rollout](https://developers.cloudflare.com/containers/guides/deploy/).
- [Тариф Containers](https://developers.cloudflare.com/containers/platform/pricing/).
- [Cloudflare Tunnel: отдельный origin](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/).

# Сайт и API

По выбору пользователя: Next.js + NestJS, оба на TypeScript. Один GitHub-репозиторий и npm workspaces.

```powershell
npm.cmd ci
npm.cmd run api
# Второй терминал:
npm.cmd run site
```

Next.js: http://127.0.0.1:3000, NestJS: http://127.0.0.1:3001/v1/health.

```powershell
npm.cmd test
npm.cmd run build
npm.cmd run check
# Production после build, в отдельных терминалах:
npm.cmd run start --workspace @space/api
npm.cmd run start --workspace @space/website
```

Версии закреплены в package.json/lockfile. `apps/website` использует App Router и Server Components. Главная страница читает `/v1/project` на сервере; при недоступном API показывает понятный статус и сохраняет основное содержимое. Переменная `SPACE_API_URL` задаёт адрес NestJS для Next-сервера; по умолчанию localhost:3001. Токены и браузерный CORS здесь не нужны: запрос идёт между серверами.

API v1:

- GET `/v1/health`: готовность процесса API.
- GET `/v1/project`: название, стадия разработки, целевые Minecraft/платформа.
- GET `/v1/releases`: пустой список до первого проверенного пользовательского релиза.

Порт API можно переопределить через `PORT`; для Next использовать его CLI-параметр порта. Локальные процессы слушают только loopback. Для размещения в контейнере/хостинге понадобятся настройка bind-адреса, TLS/reverse proxy и адрес API. База, аккаунты, CMS и платежи пока не нужны и не имитируются.

Сайт содержит главную, направления разработки, roadmap и FAQ. Отдельные страницы загрузок и changelog появятся вместе с реальными артефактами. Деплой Next + Nest требует хостинга, поддерживающего оба Node.js-процесса; статический или Worker-only хостинг не запускает NestJS как обычный сервер. Текущий этап — локальная разработка и GitHub.

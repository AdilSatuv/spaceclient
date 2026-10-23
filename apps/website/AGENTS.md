<!-- BEGIN:nextjs-agent-rules -->

## This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

## Space website boundaries

- Архитектура: docs/WEB-ARCHITECTURE.md. Общие DTO и runtime validators импортировать из @space/content-contracts; не импортировать исходники apps/api.
- Все запросы API принадлежат lib/server/content-client.ts. lib/server/api.ts помечен server-only и связывает transport, env и React cache. Страницы получают типизированный результат; пустые данные, отказ API и 404 не смешивать.
- Client Components и их транзитивные зависимости не импортируют lib/server и приватную конфигурацию. Не добавлять fetch в страницы/компоненты или передавать SPACE_API_URL в браузер.
- URL-нормализация и выбор рекомендуемой сборки — чистые функции lib/queries.ts и lib/catalog.ts. HTTP API использует строгую валидацию, браузерные URL — безопасные значения по умолчанию.
- Сохранять реальные empty/error/release/withdrawn/404 сценарии. После изменений: npm test, npm run build --workspace @space/website, npm run check:web:states, npm run check, npm run format:check.

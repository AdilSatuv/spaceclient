<!-- BEGIN:nextjs-agent-rules -->

## This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

## Space website boundaries

- Архитектура: docs/WEB-ARCHITECTURE.md. Общие DTO и runtime validators импортировать из @space/content-contracts; не импортировать исходники apps/api.
- FSD: src/_app → _pages → features → entities → shared; app/ содержит только адаптеры Next и literal route config. Импорты между соседними слайсами запрещены. Снаружи слайса использовать index.ts/index.server.ts, внутри — относительные импорты. Не обходить tests/website-architecture.test.cjs.
- Все запросы API принадлежат src/shared/api. Серверный публичный вход index.server.ts помечен server-only; server.ts связывает transport, env и React cache. Страницы получают типизированный результат; пустые данные, отказ API и 404 не смешивать.
- Client Components и их транзитивные зависимости не импортируют server API и приватную конфигурацию. Не добавлять fetch в страницы/компоненты или передавать SPACE_API_URL в браузер. Не смешивать server-only и универсальные экспорты в одном barrel.
- Нормализация query локальна в _pages/updates/model; выбор рекомендуемой сборки принадлежит entities/release. HTTP API использует строгую валидацию, браузерные URL — безопасные значения по умолчанию. Не создавать пустые слои/слайсы без сценария; локальную логику оставлять рядом со страницей.
- Сохранять реальные empty/error/release/withdrawn/404 сценарии. После изменений: npm test, npm run build --workspace @space/website, npm run check:web:states, npm run check, npm run format:check.

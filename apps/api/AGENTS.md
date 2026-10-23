# Space Content API

- Границы и порядок сборки — docs/WEB-ARCHITECTURE.md. Формат данных и runtime-проверки принадлежат @space/content-contracts; не дублировать их в приложениях.
- content/application не импортирует NestJS, HTTP или infrastructure. HTTP-контроллеры проверяют параметры и вызывают сервис. ContentModule собирает repository и сервис через providers; AppModule подключает модули.
- Production-каталог находится в content/infrastructure/curated-catalog.ts. Валидировать весь снимок до обслуживания запросов; repository не отдаёт внутренние изменяемые массивы.
- Для тестов передавать catalog в createApp; не менять глобальные production-массивы. Fixtures не включаются через публичный endpoint или production env.
- Контентный API остаётся read-only; аккаунты Space и вход по нику — отдельный будущий модуль. Не хранить пароли в каталоге контента и не имитировать регистрацию.
- Проверки: npm run test --workspace @space/api, npm test, npm run check:web:states после сборки Next. Сборка общего пакета запускается автоматически в prebuild/predev/precheck.

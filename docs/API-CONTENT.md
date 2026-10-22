# API контента v1

Этап SC-20. Данные versioned в `apps/api/src/content.ts`; записи проверяются при запуске. Публичных write-endpoints и базы нет.

| Запрос | Ответ |
|---|---|
| GET /v1/features | `{items: Feature[]}` |
| GET /v1/releases | `{items: Release[], total}` |
| GET /v1/updates?type=all&page=1 | `{items, page, pageSize:12, total}` |
| GET /v1/updates/:slug | Одна публичная запись |

Типы фильтра: all, development, release. Page — положительное целое до 999999; повторные, неизвестные и некорректные query-параметры отклоняются с HTTP 400. Страница за пределами списка возвращает пустой items с настоящим total. Порядок: publishedAt по убыванию, id при равной дате. Невалидный slug — 400, неизвестный — 404. Ошибки имеют безопасный `{statusCode, code, message}` без stack trace.

Проверяются уникальность id/slug, enum-статусы, ISO UTC timestamps, связь released feature → release и release → changelog. Артефакт требует положительного размера, 64 hex SHA-256, известной платформы и HTTPS URL строго из `github.com/AdilSatuv/spaceclient/releases/download/<tag>/<fileName>`. Credentials, query, fragment, чужой репозиторий или имя файла не принимаются.

Реального установщика пока нет: releases остаётся пустым. В content-тестах есть отдельные фикстуры, которые не публикуются API. Наличие записи о разработке не создаёт релиз клиента. Проверка схемы не заменяет тест установки и соответствие скачанного файла хешу при реальной публикации.

Проверки: HTTP health/project/features/updates/releases, пустые и невалидные запросы, pagination на 14 временных тестовых записях, отсутствующий write endpoint, целостность связей и URL allowlist. Команда `npm.cmd test`.

Изменение прототипного контракта: `/v1/releases` теперь использует `items` вместо раннего `releases`. Полноценная Next.js-интеграция выполняется вместе с новым Figma-интерфейсом, а не с отклонённым визуальным черновиком.

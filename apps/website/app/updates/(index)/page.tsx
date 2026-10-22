import Link from 'next/link';
import { PageIntro } from '../../../components/shell';
import ContentError from '../../../components/content-error';
import { getUpdates } from '../../../lib/api';
export const metadata = {
  title: 'Обновления — Space Client',
  description: 'Бортовой журнал Space Client: реальные изменения разработки и заметки о релизах.',
};
export default async function Updates({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const type =
    typeof params.type === 'string' && ['development', 'release'].includes(params.type)
      ? params.type
      : 'all';
  const page =
    typeof params.page === 'string' && /^[1-9]\d{0,5}$/.test(params.page) ? Number(params.page) : 1;
  const result = await getUpdates(type, page);
  return (
    <div className="container page-content">
      <PageIntro label="БОРТОВОЙ ЖУРНАЛ" title="Не релизы. Путь к ним.">
        Реальные изменения проекта. Заметки разработки ещё не означают выпуск клиента.
      </PageIntro>
      <form className="filter-form" action="/updates">
        <label>
          Тип записи
          <select name="type" defaultValue={type}>
            <option value="all">Все записи</option>
            <option value="development">Разработка</option>
            <option value="release">Релизы</option>
          </select>
        </label>
        <button className="button">Показать</button>
      </form>
      {!result.ok ? (
        <ContentError title="Не удалось загрузить журнал." />
      ) : (
        <>
          {result.data.items.length ? (
            <div className="article-links">
              {result.data.items.map((item) => (
                <Link className="page-panel" key={item.id} href={`/updates/${item.slug}`}>
                  <p className="eyebrow">
                    {item.type === 'release' ? 'РЕЛИЗ' : 'РАЗРАБОТКА'} ·{' '}
                    <time dateTime={item.publishedAt}>
                      {new Date(item.publishedAt).toLocaleDateString('ru-RU', {
                        day: 'numeric',
                        month: 'long',
                        year: 'numeric',
                        timeZone: 'UTC',
                      })}
                    </time>
                  </p>
                  <h2>{item.title}</h2>
                  <p>{item.summary}</p>
                  <span className="text-link">Читать заметку →</span>
                </Link>
              ))}
            </div>
          ) : (
            <section className="empty-state">
              <h2>
                {page > 1
                  ? 'На этой странице записей нет.'
                  : type === 'release'
                    ? 'Релизов ещё не было.'
                    : 'Здесь пока тихо.'}
              </h2>
              <p>Следите за ходом разработки и будущими записями.</p>
              <Link className="text-link" href="/updates">
                Все обновления →
              </Link>
            </section>
          )}
          <nav className="pagination" aria-label="Страницы обновлений">
            {page > 1 && (
              <Link className="button secondary" href={`/updates?type=${type}&page=${page - 1}`}>
                ← Назад
              </Link>
            )}
            <span>
              Страница {page} из {Math.max(1, Math.ceil(result.data.total / result.data.pageSize))}
            </span>
            {page * result.data.pageSize < result.data.total && (
              <Link className="button secondary" href={`/updates?type=${type}&page=${page + 1}`}>
                Дальше →
              </Link>
            )}
          </nav>
        </>
      )}
    </div>
  );
}

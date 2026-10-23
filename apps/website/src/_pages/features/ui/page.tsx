import Link from 'next/link';
import { PageIntro } from '@/shared/ui/page-intro';
import { FilterForm } from '@/shared/ui/filter-form';
import ContentError from '@/shared/ui/content-error';
import { getFeatures } from '@/shared/api/index.server';
import { statusLabels } from '../model/status-labels';
import { pageMetadata } from '@/shared/lib/metadata';
export async function generateMetadata() {
  const result = await getFeatures();
  return pageMetadata(
    '/features',
    'Возможности — Space Client',
    'HUD-модули Space Client: возможности, прототипы и планы разработки.',
    !result.ok,
  );
}
const categories = ['hud', 'comfort', 'launcher', 'performance'];
const statuses = ['prototype', 'planned', 'verified', 'released'];
export default async function Features({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const q = typeof params.q === 'string' ? params.q.slice(0, 120).trim() : '';
  const category =
    typeof params.category === 'string' && categories.includes(params.category)
      ? params.category
      : '';
  const status =
    typeof params.status === 'string' && statuses.includes(params.status) ? params.status : '';
  const result = await getFeatures();
  const items = result.ok
    ? result.data.items.filter(
        (item) =>
          (!category || category === item.category) &&
          (!status || status === item.status) &&
          `${item.name} ${item.summary} ${item.details}`
            .toLocaleLowerCase('ru')
            .includes(q.toLocaleLowerCase('ru')),
      )
    : [];
  return (
    <div className="container page-content">
      <PageIntro label="ТВОЙ ИНСТРУМЕНТАРИЙ" title="Твой интерфейс. Твои правила.">
        От базового HUD до будущих профилей. Каждая возможность — со своим реальным статусом.
      </PageIntro>
      <FilterForm key={`${q}|${category}|${status}`} action="/features">
        <label>
          Поиск
          <input name="q" defaultValue={q} placeholder="Например, координаты" maxLength={120} />
        </label>
        <label>
          Категория
          <select name="category" defaultValue={category}>
            <option value="">Все категории</option>
            <option value="hud">HUD</option>
            <option value="comfort">Удобство</option>
            <option value="launcher">Лаунчер</option>
            <option value="performance">Производительность</option>
          </select>
        </label>
        <label>
          Статус
          <select name="status" defaultValue={status}>
            <option value="">Все статусы</option>
            <option value="prototype">Прототип</option>
            <option value="planned">В планах</option>
            <option value="verified">Проверено</option>
            <option value="released">В релизе</option>
          </select>
        </label>
        <button className="button">Найти</button>
      </FilterForm>
      {!result.ok ? (
        <ContentError title="Не удалось загрузить возможности." />
      ) : (
        <>
          <p className="result-count" role="status">
            Найдено: {items.length}
          </p>
          {items.length ? (
            <div className="catalog">
              {items.map((item) => (
                <article className="page-panel" key={item.id}>
                  <span className="status">{statusLabels[item.status]}</span>
                  <h2>{item.name}</h2>
                  <p>{item.summary}</p>
                  <details>
                    <summary aria-label={`Подробнее: ${item.name}`}>Подробнее</summary>
                    <p>{item.details}</p>
                    {item.releaseId && (
                      <Link className="text-link" href="/download">
                        Релизы клиента →
                      </Link>
                    )}
                  </details>
                </article>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <h2>Ничего не найдено</h2>
              <p>Попробуйте другой запрос или уберите ограничения.</p>
              <Link className="text-link" href="/features">
                Сбросить фильтры →
              </Link>
            </div>
          )}
        </>
      )}
    </div>
  );
}

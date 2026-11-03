import Link from '@/shared/ui/site-link';
import { PageIntro } from '@/shared/ui/page-intro';
import { FilterForm } from '@/shared/ui/filter-form';
import ContentError from '@/shared/ui/content-error';
import { getFeatures } from '@/shared/api/index.server';
import { statusLabels } from '../model/status-labels';
import {
  categoryLabels,
  countFeatures,
  featureKind,
  filterFeatures,
  kindLabels,
} from '../model/catalog';
import { pageMetadata } from '@/shared/lib/metadata';
export async function generateMetadata() {
  const result = await getFeatures();
  return pageMetadata(
    '/features',
    'Возможности — Space Client',
    '55 модулей Space Client для Minecraft 1.8.9: 29 игровых и 26 HUD, редактор и профили запуска. Возможности и ограничения локального прототипа.',
    !result.ok,
  );
}
const statuses = ['prototype', 'planned', 'verified', 'released'];
export default async function Features({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const q = typeof params.q === 'string' ? params.q.slice(0, 120).trim() : '';
  const category =
    typeof params.category === 'string' && Object.hasOwn(categoryLabels, params.category)
      ? params.category
      : '';
  const status =
    typeof params.status === 'string' && statuses.includes(params.status) ? params.status : '';
  const result = await getFeatures();
  const all = result.ok ? result.data.items : [];
  const items = filterFeatures(all, q, category, status);
  const counts = countFeatures(all);
  return (
    <div className="container page-content module-catalog-page">
      <PageIntro
        section="Возможности"
        label="ТВОЙ ИНСТРУМЕНТАРИЙ"
        title="Твой интерфейс. Твои правила."
      >
        Модули Minecraft 1.8.9, их настройки и реальные ограничения. Найди нужное по названию или по
        тому, что хочешь изменить в игре.
      </PageIntro>
      {result.ok && all.length > 0 && (
        <aside className="catalog-overview" aria-label="Состав и статус каталога">
          <p className="catalog-total">
            <strong>{counts.modules} модулей</strong>
            <span>
              {counts.game} игровых · {counts.hud} HUD · {counts.tools} инструмента отдельно
            </span>
          </p>
          <p>Локальный прототип. Публичного релиза и готового установщика пока нет.</p>
          <details>
            <summary>Проверки и ограничения</summary>
            <p>
              Проверены настройки модулей и отдельные игровые сценарии на локальном тестовом
              сервере. Полная проверка клавиатуры и мыши, внешних серверов и шейдеров ещё впереди.
              Оценки клиента отмечены в описаниях. Официальный онлайн-вход пока недоступен.
            </p>
          </details>
        </aside>
      )}
      <FilterForm key={`${q}|${category}|${status}`} action="/features">
        <label>
          Поиск
          <input name="q" defaultValue={q} placeholder="Например, координаты" maxLength={120} />
        </label>
        <label>
          Категория
          <select name="category" defaultValue={category}>
            <option value="">Все категории</option>
            {Object.entries(categoryLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
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
          <div className="filter-results">
            <p className="result-count" role="status">
              Найдено: {items.length} из {all.length}
              {q && <> · По запросу «{q}»</>}
              {category && <> · {categoryLabels[category]}</>}
              {status && <> · {statusLabels[status]}</>}
            </p>
            {(q || category || status) && (
              <Link className="filter-reset" href="/features">
                Сбросить фильтры
              </Link>
            )}
          </div>
          {items.length ? (
            <div className="catalog">
              {items.map((item) => (
                <article
                  className="page-panel"
                  key={item.id}
                  id={item.slug}
                  data-feature-kind={featureKind(item)}
                >
                  <div className="catalog-labels">
                    <span className="status">{statusLabels[item.status]}</span>
                    <span className="catalog-kind">{kindLabels[featureKind(item)]}</span>
                  </div>
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
          ) : all.length === 0 ? (
            <div className="empty-state">
              <h2>Каталог пока пуст</h2>
              <p>
                Описания ещё не опубликованы. Это не ошибка поиска; загляните в обновления проекта.
              </p>
              <Link className="button secondary" href="/updates">
                Обновления проекта
              </Link>
            </div>
          ) : (
            <div className="empty-state">
              <h2>Ничего не найдено</h2>
              <p>
                Попробуйте «броня», «камера» или «координаты». Можно также сбросить категорию и
                статус.
              </p>
              <Link className="button secondary" href="/features">
                Сбросить фильтры
              </Link>
            </div>
          )}
        </>
      )}
    </div>
  );
}

import Link from 'next/link';
import { PageIntro } from '../../components/shell';
import { helpArticles } from '../../lib/content';
export const metadata = { title: 'Помощь — Space Client' };
export default async function Help({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const q = typeof params.q === 'string' ? params.q.slice(0, 120) : '';
  const items = helpArticles.filter((item) =>
    `${item.title} ${item.summary}`.toLowerCase().includes(q.toLowerCase()),
  );
  return (
    <div className="container page-content">
      <PageIntro label="ЦЕНТР ПОМОЩИ" title="Найдём ответ.">
        Начало работы, совместимость и полезные инструкции. Без лишних сложностей.
      </PageIntro>
      <form className="filter-form" action="/help">
        <label>
          Поиск по инструкциям
          <input name="q" defaultValue={q} maxLength={120} placeholder="Что хотите узнать?" />
        </label>
        <button className="button">Найти</button>
      </form>
      <div className="article-links">
        {items.map((item) => (
          <Link className="page-panel" key={item.slug} href={`/help/${item.slug}`}>
            <h2>{item.title} →</h2>
            <p>{item.summary}</p>
          </Link>
        ))}
      </div>
      {!items.length && (
        <div className="empty-state">
          <h2>Ответ пока не найден</h2>
          <p>Попробуйте изменить запрос.</p>
          <Link className="text-link" href="/help">
            Сбросить поиск →
          </Link>
        </div>
      )}
      <details>
        <summary>Нужна лицензия Minecraft?</summary>
        <p>
          Пользовательский лаунчер планируется с Microsoft-входом и проверкой владения игрой. Space
          Client не заменяет покупку Minecraft.
        </p>
      </details>
      <details>
        <summary>Где оставить предложение?</summary>
        <p>
          <a className="text-link" href="https://github.com/AdilSatuv/spaceclient/issues">
            Откройте issue в нашем репозитории ↗
          </a>
        </p>
      </details>
    </div>
  );
}

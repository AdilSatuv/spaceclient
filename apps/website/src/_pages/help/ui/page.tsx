import Link from 'next/link';
import { PageIntro } from '@/shared/ui/page-intro';
import { FilterForm } from '@/shared/ui/filter-form';
import { helpArticles } from '@/entities/help-article';
import { pageMetadata } from '@/shared/lib/metadata';
import { helpFaq } from '../model/faq';
export const metadata = pageMetadata(
  '/help',
  'Помощь — Space Client',
  'Начало работы, совместимость и ответы на вопросы о Space Client.',
);
export default async function Help({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const q = typeof params.q === 'string' ? params.q.slice(0, 120).trim() : '';
  const matches = (text: string) =>
    text.toLocaleLowerCase('ru').includes(q.toLocaleLowerCase('ru'));
  const items = helpArticles.filter((item) => matches(`${item.title} ${item.summary}`));
  const questions = helpFaq.filter((item) => matches(`${item.question} ${item.answer}`));
  return (
    <div className="container page-content">
      <PageIntro label="ЦЕНТР ПОМОЩИ" title="Найдём ответ.">
        Начало работы, совместимость и полезные инструкции. Без лишних сложностей.
      </PageIntro>
      <FilterForm key={q} action="/help">
        <label>
          Поиск по помощи
          <input name="q" defaultValue={q} maxLength={120} placeholder="Что хотите узнать?" />
        </label>
        <button className="button">Найти</button>
      </FilterForm>
      <p className="result-count" role="status">
        Найдено: {items.length + questions.length} · Инструкции: {items.length} · Ответы:{' '}
        {questions.length}
      </p>
      {items.length > 0 && (
        <section className="help-results" aria-labelledby="help-articles-title">
          <h2 id="help-articles-title">Инструкции</h2>
          <div className="article-links">
            {items.map((item) => (
              <Link className="page-panel" key={item.slug} href={`/help/${item.slug}`}>
                <p className="eyebrow">ИНСТРУКЦИЯ</p>
                <h3>{item.title} →</h3>
                <p>{item.summary}</p>
              </Link>
            ))}
          </div>
        </section>
      )}
      {questions.length > 0 && (
        <section className="help-results" aria-labelledby="help-faq-title">
          <h2 id="help-faq-title">Вопросы и ответы</h2>
          {questions.map((item) => (
            <details key={item.id} id={`faq-${item.id}`}>
              <summary>{item.question}</summary>
              <p>{item.answer}</p>
              <p>
                <Link className="text-link" href={item.link.href}>
                  {item.link.label}
                </Link>
              </p>
            </details>
          ))}
        </section>
      )}
      {!items.length && !questions.length && (
        <div className="empty-state">
          <h2>Ответ пока не найден</h2>
          <p>Попробуйте изменить запрос.</p>
          <Link className="text-link" href="/help">
            Сбросить поиск →
          </Link>
          <p>
            <a className="text-link" href="https://github.com/AdilSatuv/spaceclient/issues">
              Задать вопрос или сообщить о проблеме ↗
            </a>
          </p>
        </div>
      )}
    </div>
  );
}

import { notFound } from 'next/navigation';
import Link from 'next/link';
import { helpArticles } from '@/entities/help-article';
import { pageMetadata } from '@/shared/lib/metadata';
export function generateStaticParams() {
  return helpArticles.map((article) => ({ slug: article.slug }));
}
export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }) {
  const slug = (await params).slug;
  const article = helpArticles.find((item) => item.slug === slug);
  return pageMetadata(
    `/help/${slug}`,
    `${article?.title || 'Инструкция не найдена'} — Space Client`,
    article?.summary || 'Инструкция не найдена.',
    !article,
  );
}
export default async function Article({ params }: { params: Promise<{ slug: string }> }) {
  const slug = (await params).slug;
  const item = helpArticles.find((article) => article.slug === slug);
  if (!item) notFound();
  return (
    <article className="container article-body page-content">
      <div className="page-intro">
        <Link className="back-link" href="/help">
          Помощь /
        </Link>
        <p className="eyebrow">
          ОБНОВЛЕНО{' '}
          <time dateTime={item.updatedAt}>
            {new Date(`${item.updatedAt}T00:00:00Z`).toLocaleDateString('ru-RU', {
              timeZone: 'UTC',
            })}
          </time>{' '}
          · СТАДИЯ РАЗРАБОТКИ
        </p>
        <h1>{item.title}</h1>
        <p className="lead">{item.summary}</p>
      </div>
      <ol>
        {item.steps.map((step) => (
          <li key={step}>{step}</li>
        ))}
      </ol>
      <a className="text-link" href="https://github.com/AdilSatuv/spaceclient/issues">
        Открыть задачи и сообщения об ошибках ↗
      </a>
    </article>
  );
}

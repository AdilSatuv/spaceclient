import { notFound } from 'next/navigation';
import Link from 'next/link';
import { getUpdate } from '../../../lib/server/api';
import { validSlug } from '@space/content-contracts';
import ContentError from '../../../components/content-error';
import { pageMetadata } from '../../../lib/metadata';
export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  if (!validSlug(slug))
    return { title: 'Запись не найдена — Space Client', robots: { index: false, follow: false } };
  const result = await getUpdate(slug);
  return result.ok
    ? pageMetadata(`/updates/${slug}`, `${result.data.title} — Space Client`, result.data.summary)
    : { title: 'Журнал — Space Client', robots: { index: false, follow: false } };
}
export default async function UpdatePage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  if (!validSlug(slug)) notFound();
  const result = await getUpdate(slug);
  if (!result.ok && result.reason === 'missing') notFound();
  if (!result.ok)
    return (
      <div className="container page-content">
        <div className="page-intro">
          <Link className="back-link" href="/updates">
            Обновления /
          </Link>
          <h1>Журнал временно недоступен.</h1>
        </div>
        <ContentError title="Не удалось открыть запись." />
      </div>
    );
  const item = result.data;
  return (
    <article className="container page-content article-body">
      <div className="page-intro">
        <Link className="back-link" href="/updates">
          Обновления /
        </Link>
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
        <h1>{item.title}</h1>
        <p className="lead">{item.summary}</p>
      </div>
      {item.blocks.map((block, i) => (
        <section key={i}>
          <h2>{block.heading}</h2>
          <p>{block.text}</p>
        </section>
      ))}
      <Link className="text-link" href={item.type === 'release' ? '/download' : '/features'}>
        {item.type === 'release' ? 'Сборки и совместимость' : 'Возможности и их статус'} →
      </Link>
    </article>
  );
}

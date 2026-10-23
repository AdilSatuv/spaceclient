import type { MetadataRoute } from 'next';
import { configuredOrigin, indexingEnabled } from '../lib/metadata';
import { getUpdates } from '../lib/server/api';
import { helpArticles } from '../lib/content';
export const dynamic = 'force-dynamic';
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  if (!indexingEnabled()) return [];
  const origin = configuredOrigin();
  const pages: MetadataRoute.Sitemap = [
    '/',
    '/features',
    '/download',
    '/updates',
    '/help',
    ...helpArticles.map((x) => `/help/${x.slug}`),
  ].map((path) => ({ url: `${origin}${path}` }));
  const first = await getUpdates('all', 1);
  if (!first.ok) return pages;
  const entries = [...first.data.items];
  for (
    let page = 2;
    page <= Math.min(84, Math.ceil(first.data.total / first.data.pageSize));
    page++
  ) {
    const result = await getUpdates('all', page);
    if (!result.ok) break;
    entries.push(...result.data.items);
  }
  return [
    ...pages,
    ...entries.map((x) => ({ url: `${origin}/updates/${x.slug}`, lastModified: x.publishedAt })),
  ];
}

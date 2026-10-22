import type { MetadataRoute } from 'next';
import { configuredOrigin, indexingEnabled } from '../lib/metadata';
export const dynamic = 'force-dynamic';
export default function robots(): MetadataRoute.Robots {
  return indexingEnabled()
    ? { rules: { userAgent: '*', allow: '/' }, sitemap: `${configuredOrigin()}/sitemap.xml` }
    : { rules: { userAgent: '*', disallow: '/' } };
}

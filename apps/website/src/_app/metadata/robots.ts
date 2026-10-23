import type { MetadataRoute } from 'next';
import { configuredOrigin, indexingEnabled } from '@/shared/lib/metadata';
export default function robots(): MetadataRoute.Robots {
  return indexingEnabled()
    ? { rules: { userAgent: '*', allow: '/' }, sitemap: `${configuredOrigin()}/sitemap.xml` }
    : { rules: { userAgent: '*', disallow: '/' } };
}

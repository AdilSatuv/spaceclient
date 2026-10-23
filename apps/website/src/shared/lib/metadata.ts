import type { Metadata } from 'next';

export function configuredOrigin(value = process.env.SPACE_SITE_URL): string | undefined {
  if (!value) return undefined;
  try {
    const url = new URL(value);
    if (
      url.protocol !== 'https:' ||
      url.username ||
      url.password ||
      url.port ||
      url.pathname !== '/' ||
      url.search ||
      url.hash ||
      url.hostname === 'localhost' ||
      /^[\d.:]+$/.test(url.hostname) ||
      !url.hostname.includes('.')
    )
      return undefined;
    return url.origin;
  } catch {
    return undefined;
  }
}
export function indexingEnabled() {
  return (
    process.env.NODE_ENV === 'production' &&
    process.env.SPACE_SITE_INDEXABLE === 'true' &&
    !!configuredOrigin()
  );
}
export function pageMetadata(
  path: string,
  title: string,
  description: string,
  noIndex = false,
): Metadata {
  const origin = configuredOrigin();
  return {
    title,
    description,
    robots: { index: indexingEnabled() && !noIndex, follow: indexingEnabled() && !noIndex },
    ...(origin
      ? {
          alternates: { canonical: `${origin}${path}` },
          openGraph: {
            type: 'website',
            locale: 'ru_RU',
            siteName: 'Space Client',
            title,
            description,
            url: `${origin}${path}`,
            images: [
              {
                url: `${origin}/images/space-hero-v2.png`,
                width: 1672,
                height: 941,
                alt: 'Космический блочный мир Space Client — авторская иллюстрация',
              },
            ],
          },
        }
      : {}),
  };
}

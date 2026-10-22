import { isFeatureList, isReleaseList, isUpdate, isUpdateList } from './contracts';
import { cache } from 'react';

type Result<T> =
  { ok: true; data: T } | { ok: false; reason: 'unavailable' | 'invalid' | 'missing' };
async function request<T>(
  path: string,
  validate: (value: unknown) => value is T,
): Promise<Result<T>> {
  try {
    const origin = process.env.SPACE_API_URL || 'http://127.0.0.1:3001';
    const response = await fetch(`${origin.replace(/\/$/, '')}/v1/${path}`, {
      cache: 'no-store',
      signal: AbortSignal.timeout(2500),
      redirect: 'error',
    });
    if (response.status === 404) return { ok: false, reason: 'missing' };
    if (!response.ok) return { ok: false, reason: 'unavailable' };
    const value: unknown = await response.json();
    return validate(value) ? { ok: true, data: value } : { ok: false, reason: 'invalid' };
  } catch {
    return { ok: false, reason: 'unavailable' };
  }
}
export const getFeatures = cache(() => request('features', isFeatureList));
export const getReleases = cache(() => request('releases', isReleaseList));
export const getUpdates = cache((type: string, page: number) =>
  request(`updates?type=${encodeURIComponent(type)}&page=${page}`, isUpdateList),
);
export const getUpdate = cache((slug: string) =>
  request(`updates/${encodeURIComponent(slug)}`, isUpdate),
);

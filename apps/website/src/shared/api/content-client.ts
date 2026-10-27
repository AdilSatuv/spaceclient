import {
  isFeatureList,
  isReleaseList,
  isUpdate,
  isUpdateList,
  isProjectStatus,
  validSlug,
  validUpdateType,
  validUpdatePage,
} from '@space/content-contracts';
import type { Update, UpdateList, UpdateQuery } from '@space/content-contracts';

export type ContentResult<T> =
  { ok: true; data: T } | { ok: false; reason: 'unavailable' | 'invalid' | 'missing' };
type Options = { origin: string; fetcher?: typeof fetch; timeoutMs?: number };
const MAX_RESPONSE_BYTES = 1024 * 1024;
class InvalidResponse extends Error {}

async function readJson(response: Response): Promise<unknown> {
  if (!/^application\/json(?:\s*;|$)/i.test(response.headers.get('content-type') ?? '')) {
    throw new InvalidResponse();
  }
  const declared = response.headers.get('content-length');
  if (declared !== null && (!/^\d+$/.test(declared) || Number(declared) > MAX_RESPONSE_BYTES)) {
    throw new InvalidResponse();
  }
  if (!response.body) throw new InvalidResponse();
  const reader = response.body.getReader();
  const chunks: Uint8Array[] = [];
  let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      size += value.byteLength;
      if (size > MAX_RESPONSE_BYTES) throw new InvalidResponse();
      chunks.push(value);
    }
  } finally {
    await reader.cancel().catch(() => {});
    reader.releaseLock();
  }
  const bytes = new Uint8Array(size);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }
  try {
    return JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(bytes));
  } catch {
    throw new InvalidResponse();
  }
}

// Pure transport adapter; the Next composition module marks its use as server-only.
export function createContentClient({ origin, fetcher = fetch, timeoutMs = 2500 }: Options) {
  async function request<T>(
    path: string,
    validate: (value: unknown) => value is T,
  ): Promise<ContentResult<T>> {
    let response: Response | undefined;
    try {
      const base = new URL(origin);
      if (
        !['http:', 'https:'].includes(base.protocol) ||
        base.username ||
        base.password ||
        base.pathname !== '/' ||
        base.search ||
        base.hash ||
        !Number.isInteger(timeoutMs) ||
        timeoutMs < 1 ||
        timeoutMs > 30000
      ) {
        return { ok: false, reason: 'unavailable' };
      }
      response = await fetcher(new URL(`/v1/${path}`, base).href, {
        cache: 'no-store',
        signal: AbortSignal.timeout(timeoutMs),
        // Workers supports manual/follow only. Reject 3xx via !response.ok below.
        redirect: 'manual',
        headers: { Accept: 'application/json' },
      });
      if (response.status === 404) return { ok: false, reason: 'missing' };
      if (!response.ok) return { ok: false, reason: 'unavailable' };
      const value = await readJson(response);
      return validate(value) ? { ok: true, data: value } : { ok: false, reason: 'invalid' };
    } catch (error) {
      return { ok: false, reason: error instanceof InvalidResponse ? 'invalid' : 'unavailable' };
    } finally {
      if (response?.body && !response.body.locked) await response.body.cancel().catch(() => {});
    }
  }
  return {
    getProject: () => request('project', isProjectStatus),
    getFeatures: () => request('features', isFeatureList),
    getReleases: () => request('releases', isReleaseList),
    getUpdates: (type: UpdateQuery['type'], page: number) => {
      if (!validUpdateType(type) || !validUpdatePage(String(page))) {
        return Promise.resolve({ ok: false, reason: 'invalid' } as const);
      }
      return request(
        `updates?type=${type}&page=${page}`,
        (value): value is UpdateList =>
          isUpdateList(value) &&
          value.page === page &&
          (type === 'all' || value.items.every((item) => item.type === type)),
      );
    },
    getUpdate: (slug: string) => {
      if (!validSlug(slug)) return Promise.resolve({ ok: false, reason: 'missing' } as const);
      return request(
        `updates/${slug}`,
        (value): value is Update => isUpdate(value) && value.slug === slug,
      );
    },
  };
}

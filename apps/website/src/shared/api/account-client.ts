import 'server-only';
import { createHash } from 'node:crypto';
import { MAX_IMAGE_BYTES } from '@space/account-contracts';

export class AccountRequestError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
  }
}
const messages: Record<string, string> = {
  SPACE_INVALID_INPUT: 'Проверьте поля формы.',
  SPACE_INVALID_CREDENTIALS: 'Неверное имя аккаунта или пароль.',
  SPACE_UNAUTHORIZED: 'Войдите в аккаунт Space заново.',
  SPACE_CONFLICT: 'Имя аккаунта или ник уже заняты.',
  SPACE_RATE_LIMITED: 'Слишком много запросов. Подождите и повторите.',
  SPACE_INVALID_IMAGE: 'Нужен корректный PNG допустимого размера и формата.',
  SPACE_UNAVAILABLE: 'Сервис аккаунтов временно недоступен.',
  SPACE_REVISION_CONFLICT:
    'Профиль изменился в другом клиенте. Обновите профиль и повторите изменение.',
};
export const accountFailure = () =>
  new AccountRequestError(503, 'SPACE_UNAVAILABLE', messages.SPACE_UNAVAILABLE);
export async function readAccountBody(request: Request | Response, limit: number) {
  const length = request.headers.get('content-length');
  if (length && (!/^\d+$/.test(length) || Number(length) > limit))
    throw new AccountRequestError(413, 'SPACE_INVALID_INPUT', 'Файл или запрос слишком большой.');
  if (!request.body) return new Uint8Array();
  const reader = request.body.getReader();
  let timer: ReturnType<typeof setTimeout> | undefined;
  const expired = new Promise<never>((_, reject) => {
    timer = setTimeout(() => reject(accountFailure()), 5000);
  });
  try {
    const chunks: Uint8Array[] = [];
    let size = 0;
    for (;;) {
      const { done, value } = await Promise.race([reader.read(), expired]);
      if (done) break;
      size += value.byteLength;
      if (size > limit)
        throw new AccountRequestError(
          413,
          'SPACE_INVALID_INPUT',
          'Файл или запрос слишком большой.',
        );
      chunks.push(value);
    }
    const bytes = new Uint8Array(size);
    let offset = 0;
    for (const chunk of chunks) {
      bytes.set(chunk, offset);
      offset += chunk.length;
    }
    return bytes;
  } finally {
    clearTimeout(timer);
    void reader.cancel().catch(() => {});
  }
}
function apiOrigin() {
  const raw =
    process.env.SPACE_ACCOUNT_API_URL || process.env.SPACE_API_URL || 'http://127.0.0.1:3001';
  const url = new URL(raw);
  if (
    !['http:', 'https:'].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.pathname !== '/' ||
    url.search ||
    url.hash ||
    (url.protocol === 'http:' && !['127.0.0.1', 'localhost', '[::1]'].includes(url.hostname))
  )
    throw accountFailure();
  return url.origin;
}
export async function accountUpstream(
  path: string,
  method: string,
  body?: unknown,
  token?: string,
) {
  if (!/^(register|login|logout|me(?:\/(?:skin|cape))?|assets\/[a-f0-9]{64})$/.test(path))
    throw accountFailure();
  try {
    const response = await fetch(`${apiOrigin()}/v1/accounts/${path}`, {
      method,
      headers: {
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
      cache: 'no-store',
      redirect: 'manual',
      signal: AbortSignal.timeout(7000),
    });
    const bytes = await readAccountBody(
      response,
      path.startsWith('assets/') ? MAX_IMAGE_BYTES : 16384,
    );
    if (!response.ok) {
      let code = 'SPACE_UNAVAILABLE';
      try {
        const json = JSON.parse(new TextDecoder().decode(bytes));
        if (Object.hasOwn(messages, json.code)) code = json.code;
      } catch {}
      throw new AccountRequestError(
        response.status >= 400 && response.status < 500 ? response.status : 503,
        code,
        messages[code],
      );
    }
    if (path.startsWith('assets/')) {
      if (
        response.headers.get('content-type') !== 'image/png' ||
        createHash('sha256').update(bytes).digest('hex') !== path.slice(7)
      )
        throw accountFailure();
      return bytes;
    }
    if (!response.headers.get('content-type')?.includes('application/json')) throw accountFailure();
    return JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(bytes)) as unknown;
  } catch (error) {
    if (error instanceof AccountRequestError) throw error;
    throw accountFailure();
  }
}

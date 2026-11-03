import { UserFacingError } from '../application/user-facing-error.ts';

export const DEFAULT_ACCOUNT_API = 'http://127.0.0.1:3001';
export function accountApiBase(value = DEFAULT_ACCOUNT_API) {
  try {
    const url = new URL(value);
    if (
      url.username ||
      url.password ||
      url.search ||
      url.hash ||
      url.pathname !== '/' ||
      (url.protocol !== 'https:' &&
        !(url.protocol === 'http:' && ['127.0.0.1', '[::1]', 'localhost'].includes(url.hostname)))
    )
      throw 0;
    return url.origin;
  } catch {
    throw new UserFacingError(
      'Источник Space API настроен неверно. Используйте HTTPS или локальный loopback.',
    );
  }
}
const errors: Record<string, string> = {
  SPACE_INVALID_INPUT: 'Проверьте поля формы.',
  SPACE_INVALID_CREDENTIALS: 'Неверное имя Space или пароль.',
  SPACE_UNAUTHORIZED: 'Сессия Space истекла. Войдите снова.',
  SPACE_CONFLICT: 'Имя Space или игровой ник уже заняты.',
  SPACE_RATE_LIMITED: 'Слишком много попыток. Подождите и повторите.',
  SPACE_INVALID_IMAGE: 'Изображение не прошло проверку PNG.',
  SPACE_UNAVAILABLE: 'Space API недоступен. Проверьте локальный сервис и повторите.',
  SPACE_REVISION_CONFLICT: 'Профиль изменён в другом клиенте. Обновите его перед повтором.',
};
export class AccountHttpError extends UserFacingError {
  readonly code: string;
  constructor(code: string) {
    super(errors[code] ?? errors.SPACE_UNAVAILABLE!);
    this.code = code;
  }
}
export class AccountHttp {
  readonly base: string;
  private readonly fetcher: typeof fetch;
  constructor(base: string, fetcher: typeof fetch = fetch) {
    this.base = accountApiBase(base);
    this.fetcher = fetcher;
  }
  async request(
    path: string,
    method = 'GET',
    body?: unknown,
    token?: string,
    image = false,
  ): Promise<unknown> {
    if (
      !/^\/v1\/accounts\/(register|login|logout|me(?:\/(?:skin|cape))?|assets\/[a-f0-9]{64})$/.test(
        path,
      )
    )
      throw new AccountHttpError('SPACE_INVALID_INPUT');
    const signal = AbortSignal.timeout(10000);
    let response: Response;
    try {
      response = await this.fetcher(this.base + path, {
        method,
        redirect: 'error',
        cache: 'no-store',
        signal,
        headers: {
          Accept: image ? 'image/png' : 'application/json',
          ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
          ...(token ? { Authorization: 'Bearer ' + token } : {}),
        },
        ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      });
      const limit = image ? 256 * 1024 : 32 * 1024;
      if (Number(response.headers.get('content-length')) > limit || !response.body) throw 0;
      const reader = response.body.getReader();
      const chunks: Uint8Array[] = [];
      let length = 0;
      try {
        while (true) {
          const part = await reader.read();
          if (part.done) break;
          length += part.value.length;
          if (length > limit) throw 0;
          chunks.push(part.value);
        }
      } finally {
        await reader.cancel().catch(() => {});
      }
      const bytes = Buffer.concat(chunks);
      if (!response.ok) {
        let code = 'SPACE_UNAVAILABLE';
        try {
          const value = JSON.parse(bytes.toString('utf8'));
          if (Object.hasOwn(errors, value.code)) code = value.code;
        } catch {}
        throw new AccountHttpError(code);
      }
      if (image) {
        if (response.headers.get('content-type')?.split(';')[0] !== 'image/png') throw 0;
        return bytes;
      }
      if (!response.headers.get('content-type')?.startsWith('application/json')) throw 0;
      return JSON.parse(bytes.toString('utf8'));
    } catch (error) {
      if (error instanceof AccountHttpError) throw error;
      throw new AccountHttpError('SPACE_UNAVAILABLE');
    }
  }
}

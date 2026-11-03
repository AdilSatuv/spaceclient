import { readSpaceAccountProfile } from '@space/account-contracts';
import type { SpaceAccountProfile } from '@space/account-contracts';
export type AccountAction =
  | 'session'
  | 'register'
  | 'login'
  | 'logout'
  | 'rename'
  | 'skin'
  | 'cape'
  | 'deleteSkin'
  | 'deleteCape';
const routes: Record<AccountAction, [string, string]> = {
  session: ['session', 'GET'],
  register: ['register', 'POST'],
  login: ['login', 'POST'],
  logout: ['logout', 'POST'],
  rename: ['me', 'PATCH'],
  skin: ['me/skin', 'PUT'],
  cape: ['me/cape', 'PUT'],
  deleteSkin: ['me/skin', 'DELETE'],
  deleteCape: ['me/cape', 'DELETE'],
};
export class AccountBrowserError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly localLogout = false,
  ) {
    super(message);
  }
}
export async function requestAccount(
  action: AccountAction,
  body?: unknown,
  csrf?: string,
): Promise<{ profile?: SpaceAccountProfile | null; csrfToken?: string; ok?: boolean }> {
  const [path, method] = routes[action];
  let response: Response;
  try {
    response = await fetch(`/api/account/${path}`, {
      method,
      headers:
        method === 'GET' ? {} : { 'Content-Type': 'application/json', 'X-Space-CSRF': csrf ?? '' },
      body: method === 'GET' ? undefined : JSON.stringify(body ?? {}),
      credentials: 'same-origin',
      cache: 'no-store',
      signal: AbortSignal.timeout(10000),
    });
  } catch {
    throw new AccountBrowserError(
      'SPACE_UNAVAILABLE',
      'Не удалось связаться с сервисом. Попробуйте ещё раз.',
    );
  }
  let data;
  try {
    data = await response.json();
  } catch {
    throw new AccountBrowserError('SPACE_UNAVAILABLE', 'Сервис вернул некорректный ответ.');
  }
  if (!response.ok)
    throw new AccountBrowserError(
      typeof data.code === 'string' ? data.code : 'SPACE_UNAVAILABLE',
      typeof data.message === 'string' ? data.message : 'Не удалось выполнить действие.',
      action === 'logout' && data.localLogout === true,
    );
  return {
    ...(Object.hasOwn(data, 'profile')
      ? { profile: data.profile === null ? null : readSpaceAccountProfile(data.profile) }
      : {}),
    ...(typeof data.csrfToken === 'string' && /^[A-Za-z0-9_-]{43}$/.test(data.csrfToken)
      ? { csrfToken: data.csrfToken }
      : {}),
    ...(data.ok === true ? { ok: true } : {}),
  };
}
export const accountAssetUrl = (hash: string) =>
  /^[a-f0-9]{64}$/.test(hash) ? `/api/account/assets/${hash}` : '';

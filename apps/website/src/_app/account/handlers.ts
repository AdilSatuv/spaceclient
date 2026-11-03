import 'server-only';
import { randomBytes, timingSafeEqual } from 'node:crypto';
import { NextResponse } from 'next/server';
import { readSpaceAccountProfile, readSpaceAccountSession } from '@space/account-contracts';
import {
  accountUpstream,
  AccountRequestError,
  accountFailure,
  readAccountBody,
} from '@/shared/api/index.server';

const sessionName = 'space_session',
  csrfName = 'space_csrf';
type Context = { params: Promise<{ path: string[] }> };
function cookie(request: Request, name: string) {
  const values = (request.headers.get('cookie') ?? '')
    .split(';')
    .map((x) => x.trim())
    .filter((x) => x.startsWith(name + '='));
  if (values.length !== 1) return '';
  const value = values[0].slice(name.length + 1);
  return /^[A-Za-z0-9_-]{43}$/.test(value) ? value : '';
}
function secure(request: Request) {
  const url = new URL(request.url);
  if (url.protocol === 'https:') return true;
  if (url.protocol === 'http:' && ['127.0.0.1', 'localhost', '[::1]'].includes(url.hostname))
    return false;
  throw accountFailure();
}
function json(body: unknown, status = 200) {
  const response = NextResponse.json(body, { status });
  response.headers.set('Cache-Control', 'no-store');
  response.headers.set('X-Content-Type-Options', 'nosniff');
  response.headers.set('Vary', 'Cookie');
  return response;
}
function csrf(request: Request) {
  const expected = cookie(request, csrfName),
    provided = request.headers.get('x-space-csrf') ?? '';
  if (
    request.headers.get('origin') !==
      `${new URL(request.url).protocol}//${request.headers.get('host')}` ||
    request.headers.get('sec-fetch-site') === 'cross-site' ||
    !expected ||
    !/^[A-Za-z0-9_-]{43}$/.test(provided) ||
    !timingSafeEqual(Buffer.from(expected), Buffer.from(provided))
  )
    throw new AccountRequestError(
      403,
      'SPACE_INVALID_INPUT',
      'Запрос отклонён. Обновите страницу и повторите.',
    );
  secure(request);
}
async function handle(request: Request, context: Context) {
  try {
    const { path } = await context.params;
    const route = path.join('/'),
      method = request.method,
      token = cookie(request, sessionName);
    if (new URL(request.url).search)
      throw new AccountRequestError(400, 'SPACE_INVALID_INPUT', 'Некорректные параметры запроса.');
    if (method === 'GET' && route === 'session') {
      let profile = null,
        expired = false;
      if (token) {
        try {
          const data = (await accountUpstream('me', 'GET', undefined, token)) as {
            profile: unknown;
          };
          profile = readSpaceAccountProfile(data.profile);
        } catch (error) {
          if (error instanceof AccountRequestError && error.status === 401) expired = true;
          else throw error;
        }
      }
      const csrfToken = cookie(request, csrfName) || randomBytes(32).toString('base64url'),
        response = json({ profile, csrfToken });
      response.cookies.set(csrfName, csrfToken, {
        httpOnly: true,
        sameSite: 'strict',
        secure: secure(request),
        path: '/',
        maxAge: 3600,
      });
      if (expired)
        response.cookies.set(sessionName, '', {
          httpOnly: true,
          sameSite: 'strict',
          secure: secure(request),
          path: '/',
          maxAge: 0,
        });
      return response;
    }
    if (method === 'GET' && /^assets\/[a-f0-9]{64}$/.test(route)) {
      const bytes = (await accountUpstream(route, 'GET')) as Uint8Array;
      return new NextResponse(Buffer.from(bytes), {
        headers: {
          'Content-Type': 'image/png',
          'X-Content-Type-Options': 'nosniff',
          'Cache-Control': 'public, max-age=31536000, immutable',
          'Content-Security-Policy': "default-src 'none'; sandbox",
        },
      });
    }
    if (!(
      (['register', 'login', 'logout'].includes(route) && method === 'POST') ||
      (route === 'me' && method === 'PATCH') ||
      (['me/skin', 'me/cape'].includes(route) && ['PUT', 'DELETE'].includes(method))
    ))
      return json({ code: 'SPACE_INVALID_INPUT', message: 'Маршрут не найден.' }, 404);
    csrf(request);
    if (!/^application\/json(?:\s*;.*)?$/i.test(request.headers.get('content-type') ?? ''))
      throw new AccountRequestError(415, 'SPACE_INVALID_INPUT', 'Нужен JSON-запрос.');
    let body: unknown;
    try {
      const bytes = await readAccountBody(request, 360 * 1024);
      body = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(bytes));
    } catch (error) {
      if (error instanceof AccountRequestError) throw error;
      throw new AccountRequestError(400, 'SPACE_INVALID_INPUT', 'Некорректный запрос.');
    }
    if (route === 'logout') {
      if (!body || typeof body !== 'object' || Array.isArray(body) || Object.keys(body).length)
        throw new AccountRequestError(400, 'SPACE_INVALID_INPUT', 'Некорректный запрос выхода.');
      let response: NextResponse;
      try {
        const result = (await accountUpstream(route, method, body, token || undefined)) as {
          ok?: boolean;
        };
        if (result.ok !== true) throw accountFailure();
        response = json({ ok: true });
      } catch (error) {
        const safe = error instanceof AccountRequestError ? error : accountFailure();
        const message =
          safe.status === 401
            ? 'Вы вышли на этом сайте. Серверная сессия уже недействительна.'
            : 'Вы вышли на этом сайте. Сервер не подтвердил отзыв: его сессия может действовать до истечения срока. ' +
              safe.message;
        response = json({ code: safe.code, message, localLogout: true }, safe.status);
      }
      response.cookies.set(sessionName, '', {
        httpOnly: true,
        sameSite: 'strict',
        secure: secure(request),
        path: '/',
        maxAge: 0,
      });
      return response;
    }
    const result = await accountUpstream(route, method, body, token || undefined);
    if (route === 'register' || route === 'login') {
      const session = readSpaceAccountSession(result),
        response = json({ profile: session.profile });
      response.cookies.set(sessionName, session.sessionToken, {
        httpOnly: true,
        sameSite: 'strict',
        secure: secure(request),
        path: '/',
        expires: new Date(session.expiresAt),
      });
      return response;
    }
    return json({ profile: readSpaceAccountProfile((result as { profile: unknown }).profile) });
  } catch (error) {
    const safe = error instanceof AccountRequestError ? error : accountFailure();
    return json({ code: safe.code, message: safe.message }, safe.status);
  }
}
export const GET = handle,
  POST = handle,
  PATCH = handle,
  PUT = handle,
  DELETE = handle;

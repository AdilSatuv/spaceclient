import { createServer } from 'node:http';
import type { IncomingMessage, ServerResponse } from 'node:http';
import { randomBytes, timingSafeEqual } from 'node:crypto';

const MAX_BODY = 65536;
const MAX_WAIT_MS = 180000;
const MAX_IO_MS = 5000;
const responseFields = new Set([
  'code',
  'state',
  'client_info',
  'session_state',
  'error',
  'error_description',
  'error_uri',
  'suberror',
  'error_codes',
  'timestamp',
  'trace_id',
  'correlation_id',
]);

export class AuthCallbackError extends Error {
  readonly code: 'cancelled' | 'timeout' | 'unavailable' | 'denied';
  constructor(code: AuthCallbackError['code']) {
    super(
      {
        cancelled: 'Вход отменён.',
        timeout: 'Время ожидания входа истекло. Попробуйте снова.',
        unavailable: 'Не удалось открыть локальный обработчик входа.',
        denied: 'Microsoft не подтвердил вход или разрешение. Попробуйте снова.',
      }[code],
    );
    this.name = 'AuthCallbackError';
    this.code = code;
  }
}

export interface MicrosoftCodeResponse {
  code: string;
  clientInfo?: string;
}

export interface AuthCallbackSession {
  readonly redirectUri: string;
  readonly state: string;
  readonly response: Promise<MicrosoftCodeResponse>;
  close(): void;
}

function reply(response: ServerResponse, status: number): void {
  if (response.destroyed || response.headersSent) return;
  response.writeHead(status, {
    'Content-Type': 'text/html; charset=utf-8',
    'Cache-Control': 'no-store',
    'Content-Security-Policy':
      "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'",
    'X-Content-Type-Options': 'nosniff',
    'Referrer-Policy': 'no-referrer',
    Connection: 'close',
  });
  response.end(
    '<!doctype html><html lang="ru"><meta charset="utf-8"><title>Space Client</title><body>Вернитесь в Space Client, чтобы проверить результат входа.</body></html>',
  );
}

function matchesState(actual: string | null, expected: string): boolean {
  return (
    typeof actual === 'string' &&
    /^[\w-]{43}$/.test(actual) &&
    timingSafeEqual(Buffer.from(actual, 'ascii'), Buffer.from(expected, 'ascii'))
  );
}

async function readForm(request: IncomingMessage): Promise<URLSearchParams> {
  const buffers: Buffer[] = [];
  let size = 0;
  for await (const chunk of request) {
    const bytes = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
    size += bytes.length;
    if (size > MAX_BODY) throw new Error('Body limit');
    buffers.push(bytes);
  }
  return new URLSearchParams(Buffer.concat(buffers, size).toString('utf8'));
}

// Main only. No account calls, browser opening or token exchange occur here.
export async function listenForMicrosoftCode(
  options: {
    signal?: AbortSignal;
    timeoutMs?: number;
  } = {},
): Promise<AuthCallbackSession> {
  const timeoutMs = options.timeoutMs ?? MAX_WAIT_MS;
  if (!Number.isInteger(timeoutMs) || timeoutMs < 50 || timeoutMs > MAX_WAIT_MS)
    throw new AuthCallbackError('unavailable');
  if (options.signal?.aborted) throw new AuthCallbackError('cancelled');
  const state = randomBytes(32).toString('base64url');
  let resolveResponse!: (value: MicrosoftCodeResponse) => void;
  let rejectResponse!: (error: AuthCallbackError) => void;
  const response = new Promise<MicrosoftCodeResponse>((resolve, reject) => {
    resolveResponse = resolve;
    rejectResponse = reject;
  });
  // A cancellation can arrive while the caller is still awaiting listen().
  void response.catch(() => {});
  let settled = false;
  let expectedHost = '';
  let deadline: ReturnType<typeof setTimeout> | undefined;
  const server = createServer(
    { maxHeaderSize: 8192, connectionsCheckingInterval: 500 },
    (request, result) => {
      void receive(request, result).catch(() => reply(result, 400));
    },
  );
  server.maxConnections = 8;
  server.maxRequestsPerSocket = 1;
  server.headersTimeout = MAX_IO_MS;
  server.requestTimeout = MAX_IO_MS;
  server.keepAliveTimeout = 1000;
  server.on('connection', (socket) => socket.setTimeout(MAX_IO_MS, () => socket.destroy()));

  function finish(value: MicrosoftCodeResponse | AuthCallbackError, flushPage = false): void {
    if (settled) return;
    settled = true;
    clearTimeout(deadline);
    options.signal?.removeEventListener('abort', abort);
    server.close();
    if (value instanceof AuthCallbackError) {
      if (!flushPage) server.closeAllConnections();
      rejectResponse(value);
    } else resolveResponse(value);
  }
  function abort(): void {
    finish(new AuthCallbackError('cancelled'));
  }

  async function receive(request: IncomingMessage, result: ServerResponse): Promise<void> {
    if (settled) {
      reply(result, 410);
      return;
    }
    if (
      request.socket.remoteAddress !== '127.0.0.1' ||
      request.headers.host?.toLowerCase() !== expectedHost ||
      request.url !== '/'
    ) {
      reply(result, 400);
      return;
    }
    if (request.method !== 'POST') {
      reply(result, 405);
      return;
    }
    if (
      request.headers['content-type']?.split(';')[0].trim().toLowerCase() !==
        'application/x-www-form-urlencoded' ||
      request.headers['content-encoding']
    ) {
      reply(result, 415);
      return;
    }
    const length = request.headers['content-length'];
    if (length && (!/^\d+$/.test(length) || Number(length) > MAX_BODY)) {
      reply(result, 413);
      return;
    }
    const bodyTimer = setTimeout(() => {
      reply(result, 408);
      request.destroy();
    }, MAX_IO_MS);
    let form: URLSearchParams;
    try {
      form = await readForm(request);
    } catch {
      reply(result, 413);
      return;
    } finally {
      clearTimeout(bodyTimer);
    }
    if (settled) {
      reply(result, 410);
      return;
    }
    const names = [...form.keys()];
    if (
      names.length > responseFields.size ||
      new Set(names).size !== names.length ||
      names.some((name) => !responseFields.has(name)) ||
      !matchesState(form.get('state'), state)
    ) {
      reply(result, 400);
      return;
    }
    const code = form.get('code');
    const error = form.get('error');
    if ((code === null) === (error === null)) {
      reply(result, 400);
      return;
    }
    if (error !== null) {
      if (!/^[a-zA-Z0-9_]{1,100}$/.test(error)) {
        reply(result, 400);
        return;
      }
      result.once('finish', () => server.closeAllConnections());
      result.once('close', () => server.closeAllConnections());
      reply(result, 200);
      finish(new AuthCallbackError('denied'), true);
      return;
    }
    const clientInfo = form.get('client_info');
    if (
      !code ||
      code.length > 16384 ||
      !/^[\x21-\x7e]+$/.test(code) ||
      (clientInfo !== null && (clientInfo.length > 8192 || !/^[\w-]+$/.test(clientInfo)))
    ) {
      reply(result, 400);
      return;
    }
    result.once('finish', () => server.closeAllConnections());
    result.once('close', () => server.closeAllConnections());
    reply(result, 200);
    finish({ code, ...(clientInfo ? { clientInfo } : {}) });
  }

  try {
    await new Promise<void>((resolve, reject) => {
      const startupError = () => reject(new AuthCallbackError('unavailable'));
      server.once('error', startupError);
      server.listen(0, '127.0.0.1', () => {
        server.removeListener('error', startupError);
        resolve();
      });
    });
    server.on('error', () => finish(new AuthCallbackError('unavailable')));
    const address = server.address();
    if (!address || typeof address === 'string') throw new AuthCallbackError('unavailable');
    expectedHost = `localhost:${address.port}`;
    options.signal?.addEventListener('abort', abort, { once: true });
    deadline = setTimeout(() => finish(new AuthCallbackError('timeout')), timeoutMs);
    if (options.signal?.aborted) abort();
    return { redirectUri: `http://${expectedHost}`, state, response, close: abort };
  } catch {
    finish(new AuthCallbackError('unavailable'));
    throw new AuthCallbackError('unavailable');
  }
}

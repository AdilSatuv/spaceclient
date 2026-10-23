const { test } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const net = require('node:net');
const marker = 'FAKE-AUTHORIZATION-CODE-FOR-LOCAL-TEST';

async function session(t, options = {}) {
  const { listenForMicrosoftCode } = await import('../src/main/auth-callback.ts');
  const value = await listenForMicrosoftCode(options);
  t.after(() => value.close());
  return value;
}
function send(value, fields, change = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(value.redirectUri);
    const body = typeof fields === 'string' ? fields : new URLSearchParams(fields).toString();
    const request = http.request(
      {
        hostname: '127.0.0.1',
        port: url.port,
        method: change.method ?? 'POST',
        path: change.path ?? '/',
        headers: {
          Host: url.host,
          'Content-Type': 'application/x-www-form-urlencoded',
          'Content-Length': Buffer.byteLength(body),
          ...change.headers,
        },
      },
      (result) => {
        const chunks = [];
        result.on('data', (chunk) => chunks.push(chunk));
        result.on('end', () =>
          resolve({
            status: result.statusCode,
            body: Buffer.concat(chunks).toString(),
            headers: result.headers,
          }),
        );
        result.on('error', reject);
      },
    );
    request.on('error', reject);
    request.end(body);
  });
}

test('local callback accepts one form_post with random state and never echoes code or claims', async (t) => {
  const value = await session(t);
  const other = await session(t);
  assert.match(value.redirectUri, /^http:\/\/localhost:\d+$/);
  assert.match(value.state, /^[\w-]{43}$/);
  assert.notEqual(value.state, other.state);
  const delivered = await send(value, { code: marker, state: value.state, client_info: 'ZmFrZQ' });
  assert.equal(delivered.status, 200);
  assert.equal(delivered.headers['cache-control'], 'no-store');
  assert.match(delivered.headers['content-security-policy'], /default-src 'none'/);
  assert.equal(delivered.headers['referrer-policy'], 'no-referrer');
  for (const secret of [marker, value.state, 'ZmFrZQ'])
    assert.equal(delivered.body.includes(secret), false);
  assert.deepEqual(await value.response, { code: marker, clientInfo: 'ZmFrZQ' });
  await assert.rejects(send(value, { code: marker, state: value.state })); // Listener is closed, not reusable.
  const race = await session(t);
  const answers = await Promise.allSettled([
    send(race, { code: marker, state: race.state }),
    send(race, { code: 'other-fake-code', state: race.state }),
  ]);
  assert.equal(
    answers.filter((answer) => answer.status === 'fulfilled' && answer.value.status === 200).length,
    1,
  );
  assert.ok([marker, 'other-fake-code'].includes((await race.response).code));
});

test('wrong, absent and duplicated state or parameters cannot consume the pending session', async (t) => {
  const value = await session(t);
  for (const fields of [
    { code: marker },
    { code: marker, state: 'A'.repeat(43) },
    `code=${marker}&state=${value.state}&state=${value.state}`,
    `code=${marker}&code=other&state=${value.state}`,
    { code: marker, state: value.state, redirect_uri: 'https://example.invalid' },
    { code: marker, error: 'access_denied', state: value.state },
    { state: value.state },
    { code: '', state: value.state },
    { code: 'line\nbreak', state: value.state },
    { code: marker, state: value.state, client_info: 'bad+value' },
  ])
    assert.equal((await send(value, fields)).status, 400);
  assert.equal((await send(value, { code: marker, state: value.state })).status, 200);
  assert.equal((await value.response).code, marker);
});

test('foreign Host, query/path, method, media type and encodings are rejected before OAuth parsing', async (t) => {
  const value = await session(t);
  const fields = { code: marker, state: value.state };
  const cases = [
    [{ headers: { Host: 'example.invalid' } }, 400],
    [{ path: '/callback' }, 400],
    [{ path: '/?code=' + marker }, 400],
    [{ method: 'GET' }, 405],
    [{ method: 'PUT' }, 405],
    [{ method: 'OPTIONS' }, 405],
    [{ headers: { 'Content-Type': 'application/json' } }, 415],
    [{ headers: { 'Content-Encoding': 'gzip' } }, 415],
  ];
  for (const [change, status] of cases)
    assert.equal((await send(value, fields, change)).status, status);
  assert.equal((await send(value, fields)).status, 200);
  await value.response;
});

test('oversized declared body and code fail without closing the valid session', async (t) => {
  const value = await session(t);
  assert.equal((await send(value, { state: value.state, code: 'x'.repeat(65536) })).status, 413);
  assert.equal((await send(value, { state: value.state, code: 'x'.repeat(16385) })).status, 400);
  assert.equal((await send(value, { state: value.state, code: marker })).status, 200);
  await value.response;
});

test('a chunked body cannot bypass the byte limit or consume a valid state', async (t) => {
  const value = await session(t);
  const url = new URL(value.redirectUri);
  await new Promise((resolve) => {
    const request = http.request(
      {
        hostname: '127.0.0.1',
        port: url.port,
        method: 'POST',
        path: '/',
        headers: {
          Host: url.host,
          'Content-Type': 'application/x-www-form-urlencoded',
          'Transfer-Encoding': 'chunked',
        },
      },
      (result) => {
        result.resume();
        result.on('end', resolve);
      },
    );
    request.on('error', resolve); // Over-limit streams may be terminated without a page.
    request.end('code=' + 'x'.repeat(65537));
  });
  assert.equal((await send(value, { state: value.state, code: marker })).status, 200);
  await value.response;
});

test('provider refusal consumes valid state but never exposes provider details', async (t) => {
  const value = await session(t);
  const response = await send(value, {
    state: value.state,
    error: 'access_denied',
    error_description: marker,
  });
  assert.equal(response.status, 200);
  assert.equal(response.body.includes(marker), false);
  await assert.rejects(
    value.response,
    (error) => error.code === 'denied' && !String(error).includes(marker),
  );
  await assert.rejects(send(value, { state: value.state, code: marker }));
});

test('cancel and timeout close active sockets and reject without logging request data', async (t) => {
  const controller = new AbortController();
  const value = await session(t, { signal: controller.signal });
  const url = new URL(value.redirectUri);
  const socket = net.connect({ host: '127.0.0.1', port: Number(url.port) });
  socket.on('error', () => {});
  const closed = new Promise((resolve) => socket.once('close', resolve));
  await new Promise((resolve) => socket.once('connect', resolve));
  socket.write(
    `POST / HTTP/1.1\r\nHost: ${url.host}\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: 500\r\n\r\ncode=${marker}`,
  );
  controller.abort();
  await assert.rejects(value.response, { code: 'cancelled' });
  await closed;
  await assert.rejects(send(value, { state: value.state, code: marker }));
  const timeout = await session(t, { timeoutMs: 75 });
  await assert.rejects(timeout.response, { code: 'timeout' });
  await assert.rejects(send(timeout, { state: timeout.state, code: marker }));
});

test('already cancelled and invalid timeout settings never create a listener', async () => {
  const { listenForMicrosoftCode } = await import('../src/main/auth-callback.ts');
  await assert.rejects(listenForMicrosoftCode({ signal: AbortSignal.abort() }), {
    code: 'cancelled',
  });
  for (const timeoutMs of [-1, 0, 49, 180001, NaN, Infinity]) {
    await assert.rejects(listenForMicrosoftCode({ timeoutMs }), { code: 'unavailable' });
  }
});

test(
  'a stalled form body is disconnected and the session can still receive a valid response',
  { timeout: 10000 },
  async (t) => {
    const value = await session(t, { timeoutMs: 9000 });
    const url = new URL(value.redirectUri);
    const socket = net.connect({ host: '127.0.0.1', port: Number(url.port) });
    socket.on('error', () => {});
    socket.resume();
    const closed = new Promise((resolve) => socket.once('close', resolve));
    await new Promise((resolve) => socket.once('connect', resolve));
    socket.write(
      `POST / HTTP/1.1\r\nHost: ${url.host}\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: 500\r\n\r\nstate=${value.state}`,
    );
    await closed;
    assert.equal((await send(value, { state: value.state, code: marker })).status, 200);
    await value.response;
  },
);

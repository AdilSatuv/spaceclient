const { test } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { createContentClient } = require('../apps/website/lib/server/content-client.ts');
const { updatesQuery } = require('../apps/website/lib/queries.ts');
const json = (value, init) => Response.json(value, init);
const note = {
  id: 'test',
  slug: 'test',
  title: 'Test',
  summary: 'Fixture only',
  type: 'development',
  publishedAt: '2026-10-24T00:00:00.000Z',
  blocks: [{ heading: 'Test', text: 'Test' }],
};
const client = (fetcher) => createContentClient({ origin: 'http://127.0.0.1:3001/', fetcher });

test('content client uses fixed v1 routes and bounded uncached requests without redirects', async () => {
  const calls = [];
  const api = client(async (url, options) => {
    calls.push({ url, options });
    return json({ items: [], total: 0 });
  });
  assert.deepEqual(await api.getReleases(), { ok: true, data: { items: [], total: 0 } });
  assert.equal(calls[0].url, 'http://127.0.0.1:3001/v1/releases');
  assert.equal(calls[0].options.cache, 'no-store');
  assert.equal(calls[0].options.redirect, 'error');
  assert.equal(calls[0].options.signal instanceof AbortSignal, true);
  assert.equal(calls[0].options.headers.Accept, 'application/json');
  assert.equal(calls.length, 1);
});

test('content client distinguishes missing, invalid and unavailable without exposing raw errors', async () => {
  const cases = [
    [() => new Response('', { status: 404 }), 'missing'],
    [() => new Response('private backend failure', { status: 500 }), 'unavailable'],
    [
      () => new Response('bad json', { headers: { 'content-type': 'application/json' } }),
      'invalid',
    ],
    [() => json({ releases: [] }), 'invalid'],
    [() => new Response('{}', { headers: { 'content-type': 'text/html' } }), 'invalid'],
    [
      () => {
        throw new Error('token=secret');
      },
      'unavailable',
    ],
  ];
  for (const [response, reason] of cases) {
    const result = await client(async () => response()).getReleases();
    assert.deepEqual(result, { ok: false, reason });
    assert.doesNotMatch(JSON.stringify(result), /secret|private/);
  }
});

test('content client bounds both declared and streamed response bytes and cancels the body', async () => {
  assert.deepEqual(
    await client(
      async () =>
        new Response('{}', {
          headers: {
            'content-type': 'application/json',
            'content-length': String(1024 * 1024 + 1),
          },
        }),
    ).getReleases(),
    { ok: false, reason: 'invalid' },
  );
  let cancelled = false;
  const stream = new ReadableStream({
    pull(controller) {
      controller.enqueue(new Uint8Array(256 * 1024));
    },
    cancel() {
      cancelled = true;
    },
  });
  assert.deepEqual(
    await client(
      async () =>
        new Response(stream, {
          headers: { 'content-type': 'application/json' },
        }),
    ).getReleases(),
    { ok: false, reason: 'invalid' },
  );
  assert.equal(cancelled, true);
});

test('invalid origin, slug and pagination never reach the transport', async () => {
  let calls = 0;
  const fetcher = async () => {
    calls++;
    return json({});
  };
  for (const origin of [
    'bad',
    'file:///private',
    'http://name:password@localhost',
    'http://localhost/base',
    'http://localhost/?key=secret',
  ]) {
    assert.deepEqual(await createContentClient({ origin, fetcher }).getFeatures(), {
      ok: false,
      reason: 'unavailable',
    });
  }
  const api = client(fetcher);
  assert.deepEqual(await api.getUpdate('../project'), { ok: false, reason: 'missing' });
  assert.deepEqual(await api.getUpdates('bogus', 1), { ok: false, reason: 'invalid' });
  assert.deepEqual(await api.getUpdates('all', 0), { ok: false, reason: 'invalid' });
  assert.equal(calls, 0);
});

test('content client rejects an article or pagination belonging to a different request', async () => {
  assert.equal((await client(async () => json(note)).getUpdate('other')).ok, false);
  assert.equal(
    (
      await client(async () => json({ items: [note], page: 1, pageSize: 12, total: 1 })).getUpdates(
        'all',
        2,
      )
    ).ok,
    false,
  );
  assert.equal(
    (
      await client(async () => json({ items: [note], page: 1, pageSize: 12, total: 1 })).getUpdates(
        'release',
        1,
      )
    ).ok,
    false,
  );
  assert.equal((await client(async () => json(note)).getUpdate('test')).ok, true);
});

test('real HTTP timeout includes reading a stalled response body and releases its connection', async (t) => {
  const server = http.createServer((_req, response) => {
    response.writeHead(200, { 'content-type': 'application/json' });
    response.write('{');
  });
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  t.after(async () => {
    server.closeAllConnections();
    await new Promise((resolve) => server.close(resolve));
  });
  const api = createContentClient({
    origin: `http://127.0.0.1:${server.address().port}`,
    timeoutMs: 100,
  });
  assert.deepEqual(await api.getReleases(), { ok: false, reason: 'unavailable' });
});

test('website URL normalization is shared by metadata and the updates page', () => {
  assert.deepEqual(updatesQuery({ type: 'release', page: '2' }), { type: 'release', page: 2 });
  for (const params of [
    {},
    { type: ['release', 'all'], page: ['1', '2'] },
    { type: 'unknown', page: '-1' },
  ]) {
    assert.deepEqual(updatesQuery(params), { type: 'all', page: 1 });
  }
});

const assert = require('node:assert/strict');
const http = require('node:http');
const { spawn } = require('node:child_process');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const production = require(
  path.join(root, 'apps/api/dist/content/infrastructure/curated-catalog.js'),
);
const preview = process.argv.find((x) => x.startsWith('--preview='))?.slice('--preview='.length);
if (
  preview &&
  !['empty', 'error', 'stable', 'prerelease', 'withdrawn', 'invalid', 'pagination'].includes(
    preview,
  )
)
  throw new Error('Unknown fixture scenario');
let scenario = preview || 'empty';
let previewBase = '';
const release = (id, channel, extra = {}) => ({
  id,
  version: '0.1.0',
  channel,
  publishedAt: channel === 'stable' ? '2026-01-01T00:00:00.000Z' : '2026-02-01T00:00:00.000Z',
  minecraftVersions: ['1.8.9'],
  changelogSlug: 'fixture-release',
  artifacts: [
    {
      platform: 'windows',
      architecture: 'x64',
      fileName: `TEST-ONLY-${id}.zip`,
      sizeBytes: 1048576,
      sha256: 'a'.repeat(64),
      downloadUrl: `https://github.com/AdilSatuv/spaceclient/releases/download/test/TEST-ONLY-${id}.zip`,
      signatureStatus: 'unsigned',
    },
  ],
  ...extra,
});
const fixture = http.createServer(async (req, res) => {
  res.setHeader('Content-Type', 'application/json');
  const url = new URL(req.url, 'http://localhost');
  if (preview && !url.pathname.startsWith('/v1/')) {
    try {
      const response = await fetch(
        previewBase + (url.pathname === '/__clipboard-denied' ? '/download' : req.url),
        { redirect: 'manual' },
      );
      res.statusCode = response.status;
      response.headers.forEach((value, key) => {
        if (
          !['connection', 'transfer-encoding', 'content-encoding', 'content-length'].includes(key)
        )
          res.setHeader(key, value);
      });
      res.setHeader('Permissions-Policy', 'clipboard-write=()');
      res.setHeader('X-Space-Test-Only', 'clipboard-denied');
      res.end(Buffer.from(await response.arrayBuffer()));
    } catch {
      res.statusCode = 502;
      res.end('Test preview unavailable');
    }
    return;
  }
  if (scenario === 'error') {
    res.statusCode = 503;
    res.end('{}');
    return;
  }
  if (url.pathname === '/v1/releases') {
    let items = [];
    if (scenario === 'stable') items = [release('beta', 'beta'), release('stable', 'stable')];
    if (scenario === 'prerelease') items = [release('alpha', 'alpha')];
    if (scenario === 'withdrawn') items = [release('withdrawn', 'stable', { withdrawn: true })];
    if (scenario === 'invalid') {
      const item = release('invalid', 'stable');
      item.artifacts[0].downloadUrl = 'https://example.com/unsafe.zip';
      items = [item];
    }
    res.end(JSON.stringify({ items, total: items.length }));
    return;
  }
  if (url.pathname === '/v1/features') {
    res.end(JSON.stringify({ items: production.features }));
    return;
  }
  if (url.pathname === '/v1/updates') {
    const page = Number(url.searchParams.get('page') || 1);
    const type = url.searchParams.get('type');
    const all =
      scenario === 'pagination'
        ? Array.from({ length: 14 }, (_, i) => ({
            ...production.updates[0],
            id: `fixture-${i}`,
            slug: `fixture-${i}`,
            title: `TEST ONLY NOTE ${i}`,
          }))
        : production.updates;
    const items = all.filter((x) => !type || type === 'all' || x.type === type);
    res.end(
      JSON.stringify({
        items: items.slice((page - 1) * 12, page * 12),
        total: items.length,
        page,
        pageSize: 12,
      }),
    );
    return;
  }
  const item = production.updates.find((x) => url.pathname === `/v1/updates/${x.slug}`);
  if (item) {
    res.end(JSON.stringify(item));
    return;
  }
  res.statusCode = 404;
  res.end('{}');
});
async function freePort() {
  const s = http.createServer();
  await new Promise((r) => s.listen(0, '127.0.0.1', r));
  const port = s.address().port;
  await new Promise((r) => s.close(r));
  return port;
}
(async () => {
  await new Promise((r) => fixture.listen(0, '127.0.0.1', r));
  const port = await freePort();
  const base = `http://127.0.0.1:${port}`;
  previewBase = base;
  const child = spawn(
    process.execPath,
    [
      path.join(root, 'node_modules/next/dist/bin/next'),
      'start',
      '--hostname',
      '127.0.0.1',
      '--port',
      String(port),
    ],
    {
      cwd: path.join(root, 'apps/website'),
      env: { ...process.env, SPACE_API_URL: `http://127.0.0.1:${fixture.address().port}` },
      stdio: ['ignore', 'pipe', 'pipe'],
    },
  );
  let logs = '';
  child.stdout.on('data', (x) => (logs += x));
  child.stderr.on('data', (x) => (logs += x));
  try {
    let ready = false;
    for (let i = 0; i < 80; i++) {
      try {
        if ((await fetch(base)).ok) {
          ready = true;
          break;
        }
      } catch {}
      if (child.exitCode !== null) break;
      await new Promise((r) => setTimeout(r, 200));
    }
    assert.ok(ready, `Next failed to start: ${logs}`);
    if (preview) {
      console.log(`TEST FIXTURES ONLY (${preview}): ${base}/download`);
      console.log(
        `Clipboard denial: http://127.0.0.1:${fixture.address().port}/__clipboard-denied`,
      );
      console.log(
        'No artifacts exist. Do not click download links. Ctrl+C stops both test servers.',
      );
      await new Promise((resolve) => {
        process.once('SIGINT', resolve);
        process.once('SIGTERM', resolve);
      });
      return;
    }
    const page = async (route = '/download') => {
      const r = await fetch(base + route);
      assert.equal(r.headers.get('x-content-type-options'), 'nosniff');
      assert.equal(r.headers.get('x-frame-options'), 'DENY');
      assert.match(r.headers.get('content-security-policy') || '', /frame-ancestors 'none'/);
      return { status: r.status, html: await r.text() };
    };
    let result = await page();
    assert.match(result.html, /Первый релиз ещё впереди/);
    console.log('PASS empty catalog');
    scenario = 'error';
    result = await page();
    assert.match(result.html, /Не удалось загрузить каталог/);
    assert.doesNotMatch(result.html, /Первый релиз ещё впереди/);
    assert.match(result.html, /Повторить запрос/);
    console.log('PASS API failure is distinct from empty');
    scenario = 'invalid';
    result = await page();
    assert.match(result.html, /Не удалось загрузить каталог/);
    assert.ok(!result.html.includes('href="https://example.com/unsafe.zip"'));
    console.log('PASS unsafe artifact has no download action');
    scenario = 'stable';
    result = await page();
    assert.ok(result.html.indexOf('STABLE') < result.html.indexOf('BETA'));
    assert.match(result.html, /Рекомендуемая сборка/);
    assert.match(result.html, /Предварительная версия/);
    assert.match(result.html, /Копировать хеш/);
    console.log('PASS stable preference, prerelease label, hash controls');
    scenario = 'prerelease';
    result = await page();
    assert.match(result.html, /Предварительная версия/);
    assert.match(result.html, /Рекомендуемая сборка/);
    console.log('PASS prerelease fallback');
    scenario = 'withdrawn';
    result = await page();
    assert.match(result.html, /Сборка отозвана/);
    assert.ok(
      !result.html.includes(
        'href="' + release('withdrawn', 'stable').artifacts[0].downloadUrl + '"',
      ),
    );
    console.log('PASS withdrawn download removed');
    scenario = 'pagination';
    result = await page('/updates?type=development&page=2');
    assert.match(result.html, /TEST ONLY NOTE 12/);
    assert.match(result.html, /TEST ONLY NOTE 13/);
    assert.doesNotMatch(result.html, /TEST ONLY NOTE 11/);
    assert.ok(result.html.includes('page=1'));
    console.log('PASS updates pagination preserves type');
    scenario = 'empty';
    result = await page('/updates?type=release');
    assert.match(result.html, /Релизов ещё не было/);
    assert.equal((await page('/updates/unknown')).status, 404);
    console.log('PASS release filter and true article 404');
    for (const route of [
      '/',
      '/features',
      '/help',
      '/help/getting-started',
      '/help/launcher-status',
      '/updates/first-contact',
      '/updates/launcher-and-website',
    ])
      assert.equal((await page(route)).status, 200, route);
    console.log('Website production state checks passed; fixtures stayed in the test server.');
  } finally {
    child.kill();
    fixture.closeAllConnections();
    await new Promise((r) => fixture.close(r));
  }
})().catch((error) => {
  console.error(error);
  fixture.closeAllConnections();
  fixture.close();
  process.exitCode = 1;
});

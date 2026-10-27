const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createApp } = require('../dist/app.js');
const { isReleaseList } = require('@space/content-contracts');
const production = require('../dist/content/infrastructure/curated-catalog.js');

function fixtureRelease(id, publishedAt, extra = {}) {
  return {
    id,
    version: '0.1.0',
    channel: 'alpha',
    publishedAt,
    minecraftVersions: ['1.8.9'],
    changelogSlug: id,
    artifacts: [
      {
        platform: 'windows',
        architecture: 'x64',
        fileName: 'Space-Setup.exe',
        sizeBytes: 12345,
        sha256: 'a'.repeat(64),
        downloadUrl:
          'https://github.com/AdilSatuv/spaceclient/releases/download/v0.1.0/Space-Setup.exe',
        signatureStatus: 'unknown',
      },
    ],
    ...extra,
  };
}
function fixtureCatalog() {
  const releases = [
    fixtureRelease('older', '2026-10-20T00:00:00.000Z'),
    fixtureRelease('z-withdrawn', '2026-10-21T00:00:00.000Z', { withdrawn: true }),
    fixtureRelease('a-current', '2026-10-21T00:00:00.000Z', { channel: 'stable' }),
  ];
  return {
    features: [],
    releases,
    updates: releases.map((release) => ({
      id: release.id,
      slug: release.changelogSlug,
      type: 'release',
      title: 'Fixture only',
      summary: 'Not a real release',
      publishedAt: release.publishedAt,
      releaseId: release.id,
      blocks: [{ heading: 'Fixture', text: 'Test data' }],
    })),
  };
}

test('HTTP releases preserve the complete ordered catalog including withdrawn entries in isolation', async () => {
  const catalog = fixtureCatalog();
  const expected = structuredClone(catalog.releases);
  const fixtureApp = await createApp({ catalog });
  const productionApp = await createApp();
  try {
    await Promise.all([fixtureApp.listen(0, '127.0.0.1'), productionApp.listen(0, '127.0.0.1')]);
    catalog.releases[0].version = 'mutated after bootstrap';
    const response = await fetch((await fixtureApp.getUrl()) + '/v1/releases');
    assert.equal(response.status, 200);
    const body = await response.json();
    assert.equal(isReleaseList(body), true);
    assert.deepEqual(body, { items: [expected[2], expected[1], expected[0]], total: 3 });
    const productionResponse = await fetch((await productionApp.getUrl()) + '/v1/releases');
    assert.equal(productionResponse.status, 200);
    assert.deepEqual(await productionResponse.json(), { items: [], total: 0 });
    assert.deepEqual(production.releases, []);
  } finally {
    await Promise.all([fixtureApp.close(), productionApp.close()]);
  }
});

test('invalid release configuration is rejected before the HTTP server starts', async (t) => {
  const cases = [
    ['version', (r) => (r.version = '1.2')],
    ['channel', (r) => (r.channel = 'preview')],
    ['channel type', (r) => (r.channel = ['alpha'])],
    ['platform', (r) => (r.artifacts[0].platform = 'linux')],
    ['architecture', (r) => (r.artifacts[0].architecture = 'arm64')],
    ['SHA-256', (r) => (r.artifacts[0].sha256 = 'a'.repeat(63))],
    ['SHA-256 non-hex', (r) => (r.artifacts[0].sha256 = 'g'.repeat(64))],
    ['URL host', (r) => (r.artifacts[0].downloadUrl = 'https://example.com/Space-Setup.exe')],
    [
      'URL filename',
      (r) =>
        (r.artifacts[0].downloadUrl =
          'https://github.com/AdilSatuv/spaceclient/releases/download/v0.1.0/other.exe'),
    ],
    ['signature type', (r) => (r.artifacts[0].signatureStatus = ['signed'])],
    ['withdrawn type', (r) => (r.withdrawn = 'true')],
    ['duplicate artifact', (r) => r.artifacts.push(structuredClone(r.artifacts[0]))],
  ];
  for (const [name, edit] of cases) {
    await t.test(name, async () => {
      const catalog = fixtureCatalog();
      edit(catalog.releases[0]);
      await assert.rejects(createApp({ catalog }), /Invalid content|Duplicate release artifact/);
    });
  }
  const missingChangelog = fixtureCatalog();
  missingChangelog.updates = [];
  await assert.rejects(createApp({ catalog: missingChangelog }), /Missing release changelog/);
});

test('other content enum values must also be strings', async () => {
  for (const edit of [
    (c) => (c.features[0].category = ['hud']),
    (c) => (c.features[0].status = ['prototype']),
    (c) => (c.updates[0].type = ['development']),
  ]) {
    const catalog = structuredClone(production);
    edit(catalog);
    await assert.rejects(createApp({ catalog }), /Invalid content/);
  }
});

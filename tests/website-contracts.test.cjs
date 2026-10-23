const test = require('node:test');
const assert = require('node:assert/strict');
const {
  isReleaseList,
  isFeatureList,
  isUpdateList,
  allowedDownload,
} = require('@space/content-contracts');
const { recommendedRelease } = require('../apps/website/lib/catalog.ts');
const fixture = (id, channel, publishedAt, extra = {}) => ({
  id,
  version: '0.1.0',
  channel,
  publishedAt,
  minecraftVersions: ['1.8.9'],
  changelogSlug: 'fixture-release',
  artifacts: [
    {
      platform: 'windows',
      architecture: 'x64',
      fileName: 'Space-Test.zip',
      sizeBytes: 1048576,
      sha256: 'a'.repeat(64),
      downloadUrl: 'https://github.com/AdilSatuv/spaceclient/releases/download/test/Space-Test.zip',
      signatureStatus: 'unsigned',
    },
  ],
  ...extra,
});
test('website distinguishes a valid empty catalog from malformed responses', () => {
  assert.equal(isReleaseList({ items: [], total: 0 }), true);
  for (const value of [{ releases: [] }, { items: [], total: 2 }, { items: [{}], total: 1 }, null])
    assert.equal(isReleaseList(value), false);
  assert.equal(
    isFeatureList({
      items: [
        {
          id: 'x',
          slug: 'x',
          name: 'x',
          summary: 'x',
          details: 'x',
          category: 'hud',
          status: 'released',
        },
      ],
    }),
    false,
  );
  assert.equal(isUpdateList({ items: [], page: 0, pageSize: 12, total: 0 }), false);
});
test('download recommendation prefers compatible stable and excludes withdrawn versions', () => {
  const stable = fixture('stable', 'stable', '2026-01-01T00:00:00.000Z');
  const beta = fixture('beta', 'beta', '2026-02-01T00:00:00.000Z');
  const withdrawn = fixture('withdrawn', 'stable', '2026-03-01T00:00:00.000Z', { withdrawn: true });
  assert.equal(recommendedRelease([withdrawn, beta, stable]).id, 'stable');
  assert.equal(recommendedRelease([withdrawn, beta]).id, 'beta');
  assert.equal(recommendedRelease([withdrawn]), undefined);
  assert.equal(
    recommendedRelease([
      fixture('other', 'stable', '2026-04-01T00:00:00.000Z', { minecraftVersions: ['1.21.1'] }),
      beta,
    ]).id,
    'beta',
  );
  assert.equal(isReleaseList({ items: [withdrawn], total: 1 }), true);
});
test('website rejects unsafe download targets and invalid artifact metadata', () => {
  const release = fixture('stable', 'stable', '2026-01-01T00:00:00.000Z');
  assert.equal(isReleaseList({ items: [release], total: 1 }), true);
  for (const url of [
    'https://example.com/Space-Test.zip',
    'javascript:alert(1)',
    'https://github.com/Other/repo/releases/download/v1/Space-Test.zip',
    release.artifacts[0].downloadUrl + '?redirect=x',
  ])
    assert.equal(allowedDownload(url, 'Space-Test.zip'), false);
  for (const change of [
    { sha256: 'invalid' },
    { sizeBytes: 0 },
    { downloadUrl: 'https://example.com/a.zip' },
  ]) {
    const item = structuredClone(release);
    Object.assign(item.artifacts[0], change);
    assert.equal(isReleaseList({ items: [item], total: 1 }), false);
  }
});

test('shared catalog validation rejects duplicate slugs and dangling release relationships', () => {
  const { validateContent } = require('@space/content-contracts');
  const note = {
    id: 'one',
    slug: 'one',
    title: 'Test',
    summary: 'Fixture',
    type: 'development',
    publishedAt: '2026-10-24T00:00:00.000Z',
    blocks: [{ heading: 'Test', text: 'Test' }],
  };
  assert.doesNotThrow(() => validateContent({ features: [], releases: [], updates: [note] }));
  for (const updates of [
    [note, { ...note, id: 'two' }],
    [{ ...note, releaseId: 'absent' }],
    [null],
  ]) {
    assert.throws(() => validateContent({ features: [], releases: [], updates }));
  }
  for (const input of [null, {}, { features: null, releases: [], updates: [] }])
    assert.throws(() => validateContent(input));
  assert.equal(isUpdateList({ items: [], page: 1, pageSize: 12, total: 20 }), false);
  assert.equal(isUpdateList({ items: [], page: 3, pageSize: 12, total: 20 }), true);
});

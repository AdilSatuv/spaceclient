const { test } = require('node:test');
const assert = require('node:assert/strict');
const { validateContent } = require('@space/content-contracts');
const {
  features,
  releases,
  updates,
} = require('../dist/content/infrastructure/curated-catalog.js');
const fixture = () => ({
  features: [],
  releases: [
    {
      id: 'alpha-1',
      version: '0.1.0',
      channel: 'alpha',
      publishedAt: '2026-10-23T00:00:00.000Z',
      minecraftVersions: ['1.8.9'],
      changelogSlug: 'alpha-1',
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
    },
  ],
  updates: [
    {
      id: 'alpha-1',
      slug: 'alpha-1',
      type: 'release',
      title: 'Test fixture',
      summary: 'Not a real release',
      publishedAt: '2026-10-23T00:00:00.000Z',
      releaseId: 'alpha-1',
      blocks: [{ heading: 'Test', text: 'Fixture only' }],
    },
  ],
});
test('production content has no invented releases', () => {
  validateContent({ features, releases, updates });
  assert.equal(releases.length, 0);
});
test('release fixture is accepted and metadata errors are rejected', () => {
  validateContent(fixture());
  for (const edit of [
    (x) => (x.releases[0].artifacts[0].sha256 = 'oops'),
    (x) => (x.releases[0].artifacts[0].sizeBytes = -1),
    (x) => (x.releases[0].channel = 'preview'),
    (x) => (x.releases[0].publishedAt = 'tomorrow'),
    (x) => (x.updates = []),
    (x) => (x.features = [{ ...features[0], status: 'released' }]),
  ]) {
    const x = fixture();
    edit(x);
    assert.throws(() => validateContent(x));
  }
});
test('download URLs are constrained to this repository and exact artifact', () => {
  for (const url of [
    'http://github.com/AdilSatuv/spaceclient/releases/download/v0.1.0/Space-Setup.exe',
    'https://evil.example/Space-Setup.exe',
    'https://github.com/other/repo/releases/download/v0.1.0/Space-Setup.exe',
    'https://user:pass@github.com/AdilSatuv/spaceclient/releases/download/v0.1.0/Space-Setup.exe',
    'https://github.com/AdilSatuv/spaceclient/releases/download/v0.1.0/other.exe',
    'https://github.com/AdilSatuv/spaceclient/releases/download/v0.1.0/Space-Setup.exe?redirect=1',
  ]) {
    const x = fixture();
    x.releases[0].artifacts[0].downloadUrl = url;
    assert.throws(() => validateContent(x), url);
  }
});

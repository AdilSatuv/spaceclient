const test = require('node:test');
const assert = require('node:assert/strict');
const {
  configuredOrigin,
  indexingEnabled,
  pageMetadata,
} = require('../apps/website/src/shared/lib/metadata.ts');
test('preview metadata has no invented canonical or indexable pages', () => {
  const old = {
    node: process.env.NODE_ENV,
    url: process.env.SPACE_SITE_URL,
    index: process.env.SPACE_SITE_INDEXABLE,
  };
  try {
    delete process.env.SPACE_SITE_URL;
    process.env.SPACE_SITE_INDEXABLE = 'true';
    process.env.NODE_ENV = 'production';
    assert.equal(indexingEnabled(), false);
    assert.equal(pageMetadata('/help', 'Help', 'Help').alternates, undefined);
    for (const url of [
      'http://localhost:3000',
      'https://127.0.0.1',
      'https://user:pass@space.test',
      'https://space.test/path',
      'https://space.test/?x=1',
      'invalid',
    ])
      assert.equal(configuredOrigin(url), undefined);
    process.env.SPACE_SITE_URL = 'https://space.test';
    assert.equal(indexingEnabled(), true);
    const metadata = pageMetadata('/help', 'Help', 'Help');
    assert.equal(metadata.alternates.canonical, 'https://space.test/help');
    assert.equal(metadata.robots.index, true);
    assert.equal(pageMetadata('/help', 'Help', 'Help', true).robots.index, false);
    process.env.NODE_ENV = 'development';
    assert.equal(indexingEnabled(), false);
  } finally {
    for (const [key, value] of [
      ['NODE_ENV', old.node],
      ['SPACE_SITE_URL', old.url],
      ['SPACE_SITE_INDEXABLE', old.index],
    ]) {
      if (value === undefined) delete process.env[key];
      else process.env[key] = value;
    }
  }
});

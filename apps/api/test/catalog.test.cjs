const { test } = require('node:test');
const assert = require('node:assert/strict');
const {
  StaticContentRepository,
} = require('../dist/content/infrastructure/static-content-repository.js');
const { CatalogService } = require('../dist/content/application/catalog-service.js');
const production = require('../dist/content/infrastructure/curated-catalog.js');
const { createApp } = require('../dist/app.js');
const {
  isFeatureList,
  isReleaseList,
  isUpdateList,
  isUpdate,
} = require('@space/content-contracts');

test('catalog service works without Nest and repository snapshots cannot mutate content', () => {
  const input = structuredClone(production);
  const repository = new StaticContentRepository(input);
  const service = new CatalogService(repository);
  input.features[0].name = 'Outside';
  const result = service.features();
  assert.equal(result.items[0].name, production.features[0].name);
  result.items[0].name = 'Caller';
  result.items.splice(0);
  repository.read().updates.splice(0);
  assert.equal(isFeatureList(service.features()), true);
  assert.equal(service.features().items.length, 6);
  assert.equal(isReleaseList(service.releases()), true);
  assert.equal(isUpdateList(service.updates({ type: 'all', page: 1 })), true);
  assert.equal(isUpdate(service.update('first-contact')), true);
  assert.equal(service.update('missing'), undefined);
  assert.equal(service.updates({ type: 'release', page: 1 }).total, 0);
});

test('API instances own their injected catalog and cannot leak fixtures between applications', async () => {
  const catalog = structuredClone(production);
  catalog.features[0].name = 'Fixture only';
  const first = await createApp({ catalog });
  const second = await createApp();
  try {
    await first.listen(0, '127.0.0.1');
    await second.listen(0, '127.0.0.1');
    catalog.features[0].name = 'Changed after bootstrap';
    const read = async (app) =>
      (await (await fetch((await app.getUrl()) + '/v1/features')).json()).items[0].name;
    assert.equal(await read(first), 'Fixture only');
    assert.equal(await read(second), production.features[0].name);
  } finally {
    await Promise.all([first.close(), second.close()]);
  }
});

test('invalid catalog fails bootstrap rather than serving partially validated data', async () => {
  await assert.rejects(
    createApp({ catalog: { features: [], releases: [{}], updates: [] } }),
    /Invalid content/,
  );
});

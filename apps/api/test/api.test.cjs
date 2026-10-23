const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createApp } = require('../dist/app.js');
test('HTTP API exposes health, project and an honest empty release list', async () => {
  const app = await createApp();
  await app.listen(0, '127.0.0.1');
  const base = await app.getUrl();
  try {
    assert.deepEqual(await (await fetch(base + '/v1/health')).json(), {
      status: 'ok',
      service: 'space-api',
    });
    const project = await (await fetch(base + '/v1/project')).json();
    assert.equal(project.stage, 'development');
    assert.equal(project.minecraft, '1.8.9');
    assert.deepEqual(await (await fetch(base + '/v1/releases')).json(), { items: [], total: 0 });
    const features = await (await fetch(base + '/v1/features')).json();
    assert.equal(features.items.length, 6);
    assert.equal(
      features.items.some((x) => x.status === 'released'),
      false,
    );
    const updates = await (await fetch(base + '/v1/updates')).json();
    assert.equal(updates.pageSize, 12);
    assert.equal(updates.total, 1);
    assert.equal((await (await fetch(base + '/v1/updates?type=release')).json()).total, 0);
    assert.equal((await (await fetch(base + '/v1/updates?page=2')).json()).items.length, 0);
    assert.equal(
      (await (await fetch(base + '/v1/updates/first-contact')).json()).type,
      'development',
    );
    assert.equal((await fetch(base + '/v1/updates/unknown')).status, 404);
    for (const query of [
      'page=0',
      'page=-1',
      'page=1.5',
      'page=1000000',
      'page=1&page=2',
      'type=bogus',
      'limit=1',
    ]) {
      const response = await fetch(base + '/v1/updates?' + query);
      assert.equal(response.status, 400, query);
      assert.equal((await response.json()).code, 'INVALID_QUERY');
    }
    assert.equal((await fetch(base + '/v1/updates/UPPERCASE')).status, 400);
    assert.equal((await fetch(base + '/unknown')).status, 404);
    assert.deepEqual(await (await fetch(base + '/unknown')).json(), {
      statusCode: 404,
      code: 'NOT_FOUND',
      message: 'Ресурс не найден',
    });
    assert.equal(
      (
        await fetch(base + '/v1/releases', {
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: '{}',
        })
      ).status,
      404,
    );
  } finally {
    await app.close();
  }
});

test('pagination uses twelve entries and deterministic ordering for equal dates', async () => {
  const content = require('../dist/content/infrastructure/curated-catalog.js');
  const catalog = structuredClone(content);
  for (let i = 0; i < 13; i++)
    catalog.updates.push({
      ...catalog.updates[0],
      id: `test-${String(i).padStart(2, '0')}`,
      slug: `test-${i}`,
    });
  const app = await createApp({ catalog });
  await app.listen(0, '127.0.0.1');
  try {
    const base = await app.getUrl();
    const first = await (await fetch(base + '/v1/updates?page=1')).json();
    const second = await (await fetch(base + '/v1/updates?page=2')).json();
    assert.equal(first.items.length, 12);
    assert.equal(second.items.length, 2);
    assert.equal(first.total, 14);
    assert.deepEqual(
      [...first.items, ...second.items].map((x) => x.id),
      catalog.updates.map((x) => x.id).sort(),
    );
    assert.equal(content.updates.length, 1);
  } finally {
    await app.close();
  }
});

const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createApp } = require('../dist/app.js');
test('HTTP API exposes health, project and an honest empty release list', async () => {
  const app = await createApp();
  await app.listen(0, '127.0.0.1');
  const base = await app.getUrl();
  try {
    assert.deepEqual(await (await fetch(base + '/v1/health')).json(), { status: 'ok', service: 'space-api' });
    const project = await (await fetch(base + '/v1/project')).json();
    assert.equal(project.stage, 'development');
    assert.equal(project.minecraft, '1.8.9');
    assert.deepEqual((await (await fetch(base + '/v1/releases')).json()).releases, []);
    assert.equal((await fetch(base + '/unknown')).status, 404);
  } finally { await app.close(); }
});

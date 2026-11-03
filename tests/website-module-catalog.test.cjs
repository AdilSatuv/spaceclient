const { test } = require('node:test');
const assert = require('node:assert/strict');

test('module group filters preserve tools and combine with search/status and legacy categories', async () => {
  const { countFeatures, featureKind, filterFeatures } =
    await import('../apps/website/src/_pages/features/model/catalog.ts');
  const items = [
    {
      id: 'fps',
      category: 'hud',
      status: 'prototype',
      name: 'FPS',
      summary: 'Кадры',
      details: 'Частота',
    },
    {
      id: 'game-dynamic-fps',
      category: 'performance',
      status: 'prototype',
      name: 'Dynamic FPS',
      summary: 'Фон',
      details: 'Лимит',
    },
    {
      id: 'game-free-look',
      category: 'comfort',
      status: 'prototype',
      name: 'FreeLook',
      summary: 'Обзор',
      details: 'Камера',
    },
    {
      id: 'editor',
      category: 'comfort',
      status: 'verified',
      name: 'Редактор HUD',
      summary: 'Счётчик',
      details: 'Профили',
    },
    {
      id: 'profiles',
      category: 'launcher',
      status: 'verified',
      name: 'Профили запуска',
      summary: 'Java',
      details: 'Память',
    },
  ];
  const ids = (q = '', category = '', status = '') =>
    filterFeatures(items, q, category, status).map((item) => item.id);
  assert.deepEqual(ids('', 'game'), ['game-dynamic-fps', 'game-free-look']);
  assert.deepEqual(ids('', 'hud'), ['fps']);
  assert.deepEqual(ids('', 'tools'), ['editor', 'profiles']);
  assert.deepEqual(ids('', 'launcher'), ['profiles']);
  assert.deepEqual(ids('', 'comfort'), ['game-free-look', 'editor']);
  assert.deepEqual(ids('FPS', 'game', 'prototype'), ['game-dynamic-fps']);
  assert.deepEqual(ids('камера', 'game'), ['game-free-look']);
  assert.deepEqual(ids('счетчик', 'tools', 'verified'), ['editor']);
  assert.deepEqual(ids('несуществующий'), []);
  assert.deepEqual(ids('', 'hud', 'verified'), []);
  assert.equal(featureKind({ id: 'future', category: 'comfort' }), 'other');
  assert.deepEqual(countFeatures([...items, { id: 'future', category: 'comfort' }]), {
    hud: 1,
    game: 2,
    tools: 2,
    other: 1,
    modules: 3,
    total: 6,
  });
  assert.deepEqual(countFeatures([]), {
    hud: 0,
    game: 0,
    tools: 0,
    other: 0,
    modules: 0,
    total: 0,
  });
});

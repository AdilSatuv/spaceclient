const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { features } = require('../dist/content/infrastructure/curated-catalog.js');

const java = (file) =>
  fs.readFileSync(
    path.resolve(__dirname, '../../../game/src/main/java/dev/spaceclient', file),
    'utf8',
  );
const declarations = (source) => [...source.matchAll(/^    ([A-Z_]+)\("([^"]+)"/gm)];

test('catalog covers each actual game and HUD module exactly once, with tools counted separately', () => {
  const hudIds = declarations(java('hud/HudModule.java')).map((match) => match[2]);
  const gameIds = declarations(java('mods/GameplayModule.java')).map(
    (match) => `game-${match[1].toLowerCase().replaceAll('_', '-')}`,
  );
  assert.equal(hudIds.length, 26);
  assert.equal(gameIds.length, 29);
  const ids = [...hudIds, ...gameIds, 'editor', 'profiles'];
  assert.deepEqual(features.map((item) => item.id).sort(), ids.sort());
  assert.deepEqual(
    features
      .filter((item) => item.category === 'hud')
      .map((item) => item.id)
      .sort(),
    hudIds.sort(),
  );
  for (const id of [...hudIds, ...gameIds]) {
    const item = features.find((item) => item.id === id);
    assert.equal(item.status, 'prototype', id);
    assert.equal(item.releaseId, undefined, id);
    assert.match(item.summary, /[а-яё]/i, id);
    assert.match(item.details, /[а-яё]/i, id);
  }
  assert.equal(features.find((item) => item.id === 'profiles').category, 'launcher');
  assert.equal(features.find((item) => item.id === 'editor').status, 'verified');
  assert.ok(Buffer.byteLength(JSON.stringify({ items: features })) < 1024 * 1024);
});

test('client estimates and compatibility limits stay explicit in catalog content', () => {
  const details = (id) => features.find((item) => item.id === id).details;
  assert.match(details('saturation'), /не синхронизирует точное насыщение/);
  assert.match(details('combo'), /авторство.*не подтверждается/);
  assert.match(details('reach'), /Не меняет дальность/);
  assert.match(details('game-tnt-countdown'), /серверный таймер может отличаться/);
  assert.match(details('game-inventory-blur'), /OpenGL 2.0.*framebuffer/);
  assert.match(details('game-sounds'), /на слух звук пока не проверен/);
  assert.match(details('game-dynamic-fps'), /не обещание ускорения/);
});

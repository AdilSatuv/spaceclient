const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');

async function fixture(t) {
  const { ProfileStore } = await import('../src/main/store.ts');
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-launcher-'));
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  return { root, store: await ProfileStore.open(root, 8192), ProfileStore };
}
test('profiles preserve independent settings and selection across restart', async (t) => {
  const { root, store, ProfileStore } = await fixture(t);
  const second = await store.create();
  await store.save({ id: 'default', name: 'Первый', memoryMb: 3072 });
  await store.save({ id: second.activeProfileId, name: 'Второй', memoryMb: 4096 });
  const restarted = await ProfileStore.open(root, 8192);
  assert.equal(restarted.snapshot().activeProfileId, second.activeProfileId);
  assert.equal(restarted.profile('default').memoryMb, 3072);
  assert.equal(restarted.profile(second.activeProfileId).memoryMb, 4096);
  assert.equal(restarted.profile('default').name, 'Первый');
  const firstDir = await restarted.instanceDirectory('default');
  const secondDir = await restarted.instanceDirectory(second.activeProfileId);
  assert.notEqual(firstDir, secondDir);
  assert.equal(path.dirname(firstDir), path.join(root, 'instances'));
});
test('main rejects invalid memory, traversal, unknown fields and profile IDs', async (t) => {
  const { store } = await fixture(t);
  for (const memoryMb of [512, 1025, 65536, NaN, '2048'])
    assert.throws(() => store.save({ id: 'default', name: 'Test', memoryMb }));
  assert.throws(() => store.save({ id: 'default', name: '', memoryMb: 2048 }));
  assert.throws(() => store.save({ id: '../outside', name: 'Test', memoryMb: 2048 }));
  assert.throws(() =>
    store.save({ id: 'default', name: 'Test', memoryMb: 2048, javaPath: 'malicious.exe' }),
  );
  assert.throws(() => store.select('unknown'));
  await assert.rejects(store.instanceDirectory('../outside'));
});
test('corrupt and future settings are preserved before recovery', async (t) => {
  const { root, ProfileStore } = await fixture(t);
  const original = '{"schemaVersion":999,"profiles":[]}';
  await fs.writeFile(path.join(root, 'settings.json'), original);
  const recovered = await ProfileStore.open(root, 8192);
  assert.ok(recovered.snapshot().warning);
  const backup = (await fs.readdir(root)).find((file) => file.startsWith('settings.invalid-'));
  assert.equal(await fs.readFile(path.join(root, backup), 'utf8'), original);
  assert.equal(await fs.readFile(path.join(root, 'settings.json'), 'utf8'), original);
  await recovered.save({ id: 'default', name: 'Recovered', memoryMb: 2048 });
  assert.equal((await ProfileStore.open(root, 8192)).profile('default').name, 'Recovered');
});
test('queued writes preserve both profiles and failed writes do not alter memory state', async (t) => {
  const { root, store } = await fixture(t);
  await Promise.all([store.create(), store.create()]);
  assert.equal(store.snapshot().profiles.length, 3);
  await fs.rm(path.join(root, 'settings.json'));
  await fs.mkdir(path.join(root, 'settings.json'));
  await assert.rejects(
    store.save({ id: 'default', name: 'Should not apply', memoryMb: 4096 }),
    /Не удалось сохранить/,
  );
  assert.equal(store.profile('default').name, 'Моя орбита');
  assert.equal(
    (await fs.readdir(root)).some((file) => file.endsWith('.tmp')),
    false,
  );
});
test('oversized settings are backed up without parsing them into application state', async (t) => {
  const { root, ProfileStore } = await fixture(t);
  const original = 'x'.repeat(65537);
  await fs.writeFile(path.join(root, 'settings.json'), original);
  const store = await ProfileStore.open(root, 8192);
  assert.ok(store.snapshot().warning);
  assert.equal(store.snapshot().profiles.length, 1);
  const backup = (await fs.readdir(root)).find((file) => file.startsWith('settings.invalid-'));
  assert.equal((await fs.stat(path.join(root, backup))).size, 65537);
});
test('profile count is bounded and symlinked instance directories are refused', async (t) => {
  const { root, store } = await fixture(t);
  for (let i = 1; i < 10; i++) await store.create();
  await assert.rejects(store.create(), /10 профилей/);
  const outside = path.join(root, 'outside');
  await fs.mkdir(outside);
  await fs.symlink(
    outside,
    path.join(root, 'instances'),
    process.platform === 'win32' ? 'junction' : 'dir',
  );
  await assert.rejects(store.instanceDirectory('default'), /ссылкой/);
});

test('schema v1 migrates nickname without losing profiles or Java; valid nickname persists after restart', async (t) => {
  const { root, store } = await fixture(t);
  const { ProfileStore } = await import('../src/main/store.ts');
  const old = store.snapshot();
  delete old.localNickname;
  old.schemaVersion = 1;
  old.profiles[0].javaPath = 'C:\\Runtime\\java.exe';
  await fs.writeFile(path.join(root, 'settings.json'), JSON.stringify(old));
  const migrated = await ProfileStore.open(root, 8192);
  assert.equal(migrated.snapshot().schemaVersion, 2);
  assert.equal(migrated.snapshot().localNickname, null);
  assert.equal(migrated.profile('default').javaPath, old.profiles[0].javaPath);
  await migrated.saveNickname('Space_123');
  const reopened = await ProfileStore.open(root, 8192);
  assert.equal(reopened.snapshot().localNickname, 'Space_123');
  assert.deepEqual(reopened.snapshot().profiles, migrated.snapshot().profiles);
});
test('invalid nickname and failed writes preserve the last saved nickname and leave no temp files', async (t) => {
  const { root, store } = await fixture(t);
  await store.saveNickname('ValidName');
  for (const name of [null, {}, 'ab', ' abc', 'abc ', 'Юзер', 'a'.repeat(17), 'a-b', 'a\nname'])
    await assert.rejects(async () => store.saveNickname(name));
  assert.equal(store.snapshot().localNickname, 'ValidName');
  await fs.rename(path.join(root, 'settings.json'), path.join(root, 'settings.saved'));
  await fs.mkdir(path.join(root, 'settings.json'));
  await assert.rejects(store.saveNickname('OtherName'), /сохранить/);
  assert.equal(store.snapshot().localNickname, 'ValidName');
  assert.equal((await fs.readdir(root)).filter((x) => x.endsWith('.tmp')).length, 0);
});
test('invalid schema v2 nickname preserves the original settings before recovery', async (t) => {
  const { root, store } = await fixture(t);
  const value = { ...store.snapshot(), localNickname: '../bad' };
  const raw = JSON.stringify(value);
  await fs.writeFile(path.join(root, 'settings.json'), raw);
  const { ProfileStore } = await import('../src/main/store.ts');
  const reopened = await ProfileStore.open(root, 8192);
  assert.equal(reopened.snapshot().localNickname, null);
  assert.ok(reopened.snapshot().warning);
  assert.equal(await fs.readFile(path.join(root, 'settings.json'), 'utf8'), raw);
});

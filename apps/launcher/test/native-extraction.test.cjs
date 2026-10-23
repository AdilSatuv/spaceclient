const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { createHash } = require('node:crypto');
const { makeZip } = require('./zip-fixture.cjs');
async function fixture(t, entries, countOverride) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-native-'));
  assert.equal(path.dirname(root), os.tmpdir());
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  const bytes = makeZip(entries, countOverride);
  const artifact = {
    url: 'https://libraries.minecraft.net/example-natives-windows-64.jar',
    sha1: createHash('sha1').update(bytes).digest('hex'),
    size: bytes.length,
    relativePath: 'libraries/example-natives-windows-64.jar',
  };
  await fs.mkdir(path.join(root, 'libraries'));
  await fs.writeFile(path.join(root, artifact.relativePath), bytes);
  return { root, artifact, ...(await import('../src/main/native-extraction.ts')) };
}
async function generations(root) {
  return fs
    .readdir(path.join(root, 'natives'))
    .then((names) => names.filter((name) => name.startsWith('run-')))
    .catch((error) => {
      if (error.code === 'ENOENT') return [];
      throw error;
    });
}
test('verified native generations exclude META-INF and are reused only after hash verification', async (t) => {
  const { root, artifact, extractNatives } = await fixture(t, [
    { name: 'META-INF/', data: '' },
    { name: 'META-INF/MANIFEST.MF', data: 'not extracted' },
    { name: 'native.dll', data: 'first' },
    { name: 'nested/native64.dll', data: 'second', method: 8 },
  ]);
  const first = await extractNatives(root, [artifact]);
  const second = await extractNatives(root, [artifact]);
  assert.equal(first.directory, second.directory);
  assert.equal(first.fromCache, false);
  assert.equal(second.fromCache, true);
  assert.equal((await generations(root)).length, 1);
  assert.equal(path.dirname(first.directory), path.join(root, 'natives'));
  assert.deepEqual(
    first.files.map((file) => file.relativePath),
    ['native.dll', 'nested/native64.dll'],
  );
  assert.equal(first.totalBytes, 11);
  assert.equal(
    await fs.readFile(path.join(first.directory, 'nested/native64.dll'), 'utf8'),
    'second',
  );
  assert.equal(first.files[0].sha256, createHash('sha256').update('first').digest('hex'));
  await assert.rejects(fs.stat(path.join(first.directory, 'META-INF')), { code: 'ENOENT' });
  await fs.writeFile(path.join(root, artifact.relativePath), Buffer.alloc(artifact.size));
  await assert.rejects(extractNatives(root, [artifact]), /целостности/);
  assert.equal((await generations(root)).length, 1);
});
test('corrupt or unexpected native files create a new verified generation without mutating the old one', async (t) => {
  for (const mutate of [
    async (directory) => fs.writeFile(path.join(directory, 'native.dll'), 'wrong'),
    async (directory) => fs.writeFile(path.join(directory, 'extra.dll'), 'unexpected'),
  ]) {
    const { root, artifact, extractNatives } = await fixture(t, [
      { name: 'native.dll', data: 'valid' },
    ]);
    const first = await extractNatives(root, [artifact]);
    await mutate(first.directory);
    const next = await extractNatives(root, [artifact]);
    assert.notEqual(next.directory, first.directory);
    assert.equal(next.fromCache, false);
    assert.equal(await fs.readFile(path.join(next.directory, 'native.dll'), 'utf8'), 'valid');
    const cached = await extractNatives(root, [artifact]);
    assert.equal(cached.directory, next.directory);
    assert.equal(cached.fromCache, true);
    assert.equal((await generations(root)).length, 2);
  }
});
test('malformed native cache metadata is ignored and rebuilt without trusting its paths or hashes', async (t) => {
  for (const mutate of [
    (value) => {
      value.directory = '../outside';
    },
    (value) => {
      value.files[0].relativePath = '../outside.dll';
    },
    (value) => {
      value.files[0].sha256 = 'invalid';
    },
    (value) => {
      value.files[0].size = -1;
    },
    (value) => {
      value.key = '0'.repeat(64);
    },
    (value) => {
      value.totalBytes++;
    },
  ]) {
    const { root, artifact, extractNatives } = await fixture(t, [{ name: 'native.dll' }]);
    const first = await extractNatives(root, [artifact]);
    const name = (await fs.readdir(path.join(root, 'natives'))).find(
      (name) => name.startsWith('cache-') && name.endsWith('.json'),
    );
    const index = path.join(root, 'natives', name);
    const metadata = JSON.parse(await fs.readFile(index, 'utf8'));
    mutate(metadata);
    await fs.writeFile(index, JSON.stringify(metadata));
    const next = await extractNatives(root, [artifact]);
    assert.equal(next.fromCache, false);
    assert.notEqual(next.directory, first.directory);
    assert.equal((await extractNatives(root, [artifact])).fromCache, true);
  }
});
test('cache generations containing a junction are not reused even if referenced bytes match', async (t) => {
  const { root, artifact, extractNatives } = await fixture(t, [
    { name: 'native.dll', data: 'valid' },
  ]);
  const first = await extractNatives(root, [artifact]);
  const outside = path.join(root, 'outside');
  await fs.mkdir(outside);
  await fs.writeFile(path.join(outside, 'native.dll'), 'valid');
  await fs.symlink(outside, path.join(first.directory, 'link'), 'junction');
  const indexName = (await fs.readdir(path.join(root, 'natives'))).find(
    (name) => name.startsWith('cache-') && name.endsWith('.json'),
  );
  const index = path.join(root, 'natives', indexName);
  const metadata = JSON.parse(await fs.readFile(index, 'utf8'));
  metadata.files[0].relativePath = 'link/native.dll';
  await fs.writeFile(index, JSON.stringify(metadata));
  const next = await extractNatives(root, [artifact]);
  assert.equal(next.fromCache, false);
  assert.notEqual(next.directory, first.directory);
  assert.equal(await fs.readFile(path.join(outside, 'native.dll'), 'utf8'), 'valid');
});
test('ZIP traversal, absolute paths, backslashes, devices and links cannot escape staging', async (t) => {
  for (const entry of [
    { name: '../escape.dll' },
    { name: '/absolute.dll' },
    { name: 'C:/absolute.dll' },
    { name: 'nested\\escape.dll' },
    { name: 'NUL.dll' },
    { name: 'trailing./file.dll' },
    { name: 'link.dll', attributes: (0xa1ff << 16) >>> 0 },
    { name: 'reparse.dll', attributes: 0x400 },
  ]) {
    const { root, artifact, extractNatives } = await fixture(t, [entry]);
    await assert.rejects(extractNatives(root, [artifact]));
    assert.deepEqual(await generations(root), []);
  }
});
test('case collisions and file/directory conflicts discard staging but preserve older generations', async (t) => {
  for (const names of [
    ['same.dll', 'SAME.DLL'],
    ['folder/file.dll', 'folder'],
    ['folder', 'folder/file.dll'],
  ]) {
    const { root, artifact, extractNatives } = await fixture(
      t,
      names.map((name) => ({ name })),
    );
    const old = path.join(root, 'natives', 'run-existing');
    await fs.mkdir(old, { recursive: true });
    await fs.writeFile(path.join(old, 'old.dll'), 'keep');
    await assert.rejects(extractNatives(root, [artifact]), /конфликт|Конфликт/);
    assert.deepEqual(await generations(root), ['run-existing']);
    assert.equal(await fs.readFile(path.join(old, 'old.dll'), 'utf8'), 'keep');
  }
});
test('CRC, declared size, entry count and encrypted entries fail without a completed generation', async (t) => {
  for (const [entries, count] of [
    [[{ name: 'bad.dll', crc: 0 }]],
    [[{ name: 'bad.dll', method: 8, size: 40 * 1024 * 1024 }]],
    [[{ name: 'bad.dll', method: 8, size: 1 }]],
    [[{ name: 'bad.dll', encrypted: true }]],
    [[{ name: 'bad.dll', method: 99 }]],
    [[{ name: 'bad.dll' }], 2049],
  ]) {
    const { root, artifact, extractNatives } = await fixture(t, entries, count);
    await assert.rejects(extractNatives(root, [artifact]));
    assert.deepEqual(await generations(root), []);
  }
});
test('archive integrity is rechecked before extraction and pre-cancelled work creates no generation', async (t) => {
  const { root, artifact, extractNatives } = await fixture(t, [{ name: 'native.dll' }]);
  const controller = new AbortController();
  controller.abort();
  await assert.rejects(extractNatives(root, [artifact], { signal: controller.signal }), {
    name: 'AbortError',
  });
  assert.deepEqual(await generations(root), []);
  await fs.writeFile(path.join(root, artifact.relativePath), Buffer.alloc(artifact.size));
  await assert.rejects(extractNatives(root, [artifact]), /целостности/);
  assert.deepEqual(await generations(root), []);
});
test('a natives junction cannot redirect extracted files into another directory', async (t) => {
  const { root, artifact, extractNatives } = await fixture(t, [{ name: 'native.dll' }]);
  const outside = path.join(root, 'outside');
  await fs.mkdir(outside);
  await fs.symlink(outside, path.join(root, 'natives'), 'junction');
  await assert.rejects(extractNatives(root, [artifact]), /ссылкой/);
  assert.deepEqual(await fs.readdir(outside), []);
});

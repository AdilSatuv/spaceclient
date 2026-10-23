const { test } = require('node:test');
const assert = require('node:assert/strict');
test('IPC rejects another window, subframe and URL', async () => {
  const { trustedSender, APP_URL } = await import('../src/main/security.ts');
  const window = {},
    frame = {};
  assert.ok(trustedSender(window, window, frame, frame, APP_URL));
  assert.equal(trustedSender({}, window, frame, frame, APP_URL), false);
  assert.equal(trustedSender(window, window, {}, frame, APP_URL), false);
  for (const url of [
    'https://example.com/',
    APP_URL + '?x=1',
    'space://other/index.html',
    'file:///index.html',
  ])
    assert.equal(trustedSender(window, window, frame, frame, url), false);
});
test('asset protocol exposes only named local resources', async () => {
  const { assetName } = await import('../src/main/security.ts');
  assert.equal(assetName('space://app/index.html'), 'index.html');
  assert.equal(assetName('space://app/app.js'), 'app.js');
  for (const url of [
    'space://app/settings.json',
    'space://app/../../settings.json',
    'space://app/%2e%2e/settings.json',
    'space://app/app.js?file=secret',
    'space://other/app.js',
    'https://app/app.js',
    'space://user:pass@app/app.js',
  ])
    assert.equal(assetName(url), null);
});
test('Java parser accepts the pinned 21 x64 runtime and reports unsuitable versions', async () => {
  const { parseJava, inspectJava } = await import('../src/main/java.ts');
  assert.equal(parseJava('java.specification.version = 21\nsun.arch.data.model = 64').usable, true);
  assert.equal(parseJava('java.specification.version = 1.8\nsun.arch.data.model = 64').major, 8);
  assert.equal(
    parseJava('java.specification.version = 1.8\nsun.arch.data.model = 64').usable,
    false,
  );
  assert.equal(
    parseJava('java.specification.version = 21\nsun.arch.data.model = 32').usable,
    false,
  );
  assert.equal(parseJava('untrusted arbitrary output').major, null);
  await assert.rejects(inspectJava(null), /выберите/);
  await assert.rejects(inspectJava('cmd.exe'), /java.exe/);
});
test('Java paths require a local Windows drive and reject both UNC spellings', async () => {
  const { javaPath } = await import('../src/main/validation.ts');
  assert.equal(
    javaPath('C:\\Program Files\\Java\\bin\\java.exe'),
    'C:\\Program Files\\Java\\bin\\java.exe',
  );
  assert.equal(javaPath('D:/Java/bin/java.exe'), 'D:/Java/bin/java.exe');
  for (const value of [
    '//server/share/java.exe',
    '\\\\server\\share\\java.exe',
    '\\\\?\\C:\\Java\\java.exe',
    'C:java.exe',
    '/usr/bin/java.exe',
    'C:\\Java\\cmd.exe',
  ])
    assert.throws(() => javaPath(value), /локальный/);
});

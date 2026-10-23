const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { randomBytes, createCipheriv, createDecipheriv } = require('node:crypto');
const clientId = '11111111-1111-1111-1111-111111111111';
const marker = 'FAKE-NONFUNCTIONAL-REFRESH-TOKEN-FOR-TEST';
function fakeCache() {
  return JSON.stringify({
    Account: {},
    IdToken: {},
    AccessToken: {},
    AppMetadata: {},
    RefreshToken: { fake: { secret: marker } },
  });
}
async function fixture(t) {
  const { EncryptedMsalVault, EMPTY_MSAL_CACHE, MAX_CACHE_BYTES } =
    await import('../src/main/auth-vault.ts');
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-auth-test-'));
  assert.equal(path.dirname(root), os.tmpdir());
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  // Test-only key, never persisted. Production uses Electron/Windows DPAPI.
  const key = randomBytes(32);
  const state = { available: true, rotate: false, encryptions: 0, fail: false, temporary: false };
  const codec = {
    available: async () => state.available,
    encrypt: async (text) => {
      if (state.fail) throw new Error(marker);
      state.encryptions++;
      const iv = randomBytes(12);
      const cipher = createCipheriv('aes-256-gcm', key, iv);
      const bytes = Buffer.concat([cipher.update(text, 'utf8'), cipher.final()]);
      return Buffer.concat([iv, cipher.getAuthTag(), bytes]);
    },
    decrypt: async (bytes) => {
      if (state.temporary)
        throw Object.assign(new Error(marker), { isTemporarilyUnavailable: true });
      const cipher = createDecipheriv('aes-256-gcm', key, bytes.subarray(0, 12));
      cipher.setAuthTag(bytes.subarray(12, 28));
      return {
        result: Buffer.concat([cipher.update(bytes.subarray(28)), cipher.final()]).toString('utf8'),
        shouldReEncrypt: state.rotate,
      };
    },
  };
  const vault = new EncryptedMsalVault(root, clientId, codec);
  return {
    root,
    codec,
    state,
    vault,
    EncryptedMsalVault,
    EMPTY_MSAL_CACHE,
    MAX_CACHE_BYTES,
    file: path.join(root, 'auth/msal-cache.bin'),
  };
}

test('cache survives a new vault instance without plaintext on disk and is bound to the app ID', async (t) => {
  const { root, codec, vault, file, EncryptedMsalVault } = await fixture(t);
  assert.equal(await vault.read(), null);
  await vault.write(fakeCache());
  const cipher = await fs.readFile(file);
  assert.equal(cipher.includes(Buffer.from(marker)), false);
  assert.equal(cipher.includes(Buffer.from('RefreshToken')), false);
  assert.equal(await new EncryptedMsalVault(root, clientId, codec).read(), fakeCache());
  const otherApp = new EncryptedMsalVault(root, '22222222-2222-2222-2222-222222222222', codec);
  await assert.rejects(otherApp.read(), { code: 'invalid' });
  await assert.rejects(otherApp.write(fakeCache()), { code: 'invalid' });
  assert.deepEqual(await fs.readFile(file), cipher);
  assert.deepEqual(await fs.readdir(path.dirname(file)), ['msal-cache.bin']);
  await vault.clear();
  assert.equal(await vault.read(), null);
});

test('unavailable OS encryption and encryption failures never fall back to plaintext or expose payloads', async (t) => {
  const { state, vault, file } = await fixture(t);
  state.available = false;
  await assert.rejects(vault.read(), { code: 'unavailable' });
  await assert.rejects(vault.write(fakeCache()), { code: 'unavailable' });
  await assert.rejects(fs.stat(file), { code: 'ENOENT' });
  state.available = true;
  await vault.write(fakeCache());
  const before = await fs.readFile(file);
  state.temporary = true;
  await assert.rejects(
    vault.read(),
    (error) => error.code === 'unavailable' && !String(error).includes(marker),
  );
  assert.deepEqual(await fs.readFile(file), before);
  state.temporary = false;
  state.fail = true;
  await assert.rejects(
    vault.write(fakeCache()),
    (error) => error.code === 'unavailable' && !String(error).includes(marker),
  );
  assert.deepEqual(await fs.readFile(file), before);
  state.available = false;
  await vault.clear(); // Explicit local logout works even if the OS key is unavailable.
  await assert.rejects(fs.stat(file), { code: 'ENOENT' });
});

test('corrupt, oversized and incompatible sessions are preserved until explicit reset', async (t) => {
  const { vault, codec, file, EMPTY_MSAL_CACHE } = await fixture(t);
  await vault.write(fakeCache());
  const cases = [
    Buffer.from('corrupt ciphertext'),
    Buffer.alloc(0),
    Buffer.alloc(2 * 1024 * 1024 + 1),
    await codec.encrypt('not json'),
    await codec.encrypt(JSON.stringify({ schemaVersion: 2, clientId, cache: EMPTY_MSAL_CACHE })),
    await codec.encrypt(JSON.stringify({ schemaVersion: 1, clientId, cache: '[]' })),
  ];
  for (const bytes of cases) {
    await fs.writeFile(file, bytes);
    await assert.rejects(vault.read(), { code: 'invalid' });
    await assert.rejects(vault.write(EMPTY_MSAL_CACHE), { code: 'invalid' });
    assert.deepEqual(await fs.readFile(file), bytes);
  }
  await vault.clear();
  await vault.write(EMPTY_MSAL_CACHE);
  assert.equal(await vault.read(), EMPTY_MSAL_CACHE);
});

test('invalid containers, oversized input and prototype keys cannot enter persistence', async (t) => {
  const { vault, file, EMPTY_MSAL_CACHE, MAX_CACHE_BYTES } = await fixture(t);
  for (const input of [
    '',
    'null',
    '{}',
    '[]',
    'x'.repeat(MAX_CACHE_BYTES + 1),
    EMPTY_MSAL_CACHE.replace('"Account":{}', '"Account":{"__proto__":{}}'),
    EMPTY_MSAL_CACHE.replace('"Account":{}', '"Account":[]'),
  ]) {
    await assert.rejects(vault.write(input), { code: 'invalid' });
  }
  assert.equal({}.secret, undefined);
  await assert.rejects(fs.stat(file), { code: 'ENOENT' });
});

test('queued writes and reads remain ordered after an error', async (t) => {
  const { vault, EMPTY_MSAL_CACHE } = await fixture(t);
  const results = await Promise.allSettled([
    vault.write(fakeCache()),
    vault.read(),
    vault.write('bad'),
    vault.write(EMPTY_MSAL_CACHE),
    vault.read(),
  ]);
  assert.equal(results[0].status, 'fulfilled');
  assert.equal(results[1].value, fakeCache());
  assert.equal(results[2].status, 'rejected');
  assert.equal(results[3].status, 'fulfilled');
  assert.equal(results[4].value, EMPTY_MSAL_CACHE);
});

test('rotation rewrites only verified plaintext through the current encryption provider', async (t) => {
  const { vault, file, state } = await fixture(t);
  await vault.write(fakeCache());
  const before = await fs.readFile(file);
  state.rotate = true;
  assert.equal(await vault.read(), fakeCache());
  assert.equal(state.encryptions, 2);
  assert.notDeepEqual(await fs.readFile(file), before);
  state.rotate = false;
  assert.equal(await vault.read(), fakeCache());
});

test('auth junctions, linked roots, non-files and hard-linked ciphertext are refused', async (t) => {
  const { root, codec, vault, file, EncryptedMsalVault } = await fixture(t);
  const external = path.join(root, 'external');
  await fs.mkdir(external);
  await fs.symlink(external, path.join(root, 'auth'), 'junction');
  await assert.rejects(vault.read(), { code: 'storage' });
  await assert.rejects(vault.write(fakeCache()), { code: 'storage' });
  await assert.rejects(vault.clear(), { code: 'storage' });
  assert.deepEqual(await fs.readdir(external), []);
  await fs.unlink(path.join(root, 'auth'));
  const linkedRoot = path.join(root, 'linked-root');
  await fs.symlink(external, linkedRoot, 'junction');
  await assert.rejects(new EncryptedMsalVault(linkedRoot, clientId, codec).write(fakeCache()), {
    code: 'storage',
  });
  await vault.write(fakeCache());
  const copy = path.join(external, 'linked.bin');
  await fs.link(file, copy);
  await assert.rejects(vault.read(), { code: 'storage' });
  await assert.rejects(vault.clear(), { code: 'storage' });
  await fs.unlink(copy);
  await vault.clear();
  await fs.mkdir(file);
  await assert.rejects(vault.write(fakeCache()), { code: 'storage' });
});

test('failed cache reads cannot trigger persistence from the MSAL finally callback', async (t) => {
  const { vault, file } = await fixture(t);
  const { MsalCacheAccess } = await import('../src/main/msal-cache.ts');
  await vault.write(fakeCache());
  await fs.writeFile(file, 'broken');
  const access = new MsalCacheAccess(vault);
  let serialized = false;
  const context = {
    cacheHasChanged: true,
    tokenCache: {
      deserialize() {},
      serialize() {
        serialized = true;
        return fakeCache();
      },
    },
  };
  await assert.rejects(
    access.run(async () => {
      try {
        await access.plugin.beforeCacheAccess(context);
      } finally {
        await access.plugin.afterCacheAccess(context);
      }
    }),
    { code: 'invalid' },
  );
  assert.equal(serialized, false);
  assert.equal(await fs.readFile(file, 'utf8'), 'broken');
  await assert.rejects(access.plugin.beforeCacheAccess(context), { code: 'storage' });
});

test('official MSAL reads and removes fake accounts through encrypted persistence without network', async (t) => {
  const { PublicClientApplication } = await import('@azure/msal-node');
  const { MsalCacheAccess } = await import('../src/main/msal-cache.ts');
  const { vault, file } = await fixture(t);
  const data = JSON.parse(fakeCache());
  data.RefreshToken = {};
  for (const name of ['one', 'two']) {
    data.Account[name + '-login.windows.net-consumers'] = {
      home_account_id: name,
      environment: 'login.windows.net',
      realm: 'consumers',
      local_account_id: name,
      username: name + '@example.invalid',
      authority_type: 'MSSTS',
    };
  }
  await vault.write(JSON.stringify(data));
  const access = new MsalCacheAccess(vault);
  let networkCalls = 0;
  const noNetwork = async () => {
    networkCalls++;
    throw new Error('Test forbids network');
  };
  const create = () =>
    new PublicClientApplication({
      auth: { clientId, authority: 'https://login.microsoftonline.com/consumers' },
      cache: { cachePlugin: access.plugin },
      system: {
        networkClient: { sendGetRequestAsync: noNetwork, sendPostRequestAsync: noNetwork },
        loggerOptions: { piiLoggingEnabled: false, loggerCallback() {} },
      },
    });
  const pca = create();
  const accounts = await access.run(() => pca.getTokenCache().getAllAccounts());
  assert.equal(accounts.length, 2);
  await Promise.all(
    accounts.map((account) => access.run(() => pca.getTokenCache().removeAccount(account))),
  );
  assert.equal((await access.run(() => create().getTokenCache().getAllAccounts())).length, 0);
  assert.deepEqual(JSON.parse(await vault.read()).Account, {});
  assert.equal(networkCalls, 0);
  assert.equal((await fs.readFile(file)).includes(Buffer.from('@example.invalid')), false);
  assert.deepEqual(await fs.readdir(path.dirname(file)), ['msal-cache.bin']);
});

const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { png, encryption, profile, apiFixture } = require('./account-fixture.cjs');
async function fixture(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-account-'));
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  const { AccountHttp } = await import('../src/main/account/http.ts'),
    { SpaceSessionVault } = await import('../src/main/account/session.ts'),
    { SpaceAccountService } = await import('../src/main/account/service.ts');
  const crypto = encryption(),
    api = apiFixture(),
    vault = new SpaceSessionVault(root, 'http://127.0.0.1:3001', crypto);
  let nickname = 'GuestPlayer';
  const files = { skin: path.join(root, 'skin.png'), cape: path.join(root, 'cape.png') };
  await fs.writeFile(files.skin, png());
  await fs.writeFile(files.cape, png(64, 32));
  const service = new SpaceAccountService(
    new AccountHttp('http://127.0.0.1:3001', api.fetcher),
    vault,
    {
      rootForProfile: async (id) => path.join(root, id),
      localNickname: () => nickname,
      saveNickname: async (name) => {
        nickname = name;
      },
      choosePng: async (kind) => files[kind],
    },
  );
  return {
    root,
    crypto,
    api,
    vault,
    service,
    files,
    get nickname() {
      return nickname;
    },
    SpaceSessionVault,
  };
}
test('PNG fully checks chunks, CRC, dimensions, decompression and filters before accepting public pixels', async () => {
  const { validatePng } = await import('../src/main/account/png.ts');
  assert.equal(validatePng(png(), 'skin').width, 64);
  assert.equal(validatePng(png(64, 32), 'cape').height, 32);
  for (const bytes of [
    Buffer.from('not png'),
    png().subarray(0, 70),
    png(64, 64, 5),
    png(512, 256),
    Buffer.concat([png(), Buffer.from('trailing')]),
  ])
    assert.throws(() => validatePng(bytes, 'skin'));
  const bad = png();
  bad[bad.length - 1] ^= 1;
  assert.throws(() => validatePng(bad, 'skin'));
  assert.throws(() => validatePng(png(64, 32), 'skin', 'slim'));
});
test('main HTTP binds origin, fixed paths, response limits, safe errors and rejects redirects', async () => {
  const { AccountHttp, accountApiBase } = await import('../src/main/account/http.ts');
  for (const base of [
    'http://example.com',
    'http://127.0.0.1/path',
    'https://user:pass@example.com',
    'file:///tmp',
    'http://127.0.0.1/?x',
  ])
    assert.throws(() => accountApiBase(base));
  assert.equal(accountApiBase('http://127.0.0.1:1234'), 'http://127.0.0.1:1234');
  let calls = 0;
  const http = new AccountHttp('http://127.0.0.1', async (_url, options) => {
    calls++;
    assert.equal(options.redirect, 'error');
    return new Response(
      JSON.stringify({ code: 'SPACE_INVALID_CREDENTIALS', message: 'SECRET path/token' }),
      { status: 401, headers: { 'content-type': 'application/json' } },
    );
  });
  await assert.rejects(
    http.request('/v1/accounts/me'),
    (error) => !error.message.includes('SECRET') && /пароль/.test(error.message),
  );
  await assert.rejects(http.request('/v1/accounts/assets/../../secret'));
  assert.equal(calls, 1);
  for (const response of [
    new Response('x'.repeat(33000), { headers: { 'content-type': 'application/json' } }),
    new Response('', { status: 302, headers: { location: 'http://evil' } }),
  ]) {
    const h = new AccountHttp('http://127.0.0.1', async () => response);
    await assert.rejects(h.request('/v1/accounts/me'));
  }
});
test('encrypted vault persists no plaintext and binds to API origin; unavailable encryption never falls back', async (t) => {
  const f = await fixture(t),
    session = {
      profile: profile(),
      sessionToken: 'SECRET_TOKEN'.repeat(5),
      expiresAt: '2099-01-01T00:00:00Z',
    };
  await f.vault.save(session);
  const bytes = await fs.readFile(path.join(f.root, 'auth/space-session.bin'));
  assert.equal(bytes.includes(Buffer.from(session.sessionToken)), false);
  assert.deepEqual(
    (await new f.SpaceSessionVault(f.root, 'http://127.0.0.1:3001', f.crypto).load()).profile,
    profile(),
  );
  await assert.rejects(new f.SpaceSessionVault(f.root, 'http://127.0.0.1:3002', f.crypto).load());
  const other = path.join(f.root, 'unavailable');
  await fs.mkdir(other);
  const locked = new f.SpaceSessionVault(other, 'http://127.0.0.1', {
    ...f.crypto,
    available: async () => false,
  });
  await assert.rejects(locked.save(session));
  await assert.rejects(fs.readFile(path.join(other, 'auth/space-session.bin')));
});
test('register/login stay main-only; explicit apply exports only checked public appearance and changes nickname once', async (t) => {
  const f = await fixture(t);
  assert.equal((await f.service.status()).profile, null);
  let state = await f.service.register({
    username: 'FixtureSpace',
    password: 'Strong password 123!',
    nickname: 'SpacePlayer',
  });
  assert.equal(f.nickname, 'GuestPlayer');
  assert.equal('sessionToken' in state, false);
  state = await f.service.upload('skin', { expectedRevision: 1, model: 'classic' });
  state = await f.service.upload('cape', { expectedRevision: state.profile.revision });
  assert.ok(state.skinPreview && state.capePreview);
  await f.service.apply('default');
  assert.equal(f.nickname, 'SpacePlayer');
  const props = await fs.readFile(
    path.join(f.root, 'default/config/space-appearance.properties'),
    'utf8',
  );
  assert.match(props, /schemaVersion=1\naccountId=/);
  assert.doesNotMatch(props, /session|password|http|SECRET|[A-Z]:\\/);
  assert.equal(props.trim().split('\n').length, 7);
  assert.deepEqual(
    await fs.readFile(
      path.join(f.root, `default/config/space-appearance/skin-${state.profile.skin.sha256}.png`),
    ),
    png(),
  );
  await f.service.edit({ nickname: 'ChangedPlayer', expectedRevision: state.profile.revision });
  assert.equal(f.nickname, 'SpacePlayer');
  await assert.rejects(
    f.service.beforeLaunch(
      'default',
      path.join(f.root, 'default'),
      'SpacePlayer',
      new AbortController().signal,
    ),
    /Явно примените/,
  );
  await f.service.apply('default');
  assert.equal(f.nickname, 'ChangedPlayer');
  await f.service.beforeLaunch(
    'default',
    path.join(f.root, 'default'),
    'ChangedPlayer',
    new AbortController().signal,
  );
  await f.service.logout();
  assert.equal((await f.service.status()).profile, null);
  assert.equal(f.nickname, 'ChangedPlayer');
  assert.ok(f.api.calls.some((call) => call.path.endsWith('/logout') && call.token));
});
test('stale revisions, asset digest mismatch and corrupt native input cannot commit appearance', async (t) => {
  const f = await fixture(t);
  await f.service.login({ username: 'FixtureSpace', password: 'Strong password 123!' });
  await assert.rejects(
    f.service.edit({ nickname: 'NewName', expectedRevision: 2 }),
    /другом клиенте/,
  );
  await fs.writeFile(f.files.skin, 'bad PNG');
  await assert.rejects(f.service.upload('skin', { expectedRevision: 1, model: 'classic' }), /PNG/);
  assert.equal(f.api.profile.revision, 1);
  await fs.writeFile(f.files.skin, png());
  const state = await f.service.upload('skin', { expectedRevision: 1, model: 'classic' });
  f.api.assets.set(state.profile.skin.sha256, png(64, 32));
  await assert.rejects(f.service.apply('default'));
  await assert.rejects(
    fs.readFile(path.join(f.root, 'default/config/space-appearance.properties')),
  );
  assert.equal(f.nickname, 'GuestPlayer');
});
test('revoked token is discarded and guest state remains available', async (t) => {
  const f = await fixture(t);
  await f.service.login({ username: 'FixtureSpace', password: 'Strong password 123!' });
  f.api.revoke();
  await assert.rejects(f.service.status(), /истекла/);
  assert.equal((await f.service.status()).profile, null);
  await assert.rejects(fs.readFile(path.join(f.root, 'auth/space-session.bin')));
});
test('offline after Apply rechecks saved appearance, preserves nickname, and logout always removes the local encrypted session', async (t) => {
  const f = await fixture(t);
  await f.service.login({ username: 'FixtureSpace', password: 'Strong password 123!' });
  await f.service.upload('skin', { expectedRevision: 1, model: 'classic' });
  await f.service.apply('default');
  const root = path.join(f.root, 'default'),
    props = await fs.readFile(path.join(root, 'config/space-appearance.properties'));
  f.api.setOffline(true);
  await f.service.beforeLaunch('default', root, 'SpacePlayer', new AbortController().signal);
  assert.equal(f.nickname, 'SpacePlayer');
  assert.deepEqual(await fs.readFile(path.join(root, 'config/space-appearance.properties')), props);
  const loggedOut = await f.service.logout();
  assert.equal(loggedOut.profile, null);
  assert.match(loggedOut.message, /истечёт по сроку/);
  await assert.rejects(fs.readFile(path.join(f.root, 'auth/space-session.bin')));
  await f.service.beforeLaunch('default', root, 'SpacePlayer', new AbortController().signal);
  await f.service.beforeLaunch(
    'guest',
    path.join(f.root, 'guest'),
    'GuestPlayer',
    new AbortController().signal,
  );
  const skinHash = props.toString().match(/skinSha256=([a-f0-9]{64})/)[1];
  await fs.writeFile(path.join(root, `config/space-appearance/skin-${skinHash}.png`), 'corrupt');
  await assert.rejects(
    f.service.beforeLaunch('default', root, 'SpacePlayer', new AbortController().signal),
  );
});

import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import http from 'node:http';
import { createRequire } from 'node:module';
import { randomUUID, createHash } from 'node:crypto';
import { spawn, execFileSync } from 'node:child_process';
import { AccountHttp } from '../../apps/launcher/src/main/account/http.ts';
import { SpaceAccountService } from '../../apps/launcher/src/main/account/service.ts';

const repo = path.resolve(import.meta.dirname, '../..'),
  root = path.join(repo, '.reports/cross-client-account', 'run-' + randomUUID());
const require = createRequire(import.meta.url),
  { createApp } = require('../../apps/api/dist/app.js'),
  { png } = require('../../apps/api/test/account-fixtures.cjs');
await fs.mkdir(root, { recursive: true });
const api = await createApp({ accounts: { dataDir: path.join(root, 'database') } });
let child, log;
const checks = [],
  cookies = new Map();
const credentials = {
  username: 'Cross_' + randomUUID().replaceAll('-', '').slice(0, 14),
  password: 'Cross client ' + randomUUID(),
};
const nickname = 'QA_' + randomUUID().replaceAll('-', '').slice(0, 10),
  renamed = 'QA_' + randomUUID().replaceAll('-', '').slice(0, 10);
let localNickname = 'SavedGuest',
  savedSession = null,
  csrf = '',
  siteUrl;
// This check isolates wire/protocol behavior. Real Windows safeStorage is covered by Electron QA.
const memoryVault = {
  load: async () => savedSession,
  save: async (value) => {
    savedSession = value;
  },
  clear: async () => {
    savedSession = null;
  },
};
async function web(route, method = 'GET', body) {
  const response = await fetch(siteUrl + '/api/account/' + route, {
    method,
    headers: {
      Cookie: [...cookies].map(([k, v]) => k + '=' + v).join(';'),
      ...(method === 'GET'
        ? {}
        : { Origin: siteUrl, 'Content-Type': 'application/json', 'X-Space-CSRF': csrf }),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    redirect: 'error',
    signal: AbortSignal.timeout(12000),
  });
  for (const value of response.headers.getSetCookie()) {
    const [pair] = value.split(';');
    const [key, content] = pair.split('=');
    if (content) cookies.set(key, content);
    else cookies.delete(key);
  }
  const json = await response.json();
  assert.equal(JSON.stringify(json).includes('sessionToken'), false);
  if (json.csrfToken) csrf = json.csrfToken;
  return { status: response.status, json };
}
async function session() {
  const result = await web('session');
  assert.equal(result.status, 200);
  return result.json.profile;
}
try {
  await api.listen(0, '127.0.0.1');
  const apiUrl = await api.getUrl();
  const reservation = http.createServer();
  await new Promise((resolve) => reservation.listen(0, '127.0.0.1', resolve));
  const port = reservation.address().port;
  await new Promise((resolve) => reservation.close(resolve));
  siteUrl = 'http://127.0.0.1:' + port;
  log = await fs.open(path.join(root, 'next.log'), 'w');
  child = spawn(
    process.execPath,
    [
      path.join(repo, 'node_modules/next/dist/bin/next'),
      'start',
      path.join(repo, 'apps/website'),
      '--hostname',
      '127.0.0.1',
      '--port',
      String(port),
    ],
    {
      cwd: repo,
      env: {
        ...process.env,
        SPACE_API_URL: apiUrl,
        SPACE_ACCOUNT_API_URL: apiUrl,
        SPACE_SITE_URL: '',
        SPACE_SITE_INDEXABLE: 'false',
        NEXT_TELEMETRY_DISABLED: '1',
      },
      windowsHide: true,
      stdio: ['ignore', log.fd, log.fd],
    },
  );
  let exited = false;
  child.once('exit', () => {
    exited = true;
  });
  for (let i = 0; i < 100; i++) {
    if (exited) throw Error('Isolated Next process exited');
    try {
      if ((await fetch(siteUrl + '/account', { signal: AbortSignal.timeout(1000) })).ok) break;
    } catch {}
    if (i === 99) throw Error('Isolated Next did not become ready');
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  assert.equal(await session(), null);
  const registered = await web('register', 'POST', { ...credentials, nickname });
  assert.equal(registered.status, 200);
  const id = registered.json.profile.id;
  const cookie = cookies.get('space_session');
  assert.equal(typeof cookie === 'string' && /^[A-Za-z0-9_-]{43}$/.test(cookie), true);
  assert.equal(JSON.stringify(registered.json).includes(cookie), false);
  const game = path.join(root, 'game');
  await fs.mkdir(game, { recursive: true });
  const capePath = path.join(root, 'cape.png');
  await fs.writeFile(capePath, png(128, 64));
  const launcher = new SpaceAccountService(new AccountHttp(apiUrl), memoryVault, {
    rootForProfile: async () => game,
    localNickname: () => localNickname,
    saveNickname: async (value) => {
      localNickname = value;
    },
    choosePng: async () => capePath,
  });
  let state = await launcher.login(credentials);
  assert.equal(state.profile.id, id);
  assert.equal(localNickname, 'SavedGuest');
  checks.push(
    'Register on website; log into the same persistent account through actual launcher main service; guest identity stays unchanged.',
  );
  let result = await web('me', 'PATCH', {
    nickname: renamed,
    expectedRevision: state.profile.revision,
  });
  assert.equal(result.status, 200);
  state = await launcher.status();
  assert.equal(state.profile.nickname, renamed);
  assert.equal(localNickname, 'SavedGuest');
  result = await web('me/skin', 'PUT', {
    pngBase64: png().toString('base64'),
    model: 'slim',
    expectedRevision: state.profile.revision,
  });
  assert.equal(result.status, 200);
  state = await launcher.status();
  assert.equal(state.profile.skin.sha256, result.json.profile.skin.sha256);
  state = await launcher.upload('cape', { expectedRevision: state.profile.revision });
  const publicProfile = await session();
  assert.equal(publicProfile.cape.sha256, state.profile.cape.sha256);
  assert.equal(publicProfile.skin.sha256, state.profile.skin.sha256);
  const stale = await web('me', 'PATCH', { nickname, expectedRevision: 1 });
  assert.equal(stale.status, 409);
  assert.equal(stale.json.code, 'SPACE_REVISION_CONFLICT');
  checks.push(
    'Website nickname/skin changes reach launcher; launcher cape reaches website; stale revision is rejected.',
  );
  await launcher.apply('default');
  assert.equal(localNickname, renamed);
  const config = path.join(game, 'config'),
    properties = await fs.readFile(path.join(config, 'space-appearance.properties'), 'utf8');
  for (const secret of [credentials.password, savedSession.sessionToken, cookie, apiUrl])
    assert.equal(properties.includes(secret), false);
  const keys = properties
    .trim()
    .split('\n')
    .map((line) => line.split('=')[0]);
  assert.deepEqual(
    keys.sort(),
    [
      'schemaVersion',
      'accountId',
      'username',
      'nickname',
      'skinSha256',
      'skinModel',
      'capeSha256',
    ].sort(),
  );
  for (const kind of ['skin', 'cape']) {
    const asset = state.profile[kind],
      bytes = await fs.readFile(
        path.join(config, 'space-appearance', kind + '-' + asset.sha256 + '.png'),
      );
    assert.equal(createHash('sha256').update(bytes).digest('hex'), asset.sha256);
  }
  const classes = path.join(root, 'parser-classes'),
    gameClasses = path.join(repo, 'game/build/classes/java/main');
  await fs.mkdir(classes);
  execFileSync(
    path.join(repo, '.tools/jdk-21/bin/javac.exe'),
    [
      '-cp',
      gameClasses,
      '-d',
      classes,
      path.join(repo, 'scripts/qa/cosmetics/src/dev/spaceclient/qa/PublicAppearanceCheck.java'),
    ],
    { windowsHide: true, stdio: 'pipe' },
  );
  const java = execFileSync(
    path.join(repo, '.tools/jdk-21/bin/java.exe'),
    [
      '-Djava.awt.headless=true',
      '-cp',
      classes + path.delimiter + gameClasses,
      'dev.spaceclient.qa.PublicAppearanceCheck',
      config,
      renamed,
      'slim',
    ],
    { windowsHide: true, encoding: 'utf8' },
  );
  assert.equal(java.trim(), 'PUBLIC_APPEARANCE_OK');
  checks.push(
    'Explicit Apply writes only public fixed paths and verified canonical PNGs; production game AppearanceProfile accepts the actual launcher export.',
  );
  assert.equal((await web('logout', 'POST', {})).status, 200);
  assert.equal(await session(), null);
  assert.equal((await launcher.status()).profile.id, id);
  await launcher.logout();
  assert.equal((await launcher.status()).profile, null);
  assert.equal(localNickname, renamed);
  checks.push(
    'Website logout revokes its session without revoking launcher session; launcher logout preserves the explicitly applied local nickname.',
  );
  await fs.writeFile(
    path.join(root, 'report.json'),
    JSON.stringify(
      {
        ok: true,
        checks,
        javaParser: true,
        realWindowsEncryption: false,
        realMinecraftSpawn: false,
        createdAt: new Date().toISOString(),
      },
      null,
      2,
    ) + '\n',
  );
  console.log(
    JSON.stringify({
      ok: true,
      checks: checks.length,
      javaParser: true,
      report: path.join(root, 'report.json'),
    }),
  );
} finally {
  if (child) {
    child.kill();
    await new Promise((resolve) => {
      if (child.exitCode !== null) return resolve();
      const timer = setTimeout(resolve, 5000);
      child.once('exit', () => {
        clearTimeout(timer);
        resolve();
      });
    });
  }
  await log?.close();
  await api.close();
  savedSession = null;
  cookies.clear();
}

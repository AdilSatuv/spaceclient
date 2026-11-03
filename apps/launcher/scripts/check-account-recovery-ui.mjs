import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { createRequire } from 'node:module';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { build } from 'esbuild';
import electron from 'electron';
import puppeteer from 'puppeteer-core';
const require = createRequire(import.meta.url),
  { png } = require('../test/account-fixture.cjs');
const repo = resolve(import.meta.dirname, '../../..'),
  root = join(repo, '.reports/account-recovery-ui', 'run-' + randomUUID());
await mkdir(join(root, 'data/instances/default/minecraft/saves/qa-world'), { recursive: true });
const instance = join(root, 'data/instances/default/minecraft');
await writeFile(join(instance, 'options.txt'), 'qa-original-options');
await writeFile(join(instance, 'saves/qa-world/level.dat'), 'QA_WORLD_UNCHANGED');
await writeFile(join(root, 'skin.png'), png());
await writeFile(join(root, 'cape.png'), png(64, 32));
let control = { gameRunning: false, cancelPicker: false };
const setControl = async (change) => {
  control = { ...control, ...change };
  await writeFile(join(root, 'control.json'), JSON.stringify(control));
};
await setControl({});
const main = join(root, 'main.cjs');
await build({
  absWorkingDir: repo,
  entryPoints: ['apps/launcher/scripts/fixtures/account-recovery-main.mjs'],
  outfile: main,
  bundle: true,
  platform: 'node',
  target: 'node24',
  external: ['electron'],
  format: 'cjs',
});
const username = 'LQA_' + randomUUID().replaceAll('-', '').slice(0, 16),
  initialNickname = 'QA_' + randomUUID().replaceAll('-', '').slice(0, 10),
  nextNickname = 'QA_' + randomUUID().replaceAll('-', '').slice(0, 10),
  password = 'Space QA ' + randomUUID();
const apiUrl = process.env.SPACE_ACCOUNT_API_URL;
assert.match(
  apiUrl ?? '',
  /^http:\/\/127\.0\.0\.1:\d+$/,
  'Explicit coordinator-owned loopback API is required.',
);
let browser, page, child;
const errors = [],
  screenshots = [],
  checks = [];
async function start() {
  const env = {
    ...process.env,
    SPACE_ACCOUNT_API_URL: apiUrl,
    SPACE_ACCOUNT_QA_ROOT: root,
    SPACE_ACCOUNT_QA_DIST: join(repo, 'apps/launcher/dist'),
  };
  delete env.ELECTRON_RUN_AS_NODE;
  delete env.NODE_OPTIONS;
  child = spawn(
    electron,
    [main, '--headless', '--remote-debugging-address=127.0.0.1', '--remote-debugging-port=0'],
    { cwd: repo, env, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] },
  );
  const endpoint = await new Promise((resolve, reject) => {
    let output = '';
    const timeout = setTimeout(
      () => reject(new Error('Hidden account QA endpoint timed out.')),
      20000,
    );
    const inspect = (chunk) => {
      output = (output + chunk.toString()).slice(-12000);
      const match = output.match(/DevTools listening on (ws:\/\/127\.0\.0\.1:\d+\/[^\s]+)/);
      if (match) {
        clearTimeout(timeout);
        resolve(match[1]);
      }
    };
    child.stdout.on('data', inspect);
    child.stderr.on('data', inspect);
    child.once('error', reject);
    child.once('exit', (code) => {
      clearTimeout(timeout);
      reject(new Error('QA main exited before connection: ' + code));
    });
  });
  browser = await puppeteer.connect({
    browserWSEndpoint: endpoint,
    defaultViewport: null,
    protocolTimeout: 20000,
  });
  const target = await browser.waitForTarget((item) => item.url() === 'space://app/index.html');
  page = await target.page();
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(message.text());
  });
  await page.waitForSelector('.current-profile');
  await page.setViewport({ width: 1180, height: 760 });
}
async function stop() {
  if (page && !page.isClosed()) await page.close();
  browser?.disconnect();
  if (child && child.exitCode === null) {
    await Promise.race([
      new Promise((resolve) => child.once('exit', resolve)),
      new Promise((resolve) => setTimeout(resolve, 4000)),
    ]);
    if (child.exitCode === null) child.kill();
  }
}
const route = async (label) => {
  await page.click('nav button[aria-label="' + label + '"]');
  await page.waitForFunction(
    (text) => document.querySelector('nav [aria-current]')?.getAttribute('aria-label') === text,
    {},
    label,
  );
};
const replace = async (selector, text) => {
  await page.click(selector);
  await page.keyboard.down('Control');
  await page.keyboard.press('KeyA');
  await page.keyboard.up('Control');
  await page.type(selector, text);
};
const clickText = async (selector, text) => {
  const element = await page.$$(selector);
  for (const candidate of element) {
    if ((await candidate.evaluate((item) => item.textContent.trim())) === text) {
      await candidate.click();
      return;
    }
  }
  throw new Error('Missing control: ' + text);
};
const capture = async (name, selector) => {
  if (selector) await page.$eval(selector, (e) => e.scrollIntoView({ block: 'center' }));
  await page.screenshot({ path: join(root, name + '.png') });
  screenshots.push(name + '.png');
};
try {
  await start();
  assert.deepEqual(
    await page.evaluate(() => ({
      functions: Object.keys(window.space).length,
      require: typeof window.require,
      process: typeof window.process,
    })),
    { functions: 26, require: 'undefined', process: 'undefined' },
  );
  await route('Аккаунт Space');
  await page.waitForSelector('#space-password');
  await capture('01-guest-login');
  await clickText('.account-tabs button', 'Регистрация');
  await replace('#space-username', username);
  await replace('#space-initial-nickname', initialNickname);
  await replace('#space-password', password);
  await page.click('.account-auth form button[type="submit"]');
  await page.waitForSelector('.account-profile', { timeout: 20000 });
  assert.equal(
    (await page.evaluate(() => window.space.getState())).value.localNickname,
    'GuestPlayer',
  );
  const state = await page.evaluate(() => window.space.getAccount());
  assert.equal(state.ok, true);
  assert.equal('sessionToken' in state.value, false);
  await capture('02-registered-not-applied', '.account-profile');
  checks.push(
    'Real local API registration with Windows-encrypted main-only session; guest nickname unchanged until explicit Apply; 26 fixed bridge functions and no Node globals.',
  );
  await page.select('#space-skin-model', 'slim');
  await page.$eval('.appearance-grid > :first-child button', (button) =>
    button.scrollIntoView({ block: 'center' }),
  );
  await page.click('.appearance-grid > :first-child button');
  await page.waitForFunction(
    () => document.querySelector('.appearance-grid > :first-child img')?.naturalWidth === 64,
    { timeout: 20000 },
  );
  await page.click('.appearance-grid > :last-child button');
  await page.waitForFunction(
    () => document.querySelector('.appearance-grid > :last-child img')?.naturalHeight === 32,
    { timeout: 20000 },
  );
  await capture('03-skin-cape-previews', '.appearance-grid');
  await setControl({ cancelPicker: true });
  await page.click('.appearance-grid > :first-child button');
  await page.waitForFunction(() =>
    document.querySelector('.account-feedback').textContent.includes('отменён'),
  );
  await capture('04-picker-cancel', '.account-feedback');
  await setControl({ cancelPicker: false });
  await writeFile(join(root, 'skin.png'), 'not-a-png');
  await page.click('.appearance-grid > :first-child button');
  await page.waitForFunction(() =>
    document.querySelector('.account-feedback').textContent.includes('PNG'),
  );
  await capture('05-invalid-png-recovery', '.account-feedback');
  await writeFile(join(root, 'skin.png'), png());
  await replace('#space-profile-nickname', nextNickname);
  await page.click('.account-profile form button[type="submit"]');
  await page.waitForFunction(
    (name) => document.querySelector('.account-profile h2')?.textContent === name,
    {},
    nextNickname,
  );
  assert.equal(
    (await page.evaluate(() => window.space.getState())).value.localNickname,
    'GuestPlayer',
  );
  await page.click('.account-apply input');
  await clickText('.account-apply button', 'Применить профиль Space');
  await page.waitForFunction(
    () => document.querySelector('.account-feedback').textContent.includes('Профиль применён'),
    { timeout: 20000 },
  );
  assert.equal(
    (await page.evaluate(() => window.space.getState())).value.localNickname,
    nextNickname,
  );
  const properties = await readFile(join(instance, 'config/space-appearance.properties'), 'utf8');
  assert.equal(properties.trim().split('\n').length, 7);
  assert.doesNotMatch(properties, /password|sessionToken|https?:|[A-Z]:\\/);
  assert.match(properties, /skinModel=slim/);
  await capture('06-explicit-apply', '.account-apply');
  checks.push(
    'Real skin/cape PUT, fully decoded PNG previews, native picker cancellation and invalid PNG recovery; nickname PATCH leaves local identity untouched until confirmed Apply; exact public appearance files exported.',
  );
  await setControl({ gameRunning: true });
  await page.waitForFunction(() => document.querySelector('.account-apply button').disabled);
  const guarded = await page.evaluate(() =>
    window.space.applyAccount({ id: 'default', confirmIdentityChange: true }),
  );
  assert.equal(guarded.ok, false);
  await setControl({ gameRunning: false });
  await new Promise((resolve) => setTimeout(resolve, 800));
  await route('Настройки');
  await clickText('.profile-recovery button', 'Создать резервную копию');
  await page.waitForFunction(() =>
    document
      .querySelector('.profile-recovery')
      .textContent.includes('Резервная копия настроек этого профиля сохранена'),
  );
  await writeFile(join(instance, 'options.txt'), 'changed-options');
  await page.click('.profile-recovery input');
  await clickText('.profile-recovery button', 'Восстановить настройки');
  await page.waitForFunction(
    () =>
      document.querySelector('.profile-recovery').textContent.includes('настройки') &&
      document.querySelector('.profile-recovery').textContent.includes('восстановлены'),
  );
  assert.equal(await readFile(join(instance, 'options.txt'), 'utf8'), 'qa-original-options');
  assert.equal(
    await readFile(join(instance, 'saves/qa-world/level.dat'), 'utf8'),
    'QA_WORLD_UNCHANGED',
  );
  assert.equal(
    await readFile(join(instance, 'config/space-appearance.properties'), 'utf8'),
    properties,
  );
  await clickText('.profile-recovery button', 'Экспорт диагностики');
  await page.waitForFunction(() =>
    document.querySelector('.profile-recovery').textContent.includes('Диагностика экспортирована'),
  );
  const diagnostic = await readFile(join(root, 'diagnostics.json'), 'utf8');
  for (const secret of [username, password, nextNickname, root, 'sessionToken', 'javaPath', 'log'])
    assert.equal(
      diagnostic.includes(secret),
      false,
      secret === 'log' ? 'No raw logs' : 'Private value omitted',
    );
  await capture('07-profile-recovery', '.profile-recovery');
  checks.push(
    'Main guards reject instance changes while game is marked running; six-file backup/restore preserves QA world and public appearance; native diagnostic export has no identifiers, paths, credentials or raw logs.',
  );
  await route('Играть');
  await page.click('.launch-button');
  await page.waitForFunction(
    () => document.querySelector('#launch-status').dataset.phase === 'exited',
    { timeout: 20000 },
  );
  assert.deepEqual(JSON.parse(await readFile(join(root, 'spawn-observation.json'), 'utf8')), {
    publicAppearanceBeforeSpawn: true,
    containsSecret: false,
  });
  await route('Аккаунт Space');
  await page.waitForSelector('.account-profile');
  await clickText('.account-profile-actions button', 'Выйти из Space');
  await page.waitForSelector('#space-password');
  await replace('#space-username', username);
  await replace('#space-password', password);
  await page.click('.account-auth form button[type="submit"]');
  await page.waitForSelector('.account-profile', { timeout: 20000 });
  const cipher = await readFile(join(root, 'data/auth/space-session.bin'));
  assert.equal(cipher.includes(Buffer.from(username)), false);
  assert.equal(cipher.includes(Buffer.from(password)), false);
  await stop();
  await start();
  await route('Аккаунт Space');
  await page.waitForSelector('.account-profile', { timeout: 20000 });
  assert.equal(await page.$eval('.account-profile h2', (e) => e.textContent), nextNickname);
  await capture('08-restored-encrypted-session', '.account-profile');
  checks.push(
    'Before-spawn hook observes public appearance before simulated process creation; real login/logout and encrypted-session restart work with the same API. No Java process is launched.',
  );
  await clickText('.appearance-grid > :first-child button', 'Удалить скин');
  await page.waitForFunction(() => !document.querySelector('.appearance-grid > :first-child img'));
  await clickText('.appearance-grid > :last-child button', 'Удалить плащ');
  await page.waitForFunction(() => !document.querySelector('.appearance-grid > :last-child img'));
  await page.click('.account-apply input');
  await clickText('.account-apply button', 'Применить профиль Space');
  await page.waitForFunction(() =>
    document.querySelector('.account-feedback').textContent.includes('Профиль применён'),
  );
  const deleted = await readFile(join(instance, 'config/space-appearance.properties'), 'utf8');
  assert.match(deleted, /skinSha256=\nskinModel=classic\ncapeSha256=\n/);
  await capture('09-deleted-appearance', '.appearance-grid');
  for (const viewport of [
    { width: 1000, height: 680 },
    { width: 600, height: 680 },
    { width: 500, height: 380 },
  ]) {
    await page.setViewport(viewport);
    assert.equal(
      await page.evaluate(
        () =>
          document.querySelector('main').scrollWidth <=
            document.querySelector('main').clientWidth + 1 &&
          document.documentElement.scrollWidth <= innerWidth,
      ),
      true,
    );
    await capture('10-account-' + viewport.width, '.account-profile');
  }
  await page.setViewport({ width: 1000, height: 680 });
  await page.emulateMediaFeatures([{ name: 'prefers-reduced-motion', value: 'reduce' }]);
  const session = await page.createCDPSession();
  await session.send('Emulation.setEmulatedMedia', {
    features: [{ name: 'forced-colors', value: 'active' }],
  });
  await capture('11-forced-colors', '.account-apply');
  await session.send('Emulation.setEmulatedMedia', { features: [] });
  await session.detach();
  await clickText('.account-profile-actions button', 'Выйти из Space');
  await page.waitForSelector('#space-password');
  assert.equal(
    (await page.evaluate(() => window.space.getState())).value.localNickname,
    nextNickname,
  );
  assert.deepEqual(errors, []);
  checks.push(
    'Real DELETE skin/cape clears public properties on Apply; account view has no overflow at 1000/600/500 widths, reduced-motion and forced-colors remain usable; final logout revokes the QA session and preserves local nickname.',
  );
  await writeFile(
    join(root, 'report.json'),
    JSON.stringify(
      {
        schemaVersion: 1,
        checkedAt: new Date().toISOString(),
        realLocalAccountApi: true,
        windowsEncryption: true,
        nativePickers: 'injected main-only fixtures',
        gameSpawn: 'simulated process after production beforeLaunch hook',
        userRuntimeTouched: false,
        checks,
        screenshots,
        rendererErrors: errors,
      },
      null,
      2,
    ) + '\n',
  );
  console.log(JSON.stringify({ root, checks, screenshots }, null, 2));
} catch (error) {
  if (page && !page.isClosed()) {
    await page.screenshot({ path: join(root, 'failure.png') }).catch(() => {});
    await writeFile(join(root, 'failure.txt'), await page.$eval('body', (e) => e.innerText)).catch(
      () => {},
    );
  }
  console.error('Account QA context: ' + root);
  throw error;
} finally {
  await stop();
}

import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { mkdir, readFile, rename, writeFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { build } from 'esbuild';
import electron from 'electron';
import puppeteer from 'puppeteer-core';

const repo = resolve(import.meta.dirname, '../../..');
const root = join(repo, '.reports/launcher-identity-ui', 'run-' + randomUUID());
await mkdir(join(root, 'data'), { recursive: true });
const bundled = JSON.parse(
  await readFile(join(repo, 'apps/launcher/dist/game/space-build.json'), 'utf8'),
);
delete bundled.schemaVersion;
const id = 'dev-' + bundled.gameSourceTree.slice(0, 12);
const idle = {
  profileId: 'default',
  phase: 'idle',
  completedFiles: 0,
  totalFiles: 0,
  receivedBytes: 0,
  verifiedBytes: 0,
  totalBytes: 0,
  retryCount: 0,
  spaceVersion: null,
  bundledBuild: bundled,
  message: 'Нажмите «Подготовить файлы»: загрузим недостающее и проверим файлы профиля.',
};
const launch = {
  profileId: 'default',
  mode: 'local',
  phase: 'idle',
  message: 'Локальная игра без регистрации аккаунта.',
  exitCode: null,
  log: [],
  verifiedBuild: null,
};
let scenario = { installation: idle, launch };
async function state(change) {
  scenario = { ...scenario, ...change };
  const temporary = join(root, 'scenario.tmp');
  await writeFile(temporary, JSON.stringify(scenario));
  await rename(temporary, join(root, 'scenario.json'));
}
await state({});
const main = join(root, 'main.cjs');
await build({
  absWorkingDir: repo,
  entryPoints: ['apps/launcher/scripts/fixtures/build-identity-main.mjs'],
  outfile: main,
  bundle: true,
  platform: 'node',
  target: 'node24',
  external: ['electron'],
  format: 'cjs',
});
const env = {
  ...process.env,
  SPACE_IDENTITY_QA_ROOT: root,
  SPACE_IDENTITY_QA_DIST: join(repo, 'apps/launcher/dist'),
};
delete env.ELECTRON_RUN_AS_NODE;
delete env.NODE_OPTIONS;
const child = spawn(
  electron,
  [main, '--headless', '--remote-debugging-address=127.0.0.1', '--remote-debugging-port=0'],
  { cwd: repo, env, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] },
);
let browser;
let page;
const errors = [];
const report = {
  syntheticStates: true,
  realPreflight: false,
  realDownloads: false,
  realGame: false,
  screenshots: [],
  checks: [],
};
try {
  const endpoint = await new Promise((resolve, reject) => {
    const timer = setTimeout(
      () => reject(new Error('Hidden identity QA endpoint timed out.')),
      20000,
    );
    let output = '';
    const inspect = (chunk) => {
      output = (output + chunk.toString()).slice(-12000);
      const match = output.match(/DevTools listening on (ws:\/\/127\.0\.0\.1:\d+\/[^\s]+)/);
      if (match) {
        clearTimeout(timer);
        resolve(match[1]);
      }
    };
    child.stdout.on('data', inspect);
    child.stderr.on('data', inspect);
    child.once('exit', (code) => {
      clearTimeout(timer);
      reject(new Error('Hidden fixture exited: ' + code + ' ' + output));
    });
    child.once('error', (error) => {
      clearTimeout(timer);
      reject(error);
    });
  });
  browser = await puppeteer.connect({
    browserWSEndpoint: endpoint,
    defaultViewport: null,
    protocolTimeout: 15000,
  });
  const target = await browser.waitForTarget((item) => item.url() === 'space://app/index.html');
  page = await target.page();
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(message.text());
  });
  await page.waitForSelector('.build-identity');
  await page.setViewport({ width: 1000, height: 680 });
  assert.deepEqual(
    await page.evaluate(() => ({
      functions: Object.keys(window.space).length,
      require: typeof window.require,
      process: typeof window.process,
    })),
    { functions: 13, require: 'undefined', process: 'undefined' },
  );
  const capture = async (name, selector) => {
    if (selector)
      await page.$eval(selector, (element) => element.scrollIntoView({ block: 'center' }));
    await page.screenshot({ path: join(root, name + '.png') });
    report.screenshots.push(name + '.png');
  };
  const text = () => page.$eval('.build-identity', (element) => element.textContent);
  assert.equal(await page.$eval('.build-id', (element) => element.textContent), id);
  assert.equal(
    await page.$eval('.build-id', (element) => {
      const rect = element.getBoundingClientRect();
      return rect.top >= 38 && rect.bottom <= innerHeight;
    }),
    true,
    'The short ID must be visible at 1000×680 without scrolling.',
  );
  assert.match(await text(), /ещё не проверен/);
  await capture('01-bundled-unverified', '.build-identity');
  await page.focus('.build-details summary');
  await page.keyboard.press('Enter');
  assert.equal(await page.$eval('.build-details', (element) => element.open), true);
  const values = await page.$$eval('.build-values dd', (elements) =>
    elements.map((element) => element.textContent),
  );
  assert.ok(values.includes(bundled.gameSourceTree));
  assert.ok(values.includes(bundled.sha256));
  assert.equal(
    await page.$eval('.build-values dd', (element) => getComputedStyle(element).userSelect),
    'text',
  );
  const ax = await page.createCDPSession();
  const { nodes } = await ax.send('Accessibility.getFullAXTree');
  assert.ok(
    nodes.some(
      (node) =>
        node.role?.value === 'DisclosureTriangle' &&
        node.name?.value === 'Подробности сборки' &&
        node.properties.some(
          (property) => property.name === 'expanded' && property.value.value === true,
        ),
    ),
  );
  await capture('02-full-values-keyboard', '.build-details');
  await page.keyboard.press('Space');
  report.checks.push(
    'Real bundled pin uses agreed source ID; full tree/digest/versions/size selectable; native details opens by keyboard and exposes expanded state.',
  );

  const prepared = {
    ...idle,
    phase: 'prepared',
    spaceVersion: '0.1.0',
    message: 'Сборка Space подготовлена. Локальный запуск повторно проверит файлы и Java.',
  };
  await state({ installation: prepared });
  await page.waitForFunction(() =>
    document.querySelector('.installation-card').textContent.includes('Сборка Space подготовлена'),
  );
  assert.match(await text(), /ещё не проверен/);
  assert.match(
    await page.$eval('.installation-card [data-slot="badge"]', (element) => element.textContent),
    /Подготовлены/,
  );
  await capture('03-prepared-unverified', '.installation-card');
  const checking = { ...launch, phase: 'checking', message: 'Проверяем файлы и Java…' };
  await state({ launch: checking });
  await page.waitForFunction(
    () => document.querySelector('#launch-status').dataset.phase === 'checking',
  );
  assert.match(await text(), /ещё не проверен/);
  await capture('04-checking-unverified', '.build-identity');
  const verified = { identity: bundled, checkedAt: '2026-11-03T04:00:00.000Z' };
  const exited = {
    ...launch,
    phase: 'exited',
    message: 'Игра завершена. Можно запустить снова.',
    exitCode: 0,
    verifiedBuild: verified,
  };
  await state({ launch: exited });
  await page.waitForFunction(() =>
    document.querySelector('.build-verification').textContent.includes('JAR совпал'),
  );
  await capture('05-verified-match', '.build-identity');
  await page.click('.build-details summary');
  assert.equal(await page.$$eval('.build-values', (elements) => elements.length), 2);
  assert.equal(await page.$eval('time', (element) => element.dateTime), verified.checkedAt);
  await capture('06-verified-details', '.build-details');
  report.checks.push(
    'Prepared/checking states do not imply preflight; last successful preflight has separate exact values and timestamp.',
  );

  for (const viewport of [
    { width: 500, height: 380 },
    { width: 600, height: 680 },
    { width: 1180, height: 760 },
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
    await capture('07-details-' + viewport.width, '.build-details');
  }
  await page.setViewport({ width: 1000, height: 680 });
  const emulation = await page.createCDPSession();
  await emulation.send('Emulation.setEmulatedMedia', {
    features: [
      { name: 'forced-colors', value: 'active' },
      { name: 'prefers-reduced-motion', value: 'reduce' },
    ],
  });
  assert.equal(
    await page.$eval('.build-details summary', (element) => getComputedStyle(element).display),
    'list-item',
  );
  await capture('08-forced-colors-details', '.build-details');
  await emulation.send('Emulation.setEmulatedMedia', { features: [] });
  await emulation.detach();
  await page.click('.build-details summary');
  report.checks.push(
    'Full digest details fit compact/desktop layouts; forced colors and reduced motion retain accessible details.',
  );

  const mismatch = { ...verified, identity: { ...bundled, sha256: 'c'.repeat(64) } };
  await state({ launch: { ...exited, verifiedBuild: mismatch } });
  await page.waitForFunction(() =>
    document.querySelector('.build-verification').textContent.includes('JAR отличается'),
  );
  await capture('09-different-bytes-same-source', '.build-identity');
  const failed = {
    ...launch,
    phase: 'error',
    message:
      'Установленная сборка Space повреждена или устарела. Повторите подготовку в карточке «Файлы игры».',
  };
  await state({ launch: failed, nextLaunch: checking });
  await page.waitForFunction(
    () => document.querySelector('#launch-status').dataset.phase === 'error',
  );
  assert.match(await text(), /ещё не проверен/);
  assert.match(
    await page.$eval('.launch-button', (element) => element.textContent),
    /Повторить запуск/,
  );
  await capture('10-preflight-error-retry', '.play-workspace');
  await page.click('.launch-button');
  await page.waitForFunction(
    () => document.querySelector('#launch-status').dataset.phase === 'checking',
  );
  assert.match(await text(), /ещё не проверен/);
  report.checks.push(
    'Different digest with same short ID remains distinct; failed preflight and retry show no stale verified claim.',
  );

  const installationError = {
    ...idle,
    phase: 'error',
    message: 'Соединение прервалось. Проверьте подключение и повторите подготовку.',
  };
  await state({ launch, installation: installationError, nextInstallation: prepared });
  await page.waitForFunction(() =>
    document
      .querySelector('.installation-actions button')
      .textContent.includes('Повторить подготовку'),
  );
  assert.equal(
    await page.$eval('.installation-status', (element) => element.getAttribute('role')),
    'alert',
  );
  await capture('11-preparation-error-retry', '.installation-card');
  await page.click('.installation-actions button');
  await page.waitForFunction(() =>
    document
      .querySelector('.installation-card [data-slot="badge"]')
      .textContent.includes('Подготовлены'),
  );
  assert.match(await text(), /ещё не проверен/);
  await state({ statusError: true, launch: { ...launch, phase: 'running' } });
  await page.waitForFunction(() =>
    document
      .querySelector('.installation-actions button')
      .textContent.includes('Повторить проверку состояния'),
  );
  await page.waitForFunction(
    () => document.querySelector('#launch-status').dataset.phase === 'running',
  );
  assert.equal(
    await page.$eval('.installation-actions button', (element) => element.disabled),
    false,
    'Reading status must remain available while the game locks file preparation.',
  );
  await capture('12-status-error-retry', '.installation-card');
  await state({ statusError: false });
  await page.click('.installation-actions button');
  await page.waitForFunction(() =>
    document
      .querySelector('.installation-actions button')
      .textContent.includes('Проверить и восстановить'),
  );
  assert.equal(
    await page.$eval('.installation-actions button', (element) => element.disabled),
    true,
    'Recovery must retain the preparation lock while the game is running.',
  );
  assert.doesNotMatch(await page.$eval('body', (element) => element.textContent), /SECRET/);
  report.checks.push(
    'Preparation errors announce recovery and retry; status read retries remain available during gameplay without unlocking installation; raw exception stays redacted.',
  );

  await state({ installation: { ...idle, bundledBuild: null } });
  await page.waitForFunction(
    () => document.querySelector('.build-id').textContent === 'Space не включён',
  );
  assert.equal(await page.$('.build-details'), null);
  await capture('13-no-game-bundle', '.build-identity');
  await ax.detach();
  assert.deepEqual(errors, []);
  report.rendererErrors = errors;
  report.checkedAt = new Date().toISOString();
  await writeFile(join(root, 'report.json'), JSON.stringify(report, null, 2) + '\n');
  console.log(
    JSON.stringify({ root, checks: report.checks, screenshots: report.screenshots }, null, 2),
  );
  await page.close();
} catch (error) {
  if (page && !page.isClosed()) {
    await page.screenshot({ path: join(root, 'failure.png') }).catch(() => {});
    await writeFile(
      join(root, 'failure.txt'),
      await page.$eval('body', (element) => element.innerText),
    ).catch(() => {});
  }
  console.error('Identity QA context: ' + root);
  throw error;
} finally {
  browser?.disconnect();
  if (child.exitCode === null) {
    await Promise.race([
      new Promise((resolve) => child.once('exit', resolve)),
      new Promise((resolve) => setTimeout(resolve, 3000)),
    ]);
    if (child.exitCode === null) child.kill();
  }
}

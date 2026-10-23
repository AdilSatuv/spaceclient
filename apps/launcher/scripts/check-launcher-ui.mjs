import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { randomUUID } from 'node:crypto';
import electron from 'electron';
import puppeteer from 'puppeteer-core';

// Own development instance only. Never attaches to the user's browser or launcher session.
const repo = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const root = join(repo, '.reports/launcher-ui', 'run-' + randomUUID());
const data = join(root, 'data');
await mkdir(data, { recursive: true });
const env = { ...process.env, SPACE_LAUNCHER_DATA: data };
delete env.ELECTRON_RUN_AS_NODE;
delete env.NODE_OPTIONS;
const child = spawn(
  electron,
  [
    join(repo, 'apps/launcher'),
    '--headless',
    '--remote-debugging-port=0',
    '--remote-debugging-address=127.0.0.1',
  ],
  { env, cwd: repo, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] },
);
let browser;
let page;
const errors = [];
const report = {
  schemaVersion: 1,
  realAccountUsed: false,
  realDownloadsStarted: false,
  screenshots: [],
  checks: [],
};
try {
  const endpoint = await new Promise((resolve, reject) => {
    const timer = setTimeout(
      () => reject(new Error('Development launcher did not expose its temporary test endpoint.')),
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
    child.stderr.on('data', inspect);
    child.stdout.on('data', inspect);
    child.once('error', () => {
      clearTimeout(timer);
      reject(new Error('Could not start development launcher.'));
    });
    child.once('exit', (code) => {
      clearTimeout(timer);
      reject(new Error('Development launcher exited before inspection: ' + code));
    });
  });
  browser = await puppeteer.connect({ browserWSEndpoint: endpoint, defaultViewport: null });
  const target = await browser.waitForTarget((item) => item.url() === 'space://app/index.html', {
    timeout: 15000,
  });
  page = await target.page();
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(message.text());
  });
  await page.waitForSelector('.current-profile');
  assert.deepEqual(
    await page.evaluate(() => ({
      require: typeof window.require,
      process: typeof window.process,
      functions: Object.keys(window.space).length,
    })),
    { require: 'undefined', process: 'undefined', functions: 10 },
  );
  report.checks.push('Sandbox bridge remains limited to ten functions; no renderer Node globals.');
  await page.focus('.skip-link');
  await page.keyboard.press('Enter');
  assert.equal(await page.evaluate(() => document.activeElement.id), 'main');
  assert.equal(page.url(), 'space://app/index.html');
  report.checks.push('Skip control focuses main without changing the exact trusted IPC URL.');
  const route = async (view) => {
    await page.click('button[aria-label="' + view + '"]');
    await page.waitForFunction(
      (label) =>
        document.querySelector('nav button[aria-current]')?.getAttribute('aria-label') === label,
      {},
      view,
    );
  };
  const replaceName = async (name) => {
    await page.click('#profile-name');
    await page.keyboard.down('Control');
    await page.keyboard.press('KeyA');
    await page.keyboard.up('Control');
    await page.keyboard.type(name);
  };
  const save = async (name) => {
    await replaceName(name);
    await page.click('form button[type="submit"]');
    await page.waitForFunction(
      (expected) =>
        document.querySelector('form button[type="submit"]').disabled &&
        !document.querySelector('#profile-name').disabled &&
        document.querySelector('#profile-name').value === expected &&
        document.querySelector('.feedback-slot').textContent.includes('Профиль сохранён'),
      {},
      name,
    );
  };
  await route('Профили');
  await save('Основной');
  await page.focus('#profile-name');
  await page.keyboard.press('Tab');
  assert.equal(await page.evaluate(() => document.activeElement.id), 'memory');
  const memory = await page.$eval('#memory', (input) => Number(input.value));
  await page.keyboard.press('ArrowRight');
  assert.equal(await page.$eval('#memory', (input) => Number(input.value)), memory + 256);
  assert.equal(await page.$eval('.create-profile', (button) => button.disabled), true);
  await page.click('.profile-option[aria-pressed="true"]');
  assert.equal(await page.$eval('#memory', (input) => Number(input.value)), memory + 256);
  await page.click('form button[type="button"]');
  assert.equal(await page.$eval('#memory', (input) => Number(input.value)), memory);
  report.checks.push(
    'Keyboard RAM editing, cancel, dirty lock and active-profile click retain their behavior.',
  );
  await page.click('.create-profile');
  await page.waitForFunction(
    () =>
      document.querySelectorAll('.profile-option').length === 2 &&
      !document.querySelector('#profile-name').disabled,
  );
  await save('Для PvP');
  await page.click('.profile-option:first-child');
  await page.waitForFunction(() => document.querySelector('#profile-name').value === 'Основной');
  const settings = JSON.parse(await readFile(join(data, 'settings.json'), 'utf8'));
  assert.equal(settings.profiles.length, 2);
  assert.deepEqual(
    settings.profiles.map((item) => item.name),
    ['Основной', 'Для PvP'],
  );
  report.checks.push(
    'Profile creation, name save, independent selection and actual settings persistence.',
  );
  for (const viewport of [
    { width: 1180, height: 760 },
    { width: 1000, height: 680 },
    { width: 600, height: 680 },
    { width: 500, height: 380 },
  ]) {
    await page.setViewport(viewport);
    for (const [view, slug] of [
      ['Обзор', 'home'],
      ['Профили', 'profiles'],
      ['Настройки', 'settings'],
    ]) {
      await route(view);
      await page.waitForFunction(() => document.fonts.status === 'loaded');
      await new Promise((resolve) => setTimeout(resolve, 260));
      const overflow = await page.evaluate(() => {
        const main = document.querySelector('main');
        return {
          document: document.documentElement.scrollWidth > innerWidth,
          main: main.scrollWidth > main.clientWidth + 1,
        };
      });
      assert.deepEqual(
        overflow,
        { document: false, main: false },
        slug + ' has horizontal overflow at ' + viewport.width,
      );
      if (slug === 'profiles') {
        await page.focus('.folder-action');
        assert.equal(
          await page.$eval('.folder-action', (element) => {
            const rect = element.getBoundingClientRect();
            return rect.top >= 38 && rect.bottom <= innerHeight;
          }),
          true,
          'Focused profile control is hidden behind window chrome.',
        );
        await page.$eval('main', (element) => {
          element.scrollTop = 0;
        });
      }
      const file = `${slug}-${viewport.width}x${viewport.height}.png`;
      await page.screenshot({ path: join(root, file) });
      report.screenshots.push(file);
    }
  }
  await page.emulateMediaFeatures([{ name: 'prefers-reduced-motion', value: 'reduce' }]);
  await route('Профили');
  assert.equal(
    await page.$eval('.view-content', (element) => getComputedStyle(element).animationName),
    'none',
  );
  await replaceName('Д'.repeat(40));
  await page.click('form button[type="submit"]');
  await page.waitForFunction(
    () =>
      document.querySelector('form button[type="submit"]').disabled &&
      !document.querySelector('#profile-name').disabled &&
      document.querySelector('.feedback-slot').textContent.includes('Профиль сохранён'),
  );
  assert.equal(
    await page.$eval('main', (element) => element.scrollWidth <= element.clientWidth + 1),
    true,
  );
  await save('Основной');
  report.checks.push(
    'Four viewport sizes across three views have no horizontal scroll; long profile names wrap; reduced motion disables transitions.',
  );
  await page.setViewport({ width: 1180, height: 760 });
  await route('Обзор');
  const colors = await page.evaluate(() => {
    const css = getComputedStyle(document.documentElement);
    return Object.fromEntries(
      [
        'foreground',
        'background',
        'muted-foreground',
        'card',
        'primary',
        'primary-foreground',
        'input',
        'ring',
      ].map((name) => [name, css.getPropertyValue('--' + name).trim()]),
    );
  });
  function contrast(first, second) {
    const luminance = (hex) => {
      const values = hex
        .slice(1)
        .match(/../g)
        .map((value) => parseInt(value, 16) / 255)
        .map((value) => (value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4));
      return values[0] * 0.2126 + values[1] * 0.7152 + values[2] * 0.0722;
    };
    const values = [luminance(first), luminance(second)].sort((a, b) => b - a);
    return (values[0] + 0.05) / (values[1] + 0.05);
  }
  report.contrast = [
    ['foreground', 'background', 4.5],
    ['muted-foreground', 'card', 4.5],
    ['primary-foreground', 'primary', 4.5],
    ['input', 'background', 3],
    ['ring', 'background', 3],
  ].map(([foreground, background, minimum]) => {
    const ratio = contrast(colors[foreground], colors[background]);
    assert.ok(ratio >= minimum, foreground + '/' + background + ' contrast is insufficient.');
    return { foreground, background, ratio: Number(ratio.toFixed(2)), minimum };
  });
  assert.equal(
    await page.$eval('.hero-art', (element) => getComputedStyle(element).transitionDuration),
    '0s',
  );
  assert.equal(await page.evaluate(() => document.querySelectorAll('[style]').length), 0);
  await page.screenshot({ path: join(root, 'overview.png') });
  assert.deepEqual(errors, [], 'Renderer logged an error or CSP violation.');
  report.checkedAt = new Date().toISOString();
  report.rendererErrors = errors;
  report.scope =
    'Isolated Electron renderer via temporary loopback DevTools; no native picker, real OAuth, physical-window inspection or downloads.';
  await writeFile(join(root, 'report.json'), JSON.stringify(report, null, 2) + '\n');
  console.log(
    JSON.stringify(
      { report: join(root, 'report.json'), screenshots: root, checks: report.checks },
      null,
      2,
    ),
  );
  await page.close();
} catch (error) {
  if (page && !page.isClosed()) {
    await page.screenshot({ path: join(root, 'failure.png') }).catch(() => {});
    const state = await page.evaluate(() => ({
      heading: document.querySelector('h1')?.textContent,
      name: document.querySelector('#profile-name')?.value,
      feedback: document.querySelector('.feedback-slot')?.textContent,
      error: document.querySelector('[role="alert"]')?.textContent,
      focused: document.activeElement?.outerHTML,
      buttons: [...document.querySelectorAll('button')].map((button) => ({
        text: button.textContent,
        disabled: button.disabled,
      })),
    }));
    await writeFile(join(root, 'failure.json'), JSON.stringify(state, null, 2));
    console.error('UI inspection context: ' + root);
  }
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

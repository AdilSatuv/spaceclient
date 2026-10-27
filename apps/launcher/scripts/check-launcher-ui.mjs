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
const env = { ...process.env, SPACE_LAUNCHER_DATA: data, SPACE_LAUNCHER_TEST_HIDDEN: '1' };
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
  browser = await puppeteer.connect({
    browserWSEndpoint: endpoint,
    defaultViewport: null,
    protocolTimeout: 15000,
  });
  const target = await browser.waitForTarget((item) => item.url() === 'space://app/index.html', {
    timeout: 15000,
  });
  page = await target.page();
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(message.text());
  });
  await page.waitForSelector('.current-profile');
  await page.waitForFunction(() =>
    [...document.querySelectorAll('.space-mark img.ringworld-rich')].every(
      (image) => image.complete && image.naturalWidth > 0 && image.naturalHeight > 0,
    ),
  );
  assert.deepEqual(
    await page.$$eval('.titlebar .space-mark img, .brand .space-mark img', (images) =>
      images.map((image) => ({
        src: image.currentSrc,
        alt: image.alt,
        naturalWidth: image.naturalWidth,
      })),
    ),
    [
      { src: 'space://app/logo.png', alt: '', naturalWidth: 1254 },
      { src: 'space://app/logo.png', alt: '', naturalWidth: 1254 },
    ],
    'Titlebar and sidebar must show the selected local logo.',
  );
  report.checks.push(
    'Selected logo loads in the titlebar and sidebar from the local asset protocol.',
  );
  assert.deepEqual(
    await page.evaluate(() => ({
      require: typeof window.require,
      process: typeof window.process,
      functions: Object.keys(window.space).length,
    })),
    { require: 'undefined', process: 'undefined', functions: 13 },
  );
  report.checks.push(
    'Sandbox bridge remains limited to thirteen functions; no renderer Node globals.',
  );
  const siteTokens = await readFile(join(repo, 'apps/website/src/_app/styles/tokens.css'), 'utf8');
  const tokenPairs = {
    background: 'bg',
    foreground: 'text',
    card: 'surface',
    secondary: 'raised',
    primary: 'accent',
    'primary-foreground': 'on-accent',
    'muted-foreground': 'muted',
    border: 'line',
    input: 'control-border',
    ring: 'accent',
    copper: 'violet',
  };
  const expectedTokens = Object.fromEntries(
    Object.entries(tokenPairs).map(([local, source]) => [
      local,
      siteTokens.match(new RegExp('--' + source + ':\\s*([^;]+);'))[1].trim(),
    ]),
  );
  const actualTokens = await page.evaluate(
    (keys) =>
      Object.fromEntries(
        keys.map((key) => [
          key,
          getComputedStyle(document.documentElement)
            .getPropertyValue('--' + key)
            .trim(),
        ]),
      ),
    Object.keys(tokenPairs),
  );
  assert.deepEqual(
    actualTokens,
    expectedTokens,
    'Launcher roles must match canonical Eclipse website tokens.',
  );
  assert.equal(
    await page.$eval('.hero-art', (element) => getComputedStyle(element).backgroundImage),
    'none',
  );
  assert.equal(
    await page.evaluate(() => {
      const ids = [...document.querySelectorAll('[id]')].map((element) => element.id);
      return ids.length === new Set(ids).size;
    }),
    true,
    'DOM IDs must be unique.',
  );
  report.checks.push(
    'Eleven Eclipse color roles match website tokens; vector scene remains local; DOM IDs are unique.',
  );
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
  // Chromium's accessibility tree is not a substitute for testing a real screen reader.
  const accessibility = await page.createCDPSession();
  const axNode = async (selector) => {
    const { root: document } = await accessibility.send('DOM.getDocument');
    const { nodeId } = await accessibility.send('DOM.querySelector', {
      nodeId: document.nodeId,
      selector,
    });
    assert.notEqual(nodeId, 0, 'Missing accessibility target: ' + selector);
    const { nodes } = await accessibility.send('Accessibility.getPartialAXTree', {
      nodeId,
      fetchRelatives: false,
    });
    return nodes[0];
  };
  const availability = async () => {
    const view = await page.$eval('#launch-availability', (element) => {
      const rect = element.getBoundingClientRect();
      return {
        text: element.textContent.trim(),
        visible:
          rect.width > 0 && rect.height > 0 && getComputedStyle(element).visibility === 'visible',
        role: element.getAttribute('role'),
        live: element.getAttribute('aria-live'),
      };
    });
    assert.equal(view.visible, true, 'The launch explanation must be visible.');
    assert.equal(view.role, 'status');
    assert.equal(view.live, 'polite');
    const node = await axNode('#launch-availability');
    assert.equal(node.ignored, false, 'The launch explanation must be in the accessibility tree.');
    assert.equal(node.role.value, 'status');
    assert.ok(
      node.properties.some(
        (property) => property.name === 'live' && property.value.value === 'polite',
      ),
    );
    return view.text;
  };
  await page.waitForFunction(() => {
    const explanation = document.querySelector('#launch-availability');
    return (
      explanation &&
      explanation.textContent.trim() &&
      document.querySelector('.launch-button').disabled
    );
  });
  const noNickname = await availability();
  assert.match(noNickname, /ник/i, 'An absent nickname must be explained.');
  await page.setViewport({ width: 1000, height: 680 });
  assert.equal(
    await page.$eval('#launch-availability', (element) => {
      const rect = element.getBoundingClientRect();
      return rect.top >= 38 && rect.bottom <= innerHeight;
    }),
    true,
    'The explanation for a missing nickname must fit the desktop viewport.',
  );
  await page.screenshot({ path: join(root, 'no-nickname-1000x680.png') });
  report.screenshots.push('no-nickname-1000x680.png');
  const descriptionIds = await page.$eval('.launch-button', (button) =>
    button.getAttribute('aria-describedby').split(/\s+/),
  );
  assert.ok(descriptionIds.includes('launch-availability'));
  assert.ok(descriptionIds.includes('launch-status'));
  const disabledLaunch = await axNode('.launch-button');
  assert.equal(disabledLaunch.ignored, false);
  assert.equal(disabledLaunch.role.value, 'button');
  assert.ok(disabledLaunch.description?.value.includes(noNickname));
  await page.focus('#local-nickname');
  await page.keyboard.type('ab');
  await page.keyboard.press('Tab');
  assert.equal(
    await page.$eval('#local-nickname', (input) => input.getAttribute('aria-invalid')),
    'true',
  );
  await page.focus('#local-nickname');
  await page.keyboard.down('Control');
  await page.keyboard.press('KeyA');
  await page.keyboard.up('Control');
  await page.keyboard.type('SpaceTester');
  const unsavedNickname = await availability();
  assert.match(unsavedNickname, /сохран/i, 'An unsaved nickname must be explained.');
  assert.equal(await page.$eval('.launch-button', (button) => button.disabled), true);
  await page.keyboard.press('Tab');
  assert.equal(
    await page.evaluate(() => document.activeElement.textContent.trim()),
    'Сохранить ник',
    'Keyboard focus must reach the action that resolves the nickname block.',
  );
  await page.keyboard.press('Enter');
  await page.waitForFunction(() =>
    document.querySelector('.feedback-slot').textContent.includes('Локальный ник сохранён'),
  );
  assert.equal(
    JSON.parse(await readFile(join(data, 'settings.json'), 'utf8')).localNickname,
    'SpaceTester',
  );
  await page.reload();
  await page.waitForSelector('#local-nickname');
  assert.equal(await page.$eval('#local-nickname', (input) => input.value), 'SpaceTester');
  await page.waitForFunction(() => !document.querySelector('.launch-button').disabled);
  await page.focus('#local-nickname');
  await page.keyboard.press('Tab');
  assert.equal(
    await page.evaluate(() => document.activeElement.classList.contains('launch-button')),
    true,
    'The enabled launch action must be reachable by keyboard.',
  );
  await page.keyboard.press('Enter');
  await page.waitForFunction(() =>
    document.querySelector('.local-launch').textContent.includes('Выберите Java'),
  );
  const launchStatus = await axNode('#launch-status');
  assert.equal(launchStatus.ignored, false);
  assert.equal(launchStatus.role.value, 'status');
  assert.ok(
    launchStatus.properties.some(
      (property) => property.name === 'live' && property.value.value === 'polite',
    ),
  );
  await accessibility.detach();
  report.checks.push(
    'Visible launch explanations and polite status are exposed in Chromium accessibility tree; keyboard resolves nickname and reaches launch.',
  );
  await page.waitForFunction(() => !document.querySelector('.launch-button').disabled);
  const malicious = await page.evaluate(() =>
    window.space.startLaunch({ id: 'default', args: ['bad'] }),
  );
  assert.equal(malicious.ok, false);
  report.checks.push(
    'Nickname inline validation/save/reload; local status; missing Java failure and retry; structured launch payload rejected.',
  );
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
      if (slug === 'home' && viewport.width >= 1000) {
        const controls = await page.evaluate(() =>
          ['.launch-button', '.installation-actions button', '#launch-availability'].map(
            (selector) => {
              const rect = document.querySelector(selector).getBoundingClientRect();
              return {
                selector,
                visible:
                  rect.top >= 38 &&
                  rect.bottom <= innerHeight &&
                  rect.height >= (selector === '#launch-availability' ? 1 : 44),
              };
            },
          ),
        );
        assert.ok(
          controls.every((control) => control.visible),
          'Launch explanation and main controls must fit the desktop viewport: ' +
            JSON.stringify(controls),
        );
      }
      if (slug === 'settings')
        assert.equal(
          await page.evaluate(() =>
            document.body.innerText.includes('Запуск игры пока недоступен'),
          ),
          false,
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
  const emulation = await page.createCDPSession();
  await emulation.send('Emulation.setEmulatedMedia', {
    features: [{ name: 'forced-colors', value: 'active' }],
  });
  assert.equal(await page.evaluate(() => matchMedia('(forced-colors: active)').matches), true);
  assert.equal(
    await page.$eval('.ringworld-rich', (element) => getComputedStyle(element).display),
    'none',
  );
  assert.notEqual(
    await page.$eval('.ringworld-flat', (element) => getComputedStyle(element).display),
    'none',
  );
  assert.notEqual(
    await page.$eval('.hero-content h2 span', (element) => getComputedStyle(element).color),
    'rgba(0, 0, 0, 0)',
  );
  await page.screenshot({ path: join(root, 'forced-colors.png') });
  report.screenshots.push('forced-colors.png');
  report.checks.push(
    'Launch explanation and preparation controls visible at both desktop sizes; forced colors retain text and the flat planet mark.',
  );
  await emulation.detach();
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

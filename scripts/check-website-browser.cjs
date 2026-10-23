const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const { existsSync } = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const { spawn } = require('node:child_process');
const { randomUUID } = require('node:crypto');
const puppeteer = require('puppeteer-core');
const { runWebsiteFlows } = require('./check-website-flows.cjs');
const { firefoxExecutable } = require('./setup-website-firefox.cjs');
const { checkFirefoxMotion } = require('./check-website-firefox.cjs');
const root = path.resolve(__dirname, '..');
const production = require('../apps/api/dist/content/infrastructure/curated-catalog.js');
const fakeHash = 'a'.repeat(64);
const release = {
  id: 'qa-only',
  version: '0.1.0',
  channel: 'alpha',
  publishedAt: '2026-01-01T00:00:00.000Z',
  minecraftVersions: ['1.8.9'],
  // Next prefetches this link; keep the fixture connected to a served article.
  changelogSlug: production.updates[0].slug,
  artifacts: [
    {
      platform: 'windows',
      architecture: 'x64',
      fileName: 'TEST-ONLY.zip',
      sizeBytes: 1024,
      sha256: fakeHash,
      signatureStatus: 'unsigned',
      downloadUrl:
        'https://github.com/AdilSatuv/spaceclient/releases/download/qa-only/TEST-ONLY.zip',
    },
  ],
};
async function listen(server) {
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  return `http://127.0.0.1:${server.address().port}`;
}
async function close(server) {
  server.closeAllConnections();
  await new Promise((resolve) => server.close(resolve));
}
async function main() {
  const firefox = process.argv.includes('--firefox');
  const executable = firefox
    ? process.env.SPACE_FIREFOX_PATH || firefoxExecutable()
    : process.env.SPACE_CHROME_PATH ||
      [
        'C:/Program Files/Google/Chrome/Application/chrome.exe',
        '/usr/bin/google-chrome',
        '/usr/bin/google-chrome-stable',
        '/opt/google/chrome/google-chrome',
      ].find(existsSync);
  assert.ok(
    executable && existsSync(executable),
    firefox
      ? 'Run npm run setup:web:firefox first, or set SPACE_FIREFOX_PATH.'
      : 'Set SPACE_CHROME_PATH to an installed Chrome binary.',
  );
  const output = path.join(
    root,
    firefox ? '.reports/website-firefox' : '.reports/website-browser',
    `run-${randomUUID()}`,
  );
  const extension = path.join(output, 'zoom-extension');
  await fs.mkdir(output, { recursive: true });
  if (!firefox) {
    await fs.mkdir(extension, { recursive: true });
    await fs.writeFile(
      path.join(extension, 'manifest.json'),
      JSON.stringify({
        manifest_version: 3,
        name: 'Space QA zoom',
        version: '1.0',
        permissions: ['tabs'],
      }),
    );
    await fs.writeFile(path.join(extension, 'control.html'), '<title>Space QA zoom</title>');
  }
  const scenario = { release: false, pagination: false, fault: null, delayMs: 0 };
  const fixtureUpdates = Array.from({ length: 14 }, (_, i) => ({
    ...production.updates[0],
    id: `qa-note-${i}`,
    slug: `qa-note-${i}`,
    title: `TEST ONLY NOTE ${i}`,
  }));
  const fixture = http.createServer(async (req, res) => {
    res.setHeader('Content-Type', 'application/json');
    const url = new URL(req.url, 'http://localhost');
    if (scenario.fault?.path === url.pathname) {
      const kind = scenario.fault.kind;
      if (kind === 'http') {
        res.statusCode = 503;
        return res.end('{}');
      }
      if (kind === 'invalid') return res.end('{broken-json');
      if (kind === 'timeout') {
        await new Promise((resolve) => setTimeout(resolve, 3200));
        return res.end('{}');
      }
    }
    if (scenario.delayMs) await new Promise((resolve) => setTimeout(resolve, scenario.delayMs));
    if (url.pathname === '/v1/releases')
      return res.end(
        JSON.stringify({
          items: scenario.release ? [release] : [],
          total: scenario.release ? 1 : 0,
        }),
      );
    if (url.pathname === '/v1/features')
      return res.end(JSON.stringify({ items: production.features }));
    if (url.pathname === '/v1/updates') {
      const page = Number(url.searchParams.get('page') || 1),
        type = url.searchParams.get('type') || 'all';
      const items = (scenario.pagination ? fixtureUpdates : production.updates).filter(
        (item) => type === 'all' || item.type === type,
      );
      return res.end(
        JSON.stringify({
          items: items.slice((page - 1) * 12, page * 12),
          total: items.length,
          page,
          pageSize: 12,
        }),
      );
    }
    const item = [...production.updates, ...fixtureUpdates].find(
      (item) => url.pathname === `/v1/updates/${item.slug}`,
    );
    if (item) return res.end(JSON.stringify(item));
    res.statusCode = 404;
    res.end('{}');
  });
  const api = await listen(fixture);
  const reservation = http.createServer();
  const base = await listen(reservation);
  await close(reservation);
  const child = spawn(
    process.execPath,
    [
      path.join(root, 'node_modules/next/dist/bin/next'),
      'start',
      '--hostname',
      '127.0.0.1',
      '--port',
      new URL(base).port,
    ],
    {
      cwd: path.join(root, 'apps/website'),
      env: { ...process.env, SPACE_API_URL: api },
      windowsHide: true,
      stdio: ['ignore', 'pipe', 'pipe'],
    },
  );
  let logs = '';
  child.stdout.on('data', (x) => (logs += x));
  child.stderr.on('data', (x) => (logs += x));
  const denied = http.createServer(async (req, res) => {
    try {
      const response = await fetch(base + req.url, { redirect: 'manual' });
      res.statusCode = response.status;
      response.headers.forEach((value, key) => {
        if (
          !['content-length', 'content-encoding', 'transfer-encoding', 'connection'].includes(key)
        )
          res.setHeader(key, value);
      });
      res.setHeader('Permissions-Policy', 'clipboard-write=()');
      res.end(Buffer.from(await response.arrayBuffer()));
    } catch {
      res.statusCode = 502;
      res.end('QA proxy unavailable');
    }
  });
  const deniedBase = await listen(denied);
  let browser;
  const errors = [],
    checks = [],
    layouts = [],
    flows = [];
  const expected404 = new Set(['/qa-missing-route', '/help/qa-unknown', '/updates/qa-unknown']);
  try {
    let ready = false;
    for (let i = 0; i < 100; i++) {
      try {
        if ((await fetch(base)).ok) {
          ready = true;
          break;
        }
      } catch {}
      if (child.exitCode !== null) break;
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
    assert.ok(ready, logs);
    browser = await puppeteer.launch({
      executablePath: executable,
      headless: true,
      defaultViewport: null,
      userDataDir: path.join(output, 'browser-data'),
      ...(firefox
        ? {
            browser: 'firefox',
            protocolTimeout: 30000,
            extraPrefsFirefox: {
              'ui.prefersReducedMotion': 0,
              'browser.shell.checkDefaultBrowser': false,
            },
          }
        : {
            enableExtensions: true,
            args: ['--enable-unsafe-extension-debugging', '--window-size=1440,1000'],
            downloadBehavior: { policy: 'deny' },
          }),
    });
    let control;
    if (!firefox) {
      const extensionId = await browser.installExtension(extension);
      control = await browser.newPage();
      await control.goto(`chrome-extension://${extensionId}/control.html`);
    }
    const page = await browser.newPage();
    await page.setCacheEnabled(false);
    if (firefox) {
      await page.setRequestInterception(true);
      page.on('request', (request) => {
        if (
          /^https:\/\/github\.com\/AdilSatuv\/spaceclient\/releases\/download\//.test(request.url())
        ) {
          errors.push('Unexpected fixture download request');
          void request.abort();
        } else {
          void request.continue();
        }
      });
    }
    page.on('pageerror', (error) => errors.push(error.message));
    page.on('console', (message) => {
      if (message.type() !== 'error') return;
      const value = message.text();
      if (
        /404 \(Not Found\)/.test(value) ||
        (/clipboard/i.test(value) && /permissions policy/i.test(value))
      )
        return;
      errors.push(value);
    });
    page.on('response', (response) => {
      if (response.status() < 400) return;
      const url = new URL(response.url());
      if (
        [base, deniedBase].includes(url.origin) &&
        url.pathname !== '/favicon.ico' &&
        !(response.status() === 404 && expected404.has(url.pathname))
      ) {
        errors.push(`Unexpected HTTP ${response.status()}: ${url.pathname}`);
      }
    });
    const goto = async (route) => {
      const response = await page.goto(base + route, { waitUntil: 'networkidle0' });
      assert.equal(response.status(), 200, route);
      return response;
    };
    const routes = [
      '/',
      '/features',
      '/download',
      '/updates',
      '/help',
      '/help/launcher-status',
      '/updates/launcher-and-website',
    ];
    if (!process.argv.includes('--flows-only')) {
      for (const width of [320, 375, 390, 768, 1024, 1440]) {
        await page.setViewport({ width, height: 900 });
        for (const route of routes) {
          await goto(route);
          assert.ok(
            await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1),
            `${route}: overflow at ${width}`,
          );
          assert.equal(await page.$$eval('h1', (items) => items.length), 1);
          layouts.push({ route, width });
        }
      }
      checks.push(
        'Seven routes at six widths, including 320 px: no horizontal overflow, one h1 and HTTP 200.',
      );
      if (!firefox) {
        await page.setViewport(null);
        await goto('/');
        const before = await page.evaluate(() => ({ width: innerWidth, dpr: devicePixelRatio }));
        const zoom = await control.evaluate(async (origin) => {
          const tab = (await chrome.tabs.query({})).find((item) => item.url.startsWith(origin));
          await chrome.tabs.setZoom(tab.id, 2);
          return chrome.tabs.getZoom(tab.id);
        }, base);
        assert.equal(zoom, 2);
        await page.waitForFunction((width) => innerWidth < width * 0.6, {}, before.width);
        for (const route of routes) {
          await goto(route);
          assert.ok(
            await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1),
            `${route}: 200% zoom overflow`,
          );
          const clipped = await page.$$eval('h1, h2, h3, p, li, summary', (elements) =>
            elements
              .filter((element) => {
                if (element.closest('[aria-hidden="true"]') || !element.getClientRects().length)
                  return false;
                const range = document.createRange();
                range.selectNodeContents(element);
                return [...range.getClientRects()].some(
                  (rect) => rect.width > 1 && (rect.left < -1 || rect.right > innerWidth + 1),
                );
              })
              .map((element) => element.textContent.slice(0, 80)),
          );
          assert.deepEqual(clipped, [], `${route}: clipped text at 200%`);
        }
        const after = await page.evaluate(() => ({ width: innerWidth, dpr: devicePixelRatio }));
        // Full-page capture can change CDP metrics at browser zoom; preserve the actual viewport.
        await page.screenshot({ path: path.join(output, 'zoom-200.png'), fullPage: false });
        await control.evaluate(async (origin) => {
          const tab = (await chrome.tabs.query({})).find((item) => item.url.startsWith(origin));
          await chrome.tabs.setZoom(tab.id, 1);
        }, base);
        checks.push({
          browserZoom: zoom,
          before,
          after,
          method: 'chrome.tabs.setZoom; real browser zoom, not CSS zoom or pinch scaling.',
        });
        await goto('/');
        await page.waitForFunction(() => !document.querySelector('.motion-toggle').disabled);
        await page.emulateMediaFeatures([{ name: 'prefers-reduced-motion', value: 'reduce' }]);
        await page.waitForFunction(
          () =>
            document.querySelector('.motion-toggle').disabled &&
            document.documentElement.dataset.motion === 'paused',
        );
        const animation = await page.$eval('.starfield', (element) => ({
          state: getComputedStyle(element).animationPlayState,
          duration: getComputedStyle(element).animationDuration,
        }));
        assert.ok(animation.state === 'paused' || animation.duration === '0s');
        assert.equal(
          await page.$eval(
            '.planet-assembly',
            (element) => getComputedStyle(element).animationName,
          ),
          'none',
        );
        await page.emulateMediaFeatures([
          { name: 'prefers-reduced-motion', value: 'no-preference' },
        ]);
        await page.waitForFunction(
          () =>
            !document.querySelector('.motion-toggle').disabled &&
            document.documentElement.dataset.motion === 'active',
        );
        checks.push(
          'Reduced-motion changed live without reload, pauses decoration and restores the normal control afterwards.',
        );
        await page.setViewport({ width: 390, height: 900 });
        await goto('/');
        await page.click('.menu-trigger');
        await page.waitForSelector('dialog[open]');
        await page.keyboard.press('Escape');
        await page.waitForFunction(
          () =>
            !document.querySelector('dialog').open &&
            document.activeElement === document.querySelector('.menu-trigger'),
        );
        checks.push('Mobile menu Escape restores focus to its trigger.');
        scenario.release = true;
        await goto('/download');
        await browser
          .defaultBrowserContext()
          .overridePermissions(base, [
            'clipboard-read',
            'clipboard-write',
            'clipboard-sanitized-write',
          ]);
        await page.bringToFront();
        let previous;
        try {
          previous = await page.evaluate(() => navigator.clipboard.readText());
          await page.click('.hash-block button');
          await page.waitForFunction(
            () => document.querySelector('.hash-block [role=status]').textContent.length > 0,
          );
          assert.equal(
            await page.$eval('.hash-block [role=status]', (element) => element.textContent),
            'Скопировано',
          );
          assert.ok(
            (await page.evaluate(() => navigator.clipboard.readText())) === fakeHash,
            'Clipboard does not contain the fixture hash',
          );
        } finally {
          if (previous !== undefined)
            await page.evaluate(
              async ({ expected, saved }) => {
                if ((await navigator.clipboard.readText()) === expected)
                  await navigator.clipboard.writeText(saved);
              },
              { expected: fakeHash, saved: previous },
            );
        }
        checks.push(
          'Clipboard success verified by reading the fake SHA-256 back; previous clipboard restored when still unchanged.',
        );
        await page.goto(deniedBase + '/download', { waitUntil: 'networkidle0' });
        await page.click('.hash-block button');
        await page.waitForFunction(() =>
          document
            .querySelector('.hash-block [role=status]')
            .textContent.includes('Не удалось скопировать'),
        );
        await page.focus('.hash-block input');
        await page.keyboard.down('Control');
        await page.keyboard.press('A');
        await page.keyboard.up('Control');
        assert.equal(
          await page.$eval('.hash-block input', (input) =>
            input.value.slice(input.selectionStart, input.selectionEnd),
          ),
          fakeHash,
        );
        checks.push(
          'Actual Permissions-Policy clipboard denial shows fallback; the complete hash remains keyboard-selectable.',
        );
      }
    }
    await runWebsiteFlows({
      browser,
      page,
      base,
      output,
      setScenario: (value) => Object.assign(scenario, value),
      results: flows,
    });
    const browserVersion = await browser.version();
    if (firefox) {
      // Run Firefox profiles sequentially, including on Windows.
      await browser.close();
      browser = undefined;
      await checkFirefoxMotion({ executable, base, output, errors });
      checks.push(
        'A separate Firefox profile with ui.prefersReducedMotion=1 exposes the real reduced-motion media query, pauses animation and disables the motion toggle.',
      );
    }
    assert.deepEqual(errors, []);
    const report = {
      schemaVersion: 1,
      checkedAt: new Date().toISOString(),
      browser: browserVersion,
      protocol: firefox ? 'webDriverBiDi' : 'cdp',
      realAccountUsed: false,
      realDownloadsStarted: false,
      layouts,
      checks,
      flows,
      errors,
      skipped: firefox
        ? [
            '200% browser zoom, clipboard success/denial and live media emulation remain covered by the Chrome suite; they are not asserted for Firefox.',
          ]
        : [],
      scope: `Isolated production Next with fixture API and own ${firefox ? 'Firefox profiles over WebDriver BiDi' : 'Chrome profile and test-only tabs extension'}. No personal browser session, real release download or public HTTPS audit.`,
    };
    await fs.writeFile(path.join(output, 'report.json'), JSON.stringify(report, null, 2) + '\n');
    console.log(
      JSON.stringify({
        report: path.join(output, 'report.json'),
        checks: checks.length,
        layouts: layouts.length,
        flows: flows.length,
      }),
    );
  } catch (error) {
    await fs.writeFile(
      path.join(output, 'failure.json'),
      JSON.stringify({ error: String(error), errors, checks, layouts, flows }, null, 2),
    );
    throw error;
  } finally {
    await browser?.close();
    child.kill();
    await close(denied);
    await close(fixture);
  }
}
main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

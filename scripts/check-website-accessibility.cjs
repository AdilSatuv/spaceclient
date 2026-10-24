const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const { randomUUID } = require('node:crypto');
const puppeteer = require('puppeteer-core');
const { firefoxExecutable } = require('./setup-website-firefox.cjs');

// W3C SC 1.4.12 overrides, confined to the disposable page/profile.
const textSpacing = `
  * { line-height: 1.5 !important; letter-spacing: .12em !important;
      word-spacing: .16em !important; }
  p { margin-bottom: 2em !important; }
`;
const routes = [
  '/',
  '/features',
  '/download',
  '/updates',
  '/help',
  '/help/launcher-status',
  '/updates/local-launch-and-hud',
  '/sc23-missing',
];

async function layoutState(page) {
  return page.evaluate(() => {
    const clipped = [];
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    while (walker.nextNode()) {
      const node = walker.currentNode;
      const parent = node.parentElement;
      if (
        !node.textContent.trim() ||
        !parent?.getClientRects().length ||
        parent.closest('script, style, [aria-hidden="true"], .visually-hidden')
      )
        continue;
      const range = document.createRange();
      range.selectNodeContents(node);
      for (const rect of range.getClientRects()) {
        if (!rect.width || !rect.height) continue;
        for (let ancestor = parent; ancestor; ancestor = ancestor.parentElement) {
          const style = getComputedStyle(ancestor);
          const box = ancestor.getBoundingClientRect();
          if (
            ['hidden', 'clip'].includes(style.overflowX) &&
            (rect.left < box.left - 1 || rect.right > box.right + 1)
          ) {
            clipped.push({
              text: node.textContent.trim().slice(0, 80),
              container: ancestor.className,
            });
            break;
          }
        }
      }
    }
    const controls = [...document.querySelectorAll('.site-header a, .site-header button')].filter(
      (el) => el.getClientRects().length,
    );
    const outsideControls = controls
      .filter((el) => {
        const rect = el.getBoundingClientRect();
        return rect.left < -1 || rect.right > document.documentElement.clientWidth + 1;
      })
      .map((el) => el.textContent.trim());
    return {
      viewportWidth: innerWidth,
      clientWidth: document.documentElement.clientWidth,
      scrollWidth: document.documentElement.scrollWidth,
      h1: document.querySelector('h1')?.textContent,
      clipped,
      outsideControls,
    };
  });
}

async function checkWebsiteAccessibility({
  browser,
  base,
  output,
  results = [],
  visuals = false,
  firefox = false,
}) {
  const page = await browser.newPage();
  const checks = [];
  const errors = [];
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('response', (response) => {
    if (response.url().startsWith(base + '/_next/') && response.status() >= 400)
      errors.push(`Asset HTTP ${response.status()}: ${response.url()}`);
  });
  page.on('requestfailed', (request) => {
    if (request.url().startsWith(base + '/_next/')) errors.push(`Failed asset: ${request.url()}`);
  });
  try {
    await page.setCacheEnabled(false);
    if (!firefox)
      await page.emulateMediaFeatures([{ name: 'prefers-reduced-motion', value: 'reduce' }]);
    for (const route of routes) {
      const slug = route === '/' ? 'home' : route.slice(1).replaceAll('/', '-');
      for (const width of [320, 761, 1440]) {
        await page.setViewport({ width, height: 900 });
        const response = await page.goto(base + route, { waitUntil: 'networkidle0' });
        assert.equal(response.status(), route === '/sc23-missing' ? 404 : 200, route);
        await page.addStyleTag({ content: textSpacing });
        const state = await layoutState(page);
        const check = { route, width, ...state };
        checks.push(check);
        await page.screenshot({
          path: path.join(output, `spacing-${slug}-${width}.png`),
          fullPage: true,
        });
        assert.ok(state.scrollWidth <= state.clientWidth + 1, JSON.stringify(check));
        assert.deepEqual(state.clipped, [], JSON.stringify(check));
        assert.deepEqual(state.outsideControls, [], JSON.stringify(check));
      }
      if (visuals)
        for (const width of [390, 1440]) {
          await page.setViewport({ width, height: 900 });
          await page.goto(base + route, { waitUntil: 'networkidle0' });
          await page.screenshot({
            path: path.join(output, `visual-${slug}-${width}.png`),
            fullPage: true,
          });
        }
    }
    // A short landscape screen requires scrolling inside the modal. Exercise
    // actual keyboard focus/reveal rather than just counting the dialog links.
    for (const viewport of [
      { width: 568, height: 320 },
      { width: 320, height: 568 },
    ]) {
      await page.setViewport(viewport);
      await page.goto(base + '/help', { waitUntil: 'networkidle0' });
      await page.addStyleTag({ content: textSpacing });
      await page.focus('.menu-trigger');
      await page.keyboard.press('Enter');
      await page.waitForSelector('dialog[open]');
      const sequence = [];
      for (let i = 0; i < 6; i++) {
        const focus = await page.evaluate(() => {
          const active = document.activeElement;
          const dialog = document.querySelector('dialog');
          const rect = active.getBoundingClientRect(),
            box = dialog.getBoundingClientRect();
          return {
            text: active.textContent.trim(),
            inside: dialog.contains(active),
            visible: rect.top >= box.top - 1 && rect.bottom <= box.bottom + 1,
            horizontal: dialog.scrollWidth <= dialog.clientWidth + 1,
          };
        });
        sequence.push(focus);
        assert.ok(
          focus.inside && focus.visible && focus.horizontal,
          JSON.stringify({ viewport, focus }),
        );
        await page.keyboard.press('Tab');
      }
      assert.equal(await page.$eval('dialog button', (el) => el === document.activeElement), true);
      await page.keyboard.down('Shift');
      await page.keyboard.press('Tab');
      await page.keyboard.up('Shift');
      assert.equal(
        await page.$eval('dialog nav a:last-child', (el) => el === document.activeElement),
        true,
      );
      await page.screenshot({
        path: path.join(output, `menu-${viewport.width}x${viewport.height}.png`),
      });
      await page.keyboard.press('Escape');
      assert.equal(await page.$eval('.menu-trigger', (el) => el === document.activeElement), true);
      checks.push({ viewport, menuFocus: sequence, escapeReturnsFocus: true });
    }
    assert.deepEqual(errors, []);
    results.push({ id: 'WEB-TEXT-SPACING', checks: checks.slice(0, 24), errors });
    results.push({ id: 'WEB-SHORT-MENU', checks: checks.slice(24) });
    return checks;
  } catch (error) {
    await page.screenshot({ path: path.join(output, 'accessibility-failure.png'), fullPage: true });
    await fs.writeFile(
      path.join(output, 'accessibility-failure.json'),
      JSON.stringify({ error: String(error), checks, errors }, null, 2),
    );
    throw error;
  } finally {
    await page.close();
  }
}

async function main() {
  const root = path.resolve(__dirname, '..');
  const base =
    process.env.SPACE_SITE_URL ||
    JSON.parse(await fs.readFile(path.join(root, '.reports/local-preview.json'), 'utf8')).url;
  assert.ok(/^http:\/\/127\.0\.0\.1:\d+$/.test(base), 'Use a local QA preview origin.');
  const firefox = process.argv.includes('--firefox');
  const output = path.join(root, '.reports/website-accessibility', `run-${randomUUID()}`);
  await fs.mkdir(output, { recursive: true });
  const browser = await puppeteer.launch({
    executablePath: firefox
      ? firefoxExecutable()
      : process.env.SPACE_CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe',
    headless: true,
    userDataDir: path.join(output, 'browser-data'),
    ...(firefox
      ? { browser: 'firefox', extraPrefsFirefox: { 'ui.prefersReducedMotion': 1 } }
      : { downloadBehavior: { policy: 'deny' } }),
  });
  try {
    const results = [];
    await checkWebsiteAccessibility({ browser, base, output, results, visuals: true, firefox });
    const report = {
      schemaVersion: 1,
      checkedAt: new Date().toISOString(),
      browser: await browser.version(),
      scope:
        'Text spacing and short-screen keyboard checks in an isolated profile; screenshots require separate visual inspection. No screen-reader speech assertion.',
      results,
    };
    await fs.writeFile(path.join(output, 'report.json'), JSON.stringify(report, null, 2) + '\n');
    console.log(JSON.stringify({ output, checks: 26 }));
  } finally {
    await browser.close();
  }
}

module.exports = { checkWebsiteAccessibility };
if (require.main === module)
  main().catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });

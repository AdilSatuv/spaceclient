const assert = require('node:assert/strict');
const path = require('node:path');
const puppeteer = require('puppeteer-core');

async function checkWebsiteResilience({ browser, base, output, firefox, results }) {
  const page = await browser.newPage();
  try {
    await page.setCacheEnabled(false);
    // Firefox uses a separate profile with javascript.enabled=false: BiDi does not
    // implement the scripting-emulation command in the pinned browser.
    if (!firefox) await page.setJavaScriptEnabled(false);
    const goto = async (route) => {
      const response = await page.goto(base + route, { waitUntil: 'networkidle0' });
      assert.equal(response.status(), 200);
    };
    const submit = async () => {
      // Firefox's disabled-script profile cannot run the IntersectionObserver
      // callback used by ElementHandle.click. Scroll synchronously, then use a
      // real pointer click at the observed button bounds.
      const button = await page.$('.filter-form button');
      await button.evaluate((element) => element.scrollIntoView({ block: 'center' }));
      const bounds = await button.boundingBox();
      assert.ok(bounds && bounds.width > 0 && bounds.height > 0);
      await Promise.all([
        page.waitForNavigation({ waitUntil: 'networkidle0' }),
        page.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2),
      ]);
    };
    for (const width of [320, 390, 1440]) {
      await page.setViewport({ width, height: 900 });
      await goto('/');
      const state = await page.evaluate(() => {
        const visible = (element) => element.getClientRects().length > 0;
        const ids = [...document.querySelectorAll('.ringworld-mark [id]')].map(
          (element) => element.id,
        );
        return {
          none: matchMedia('(scripting: none)').matches,
          nav: [...document.querySelectorAll('.desktop-nav a')].filter(visible).length,
          inertControls: [...document.querySelectorAll('.menu-trigger, .motion-toggle')].filter(
            visible,
          ).length,
          overflow: document.documentElement.scrollWidth > innerWidth + 1,
          motion: document.documentElement.dataset.motion,
          idsUnique: new Set(ids).size === ids.length && ids.length >= 4,
        };
      });
      assert.deepEqual(state, {
        none: true,
        nav: 4,
        inertControls: 0,
        overflow: false,
        motion: 'paused',
        idsUnique: true,
      });
      if (width === 390)
        await page.screenshot({ path: path.join(output, 'no-js-390.png'), fullPage: false });
    }
    await page.setViewport({ width: 390, height: 900 });
    await goto('/features');
    await page.type('input[name=q]', 'CPS');
    await submit();
    assert.deepEqual(
      await page.evaluate(() =>
        [...document.querySelectorAll('.catalog h2')].map((item) => item.textContent),
      ),
      ['CPS Counter'],
    );
    await page.focus('.catalog summary');
    await page.keyboard.press('Enter');
    assert.equal(await page.$eval('.catalog details', (element) => element.open), true);
    await goto('/help');
    await page.type('input[name=q]', 'вход');
    await submit();
    assert.equal(await page.evaluate(() => document.querySelectorAll('main details').length), 1);
    await page.focus('main summary');
    await page.keyboard.press('Enter');
    assert.equal(await page.$eval('main details', (element) => element.open), true);
    await goto('/download');
    assert.match(
      await page.$eval('main', (element) => element.textContent),
      /Первый релиз ещё впереди/,
    );
    await goto('/updates?type=release');
    assert.match(
      await page.$eval('.empty-state', (element) => element.textContent),
      /Релизов ещё не было/,
    );
    const missing = await page.goto(base + '/help/qa-unknown', { waitUntil: 'networkidle0' });
    assert.equal(missing.status(), 404);
    results.push({
      id: 'WEB-NO-JS',
      result:
        'JavaScript disabled before navigation: visible header navigation at 320/390/1440, no inert menu/motion buttons, unique SVG IDs, native search and disclosure, empty download/update states and HTTP 404.',
    });
  } catch (error) {
    throw new Error('No-JavaScript scenario at ' + page.url() + ': ' + error.message, {
      cause: error,
    });
  } finally {
    await page.close();
  }
  if (firefox) return;
  const contrast = await browser.newPage();
  try {
    await contrast.setCacheEnabled(false);
    const session = await contrast.createCDPSession();
    for (const scheme of ['dark', 'light']) {
      await session.send('Emulation.setEmulatedMedia', {
        features: [
          { name: 'forced-colors', value: 'active' },
          { name: 'prefers-color-scheme', value: scheme },
        ],
      });
      for (const width of [390, 1440]) {
        await contrast.setViewport({ width, height: 900 });
        await contrast.goto(base, { waitUntil: 'networkidle0' });
        const state = await contrast.evaluate(() => ({
          forced: matchMedia('(forced-colors: active)').matches,
          titleFill: getComputedStyle(document.querySelector('.orbit-title')).webkitTextFillColor,
          titleColor: getComputedStyle(document.querySelector('h1')).color,
          background: getComputedStyle(document.querySelector('.orbit-title')).backgroundImage,
          backdrop: getComputedStyle(document.querySelector('.space-backdrop')).display,
          rich: getComputedStyle(document.querySelector('.ringworld-rich')).display,
          flat: getComputedStyle(document.querySelector('.ringworld-flat')).display,
          overflow: document.documentElement.scrollWidth > innerWidth + 1,
        }));
        assert.equal(state.forced, true);
        assert.equal(state.titleFill, state.titleColor);
        assert.equal(state.background, 'none');
        assert.equal(state.backdrop, 'none');
        assert.equal(state.rich, 'none');
        assert.equal(state.flat, 'inline');
        assert.equal(state.overflow, false);
        if (width === 390)
          await contrast.screenshot({
            path: path.join(output, 'forced-colors-' + scheme + '-390.png'),
            fullPage: false,
          });
      }
    }
    await contrast.goto(base + '/features', { waitUntil: 'networkidle0' });
    await contrast.keyboard.press('Tab');
    await contrast.focus('input[name=q]');
    assert.equal(
      await contrast.$eval('input[name=q]', (element) => getComputedStyle(element).outlineWidth),
      '2px',
    );
    results.push({
      id: 'WEB-FORCED-COLORS',
      result:
        'Chrome CDP forced-colors with dark/light scheme at 390/1440: gradient text remains visible, decoration is hidden, logo uses a flat system-color fallback and keyboard focus remains outlined. OS settings were not changed.',
    });
  } finally {
    await contrast.close();
  }
}
async function checkFirefoxNoScript({ executable, base, output, results }) {
  const browser = await puppeteer.launch({
    browser: 'firefox',
    protocolTimeout: 30000,
    executablePath: executable,
    headless: true,
    userDataDir: path.join(output, 'no-script-profile'),
    extraPrefsFirefox: { 'javascript.enabled': false, 'browser.shell.checkDefaultBrowser': false },
  });
  try {
    await checkWebsiteResilience({ browser, base, output, firefox: true, results });
  } finally {
    await browser.close();
  }
}
async function checkNavigationFeedback({
  browser,
  page: originalPage,
  base,
  output,
  setScenario,
  results,
}) {
  const page = await browser.newPage();
  try {
    setScenario({ delayMs: 1200 });
    await page.setCacheEnabled(false);
    await page.setViewport({ width: 1440, height: 900 });
    await page.goto(base, { waitUntil: 'networkidle0' });
    const selector = '.desktop-nav a[href="/features"]';
    const before = await page.$eval(selector, (element) => ({
      width: element.offsetWidth,
      height: element.offsetHeight,
    }));
    const navigation = page.waitForNavigation({ waitUntil: 'networkidle0' });
    await page.click(selector);
    await page.waitForSelector(selector + ' .navigation-feedback[data-pending="true"]');
    assert.match(await page.$eval('h1', (element) => element.textContent), /ТВОЙ МИР/);
    const during = await page.$eval(selector, (element) => ({
      width: element.offsetWidth,
      height: element.offsetHeight,
    }));
    assert.deepEqual(during, before, 'Loading feedback must not shift navigation');
    await page.screenshot({
      path: path.join(output, 'navigation-pending-1440.png'),
      fullPage: false,
    });
    await navigation;
    await page.waitForSelector('.catalog');
    assert.equal(
      await page.$$eval('.navigation-feedback[data-pending="true"]', (items) => items.length),
      0,
    );
    results.push({
      id: 'WEB-PENDING',
      result:
        'Delayed client navigation keeps the previous page visible, announces opening, shows feedback on the clicked link without changing its dimensions and clears the indicator after arrival.',
    });
  } finally {
    setScenario({ delayMs: 0 });
    await page.close();
    await originalPage.bringToFront();
  }
}
module.exports = { checkWebsiteResilience, checkFirefoxNoScript, checkNavigationFeedback };

const assert = require('node:assert/strict');
const path = require('node:path');

async function runWebsiteFlows({ browser, page, base, output, setScenario, results = [] }) {
  const goto = async (route) => {
    const response = await page.goto(base + route, { waitUntil: 'networkidle0' });
    assert.equal(response.status(), 200, route);
  };
  const text = (selector) => page.$eval(selector, (element) => element.textContent);
  const submit = () =>
    Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle0' }),
      page.click('.filter-form button'),
    ]);
  const typeQuery = async (value) => {
    await page.click('input[name=q]', { count: 3 });
    await page.type('input[name=q]', value);
  };
  const follow = async (selector) => {
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle0' }),
      page.click(selector),
    ]);
  };
  const formValues = () =>
    page.$eval('.filter-form', (form) => Object.fromEntries(new FormData(form)));
  setScenario({ release: false, pagination: false, fault: null, delayMs: 0 });

  for (const width of [390, 1440]) {
    await page.setViewport({ width, height: 900 });
    await goto('/features');
    await typeQuery('CPS');
    await page.select('select[name=category]', 'hud');
    await page.select('select[name=status]', 'prototype');
    await submit();
    assert.deepEqual(await formValues(), { q: 'CPS', category: 'hud', status: 'prototype' });
    const filteredUrl = page.url();
    const query = new URL(filteredUrl).searchParams;
    assert.equal(query.get('q'), 'CPS');
    assert.equal(query.get('category'), 'hud');
    assert.equal(query.get('status'), 'prototype');
    assert.deepEqual(await page.$$eval('.catalog h2', (items) => items.map((x) => x.textContent)), [
      'CPS Counter',
    ]);
    await page.focus('.catalog summary');
    await page.keyboard.press('Enter');
    assert.equal(await page.$eval('.catalog details', (item) => item.open), true);
    await page.reload({ waitUntil: 'networkidle0' });
    assert.deepEqual(await formValues(), { q: 'CPS', category: 'hud', status: 'prototype' });
    await typeQuery('space-no-such-feature');
    await submit();
    assert.match(await text('.empty-state'), /Ничего не найдено/);
    assert.equal((await formValues()).q, 'space-no-such-feature');
    await follow('.empty-state a[href="/features"]');
    assert.equal(await page.$$eval('.catalog article', (items) => items.length), 6);
    await page.goBack({ waitUntil: 'networkidle0' });
    await page.waitForSelector('.empty-state');
    assert.equal((await formValues()).q, 'space-no-such-feature');
    await page.goBack({ waitUntil: 'networkidle0' });
    await page.waitForSelector('.catalog');
    assert.equal(page.url(), filteredUrl);
    assert.deepEqual(await formValues(), { q: 'CPS', category: 'hud', status: 'prototype' });
    assert.match(await text('.catalog'), /CPS Counter/);
    results.push({
      id: 'WEB-03',
      width,
      result:
        'Search/category/status, detail Enter, reload, empty/reset and browser Back restore the query and results.',
    });

    setScenario({ release: true });
    await goto('/download');
    await page.select('select[name=channel]', 'alpha');
    await submit();
    assert.equal(await page.$$eval('.release-list article', (items) => items.length), 1);
    assert.match(await text('.release-list'), /Предварительная версия/);
    const alphaUrl = page.url();
    await page.select('select[name=channel]', 'stable');
    await submit();
    assert.match(await text('.empty-state'), /В этом канале сборок нет/);
    assert.equal(await page.$$eval('.release-list article', (items) => items.length), 0);
    await page.goBack({ waitUntil: 'networkidle0' });
    await page.waitForSelector('.release-list article');
    assert.equal(page.url(), alphaUrl);
    assert.equal(await page.$eval('select[name=channel]', (item) => item.value), 'alpha');
    setScenario({ release: false });
    results.push({
      id: 'WEB-04',
      width,
      result:
        'Release channel selection, empty channel and browser Back restore the alpha filter; no download is started.',
    });

    setScenario({ pagination: true });
    await goto('/updates?type=development');
    assert.equal(await page.$$eval('.article-links > a', (items) => items.length), 12);
    await follow('.pagination a[href*="page=2"]');
    await page.waitForFunction(() =>
      document.querySelector('.pagination').textContent.includes('Страница 2'),
    );
    assert.deepEqual(
      await page.$$eval('.article-links h2', (items) => items.map((x) => x.textContent)),
      ['TEST ONLY NOTE 12', 'TEST ONLY NOTE 13'],
    );
    assert.equal(new URL(page.url()).searchParams.get('type'), 'development');
    await follow('.article-links a[href="/updates/qa-note-12"]');
    assert.equal(await text('h1'), 'TEST ONLY NOTE 12');
    await page.goBack({ waitUntil: 'networkidle0' });
    await page.waitForFunction(() =>
      document.querySelector('.pagination')?.textContent.includes('Страница 2'),
    );
    assert.equal(new URL(page.url()).searchParams.get('type'), 'development');
    assert.equal(await page.$eval('select[name=type]', (item) => item.value), 'development');
    await page.select('select[name=type]', 'release');
    await submit();
    assert.match(await text('.empty-state'), /Релизов ещё не было/);
    assert.equal(new URL(page.url()).searchParams.get('page'), null);
    await page.goBack({ waitUntil: 'networkidle0' });
    await page.waitForFunction(() =>
      document.querySelector('.pagination')?.textContent.includes('Страница 2'),
    );
    assert.equal(await page.$eval('select[name=type]', (item) => item.value), 'development');
    setScenario({ pagination: false });
    results.push({
      id: 'WEB-06',
      width,
      result:
        'Fourteen fixture notes paginate 12+2; article and filter Back preserve type/page; changing filter resets page.',
    });

    await goto('/help');
    await typeQuery('вХоД');
    await submit();
    assert.equal(await page.$eval('input[name=q]', (item) => item.value), 'вХоД');
    assert.equal(await page.$$eval('#help-articles-title', (items) => items.length), 0);
    assert.equal(await page.$$eval('main details', (items) => items.length), 1);
    assert.match(await text('#faq-sign-in'), /вход по нику/);
    assert.match(await text('.result-count'), /Найдено: 1\s*· Инструкции: 0\s*· Ответы: 1/);
    assert.doesNotMatch(await text('main'), /Ответ пока не найден/);
    await page.focus('#faq-sign-in summary');
    await page.keyboard.press('Enter');
    assert.equal(await page.$eval('#faq-sign-in', (item) => item.open), true);
    await page.keyboard.press('Tab');
    assert.equal(
      await page.evaluate(() => document.activeElement.getAttribute('href')),
      '/help/launcher-status',
    );
    await page.keyboard.press('Enter');
    await page.waitForFunction(() => location.pathname === '/help/launcher-status');
    await page.waitForSelector('.article-body');
    await page.goBack({ waitUntil: 'networkidle0' });
    await page.waitForSelector('#faq-sign-in');
    assert.equal(await page.$eval('input[name=q]', (item) => item.value), 'вХоД');
    await goto('/help?q=предложение');
    assert.equal(await page.$$eval('main details', (items) => items.length), 1);
    assert.match(await text('#faq-suggestions'), /GitHub Issues/);
    await goto('/help?q=совместимость');
    assert.equal(await page.$$eval('.article-links > a', (items) => items.length), 1);
    assert.equal(await page.$$eval('main details', (items) => items.length), 0);
    await follow('.article-links a');
    assert.match(await text('h1'), /совместимость/);
    await goto('/help?q=space-no-such-answer');
    assert.match(await text('.empty-state'), /Ответ пока не найден/);
    assert.equal(await page.$$eval('main details', (items) => items.length), 0);
    assert.equal(
      await page.$eval('.empty-state a[href*="github.com"]', (item) => item.href),
      'https://github.com/AdilSatuv/spaceclient/issues',
    );
    await follow('.empty-state a[href="/help"]');
    assert.equal(await page.$eval('input[name=q]', (item) => item.value), '');
    await page.focus('#faq-sign-in summary');
    await page.keyboard.press('Enter');
    await page.focus('#faq-suggestions summary');
    await page.keyboard.press(' ');
    assert.equal(await page.$$eval('main details[open]', (items) => items.length), 2);
    const wasPaused = await page.evaluate(
      () => document.documentElement.dataset.motion === 'paused',
    );
    if (!wasPaused) {
      await page.click('.motion-toggle');
      await page.focus('#faq-suggestions summary');
    }
    await page.waitForFunction(() => document.documentElement.dataset.motion === 'paused');
    // Capture the actual viewport: Chrome's full-page resize can expose offscreen fixed UI.
    await page.screenshot({ path: path.join(output, `help-faq-${width}.png`), fullPage: false });
    await page.evaluate(() => scrollTo(0, 0));
    await page.screenshot({ path: path.join(output, `help-top-${width}.png`), fullPage: false });
    if (!wasPaused) await page.click('.motion-toggle');
    results.push({
      id: 'WEB-07',
      width,
      result:
        'Case-insensitive FAQ and article search, result counts, keyboard accordion/link, simultaneous open answers, empty/issue/reset and Back.',
    });
  }

  await page.setViewport({ width: 390, height: 900 });
  await goto('/');
  await page.keyboard.press('Tab');
  assert.equal(await page.evaluate(() => document.activeElement.className), 'skip');
  await page.keyboard.press('Enter');
  assert.equal(await page.evaluate(() => document.activeElement.id), 'main');
  await page.focus('.menu-trigger');
  await page.keyboard.press('Enter');
  await page.waitForSelector('dialog[open]');
  assert.equal(await page.$eval('dialog button', (item) => item === document.activeElement), true);
  await page.keyboard.down('Shift');
  await page.keyboard.press('Tab');
  await page.keyboard.up('Shift');
  assert.equal(
    await page.$eval('dialog a:last-child', (item) => item === document.activeElement),
    true,
  );
  await page.keyboard.press('Tab');
  assert.equal(await page.$eval('dialog button', (item) => item === document.activeElement), true);
  for (let i = 0; i < 12; i++) {
    await page.keyboard.press('Tab');
    assert.equal(await page.evaluate(() => !!document.activeElement.closest('dialog[open]')), true);
    assert.ok(
      await page.evaluate(() => {
        const element = document.activeElement;
        const rect = element.getBoundingClientRect();
        const css = getComputedStyle(element);
        return (
          rect.top >= 0 &&
          rect.bottom <= innerHeight &&
          css.outlineStyle !== 'none' &&
          parseFloat(css.outlineWidth) >= 2
        );
      }),
      'Menu keyboard focus must remain visible',
    );
  }
  await page.keyboard.press('Escape');
  await page.waitForFunction(
    () => !document.querySelector('dialog').open && document.body.style.overflow !== 'hidden',
  );
  assert.equal(
    await page.evaluate(() => document.activeElement.classList.contains('menu-trigger')),
    true,
  );
  await page.click('.menu-trigger');
  await page.click('dialog a[href="/help"]');
  await page.waitForFunction(
    () =>
      location.pathname === '/help' &&
      !document.querySelector('dialog').open &&
      document.body.style.overflow !== 'hidden',
  );
  await page.click('.menu-trigger');
  await page.setViewport({ width: 1024, height: 900 });
  await page.waitForFunction(
    () => !document.querySelector('dialog').open && document.body.style.overflow !== 'hidden',
  );
  assert.ok(await page.evaluate(() => document.activeElement.getClientRects().length > 0));
  results.push({
    id: 'WEB-10',
    result:
      'Skip link, forward/reverse menu focus trap, focus visibility, Escape, route and desktop breakpoint cleanup.',
  });

  for (const [route, endpoint, heading, healthy] of [
    ['/features', '/v1/features', 'Не удалось загрузить возможности.', '.catalog'],
    ['/download', '/v1/releases', 'Не удалось загрузить каталог.', '.compatibility-panel'],
    ['/updates', '/v1/updates', 'Не удалось загрузить журнал.', '.article-links'],
  ]) {
    for (const kind of ['http', 'invalid', 'timeout']) {
      setScenario({ fault: { path: endpoint, kind } });
      await goto(route);
      assert.equal(await text('#content-error-title'), heading);
      assert.doesNotMatch(
        await text('main'),
        /Первый релиз ещё впереди|Релизов ещё не было|Ничего не найдено/,
      );
      assert.equal(await page.$$eval('.site-header, .site-footer', (items) => items.length), 2);
      assert.equal(
        await page.$$eval(
          'main a[href^="https://github.com/"][href*="/releases/download/"]',
          (items) => items.length,
        ),
        0,
      );
      const documentMarker = `${route}:${kind}`;
      await page.evaluate((marker) => {
        document.documentElement.dataset.qaDocument = marker;
      }, documentMarker);
      setScenario({ fault: null, delayMs: 700 });
      await page.click('.empty-state button');
      await page.waitForFunction(() => {
        const button = document.querySelector('.empty-state button');
        return (
          button?.disabled &&
          button.getAttribute('aria-busy') === 'true' &&
          button.textContent.includes('Повторяем запрос')
        );
      });
      await page.waitForFunction(() => !document.querySelector('#content-error-title'));
      await page.waitForSelector(healthy);
      assert.equal(
        await page.evaluate(() => document.documentElement.dataset.qaDocument),
        documentMarker,
      );
      setScenario({ delayMs: 0 });
      if (route === '/download') assert.match(await text('main'), /Первый релиз ещё впереди/);
      results.push({
        id: 'WEB-09',
        route,
        fault: kind,
        result:
          'Failure differs from empty; real refresh shows busy/disabled state then recovers without reloading the browser document.',
      });
    }
  }

  for (const route of ['/qa-missing-route', '/help/qa-unknown', '/updates/qa-unknown']) {
    const response = await page.goto(base + route, { waitUntil: 'networkidle0' });
    assert.equal(response.status(), 404, route);
    assert.equal(await page.$$eval('h1', (items) => items.length), 1);
    assert.ok(await page.$('main a[href="/"]'));
  }
  results.push({
    id: 'WEB-01',
    result: 'Unknown route, help article and update return HTTP 404 with a home link.',
  });

  const storagePage = await browser.newPage();
  const storageErrors = [];
  storagePage.on('pageerror', (error) => storageErrors.push(error.message));
  try {
    await storagePage.evaluateOnNewDocument(() => {
      window.__qaStorageAttempts = 0;
      Object.defineProperty(window, 'localStorage', {
        get() {
          window.__qaStorageAttempts++;
          throw new DOMException('QA storage unavailable', 'SecurityError');
        },
      });
    });
    await storagePage.goto(base, { waitUntil: 'networkidle0' });
    await storagePage.waitForFunction(() => !document.querySelector('.motion-toggle').disabled);
    await storagePage.click('.motion-toggle');
    await storagePage.waitForFunction(() => document.documentElement.dataset.motion === 'paused');
    await storagePage.click('.motion-toggle');
    await storagePage.waitForFunction(() => document.documentElement.dataset.motion === 'active');
    assert.ok(await storagePage.evaluate(() => window.__qaStorageAttempts >= 3));
    assert.deepEqual(storageErrors, []);
    results.push({
      id: 'WEB-10',
      result:
        'Injected localStorage SecurityError on read/write does not prevent pause/resume; this checks the fallback, not an OS storage policy.',
    });
  } finally {
    await storagePage.close();
  }
  return results;
}
module.exports = { runWebsiteFlows };

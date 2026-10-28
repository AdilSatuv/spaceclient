const assert = require('node:assert/strict');

// Observe the live region before the browser replaces the native GET document.
async function checkFilterSubmit({ browser, base, setScenario, results, firefox }) {
  const page = await browser.newPage();
  let report;
  let pending;
  await page.setRequestInterception(true);
  page.on('request', (request) => {
    const submission = pending;
    if (submission && request.isNavigationRequest()) {
      // Keep the old document alive until its submit status reaches the test.
      Promise.all([submission.release, new Promise((resolve) => setTimeout(resolve, 1200))]).then(
        () => request.continue().catch(() => {}),
      );
    } else {
      request.continue().catch(() => {});
    }
  });
  await page.exposeFunction('__reportFilterSubmit', (state) => report?.(state));
  const submit = async (method, expected = { q: 'CPS', category: '', status: '' }) => {
    const observed = new Promise((resolve) => (report = resolve));
    let release;
    pending = { release: new Promise((resolve) => (release = resolve)) };
    const route = new URL(page.url()).pathname;
    await page.evaluate(() => {
      document.addEventListener(
        'submit',
        () => {
          // React flushes its submit update in a microtask before native navigation.
          queueMicrotask(() => {
            const form = document.querySelector('.filter-form');
            const status = form.querySelector('[role="status"]');
            window.__reportFilterSubmit({
              status: status?.textContent.trim(),
              live: status?.getAttribute('aria-live'),
              disabled: form.querySelector('button').disabled,
              values: Object.fromEntries(new FormData(form)),
              oldCount: document.querySelector('.result-count')?.textContent,
            });
          });
        },
        { once: true },
      );
    });
    const navigation = page.waitForNavigation({ waitUntil: 'networkidle0' });
    // The observation and navigation run concurrently; handle early rejection too.
    navigation.catch(() => {});
    try {
      if (method === 'keyboard') {
        await page.focus('input[name=q]');
        await page.keyboard.press('Enter');
      } else {
        await page.click('.filter-form button');
      }
      let timeout;
      let state;
      try {
        state = await Promise.race([
          observed,
          new Promise(
            (_, reject) =>
              (timeout = setTimeout(() => reject(new Error('No submit observation')), 5000)),
          ),
        ]);
      } finally {
        clearTimeout(timeout);
      }
      assert.match(state.status, /^Запрос отправлен\. Пока показано прежнее состояние\./);
      assert.ok(state.live === 'polite' || state.live === null, 'role=status is polite by default');
      assert.equal(state.disabled, false);
      assert.deepEqual(state.values, expected);
      if (expected.q === 'CPS') assert.match(state.oldCount, /Найдено: 6/);
      release();
      await navigation;
    } catch (error) {
      throw new Error(`Filter submit at ${route} via ${method}: ${error.message}`, {
        cause: error,
      });
    } finally {
      release();
      pending = undefined;
      report = undefined;
    }
  };
  try {
    await page.setCacheEnabled(false);
    setScenario({ release: false, pagination: false, fault: null, delayMs: 0 });
    await page.goto(base + '/features', { waitUntil: 'networkidle0' });
    assert.equal(
      await page.$eval('.filter-form [role="status"]', (el) => el.textContent.trim()),
      '',
    );
    await page.type('input[name=q]', 'CPS');
    setScenario({ delayMs: 1200 });
    await submit('keyboard');
    assert.equal(new URL(page.url()).searchParams.get('q'), 'CPS');
    const filteredUrl = page.url();
    assert.equal(await page.$$eval('.catalog h2', (els) => els.length), 1);
    assert.equal(
      await page.$eval('.filter-form [role="status"]', (el) => el.textContent.trim()),
      '',
    );
    setScenario({ delayMs: 0 });
    await page.goBack({ waitUntil: 'networkidle0' });
    assert.equal(await page.$eval('input[name=q]', (el) => el.value), '');
    assert.equal(
      await page.$eval('.filter-form [role="status"]', (el) => el.textContent.trim()),
      '',
    );
    await page.goForward({ waitUntil: 'networkidle0' });
    await page.waitForFunction(() => document.querySelector('input[name=q]')?.value === 'CPS');
    assert.equal(page.url(), filteredUrl);
    assert.equal(await page.$$eval('.catalog h2', (els) => els.length), 1);
    assert.equal(
      await page.$eval('.filter-form [role="status"]', (el) => el.textContent.trim()),
      '',
    );
    await page.goBack({ waitUntil: 'networkidle0' });
    await page.waitForFunction(() => document.querySelector('input[name=q]')?.value === '');
    await page.type('input[name=q]', 'CPS');
    setScenario({ delayMs: 1200, fault: { path: '/v1/features', kind: 'http' } });
    await submit('pointer');
    assert.match(
      await page.$eval('#content-error-title', (el) => el.textContent),
      /Не удалось загрузить возможности/,
    );
    assert.match(await page.$eval('meta[name="robots"]', (el) => el.content), /noindex/);
    assert.equal(
      await page.$eval('.filter-form [role="status"]', (el) => el.textContent.trim()),
      '',
    );
    setScenario({ fault: null, delayMs: 0, release: true });
    for (const { route, name, value, expected } of [
      { route: '/updates', name: 'type', value: 'release', expected: { type: 'release' } },
      { route: '/download', name: 'channel', value: 'beta', expected: { channel: 'beta' } },
    ]) {
      await page.goto(base + route, { waitUntil: 'networkidle0' });
      await page.select(`select[name=${name}]`, value);
      setScenario({ delayMs: 1200 });
      await submit('pointer', expected);
      assert.equal(new URL(page.url()).searchParams.get(name), value);
      assert.equal(
        await page.$eval('.filter-form [role="status"]', (el) => el.textContent.trim()),
        '',
      );
      setScenario({ delayMs: 0 });
    }
    await page.goto(base + '/help', { waitUntil: 'networkidle0' });
    await page.type('input[name=q]', 'вход');
    await submit('keyboard', { q: 'вход' });
    assert.equal(new URL(page.url()).searchParams.get('q'), 'вход');
    assert.equal(
      await page.$eval('.filter-form [role="status"]', (el) => el.textContent.trim()),
      '',
    );
    results.push({
      id: 'WEB-FILTER-SUBMIT',
      result:
        'Keyboard and pointer GET submissions across features, help, updates and fixture download expose a status about the previous state, keep controls available, and clear the status on arrival, Back and Forward; delayed API failure remains an error with noindex.',
    });
  } finally {
    setScenario({ fault: null, delayMs: 0, release: false });
    await page.close();
  }
  if (!firefox) {
    const cancelled = await browser.newPage();
    const cdp = await cancelled.createCDPSession();
    let slow = false;
    let requestStarted;
    let timer;
    try {
      await cancelled.setRequestInterception(true);
      cancelled.on('request', (request) => {
        if (slow && request.isNavigationRequest() && new URL(request.url()).searchParams.has('q')) {
          requestStarted();
          timer = setTimeout(() => request.continue().catch(() => {}), 3000);
        } else {
          request.continue().catch(() => {});
        }
      });
      await cancelled.goto(base + '/features', { waitUntil: 'networkidle0' });
      await cancelled.type('input[name=q]', 'CPS');
      slow = true;
      const requested = new Promise((resolve) => (requestStarted = resolve));
      await cancelled.keyboard.press('Enter');
      await requested;
      // Stop the delayed document request without touching the user's browser.
      await cdp.send('Page.stopLoading');
      await cancelled.waitForSelector('.filter-form [role="status"]');
      assert.match(
        await cancelled.$eval('.filter-form [role="status"]', (el) => el.textContent),
        /Если ответ не появился, попробуйте снова/,
      );
      assert.equal(await cancelled.$eval('input[name=q]', (el) => el.value), 'CPS');
      slow = false;
      await Promise.all([
        cancelled.waitForNavigation({ waitUntil: 'networkidle0' }),
        cancelled.click('.filter-form button'),
      ]);
      assert.equal(await cancelled.$$eval('.catalog h2', (els) => els.length), 1);
      results.push({
        id: 'WEB-FILTER-STOP-RETRY',
        result:
          'Chrome DevTools stops a delayed native GET; the form retains its query and retry guidance, and a subsequent submission returns the matching result.',
      });
    } finally {
      clearTimeout(timer);
      await cdp.detach();
      await cancelled.close();
    }
  }
}

module.exports = { checkFilterSubmit };

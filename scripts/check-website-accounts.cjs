const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const http = require('node:http');
const { spawn } = require('node:child_process');
const { once } = require('node:events');
const { randomUUID, createHash } = require('node:crypto');
const { createApp } = require('../apps/api/dist/app.js');
const { png } = require('../apps/api/test/account-fixtures.cjs');
const root = path.resolve(__dirname, '..'),
  password = ' local fixture password 123456 ';
const fixtureImage = (w, h) => {
  const raw = Buffer.alloc(h * (w * 4 + 1));
  if (w === 64 && [32, 64].includes(h)) {
    const fill = (x, y, width, height, color) => {
      for (let row = y; row < y + height; row++)
        for (let col = x; col < x + width; col++) raw.set(color, row * (w * 4 + 1) + col * 4 + 1);
    };
    fill(8, 8, 8, 8, [218, 167, 122, 255]);
    fill(20, 20, 8, 12, [34, 67, 108, 255]);
    fill(44, 20, 4, 12, [218, 167, 122, 255]);
    fill(4, 20, 4, 12, [28, 37, 55, 255]);
    if (h === 64) {
      fill(36, 52, 4, 12, [218, 167, 122, 255]);
      fill(20, 52, 4, 12, [28, 37, 55, 255]);
      fill(40, 8, 8, 2, [124, 66, 168, 255]);
      fill(23, 36, 2, 12, [246, 189, 130, 255]); // Distinct jacket stripe over a blue base.
      fill(44, 39, 4, 3, [193, 70, 69, 255]);
      fill(52, 55, 4, 3, [193, 70, 69, 255]);
      fill(4, 45, 4, 3, [228, 232, 237, 255]);
      fill(4, 61, 4, 3, [228, 232, 237, 255]);
    } else {
      fill(44, 20, 2, 12, [200, 40, 30, 255]);
      fill(46, 20, 2, 12, [20, 60, 210, 255]);
      fill(4, 20, 2, 12, [20, 180, 50, 255]);
      fill(6, 20, 2, 12, [230, 180, 20, 255]);
    }
    return png(w, h, { raw });
  }
  for (let y = 0; y < h; y++)
    for (let x = 0; x < w; x++) {
      const offset = y * (w * 4 + 1) + x * 4 + 1;
      raw.set([40 + (x % 8) * 15, 110 + (y % 8) * 10, 160, 255], offset);
    }
  return png(w, h, { raw });
};
async function port() {
  const server = http.createServer();
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  const value = server.address().port;
  await new Promise((r) => server.close(r));
  return value;
}
async function main() {
  const output = path.join(root, '.reports', 'website-accounts', `run-${randomUUID()}`);
  await fs.mkdir(output, { recursive: true });
  let now = Date.now();
  const dataDir = path.join(output, 'accounts');
  let api = await createApp({ accounts: { dataDir, now: () => now } });
  const apiPort = await port();
  await api.listen(apiPort, '127.0.0.1');
  const apiBase = `http://127.0.0.1:${apiPort}`,
    base = `http://127.0.0.1:${await port()}`;
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
      env: { ...process.env, SPACE_API_URL: apiBase, SPACE_ACCOUNT_API_URL: apiBase },
      windowsHide: true,
      stdio: ['ignore', 'pipe', 'pipe'],
    },
  );
  let logs = '',
    exit = null;
  child.stdout.on('data', (x) => (logs += x));
  child.stderr.on('data', (x) => (logs += x));
  child.on('exit', (code, signal) => (exit = { code, signal }));
  let browser;
  const checks = [],
    errors = [];
  const direct = async (route, method = 'GET', body, token) => {
    const response = await fetch(apiBase + '/v1/accounts/' + route, {
      method,
      headers: {
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: 'Bearer ' + token } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    return { status: response.status, body: await response.json() };
  };
  const jar = new Map();
  let csrf = '';
  const cookies = () => [...jar].map(([name, value]) => name + '=' + value).join('; ');
  const bff = async (route, method = 'GET', body, extra = {}) => {
    const response = await fetch(base + '/api/account/' + route, {
      method,
      headers: {
        Cookie: cookies(),
        ...(method === 'GET'
          ? {}
          : { Origin: base, 'Content-Type': 'application/json', 'X-Space-CSRF': csrf }),
        ...extra,
      },
      body: body === undefined ? undefined : typeof body === 'string' ? body : JSON.stringify(body),
    });
    for (const line of response.headers.getSetCookie()) {
      const [pair] = line.split(';'),
        index = pair.indexOf('=');
      jar.set(pair.slice(0, index), pair.slice(index + 1));
    }
    const text = await response.text();
    return { response, text, body: JSON.parse(text) };
  };
  try {
    for (let i = 0; i < 100; i++) {
      try {
        if ((await fetch(base + '/account')).ok) break;
      } catch {}
      if (exit) throw new Error('Next exited before readiness');
      await new Promise((r) => setTimeout(r, 100));
    }
    let result = await bff('session');
    assert.equal(result.body.profile, null);
    csrf = result.body.csrfToken;
    const firstTabCsrf = csrf;
    result = await bff('session');
    assert.equal(result.body.csrfToken, firstTabCsrf, 'Second tab preserves CSRF');
    assert.ok(/HttpOnly/i.test(result.response.headers.getSetCookie()[0]));
    assert.ok(/SameSite=strict/i.test(result.response.headers.getSetCookie()[0]));
    const registration = { username: 'HttpFixture', nickname: 'HttpNick', password };
    for (const headers of [
      { Origin: 'https://unrelated.invalid' },
      { Origin: '' },
      { 'X-Space-CSRF': '' },
      { 'X-Space-CSRF': 'a'.repeat(43) },
    ])
      assert.equal((await bff('register', 'POST', registration, headers)).response.status, 403);
    assert.equal(
      (
        await bff('register', 'POST', registration, {
          'Content-Type': 'application/x-www-form-urlencoded',
        })
      ).response.status,
      415,
    );
    assert.equal((await bff('register', 'POST', '{"bad"')).response.status, 400);
    assert.equal(
      (await bff('register', 'POST', { padding: 'x'.repeat(400000) })).response.status,
      413,
    );
    result = await bff('register', 'POST', registration);
    assert.equal(result.response.status, 200);
    assert.deepEqual(Object.keys(result.body), ['profile']);
    const session = jar.get('space_session');
    assert.ok(session);
    assert.ok(
      !result.text.includes(session) &&
        !result.text.includes('sessionToken') &&
        !result.text.includes(password),
    );
    assert.ok(/HttpOnly/i.test(result.response.headers.getSetCookie()[0]));
    assert.ok(/SameSite=strict/i.test(result.response.headers.getSetCookie()[0]));
    const desktop = await direct('login', 'POST', { username: 'httpfixture', password });
    assert.equal(desktop.status, 201);
    await direct(
      'me',
      'PATCH',
      { nickname: 'DesktopNick', expectedRevision: 1 },
      desktop.body.sessionToken,
    );
    result = await bff('me', 'PATCH', { nickname: 'StaleNick', expectedRevision: 1 });
    assert.equal(result.body.code, 'SPACE_REVISION_CONFLICT');
    result = await bff('session');
    csrf = result.body.csrfToken;
    assert.equal(result.body.profile.nickname, 'DesktopNick');
    result = await bff('me', 'PATCH', { nickname: 'HttpNext', expectedRevision: 2 });
    assert.equal(result.body.profile.revision, 3);
    result = await bff('me/skin', 'PUT', {
      pngBase64: fixtureImage(64, 64).toString('base64'),
      model: 'slim',
      expectedRevision: 3,
    });
    const skin = result.body.profile.skin;
    assert.equal(result.body.profile.revision, 4);
    const media = await fetch(base + '/api/account/assets/' + skin.sha256);
    assert.equal(media.headers.get('content-type'), 'image/png');
    assert.equal(
      createHash('sha256')
        .update(Buffer.from(await media.arrayBuffer()))
        .digest('hex'),
      skin.sha256,
    );
    assert.equal(
      (
        await bff('me/skin', 'PUT', {
          pngBase64: Buffer.from('<svg/>').toString('base64'),
          model: 'classic',
          expectedRevision: 4,
        })
      ).body.code,
      'SPACE_INVALID_IMAGE',
    );
    await api.close();
    api = await createApp({ accounts: { dataDir, now: () => now } });
    await api.listen(apiPort, '127.0.0.1');
    result = await bff('session');
    csrf = result.body.csrfToken;
    assert.equal(result.body.profile.skin.sha256, skin.sha256);
    assert.equal(result.body.profile.nickname, 'HttpNext');
    result = await bff('logout', 'POST', {});
    assert.equal(result.body.ok, true);
    assert.equal(jar.get('space_session'), '');
    assert.equal((await direct('me', 'GET', undefined, session)).status, 401);
    assert.equal(
      (await bff('login', 'POST', { username: 'HttpFixture', password: 'wrong password 12345' }))
        .response.status,
      401,
    );
    await bff('login', 'POST', { username: 'HttpFixture', password });
    now += 7 * 24 * 60 * 60 * 1000 + 1;
    result = await bff('session');
    assert.equal(result.body.profile, null);
    assert.equal(jar.get('space_session'), '');
    await bff('login', 'POST', { username: 'HttpFixture', password });
    const offlineToken = jar.get('space_session');
    await api.close();
    result = await bff('logout', 'POST', {});
    assert.equal(result.response.status, 503);
    assert.equal(result.body.localLogout, true);
    assert.equal(jar.get('space_session'), '');
    assert.match(result.body.message, /до истечения срока/);
    api = await createApp({ accounts: { dataDir, now: () => now } });
    await api.listen(apiPort, '127.0.0.1');
    assert.equal(
      (await direct('me', 'GET', undefined, offlineToken)).status,
      200,
      'Offline logout does not claim server revocation',
    );
    await direct('logout', 'POST', {}, offlineToken);
    checks.push(
      'BFF origin/CSRF/content-type/body bounds, safe public JSON, HttpOnly/SameSite cookies, wrong credentials, shared desktop edits, revision conflict, PNG digest, persistent API restart, logout revocation and expiry.',
    );
    console.log('PASS account BFF HTTP/security and shared persistent service');
    if (!process.argv.includes('--http-only')) {
      const puppeteer = require('puppeteer-core');
      browser = await puppeteer.launch({
        executablePath:
          process.env.SPACE_CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe',
        headless: true,
        pipe: true,
        userDataDir: path.join(output, 'browser-data'),
        args: ['--no-first-run'],
      });
      const skinPath = path.join(output, 'skin.png'),
        capePath = path.join(output, 'cape.png');
      await fs.writeFile(skinPath, fixtureImage(64, 64));
      await fs.writeFile(capePath, fixtureImage(128, 64));
      const legacyPath = path.join(output, 'legacy-skin.png');
      await fs.writeFile(legacyPath, fixtureImage(64, 32));
      const maliciousPath = path.join(output, 'huge-bitmap.png'),
        oversizedPath = path.join(output, 'oversized.png');
      const malicious = fixtureImage(64, 64);
      malicious.writeUInt32BE(100000, 16);
      malicious.writeUInt32BE(100000, 20);
      await fs.writeFile(maliciousPath, malicious);
      await fs.writeFile(oversizedPath, Buffer.alloc(256 * 1024 + 1));
      for (const width of [1440, 390]) {
        const context = await browser.createBrowserContext(),
          page = await context.newPage();
        const consoleMessages = [],
          jsonBodies = [],
          responseReads = [];
        page.on('console', (message) => consoleMessages.push(message.text()));
        page.on('response', (response) => {
          if (
            response.url().includes('/api/account/') &&
            response.headers()['content-type']?.includes('application/json')
          )
            responseReads.push(
              response
                .text()
                .then((text) => jsonBodies.push(text))
                .catch(() => {}),
            );
        });
        page.on('pageerror', (error) => errors.push(error.message));
        await page.setViewport({ width, height: 1000 });
        await page.emulateMediaFeatures([{ name: 'prefers-reduced-motion', value: 'reduce' }]);
        await page.goto(base + '/account', { waitUntil: 'networkidle0' });
        await page.waitForSelector('.account-entry');
        if (width === 390) {
          await page.click('.menu-trigger');
          await page.waitForSelector('dialog[open]');
          assert.ok(await page.$('dialog a[href="/account"]'));
          await page.keyboard.press('Escape');
          assert.equal(
            await page.$eval('.menu-trigger', (element) => element === document.activeElement),
            true,
          );
        }
        assert.equal(await page.$$eval('h1', (x) => x.length), 1);
        assert.equal(
          await page.$eval('html', () => document.documentElement.scrollWidth > innerWidth + 1),
          false,
        );
        await page.screenshot({
          path: path.join(output, `account-login-${width}.png`),
          fullPage: true,
        });
        await page.click('.account-tabs button:nth-child(2)');
        await page.click('.account-entry button[type=submit]');
        await page.waitForSelector('.account-error');
        assert.equal(
          await page.$eval('.account-error', (element) => element === document.activeElement),
          true,
        );
        await page.type('#account-username', 'Browser' + width);
        await page.type('#account-nickname', 'Browser' + width);
        await page.type('#account-password', password);
        await page.type('#account-confirm', password);
        await page.click('.account-entry button[type=submit]');
        await page.waitForSelector('.account-profile');
        assert.equal(await page.evaluate(() => document.cookie.includes('space_session')), false);
        assert.equal(
          await page.evaluate(() =>
            Object.keys(localStorage).some((x) => /session|account|token/i.test(x)),
          ),
          false,
        );
        const stored = (await context.cookies()).find((x) => x.name === 'space_session');
        assert.ok(stored.httpOnly && stored.sameSite === 'Strict');
        await Promise.all(responseReads);
        assert.equal(
          jsonBodies.some(
            (text) =>
              text.includes('sessionToken') ||
              text.includes(stored.value) ||
              text.includes(password),
          ),
          false,
        );
        assert.equal(
          consoleMessages.some((text) => text.includes(stored.value) || text.includes(password)),
          false,
        );
        assert.equal(
          await page.evaluate(
            (origin) => document.documentElement.innerHTML.includes(origin),
            apiBase,
          ),
          false,
        );
        const secondTab = await context.newPage();
        await secondTab.goto(base + '/account', { waitUntil: 'networkidle0' });
        await secondTab.waitForSelector('.account-profile');
        await secondTab.close();
        await page.bringToFront();
        await page.click('#profile-nickname', { count: 3 });
        await page.type('#profile-nickname', 'New' + width);
        await page.click('.account-nickname button');
        await page.waitForFunction(
          (name) => document.querySelector('.account-profile h2')?.textContent === name,
          {},
          'New' + width,
        );
        for (const rejectedPath of [maliciousPath, oversizedPath]) {
          await (await page.$('#skin-file')).uploadFile(rejectedPath);
          await page.waitForSelector('.account-error');
          await page.waitForFunction(() => document.querySelector('#skin-file').value === '');
          assert.match(
            await page.$eval('.account-error', (element) => element.textContent),
            /PNG|Скин/,
          );
          assert.equal(
            await page.$('.appearance-preview img[src^="blob:"], .skin-figure'),
            null,
            'Rejected header must not reach image/canvas decode',
          );
          assert.equal(
            await page.$eval(
              '.account-media:first-child button[type=submit]',
              (element) => element.disabled,
            ),
            true,
          );
        }
        await (await page.$('#skin-file')).uploadFile(legacyPath);
        await page.waitForFunction(
          () =>
            document.querySelector('canvas')?.getContext('2d').getImageData(5, 120, 1, 1)
              .data[0] === 200,
        );
        assert.deepEqual(
          await page.$eval('canvas', (element) => {
            const context = element.getContext('2d');
            return [
              [5, 120],
              [125, 120],
              [45, 260],
              [85, 260],
            ].map(([x, y]) => [...context.getImageData(x, y, 1, 1).data]);
          }),
          [
            [200, 40, 30, 255],
            [20, 60, 210, 255],
            [20, 180, 50, 255],
            [230, 180, 20, 255],
          ],
          'Legacy left limbs mirror the right-limb texture',
        );
        await (
          await page.$('canvas')
        ).screenshot({ path: path.join(output, `account-legacy-preview-${width}.png`) });
        await (await page.$('#skin-file')).uploadFile(skinPath);
        await page.waitForFunction(
          () => !document.querySelector('.account-media:first-child button[type=submit]').disabled,
        );
        await page.waitForFunction(
          () =>
            document.querySelector('canvas')?.getContext('2d').getImageData(80, 120, 1, 1)
              .data[0] === 246,
        );
        assert.deepEqual(
          await page.$eval('canvas', (element) => {
            const context = element.getContext('2d');
            return [
              [5, 120],
              [155, 120],
            ].map(([x, y]) => [...context.getImageData(x, y, 1, 1).data]);
          }),
          [
            [193, 70, 69, 255],
            [193, 70, 69, 255],
          ],
          'Classic sleeves use four texture columns',
        );
        await page.select('#skin-model', 'slim');
        await page.click('.account-media:first-child button[type=submit]');
        await page.waitForFunction(() =>
          document.querySelector('.account-profile .caption')?.textContent.includes('профиля 3.'),
        );
        await (await page.$('#cape-file')).uploadFile(capePath);
        await page.waitForFunction(
          () => !document.querySelector('.account-media:nth-child(2) button[type=submit]').disabled,
        );
        await page.click('.account-media:nth-child(2) button[type=submit]');
        await page.waitForFunction(() =>
          document.querySelector('.account-profile .caption')?.textContent.includes('профиля 4.'),
        );
        await page.waitForFunction(
          () =>
            document.querySelector('canvas')?.getContext('2d').getImageData(80, 120, 1, 1)
              .data[0] === 246,
        );
        assert.deepEqual(
          await page.$eval('canvas', (element) => {
            const context = element.getContext('2d');
            return [
              [80, 5],
              [80, 40],
              [50, 120],
              [80, 120],
              [20, 120],
              [130, 120],
              [60, 300],
              [100, 300],
            ].map(([x, y]) => [...context.getImageData(x, y, 1, 1).data]);
          }),
          [
            [124, 66, 168, 255],
            [218, 167, 122, 255],
            [34, 67, 108, 255],
            [246, 189, 130, 255],
            [193, 70, 69, 255],
            [193, 70, 69, 255],
            [228, 232, 237, 255],
            [228, 232, 237, 255],
          ],
          'Hat/jacket/sleeves/pants alpha layers reach actual canvas pixels',
        );
        assert.deepEqual(
          await page.$eval('canvas', (element) => {
            const context = element.getContext('2d');
            return [
              [5, 120],
              [155, 120],
            ].map(([x, y]) => context.getImageData(x, y, 1, 1).data[3]);
          }),
          [0, 0],
          'Slim sleeves use three texture columns',
        );
        assert.equal(
          await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1),
          false,
        );
        await page.screenshot({
          path: path.join(output, `account-profile-${width}.png`),
          fullPage: true,
        });
        const external = await direct('login', 'POST', { username: 'Browser' + width, password });
        await direct(
          'me',
          'PATCH',
          { nickname: 'Other' + width, expectedRevision: 4 },
          external.body.sessionToken,
        );
        await page.click('.account-nickname button');
        await page.waitForSelector('.account-error');
        assert.match(await page.$eval('.account-error', (x) => x.textContent), /другом клиенте/);
        await page.click('.account-error button');
        await page.waitForFunction(
          (name) => document.querySelector('.account-profile h2')?.textContent === name,
          {},
          'Other' + width,
        );
        await page.click('.account-media:first-child button.secondary');
        await page.waitForFunction(() =>
          document.querySelector('.account-profile .caption')?.textContent.includes('профиля 6.'),
        );
        await page.click('.account-media:nth-child(2) button.secondary');
        await page.waitForFunction(() =>
          document.querySelector('.account-profile .caption')?.textContent.includes('профиля 7.'),
        );
        await page.reload({ waitUntil: 'networkidle0' });
        await page.waitForSelector('.account-profile');
        await page.click('.account-profile button:last-child');
        await page.waitForSelector('.account-entry');
        await page.type('#account-username', 'Browser' + width);
        await page.type('#account-password', 'wrong password 12345');
        await page.click('.account-entry button[type=submit]');
        await page.waitForSelector('.account-error');
        assert.match(await page.$eval('.account-error', (x) => x.textContent), /Неверное/);
        await page.click('#account-password', { count: 3 });
        await page.type('#account-password', password);
        await page.click('.account-entry button[type=submit]');
        await page.waitForSelector('.account-profile');
        await api.close();
        await page.click('.account-profile button:last-child');
        await page.waitForSelector('.account-entry');
        assert.match(
          await page.$eval('.account-error', (element) => element.textContent),
          /до истечения срока/,
        );
        assert.equal(
          (await context.cookies()).some((cookie) => cookie.name === 'space_session'),
          false,
        );
        api = await createApp({ accounts: { dataDir, now: () => now } });
        await api.listen(apiPort, '127.0.0.1');
        await page.screenshot({
          path: path.join(output, `account-offline-logout-${width}.png`),
          fullPage: true,
        });
        await page.type('#account-username', 'Browser' + width);
        await page.type('#account-password', password);
        await page.click('.account-entry button[type=submit]');
        await page.waitForSelector('.account-profile');
        const layoutPage = await context.newPage();
        for (const layoutWidth of [320, 375, 768, 1024, 1440]) {
          await layoutPage.setViewport({ width: layoutWidth, height: 900 });
          await layoutPage.goto(base + '/account', { waitUntil: 'networkidle0' });
          await layoutPage.waitForSelector('.account-profile');
          await layoutPage.addStyleTag({
            content:
              '* {line-height:1.5!important;letter-spacing:.12em!important;word-spacing:.16em!important} p {margin-bottom:2em!important}',
          });
          assert.equal(
            await layoutPage.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1),
            false,
          );
        }
        await layoutPage.close();
        const noScript = await context.newPage();
        await noScript.setJavaScriptEnabled(false);
        await noScript.goto(base + '/account', { waitUntil: 'networkidle0' });
        assert.match(
          await noScript.$eval('noscript', (element) => element.textContent),
          /включите JavaScript/,
        );
        assert.equal(await noScript.$('.account-entry'), null);
        await noScript.close();
        await context.close();
        checks.push(
          `Browser ${width}: register/login/logout, HttpOnly session, reload persistence, nickname, PNG upload/previews/delete, external client conflict/recovery and responsive layout.`,
        );
        console.log('PASS account browser', width);
      }
    }
    assert.deepEqual(errors, []);
    await fs.writeFile(
      path.join(output, 'report.json'),
      JSON.stringify(
        {
          checkedAt: new Date().toISOString(),
          checks,
          errors,
          base,
          apiBase,
          fixtureStores: true,
          publicDeployment: false,
          realAccountsUsed: false,
        },
        null,
        2,
      ),
    );
    console.log(
      JSON.stringify({ report: path.join(output, 'report.json'), checks: checks.length }),
    );
  } catch (error) {
    await fs.writeFile(
      path.join(output, 'failure.json'),
      JSON.stringify({ error: String(error), stack: error.stack, checks, errors, exit }, null, 2),
    );
    throw error;
  } finally {
    await browser?.close();
    child.kill();
    if (child.exitCode === null)
      await Promise.race([once(child, 'close'), new Promise((r) => setTimeout(r, 5000))]);
    await api.close();
    await fs.writeFile(path.join(output, 'next.log'), logs);
    await fs.writeFile(path.join(output, 'next-exit.json'), JSON.stringify(exit));
  }
}
main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

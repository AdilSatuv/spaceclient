const assert = require('node:assert/strict');
const path = require('node:path');
const puppeteer = require('puppeteer-core');

async function checkFirefoxMotion({ executable, base, output, errors }) {
  const browser = await puppeteer.launch({
    browser: 'firefox',
    protocolTimeout: 30000,
    executablePath: executable,
    headless: true,
    userDataDir: path.join(output, 'reduced-motion-profile'),
    extraPrefsFirefox: { 'ui.prefersReducedMotion': 1, 'browser.shell.checkDefaultBrowser': false },
  });
  try {
    const page = await browser.newPage();
    page.on('pageerror', (error) => errors.push(error.message));
    await page.setViewport({ width: 390, height: 900 });
    const response = await page.goto(base, { waitUntil: 'networkidle0' });
    assert.equal(response.status(), 200);
    assert.equal(
      await page.evaluate(() => matchMedia('(prefers-reduced-motion: reduce)').matches),
      true,
    );
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
      await page.$eval('.planet-assembly', (element) => getComputedStyle(element).animationName),
      'none',
    );
    assert.match(
      await page.$eval('.motion-toggle', (element) => element.textContent),
      /Движение уменьшено/,
    );
    await page.screenshot({ path: path.join(output, 'reduced-motion-390.png'), fullPage: false });
  } finally {
    await browser.close();
  }
}
module.exports = { checkFirefoxMotion };

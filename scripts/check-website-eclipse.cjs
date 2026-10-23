const assert = require('node:assert/strict');

async function checkWebsiteEclipse({ browser, page, base, results }) {
  await page.bringToFront();
  await page.goto(base, { waitUntil: 'networkidle0' });
  await page.evaluate(() => localStorage.removeItem('space.motion'));
  await page.reload({ waitUntil: 'networkidle0' });
  const actors =
    '.planet-assembly, .orbit-track, .space-fragments, .starfield-near, .asteroid-one, .distant-comet';
  const allPlaying = (expected) =>
    page.$$eval(
      actors,
      (elements, state) =>
        elements.length === 6 &&
        elements.every((element) => getComputedStyle(element).animationPlayState === state),
      expected,
    );
  for (const width of [1440, 390]) {
    await page.setViewport({ width, height: 900 });
    await page.evaluate(() => scrollTo(0, 0));
    await page.waitForFunction(
      () => document.querySelector('[data-cosmos]').dataset.active === 'true',
    );
    assert.ok(await allPlaying('running'), 'Visible scene must animate');
    await page.evaluate(() => scrollTo(0, document.body.scrollHeight));
    await page.waitForFunction(
      () => document.querySelector('[data-cosmos]').dataset.active === 'false',
    );
    assert.equal(
      await page.$eval(
        '.planet-assembly',
        (element) => getComputedStyle(element).animationPlayState,
      ),
      'paused',
    );
  }
  await page.evaluate(() => scrollTo(0, 0));
  await page.waitForFunction(
    () => document.querySelector('[data-cosmos]').dataset.active === 'true',
  );
  await page.focus('.motion-toggle');
  await page.keyboard.press(' ');
  await page.waitForFunction(() => document.documentElement.dataset.motion === 'paused');
  assert.ok(
    await allPlaying('paused'),
    'Keyboard pause must freeze planets, fragments and global stars',
  );
  await page.reload({ waitUntil: 'networkidle0' });
  assert.ok(await allPlaying('paused'), 'Pause persists after reload');
  await page.click('.motion-toggle');
  await page.waitForFunction(() => document.documentElement.dataset.motion === 'active');
  assert.ok(await allPlaying('running'));
  const foreground = await browser.newPage();
  try {
    await foreground.bringToFront();
    await page.waitForFunction(
      () => document.hidden && document.documentElement.dataset.pageVisible === 'hidden',
    );
    assert.ok(await allPlaying('paused'), 'Hidden tabs must stop decorative animations');
  } finally {
    await foreground.close();
    await page.bringToFront();
  }
  await page.waitForFunction(
    () => !document.hidden && document.querySelector('[data-cosmos]').dataset.active === 'true',
  );
  assert.ok(await allPlaying('running'));
  results.push({
    id: 'WEB-ECLIPSE',
    result:
      'Desktop/mobile scene pauses offscreen; keyboard pause persists across reload; actual tab visibility pauses all decorative CSS animations and resumes on return.',
  });
}
module.exports = { checkWebsiteEclipse };

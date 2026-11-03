const assert = require('node:assert/strict');
const path = require('node:path');

async function checkModuleCatalog({ browser, page, base, output, results, setScenario, firefox }) {
  const goto = async (route) => {
    const response = await page.goto(base + route, { waitUntil: 'networkidle0' });
    assert.equal(response.status(), 200);
  };
  const count = () => page.$$eval('.catalog article', (items) => items.length);
  const text = (selector) => page.$eval(selector, (item) => item.textContent);
  const settleCards = () =>
    page.evaluate(async () => {
      await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)));
      await Promise.all(
        [...document.querySelectorAll('.catalog article')].flatMap((card) =>
          card.getAnimations().map((animation) => animation.finished),
        ),
      );
    });
  for (const width of [390, 1440]) {
    await page.setViewport({ width, height: 1000 });
    await goto('/features');
    assert.equal(await count(), 57);
    const groups = await page.$$eval('.catalog article', (items) =>
      items.reduce((counts, item) => {
        const group = item.dataset.featureKind;
        counts[group] = (counts[group] || 0) + 1;
        return counts;
      }, {}),
    );
    assert.deepEqual(groups, { hud: 26, game: 29, tools: 2 });
    assert.match(await text('.catalog-total'), /55 модулей/);
    assert.match(await text('.catalog-total'), /29 игровых · 26 HUD · 2 инструмента отдельно/);
    assert.match(await text('.result-count'), /Найдено: 57 из 57/);
    await page.screenshot({ path: path.join(output, `catalog-${width}.png`) });
    await page.$eval('.filter-form', (item) => item.scrollIntoView());
    await settleCards();
    await page.screenshot({ path: path.join(output, `catalog-cards-${width}.png`) });

    await goto('/features?category=game');
    assert.equal(await count(), 29);
    await goto('/features?category=hud&status=prototype');
    assert.equal(await count(), 26);
    await goto('/features?category=tools');
    assert.equal(await count(), 2);
    assert.deepEqual(
      await page.$$eval('.catalog h2', (items) => items.map((item) => item.textContent)),
      ['Редактор HUD', 'Профили запуска'],
    );
    await goto('/features?category=launcher');
    assert.equal(await count(), 1);
    assert.match(await text('.catalog'), /Профили запуска/);

    await goto('/features?q=' + encodeURIComponent('насыщение'));
    assert.equal(await count(), 2);
    await goto('/features?q=' + encodeURIComponent('не синхронизирует'));
    assert.equal(await count(), 1);
    await page.focus('.catalog summary');
    await page.keyboard.press('Enter');
    assert.equal(await page.$eval('.catalog details', (item) => item.open), true);
    assert.match(await text('.catalog details'), /не синхронизирует точное насыщение/);
    await page.$eval('.catalog', (item) => item.scrollIntoView());
    await settleCards();
    await page.screenshot({ path: path.join(output, `catalog-estimate-${width}.png`) });

    await goto('/features?q=Zoom&category=hud');
    assert.equal(await count(), 0);
    assert.match(await text('.empty-state'), /Попробуйте/);
    await page.$eval('.filter-form', (item) => item.scrollIntoView());
    await page.screenshot({ path: path.join(output, `catalog-empty-${width}.png`) });
    await goto('/features?category=unknown&status=invalid');
    assert.equal(await count(), 57);
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth > innerWidth + 1,
    );
    assert.equal(overflow, false);
    results.push({
      id: 'WEB-MODULE-CATALOG',
      width,
      result:
        '55 native modules + 2 tools, game/HUD/tools/launcher filters, RU search, keyboard details, estimates, empty state and invalid query defaults.',
    });
  }
  setScenario({ emptyFeatures: true });
  try {
    await goto('/features');
    assert.match(await text('.empty-state'), /Каталог пока пуст/);
    assert.equal(await page.$('.catalog-overview'), null);
    assert.equal(await count(), 0);
    await page.screenshot({ path: path.join(output, 'catalog-unpublished.png') });
    results.push({
      id: 'WEB-MODULE-UNPUBLISHED',
      result:
        'A valid empty API catalog has its own state, distinct from no search matches and API failure.',
    });
  } finally {
    setScenario({ emptyFeatures: false });
  }
  if (!firefox) await checkModuleCatalogNoScript({ browser, base, results });
}

async function checkModuleCatalogNoScript({ browser, base, results, firefox = false }) {
  const noScript = await browser.newPage();
  try {
    // Firefox runs in the existing javascript.enabled=false QA profile.
    if (!firefox) await noScript.setJavaScriptEnabled(false);
    await noScript.goto(base + '/features?category=performance&q=FPS', {
      waitUntil: 'networkidle0',
    });
    assert.equal(await noScript.evaluate(() => matchMedia('(scripting: none)').matches), true);
    assert.equal(await noScript.$$eval('.catalog article', (items) => items.length), 1);
    assert.match(await noScript.$eval('.catalog', (item) => item.textContent), /Dynamic FPS/);
    // A disabled-script Firefox profile cannot run click's IntersectionObserver.
    const button = await noScript.$('.filter-form button');
    await button.evaluate((element) => element.scrollIntoView({ block: 'center' }));
    const bounds = await button.boundingBox();
    assert.ok(bounds && bounds.width > 0 && bounds.height > 0);
    await Promise.all([
      noScript.waitForNavigation({ waitUntil: 'networkidle0' }),
      noScript.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2),
    ]);
    assert.equal(await noScript.$$eval('.catalog article', (items) => items.length), 1);
    results.push({
      id: 'WEB-MODULE-NOJS',
      result: 'Filtered catalog and GET form submission work with JavaScript disabled.',
    });
  } finally {
    await noScript.close();
  }
}
module.exports = { checkModuleCatalog, checkModuleCatalogNoScript };

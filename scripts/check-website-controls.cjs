const assert = require('node:assert/strict');
const foundation = require('../docs/design/space-dark.tokens.json');
const controls = require('../docs/design/control-states.tokens.json');
const rgb = (token) =>
  `rgb(${token.$value.components.map((channel) => Math.round(channel * 255)).join(', ')})`;

async function checkWebsiteControls({ page, base, results }) {
  await page.setViewport({ width: 1440, height: 900 });
  await page.goto(base + '/features', { waitUntil: 'networkidle0' });
  const primary = '.filter-form .button';
  const read = (selector) =>
    page.$eval(selector, (element) => {
      const css = getComputedStyle(element);
      return {
        background: css.backgroundImage,
        surface: css.backgroundColor,
        border: css.borderTopColor,
        width: element.offsetWidth,
        height: element.offsetHeight,
        top: element.offsetTop,
      };
    });
  const initial = await read(primary);
  assert.ok(initial.height >= 52);
  assert.ok(initial.background.includes(rgb(controls.color['primary-end'])));
  await page.hover(primary);
  assert.ok((await read(primary)).background.includes(rgb(controls.color['primary-hover-end'])));
  await page.mouse.down();
  const pressed = await read(primary);
  assert.ok(pressed.background.includes(rgb(controls.color['primary-pressed-end'])));
  assert.deepEqual(
    [pressed.width, pressed.height, pressed.top],
    [initial.width, initial.height, initial.top],
    'Pressed state must not move the surrounding layout',
  );
  await page.mouse.move(0, 0);
  await page.mouse.up();
  await page.keyboard.press('Tab');
  await page.focus('input[name=q]');
  const field = await page.$eval('input[name=q]', (element) => {
    const css = getComputedStyle(element);
    return {
      height: element.offsetHeight,
      fontSize: css.fontSize,
      focus: element.matches(':focus-visible'),
      outline: css.outlineStyle,
      width: css.outlineWidth,
      offset: css.outlineOffset,
    };
  });
  assert.ok(field.height >= 52 && field.focus);
  assert.equal(field.fontSize, '16px');
  assert.equal(field.outline, 'solid');
  assert.equal(field.width, '2px');
  assert.equal(field.offset, '5px');

  await page.goto(base + '/', { waitUntil: 'networkidle0' });
  const secondary = '.hero .button.secondary';
  const secondaryDefault = await read(secondary);
  assert.equal(secondaryDefault.surface, rgb(controls.color['secondary-background']));
  assert.equal(secondaryDefault.border, rgb(controls.color.border));
  await page.hover(secondary);
  await page.waitForFunction(
    (expectedSurface, expectedBorder) => {
      const css = getComputedStyle(document.querySelector('.hero .button.secondary'));
      return css.backgroundColor === expectedSurface && css.borderTopColor === expectedBorder;
    },
    {},
    rgb(controls.color['secondary-hover']),
    rgb(foundation.color.accent),
  );
  assert.equal((await read(secondary)).border, rgb(foundation.color.accent));
  await page.mouse.down();
  await page.waitForFunction(
    (expected) =>
      getComputedStyle(document.querySelector('.hero .button.secondary')).backgroundColor ===
      expected,
    {},
    rgb(controls.color['secondary-pressed']),
  );
  await page.mouse.move(0, 0);
  await page.mouse.up();
  results.push({
    id: 'WEB-CONTROLS',
    result:
      'Real pointer hover/press match shared tokens; primary preserves layout; secondary has distinct surfaces; field is 52 px with visible 2 px focus and 5 px offset.',
  });
}

module.exports = { checkWebsiteControls };

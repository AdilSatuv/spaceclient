const assert = require('node:assert/strict');
const base = process.env.SPACE_SITE_URL || 'http://127.0.0.1:3000';
(async () => {
  for (const route of [
    '/',
    '/features',
    '/download',
    '/updates',
    '/updates/first-contact',
    '/help',
    '/help/getting-started',
    '/help/compatibility',
    '/help/report-a-bug',
  ]) {
    const response = await fetch(base + route);
    const html = await response.text();
    assert.equal(response.status, 200, route);
    assert.equal((html.match(/<h1(?:\s|>)/g) || []).length, 1, route);
    const head = html.slice(0, html.indexOf('</head>'));
    assert.match(
      head,
      /<meta name="description" content="[^"]+"/,
      `${route}: description belongs in head`,
    );
    assert.match(head, /<title>[^<]+<\/title>/, `${route}: title belongs in head`);
    assert.ok(html.includes('SPACE'), route);
    console.log('200', route);
  }
  for (const route of ['/unknown', '/help/unknown', '/updates/unknown'])
    assert.equal((await fetch(base + route)).status, 404, route);
  let html = await (await fetch(base + '/features?category=hud&status=prototype')).text();
  assert.match(html, /Найдено: <!-- -->4/, 'Expected four prototype HUD modules, including CPS');
  assert.ok(html.includes('CPS Counter'), 'CPS prototype must be present in the filtered catalog');
  assert.ok(
    !html.includes('Редактор HUD'),
    'Planned editor must stay outside the prototype filter',
  );
  html = await (await fetch(base + '/features?q=zzzzzz')).text();
  assert.match(html, /Ничего не найдено/);
  html = await (await fetch(base + '/help?q=zzzzzz')).text();
  assert.match(html, /Ответ пока не найден/);
  console.log('Route, head metadata, heading, 404 and filter checks passed.');
})().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});

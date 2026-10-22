const assert = require('node:assert/strict');
const base = process.env.SPACE_SITE_URL || 'http://127.0.0.1:3000';
(async()=>{
 for (const route of ['/', '/features', '/download', '/updates', '/updates/first-contact', '/help', '/help/getting-started', '/help/compatibility', '/help/report-a-bug']) {
  const response=await fetch(base+route);const html=await response.text();
  assert.equal(response.status,200,route);assert.equal((html.match(/<h1(?:\s|>)/g)||[]).length,1,route);
  assert.ok(html.includes('SPACE'),route);console.log('200',route);
 }
 for (const route of ['/unknown','/help/unknown','/updates/unknown']) assert.equal((await fetch(base+route)).status,404,route);
 let html=await(await fetch(base+'/features?category=hud&status=prototype')).text();assert.match(html,/Найдено: <!-- -->3/);
 html=await(await fetch(base+'/features?q=zzzzzz')).text();assert.match(html,/Ничего не найдено/);
 html=await(await fetch(base+'/help?q=zzzzzz')).text();assert.match(html,/Ответ пока не найден/);
 console.log('Route, heading, 404 and filter checks passed.');
})().catch(error=>{console.error(error.message);process.exitCode=1;});

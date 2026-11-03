const test = require('node:test');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const { readdirSync, readFileSync, mkdirSync } = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
test('test worker observations preserve nonzero exits and uncaught failure without logging env or error text', () => {
  const directory = path.resolve(__dirname, '../../../.reports/api-workers');
  mkdirSync(directory, { recursive: true });
  const before = new Set(readdirSync(directory));
  const childEnv = { ...process.env, SPACE_TEST_SECRET: 'do-not-record-this' };
  delete childEnv.NODE_TEST_CONTEXT;
  for (const source of [
    'process.exit(17)',
    "throw Object.assign(new Error('private fixture text'),{code:'QA_WORKER_CRASH'})",
  ]) {
    const result = spawnSync(
      process.execPath,
      [
        '--import',
        pathToFileURL(path.join(__dirname, 'worker-diagnostics.mjs')).href,
        '-e',
        source,
      ],
      {
        encoding: 'utf8',
        windowsHide: true,
        env: childEnv,
      },
    );
    assert.equal(result.status, source.includes('exit(17)') ? 17 : 1);
  }
  const records = readdirSync(directory)
    .filter((file) => !before.has(file))
    .flatMap((file) =>
      readFileSync(path.join(directory, file), 'utf8')
        .trim()
        .split('\n')
        .map((line) => JSON.parse(line)),
    );
  assert.ok(records.some((row) => row.event === 'exit' && row.code === 17));
  assert.ok(
    records.some(
      (row) => row.event === 'uncaughtExceptionMonitor' && row.code === 'QA_WORKER_CRASH',
    ),
  );
  assert.ok(records.some((row) => row.event === 'exit' && row.code === 1));
  const text = JSON.stringify(records);
  assert.ok(!text.includes('do-not-record-this'));
  assert.ok(!text.includes('private fixture text'));
  assert.ok(records.every((row) => typeof row.memory.rss === 'number'));
});

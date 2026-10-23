const test = require('node:test');
const assert = require('node:assert/strict');
const { execFile } = require('node:child_process');
const { promisify } = require('node:util');
const path = require('node:path');
const run = promisify(execFile);
const entry = path.join(__dirname, '../dist/main.js');
test('API startup rejects invalid bind and port configuration before listening', async () => {
  for (const values of [
    { HOST: 'example.com', PORT: '3001' },
    { HOST: '127.0.0.1', PORT: '0' },
    { HOST: '127.0.0.1', PORT: 'not-a-port' },
  ]) {
    await assert.rejects(
      run(process.execPath, [entry], { env: { ...process.env, ...values }, timeout: 5000 }),
      (error) => {
        assert.equal(error.code, 1);
        assert.match(error.stderr, /Invalid (HOST|PORT)/);
        return true;
      },
    );
  }
});

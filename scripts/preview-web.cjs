const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { mkdir, writeFile } = require('node:fs/promises');
const http = require('node:http');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const next = path.join(root, 'node_modules/next/dist/bin/next');
const buildOnly = process.argv.includes('--build');
const prepareOnly = process.argv.includes('--prepare');
function run(args, env) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, args, {
      cwd: root,
      env,
      stdio: 'inherit',
      windowsHide: true,
    });
    child.on('error', reject);
    child.on('exit', (code) =>
      code === 0 ? resolve() : reject(new Error(`Build exited ${code}`)),
    );
  });
}
async function build() {
  const tsc = path.join(root, 'node_modules/typescript/bin/tsc');
  const env = {
    ...process.env,
    SPACE_SITE_URL: '',
    SPACE_SITE_INDEXABLE: 'false',
    NEXT_TELEMETRY_DISABLED: '1',
  };
  await run([tsc, '-p', 'packages/content-contracts/tsconfig.json'], env);
  await run([tsc, '-p', 'apps/api/tsconfig.json'], env);
  await run([next, 'build', path.join(root, 'apps/website')], env);
}
async function main() {
  if (buildOnly || prepareOnly) await build();
  if (prepareOnly) return;
  const { createApp } = require('../apps/api/dist/app.js');
  const api = await createApp();
  await api.listen(0, '127.0.0.1');
  const reservation = http.createServer();
  await new Promise((resolve) => reservation.listen(0, '127.0.0.1', resolve));
  const port = reservation.address().port;
  await new Promise((resolve) => reservation.close(resolve));
  const url = `http://127.0.0.1:${port}`;
  const apiUrl = await api.getUrl();
  const child = spawn(
    process.execPath,
    [
      next,
      'start',
      path.join(root, 'apps/website'),
      '--hostname',
      '127.0.0.1',
      '--port',
      String(port),
    ],
    {
      cwd: root,
      env: {
        ...process.env,
        SPACE_API_URL: apiUrl,
        SPACE_SITE_URL: '',
        SPACE_SITE_INDEXABLE: 'false',
      },
      stdio: ['ignore', 'inherit', 'inherit'],
      windowsHide: true,
    },
  );
  let stopping = false;
  async function stop() {
    if (stopping) return;
    stopping = true;
    child.kill();
    await api.close();
  }
  process.once('SIGINT', () => {
    void stop();
  });
  process.once('SIGTERM', () => {
    void stop();
  });
  child.once('error', (error) => {
    console.error(error.message);
    process.exitCode = 1;
    void stop();
  });
  child.once('exit', (code) => {
    if (!stopping && code) process.exitCode = code;
    void stop();
  });
  try {
    let ready = false;
    for (let attempt = 0; attempt < 100; attempt++) {
      if (stopping) break;
      try {
        if ((await fetch(url)).ok) {
          ready = true;
          break;
        }
      } catch {}
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
    if (stopping) return;
    assert.ok(ready, 'Preview server failed to start.');
    await mkdir(path.join(root, '.reports'), { recursive: true });
    await writeFile(
      path.join(root, '.reports/local-preview.json'),
      JSON.stringify(
        {
          url,
          apiUrl,
          pid: process.pid,
          childPid: child.pid,
          startedAt: new Date().toISOString(),
          publicDeployment: false,
        },
        null,
        2,
      ) + '\n',
    );
    console.log(
      `Space local preview: ${url}\nAPI health: ${apiUrl}/v1/health\nCtrl+C stops both processes. No public deployment.`,
    );
  } catch (error) {
    await stop();
    throw error;
  }
}
main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

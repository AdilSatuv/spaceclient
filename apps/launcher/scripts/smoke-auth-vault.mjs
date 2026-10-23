import { build } from 'esbuild';
import electron from 'electron';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';

if (process.platform !== 'win32') throw new Error('The DPAPI smoke runs only on Windows.');
const repo = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const root = join(repo, '.reports/launcher-auth-vault-smoke', `run-${randomUUID()}`);
await mkdir(join(root, 'electron'), { recursive: true });
await mkdir(join(root, 'data'));
const main = join(root, 'main.cjs');
await build({
  entryPoints: [join(repo, 'apps/launcher/scripts/auth-vault-smoke-main.mjs')],
  outfile: main,
  bundle: true,
  platform: 'node',
  target: 'node24',
  format: 'cjs',
  external: ['electron'],
});
const env = { ...process.env, SPACE_AUTH_VAULT_SMOKE_ROOT: root };
delete env.ELECTRON_RUN_AS_NODE;
delete env.NODE_OPTIONS;
for (const stage of ['write', 'read']) {
  const result = spawnSync(electron, [main, `--vault-${stage}`], {
    cwd: repo,
    env,
    windowsHide: true,
    timeout: 45000,
    encoding: 'utf8',
    maxBuffer: 65536,
  });
  if (result.error || result.status !== 0)
    throw new Error(
      `Windows encrypted cache ${stage} smoke failed (exit ${result.status ?? 'unavailable'}).`,
    );
}
const report = {
  ...JSON.parse(await readFile(join(root, 'write.json'), 'utf8')),
  ...JSON.parse(await readFile(join(root, 'read.json'), 'utf8')),
  recordedAt: new Date().toISOString(),
  scope: 'Fake data only; no OAuth, Xbox, Minecraft ownership or game launch.',
};
const output = join(repo, '.reports/launcher-auth-vault-smoke/report.json');
await writeFile(output, JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report, null, 2));

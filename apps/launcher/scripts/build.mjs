import { build } from 'esbuild';
import { mkdir, copyFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { createRequire } from 'node:module';
import { spawnSync } from 'node:child_process';
import { prepareGameBundle } from './game-bundle.mjs';

await mkdir('dist/renderer', { recursive: true });
const require = createRequire(import.meta.url);
const cssCli = resolve(dirname(require.resolve('@tailwindcss/cli/package.json')), 'dist/index.mjs');
const css = spawnSync(
  process.execPath,
  [cssCli, '-i', 'src/renderer/style.css', '-o', 'dist/renderer/app.css', '--minify'],
  { stdio: 'inherit', windowsHide: true },
);
if (css.error || css.status !== 0) throw new Error('Launcher CSS build failed.');
const gameBundle = await prepareGameBundle(resolve('dist/game'));
await build({
  entryPoints: ['src/main/index.ts'],
  outfile: 'dist/main.cjs',
  bundle: true,
  platform: 'node',
  target: 'node24',
  external: ['electron'],
  format: 'cjs',
  define: { __SPACE_GAME_BUNDLE__: JSON.stringify(gameBundle) },
});
await build({
  entryPoints: ['src/preload.ts'],
  outfile: 'dist/preload.cjs',
  bundle: true,
  platform: 'node',
  target: 'node24',
  external: ['electron'],
  format: 'cjs',
});
await build({
  entryPoints: ['src/renderer/index.tsx'],
  outfile: 'dist/renderer/app.js',
  bundle: true,
  platform: 'browser',
  target: 'chrome156',
  minify: true,
  external: ['./hero.png'],
  define: { 'process.env.NODE_ENV': '"production"' },
});
await copyFile('src/renderer/index.html', 'dist/renderer/index.html');
await copyFile('../website/public/images/space-hero-v2.png', 'dist/renderer/hero.png');
await copyFile('../website/public/images/space-logo-warm.png', 'dist/renderer/logo.png');
await mkdir('dist/licenses', { recursive: true });
await copyFile('../../docs/licenses/shadcn-ui.md', 'dist/licenses/shadcn-ui.md');
await copyFile('../../docs/licenses/lucide.txt', 'dist/licenses/lucide.txt');
console.log('Space Launcher: main, sandboxed preload and renderer built.');

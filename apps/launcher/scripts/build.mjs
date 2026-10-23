import { build } from 'esbuild';
import { mkdir, copyFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { prepareGameBundle } from './game-bundle.mjs';

await mkdir('dist/renderer', { recursive: true });
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
  entryPoints: ['src/renderer/app.tsx'],
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
console.log('Space Launcher: main, sandboxed preload and renderer built.');

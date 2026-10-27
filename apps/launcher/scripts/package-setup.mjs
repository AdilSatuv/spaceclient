import {
  cpSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  readdirSync,
  renameSync,
  writeFileSync,
} from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { createRequire } from 'node:module';
import { execFileSync } from 'node:child_process';
import { build } from 'esbuild';

const launcher = resolve(import.meta.dirname, '..');
const repo = resolve(launcher, '../..');
const require = createRequire(join(launcher, 'package.json'));
const icon = join(launcher, 'assets/space.ico');
const csc = join(
  process.env.WINDIR || 'C:\\Windows',
  'Microsoft.NET/Framework64/v4.0.30319/csc.exe',
);
if (process.platform !== 'win32' || !existsSync(csc) || !existsSync(icon))
  throw new Error(
    'Windows, .NET Framework C# compiler and apps/launcher/assets/space.ico are required.',
  );
const run = (bin, args, options = {}) =>
  execFileSync(bin, args, { cwd: repo, windowsHide: true, stdio: 'inherit', ...options });
run('cmd.exe', ['/d', '/s', '/c', 'npm.cmd run build:launcher'], {
  env: { ...process.env, SPACE_REQUIRE_GAME_BUNDLE: '1' },
});
const outputParent = join(repo, '.reports', 'launcher-setup');
mkdirSync(outputParent, { recursive: true });
const runDir = mkdtempSync(join(outputParent, 'build-'));
const payload = join(runDir, 'payload');
const electronDist = join(dirname(require.resolve('electron/package.json')), 'dist');
cpSync(electronDist, payload, { recursive: true, errorOnExist: true });
renameSync(join(payload, 'electron.exe'), join(payload, 'Space Client.exe'));
const app = join(payload, 'resources', 'app');
mkdirSync(app, { recursive: true });
cpSync(join(launcher, 'dist'), join(app, 'dist'), { recursive: true });
writeFileSync(
  join(app, 'package.json'),
  JSON.stringify(
    { name: 'space-client', productName: 'Space Client', version: '0.1.0', main: 'dist/main.cjs' },
    null,
    2,
  ) + '\n',
);

// Use esbuild's actual input graph; npm ls is unreliable for a junction-backed workspace.
const entries = [
  {
    entryPoints: ['src/main/index.ts'],
    platform: 'node',
    target: 'node24',
    external: ['electron'],
    define: { __SPACE_GAME_BUNDLE__: 'null' },
  },
  { entryPoints: ['src/preload.ts'], platform: 'node', target: 'node24', external: ['electron'] },
  {
    entryPoints: ['src/renderer/index.tsx'],
    platform: 'browser',
    target: 'chrome156',
    external: ['./hero.png'],
    define: { 'process.env.NODE_ENV': '"production"' },
  },
];
const packages = new Map();
for (const entry of entries) {
  const result = await build({
    ...entry,
    absWorkingDir: launcher,
    bundle: true,
    write: false,
    metafile: true,
    logLevel: 'silent',
  });
  for (const name of Object.keys(result.metafile.inputs)) {
    const file = resolve(launcher, name);
    if (!file.toLowerCase().includes('node_modules')) continue;
    let dir = dirname(file);
    while (!existsSync(join(dir, 'package.json'))) {
      const parent = dirname(dir);
      if (parent === dir) throw new Error(`Package metadata not found for ${name}`);
      dir = parent;
    }
    const meta = JSON.parse(readFileSync(join(dir, 'package.json'), 'utf8'));
    packages.set(`${meta.name}@${meta.version}`, { dir, meta });
  }
}
// Tailwind contributes generated CSS even though it is not a JavaScript bundle input.
const tw = dirname(require.resolve('tailwindcss/package.json'));
const twMeta = JSON.parse(readFileSync(join(tw, 'package.json'), 'utf8'));
packages.set(`${twMeta.name}@${twMeta.version}`, { dir: tw, meta: twMeta });
const licenses = join(payload, 'licenses', 'bundled');
mkdirSync(licenses, { recursive: true });
const manifest = [];
for (const [key, { dir, meta }] of [...packages].sort(([a], [b]) => a.localeCompare(b))) {
  const files = readdirSync(dir).filter((name) =>
    /^(license|licence|copying|notice)(\.|$)/i.test(name),
  );
  if (!files.length) throw new Error(`No license file found for bundled dependency ${key}`);
  const dest = join(licenses, key.replace(/[\\/:]/g, '_'));
  mkdirSync(dest, { recursive: true });
  for (const file of files) cpSync(join(dir, file), join(dest, file));
  manifest.push({ name: meta.name, version: meta.version, license: meta.license, files });
}
writeFileSync(join(licenses, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
// Electron's own LICENSE and LICENSES.chromium.html remain at the application root.
const iconTool = join(runDir, 'set-windows-icon.exe');
run(csc, [
  '/nologo',
  '/target:exe',
  `/out:${iconTool}`,
  join(launcher, 'scripts/set-windows-icon.cs'),
]);
run(iconTool, [join(payload, 'Space Client.exe'), icon]);
const zip = join(runDir, 'payload.zip');
const zipScript =
  'Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::CreateFromDirectory($env:SPACE_SETUP_INPUT,$env:SPACE_SETUP_ZIP,[System.IO.Compression.CompressionLevel]::Optimal,$false)';
run('powershell.exe', ['-NoProfile', '-NonInteractive', '-Command', zipScript], {
  env: { ...process.env, SPACE_SETUP_INPUT: payload, SPACE_SETUP_ZIP: zip },
});
const source = join(launcher, 'installer/Setup.cs');
const references = [
  '/r:System.IO.Compression.dll',
  '/r:System.IO.Compression.FileSystem.dll',
  '/r:System.Windows.Forms.dll',
];
const setup = join(runDir, 'Space Client Setup.exe');
run(csc, [
  '/nologo',
  '/target:winexe',
  '/platform:x64',
  ...references,
  `/win32icon:${icon}`,
  `/resource:${zip},SpacePayload`,
  `/out:${setup}`,
  source,
]);
run(csc, [
  '/nologo',
  '/target:exe',
  '/platform:x64',
  '/define:TEST_HARNESS',
  ...references,
  `/out:${join(runDir, 'test-extract.exe')}`,
  source,
]);
console.log(
  JSON.stringify(
    {
      setup,
      payload,
      zip,
      testHarness: join(runDir, 'test-extract.exe'),
      bundledPackages: manifest.length,
    },
    null,
    2,
  ),
);

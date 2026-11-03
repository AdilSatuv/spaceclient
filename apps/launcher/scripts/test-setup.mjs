import { createHash } from 'node:crypto';
import {
  createReadStream,
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  readdirSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { join, relative, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import assert from 'node:assert/strict';

const build = resolve(process.argv[2] || '');
const expectedParent = resolve(import.meta.dirname, '../../../.reports/launcher-setup');
if (!build.startsWith(expectedParent + '\\') || !existsSync(join(build, 'payload.zip')))
  throw new Error('Provide the local setup build directory.');
const harness = join(build, 'test-extract.exe');
const testRoot = mkdtempSync(join(build, 'test-'));
const extract = (zip, target, options = {}) =>
  spawnSync(
    harness,
    [
      zip,
      target,
      options.hash || createHash('sha256').update(readFileSync(zip)).digest('hex'),
      ...(options.cancelAfter === undefined ? [] : [String(options.cancelAfter)]),
    ],
    { windowsHide: true, encoding: 'utf8' },
  );
const full = join(testRoot, 'full');
const extracted = extract(join(build, 'payload.zip'), full);
assert.equal(extracted.status, 0, `Full payload extraction failed: ${extracted.stderr}`);
function files(root, dir = root) {
  return readdirSync(dir, { withFileTypes: true })
    .flatMap((entry) => {
      const path = join(dir, entry.name);
      return entry.isDirectory() ? files(root, path) : [relative(root, path)];
    })
    .sort();
}
async function hash(path) {
  const digest = createHash('sha256');
  for await (const chunk of createReadStream(path)) digest.update(chunk);
  return digest.digest('hex');
}
const original = join(build, 'payload');
const expected = files(original);
assert.deepEqual(files(full), expected);
for (const name of expected) {
  assert.equal(statSync(join(original, name)).size, statSync(join(full, name)).size, name);
  assert.equal(await hash(join(original, name)), await hash(join(full, name)), name);
}
const existing = join(testRoot, 'existing');
mkdirSync(existing);
writeFileSync(join(existing, 'user-file.txt'), 'preserve');
assert.notEqual(extract(join(build, 'payload.zip'), existing).status, 0);
assert.equal(readFileSync(join(existing, 'user-file.txt'), 'utf8'), 'preserve');
const unicode = join(testRoot, 'Профиль Windows с пробелами', 'Space Client');
const unicodeResult = extract(join(build, 'payload.zip'), unicode);
assert.equal(
  unicodeResult.status,
  0,
  'Unicode/profile-path extraction failed: ' + unicodeResult.stderr,
);
assert.deepEqual(files(unicode), expected);
for (const name of expected)
  assert.equal(await hash(join(unicode, name)), await hash(join(original, name)), name);
for (const cancelAfter of [0, 6, 20]) {
  const destination = join(testRoot, 'cancel-' + cancelAfter);
  assert.notEqual(extract(join(build, 'payload.zip'), destination, { cancelAfter }).status, 0);
  assert.equal(existsSync(destination), false);
}
const corrupt = join(testRoot, 'corrupt.zip'),
  broken = readFileSync(join(build, 'payload.zip'));
broken[Math.floor(broken.length / 2)] ^= 1;
writeFileSync(corrupt, broken);
assert.notEqual(
  extract(corrupt, join(testRoot, 'corrupt-target'), {
    hash: readFileSync(join(build, 'payload.sha256'), 'utf8').trim(),
  }).status,
  0,
);
assert.equal(existsSync(join(testRoot, 'corrupt-target')), false);
assert.equal(
  readdirSync(testRoot).some((name) => name.startsWith('.sc-')),
  false,
);
const makeZip = `Add-Type -AssemblyName System.IO.Compression.FileSystem; $f=[IO.File]::Open($env:SPACE_ZIP,[IO.FileMode]::CreateNew); $z=[IO.Compression.ZipArchive]::new($f,[IO.Compression.ZipArchiveMode]::Create); foreach($n in ($env:SPACE_ENTRY -split '\\|')) { $e=$z.CreateEntry($n); $w=[IO.StreamWriter]::new($e.Open()); $w.Write('test'); $w.Dispose() }; $z.Dispose(); $f.Dispose()`;
for (const [label, name] of [
  ['traversal', '../outside.txt'],
  ['backslash', '..\\outside.txt'],
  ['absolute', '/outside.txt'],
  ['duplicate', 'same.txt|same.txt'],
]) {
  const zip = join(testRoot, label + '.zip');
  const generated = spawnSync(
    'powershell.exe',
    ['-NoProfile', '-NonInteractive', '-Command', makeZip],
    {
      windowsHide: true,
      env: { ...process.env, SPACE_ZIP: zip, SPACE_ENTRY: name },
      encoding: 'utf8',
    },
  );
  assert.equal(generated.status, 0, generated.stderr);
  const dest = join(testRoot, label + '-output');
  assert.notEqual(extract(zip, dest).status, 0, label);
  assert.equal(existsSync(dest), false, `${label}: archive must be rejected before writing`);
}
assert.equal(existsSync(join(testRoot, 'outside.txt')), false);
console.log(
  JSON.stringify(
    {
      files: expected.length,
      payloadHashes: 'matched',
      existingTarget: 'preserved',
      unicodeAndSpaces: 'all file hashes matched',
      cancellation: 'three stages leave no target or staging',
      corruption: 'payload SHA-256 rejected before writes',
      hostileArchives: 'rejected before writes',
      testRoot,
    },
    null,
    2,
  ),
);

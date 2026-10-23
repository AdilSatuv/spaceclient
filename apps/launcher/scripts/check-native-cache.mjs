import { readFile, writeFile, readdir, lstat } from 'node:fs/promises';
import { resolve, join } from 'node:path';

// Disk evidence around two UI preparations. This script does not operate the UI.
const reports = resolve(import.meta.dirname, '../../../.reports/launcher-desktop');
const root = join(reports, 'instances/default/minecraft');
const receiptPath = join(root, 'base-installation.json');
const info = await lstat(receiptPath);
if (!info.isFile() || info.isSymbolicLink() || info.size > 65536)
  throw new Error('Invalid receipt');
const receipt = JSON.parse(await readFile(receiptPath, 'utf8'));
if (
  receipt.kind !== 'space-development' ||
  receipt.launchReady !== false ||
  !/^natives\/run-[a-z0-9]+$/i.test(receipt.nativeDirectory)
)
  throw new Error('Prepare the development bundle through the UI first');
const generations = (await readdir(join(root, 'natives')))
  .filter((name) => /^run-[a-z0-9]+$/i.test(name))
  .sort();
const snapshot = {
  directory: receipt.nativeDirectory,
  completedAt: receipt.completedAt,
  generations,
  nativeBytes: receipt.natives.reduce((sum, file) => sum + file.size, 0),
};
const baseline = join(reports, 'native-cache-baseline.json');
if (process.argv[2] === '--before') {
  await writeFile(baseline, JSON.stringify(snapshot, null, 2) + '\n');
  console.log(
    JSON.stringify({
      savedBaseline: true,
      generationCount: generations.length,
      next: 'Repeat preparation in the launcher, then run this script without --before.',
    }),
  );
} else if (process.argv.length === 2) {
  const previous = JSON.parse(await readFile(baseline, 'utf8'));
  if (
    previous.completedAt === snapshot.completedAt ||
    receipt.nativeCacheReused !== true ||
    previous.directory !== snapshot.directory ||
    JSON.stringify(previous.generations) !== JSON.stringify(generations)
  )
    throw new Error('Native generation was not reused by a new completed UI preparation');
  const result = {
    checkedAt: new Date().toISOString(),
    cacheReused: true,
    sameGeneration: true,
    generationCountBefore: previous.generations.length,
    generationCountAfter: generations.length,
    duplicateNativeBytesAvoided: snapshot.nativeBytes,
    note: 'Two completed UI preparations used one verified native generation; existing older generations were preserved. No game launch or DLL loading.',
  };
  await writeFile(
    join(reports, 'native-cache-result.json'),
    JSON.stringify(result, null, 2) + '\n',
  );
  console.log(JSON.stringify(result, null, 2));
} else throw new Error('Expected no argument or --before');

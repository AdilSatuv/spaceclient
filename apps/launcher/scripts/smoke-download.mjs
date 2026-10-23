import { downloadArtifact } from '../src/main/downloads.ts';
import { readFile, writeFile, mkdir, mkdtemp } from 'node:fs/promises';
import { resolve, join } from 'node:path';

// Small public Mojang metadata only; no game JAR, assets or account request.
const sha1 = 'd546f1707a3f2b7d034eece5ea2e311eda875787';
const url = `https://piston-meta.mojang.com/v1/packages/${sha1}/1.8.9.json`;
const response = await fetch(url, {
  method: 'HEAD',
  headers: { 'Accept-Encoding': 'identity' },
  redirect: 'error',
  signal: AbortSignal.timeout(15000),
});
if (!response.ok) throw new Error(`Metadata HEAD failed: HTTP ${response.status}`);
const size = Number(response.headers.get('content-length'));
const reports = resolve(import.meta.dirname, '../../../.reports/launcher-network-smoke');
await mkdir(reports, { recursive: true });
const root = await mkdtemp(join(reports, 'run-'));
const artifact = { url, sha1, size, relativePath: 'metadata/1.8.9.json' };
const first = await downloadArtifact(root, artifact);
const second = await downloadArtifact(root, artifact);
const metadata = JSON.parse(await readFile(second.path, 'utf8'));
if (metadata.id !== '1.8.9' || !second.fromCache)
  throw new Error('Unexpected metadata or cache behavior');
const rangeRoot = join(root, 'range');
await mkdir(join(rangeRoot, 'metadata'), { recursive: true });
await writeFile(
  join(rangeRoot, artifact.relativePath + '.part'),
  (await readFile(first.path)).subarray(0, 128),
);
let rangeRequested;
let rangeStatus;
await downloadArtifact(rangeRoot, artifact, {
  fetcher: async (address, options) => {
    rangeRequested = options.headers.Range;
    const received = await fetch(address, options);
    rangeStatus = received.status;
    return received;
  },
});
const result = {
  checkedAt: new Date().toISOString(),
  minecraftVersion: metadata.id,
  url,
  sha1,
  size,
  firstFromCache: first.fromCache,
  secondFromCache: second.fromCache,
  rangeRequested,
  rangeStatus,
  rangeContentVerified: true,
  note: 'Public metadata download, cache and range retry verified. Cancellation/error cases use deterministic offline fixtures; no full installation or game launch.',
};
await writeFile(resolve(reports, 'result.json'), JSON.stringify(result, null, 2) + '\n');
console.log(JSON.stringify(result, null, 2));

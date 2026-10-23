import { prepareVersionPlan } from '../src/main/prepare-plan.ts';
import { verifiedCachedArtifact, validateArtifact } from '../src/main/downloads.ts';
import { createHash } from 'node:crypto';
import { createReadStream } from 'node:fs';
import { readFile, lstat, writeFile, mkdir } from 'node:fs/promises';
import { resolve, join } from 'node:path';

// Audit this task's isolated UI test profile. Does not authenticate, execute Java or load DLLs.
const reports = resolve(import.meta.dirname, '../../../.reports/launcher-desktop');
const root = join(reports, 'instances/default/minecraft');
const receiptPath = join(root, 'base-installation.json');
const info = await lstat(receiptPath);
if (!info.isFile() || info.isSymbolicLink() || info.size > 65536)
  throw new Error('Invalid receipt');
const receipt = JSON.parse(await readFile(receiptPath, 'utf8'));
if (
  receipt.kind !== 'minecraft-base-only' ||
  receipt.launchReady !== false ||
  !/^natives\/run-[a-z0-9]+$/i.test(receipt.nativeDirectory) ||
  !Array.isArray(receipt.natives) ||
  receipt.natives.length > 2048
)
  throw new Error('Invalid base-only receipt');
const plan = await prepareVersionPlan(root);
if (
  receipt.fileCount !== plan.files.length ||
  receipt.verifiedBytes !== plan.totalBytes ||
  receipt.loggingVersion !== plan.loggingVersion ||
  receipt.loaderVersion !== plan.loaderVersion
)
  throw new Error('Receipt does not match pinned plan');
for (const artifact of plan.files) await verifiedCachedArtifact(root, artifact);
for (const native of receipt.natives) {
  validateArtifact({
    relativePath: native.relativePath,
    size: native.size,
    sha1: '0'.repeat(40),
    url: 'https://libraries.minecraft.net/validation-only',
  });
  let path = root;
  const parts = `${receipt.nativeDirectory}/${native.relativePath}`.split('/');
  for (const part of parts.slice(0, -1)) {
    path = join(path, part);
    const directory = await lstat(path);
    if (!directory.isDirectory() || directory.isSymbolicLink())
      throw new Error('Invalid native directory');
  }
  path = join(path, parts.at(-1));
  const file = await lstat(path);
  if (!file.isFile() || file.isSymbolicLink() || file.size !== native.size)
    throw new Error('Native size mismatch');
  const hash = createHash('sha256');
  for await (const chunk of createReadStream(path)) hash.update(chunk);
  if (hash.digest('hex') !== native.sha256) throw new Error('Native receipt digest mismatch');
}
const result = {
  checkedAt: new Date().toISOString(),
  version: plan.version,
  platform: plan.platform,
  loggingVersion: plan.loggingVersion,
  loaderVersion: plan.loaderVersion,
  sha256PinnedArtifactCount: plan.files.filter((file) => file.sha256).length,
  verifiedFiles: plan.files.length,
  verifiedBytes: plan.totalBytes,
  nativeFilesCheckedAgainstLocalReceipt: receipt.natives.length,
  excludedLibraries: plan.excludedLibraries,
  launchReady: false,
  note: 'Pinned plan files checked by SHA-1, controlled logging/loader/LWJGL artifacts also by SHA-256; extracted natives checked against local receipt SHA-256. This is a base file preparation audit, not a Space launch or authentication test.',
};
await mkdir(reports, { recursive: true });
await writeFile(join(reports, 'base-audit.json'), JSON.stringify(result, null, 2) + '\n');
console.log(JSON.stringify(result, null, 2));

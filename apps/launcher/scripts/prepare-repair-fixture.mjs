import { prepareVersionPlan } from '../src/main/prepare-plan.ts';
import { verifiedCachedArtifact } from '../src/main/downloads.ts';
import { readFile, writeFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { createHash } from 'node:crypto';

// Deliberately corrupt ONE downloaded asset in this task's disposable .reports profile.
// Never accepts a user-supplied installation directory and never touches .minecraft.
const reports = resolve(import.meta.dirname, '../../../.reports/launcher-desktop');
const root = join(reports, 'instances/default/minecraft');
const plan = await prepareVersionPlan(root);
const artifact = plan.files
  .filter((file) => file.relativePath.startsWith('assets/objects/'))
  .sort((a, b) => a.size - b.size)[0];
if (!artifact || artifact.size > 4096) throw new Error('No suitable small fixture');
const target = await verifiedCachedArtifact(root, artifact);
const original = await readFile(target);
await writeFile(join(reports, `repair-original-${artifact.sha1}.bin`), original);
const changed = Buffer.from(original);
changed[0] ^= 0xff;
await writeFile(target, changed);
let rejected = false;
try {
  await verifiedCachedArtifact(root, artifact);
} catch {
  rejected = true;
}
if (!rejected) throw new Error('Corrupt asset unexpectedly passed verification');
const result = {
  checkedAt: new Date().toISOString(),
  relativePath: artifact.relativePath,
  size: artifact.size,
  expectedSha1: artifact.sha1,
  damagedSha1: createHash('sha1').update(changed).digest('hex'),
  corruptionRejected: rejected,
  nextStep: 'Use Space Client → Проверить и восстановить, then run audit-base-installation.mjs.',
};
await writeFile(join(reports, 'repair-fixture.json'), JSON.stringify(result, null, 2) + '\n');
console.log(JSON.stringify(result, null, 2));

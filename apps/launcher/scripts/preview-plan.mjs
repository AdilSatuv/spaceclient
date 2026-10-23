import { prepareVersionPlan } from '../src/main/prepare-plan.ts';
import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '../../../.reports/launcher-plan-smoke');
await mkdir(root, { recursive: true });
const plan = await prepareVersionPlan(root);
await writeFile(resolve(root, 'plan.json'), JSON.stringify(plan, null, 2) + '\n');
const result = {
  checkedAt: new Date().toISOString(),
  version: plan.version,
  platform: plan.platform,
  loggingVersion: plan.loggingVersion,
  loaderVersion: plan.loaderVersion,
  mainClass: plan.mainClass,
  fileCount: plan.files.length,
  assetCount: plan.assetCount,
  classpathCount: plan.classpath.length,
  nativeArchives: plan.natives.map((native) => native.archive),
  excludedLibraries: plan.excludedLibraries,
  totalBytes: plan.totalBytes,
  downloadedMetadataBytes: 18244 + 78494,
  launchReady: plan.launchReady,
  blockers: plan.blockers,
  note: 'Only two pinned JSON files were fetched and hash-verified. Planned game files were not downloaded or launched.',
};
await writeFile(resolve(root, 'result.json'), JSON.stringify(result, null, 2) + '\n');
console.log(JSON.stringify(result, null, 2));

import { prepareVersionPlan } from '../src/main/prepare-plan.ts';
import { downloadPlan } from '../src/main/download-queue.ts';
import { extractNatives } from '../src/main/native-extraction.ts';
import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '../../../.reports/launcher-native-smoke');
await mkdir(root, { recursive: true });
const plan = await prepareVersionPlan(root);
const paths = new Set(plan.natives.map((native) => native.archive));
const archives = plan.files.filter((file) => paths.has(file.relativePath));
const downloaded = await downloadPlan(root, archives);
const extracted = await extractNatives(root, archives);
const result = {
  checkedAt: new Date().toISOString(),
  version: plan.version,
  platform: plan.platform,
  archiveCount: downloaded.completedFiles,
  archiveBytes: downloaded.verifiedBytes,
  extractedBytes: extracted.totalBytes,
  files: extracted.files,
  note: 'Official Windows native archives downloaded, SHA-1 verified and extracted with size/CRC checks. DLLs were not loaded; game was not launched. Failure and cancellation cases use offline fixtures.',
};
await writeFile(resolve(root, 'result.json'), JSON.stringify(result, null, 2) + '\n');
console.log(JSON.stringify(result, null, 2));

import { readFile } from 'node:fs/promises';
import { downloadArtifact } from './downloads.ts';
import { buildVersionPlan, VERSION_METADATA, ASSET_INDEX } from './version-plan.ts';

/** Downloads only two pinned JSON files; no game binaries, extraction or process launch. */
export async function prepareVersionPlan(
  root: string,
  options: { signal?: AbortSignal; fetcher?: typeof fetch } = {},
) {
  const metadata = await downloadArtifact(root, VERSION_METADATA, options);
  options.signal?.throwIfAborted();
  const index = await downloadArtifact(root, ASSET_INDEX, options);
  options.signal?.throwIfAborted();
  const result = buildVersionPlan(
    JSON.parse(await readFile(metadata.path, 'utf8')),
    JSON.parse(await readFile(index.path, 'utf8')),
  );
  options.signal?.throwIfAborted();
  return result;
}

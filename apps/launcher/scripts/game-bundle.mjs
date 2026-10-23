import { inspectSpaceJar } from '../src/main/game-bundle.ts';
import { execFileSync } from 'node:child_process';
import { lstat, mkdir, writeFile, unlink } from 'node:fs/promises';
import { join, resolve } from 'node:path';

export async function prepareGameBundle(output) {
  const repository = resolve(import.meta.dirname, '../../..');
  const source = join(repository, 'game/build/libs/space-client-0.1.0.jar');
  try {
    await lstat(source);
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    if (process.env.SPACE_REQUIRE_GAME_BUNDLE === '1')
      throw new Error('Game JAR missing: build game before packaging the launcher.');
    for (const name of ['space-client.jar', 'space-build.json'])
      await unlink(join(output, name)).catch((missing) => {
        if (missing.code !== 'ENOENT') throw missing;
      });
    console.log('Space game bundle absent: base-file preparation only.');
    return null;
  }
  const options = { cwd: repository, encoding: 'utf8', timeout: 5000, windowsHide: true };
  if (execFileSync('git', ['status', '--porcelain', '--', 'game'], options).trim())
    throw new Error('Commit game source changes before bundling its JAR.');
  const tree = execFileSync('git', ['rev-parse', 'HEAD:game'], options).trim();
  if (!/^[a-f0-9]{40}$/.test(tree)) throw new Error('Invalid committed game source tree');
  const bundle = await inspectSpaceJar(source, tree);
  await mkdir(output, { recursive: true });
  await writeFile(join(output, 'space-client.jar'), bundle.bytes);
  await writeFile(
    join(output, 'space-build.json'),
    JSON.stringify(bundle.manifest, null, 2) + '\n',
  );
  console.log(`Space development bundle ${bundle.manifest.modVersion}: ${bundle.manifest.sha256}`);
  return bundle.manifest;
}

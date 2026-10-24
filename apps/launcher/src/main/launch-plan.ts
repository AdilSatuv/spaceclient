import { createHash } from 'node:crypto';
import { readFile, readdir } from 'node:fs/promises';
import { join, delimiter } from 'node:path';
import { UserFacingError } from './application/user-facing-error.ts';
import { installationTarget, verifiedCachedArtifact } from './downloads.ts';
import { buildVersionPlan, VERSION_METADATA, ASSET_INDEX } from './version-plan.ts';
import type { VersionPlan } from './version-plan.ts';
import { findNativeCache, nativeCacheKey } from './native-cache.ts';
import { inspectSpaceJar } from './game-bundle.ts';
import type { SpaceBundleManifest } from './game-bundle.ts';
import { inspectJava, javaEnvironment } from './java.ts';
import { nickname, javaPath, editProfile } from './validation.ts';
import type { Profile } from '../shared/models.ts';

export type LaunchPlan = {
  executable: string;
  args: string[];
  cwd: string;
  env: NodeJS.ProcessEnv;
};
type ValidationDependencies = {
  runtime: typeof inspectJava;
  version(root: string, signal: AbortSignal): Promise<VersionPlan>;
  space: typeof inspectSpaceJar;
};
const validation: ValidationDependencies = {
  runtime: inspectJava,
  space: inspectSpaceJar,
  version: async (root, signal) => {
    const metadata = await verifiedCachedArtifact(root, VERSION_METADATA, signal);
    const index = await verifiedCachedArtifact(root, ASSET_INDEX, signal);
    return buildVersionPlan(
      JSON.parse(await readFile(metadata, 'utf8')),
      JSON.parse(await readFile(index, 'utf8')),
    );
  },
};

/** Java's name-based offline UUID (UUID v3), deliberately distinct from an authenticated identity. */
export function offlineUuid(name: string): string {
  const bytes = createHash('md5')
    .update('OfflinePlayer:' + nickname(name), 'utf8')
    .digest();
  bytes[6] = (bytes[6]! & 0x0f) | 0x30;
  bytes[8] = (bytes[8]! & 0x3f) | 0x80;
  return bytes.toString('hex');
}

/** No network or receipt-based permission: recheck pinned metadata and every installed byte. */
export async function prepareLocalLaunch(
  root: string,
  profile: Profile,
  name: string,
  pin: SpaceBundleManifest | null,
  signal: AbortSignal,
  dependencies: ValidationDependencies = validation,
): Promise<LaunchPlan> {
  const localName = nickname(name);
  editProfile({ id: profile.id, name: profile.name, memoryMb: profile.memoryMb }, 16384);
  if (!pin)
    throw new UserFacingError(
      'В этой сборке нет Space. Соберите игровой bundle и подготовьте файлы.',
    );
  if (!profile.javaPath) throw new UserFacingError('Выберите Java 21 x64 в настройках профиля.');
  const executable = javaPath(profile.javaPath);
  const runtime = await dependencies.runtime(executable);
  signal.throwIfAborted();
  if (!runtime.usable) throw new UserFacingError(runtime.message);
  const plan = await dependencies.version(root, signal);
  for (const artifact of plan.files) await verifiedCachedArtifact(root, artifact, signal);
  const mod = await installationTarget(root, 'mods/space-client.jar');
  const installed = await dependencies.space(mod, pin.gameSourceTree, signal).catch(() => {
    signal.throwIfAborted();
    throw new UserFacingError(
      'Установленная сборка Space повреждена или устарела. Нажмите «Проверить и восстановить».',
    );
  });
  if (installed.manifest.sha256 !== pin.sha256 || installed.manifest.size !== pin.size)
    throw new UserFacingError(
      'Space не совпадает со сборкой лаунчера. Проверьте и восстановите файлы.',
    );
  // This launch contract supports our pinned mod only, including no version-specific mod folders.
  const mods = await readdir(join(root, 'mods'));
  if (mods.length !== 1 || mods[0] !== 'space-client.jar')
    throw new UserFacingError(
      'Локальный запуск поддерживает только Space. Уберите дополнительные файлы из mods.',
    );
  await installationTarget(root, 'natives/check');
  const archives = plan.files.filter((file) =>
    plan.natives.some((native) => native.archive === file.relativePath),
  );
  const natives = await findNativeCache(join(root, 'natives'), nativeCacheKey(archives), signal);
  if (!natives)
    throw new UserFacingError('Natives не прошли проверку. Нажмите «Проверить и восстановить».');
  signal.throwIfAborted();
  return {
    executable,
    cwd: root,
    env: javaEnvironment(),
    args: [
      '-Xms512M',
      `-Xmx${profile.memoryMb}M`,
      `-Djava.library.path=${natives.directory}`,
      '-Dfile.encoding=UTF-8',
      '-Dfabric.gameVersion=1.8.9',
      `-Dfabric.gameJarPath=${join(root, 'versions/1.8.9/1.8.9.jar')}`,
      '-cp',
      plan.classpath.map((path) => join(root, path)).join(delimiter),
      plan.mainClass,
      '--username',
      localName,
      '--version',
      'Space-1.8.9',
      '--gameDir',
      root,
      '--assetsDir',
      join(root, 'assets'),
      '--assetIndex',
      '1.8',
      '--uuid',
      offlineUuid(localName),
      '--accessToken',
      '0',
      '--userProperties',
      '{}',
      '--userType',
      'legacy',
      '--width',
      '1000',
      '--height',
      '680',
    ],
  };
}

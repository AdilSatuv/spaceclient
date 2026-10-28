import { UserFacingError } from './application/user-facing-error.ts';
import { createHash, randomUUID } from 'node:crypto';
import { lstat, readFile, writeFile, rename, unlink } from 'node:fs/promises';
import { join, dirname } from 'node:path';
import { crc32 } from 'node:zlib';
import { fromBufferPromise } from 'yauzl';
import { installationTarget } from './downloads.ts';

export type SpaceBundleManifest = {
  schemaVersion: 1;
  channel: 'development';
  modVersion: '0.1.0';
  minecraftVersion: '1.8.9';
  loaderVersion: '0.18.3';
  gameSourceTree: string;
  size: number;
  sha256: string;
};
export type InstalledSpace = SpaceBundleManifest & { relativePath: 'mods/space-client.jar' };
const expectedMixins = [
  'ClientMixin',
  'HudMixin',
  'GameMenuMixin',
  'AutoSprintMixin',
  'FreeLookRendererMixin',
  'FreeLookInputMixin',
];
const required = [
  'fabric.mod.json',
  'spaceclient.mixins.json',
  'spaceclient-build.properties',
  'dev/spaceclient/SpaceClient.class',
  'dev/spaceclient/ClickCounter.class',
  'dev/spaceclient/HudConfig.class',
  'dev/spaceclient/mixin/ClientMixin.class',
  'dev/spaceclient/mixin/HudMixin.class',
  'dev/spaceclient/mixin/GameMenuMixin.class',
  'dev/spaceclient/mixin/AutoSprintMixin.class',
  'dev/spaceclient/mixin/FreeLookRendererMixin.class',
  'dev/spaceclient/mixin/FreeLookInputMixin.class',
  'dev/spaceclient/hud/HudPlacement.class',
  'dev/spaceclient/hud/HudProfile.class',
  'dev/spaceclient/hud/HudRenderer.class',
  'dev/spaceclient/hud/HudEditorLayout.class',
  'dev/spaceclient/hud/HudEditorScreen.class',
  'dev/spaceclient/hud/HudEditorConfirmation.class',
  'dev/spaceclient/hud/HudModule.class',
  'dev/spaceclient/hud/HudAppearance.class',
  'dev/spaceclient/hud/HudAppearance$Color.class',
  'dev/spaceclient/hud/HudAppearance$Backdrop.class',
  'dev/spaceclient/hud/HudSettings.class',
  'dev/spaceclient/hud/HudModulesLayout.class',
  'dev/spaceclient/hud/HudModulesScreen.class',
  'dev/spaceclient/hud/HudModuleSettingsScreen.class',
  'dev/spaceclient/hud/SpaceUiFont.class',
  'assets/spaceclient/font/DejaVuSans.ttf',
  'assets/spaceclient/font/LICENSE-DejaVu.txt',
];
function failure(): never {
  throw new UserFacingError(
    'Игровая сборка Space повреждена или несовместима. Пересоберите game и лаунчер.',
  );
}
function stamp(input: Buffer): Record<string, string> {
  const result: Record<string, string> = {};
  for (const line of input.toString('utf8').trim().split(/\r?\n/)) {
    const match = /^([a-zA-Z]+)=([a-zA-Z0-9.-]+)$/.exec(line);
    if (!match || Object.hasOwn(result, match[1])) failure();
    result[match[1]] = match[2];
  }
  if (
    Object.keys(result).length !== 6 ||
    result.schemaVersion !== '1' ||
    result.channel !== 'development' ||
    result.modVersion !== '0.1.0' ||
    result.minecraftVersion !== '1.8.9' ||
    result.loaderVersion !== '0.18.3' ||
    !/^[a-f0-9]{40}$/.test(result.gameSourceTree)
  )
    failure();
  return result;
}

/** Read bounded bytes once, validate the JAR structure and metadata without executing it. */
export async function inspectSpaceJar(path: string, expectedTree?: string, signal?: AbortSignal) {
  signal?.throwIfAborted();
  const info = await lstat(path);
  if (!info.isFile() || info.isSymbolicLink() || info.size < 1 || info.size > 4 * 1024 * 1024)
    failure();
  const bytes = await readFile(path, { signal });
  if (bytes.length !== info.size) failure();
  const zip = await fromBufferPromise(bytes, {
    lazyEntries: true,
    decodeStrings: true,
    strictFileNames: true,
    validateEntrySizes: true,
  });
  const contents = new Map<string, Buffer>();
  const names = new Set<string>();
  let total = 0;
  try {
    if (zip.entryCount > 1024) failure();
    for await (const entry of zip.eachEntry()) {
      signal?.throwIfAborted();
      const name = entry.fileName;
      if (name.startsWith('net/minecraft/')) failure();
      const parts = name.replace(/\/$/, '').split('/');
      if (
        name.length > 220 ||
        !/^[a-z0-9_$][a-z0-9_$./+\-]*$/i.test(name) ||
        parts.some((part) => !part || part === '.' || part === '..') ||
        names.has(name.toLowerCase()) ||
        entry.isEncrypted() ||
        ![0, 8].includes(entry.compressionMethod) ||
        ((entry.externalFileAttributes >>> 16) & 0xf000) === 0xa000
      )
        failure();
      names.add(name.toLowerCase());
      total += entry.uncompressedSize;
      if (entry.uncompressedSize > 1024 * 1024 || total > 16 * 1024 * 1024) failure();
      if (name.endsWith('/')) {
        if (entry.uncompressedSize !== 0) failure();
        continue;
      }
      const stream = await zip.openReadStreamPromise(entry);
      stream.on('error', () => {});
      const abort = () => stream.destroy(new UserFacingError('Проверка сборки отменена.'));
      signal?.addEventListener('abort', abort, { once: true });
      const chunks: Buffer[] = [];
      let size = 0;
      try {
        for await (const chunk of stream) {
          signal?.throwIfAborted();
          const data = Buffer.from(chunk);
          size += data.length;
          if (size > entry.uncompressedSize || size > 1024 * 1024) failure();
          chunks.push(data);
        }
      } finally {
        signal?.removeEventListener('abort', abort);
        stream.destroy();
      }
      const data = Buffer.concat(chunks);
      if (size !== entry.uncompressedSize || crc32(data) !== entry.crc32) failure();
      if (name.endsWith('.class')) {
        if (
          data.length < 8 ||
          data.readUInt32BE(0) !== 0xcafebabe ||
          data.readUInt16BE(6) > 52 ||
          data.readUInt16BE(6) < 45 ||
          data.includes(Buffer.from('net/minecraft/client/MinecraftClient'))
        )
          failure();
      }
      if (required.includes(name)) contents.set(name, data);
    }
  } finally {
    zip.close();
  }
  if (required.some((name) => !contents.has(name))) failure();
  const properties = stamp(contents.get('spaceclient-build.properties')!);
  if (expectedTree && properties.gameSourceTree !== expectedTree)
    throw new UserFacingError(
      'Игровой JAR собран из других исходников. Выполните game build заново.',
    );
  let mod, mixin;
  try {
    mod = JSON.parse(contents.get('fabric.mod.json')!.toString('utf8'));
    mixin = JSON.parse(contents.get('spaceclient.mixins.json')!.toString('utf8'));
  } catch {
    failure();
  }
  if (
    !mod ||
    !mixin ||
    mod.schemaVersion !== 1 ||
    mod.id !== 'spaceclient' ||
    mod.version !== '0.1.0' ||
    mod.environment !== 'client' ||
    mod.depends?.minecraft !== '1.8.9' ||
    mod.depends?.fabricloader !== '>=0.18.3' ||
    !Array.isArray(mod.entrypoints?.client) ||
    mod.entrypoints.client.length !== 1 ||
    mod.entrypoints.client[0] !== 'dev.spaceclient.SpaceClient' ||
    mixin.required !== true ||
    mixin.package !== 'dev.spaceclient.mixin' ||
    mixin.compatibilityLevel !== 'JAVA_8' ||
    !Array.isArray(mixin.client) ||
    mixin.client.length !== expectedMixins.length ||
    !expectedMixins.every((name) => mixin.client.includes(name))
  )
    failure();
  signal?.throwIfAborted();
  const manifest: SpaceBundleManifest = {
    schemaVersion: 1,
    channel: 'development',
    modVersion: '0.1.0',
    minecraftVersion: '1.8.9',
    loaderVersion: '0.18.3',
    gameSourceTree: properties.gameSourceTree,
    size: bytes.length,
    sha256: createHash('sha256').update(bytes).digest('hex'),
  };
  return { manifest, bytes };
}

/** Main-only copy of a build-pinned resource; renderer never selects the source or target. */
export async function installSpaceBundle(
  root: string,
  source: string,
  pin: SpaceBundleManifest,
  signal?: AbortSignal,
): Promise<InstalledSpace> {
  const verified = await inspectSpaceJar(source, pin.gameSourceTree, signal);
  if (verified.manifest.sha256 !== pin.sha256 || verified.manifest.size !== pin.size) failure();
  const relativePath = 'mods/space-client.jar' as const;
  const target = await installationTarget(root, relativePath);
  let temporary: string | undefined;
  try {
    try {
      const existing = await lstat(target);
      if (!existing.isFile() || existing.isSymbolicLink())
        throw new UserFacingError('Файл Space не может быть каталогом или ссылкой.');
      if (
        existing.size === pin.size &&
        createHash('sha256')
          .update(await readFile(target, { signal }))
          .digest('hex') === pin.sha256
      )
        return { ...verified.manifest, relativePath };
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error;
    }
    signal?.throwIfAborted();
    temporary = join(dirname(target), `space-client-${randomUUID()}.tmp`);
    await writeFile(temporary, verified.bytes, { flag: 'wx', mode: 0o600, signal });
    signal?.throwIfAborted();
    await rename(temporary, target);
    temporary = undefined;
    return { ...verified.manifest, relativePath };
  } finally {
    if (temporary) await unlink(temporary).catch(() => {});
  }
}

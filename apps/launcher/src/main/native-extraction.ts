import { createHash } from 'node:crypto';
import { mkdir, lstat, mkdtemp, open, rm } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { crc32 } from 'node:zlib';
import type { Readable } from 'node:stream';
import { openPromise } from 'yauzl';
import { validatePlan } from './download-queue.ts';
import { validateRelativePath, verifiedCachedArtifact } from './downloads.ts';
import { nativeCacheKey, findNativeCache, publishNativeCache } from './native-cache.ts';
import type { DownloadArtifact } from './downloads.ts';

const MAX_ENTRIES = 2048;
const MAX_FILE = 32 * 1024 * 1024;
const MAX_TOTAL = 128 * 1024 * 1024;
export type ExtractedNative = { relativePath: string; size: number; sha256: string };

function entryName(raw: string): { name: string; directory: boolean } {
  const directory = raw.endsWith('/');
  const name = directory ? raw.slice(0, -1) : raw;
  // Reuse the Windows-safe artifact path policy, without trusting an archive's path separator.
  validateRelativePath(name);
  return { name, directory };
}

/** Extract verified native archives into a new, isolated generation. Does not load any DLL. */
export async function extractNatives(
  root: string,
  input: DownloadArtifact[],
  options: { signal?: AbortSignal } = {},
) {
  const archives = validatePlan(input);
  if (
    archives.length > 8 ||
    archives.some(
      (archive) =>
        !archive.relativePath.startsWith('libraries/') ||
        !/-natives-windows(?:-64)?\.jar$/.test(archive.relativePath) ||
        archive.size > MAX_FILE,
    )
  )
    throw new Error('Неверный набор Windows native-архивов.');
  const sources: string[] = [];
  for (const archive of archives)
    sources.push(await verifiedCachedArtifact(root, archive, options.signal));
  options.signal?.throwIfAborted();
  const base = join(resolve(root), 'natives');
  await mkdir(base, { recursive: true });
  const baseInfo = await lstat(base);
  if (!baseInfo.isDirectory() || baseInfo.isSymbolicLink())
    throw new Error('Каталог natives не может быть ссылкой.');
  const key = nativeCacheKey(archives);
  const cached = await findNativeCache(base, key, options.signal);
  if (cached) return cached;
  const stage = await mkdtemp(join(base, 'run-'));
  const names = new Map<string, 'file' | 'directory'>();
  const files: ExtractedNative[] = [];
  let count = 0,
    declaredBytes = 0;
  try {
    for (const source of sources) {
      const zip = await openPromise(source, {
        lazyEntries: true,
        autoClose: true,
        decodeStrings: true,
        validateEntrySizes: true,
        strictFileNames: true,
      });
      try {
        if (zip.entryCount > MAX_ENTRIES - count) throw new Error('Слишком много записей natives.');
        for await (const entry of zip.eachEntry()) {
          options.signal?.throwIfAborted();
          if (++count > MAX_ENTRIES) throw new Error('Слишком много записей natives.');
          const { name, directory } = entryName(entry.fileName);
          const unixType = (entry.externalFileAttributes >>> 16) & 0xf000;
          if (
            (unixType !== 0 && unixType !== (directory ? 0x4000 : 0x8000)) ||
            entry.isEncrypted() ||
            ![0, 8].includes(entry.compressionMethod) ||
            (entry.externalFileAttributes & 0x400) !== 0
          )
            throw new Error('Ссылки, специальные и зашифрованные ZIP entries запрещены.');
          if (
            !Number.isSafeInteger(entry.uncompressedSize) ||
            entry.uncompressedSize < 0 ||
            entry.uncompressedSize > MAX_FILE ||
            (directory && entry.uncompressedSize !== 0)
          )
            throw new Error('Неверный размер записи natives.');
          declaredBytes += entry.uncompressedSize;
          if (declaredBytes > MAX_TOTAL) throw new Error('Распаковка natives превышает 128 МиБ.');
          if (name === 'META-INF' || name.startsWith('META-INF/')) continue;
          const key = name.toLowerCase();
          const previous = names.get(key);
          if (previous && !(previous === 'directory' && directory))
            throw new Error('Конфликт имён файлов natives.');
          let parent = '';
          for (const part of key.split('/').slice(0, -1)) {
            parent = parent ? parent + '/' + part : part;
            if (names.get(parent) === 'file')
              throw new Error('Файл natives конфликтует с каталогом.');
            names.set(parent, 'directory');
          }
          names.set(key, directory ? 'directory' : 'file');
          const target = join(stage, ...name.split('/'));
          if (directory) {
            await mkdir(target, { recursive: true });
            continue;
          }
          await mkdir(dirname(target), { recursive: true });
          let stream: Readable | undefined;
          const abort = () => stream?.destroy(new Error('Распаковка отменена.'));
          let destination;
          let size = 0,
            crc = 0;
          const digest = createHash('sha256');
          try {
            options.signal?.throwIfAborted();
            destination = await open(target, 'wx', 0o600);
            stream = await zip.openReadStreamPromise(entry);
            // Keep early stream errors handled until the async iterator attaches its listener.
            stream.on('error', () => {});
            options.signal?.addEventListener('abort', abort, { once: true });
            options.signal?.throwIfAborted();
            for await (const chunk of stream) {
              options.signal?.throwIfAborted();
              const bytes = Buffer.from(chunk);
              size += bytes.length;
              if (size > entry.uncompressedSize || size > MAX_FILE)
                throw new Error('ZIP превысил объявленный размер.');
              crc = crc32(bytes, crc);
              digest.update(bytes);
              let offset = 0;
              while (offset < bytes.length) {
                const result = await destination.write(bytes, offset, bytes.length - offset);
                if (!result.bytesWritten) throw new Error('Не удалось записать natives.');
                offset += result.bytesWritten;
              }
            }
          } finally {
            options.signal?.removeEventListener('abort', abort);
            stream?.destroy();
            await destination?.close();
          }
          if (size !== entry.uncompressedSize || crc !== entry.crc32)
            throw new Error('Проверка размера или CRC natives не пройдена.');
          files.push({ relativePath: name, size, sha256: digest.digest('hex') });
        }
      } finally {
        zip.close();
      }
    }
    options.signal?.throwIfAborted();
    if (!files.length) throw new Error('Архивы не содержат файлов natives.');
    await publishNativeCache(base, key, stage, files, options.signal);
    return {
      directory: stage,
      files,
      totalBytes: files.reduce((sum, file) => sum + file.size, 0),
      fromCache: false as const,
    };
  } catch (error) {
    // Only remove this call's fresh generation, never an existing installation.
    if (dirname(stage) !== base || !stage.startsWith(join(base, 'run-')))
      throw new Error('Не удалось проверить каталог очистки natives.', { cause: error });
    await rm(stage, { recursive: true, force: true });
    if (options.signal?.aborted) throw options.signal.reason;
    throw error;
  }
}

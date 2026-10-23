import { UserFacingError } from './application/user-facing-error.ts';
import { createHash, randomUUID } from 'node:crypto';
import { createReadStream } from 'node:fs';
import { lstat, readFile, writeFile, rename, unlink, readdir } from 'node:fs/promises';
import { join, dirname, basename, resolve } from 'node:path';
import { validateRelativePath } from './downloads.ts';
import type { DownloadArtifact } from './downloads.ts';
import type { ExtractedNative } from './native-extraction.ts';

const MAX_INDEX = 1024 * 1024;
export function nativeCacheKey(archives: DownloadArtifact[]): string {
  const ordered = [...archives].sort((a, b) =>
    a.relativePath < b.relativePath ? -1 : a.relativePath > b.relativePath ? 1 : 0,
  );
  return createHash('sha256')
    .update(JSON.stringify({ policy: 1, archives: ordered }))
    .digest('hex');
}
function indexPath(base: string, key: string) {
  if (!/^[a-f0-9]{64}$/.test(key)) throw new UserFacingError('Неверный ключ кеша natives.');
  return join(base, `cache-${key}.json`);
}
async function indexInfo(path: string) {
  try {
    const info = await lstat(path);
    if (!info.isFile() || info.isSymbolicLink())
      throw new UserFacingError('Индекс кеша natives не может быть каталогом или ссылкой.');
    return info;
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') return null;
    throw error;
  }
}
async function checkedFile(directory: string, file: ExtractedNative, signal?: AbortSignal) {
  let current = directory;
  const parts = validateRelativePath(file.relativePath).split('/');
  for (const part of parts.slice(0, -1)) {
    current = join(current, part);
    const info = await lstat(current);
    if (!info.isDirectory() || info.isSymbolicLink()) return false;
  }
  const path = join(current, parts.at(-1)!);
  const info = await lstat(path);
  if (!info.isFile() || info.isSymbolicLink() || info.size !== file.size) return false;
  const hash = createHash('sha256');
  for await (const chunk of createReadStream(path)) {
    signal?.throwIfAborted();
    hash.update(chunk);
  }
  signal?.throwIfAborted();
  return hash.digest('hex') === file.sha256;
}
async function noUnexpectedFiles(directory: string, expected: Set<string>, signal?: AbortSignal) {
  const pending = [''];
  const seen = new Set<string>();
  let visited = 0;
  while (pending.length) {
    signal?.throwIfAborted();
    const parent = pending.pop()!;
    const entries = await readdir(join(directory, parent), { withFileTypes: true });
    if (entries.length > 2048) return false;
    for (const entry of entries) {
      if (++visited > 2048 * 220 || entry.isSymbolicLink()) return false;
      const relative = parent ? parent + '/' + entry.name : entry.name;
      const normalized = relative.toLowerCase();
      if (seen.has(normalized)) return false;
      seen.add(normalized);
      if (entry.isDirectory()) pending.push(relative);
      else if (!entry.isFile() || !expected.has(relative.toLowerCase())) return false;
    }
  }
  return true;
}

/** A local integrity cache, not a publisher signature; archives must be verified before this call. */
export async function findNativeCache(base: string, key: string, signal?: AbortSignal) {
  signal?.throwIfAborted();
  const path = indexPath(base, key);
  const info = await indexInfo(path);
  if (!info || info.size > MAX_INDEX) return null;
  try {
    const bytes = await readFile(path, { signal });
    if (bytes.length > MAX_INDEX) return null;
    const value = JSON.parse(bytes.toString('utf8'));
    if (
      !value ||
      value.schemaVersion !== 1 ||
      value.key !== key ||
      typeof value.directory !== 'string' ||
      !/^run-[a-z0-9]+$/i.test(value.directory) ||
      !Array.isArray(value.files) ||
      !value.files.length ||
      value.files.length > 2048
    )
      return null;
    const directory = join(base, value.directory);
    const folder = await lstat(directory);
    if (!folder.isDirectory() || folder.isSymbolicLink()) return null;
    const expected = new Set<string>();
    const files: ExtractedNative[] = [];
    let totalBytes = 0;
    for (const item of value.files) {
      if (
        !item ||
        typeof item.relativePath !== 'string' ||
        !Number.isSafeInteger(item.size) ||
        item.size < 0 ||
        item.size > 32 * 1024 * 1024 ||
        typeof item.sha256 !== 'string' ||
        !/^[a-f0-9]{64}$/.test(item.sha256)
      )
        return null;
      const name = validateRelativePath(item.relativePath);
      const normalized = name.toLowerCase();
      if (expected.has(normalized)) return null;
      expected.add(normalized);
      totalBytes += item.size;
      if (totalBytes > 128 * 1024 * 1024) return null;
      const file = { relativePath: name, size: item.size, sha256: item.sha256 };
      if (!(await checkedFile(directory, file, signal))) return null;
      files.push(file);
    }
    if (value.totalBytes !== totalBytes || !(await noUnexpectedFiles(directory, expected, signal)))
      return null;
    signal?.throwIfAborted();
    return { directory, files, totalBytes, fromCache: true as const };
  } catch (error) {
    if (signal?.aborted) throw signal.reason;
    // Corrupt/missing generations are rebuilt into a new directory, never repaired in place.
    return null;
  }
}

export async function publishNativeCache(
  base: string,
  key: string,
  directory: string,
  files: ExtractedNative[],
  signal?: AbortSignal,
) {
  if (dirname(directory) !== resolve(base) || !/^run-[a-z0-9]+$/i.test(basename(directory)))
    throw new UserFacingError('Неверный каталог генерации natives.');
  const target = indexPath(base, key);
  await indexInfo(target);
  signal?.throwIfAborted();
  const body =
    JSON.stringify({
      schemaVersion: 1,
      key,
      directory: basename(directory),
      files,
      totalBytes: files.reduce((sum, file) => sum + file.size, 0),
    }) + '\n';
  if (Buffer.byteLength(body) > MAX_INDEX)
    throw new UserFacingError('Индекс natives превысил допустимый размер.');
  let temporary: string | undefined = join(base, `cache-${key}-${randomUUID()}.tmp`);
  try {
    await writeFile(temporary, body, { flag: 'wx', mode: 0o600, signal });
    signal?.throwIfAborted();
    await rename(temporary, target);
    temporary = undefined;
  } finally {
    if (temporary) await unlink(temporary).catch(() => {});
  }
}

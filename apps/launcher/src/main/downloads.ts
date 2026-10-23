import { createHash } from 'node:crypto';
import { createReadStream } from 'node:fs';
import { mkdir, lstat, open, rename, unlink } from 'node:fs/promises';
import { join, resolve } from 'node:path';

export type DownloadArtifact = { url: string; sha1: string; size: number; relativePath: string };
export class DownloadFailure extends Error {
  readonly retryable: boolean;
  constructor(message: string, retryable = false, cause?: unknown) {
    super(message, { cause });
    this.name = 'DownloadFailure';
    this.retryable = retryable;
  }
}
type DownloadOptions = {
  signal?: AbortSignal;
  onProgress?: (received: number, total: number) => void;
  // Internal transport substitution for tests; never exposed through IPC.
  fetcher?: typeof fetch;
};
const active = new Set<string>();
const hosts = new Set([
  'launcher.mojang.com',
  'piston-data.mojang.com',
  'launchermeta.mojang.com',
  'piston-meta.mojang.com',
  'libraries.minecraft.net',
  'resources.download.minecraft.net',
]);

export function validateArtifact(value: DownloadArtifact): DownloadArtifact {
  if (
    !value ||
    typeof value !== 'object' ||
    typeof value.sha1 !== 'string' ||
    !/^[a-f0-9]{40}$/.test(value.sha1) ||
    !Number.isSafeInteger(value.size) ||
    value.size < 1 ||
    value.size > 512 * 1024 * 1024
  )
    throw new Error('Неверные размер или хеш файла.');
  if (
    typeof value.relativePath !== 'string' ||
    value.relativePath.length > 220 ||
    !/^[a-z0-9][a-z0-9._/-]*$/i.test(value.relativePath) ||
    value.relativePath
      .split('/')
      .some(
        (part) =>
          !part ||
          part === '.' ||
          part === '..' ||
          part.endsWith('.') ||
          /^(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\.|$)/i.test(part),
      )
  )
    throw new Error('Недопустимый путь файла.');
  let url: URL;
  try {
    url = new URL(value.url);
  } catch {
    throw new Error('Недопустимый источник файла.');
  }
  if (
    url.protocol !== 'https:' ||
    !hosts.has(url.hostname) ||
    url.port ||
    url.username ||
    url.password ||
    url.search ||
    url.hash ||
    !/^\/[a-z0-9._/+\-]+(?:\/[a-z0-9._+\-]+)*$/i.test(url.pathname)
  )
    throw new Error('Недопустимый источник файла.');
  if (
    ['launcher.mojang.com', 'piston-data.mojang.com'].includes(url.hostname) &&
    !url.pathname.startsWith(`/v1/objects/${value.sha1}/`)
  )
    throw new Error('Хеш источника не совпадает с манифестом.');
  if (
    url.hostname === 'resources.download.minecraft.net' &&
    url.pathname !== `/${value.sha1.slice(0, 2)}/${value.sha1}`
  )
    throw new Error('Хеш ресурса не совпадает с манифестом.');
  return { url: url.href, sha1: value.sha1, size: value.size, relativePath: value.relativePath };
}

async function fileInfo(path: string) {
  try {
    const info = await lstat(path);
    if (!info.isFile() || info.isSymbolicLink())
      throw new Error('Файл загрузки не может быть каталогом или ссылкой.');
    return info;
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') return null;
    throw error;
  }
}
async function matches(
  path: string,
  artifact: DownloadArtifact,
  signal?: AbortSignal,
): Promise<boolean> {
  const info = await fileInfo(path);
  if (!info || info.size !== artifact.size) return false;
  const digest = createHash('sha1');
  for await (const chunk of createReadStream(path)) {
    signal?.throwIfAborted();
    digest.update(chunk);
  }
  signal?.throwIfAborted();
  return digest.digest('hex') === artifact.sha1;
}
async function targetPath(root: string, relativePath: string): Promise<string> {
  const base = resolve(root);
  await mkdir(base, { recursive: true });
  if ((await lstat(base)).isSymbolicLink())
    throw new Error('Каталог установки не может быть ссылкой.');
  let current = base;
  const parts = relativePath.split('/');
  for (const part of parts.slice(0, -1)) {
    current = join(current, part);
    try {
      await mkdir(current);
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== 'EEXIST') throw error;
    }
    const info = await lstat(current);
    if (!info.isDirectory() || info.isSymbolicLink())
      throw new Error('Путь установки содержит ссылку или файл вместо папки.');
  }
  return join(current, parts.at(-1)!);
}

/** Download one declared artifact. This does not install or launch Minecraft. */
export async function downloadArtifact(
  root: string,
  input: DownloadArtifact,
  options: DownloadOptions = {},
) {
  const artifact = validateArtifact(input);
  options.signal?.throwIfAborted();
  const target = await targetPath(root, artifact.relativePath);
  const lock = process.platform === 'win32' ? target.toLowerCase() : target;
  if (active.has(lock)) throw new Error('Этот файл уже загружается.');
  active.add(lock);
  try {
    if (await matches(target, artifact, options.signal)) {
      options.onProgress?.(artifact.size, artifact.size);
      return { path: target, fromCache: true };
    }
    const partial = target + '.part';
    let offset = (await fileInfo(partial))?.size ?? 0;
    if (offset === artifact.size && (await matches(partial, artifact, options.signal))) {
      await rename(partial, target);
      options.onProgress?.(artifact.size, artifact.size);
      return { path: target, fromCache: false };
    }
    if (offset >= artifact.size) {
      await unlink(partial);
      offset = 0;
    }
    const signal = options.signal
      ? AbortSignal.any([options.signal, AbortSignal.timeout(30000)])
      : AbortSignal.timeout(30000);
    const headers: Record<string, string> = { 'Accept-Encoding': 'identity' };
    if (offset > 0) headers.Range = `bytes=${offset}-`;
    let response: Response;
    try {
      response = await (options.fetcher ?? fetch)(artifact.url, {
        headers,
        signal,
        redirect: 'error',
      });
    } catch (error) {
      signal.throwIfAborted();
      throw new DownloadFailure('Не удалось связаться с сервером загрузки.', true, error);
    }
    const ranged = response.status === 206;
    if (response.status !== 200 && !ranged) {
      await response.body?.cancel();
      throw new DownloadFailure(
        `Сервер загрузки вернул HTTP ${response.status}. Повторите позже.`,
        response.status === 429 || response.status >= 500,
      );
    }
    if (
      ranged &&
      (offset === 0 ||
        response.headers.get('content-range') !==
          `bytes ${offset}-${artifact.size - 1}/${artifact.size}`)
    ) {
      await response.body?.cancel();
      throw new Error('Сервер вернул неверный диапазон файла.');
    }
    if (!ranged) offset = 0;
    const declared = response.headers.get('content-length');
    if (!response.body || (declared !== null && Number(declared) !== artifact.size - offset)) {
      await response.body?.cancel();
      throw new Error('Размер ответа не совпадает с манифестом.');
    }
    const reader = response.body.getReader();
    let file;
    let received = offset;
    try {
      file = await open(partial, offset ? 'a' : 'w', 0o600);
      options.onProgress?.(received, artifact.size);
      while (true) {
        signal.throwIfAborted();
        let chunk: ReadableStreamReadResult<Uint8Array>;
        try {
          chunk = await reader.read();
        } catch (error) {
          signal.throwIfAborted();
          throw new DownloadFailure(
            'Соединение прервалось. Сохранённую часть можно продолжить.',
            true,
            error,
          );
        }
        const { done, value } = chunk;
        if (done) break;
        if (received + value.byteLength > artifact.size)
          throw new Error('Ответ превысил размер файла из манифеста.');
        let written = 0;
        while (written < value.byteLength) {
          const result = await file.write(value, written, value.byteLength - written);
          if (result.bytesWritten === 0) throw new Error('Не удалось записать файл загрузки.');
          written += result.bytesWritten;
        }
        received += value.byteLength;
        options.onProgress?.(received, artifact.size);
      }
      signal.throwIfAborted();
    } finally {
      await reader.cancel().catch(() => {});
      await file?.close();
    }
    if (received !== artifact.size)
      throw new DownloadFailure(
        'Загрузка оборвалась. При повторе продолжим с сохранённой части.',
        true,
      );
    if (!(await matches(partial, artifact, signal))) {
      await unlink(partial);
      throw new DownloadFailure(
        'Хеш файла не совпал. Повреждённая загрузка удалена; повторите попытку.',
        true,
      );
    }
    await rename(partial, target);
    return { path: target, fromCache: false };
  } finally {
    active.delete(lock);
  }
}

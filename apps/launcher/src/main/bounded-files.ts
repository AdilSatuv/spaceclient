import { lstat, open, rename, unlink } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import { installationTarget } from './downloads.ts';
import { UserFacingError } from './application/user-facing-error.ts';

export async function readBounded(path: string, limit: number): Promise<Buffer | null> {
  try {
    const before = await lstat(path);
    if (!before.isFile() || before.isSymbolicLink() || before.nlink !== 1 || before.size > limit)
      throw 0;
    const file = await open(path, 'r');
    try {
      const metadata = await file.stat();
      if (
        metadata.ino !== before.ino ||
        metadata.dev !== before.dev ||
        metadata.size > limit ||
        metadata.nlink !== 1
      )
        throw 0;
      const bytes = Buffer.alloc(metadata.size + 1);
      let length = 0;
      while (length < bytes.length) {
        const result = await file.read(bytes, length, bytes.length - length, null);
        if (!result.bytesRead) break;
        length += result.bytesRead;
      }
      if (length !== metadata.size) throw 0;
      return bytes.subarray(0, length);
    } finally {
      await file.close();
    }
  } catch (error) {
    if ((error as NodeJS.ErrnoException)?.code === 'ENOENT') return null;
    throw new UserFacingError('Файл недоступен, превышает лимит или является ссылкой.');
  }
}
export async function atomicFile(root: string, relative: string, bytes: Buffer | null) {
  const target = await installationTarget(root, relative);
  await readBounded(target, 16 * 1024 * 1024);
  if (bytes === null) {
    await unlink(target).catch((e) => {
      if (e.code !== 'ENOENT') throw e;
    });
    return;
  }
  const temporary = target + '.' + randomUUID() + '.tmp';
  try {
    const file = await open(temporary, 'wx', 0o600);
    try {
      await file.writeFile(bytes);
      await file.sync();
    } finally {
      await file.close();
    }
    await installationTarget(root, relative);
    await readBounded(target, 16 * 1024 * 1024);
    await rename(temporary, target);
  } finally {
    await unlink(temporary).catch(() => {});
  }
}
/** Fixed paths supplied by main only. Restore every previous byte if any stage fails. */
export async function fileTransaction(
  root: string,
  paths: readonly string[],
  limit: number,
  operation: () => Promise<void>,
  writer = atomicFile,
) {
  const before = new Map<string, Buffer | null>();
  for (const path of paths)
    before.set(path, await readBounded(await installationTarget(root, path), limit));
  try {
    await operation();
  } catch (error) {
    try {
      for (const [path, bytes] of before) await writer(root, path, bytes);
    } catch {
      throw new UserFacingError(
        'Не удалось завершить откат файлов. Сохранённые данные требуют проверки перед запуском.',
      );
    }
    throw error;
  }
}

import { UserFacingError } from './application/user-facing-error.ts';
import { setTimeout as delay } from 'node:timers/promises';
import { downloadArtifact, validateArtifact, DownloadFailure } from './downloads.ts';
import type { DownloadArtifact } from './downloads.ts';

export type QueueProgress = {
  state: 'running' | 'retrying' | 'completed' | 'cancelled' | 'failed';
  totalFiles: number;
  completedFiles: number;
  activeFiles: number;
  totalBytes: number;
  receivedBytes: number;
  verifiedBytes: number;
  retryCount: number;
};
type QueueOptions = {
  signal?: AbortSignal;
  concurrency?: number;
  retries?: number;
  onProgress?: (progress: QueueProgress) => void;
  fetcher?: typeof fetch;
  wait?: (milliseconds: number, signal: AbortSignal) => Promise<unknown>;
};
export function validatePlan(input: DownloadArtifact[]): DownloadArtifact[] {
  if (!Array.isArray(input) || input.length < 1 || input.length > 10000)
    throw new UserFacingError('План должен содержать от 1 до 10000 файлов.');
  const files = new Map<string, DownloadArtifact>();
  let total = 0;
  for (const raw of input) {
    const artifact = validateArtifact(raw);
    // The target platform is Windows, regardless of the platform running unit tests.
    const key = artifact.relativePath.toLowerCase();
    const previous = files.get(key);
    if (previous) {
      if (
        previous.sha1 !== artifact.sha1 ||
        previous.sha256 !== artifact.sha256 ||
        previous.size !== artifact.size ||
        previous.url !== artifact.url
      )
        throw new UserFacingError('План содержит конфликтующие файлы.');
      continue;
    }
    total += artifact.size;
    if (total > 2 * 1024 * 1024 * 1024)
      throw new UserFacingError('План превышает допустимые 2 ГиБ для текущего прототипа.');
    files.set(key, artifact);
  }
  for (const key of files.keys()) {
    if (files.has(key + '.part'))
      throw new UserFacingError('Файл плана конфликтует с временной загрузкой.');
    let parent = '';
    for (const part of key.split('/').slice(0, -1)) {
      parent = parent ? parent + '/' + part : part;
      if (files.has(parent)) throw new UserFacingError('Файл плана конфликтует с каталогом.');
      if (parent.endsWith('.part') && files.has(parent.slice(0, -5)))
        throw new UserFacingError('Каталог плана конфликтует с временной загрузкой.');
    }
  }
  return [...files.values()];
}

export async function downloadPlan(
  root: string,
  input: DownloadArtifact[],
  options: QueueOptions = {},
) {
  const files = validatePlan(input);
  const concurrency = options.concurrency ?? 3;
  const retries = options.retries ?? 2;
  if (
    !Number.isInteger(concurrency) ||
    concurrency < 1 ||
    concurrency > 4 ||
    !Number.isInteger(retries) ||
    retries < 0 ||
    retries > 2
  )
    throw new UserFacingError('Неверные ограничения очереди загрузки.');
  options.signal?.throwIfAborted();
  const controller = new AbortController();
  const signal = options.signal
    ? AbortSignal.any([options.signal, controller.signal])
    : controller.signal;
  const totalBytes = files.reduce((sum, file) => sum + file.size, 0);
  const received = new Map<number, number>();
  let completedFiles = 0,
    verifiedBytes = 0,
    activeFiles = 0,
    retryCount = 0,
    nextIndex = 0,
    receivedBytes = 0;
  let failure: unknown;
  const emit = (state: QueueProgress['state']) => {
    try {
      options.onProgress?.({
        state,
        totalFiles: files.length,
        completedFiles,
        activeFiles,
        totalBytes,
        receivedBytes,
        verifiedBytes,
        retryCount,
      });
    } catch {
      /* Progress observers cannot interrupt the download queue. */
    }
  };
  const setReceived = (index: number, size: number) => {
    receivedBytes += size - (received.get(index) ?? 0);
    received.set(index, size);
  };
  const wait =
    options.wait ?? ((milliseconds, signal) => delay(milliseconds, undefined, { signal }));
  const worker = async () => {
    while (!signal.aborted) {
      const index = nextIndex++;
      if (index >= files.length) return;
      const artifact = files[index];
      activeFiles++;
      emit('running');
      try {
        for (let attempt = 0; ; attempt++) {
          try {
            await downloadArtifact(root, artifact, {
              signal,
              fetcher: options.fetcher,
              onProgress: (size) => {
                setReceived(index, size);
                emit('running');
              },
            });
            completedFiles++;
            verifiedBytes += artifact.size;
            setReceived(index, artifact.size);
            break;
          } catch (error) {
            if (signal.aborted) throw signal.reason;
            const retryable =
              (error instanceof DownloadFailure && error.retryable) ||
              (error instanceof Error && error.name === 'TimeoutError');
            if (!retryable || attempt >= retries) throw error;
            retryCount++;
            emit('retrying');
            await wait(250 * 2 ** attempt, signal);
          }
        }
      } catch (error) {
        if (failure === undefined) failure = error;
        controller.abort(error);
      } finally {
        activeFiles--;
        emit('running');
      }
    }
  };
  emit('running');
  await Promise.all(Array.from({ length: Math.min(concurrency, files.length) }, worker));
  if (signal.aborted) {
    emit(options.signal?.aborted ? 'cancelled' : 'failed');
    throw options.signal?.aborted ? options.signal.reason : (failure ?? signal.reason);
  }
  emit('completed');
  return { completedFiles, verifiedBytes, totalBytes, retryCount };
}

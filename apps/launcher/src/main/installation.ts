import { randomUUID } from 'node:crypto';
import { writeFile, rename, unlink, statfs } from 'node:fs/promises';
import { join, relative } from 'node:path';
import { prepareVersionPlan } from './prepare-plan.ts';
import { downloadPlan } from './download-queue.ts';
import { extractNatives } from './native-extraction.ts';
import { VERSION_METADATA } from './version-plan.ts';
import { profileId } from './validation.ts';
import type { InstallationStatus } from '../shared/installation.ts';

type Dependencies = {
  prepare: typeof prepareVersionPlan;
  download: typeof downloadPlan;
  extract: typeof extractNatives;
  availableBytes: (root: string) => Promise<number>;
};
type Job = { id: string; controller: AbortController; done: Promise<void> };
const defaults: Dependencies = {
  prepare: prepareVersionPlan,
  download: downloadPlan,
  extract: extractNatives,
  availableBytes: async (root) => {
    const info = await statfs(root);
    return info.bavail * info.bsize;
  },
};

/** Main-only lifecycle. The renderer supplies a known profile ID, never URLs or paths. */
export class InstallationService {
  private statuses = new Map<string, InstallationStatus>();
  private job: Job | null = null;
  private dependencies: Dependencies;
  private readonly rootForProfile: (id: string) => Promise<string>;
  constructor(
    rootForProfile: (id: string) => Promise<string>,
    dependencies: Partial<Dependencies> = {},
  ) {
    this.rootForProfile = rootForProfile;
    this.dependencies = { ...defaults, ...dependencies };
  }
  get running() {
    return this.job !== null;
  }
  status(input: unknown): InstallationStatus {
    const id = profileId(input);
    return structuredClone(
      this.statuses.get(id) ?? {
        profileId: id,
        phase: 'idle',
        completedFiles: 0,
        totalFiles: 0,
        receivedBytes: 0,
        verifiedBytes: 0,
        totalBytes: 0,
        retryCount: 0,
        message: 'Базовые файлы ещё не проверялись в этом сеансе.',
      },
    );
  }
  start(input: unknown): InstallationStatus {
    const id = profileId(input);
    if (this.job)
      throw new Error('Уже идёт подготовка файлов. Дождитесь её завершения или отмените.');
    const status = {
      ...this.status(id),
      phase: 'planning' as const,
      completedFiles: 0,
      totalFiles: 0,
      receivedBytes: 0,
      verifiedBytes: 0,
      totalBytes: 0,
      retryCount: 0,
      message: 'Проверяем официальные metadata Minecraft 1.8.9…',
    };
    this.statuses.set(id, status);
    const job: Job = { id, controller: new AbortController(), done: Promise.resolve() };
    this.job = job;
    job.done = this.perform(job);
    return this.status(id);
  }
  async cancel(input: unknown) {
    const id = profileId(input);
    const job = this.job;
    if (job?.id === id) {
      this.update(id, {
        phase: 'cancelling',
        message: 'Останавливаем загрузки и сохраняем полученные части…',
      });
      job.controller.abort();
      await job.done;
    }
    return this.status(id);
  }
  async settled() {
    await this.job?.done;
  }
  async shutdown() {
    if (this.job) await this.cancel(this.job.id);
  }
  private update(id: string, change: Partial<InstallationStatus>) {
    Object.assign(this.statuses.get(id)!, change);
  }
  private async perform(job: Job) {
    const signal = job.controller.signal;
    let receiptTemp: string | undefined;
    try {
      const root = await this.rootForProfile(job.id);
      signal.throwIfAborted();
      const plan = await this.dependencies.prepare(root, { signal });
      signal.throwIfAborted();
      // Conservative room for a full file set, native expansion and filesystem overhead.
      if ((await this.dependencies.availableBytes(root)) < plan.totalBytes + 160 * 1024 * 1024)
        throw new Error('Недостаточно свободного места. Освободите не менее 320 МиБ и повторите.');
      signal.throwIfAborted();
      this.update(job.id, {
        phase: 'downloading',
        totalFiles: plan.files.length,
        totalBytes: plan.totalBytes,
        message: 'Загружаем и проверяем базовые файлы Minecraft…',
      });
      await this.dependencies.download(root, plan.files, {
        signal,
        onProgress: (progress) => {
          if (signal.aborted) return;
          this.update(job.id, {
            completedFiles: progress.completedFiles,
            totalFiles: progress.totalFiles,
            receivedBytes: progress.receivedBytes,
            verifiedBytes: progress.verifiedBytes,
            totalBytes: progress.totalBytes,
            retryCount: progress.retryCount,
            message:
              progress.state === 'retrying'
                ? 'Соединение прервалось. Повторяем загрузку…'
                : 'Загружаем и проверяем базовые файлы Minecraft…',
          });
        },
      });
      signal.throwIfAborted();
      this.update(job.id, {
        phase: 'extracting',
        message: 'Проверяем и распаковываем библиотеки Windows…',
      });
      const paths = new Set(plan.natives.map((native) => native.archive));
      const natives = await this.dependencies.extract(
        root,
        plan.files.filter((file) => paths.has(file.relativePath)),
        { signal },
      );
      signal.throwIfAborted();
      const receipt = {
        schemaVersion: 1,
        kind: 'minecraft-base-only',
        version: plan.version,
        loggingVersion: plan.loggingVersion,
        loaderVersion: plan.loaderVersion,
        metadataSha1: VERSION_METADATA.sha1,
        completedAt: new Date().toISOString(),
        fileCount: plan.files.length,
        verifiedBytes: plan.totalBytes,
        nativeDirectory: relative(root, natives.directory).replaceAll('\\', '/'),
        natives: natives.files,
        launchReady: false,
        blockers: plan.blockers,
      };
      receiptTemp = join(root, `base-installation-${randomUUID()}.tmp`);
      await writeFile(receiptTemp, JSON.stringify(receipt, null, 2) + '\n', {
        flag: 'wx',
        mode: 0o600,
      });
      signal.throwIfAborted();
      await rename(receiptTemp, join(root, 'base-installation.json'));
      receiptTemp = undefined;
      signal.throwIfAborted();
      this.update(job.id, {
        phase: 'prepared',
        completedFiles: plan.files.length,
        receivedBytes: plan.totalBytes,
        verifiedBytes: plan.totalBytes,
        message: 'Базовые файлы проверены. Для запуска ещё нужны сборка Space и вход Microsoft.',
      });
    } catch (error) {
      this.update(job.id, {
        phase: signal.aborted ? 'cancelled' : 'error',
        message: signal.aborted
          ? 'Подготовка отменена. При повторе используем проверенные файлы и сохранённые части.'
          : error instanceof Error
            ? error.message
            : 'Не удалось подготовить файлы.',
      });
    } finally {
      if (receiptTemp) await unlink(receiptTemp).catch(() => {});
      if (this.job === job) this.job = null;
    }
  }
}

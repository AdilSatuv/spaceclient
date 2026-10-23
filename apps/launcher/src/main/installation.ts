import { UserFacingError } from './application/user-facing-error.ts';
import { randomUUID } from 'node:crypto';
import { writeFile, rename, unlink, statfs } from 'node:fs/promises';
import { join, relative } from 'node:path';
import { prepareVersionPlan } from './prepare-plan.ts';
import { downloadPlan } from './download-queue.ts';
import { extractNatives } from './native-extraction.ts';
import { VERSION_METADATA } from './version-plan.ts';
import { profileId } from './validation.ts';
import type { SpaceBundleManifest, InstalledSpace } from './game-bundle.ts';
import type { InstallationStatus } from '../shared/installation.ts';

type Dependencies = {
  prepare: typeof prepareVersionPlan;
  download: typeof downloadPlan;
  extract: typeof extractNatives;
  availableBytes: (root: string) => Promise<number>;
  space: {
    pin: SpaceBundleManifest;
    install: (root: string, signal: AbortSignal) => Promise<InstalledSpace>;
  } | null;
};
type Job = { id: string; controller: AbortController; done: Promise<void> };
const defaults: Dependencies = {
  prepare: prepareVersionPlan,
  download: downloadPlan,
  extract: extractNatives,
  space: null,
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
        spaceVersion: null,
        message: 'Базовые файлы ещё не проверялись в этом сеансе.',
      },
    );
  }
  start(input: unknown): InstallationStatus {
    const id = profileId(input);
    if (this.job)
      throw new UserFacingError(
        'Уже идёт подготовка файлов. Дождитесь её завершения или отмените.',
      );
    const status = {
      ...this.status(id),
      phase: 'planning' as const,
      completedFiles: 0,
      totalFiles: 0,
      receivedBytes: 0,
      verifiedBytes: 0,
      totalBytes: 0,
      retryCount: 0,
      spaceVersion: null,
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
      const space = this.dependencies.space;
      const totalFiles = plan.files.length + (space ? 1 : 0);
      const totalBytes = plan.totalBytes + (space?.pin.size ?? 0);
      signal.throwIfAborted();
      // Conservative room for a full file set, native expansion and filesystem overhead.
      if ((await this.dependencies.availableBytes(root)) < totalBytes + 160 * 1024 * 1024)
        throw new UserFacingError(
          'Недостаточно свободного места. Освободите не менее 320 МиБ и повторите.',
        );
      signal.throwIfAborted();
      this.update(job.id, {
        phase: 'downloading',
        totalFiles,
        totalBytes,
        message: 'Загружаем и проверяем базовые файлы Minecraft…',
      });
      await this.dependencies.download(root, plan.files, {
        signal,
        onProgress: (progress) => {
          if (signal.aborted) return;
          this.update(job.id, {
            completedFiles: progress.completedFiles,
            totalFiles,
            receivedBytes: progress.receivedBytes,
            verifiedBytes: progress.verifiedBytes,
            totalBytes,
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
      let installedSpace: InstalledSpace | null = null;
      if (space) {
        this.update(job.id, {
          phase: 'installing',
          message: 'Проверяем и добавляем сборку Space Client…',
        });
        installedSpace = await space.install(root, signal);
        if (installedSpace.sha256 !== space.pin.sha256 || installedSpace.size !== space.pin.size)
          throw new UserFacingError('Установленный Space не совпадает со сборкой лаунчера.');
        signal.throwIfAborted();
      }
      const receipt = {
        schemaVersion: 1,
        kind: installedSpace ? 'space-development' : 'minecraft-base-only',
        version: plan.version,
        loggingVersion: plan.loggingVersion,
        loaderVersion: plan.loaderVersion,
        metadataSha1: VERSION_METADATA.sha1,
        completedAt: new Date().toISOString(),
        fileCount: totalFiles,
        verifiedBytes: totalBytes,
        space: installedSpace,
        nativeDirectory: relative(root, natives.directory).replaceAll('\\', '/'),
        natives: natives.files,
        nativeCacheReused: natives.fromCache,
        launchReady: false,
        blockers: installedSpace
          ? ['Не подтверждены Microsoft-вход, владение игрой и runtime.']
          : plan.blockers,
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
        completedFiles: totalFiles,
        receivedBytes: totalBytes,
        verifiedBytes: totalBytes,
        spaceVersion: installedSpace?.modVersion ?? null,
        message: installedSpace
          ? 'Сборка Space подготовлена. Для запуска ещё нужен Microsoft-вход и проверка игры.'
          : 'Базовые файлы проверены. Для запуска ещё нужны сборка Space и вход Microsoft.',
      });
    } catch (error) {
      this.update(job.id, {
        phase: signal.aborted ? 'cancelled' : 'error',
        message: signal.aborted
          ? 'Подготовка отменена. При повторе используем проверенные файлы и сохранённые части.'
          : error instanceof UserFacingError
            ? error.message
            : 'Не удалось подготовить файлы.',
      });
    } finally {
      if (receiptTemp) await unlink(receiptTemp).catch(() => {});
      if (this.job === job) this.job = null;
    }
  }
}

import { spawn } from 'node:child_process';
import { UserFacingError } from './application/user-facing-error.ts';
import { prepareLocalLaunch } from './launch-plan.ts';
import type { SpaceBundleManifest } from './game-bundle.ts';
import type { Profile } from '../shared/models.ts';
import type { LaunchStatus } from '../shared/launch.ts';
import { nickname, profileId } from './validation.ts';

type Dependencies = { prepare: typeof prepareLocalLaunch; spawn: typeof spawn };
type Job = { done: Promise<void>; controller: AbortController; spawned: boolean };

/** Owns the reservation from the first await through process close, never accepts renderer args. */
export class LaunchService {
  private statuses = new Map<string, LaunchStatus>();
  private jobs = new Map<string, Job>();
  private closing = false;
  private readonly rootForProfile: (id: string) => Promise<string>;
  private readonly pin: SpaceBundleManifest | null;
  private readonly dependencies: Dependencies;
  constructor(
    rootForProfile: (id: string) => Promise<string>,
    pin: SpaceBundleManifest | null,
    dependencies: Partial<Dependencies> = {},
  ) {
    this.rootForProfile = rootForProfile;
    this.pin = pin;
    this.dependencies = { prepare: prepareLocalLaunch, spawn, ...dependencies };
  }
  get running() {
    return this.jobs.size > 0;
  }
  status(input: unknown): LaunchStatus {
    const id = profileId(input);
    return structuredClone(
      this.statuses.get(id) ?? {
        profileId: id,
        mode: 'local',
        phase: 'idle',
        message: 'Локальная игра без регистрации аккаунта.',
        exitCode: null,
        log: [],
      },
    );
  }
  start(profile: Profile, localNickname: string | null): LaunchStatus {
    const id = profileId(profile.id);
    const name = nickname(localNickname);
    if (this.closing) throw new UserFacingError('Лаунчер завершает работу.');
    if (this.jobs.has(id))
      throw new UserFacingError('Игра этого профиля уже запускается или работает.');
    this.statuses.set(id, {
      profileId: id,
      mode: 'local',
      phase: 'checking',
      message: 'Проверяем файлы и Java…',
      exitCode: null,
      log: ['Начата локальная проверка установки и runtime.'],
    });
    const job: Job = { done: Promise.resolve(), controller: new AbortController(), spawned: false };
    this.jobs.set(id, job);
    job.done = this.perform(id, { ...profile }, name, job);
    return this.status(id);
  }
  private update(id: string, change: Partial<LaunchStatus>, entry?: string) {
    const status = this.statuses.get(id)!;
    Object.assign(status, change);
    if (entry) status.log = [...status.log, entry].slice(-60);
  }
  private async perform(id: string, profile: Profile, name: string, job: Job) {
    try {
      const root = await this.rootForProfile(id);
      const plan = await this.dependencies.prepare(
        root,
        profile,
        name,
        this.pin,
        job.controller.signal,
      );
      job.controller.signal.throwIfAborted();
      this.update(
        id,
        { phase: 'starting', message: 'Запускаем локальную игру…' },
        'Установка и Java проверены.',
      );
      await new Promise<void>((resolve, reject) => {
        const child = this.dependencies.spawn(plan.executable, plan.args, {
          cwd: plan.cwd,
          env: plan.env,
          shell: false,
          windowsHide: false,
          stdio: ['ignore', 'pipe', 'pipe'],
        });
        let failed = false;
        const seen = new Set<string>();
        // Never forward raw stdout/stderr, arguments, paths, nicknames, tokens or stack traces.
        // Each chunk is bounded and converted to a fixed diagnostic label once per process.
        const output = (chunk: Buffer) => {
          const text = chunk.toString('utf8', 0, Math.min(chunk.length, 8192));
          for (const [pattern, label] of [
            ['Loading Minecraft', 'Загрузчик начал запуск Minecraft.'],
            ['Space Client 0.1.0 initialized', 'Игровой мод Space инициализирован.'],
            ['OpenAL initialized', 'Звуковой движок инициализирован.'],
            ['OutOfMemoryError', 'Java сообщила о нехватке памяти.'],
            ['Failed to create display', 'Не удалось создать игровое окно OpenGL.'],
            ['Exception in thread', 'Java сообщила об ошибке игрового потока.'],
          ]) {
            if (text.includes(pattern!) && !seen.has(label!)) {
              seen.add(label!);
              this.update(id, {}, label!);
            }
          }
        };
        child.stdout?.on('data', output);
        child.stderr?.on('data', output);
        child.once('spawn', () => {
          job.spawned = true;
          this.update(
            id,
            {
              phase: 'running',
              message: 'Локальная игра работает. Закройте её из меню Minecraft.',
            },
            'Процесс Java запущен.',
          );
        });
        child.once('error', () => {
          failed = true;
        });
        child.once('close', (code, signal) => {
          if (failed) {
            reject(
              new UserFacingError('Не удалось запустить Java. Проверьте сохранённый runtime.'),
            );
            return;
          }
          const normal = code === 0 && signal === null;
          this.update(
            id,
            {
              phase: normal ? 'exited' : 'error',
              exitCode: code,
              message: normal
                ? 'Игра завершена. Можно запустить снова.'
                : 'Игра завершилась с ошибкой. Проверьте Java и восстановите файлы.',
            },
            code === null
              ? 'Процесс завершён без кода выхода.'
              : `Процесс завершён с кодом ${code}.`,
          );
          resolve();
        });
      });
    } catch (error) {
      this.update(
        id,
        {
          phase: 'error',
          message: job.controller.signal.aborted
            ? 'Запуск отменён при закрытии лаунчера.'
            : error instanceof UserFacingError
              ? error.message
              : 'Не удалось запустить игру. Проверьте Java и восстановите файлы.',
        },
        'Локальный запуск остановлен.',
      );
    } finally {
      this.jobs.delete(id);
    }
  }
  async settled() {
    await Promise.all([...this.jobs.values()].map((job) => job.done));
  }
  async shutdown() {
    this.closing = true;
    // Never terminate a world while it is saving. Keep the main process until Java closes.
    for (const job of this.jobs.values()) if (!job.spawned) job.controller.abort();
    await this.settled();
  }
}

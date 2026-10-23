import type { CommandHandlers } from '../../shared/ipc.ts';
import type { LauncherDependencies } from './ports.ts';
import { UserFacingError } from './user-facing-error.ts';

export class LauncherApplication {
  readonly commands: CommandHandlers;
  private readonly dependencies: LauncherDependencies;
  private checkingJava = false;
  private choosingJava = false;

  constructor(dependencies: LauncherDependencies) {
    this.dependencies = dependencies;
    const { profiles, installation } = dependencies;
    const noPayload = (payload: unknown) => {
      if (payload !== undefined) throw new UserFacingError('Действие не принимает параметры.');
    };
    this.commands = {
      'state:get': (payload) => {
        noPayload(payload);
        return profiles.snapshot();
      },
      'profile:save': (payload) => profiles.save(payload),
      'profile:create': (payload) => {
        noPayload(payload);
        return profiles.create();
      },
      'profile:select': (payload) => profiles.select(payload),
      'java:choose': (payload) => this.chooseJava(payload),
      'java:check': (payload) => this.checkJava(payload),
      'profile:open': async (payload) => {
        const path = await profiles.instanceDirectory(payload);
        await dependencies.desktop.openDirectory(path);
        return null;
      },
      'installation:get': (payload) => installation.status(profiles.profile(payload).id),
      'installation:start': (payload) => installation.start(profiles.profile(payload).id),
      'installation:cancel': (payload) => installation.cancel(profiles.profile(payload).id),
    };
  }
  get running() {
    return this.dependencies.installation.running;
  }
  shutdown() {
    return this.dependencies.installation.shutdown();
  }

  private async chooseJava(payload: unknown) {
    if (this.choosingJava) throw new UserFacingError('Выбор Java уже открыт.');
    const { profiles, desktop } = this.dependencies;
    const profile = profiles.profile(payload);
    this.choosingJava = true;
    try {
      const selected = await desktop.chooseJava();
      return selected === null ? profiles.snapshot() : await profiles.setJava(profile.id, selected);
    } finally {
      this.choosingJava = false;
    }
  }
  private async checkJava(payload: unknown) {
    if (this.checkingJava) throw new UserFacingError('Проверка Java уже выполняется.');
    const profile = this.dependencies.profiles.profile(payload);
    this.checkingJava = true;
    try {
      return await this.dependencies.inspectJava(profile.javaPath);
    } finally {
      this.checkingJava = false;
    }
  }
}
